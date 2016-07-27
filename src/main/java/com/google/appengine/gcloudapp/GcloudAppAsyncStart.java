/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import java.io.File;
import java.util.ArrayList;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Starts the Gcloud App Engine development server and does not wait.
 *
 * @author Ludo
 * @goal run_start
 * @execute phase="package"
 * @threadSafe false
 */
public class GcloudAppAsyncStart extends GCloudAppRun {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("");
    getLog().info("Gcloud SDK - Starting the Development Server");
    getLog().info("");

    if (application_directory == null) {
      application_directory =
          maven_project.getBuild().getDirectory() + "/" + maven_project.getBuild().getFinalName();
    }

    File appDirFile = new File(application_directory);

    if (!appDirFile.exists()) {
      throw new MojoExecutionException(
          "The application directory does not exist : " + application_directory);
    }

    if (!appDirFile.isDirectory()) {
      throw new MojoExecutionException(
          "The application directory is not a directory : " + application_directory);
    }

    ArrayList<String> devAppServerCommand = getCommand(application_directory);

    startCommand(appDirFile, devAppServerCommand, WaitDirective.WAIT_SERVER_STARTED);
  }

}
