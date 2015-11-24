/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import com.google.appengine.repackaged.com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Stage an application in order to be able to call the Cloud SDK deploy command.
 *
 * @author Ludo
 * @goal stage
 * @execute phase="package"
 * @threadSafe false
 */
public class GCloudAppStage extends AbstractGcloudMojo {

  /**
   * server The App Engine server to connect to.
   *
   * @parameter expression="${gcloud.server}"
   */
  protected String server;

  /**
   * env-vars ENV_VARS Environment variable overrides for your app.
   *
   * @parameter expression="${gcloud.env_vars}"
   */
  protected String env_vars;

  /**
   * force Force deploying, overriding any previous in-progress deployments to
   * this version.
   *
   * @parameter expression="${gcloud.force}"
   */
  protected boolean force;

  /**
   * Set the encoding to be used when compiling Java source files (default
   * "UTF-8")
   *
   * @parameter expression="${gcloud.compile_encoding}"
   */
  protected String compile_encoding;
  /**
   * Delete the JSP source files after compilation
   *
   * @parameter expression="${gcloud.delete_jsps}"
   */
  protected boolean delete_jsps;
  /**
   * Do not jar the classes generated from JSPs
   *
   * @parameter expression="${gcloud.disable_jar_jsps}"
   */
  protected boolean disable_jar_jsps;
  /**
   * Jar the WEB-INF/classes content
   *
   * @parameter expression="${gcloud.enable_jar_classes}"
   */
  protected boolean enable_jar_classes;
  /**
   * Split large jar files (> 32M) into smaller fragments
   *
   * @parameter expression="${gcloud.enable_jar_splitting}"
   */
  protected boolean enable_jar_splitting;
  /**
   * Do not use symbolic links when making the temporary (staging)
   * gcloud_directory used in uploading Java apps
   *
   * @parameter expression="${gcloud.no_symlinks}"
   */
  protected boolean no_symlinks;
  /**
   * Do not delete temporary (staging) gcloud_directory used in uploading Java
   * apps
   *
   * @parameter expression="${gcloud.retain_upload_dir}"
   */
  protected boolean retain_upload_dir;
  /**
   * When --enable-jar-splitting is specified and --jar-splitting-excludes
   * specifies a comma-separated list of suffixes, a file in a jar whose name
   * ends with one of the suffixes will not be included in the split jar
   * fragments
   *
   * @parameter expression="${gcloud.jar_splitting_excludes}"
   */
  protected String jar_splitting_excludes;


  public GCloudAppStage() {
    this.deployCommand = true;
  }

  protected File createStagingAreaEnv2() throws MojoExecutionException {
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
    File artifactToDeploy = new File(targetDir, maven_project.getBuild().getFinalName()
            + "-jar-with-dependencies." + packaging);
    if (!artifactToDeploy.exists()) {
      artifactToDeploy = new File(targetDir, maven_project.getBuild().getFinalName()
              + "." + packaging);
    }
    if (!artifactToDeploy.exists()) {
      // Handle fat jar with -fat.jar extension
      artifactToDeploy = new File(targetDir, maven_project.getBuild().getFinalName()
              + "-fat." + packaging);
    }
    if (artifactToDeploy.exists()) {
      try {
        File stagingArtifact = new File(stagingDir, artifactToDeploy.getName());
        Files.copy(artifactToDeploy, stagingArtifact);

        if (!System.getProperty("os.name").contains("Windows")) {
          // Woraround possible permission issues, see 
          // https://github.com/jboss-dockerfiles/wildfly/issues/19
          // Smaller image is done outside of Docker.
          Set<PosixFilePermission> perms = new HashSet<>();
          //add owners permission
          perms.add(PosixFilePermission.OWNER_READ);
          perms.add(PosixFilePermission.OWNER_WRITE);
          //add group permissions
          perms.add(PosixFilePermission.GROUP_READ);
          //add others permissions
          perms.add(PosixFilePermission.OTHERS_READ);

          java.nio.file.Files.setPosixFilePermissions(stagingArtifact.toPath(), perms);
        }
      } catch (IOException ex) {
        throw new MojoExecutionException("Error: copying artifact" + ex);
      }
    }
    return stagingDir;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File stagingDir = executeStaging();
  getLog().info("Stagin directory updated at: " + stagingDir.getAbsolutePath());

  }
  
  protected File executeStaging() throws MojoExecutionException {
    String appDir = maven_project.getBuild().getDirectory() + "/" + maven_project.getBuild().getFinalName();
    File appDirFile = new File(appDir);
    String aewebxml = appDir + "/WEB-INF/appengine-web.xml";
    String packaging = maven_project.getPackaging();

    // new Staging for pure jar or pure Jetty9 apps:
    if (packaging.equals("jar") || !new File(aewebxml).exists()) {
      return createStagingAreaEnv2();
//      ArrayList<String> devAppServerCommand = getCommand(stagingDir.getAbsolutePath());
//      startCommand(stagingDir, devAppServerCommand, WaitDirective.WAIT_SERVER_STOPPED);
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
    return appToDeploy;
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

  @Override
  protected ArrayList<String> getCommand(String appDir) throws MojoExecutionException {
    return new ArrayList<>();
  }
}
