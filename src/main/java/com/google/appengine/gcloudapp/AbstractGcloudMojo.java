/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;


import com.google.appengine.SdkResolver;
import com.google.appengine.Utils;
import com.google.appengine.repackaged.com.google.common.io.Files;
import com.google.appengine.tools.admin.AppCfg;
import com.google.apphosting.utils.config.AppEngineApplicationXml;
import com.google.apphosting.utils.config.AppEngineApplicationXmlReader;
import com.google.apphosting.utils.config.AppEngineWebXml;
import com.google.apphosting.utils.config.AppEngineWebXmlReader;
import com.google.apphosting.utils.config.EarHelper;
import com.google.common.base.Charsets;
import static com.google.common.base.Charsets.UTF_8;
import com.google.common.base.Joiner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.ini4j.Ini;

/**
 *
 * @author ludo
 */
public abstract class AbstractGcloudMojo extends AbstractMojo {

  /**
   * @parameter property="project"
   * @required
   * @readonly
   */
  protected MavenProject maven_project;

  /**
   * gcloud installation gcloud_directory
   *
   * @parameter expression="${gcloud.gcloud_directory}"
   */
  protected String gcloud_directory;

  /**
   * docker_host
   *
   * @parameter expression="${gcloud.docker_host}" default-value="ENV_or_default"
   */
  protected String docker_host;
  /**
   * docker_tls_verify
   *
   * @parameter expression="${gcloud.docker_tls_verify}" default-value="ENV_or_default"
   */
  protected String docker_tls_verify;

  /**
   * docker_host_cert_path
   *
   * @parameter expression="${gcloud.docker_cert_path}" default-value="ENV_or_default"
   */
  protected String docker_cert_path;

  /**
   * Override the default verbosity for this command. This must be a standard
   * logging verbosity level: [debug, info, warning, error, critical, none]
   * (Default: [info]).
   *
   * @parameter expression="${gcloud.verbosity}"
   */
  protected String verbosity;

  /**
   * Google Cloud Platform gcloud_project to use for this invocation.
   *
   * @parameter expression="${gcloud.gcloud_project}"
   */
  protected String gcloud_project;

 /**
   * version The version of the app that will be created or replaced by this
   * deployment.
   *
   * @parameter expression="${gcloud.version}"
   */
  protected String version;

  /**
   * Quiet mode, if true does not ask to perform the action.
   *
   * @parameter expression="${gcloud.quiet}" default-value="true"
   */
  protected boolean quiet = true;

  /**
   * The location of the appengine application to run.
   *
   * @parameter expression="${gcloud.application_directory}"
   */
  protected String application_directory;


  /**
   * Use this option if you are deploying using a remote docker host.
   *
   * @parameter expression="${gcloud.remote}"
   */
  protected boolean remote;

  /**
   * Perform a hosted (´remote´) or local Docker build.
   * To perform a local build, you must have your local docker environment
   * configured correctly.
   *
   * @parameter expression="${gcloud.docker_build}"
   */
  protected String docker_build;
  
   /**
   * The directory for the Staging phase. It has to be under target/ and is deleted
   * at each run or deploy command.
   *
   * @parameter expression="${gcloud.staging_directory}" default-value="${project.build.directory}/appengine-staging"
   */
  protected String staging_directory;
  
 /**
   * Tell if the command will be for run or deploy. Default is false: command is
   * for `gcloud run`.
   *
   */
  protected boolean deployCommand = false;

  protected abstract ArrayList<String> getCommand(String appDir) throws MojoExecutionException;

