/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 */
package com.google.appengine;

import java.io.File;

/**
 *
 * @author ludo
 */
public class Utils {

  public static String getPythonExecutableLocation() {
    String pythonLocation = "python"; //default in the path for Linux
    boolean isWindows = System.getProperty("os.name").contains("Windows");
    if (isWindows) {
      pythonLocation = System.getenv("CLOUDSDK_PYTHON");
      if (pythonLocation == null) {
        //   getLog().info("CLOUDSDK_PYTHON env variable is not defined. Choosing a default python.exe interpreter.");
        //   getLog().info("If this does not work, please set CLOUDSDK_PYTHON to a correct Python interpreter location.");

        pythonLocation = "python.exe";
      }
    } else {
      String possibleLinuxPythonLocation = System.getenv("CLOUDSDK_PYTHON");
      if (possibleLinuxPythonLocation != null) {
        //  getLog().info("Found a python interpreter specified via CLOUDSDK_PYTHON at: " + possibleLinuxPythonLocation);
        pythonLocation = possibleLinuxPythonLocation;
      }
    }
    return pythonLocation;
  }

  public static String getCloudSDKLocation() {
    String gcloudDir;
    boolean isWindows = System.getProperty("os.name").contains("Windows");
    if (isWindows) {
      String programFiles = System.getenv("ProgramFiles");
      if (programFiles == null) {
        programFiles = System.getenv("ProgramFiles(x86)");
      }
      if (programFiles == null) {
        gcloudDir = "cannotFindProgramFiles";
      } else {
        gcloudDir = programFiles + "\\Google\\Cloud SDK\\google-cloud-sdk";
      }
    } else {
      gcloudDir = System.getProperty("user.home") + "/google-cloud-sdk";
      if (!new File(gcloudDir).exists()) {
        // try devshell VM:
        gcloudDir = "/google/google-cloud-sdk";
        if (!new File(gcloudDir).exists()) {
          // try bitnani Jenkins VM:
          gcloudDir = "/usr/local/share/google/google-cloud-sdk";
        }
      }
    }
    return gcloudDir;
  }

}
