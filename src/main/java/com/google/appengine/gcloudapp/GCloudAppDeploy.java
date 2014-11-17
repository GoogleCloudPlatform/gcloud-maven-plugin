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
public class GCloudAppDeploy extends AbstractGcloudMojo {

  /**
   * server The App Engine server to connect to.
   *
   * @parameter
   */
  private String server;

  /**
   * version The version of the app that will be created or replaced by this
   * deployment.
   *
   * @parameter
   */
  private String version;

  /**
   * env-vars ENV_VARS Environment variable overrides for your app.
   *
   * @parameter
   */
  private String env_vars;


  /**
   * force Force deploying, overriding any previous in-progress deployments to
   * this version.
   *
   * @parameter
   */
  private boolean force;
  /**
   * Set the encoding to be used when compiling Java source files (default
   * "UTF-8")
   *
   * @parameter
   */
  private String compile_encoding;
  /**
   * Delete the JSP source files after compilation
   *
   * @parameter
   */
  private boolean delete_jsps;
  /**
   * Do not jar the classes generated from JSPs
   *
   * @parameter
   */
  private boolean disable_jar_jsps;
  /**
   * Jar the WEB-INF/classes content
   *
   * @parameter
   */
  private boolean enable_jar_classes;
  /**
   * Split large jar files (> 32M) into smaller fragments
   *
   * @parameter
   */
  private boolean enable_jar_splitting;
  /**
   * Do not use symbolic links when making the temporary (staging) gcloud_directory
 used in uploading Java apps
   *
   * @parameter
   */
  private boolean no_symlinks;
  /**
   * Do not delete temporary (staging) gcloud_directory used in uploading Java apps
   *
   * @parameter
   */
  private boolean retain_upload_dir;
  /**
   * When --enable-jar-splitting is specified and --jar-splitting-excludes
   * specifies a comma-separated list of suffixes, a file in a jar whose name
   * ends with one of the suffixes will not be included in the split jar
   * fragments
   *
   * @parameter
   */
  private String jar_splitting_excludes;

  /**
   * Set the deployed version to be the default serving version.
   *
   * @parameter
   */
  private boolean set_default;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("");
    String appDir = maven_project.getBuild().getDirectory() + "/" + maven_project.getBuild().getFinalName();
    File appDirFile = new File(appDir);
    if (!appDirFile.exists()) {
      throw new MojoExecutionException("The application directory does not exist : " + appDir);
    }
    if (!appDirFile.isDirectory()) {
      throw new MojoExecutionException("The application directory is not a directory : " + appDir);
    }
    ArrayList<String> devAppServerCommand = getCommand(appDir);
    startCommand(appDirFile, devAppServerCommand, WaitDirective.WAIT_SERVER_STOPPED);
  }

  @Override
  protected ArrayList<String> getCommand(String appDir) throws MojoExecutionException {

    getLog().info("Running gcloud app deploy...");

    ArrayList<String> devAppServerCommand = new ArrayList<>();
    setupInitialCommands(devAppServerCommand);

    devAppServerCommand.add("deploy");

    File f = new File(appDir, "WEB-INF/appengine-web.xml");
    if (!f.exists()) { // EAR project possibly, add all modules one by one:
      File ear = new File(appDir);
      for (File w : ear.listFiles()) {
        if (new File(w, "WEB-INF/appengine-web.xml").exists()) {
          devAppServerCommand.add(w.getAbsolutePath());
        }
      }
    } else {
      // Point to our application
      devAppServerCommand.add(appDir);
    }
    setupExtraCommands(devAppServerCommand);

    // Add in additional options for starting the DevAppServer
    if (version != null) {
      devAppServerCommand.add("--version=" + version);
    }
    if (env_vars != null) {
      devAppServerCommand.add("--env-vars=" + env_vars);
    }
    if (server != null) {
      devAppServerCommand.add("--server=" + server);
    } else if (server != null) {
      devAppServerCommand.add("--server=" + server);
    }
    if (force) {
      devAppServerCommand.add("--force");
    }
    if (delete_jsps) {
      devAppServerCommand.add("--delete-jsps");
    }
    if (disable_jar_jsps) {
      devAppServerCommand.add("--disable-jar-jsps");
    }
    if (enable_jar_classes) {
      devAppServerCommand.add("--enable-jar-classes");
    }
    if (enable_jar_splitting) {
      devAppServerCommand.add("--enable-jar-splitting");
    }
    if (compile_encoding != null) {
      devAppServerCommand.add("--compile-encoding=" + compile_encoding);
    }
    if (retain_upload_dir) {
      devAppServerCommand.add("--retain-upload-dir");
    }
    if (no_symlinks) {
      devAppServerCommand.add("--no-symlinks");
    }
    if (jar_splitting_excludes != null) {
      devAppServerCommand.add("--jar-splitting-excludes=" + jar_splitting_excludes);
    }
    if (set_default) {
      devAppServerCommand.add("--set-default");
    }
    return devAppServerCommand;
  }

}
