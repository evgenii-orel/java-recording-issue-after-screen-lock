package com.epam.sound;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

/**
 * Manual test to try out different sequences of recording, creating JFileChooser & screen locking.
 * */
class RecordChannelLockingConsoleTest {

  public static final String EXIT_COMMAND = "q";
  public static final String FILE_CHOOSER_COMMAND = "f";
  public static final String INIT_COM_COMMAND = "c";
  public static final String RECORDING_COMMAND = "r";

  private final static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

  public static void main(String[] args) throws InterruptedException, InvocationTargetException, IOException {
    RecordChannelLockingTest test = new RecordChannelLockingTest();
    String line;
    printHelp();
    while (!EXIT_COMMAND.equals(line = reader.readLine())) {
      if (RECORDING_COMMAND.equals(line)) {
        SwingUtilities.invokeAndWait(test::record);
      } else if (FILE_CHOOSER_COMMAND.equals(line)) {
        SwingUtilities.invokeAndWait(test::fileChooser);
      } else if (INIT_COM_COMMAND.equals(line)) {
        SwingUtilities.invokeAndWait(test::initCom);
      } else {
        System.out.println("Unknown command: " + line);
      }
      printHelp();
    }
  }

  static void printHelp() {
    System.out.printf("Press %s to quit, %s to record, %s to create FileChooser.%n",
      EXIT_COMMAND, RECORDING_COMMAND, FILE_CHOOSER_COMMAND);
  }
}