/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Stops the Gcloud App Engine development server.
 *
 * @author Ludo
 * @goal run_stop
 * @execute phase="validate"
 * @threadSafe false
 */
public class GcloudAppStop extends GCloudAppRun {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("");
    getLog().info("Gcloud SDK - Stopping the Development Server");
    getLog().info("");

    stopDevAppServer();
  }

}
