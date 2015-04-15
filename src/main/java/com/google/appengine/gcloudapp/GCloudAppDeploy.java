/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import com.google.appengine.tools.admin.AppCfg;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
   * @parameter expression="${gcloud.server}"
   */
  private String server;

  /**
   * version The version of the app that will be created or replaced by this
   * deployment.
   *
   * @parameter expression="${gcloud.version}"
   */
  private String version;

  /**
   * env-vars ENV_VARS Environment variable overrides for your app.
   *
   * @parameter expression="${gcloud.env_vars}"
   */
  private String env_vars;


  /**
   * force Force deploying, overriding any previous in-progress deployments to
   * this version.
   *
   * @parameter expression="${gcloud.force}"
   */
  private boolean force;
  /**
   * Set the encoding to be used when compiling Java source files (default
   * "UTF-8")
   *
   * @parameter expression="${gcloud.compile_encoding}"
   */
  private String compile_encoding;
  /**
   * Delete the JSP source files after compilation
   *
   * @parameter expression="${gcloud.delete_jsps}"
   */
  private boolean delete_jsps;
  /**
   * Do not jar the classes generated from JSPs
   *
   * @parameter expression="${gcloud.disable_jar_jsps}"
   */
  private boolean disable_jar_jsps;
  /**
   * Jar the WEB-INF/classes content
   *
   * @parameter expression="${gcloud.enable_jar_classes}"
   */
  private boolean enable_jar_classes;
  /**
   * Split large jar files (> 32M) into smaller fragments
   *
   * @parameter expression="${gcloud.enable_jar_splitting}"
   */
  private boolean enable_jar_splitting;
  /**
   * Do not use symbolic links when making the temporary (staging) gcloud_directory
 used in uploading Java apps
   *
   * @parameter expression="${gcloud.no_symlinks}"
   */
  private boolean no_symlinks;
  /**
   * Do not delete temporary (staging) gcloud_directory used in uploading Java apps
   *
   * @parameter expression="${gcloud.retain_upload_dir}"
   */
  private boolean retain_upload_dir;
  /**
   * When --enable-jar-splitting is specified and --jar-splitting-excludes
   * specifies a comma-separated list of suffixes, a file in a jar whose name
   * ends with one of the suffixes will not be included in the split jar
   * fragments
   *
   * @parameter expression="${gcloud.jar_splitting_excludes}"
   */
  private String jar_splitting_excludes;

  /**
   * Set the deployed version to be the default serving version.
   *
   * @parameter expression="${gcloud.set_default}"
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

    File temp = Files.createTempDir();
    getLog().info("Creating staging directory in: " + temp.getAbsolutePath());
    try {
      executeAppCfgStagingCommand(appDir, temp.getAbsolutePath());

      // TODO(ludo) remove with the next release as appcfg will do that step:
      File[] yamlFiles = new File(temp, "/WEB-INF/appengine-generated").listFiles();
      for (File f : yamlFiles) {
        Files.copy(f, new File(temp, f.getName()));
      }
    } catch (Exception ex) {
      getLog().error(ex);
      throw new MojoExecutionException("Staging error " + ex);

    }

    ArrayList<String> devAppServerCommand = getCommand(temp.getAbsolutePath());
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
    } 
    if (force) {
      devAppServerCommand.add("--force");
    }
/*    if (delete_jsps) {
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
    }*/

    if (set_default) {
      devAppServerCommand.add("--set-default");
    }
    return devAppServerCommand;
  }
  
  
   protected ArrayList<String> collectAppCfgParameters() {
    ArrayList<String> arguments = new ArrayList<>();

    if (server != null && !server.isEmpty()) {
      arguments.add("-s");
      arguments.add(server);
    }

 
//    if (appId != null && !appId.isEmpty()) {
//      arguments.add("-A");
//      arguments.add(appId);
//    }

    if (version != null && !version.isEmpty()) {
      arguments.add("-V");
      arguments.add(version);
    }


    if (enable_jar_splitting) {
      arguments.add("--enable_jar_splitting");
    }

    if (jar_splitting_excludes != null && !jar_splitting_excludes.isEmpty()) {
      arguments.add("--jar_splitting_excludes=" + jar_splitting_excludes);
    }

    if (retain_upload_dir) {
      arguments.add("--retain_upload_dir");
    }

    if (compile_encoding != null) {
      arguments.add("--compile-encoding=" + compile_encoding);
    }



    if (force) {
      arguments.add("-f");
    }

    if (delete_jsps) {
      arguments.add("--delete_jsps");
    }

    if (enable_jar_classes) {
      arguments.add("--enable_jar_classes");
    }

    return arguments;
  }

  protected void executeAppCfgStagingCommand( String appDir, String destDir)
      throws Exception {
    
    resolveAndSetSdkRoot();
    ArrayList<String> arguments = collectAppCfgParameters();

    arguments.add("stage");
    arguments.add(appDir);
    arguments.add(destDir);
    getLog().info("Running " + Joiner.on(" ").join(arguments));

    AppCfg.main(arguments.toArray(new String[arguments.size()]));

  }
}
