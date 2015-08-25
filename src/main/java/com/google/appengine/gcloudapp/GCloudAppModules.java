/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import com.google.apphosting.utils.config.AppEngineWebXml;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.ArrayList;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Manage an application via gcloud app modules.
 *
 * @author Ludo
 */
public abstract class GCloudAppModules extends AbstractGcloudMojo {

  /**
   * server The App Engine server to connect to.
   *
   * @parameter property="gcloud.server"
   */
  private String server;

  /**
   * version The version of the app that will be created or replaced by this
   * deployment.
   *
   * @parameter property="gcloud.version"
   */
  private String version;

  protected abstract String getSubCommand();

   public GCloudAppModules() {
     this.deployCommand = true;
  }
   
   @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("");

    ArrayList<String> devAppServerCommand = createCommand(
            getApplicationDirectory(), getSubCommand());
    startCommand(new File(getApplicationDirectory()), devAppServerCommand, WaitDirective.WAIT_SERVER_STOPPED);
  }

  protected ArrayList<String> createCommand(String appDir, String subCommand) throws MojoExecutionException {

    getLog().info("Running gcloud app modules...");

    ArrayList<String> devAppServerCommand = new ArrayList<>();
    setupInitialCommands(devAppServerCommand);

    devAppServerCommand.add("modules");

    File f = new File(appDir, "WEB-INF/appengine-web.xml");
    if (!f.exists()) { // EAR project possibly, add all modules one by one:
      File ear = new File(appDir);
      for (File w : ear.listFiles()) {
        if (new File(w, "WEB-INF/appengine-web.xml").exists()) {
          System.out.println("Need to handle EAR for module " + w.getAbsolutePath());
        }
      }
    } else {
      // get module name and module version
      AppEngineWebXml xmlContent = getAppEngineWebXml(appDir);
      String module = xmlContent.getModule();
      String localVersion = xmlContent.getMajorVersionId();
      devAppServerCommand.add(subCommand);
      if (module == null) {
        module = "default";
      }
      devAppServerCommand.add(module);

      // Add in additional options for starting the DevAppServer
      if (version != null) {
        devAppServerCommand.add("--version=" + version);
      } else if (localVersion != null) {
        devAppServerCommand.add("--version=" + localVersion);
      } else {
        getLog().error("Warning: the <version> Maven configuration is not defined, or <version> is not defined in appengine-web.xml");

      }
    }
    setupExtraCommands(devAppServerCommand);

    if (server != null) {
      devAppServerCommand.add("--server=" + server);
    }

    return devAppServerCommand;
  }

  @Override
  protected ArrayList<String> getCommand(String appDir) throws MojoExecutionException {
    return null; //not used
  }

  /**
   * Delete the module of this app.
   *
   * @goal module_delete
   * @execute phase="package"
   * @threadSafe false
   */
  static public class Delete extends GCloudAppModules {

    @Override
    protected String getSubCommand() {
      return "delete";
    }
  }

  /**
   * Cancel Deployment.
   *
   * @goal module_cancel_deployment
   * @execute phase="package"
   * @threadSafe false
   */
  static public class CancelDeployment extends GCloudAppModules {

    @Override
    protected String getSubCommand() {
      return "cancel-deployment";
    }
  }

  /**
   * set default.
   *
   * @goal module_set_default
   * @execute phase="package"
   * @threadSafe false
   */
  static public class SetDefault extends GCloudAppModules {

    @Override
    protected String getSubCommand() {
      return "set-default";
    }
  }

  /**
   * set Managed (google or self).
   *
   * @goal module_set_managed_by
   * @execute phase="package"
   * @threadSafe false
   */
  static public class SetManaged extends GCloudAppModules {

    @Override
    protected String getSubCommand() {
      return "set-managed-by";
    }
  }

  /**
   * start a module
   *
   * @goal module_start
   * @execute phase="package"
   * @threadSafe false
   */
  static public class Start extends GCloudAppModules {

    @Override
    protected String getSubCommand() {
      return "start";
    }
  }

  /**
   * stop a module
   *
   * @goal module_stop
   * @execute phase="package"
   * @threadSafe false
   */
  static public class Stop extends GCloudAppModules {

    @Override
    protected String getSubCommand() {
      return "stop";
    }
  }
}
