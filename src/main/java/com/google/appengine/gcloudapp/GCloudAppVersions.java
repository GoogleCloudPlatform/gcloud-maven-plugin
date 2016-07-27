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
 * Manage an application via gcloud app versions.
 *
 * @author Ludo
 */
public abstract class GCloudAppVersions extends AbstractGcloudMojo {

  /**
   * version The version of the app that will be created or replaced by this deployment.
   *
   * @parameter property="gcloud.version"
   */
  private String version;

  protected abstract String[] getSubCommand();

  public GCloudAppVersions() {
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

    getLog().info("Running gcloud app versions...");

    ArrayList<String> devAppServerCommand = new ArrayList<>();
    setupInitialCommands(devAppServerCommand);

    devAppServerCommand.add("versions");

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
      devAppServerCommand.add(version);
    } else if (localVersion != null) {
      devAppServerCommand.add(localVersion);
    } else {
      getLog().error(
          "Warning: the Gcloud <version> Maven configuration is not defined, or <version> is not "
              + "defined in appengine-web.xml");

    }

    return devAppServerCommand;
  }

  @Override
  protected ArrayList<String> getCommand(String appDir) throws MojoExecutionException {
    return null; // not used
  }

  /**
   * start a service
   *
   * @goal service_start
   * @execute phase="package"
   * @threadSafe false
   */
  public static class Start extends GCloudAppVersions {

    @Override
    protected String[] getSubCommand() {
      return new String[]{"start"};
    }
  }

  /**
   * stop a service
   *
   * @goal service_stop
   * @execute phase="package"
   * @threadSafe false
   */
  public static class Stop extends GCloudAppVersions {

    @Override
    protected String[] getSubCommand() {
      return new String[]{"stop"};
    }
  }
}
