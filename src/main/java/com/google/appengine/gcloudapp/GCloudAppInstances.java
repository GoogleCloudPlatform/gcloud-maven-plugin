package com.google.appengine.gcloudapp;

import com.google.appengine.repackaged.net.sourceforge.yamlbeans.YamlException;
import com.google.appengine.repackaged.net.sourceforge.yamlbeans.YamlReader;
import com.google.apphosting.utils.config.AppEngineWebXml;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Manage an application via gcloud app instances.
 *
 * @author Ludo
 */
public abstract class GCloudAppInstances extends AbstractGcloudMojo {

  /**
   * version The version of the app that will be created or replaced by this deployment.
   *
   * @parameter property="gcloud.version"
   */
  private String version;

  /**
   * instance The instance of the app that will be used to enable/disable debug. Default is 1.
   *
   * @parameter property="gcloud.instance"
   */
  private String instance = "1";

  protected abstract String[] getSubCommand();

  public GCloudAppInstances() {
    this.deployCommand = true;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("");
    ArrayList<String> devAppServerCommand = createCommand(
        getApplicationDirectory(), getSubCommand());
    startCommand(new File(getApplicationDirectory()), devAppServerCommand,
        AbstractGcloudMojo.WaitDirective.WAIT_SERVER_STOPPED);
  }

  protected ArrayList<String> createCommand(String appDir, String[] subCommand)
      throws MojoExecutionException {

    getLog().info("Running gcloud app instances...");

    ArrayList<String> devAppServerCommand = new ArrayList<>();
    setupInitialCommands(devAppServerCommand);

    devAppServerCommand.add("instances");

    File appwebxml = new File(appDir, "WEB-INF/appengine-web.xml");
    File appyaml = new File(appengine_config_directory, "app.yaml");
    String service = null;
    String localVersion = null;
    if (appwebxml.exists()) {      // get module name and module version
      AppEngineWebXml xmlContent = getAppEngineWebXml(appDir);
      service = xmlContent.getModule();
      if (service == null) {
        service = xmlContent.getService();
      }
      localVersion = xmlContent.getMajorVersionId();
    } else if (appyaml.exists()) {
      try {
        YamlReader reader = new YamlReader(new FileReader(appyaml));
        Object object = reader.read();
        reader.close();
        Map map = (Map) object;
        service = (String) map.get("module");
        if (service == null) {
          service = (String) map.get("service");
        }
      } catch (FileNotFoundException | YamlException ex) {
        Logger.getLogger(GCloudAppModules.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
        Logger.getLogger(GCloudAppModules.class.getName()).log(Level.SEVERE, null, ex);
      }
    } else {
      System.out.println("Cannot find app.yaml or appengine-web.xml");
      return null;
    }
    for (String c : subCommand) {
      devAppServerCommand.add(c);
    }
    if (service == null) {
      service = "default";
    }
    devAppServerCommand.add("--service");
    devAppServerCommand.add(service);
    // Add version
    if (version != null) {
      devAppServerCommand.add("--version");
      devAppServerCommand.add(version);
    } else if (localVersion != null) {
      devAppServerCommand.add("--version");
      devAppServerCommand.add(localVersion);
    } else {
      getLog().error(
          "Warning: the Gcloud <version> Maven configuration is not defined, or <version> is not "
              + "defined in appengine-web.xml");
    }
    if (instance != null) {
      devAppServerCommand.add(instance);
    } else {
      getLog().error("You need to specify an instance via <instance> configuration.");

    }

    return devAppServerCommand;
  }

  @Override
  protected ArrayList<String> getCommand(String appDir) throws MojoExecutionException {
    return null; // not used
  }

  /**
   * Enable debug of an instance
   *
   * @goal enable_debug
   * @execute phase="package"
   * @threadSafe false
   */
  public static class EnableDebug extends GCloudAppInstances {

    @Override
    protected String[] getSubCommand() {
      return new String[]{"enable-debug"};
    }
  }

  /**
   * Disable debug of an instance
   *
   * @goal disable_debug
   * @execute phase="package"
   * @threadSafe false
   */
  public static class DisableDebug extends GCloudAppInstances {

    @Override
    protected String[] getSubCommand() {
      return new String[]{"disable-debug"};
    }
  }
}