  protected ArrayList<String> setupInitialCommands(ArrayList<String> commands) throws MojoExecutionException {
    String pythonLocation = Utils.getPythonExecutableLocation();

    commands.add(pythonLocation);
    if (Utils.canDisableImportOfPythonModuleSite()) {
      commands.add("-S");
    }

    if (gcloud_directory == null) {
      gcloud_directory = Utils.getCloudSDKLocation();
    }
    File s = new File(gcloud_directory);
    File script = new File(s, "/lib/googlecloudsdk/gcloud/gcloud.py");

    if (!script.exists()) {
      getLog().error("Cannot determine the default location of the Google Cloud SDK.");
      getLog().error("If you need to install the Google Cloud SDK, follow the instructions located at https://cloud.google.com/appengine/docs/java/managed-vms");
      getLog().error("You can then set it via <gcloud_directory> </gcloud_directory> in the pom.xml");
      throw new MojoExecutionException("Unkown Google Cloud SDK location:" + gcloud_directory);
    }

    if (deployCommand) {
      commands.add(gcloud_directory + "/lib/googlecloudsdk/gcloud/gcloud.py");
      if (quiet) {
        commands.add("--quiet");
      }
      if (verbosity != null) {
        commands.add("--verbosity=" + verbosity);
      }
      if (gcloud_project != null) {
        commands.add("--project=" + gcloud_project);
      }
      commands.add("preview");
      commands.add("app");

    } else { // run command
      File devServer = new File(gcloud_directory + "/platform/google_appengine/dev_appserver.py");
      // Check if we need to install the app-engine-java component!
      if (!devServer.exists()) {
        installJavaAppEngineComponent(pythonLocation);
      }

      commands.add(gcloud_directory + "/platform/google_appengine/dev_appserver.py");
      commands.add("--skip_sdk_update_check=true");
      if (verbosity != null) {
        commands.add("--dev_appserver_log_level=" + verbosity);
      }
      if (gcloud_project != null) {
        commands.add("-A");
        commands.add(gcloud_project);
      } else {
        commands.add("-A");
        commands.add("app"); // local sdk default project name 
      }
    }
 
    return commands;
  }

  protected static enum WaitDirective {

    WAIT_SERVER_STARTED,
    WAIT_SERVER_STOPPED
  }

