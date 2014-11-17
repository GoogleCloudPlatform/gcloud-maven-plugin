/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 */
package com.google.appengine.endpoints;

import com.google.api.server.spi.tools.EndpointsTool;
import com.google.common.base.Function;
import com.google.common.base.Joiner;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs the various endpoints tools commands.
 *
 * @author Ludovic Champenois ludo at google dot com
 *
 * @requiresDependencyResolution compile
 */
public abstract class EndpointsMojo extends AbstractMojo {

  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  protected MavenProject project;

  /**
   * The classpath of the service-service_classes.
   *
   * @parameter expression="${service_classes}" default-value="${project.build.directory}/classes"
   */
  protected String service_classes;

  /**
   * The directory for the generated discovery file.
   *
   * @parameter expression="${output_directory}" default-value="${project.build.directory}/generated-sources/appengine-endpoints"
   */
  protected String output_directory;

  /**
   * The source location of the web.xml file.
   *
   * @parameter expression="${webxml_source_path}" default-value="${basedir}/src/main/webapp/WEB-INF/web.xml"
   */
  private String webxml_source_path;
  
  /**
   * The full qualified names of the service endpoints service_classes(comma separated).
   * If not specified, the maven plugin will calculate the list based on
 Annotation scanning of @Api service_classes.
   *
   * @parameter expression="${service_class_names}"
   */
  protected String service_class_names;
  
    /**
   * The build system used for building the generated client project: maven or gradle.
   *
   * @parameter expression="${build_system}"  default-value="maven"
   */
  protected String build_system;

  protected void handleClassPath(ArrayList<String> arguments) {
    Iterable<File> jars = Iterables.transform(
            Iterables.filter(project.getArtifacts(), new Predicate<Artifact>() {
              @Override
              public boolean apply(Artifact artifact) {
                return artifact.getScope().equals("compile");
              }
            }), new Function<Artifact, File>() {
      @Override
      public File apply(Artifact artifact) {
        return artifact.getFile();
      }
    });

    String cp = Joiner.on(System.getProperty("path.separator")).join(jars);
    arguments.add("-cp");
    arguments.add(project.getBuild().getOutputDirectory() +
      System.getProperty("path.separator") + service_classes +
      System.getProperty("path.separator") + cp);
  }
  
  abstract protected ArrayList<String> collectParameters(String command);
 
  protected void executeEndpointsCommand(String action, String extraParams [],
          String[] lastParam)
      throws MojoExecutionException {
    ArrayList<String> arguments = collectParameters(action);

    for (String param : extraParams) {
      arguments.add(param);
    }
    for (String param : lastParam) {
      arguments.add(param);
      getLog().info("Using Class Name:" + param);
    }
    try {
      getLog().info("Executing endpoints Command=" + arguments);
      EndpointsTool.main(arguments.toArray(new String[arguments.size()]));
    } catch (Exception ex) {
      getLog().error(ex);
      throw new MojoExecutionException("Error executing endpoints command="
          + arguments, ex);
    }
  }
  
  protected List<String> getAPIServicesClasses() {
    return new WebXmlProcessing(getLog(), webxml_source_path,
              output_directory, project,
              service_class_names).getAPIServicesClasses();
  }
}
