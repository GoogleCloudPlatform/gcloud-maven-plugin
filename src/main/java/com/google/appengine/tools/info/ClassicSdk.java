package com.google.appengine.tools.info;

import com.google.common.base.Joiner;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the SDK abstraction by the existing GAE SDK distribution, which is composed
 * of multiple jar directories for both local execution and deployment of applications.
 * @author ludo
 */
class ClassicSdk extends AppengineSdk {

  @Override
  public void includeTestingJarOnSharedPath(boolean val) {
    SdkInfo.includeTestingJarOnSharedPath(val);
  }

  @Override
  public File getToolsApiJarFile() {
    return new File(getSdkRoot() + "/lib/appengine-tools-api.jar");
  }

  @Override
  public List<URL> getAgentRuntimeLibs() {
    return SdkImplInfo.getAgentRuntimeLibs();
  }

  @Override
  public List<File> getUserJspLibFiles() {
    return SdkImplInfo.getUserJspLibFiles();
  }

  @Override
  public List<File> getUserLibFiles() {
    return SdkInfo.getUserLibFiles();
  }

  @Override
  public List<URL> getWebApiToolsLibs() {
    return SdkImplInfo.getWebApiToolLibs();
  }

  @Override
  public List<File> getSharedJspLibFiles() {
    return SdkImplInfo.getSharedJspLibFiles();
  }

  @Override
  public List<URL> getImplLibs() {
    return SdkImplInfo.getImplLibs();
  }

  @Override
  public List<File> getSharedLibFiles() {
    return SdkInfo.getSharedLibFiles();
  }

  @Override
  public List<URL> getDatanucleusLibs() {
    return SdkInfo.getOptionalToolsLib("datanucleus").getURLsForVersion("v1");
  }

  @Override
  public String getQuickStartClasspath() {
    File jettyDir = new File(getSdkRoot() + "/lib/java-managed-vm/appengine-java-vmruntime");
    File dir = new File(jettyDir, "lib");
    List<String> list = new ArrayList<>();
    list.add(new File(jettyDir, "quickstartgenerator.jar").getAbsolutePath());
    for (File f : dir.listFiles()) {
      list.add(f.getAbsolutePath());
    }
    System.out.println("LUDO");
    System.out.println("LUDO");
    System.out.println("LUDO");
    System.out.println("LUDO");
    System.out.println("LUDO");
    System.out.println("LUDO");
    System.out.println("LUDO");
    for (File f : new File(dir, "jsp").listFiles()) {
      // Filter the taglib jars we do not want by default. They should be provided by the
      // app classpath if needed.
      if (!f.getName().startsWith("taglibs")) {
        list.add(f.getAbsolutePath());
      }
    }
    for (File f : new File(dir, "jndi").listFiles()) {
      list.add(f.getAbsolutePath());
    }
    return Joiner.on(System.getProperty("path.separator")).join(list);
  }

  @Override
  public String getWebDefaultXml(String jettyVersion) {
    switch (jettyVersion) {
      case "9.2":
        return getSdkRoot() + "/lib/jetty-base-sdk/etc/webdefault.xml";
      case "9.3":
        return getSdkRoot() + "/jetty93-base/etc/webdefault.xml";
      default:
        throw new IllegalArgumentException("Invalid Jetty version: " + jettyVersion);
    }
  }

  public String getSdkRoot() {
    return SdkInfo.getSdkRoot().getAbsolutePath();
  }

  @Override
  public File getResourcesDirectory() {
    return new File(getSdkRoot(), "docs");
  }

  @Override
  public File getAgentJarFile() {
    return new File(getSdkRoot() + "/lib/agent/appengine-agent.jar");
  }

  @Override
  public File getOverridesJarFile() {
    return new File(getSdkRoot() + "/lib/override/appengine-dev-jdk-overrides.jar");
  }

  @Override
  public List<URL> getSharedLibs() {
    return SdkInfo.getSharedLibs();
  }

  @Override
  public File getLoggingProperties() {
    return SdkImplInfo.getLoggingProperties();
  }
}