  protected void startCommand(File appDirFile, ArrayList<String> devAppServerCommand, WaitDirective waitDirective) throws MojoExecutionException {
    getLog().info("Running " + Joiner.on(" ").join(devAppServerCommand));

    Thread stdOutThread;
    Thread stdErrThread;
    try {

      ProcessBuilder processBuilder = new ProcessBuilder(devAppServerCommand);

      processBuilder.directory(appDirFile);

      processBuilder.redirectErrorStream(true);
      Map<String, String> env = processBuilder.environment();
      String env_docker_host = env.get("DOCKER_HOST");
      String docker_host_tls_verify = env.get("DOCKER_TLS_VERIFY");
      String docker_host_cert_path = env.get("DOCKER_CERT_PATH");
      boolean userDefined = (env_docker_host != null)
              || (docker_host_tls_verify != null)
              || (docker_host_cert_path != null);

     if (!userDefined) {
        if ("ENV_or_default".equals(docker_host)) {
          if (env_docker_host == null) {
            if (env.get("DEVSHELL_CLIENT_PORT") != null) {
              // we know we have a good chance to be in an old Google devshell:
              env_docker_host = "unix:///var/run/docker.sock";
            } else {
                // we assume docker machine environment (Windows, Mac, and some Linux)
                  env_docker_host = "tcp://192.168.99.100:2376";
                }
              }
        } else {
          env_docker_host = docker_host;
        }
        env.put("DOCKER_HOST", env_docker_host);
        // we handle TLS extra variables only when we are tcp:
        if (env_docker_host.startsWith("tcp")) {
          if ("ENV_or_default".equals(docker_tls_verify)) {
            if (env.get("DOCKER_TLS_VERIFY") == null) {
              env.put("DOCKER_TLS_VERIFY", "1");
            }
          } else {
            env.put("DOCKER_TLS_VERIFY", docker_tls_verify);
          }
         // do not set the cert path if we do a dockerless deploy command:
         boolean dockerless = deployCommand && remote;
         if (!dockerless) {
           if ("ENV_or_default".equals(docker_cert_path)) {
             if (env.get("DOCKER_CERT_PATH") == null) {
                 env.put("DOCKER_CERT_PATH",
                         System.getProperty("user.home")
                         + File.separator
                         + ".docker"
                         + File.separator
                         + "machine"
                         + File.separator
                         + "machines"   
                         + File.separator
                         + "default"                                  
                 );
               }
           } else {
             env.put("DOCKER_CERT_PATH", docker_cert_path);
           }
         }
       }
      }
      //export DOCKER_CERT_PATH=/Users/ludo/.boot2docker/certs/boot2docker-vm
      //export DOCKER_TLS_VERIFY=1
      //export DOCKER_HOST=tcp://192.168.59.103:2376

      // for the docker library path:
      env.put("PYTHONPATH", gcloud_directory + "/platform/google_appengine/lib/docker");

      final Process devServerProcess = processBuilder.start();

      final CountDownLatch waitStartedLatch = new CountDownLatch(1);

      final Scanner stdOut = new Scanner(devServerProcess.getInputStream());
      stdOutThread = new Thread("standard-out-redirection-devappserver") {
        @Override
        public void run() {
         boolean serverStartedOK = false;
         try {
            long healthCount = 0;
            while (stdOut.hasNextLine() && !Thread.interrupted()) {
              String line = stdOut.nextLine();
              // emit this every 30 times, no need for more...
              if (line.contains("GET /_ah/health?IsLastSuccessful=yes HTTP/1.1\" 200 2")) {
                waitStartedLatch.countDown();
                if (healthCount % 20 == 0) {
                  getLog().info(line);
                }
                healthCount++;
              } else if (line.contains("Dev App Server is now running")) {
                // App Engine V1
                waitStartedLatch.countDown();
                serverStartedOK = true;

              } else if (line.contains("INFO:oejs.Server:main: Started")) {
                // App Engine V2
                waitStartedLatch.countDown();
                serverStartedOK = true;

              } else {
                getLog().info(line);
              }
            }
          } finally {
           waitStartedLatch.countDown();
           if ((!serverStartedOK) && (!deployCommand)) {
             throw new RuntimeException("The Java Dev Server has stopped.");

           }
          }
        }
      };
      stdOutThread.setDaemon(true);
      stdOutThread.start();

      final Scanner stdErr = new Scanner(devServerProcess.getErrorStream());
      stdErrThread = new Thread("standard-err-redirection-devappserver") {
        @Override
        public void run() {
          while (stdErr.hasNextLine() && !Thread.interrupted()) {
            getLog().error(stdErr.nextLine());
          }
        }
      };
      stdErrThread.setDaemon(true);
      stdErrThread.start();
      if (waitDirective == WaitDirective.WAIT_SERVER_STOPPED) {
        Runtime.getRuntime().addShutdownHook(new Thread("destroy-devappserver") {
          @Override
          public void run() {
            if (devServerProcess != null) {
              devServerProcess.destroy();
            }
          }
        });

        devServerProcess.waitFor();
        int status = devServerProcess.exitValue();
        if (status != 0) {
          getLog().error("Error: gcloud app command with exit code : " + status);
          throw new MojoExecutionException("Error: gcloud app command exit code is: " + status);
        }
      } else if (waitDirective == WaitDirective.WAIT_SERVER_STARTED) {
        waitStartedLatch.await();
        getLog().info("");
        getLog().info("App Engine Dev Server started in Async mode and running.");
        getLog().info("you can stop it with this command: mvn gcloud:run_stop");
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not start the dev app server", e);
    } catch (InterruptedException e) {
    }
  }

  protected String getApplicationDirectory() throws MojoExecutionException {
    if (application_directory != null) {
      return application_directory;
    }
    application_directory = maven_project.getBuild().getDirectory() + "/" + maven_project.getBuild().getFinalName();
    File appDirFile = new File(application_directory);
    if (!appDirFile.exists()) {
      throw new MojoExecutionException("The application directory does not exist : " + application_directory);
    }
    if (!appDirFile.isDirectory()) {
      throw new MojoExecutionException("The application directory is not a directory : " + application_directory);
    }
    return application_directory;
  }

  protected String getProjectIdfromMetaData() {
    try {
      URL url = new URL("http://metadata/computeMetadata/v1/project/project-id");
      URLConnection connection = url.openConnection();
      connection.setRequestProperty("X-Google-Metadata-Request", "True");
      try (BufferedReader reader
              = new BufferedReader(new InputStreamReader(
                      connection.getInputStream(), UTF_8))) {
        return reader.readLine();
      }
    } catch (IOException ignore) {
      // return null if can't determine
      return null;
    }
  }

  protected String getAppId() throws MojoExecutionException {

    if (gcloud_project != null) {
      return gcloud_project;
    }

    try { // Check for Cloud SDK properties:
      String userHome;
      if (System.getProperty("os.name").contains("Windows")) {
        userHome = System.getenv("APPDATA");
      } else {
        userHome = System.getProperty("user.home") + "/.config";
      }
      //Default value:
      File cloudSDKProperties = new File(userHome + "/gcloud/properties");
      // But can be overriden: take this one if it is:
      String env = System.getenv("CLOUDSDK_CONFIG");
      if (env != null) {
        cloudSDKProperties = new File(env, "properties");
      }
      if (cloudSDKProperties.exists()) {
        org.ini4j.Ini ini = new org.ini4j.Ini();
        ini.load(new FileReader(cloudSDKProperties));
        Ini.Section section = ini.get("core");
        String project = section.get("project");
        if (project != null) {
          getLog().info("Getting project name: " + project
                  + " from gcloud settings.");
          return project;
        }
      }
      // now try the metadata server location:
      String project = getProjectIdfromMetaData();
      if (project != null) {
        getLog().info("Getting project name: " + project
                + " from the metadata server.");
        return project;
      }
    } catch (IOException ioe) {
      // nothing for now. Trying to read appengine-web.xml.
    }

    String appDir = getApplicationDirectory();
    if (EarHelper.isEar(appDir)) { // EAR project
      AppEngineApplicationXmlReader reader
              = new AppEngineApplicationXmlReader();
      AppEngineApplicationXml appEngineApplicationXml = reader.processXml(
              getInputStream(new File(appDir, "META-INF/appengine-application.xml")));
      return appEngineApplicationXml.getApplicationId();

    }
    if (new File(appDir, "WEB-INF/appengine-web.xml").exists()) {
      return getAppEngineWebXml(appDir).getAppId();
    } else {
      return null;
    }
  }

  private static InputStream getInputStream(File file) {
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException fnfe) {
      throw new IllegalStateException("File should exist - '" + file + "'");
    }
  }

