package com.google.appengine;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;

/**
 * Runs the datanucleus enhancer.
 *
 * @author Matt Stephenson <mattstep@google.com>
 * @goal enhance
 * @phase compile
 */
public class AppengineEnhancerMojo extends AbstractMojo {

  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * The Maven Session Object
   *
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  protected MavenSession session;

  /**
   * The Maven PluginManager Object
   *
   * @component
   * @required
   */
  protected BuildPluginManager plugin_manager;

  /**
   * The api to use ( JDO or JPA ) for enhancement
   *
   * @parameter expression="${gcloud.enhancer_api}" default-value="JDO"
   */
  private String enhancer_api;

  private static final String DATANUCLEUS_VERSION = "3.2.0-release";

  private static final Dependency JDO_DEPENDENCY = new Dependency() {
    {
      setGroupId("org.datanucleus");
      setArtifactId("datanucleus-api-jdo");
      setVersion(DATANUCLEUS_VERSION);
    }
  };

  private static final Dependency JPA_DEPENDENCY = new Dependency() {
    {
      setGroupId("org.datanucleus");
      setArtifactId("datanucleus-api-jpa");
      setVersion(DATANUCLEUS_VERSION);
    }
  };

  @Override
  public void execute() throws MojoExecutionException {

    if (!enhancer_api.equals("JDO") && !enhancer_api.equals("JPA")) {
      throw new MojoExecutionException("enhancerApi must be either JPA or JDO");
    }

    Plugin plugin = new Plugin();
    plugin.setGroupId("org.datanucleus");
    plugin.setArtifactId("datanucleus-maven-plugin");
    plugin.setVersion(DATANUCLEUS_VERSION);
    plugin.addDependency(enhancer_api.equals("JDO") ? JDO_DEPENDENCY : JPA_DEPENDENCY);
    for (Dependency transitiveDep : project.getDependencies()) {
      plugin.addDependency(transitiveDep);
    }

    PluginDescriptor pluginDescriptor = null;

    try {
      pluginDescriptor = plugin_manager.loadPlugin(plugin,
          project.getRemotePluginRepositories(),
          session.getRepositorySession());
    } catch (PluginNotFoundException | PluginResolutionException | PluginDescriptorParsingException
        | InvalidPluginDescriptorException e) {
      throw new MojoExecutionException("Could not load the datanucleus plugin.", e);
    }

    MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo("enhance");

    Xpp3Dom configuration = new Xpp3Dom("configuration");

    Xpp3Dom apiElement = new Xpp3Dom("api");
    apiElement.setValue(enhancer_api);

    Xpp3Dom verboseElement = new Xpp3Dom("verbose");
    verboseElement.setValue("true");

    configuration.addChild(apiElement);
    configuration.addChild(verboseElement);

    configuration = Xpp3DomUtils.mergeXpp3Dom(configuration,
        convertPlexusConfiguration(mojoDescriptor.getMojoConfiguration()));

    MojoExecution exec = new MojoExecution(mojoDescriptor, configuration);

    try {
      plugin_manager.executeMojo(session, exec);
    } catch (MojoFailureException | MojoExecutionException | PluginConfigurationException
        | PluginManagerException e) {
      throw new MojoExecutionException("Could not execute datanucleus enhancer.", e);
    }
  }

  private Xpp3Dom convertPlexusConfiguration(PlexusConfiguration config) {

    Xpp3Dom xpp3DomElement = new Xpp3Dom(config.getName());
    xpp3DomElement.setValue(config.getValue());

    for (String name : config.getAttributeNames()) {
      xpp3DomElement.setAttribute(name, config.getAttribute(name));
    }

    for (PlexusConfiguration child : config.getChildren()) {
      xpp3DomElement.addChild(convertPlexusConfiguration(child));
    }

    return xpp3DomElement;
  }
}
