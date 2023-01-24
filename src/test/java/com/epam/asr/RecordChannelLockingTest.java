package com.epam.asr;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Disabled // manual tests
class RecordChannelLockingTest {

  public static final String EXIT_COMMAND = "q";
  public static final String FILE_CHOOSER_COMMAND = "f";
  public static final String RECORDING_COMMAND = "r";

  private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

  @Test // the only successful test
  void shouldNotHangIfAccessedAfterFileChooser() {
    filechooser();
    System.out.println("breakpoint, lock / unlock screen before resuming");
    assertDoesNotThrow(this::record);
  }

  @Test
  void shouldNotHangIfAccessedBeforeFileChooser() {
    record();
    filechooser();
    System.out.println("breakpoint, lock / unlock screen before resuming");
    assertDoesNotThrow(this::record);
  }

  @Test
  void shouldNotHangInUiThread() throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(() -> {
      record();
      filechooser();
      System.out.println("breakpoint, lock / unlock screen before resuming");
      assertDoesNotThrow(this::record);
    });
  }

  @Test
  void shouldNotHangInUiThreadSeparately() throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(this::record);
    SwingUtilities.invokeAndWait(this::filechooser);
    System.out.println("breakpoint, lock / unlock screen before resuming");
    assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(this::record));
  }

  public static void main(String[] args) throws InterruptedException, InvocationTargetException {
    new RecordChannelLockingTest().runManually();
  }

  void runManually() throws InterruptedException, InvocationTargetException {
    String line;
    printHelp();
    while (!EXIT_COMMAND.equals(line = readLine())) {
      if (RECORDING_COMMAND.equals(line)) {
        SwingUtilities.invokeAndWait(this::record);
      } else if (FILE_CHOOSER_COMMAND.equals(line)) {
        SwingUtilities.invokeAndWait(this::filechooser);
      } else {
        System.out.println("Unknown command: " + line);
      }
      printHelp();
    }
  }

  private String readLine() {
    try {
      return reader.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void printHelp() {
    System.out.printf("Press %s to quit, %s to record, %s to create FileChooser.%n",
      EXIT_COMMAND, RECORDING_COMMAND, FILE_CHOOSER_COMMAND);
  }

  private void filechooser() {
    System.out.println("----");
    System.out.println("File chooser created with hash " + new JFileChooser().hashCode());
    System.out.println("Thread: " + Thread.currentThread().getName());
  }

  private void record() {
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

  private Mixer getMixer() {
    return Arrays.stream(AudioSystem.getMixerInfo())
      .map(AudioSystem::getMixer)
      .filter(this::isRecordingDevice)
      .skip(1) // to skip the primary one
      .findAny()
      .orElseThrow();
  }

  private boolean isRecordingDevice(Mixer mixer) {
    Line.Info[] lineInfos = mixer.getTargetLineInfo();
    return lineInfos.length > 0 && lineInfos[0].getLineClass() == TargetDataLine.class;
  }
}