  protected AppEngineWebXml getAppEngineWebXml(String webAppDir) throws MojoExecutionException {
    AppEngineWebXmlReader reader = new AppEngineWebXmlReader(webAppDir);
    AppEngineWebXml appengineWebXml = reader.readAppEngineWebXml();
    return appengineWebXml;
  }


  /**
   * The entry point to Aether, i.e. the component doing all the work.
   *
   * @component
   */

  protected RepositorySystem repoSystem;

  /**
   * The current repository/network configuration of Maven.
   *
   * @parameter default-value="${repositorySystemSession}"
   * @readonly
   */
  protected RepositorySystemSession repoSession;

  /**
   * The project's remote repositories to use for the resolution of project
   * dependencies.
   *
   * @parameter default-value="${project.remoteProjectRepositories}"
   * @readonly
   */
  protected List<RemoteRepository> projectRepos;

  /**
   * The project's remote repositories to use for the resolution of plugins and
   * their dependencies.
   *
   * @parameter default-value="${project.remotePluginRepositories}"
   * @readonly
   */
  protected List<RemoteRepository> pluginRepos;

  protected void resolveAndSetSdkRoot() throws MojoExecutionException {

    File sdkBaseDir = SdkResolver.getSdk(maven_project, repoSystem, repoSession, pluginRepos, projectRepos);

    try {
      System.setProperty("appengine.sdk.root", sdkBaseDir.getCanonicalPath());
    } catch (IOException e) {
      throw new MojoExecutionException("Could not open SDK zip archive.", e);
    }
  }

