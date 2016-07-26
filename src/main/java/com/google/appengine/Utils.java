/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 */
package com.google.appengine;

import java.io.File;

/**
 * @author ludo
 */
public class Utils {

  public static String getPythonExecutableLocation() {
    String pythonLocation = "python"; // default in the path for Linux
    boolean isWindows = System.getProperty("os.name").contains("Windows");
    if (isWindows) {
      pythonLocation = System.getenv("CLOUDSDK_PYTHON");
      if (pythonLocation == null) {
        pythonLocation = "python.exe";
      }
    } else {
      String possibleLinuxPythonLocation = System.getenv("CLOUDSDK_PYTHON");
      if (possibleLinuxPythonLocation != null) {
        pythonLocation = possibleLinuxPythonLocation;
      }
    }
    return pythonLocation;
  }

  public static String getCloudSDKLocation() {
    String gcloudDir;
    boolean isWindows = System.getProperty("os.name").contains("Windows");
    if (isWindows) {
      String relPath = "\\Google\\Cloud SDK\\google-cloud-sdk";
      // first look for user installation under "LOCALAPPDATA"
      String localSDK = System.getenv("LOCALAPPDATA") + relPath;
      if (new File(localSDK).exists()) {
        return localSDK;
      }
      // then look for globally installed Cloud SDK:
      String programFiles = System.getenv("ProgramFiles");
      if (programFiles == null) {
        programFiles = System.getenv("ProgramFiles(x86)");
      }
      if (programFiles == null) {
        gcloudDir = "cannotFindProgramFiles";
      } else {
        gcloudDir = programFiles + relPath;
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

  /**
   * Checks if either CLOUDSDK_PYTHON_SITEPACKAGES or VIRTUAL_ENV is defined.
   *
   * <p> If either variable is defined, we shall not disable import of module 'site.
   *
   * @return true if it is OK to disable import of module 'site' (python -S)
   */
  public static boolean canDisableImportOfPythonModuleSite() {
    String sitePackages = System.getenv("CLOUDSDK_PYTHON_SITEPACKAGES");
    String virtualEnv = System.getenv("VIRTUAL_ENV");
    boolean noSiteDefined = sitePackages == null || sitePackages.isEmpty();
    boolean noVirtEnvDefined = virtualEnv == null || virtualEnv.isEmpty();
    return noSiteDefined && noVirtEnvDefined;
  }
}
