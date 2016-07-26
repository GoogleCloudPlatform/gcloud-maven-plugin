/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 */
package com.google.appengine.endpoints;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * App Engine endpoints get-client-lib ... command.
 *
 * @author Ludovic Champenois ludo at google dot com
 * @goal endpoints_get_client_lib
 * @phase compile
 */
public class EndpointsGetClientLib extends EndpointsMojo {

  /**
   * The directory for the generated Maven client lib projects.
   *
   * @parameter expression="${client_libs_directory}"
   *     default-value="${project.build.directory}/endpoints-client-libs"
   */
  protected String client_libs_directory;

  @Override
  protected ArrayList<String> collectParameters(String command) {
    ArrayList<String> arguments = new ArrayList<>();
    arguments.add(command);
    handleClassPath(arguments);

    if (output_directory != null && !output_directory.isEmpty()) {
      arguments.add("-o");
      arguments.add(output_directory + "/WEB-INF");
      new File(output_directory).mkdirs();
    }
    arguments.add("-w");
    arguments.add(output_directory);
    arguments.add("-l");
    arguments.add("java");
    if (build_system != null && !build_system.isEmpty()) {
      arguments.add("-bs");
      arguments.add(build_system);
    }
    return arguments;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("");
    getLog().info("Google App Engine Java SDK - Generate endpoints get client lib");

    List<String> classNames = getAPIServicesClasses();
    if (classNames.isEmpty()) {
      getLog().info("No Endpoints classes detected.");
      return;
    }

    try {
      executeEndpointsCommand("get-client-lib", new String[0],
          classNames.toArray(new String[classNames.size()]));
      File webInf = new File(output_directory + "/WEB-INF");
      if (webInf.exists() && webInf.isDirectory()) {
        File[] files = webInf.listFiles(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.endsWith("-java.zip");
          }
        });
        File mavenProjectsDir = new File(client_libs_directory);
        mavenProjectsDir.mkdirs();
        for (File source : files) {
          File pomFile = unjarMavenProject(source, mavenProjectsDir);
          if (pomFile != null) {
            getLog().info("BUILDING Endpoints Client Library from: " + pomFile);
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(pomFile);
            request.setGoals(Collections.singletonList("install"));
            Invoker invoker = new DefaultInvoker();
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
              throw new IllegalStateException("Build failed.");
            }
            getLog().info("Endpoint get client lib generation and compilation done.");

          }
        }
      }
    } catch (MojoExecutionException e) {
      getLog().error(e);
      throw new MojoExecutionException(
          "Error while generating Google App Engine endpoint get client lib", e);
    } catch (MavenInvocationException ex) {
      Logger.getLogger(EndpointsGetClientLib.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /*
  * Un-jar the jar file, and potentially, returns the pom.xml file if it exists 
  * in the jar file.
  */
  private File unjarMavenProject(File jar, File destdir) {
    File pomFile = null;
    JarFile jarfile;
    try {
      jarfile = new JarFile(jar);
    } catch (IOException ex) {
      Logger.getLogger(EndpointsGetClientLib.class.getName()).log(Level.SEVERE, null, ex);
      return pomFile;
    }

    Enumeration<JarEntry> enu = jarfile.entries();
    while (enu.hasMoreElements()) {
      InputStream is = null;
      try {
        JarEntry je = enu.nextElement();
        File fl = new File(destdir, je.getName());
        if (fl.getName().equals("pom.xml")) {
          pomFile = fl;
        }
        if (!fl.exists()) {
          fl.getParentFile().mkdirs();
          fl = new java.io.File(destdir, je.getName());
        }
        if (je.isDirectory()) {
          continue;
        }
        is = jarfile.getInputStream(je);
        FileOutputStream fo = new FileOutputStream(fl);
        while (is.available() > 0) {
          fo.write(is.read());
        }
      } catch (IOException ex) {
        Logger.getLogger(EndpointsGetClientLib.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
        try {
          if (is != null) {
            is.close();
          }
        } catch (IOException ex) {
          Logger.getLogger(EndpointsGetClientLib.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    return pomFile;
  }
}
