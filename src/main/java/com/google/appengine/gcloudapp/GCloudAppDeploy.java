/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import com.google.appengine.repackaged.com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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
   * @parameter expression="${gcloud.server}"
   */
  private String server;

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
   * Do not use symbolic links when making the temporary (staging)
   * gcloud_directory used in uploading Java apps
   *
   * @parameter expression="${gcloud.no_symlinks}"
   */
  private boolean no_symlinks;
  /**
   * Do not delete temporary (staging) gcloud_directory used in uploading Java
   * apps
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
  /**
   * Bucket used for Admin Deployment API.
   *
   * @parameter expression="${gcloud.bucket}"
   */
  protected String bucket;

  /**
   * Directory containing the App Engine app.yaml/Dockerfile files.
   *
   * @parameter expression="${gcloud.appengine_config_directory}"
   * default-value="${project.basedir}/src/main/appengine"
   */
  private String appengine_config_directory;

  public GCloudAppDeploy() {
    this.deployCommand = true;
  }

  private File createStagingArea() throws MojoExecutionException {
    String packaging = maven_project.getPackaging();
    File stagingDir = new File(staging_directory);
    File appengineConfigDir = new File(appengine_config_directory);
    stagingDir.mkdir();

    File appyaml = new File(appengineConfigDir, "app.yaml");
    if (appyaml.exists()) {
      try {
        Files.copy(appyaml, new File(stagingDir, "app.yaml"));
      } catch (IOException ex) {
        throw new MojoExecutionException("Error: copying app.yaml" + ex);
      }
    } else {
      PrintWriter out;
      try {
        out = new PrintWriter(new File(stagingDir, "app.yaml"));
        out.println("runtime: java");
        out.println("env: 2");
        out.println("api_version: 1");
        out.println("threadsafe: True");
        out.println("handlers:");
        out.println("  - url: .*");
        out.println("    script: dynamic");
        out.close();
      } catch (FileNotFoundException ex) {
        throw new MojoExecutionException("Error: creating default app.yaml " + ex);
      }
    }
    File dockerFile = new File(appengine_config_directory, "Dockerfile");
    if (dockerFile.exists()) {
      try {
        Files.copy(dockerFile, new File(stagingDir, "Dockerfile"));
      } catch (IOException ex) {
        throw new MojoExecutionException("Error: copying Dockerfile" + ex);
      }
    }

    File targetDir = new File(maven_project.getBuild().getDirectory());
    File artifactToDeploy = new File(targetDir, maven_project.getBuild().getFinalName() + "." + packaging);
    if (artifactToDeploy.exists()) {
      try {
        Files.copy(artifactToDeploy, new File(stagingDir, artifactToDeploy.getName()));
      } catch (IOException ex) {
        throw new MojoExecutionException("Error: copying artifact" + ex);
      }
    }
    return stagingDir;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String appDir = maven_project.getBuild().getDirectory() + "/" + maven_project.getBuild().getFinalName();
    File appDirFile = new File(appDir);
    String aewebxml = maven_project.getBasedir() + "/src/main/webapp/WEB-INF/appengine-web.xml";
    String packaging = maven_project.getPackaging();

    // new Staging for pure jar or pure Jetty9 apps:
    if (packaging.equals("jar") || !new File(aewebxml).exists()) {
      File stagingDir = createStagingArea();
      ArrayList<String> devAppServerCommand = getCommand(stagingDir.getAbsolutePath());
      startCommand(stagingDir, devAppServerCommand, WaitDirective.WAIT_SERVER_STOPPED);
      return;
    }

    if (!appDirFile.exists()) {
      throw new MojoExecutionException("The application directory does not exist : " + appDir);
    }
    if (!appDirFile.isDirectory()) {
      throw new MojoExecutionException("The application directory is not a directory : " + appDir);
    }
    File appYaml = new File(appDirFile, "app.yaml");
    boolean isAppYamlGenerated = new File(appDirFile, ".appyamlgenerated").exists();
    File appToDeploy;
    if (appYaml.exists() && !isAppYamlGenerated) {
      // There is an app.yaml that has not been generated by the staging phase.
      // No staging phase needed.
      appToDeploy = appDirFile;
    } else {
      // We are in the staging generated app.yaml scenario:
      appToDeploy = executeAppCfgStagingCommand(appDir);
    }

    ArrayList<String> devAppServerCommand = getCommand(appToDeploy.getAbsolutePath());
    startCommand(appDirFile, devAppServerCommand, WaitDirective.WAIT_SERVER_STOPPED);
  }

  /**
   * Add extra config files like dos, dispatch, index or queue to the deployment
   * payload.
   *
   *
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
    if (env_vars != null) {
      // TODO(ludo) uncomment when the feature is working in Cloud SDK.
      // devAppServerCommand.add("--env-vars=" + env_vars);
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

    if (set_default) {
      devAppServerCommand.add("--set-default");
    }
    return devAppServerCommand;
  }

  protected ArrayList<String> collectAppCfgParameters() throws MojoExecutionException {
    ArrayList<String> arguments = new ArrayList<>();

    if (server != null && !server.isEmpty()) {
      arguments.add("-s");
      arguments.add(server);
    }

    if (gcloud_project != null) {
      arguments.add("-A");
      arguments.add(gcloud_project);
    }

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
}