  protected File executeAppCfgStagingCommand(String appDir)
          throws MojoExecutionException {

   ArrayList<String> arguments = new ArrayList<>();
  File destinationDir = new File(staging_directory);
  if (!destinationDir.getParentFile().getAbsolutePath().equals(maven_project.getBuild().getDirectory())) {
       throw new MojoExecutionException("Does not want to delete a directory no under the target directory" +staging_directory);
   
  }
    try {
      FileUtils.deleteDirectory(destinationDir);
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot delete staging directory.", ex);
    }

   getLog().info("Creating staging directory in: " + destinationDir.getAbsolutePath());
   resolveAndSetSdkRoot();
  // System.setProperty("appengine.sdk.root", gcloud_directory +"/platform/google_appengine/google/appengine/tools/java");
    AppEngineWebXml appengineWeb = getAppEngineWebXml(appDir);
    if ("true".equals(appengineWeb.getBetaSettings().get("java_quickstart"))) {
      arguments.add("--enable_quickstart");
    }
    arguments.add("--disable_update_check");
    File  appDirFile= new File(appDir);
    if (!new File(appDirFile, "WEB-INF/web.xml").exists()) {
      PrintWriter out;
      try {
        out = new PrintWriter(new File(appDirFile, "WEB-INF/web.xml"));
        out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        out.println("<web-app version=\"3.1\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\"></web-app>");
        out.close();
      } catch (FileNotFoundException ex) {
          throw new MojoExecutionException("Error: creating default web.xml " + ex);
      }
    }
    if (!new File(appDirFile, ".appyamlgenerated").exists()) {
      PrintWriter out;
      try {
        out = new PrintWriter(new File(appDirFile, ".appyamlgenerated"));
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        System.out.println(dateFormat.format(date));
        out.println("generated by the Maven Plugin on " + dateFormat.format(date));
        out.close();
      } catch (FileNotFoundException ex) {
        throw new MojoExecutionException("Error: generating .appyamlgenerated " + ex);
      }
    }
    arguments.add("-A");
    arguments.add("notused");

    if (version != null && !version.isEmpty()) {
      arguments.add("-V");
      arguments.add(version);
    }
    arguments.add("stage");
    arguments.add(appDir);
    arguments.add(destinationDir.getAbsolutePath());
    getLog().info("Running appcfg " + Joiner.on(" ").join(arguments));
    AppCfg.main(arguments.toArray(new String[arguments.size()]));
    // For now, treat custom as java7 so that the app run command works.
    try {
      File fileAppYaml = new File(destinationDir, "/app.yaml");
      String content = Files.toString(fileAppYaml, Charsets.UTF_8);
      if ("2".equals(appengineWeb.getEnv())) {
        content = content.replace("runtime: java7", "runtime: java");
      }
      content = content.replace("auto_id_policy: default", "");
      Files.write(content, fileAppYaml, Charsets.UTF_8);
    } catch (IOException ioe) {
      System.out.println("Error " + ioe);
    }
    
    File[] yamlFiles = new File(destinationDir, "/WEB-INF/appengine-generated").listFiles();
    for (File f : yamlFiles) {
     try {
       Files.copy(f, new File(appDir, f.getName()));
     } catch (IOException ex) {
          throw new MojoExecutionException("Error: copying yaml file " + ex);
     }
    }
    File qs = new File(destinationDir, "/WEB-INF/quickstart-web.xml");
    if (qs.exists()) {
     try {
       Files.copy(qs, new File(appDir, "/WEB-INF/quickstart-web.xml"));
     } catch (IOException ex) {
          throw new MojoExecutionException("Error: copying WEB-INF/quickstart-web.xml" + ex);
     }
    }
    // Delete the xml as we have now the index.yaml equivalent
    File index = new File(appDir, "/WEB-INF/datastore-indexes.xml");
    if (index.exists()) {
      index.delete();
    }
    return destinationDir;
  }

  /**
   * Executes the gcloud components update app-engine-java command to install
   * the extra component needed for the Maven plugin.
   * @param pythonLocation
   * @throws MojoExecutionException
   */
  private void installJavaAppEngineComponent(String pythonLocation ) throws MojoExecutionException {
    ArrayList<String> installCommand = new ArrayList<>();
    installCommand.add(pythonLocation);
    if (Utils.canDisableImportOfPythonModuleSite()) {
      installCommand.add("-S");
    }
    installCommand.add(gcloud_directory + "/lib/googlecloudsdk/gcloud/gcloud.py");
    installCommand.add("components");
    installCommand.add("update");
    installCommand.add("app-engine-java");
    installCommand.add("--quiet");
    ProcessBuilder pb = new ProcessBuilder(installCommand);
    getLog().info("Installing the Cloud SDK app-engine-java component");
    getLog().info("Please, be patient, it takes a while on slow network...");

    try {
      Process process = pb.start();
      final Scanner stdOut = new Scanner(process.getInputStream());
      new Thread("standard-out-redirection") {
        @Override
        public void run() {
          while (stdOut.hasNextLine() && !Thread.interrupted()) {
            getLog().info(stdOut.nextLine());
          }
        }
      };
      process.waitFor();
      getLog().info("Cloud SDK app-engine-java component installed.");

    } catch (IOException | InterruptedException ex) {
      throw new MojoExecutionException("Error: cannot execute gcloud command " + ex);
    }
  }
}
