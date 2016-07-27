/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import java.io.File;
import java.util.ArrayList;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Deploy an application via gcloud deploy.
 *
 * @author Ludo
 * @goal deploy
 * @execute phase="package"
 * @threadSafe false
 */
public class GCloudAppDeploy extends GCloudAppStage {

  /**
   * Set the deployed version to be the default serving version.
   *
   * @parameter expression="${gcloud.promote}"
   */
  private final boolean promote = true;

  /**
   * Bucket used for Admin Deployment API.
   *
   * @parameter expression="${gcloud.bucket}"
   */
  protected String bucket;

  public GCloudAppDeploy() {
    this.deployCommand = true;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File appToDeploy = executeStaging();
    ArrayList<String> devAppServerCommand = getCommand(appToDeploy.getAbsolutePath());
    startCommand(appToDeploy, devAppServerCommand, WaitDirective.WAIT_SERVER_STOPPED);
  }

  /**
   * Add extra config files like dos, dispatch, index or queue to the deployment payload.
   */
  private void addOtherConfigFiles(ArrayList<String> command, String appDir) {
    if (new File(appDir + "/cron.yaml").exists()) {
      command.add(appDir + "/cron.yaml");
    }
    if (new File(appDir + "/queue.yaml").exists()) {
      command.add(appDir + "/queue.yaml");
    }
    if (new File(appDir + "/dispatch.yaml").exists()) {
      command.add(appDir + "/dispatch.yaml");
    }
    if (new File(appDir + "/index.yaml").exists()) {
      command.add(appDir + "/index.yaml");
    }
    if (new File(appDir + "/dos.yaml").exists()) {
      command.add(appDir + "/dos.yaml");
    }
  }

  @Override
  protected ArrayList<String> getCommand(String appDir) throws MojoExecutionException {
    getLog().info("Running gcloud app deploy...");

    ArrayList<String> deployCommand = new ArrayList<>();
    setupInitialCommands(deployCommand);

    deployCommand.add("deploy");

    File f = new File(appDir, "WEB-INF/appengine-web.xml");
    if (!f.exists()) {
      // it might be an app with app.yaml:
      File appyaml = new File(appDir, "app.yaml");
      if (appyaml.exists()) {
        // Point to our application
        deployCommand.add(appDir + "/app.yaml");
        addOtherConfigFiles(deployCommand, appDir);
      } else {
        // EAR project possibly, add all modules one by one:
        File ear = new File(appDir);
        for (File w : ear.listFiles()) {
          if (new File(w, "WEB-INF/appengine-web.xml").exists()) {
            deployCommand.add(w.getAbsolutePath() + "/app.yaml");
            addOtherConfigFiles(deployCommand, w.getAbsolutePath());
          }
        }
      }
    } else {
      // Point to our application
      deployCommand.add(appDir + "/app.yaml");
      addOtherConfigFiles(deployCommand, appDir);
    }

    // Add in additional options for starting the DevAppServer
    if (version != null) {
      deployCommand.add("--version=" + version);
    }
    if (server != null) {
      deployCommand.add("--server=" + server);
    }
    if (force) {
      deployCommand.add("--force");
    }
    if (docker_build != null) {
      deployCommand.add("--docker-build=" + docker_build);

    } else if (remote) {
      deployCommand.add("--remote");
    }
    if (bucket != null) {
      deployCommand.add("--bucket=" + bucket);
    }

    if (promote) {
      deployCommand.add("--promote");
    } else {
      deployCommand.add("--no-promote");
    }

    String projectIdUsed = gcloud_project;
    if (projectIdUsed == null) {
      projectIdUsed = "the Cloud SDK default project";
    }
    String versionUsed = version;
    if (version == null) {
      versionUsed = "the time of deployment";
    }
    getLog().info("Note that the project ID and version specified in application configuration files"
        + " (e.g. app.yaml or appengine-web.xml) are ignored. The project ID is set to "
        + projectIdUsed + " and the version is set to " + versionUsed + ".");

    return deployCommand;
  }
}
