package com.epam.sound;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Demonstrates breaking TargetDataLines:
 * after subsequently using AudioSystem, JFileChooser and locking/ unlocking screen,
 * any recording attempt fails with LineUnavailableException.
 * OS: Windows, Java: 17, 19.0.2
 * The issue somehow relates to ShellFolders usage in the JFileChooser, so disabling it solves the problem.
 * Other workaround is creating a JFileChooser before using AudioSystem.
 * UI Thread usage in the tests were added since it somehow resolved the issue in our complex app setup,
 * but it doesn't help in the provided tests.
 */
@Disabled // manual tests
class RecordChannelLockingTest {

  @Test // fails
  void shouldNotHangIfAccessedBeforeFileChooser() {
    record();
    fileChooser();
    System.out.println("breakpoint, lock / unlock screen before resuming");
    assertDoesNotThrow(this::record);
  }

  @Test // successful
  void shouldNotHangIfAccessedAfterFileChooser() {
    fileChooser();
    record();
    System.out.println("breakpoint, lock / unlock screen before resuming");
    assertDoesNotThrow(this::record);
  }

  @Test // successful
  void shouldNotHangIfAccessedBeforeFileChooserWithDisabledShellFolders() {
    record();
    fileChooser(false);
    System.out.println("breakpoint, lock / unlock screen before resuming");
    assertDoesNotThrow(this::record);
  }

  @Test // fails
  void shouldNotHangInUiThread() throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(() -> {
      record();
      fileChooser();
      System.out.println("breakpoint, lock / unlock screen before resuming");
      assertDoesNotThrow(this::record);
    });
  }

  @Test // fails
  void shouldNotHangInUiThreadSeparately() throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(this::record);
    SwingUtilities.invokeAndWait(this::fileChooser);
    System.out.println("breakpoint, lock / unlock screen before resuming");
    assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(this::record));
  }

  void fileChooser() {
    fileChooser(true);
  }

  void fileChooser(boolean useShellFolder) {
    System.out.println("----");
    JFileChooser fileChooser = new JFileChooser() {
      @Override
      protected void setup(FileSystemView view) {
        super.setup(view);
        putClientProperty("FileChooser.useShellFolder", useShellFolder);
      }
    };
    System.out.println("File chooser created with hash " + fileChooser.hashCode());
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