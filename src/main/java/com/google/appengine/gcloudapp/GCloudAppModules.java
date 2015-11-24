/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import com.google.appengine.repackaged.net.sourceforge.yamlbeans.YamlException;
import com.google.appengine.repackaged.net.sourceforge.yamlbeans.YamlReader;
import com.google.apphosting.utils.config.AppEngineWebXml;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  protected abstract String[] getSubCommand();

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

  protected ArrayList<String> createCommand(String appDir, String[] subCommand) throws MojoExecutionException {

    getLog().info("Running gcloud app modules...");

    ArrayList<String> devAppServerCommand = new ArrayList<>();
    setupInitialCommands(devAppServerCommand);

    devAppServerCommand.add("modules");

    File appwebxml = new File(appDir, "WEB-INF/appengine-web.xml");
    File appyaml = new File(appengine_config_directory, "app.yaml");
    String module = null;
    String localVersion = null;
    if (appwebxml.exists()) {      // get module name and module version
      AppEngineWebXml xmlContent = getAppEngineWebXml(appDir);
      module = xmlContent.getModule();
//      if (module == null) {
//              module = xmlContent.getService();
//      }
      localVersion = xmlContent.getMajorVersionId();

    } else if (appyaml.exists()) {
      try {
        YamlReader reader = new YamlReader(new FileReader(appyaml));
        Object object = reader.read();
        reader.close();
        Map map = (Map) object;
        module = (String) map.get("module");
        if (module == null) {
          module = (String) map.get("service");
        }
      } catch (FileNotFoundException | YamlException ex) {
        Logger.getLogger(GCloudAppModules.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(GCloudAppModules.class.getName()).log(Level.SEVERE, null, ex);
      }

    } else { // EAR project possibly, add all modules one by one:
      File ear = new File(appDir);
      for (File w : ear.listFiles()) {
        if (new File(w, "WEB-INF/appengine-web.xml").exists()) {
          System.out.println("Need to handle EAR for module " + w.getAbsolutePath());
        }
      }
      return null;
    }
    for (String c : subCommand) {
      devAppServerCommand.add(c);
    }
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
      getLog().error("Warning: the Gcloud <version> Maven configuration is not defined, or <version> is not defined in appengine-web.xml");

    }

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
    protected String[] getSubCommand() {
      return new String[]{"delete"};
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
    protected String[] getSubCommand() {
      return new String[]{"cancel-deployment"};
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
    protected String[] getSubCommand() {
      return new String[]{"set-default"};
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

    /**
     * This command sets the policy for the Managed VMs of the given modules and
     * version. When your module uses VM runtimes, you can use this command to
     * change the management mode for a set of your VMs. If you switch to
     * self-managed, SSH will be enabled on the VMs, and they will be removed
     * from the health checking pools, but will still receive requests. When you
     * switch back to Google-managed mode, any local changes on the VMs are lost
     * and they are restarted and added back into the normal pools.
     *
     * `google` Switch the VMs back to being Google managed. Any local changes
     * on the VMs will be lost.
     *
     * `self` Switch the VMs to self managed mode. This will allow you SSH into,
     * and debug your app on these machines. (Default).
     *
     * @parameter property="gcloud.set_managed_by" default-value="self"
     */
    private String set_managed_by;

    @Override
    protected String[] getSubCommand() {
      return new String[]{"set-managed-by", "--" + set_managed_by};
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
    protected String[] getSubCommand() {
      return new String[]{"start"};
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
    protected String[] getSubCommand() {
      return new String[]{"stop"};
    }
  }
}
