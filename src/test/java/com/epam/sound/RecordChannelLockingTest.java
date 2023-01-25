package com.epam.sound;

import com.sun.jna.platform.win32.Ole32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFileChooser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Demonstrates breaking {@link TargetDataLine}s so that it always fails with {@link LineUnavailableException}:
 * <br>
 * Option 1. subsequently use {@link AudioSystem}, {@link JFileChooser} in any threads, lock / unlock screen
 * then any following recording attempt fails.
 * <br>
 * Option 2. subsequently use {@link AudioSystem}, COM initialization in the same thread, lock / unlock screen
 * then any following recording attempt fails in that thread.
 * <br>
 * OS: Windows 10, Java: 17, 19.0.2
 * <br>
 * The issue somehow relates to {@link sun.awt.shell.ShellFolder}s usage in the {@link JFileChooser} or COM initialization.
 * A workaround could be calling a {@link JFileChooser} in any thread
 * or COM initialization in the same thread before using {@link AudioSystem}.
 */
@Disabled // manual tests
class RecordChannelLockingTest {

  private final ExecutorService thread1 = Executors.newSingleThreadExecutor();
  private final ExecutorService thread2 = Executors.newSingleThreadExecutor();
  private final Runnable maliciousMethod = this::fileChooser;
//  private final Runnable maliciousMethod = this::initCom;

  @AfterEach
  void shutdown() {
    thread1.shutdown();
    thread2.shutdown();
  }

  // AudioSystem is accessed before JFileChooser / COM

  @Test // fails
  void shouldRecordIfBeforeInTheSameThread() throws ExecutionException, InterruptedException {
    thread1.submit(this::record).get();
    thread1.submit(maliciousMethod).get();
    // breakpoint, lock / unlock screen before resuming
    assertDoesNotThrow(() -> thread1.submit(this::record).get());
  }

  @Test // fails
  void shouldRecordIfBeforeInAnyThread() throws ExecutionException, InterruptedException {
    thread2.submit(this::record).get();
    thread1.submit(maliciousMethod).get();
    // breakpoint, lock / unlock screen before resuming
    assertDoesNotThrow(() -> thread1.submit(this::record).get());
  }

  @Test // fails for file chooser, but successful for COM
  void shouldRecordIfBeforeInAnyThread2() throws ExecutionException, InterruptedException {
    thread2.submit(this::record).get();
    thread1.submit(maliciousMethod).get();
    // breakpoint, lock / unlock screen before resuming
    assertDoesNotThrow(() -> thread2.submit(this::record).get());
  }

  // AudioSystem is accessed after JFileChooser / COM

  @Test // successful
  void shouldRecordIfAfterInTheSameThread() throws ExecutionException, InterruptedException {
    thread1.submit(maliciousMethod).get();
    thread1.submit(this::record).get();
    // breakpoint, lock / unlock screen before resuming
    assertDoesNotThrow(() -> thread1.submit(this::record).get());
  }

  @Test // successful for file chooser, but fails for COM
  void shouldRecordIfAfterInOtherThread() throws ExecutionException, InterruptedException {
    thread1.submit(maliciousMethod).get();
    thread2.submit(this::record).get();
    // breakpoint, lock / unlock screen before resuming
    assertDoesNotThrow(() -> thread1.submit(this::record).get());
  }

  @Test // successful for file chooser, but fails for COM
  void shouldRecordIfAfterInOtherThreadAndBeforeInTeSame() throws ExecutionException, InterruptedException {
    thread1.submit(maliciousMethod).get();
    thread2.submit(this::record).get();
    thread2.submit(maliciousMethod).get();
    // breakpoint, lock / unlock screen before resuming
    assertDoesNotThrow(() -> thread2.submit(this::record).get());
    assertDoesNotThrow(() -> thread1.submit(this::record).get());
  }

  void initCom() {
    System.out.println("----");
    System.out.println("Initializing COM");
    System.out.println("Thread: " + Thread.currentThread().getName());
    Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
  }

  void fileChooser() {
    System.out.println("----");
    System.out.println("File chooser created with hash " + new JFileChooser().hashCode());
    System.out.println("Thread: " + Thread.currentThread().getName());
  }

  void record() {
    try {
      System.out.println("----");
      System.out.println("Recording...");
      System.out.println("Thread: " + Thread.currentThread().getName());
      Mixer mixer = getMixer();
      System.out.println("Mixer: " + mixer.getMixerInfo().getName());
      Line.Info lineInfo = mixer.getTargetLineInfo()[0];
      TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
      line.open();
      line.close();
      System.out.println("Recording stopped.");
    } catch (LineUnavailableException e) {
      throw new IllegalStateException(e);
    }
  }

  Mixer getMixer() {
    return Arrays.stream(AudioSystem.getMixerInfo())
      .map(AudioSystem::getMixer)
      .filter(this::isRecordingDevice)
      .skip(1) // to skip the primary driver and choose one directly
      .findAny()
      .orElseThrow();
  }

  boolean isRecordingDevice(Mixer mixer) {
    Line.Info[] lineInfos = mixer.getTargetLineInfo();
    return lineInfos.length > 0 && lineInfos[0].getLineClass() == TargetDataLine.class;
  }
}