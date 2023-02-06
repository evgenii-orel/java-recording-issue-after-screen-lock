package com.epam.sound;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFileChooser;

public class StandaloneTest {

  public static void main(String[] args) {
    new StandaloneTest().doTest();
  }

  void doTest() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      record(); // move record() below fileChooser() to make it work
      fileChooser();
      System.out.println("Please lock / unlock screen and press Enter to proceed");
      reader.readLine();
      record();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
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
