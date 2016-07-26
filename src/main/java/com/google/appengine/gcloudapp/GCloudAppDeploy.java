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

    ArrayList<String> devAppServerCommand = new ArrayList<>();
    setupInitialCommands(devAppServerCommand);

    devAppServerCommand.add("deploy");

    File f = new File(appDir, "WEB-INF/appengine-web.xml");
    if (!f.exists()) {
      // it might be an app with app.yaml:
      File appyaml = new File(appDir, "app.yaml");
      if (appyaml.exists()) {
        // Point to our application
        devAppServerCommand.add(appDir + "/app.yaml");
        addOtherConfigFiles(devAppServerCommand, appDir);
      } else {
        // EAR project possibly, add all modules one by one:
        File ear = new File(appDir);
        for (File w : ear.listFiles()) {
          if (new File(w, "WEB-INF/appengine-web.xml").exists()) {
            devAppServerCommand.add(w.getAbsolutePath() + "/app.yaml");
            addOtherConfigFiles(devAppServerCommand, w.getAbsolutePath());
          }
        }
      }
    } else {
      // Point to our application
      devAppServerCommand.add(appDir + "/app.yaml");
      addOtherConfigFiles(devAppServerCommand, appDir);
    }

    // Add in additional options for starting the DevAppServer
    if (version != null) {
      devAppServerCommand.add("--version=" + version);
    }
    if (server != null) {
      devAppServerCommand.add("--server=" + server);
    }
    if (force) {
      devAppServerCommand.add("--force");
    }
    if (docker_build != null) {
      devAppServerCommand.add("--docker-build=" + docker_build);

    } else if (remote) {
      devAppServerCommand.add("--remote");
    }
    if (bucket != null) {
      devAppServerCommand.add("--bucket=" + bucket);
    }

    if (promote) {
      devAppServerCommand.add("--promote");
    } else {
      devAppServerCommand.add("--no-promote");
    }
    return devAppServerCommand;
  }
}
