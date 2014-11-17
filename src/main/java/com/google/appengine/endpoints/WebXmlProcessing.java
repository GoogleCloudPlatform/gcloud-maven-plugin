/**
 * Copyright 2013 Google Inc. All Rights Reserved.
 */

package com.google.appengine.endpoints;

import com.google.api.server.spi.config.Api;
import com.google.appengine.repackaged.com.google.common.io.Files;
import com.google.common.base.Joiner;
import eu.infomas.annotation.AnnotationDetector;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Process Endpoints annotations and change web.xml accordingly.
 *
 */
public class WebXmlProcessing {

  Log log;
  String webXmlSourcePath;
  String outputDirectory;
  MavenProject project;
  String userSpecifiedServiceClassNames;


  public WebXmlProcessing(Log log, String webXmlSourcePath,
          String outputDirectory, MavenProject project,
          String userSpecifiedServiceClassNames) {
    this.log = log;
    this.webXmlSourcePath = webXmlSourcePath;
    this.outputDirectory = outputDirectory;
    this.project = project;
    this.userSpecifiedServiceClassNames = userSpecifiedServiceClassNames;

  }

  private Log getLog() {
    return log;
  }

  public List<String> getAPIServicesClasses() {
    List<String> classes;
    if (userSpecifiedServiceClassNames != null) {
          classes = Arrays.asList(userSpecifiedServiceClassNames.split(","));
    } else {
          ApiReporter reporter = new ApiReporter();
          String targetDir = project.getBuild().getOutputDirectory();

          final AnnotationDetector cf = new AnnotationDetector(reporter);
          try {
            cf.detect(new File(targetDir));
          } catch (IOException ex) {
            getLog().info(ex);
          }
          classes = reporter.getClasses();
    }
    XmlUtil util = new XmlUtil();
    try {
      util.updateWebXml(classes, webXmlSourcePath);
    } catch (Exception ex) {
      getLog().info("Error: " + ex);
    }
    return classes;
  }

  class ApiReporter implements AnnotationDetector.TypeReporter {

    private List<String> classes = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Annotation>[] annotations() {
      return new Class[]{Api.class};
    }

    @Override
    public void reportTypeAnnotation(Class<? extends Annotation> annotation, String className) {
      classes.add(className);

    }

    public List<String> getClasses() {
      return classes;
    }
  }

  /**
   * Xml Manipulation Utility Class for modifying web.xml for generated APIs.
   */
  class XmlUtil {

    private static final String COMMA = ",";
    private static final String WEB_APP = "web-app";
    private static final String INIT_PARAM = "init-param";
    private static final String SERVLET = "servlet";
    private static final String SERVLET_NAME = "servlet-name";
    private static final String SERVLET_MAPPING = "servlet-mapping";
    private static final String SERVLET_CLASS = "servlet-class";
    private static final String URL_PATTERN = "url-pattern";
    private static final String SPI_URL_PATTERN = "/_ah/spi/*";
    private static final String PARAM_NAME = "param-name";
    private static final String PARAM_VALUE = "param-value";
    private static final String SERVICES = "services";
    private static final String SYSTEM_SERVICE_SERVLET = "SystemServiceServlet";
    private static final String SYSTEM_SERVICE_SERVLET_CLASS = 
            "com.google.api.server.spi.SystemServiceServlet";

    /**
     * Finds the WebApp node in web.xml document. Then tries to find
     * SystemServiceServlet node and returns it. If not found, returns WebApp
     * node. The returned type is of type Element
     *
     * @return SystemServiceServlet node if found, else WebApp node.
     */
    private Node findSystemServiceServlet(Document doc) {
      Node webAppNode;
      for (webAppNode = doc.getFirstChild(); webAppNode != null;
              webAppNode = webAppNode.getNextSibling()) {

        if (isElementAndNamed(webAppNode, WEB_APP)) {
          break;
        }
      }
      if (webAppNode == null) {
        getLog().info("Not a valid web.xml document");
        return null;
      }

      Node systemServiceServletNode;
      for (systemServiceServletNode = webAppNode.getFirstChild();
              systemServiceServletNode != null;
              systemServiceServletNode = systemServiceServletNode.getNextSibling()) {
        if (isElementAndNamed(systemServiceServletNode, SERVLET)) {
          for (Node n3 = systemServiceServletNode.getFirstChild();
                  n3 != null; n3 = n3.getNextSibling()) {
            if (isElementAndNamed(n3, SERVLET_NAME)
                    && n3.getTextContent().equals(SYSTEM_SERVICE_SERVLET)) {

              return systemServiceServletNode;
            }
          }
        }
      }
      return webAppNode;
    }

    /**
     * Insert a SystemServiceServlet node in web.xml inside webApp node.
     *
     * @return The inserted SystemServiceServlet node.
     */
    private Node insertSystemServiceServlet(Document doc, Node webAppNode, String spc,
            String delimiter) {
      Node n2, n3, n4, n5;
      n5 = doc.createTextNode(spc);
      webAppNode.appendChild(n5);
      n2 = doc.createElement(SERVLET);
      webAppNode.appendChild(n2);
      n5 = doc.createTextNode(delimiter + spc);
      webAppNode.appendChild(n5);
      n3 = doc.createElement(SERVLET_MAPPING);
      webAppNode.appendChild(n3);
      n5 = doc.createTextNode(delimiter);
      webAppNode.appendChild(n5);

      n5 = doc.createTextNode("\n" + spc + spc);
      n2.appendChild(n5);
      n5 = doc.createElement(SERVLET_NAME);
      n5.setTextContent(SYSTEM_SERVICE_SERVLET);
      n2.appendChild(n5);
      n5 = doc.createTextNode("\n" + spc + spc);
      n2.appendChild(n5);
      n5 = doc.createElement(SERVLET_CLASS);
      n5.setTextContent(SYSTEM_SERVICE_SERVLET_CLASS);
      n2.appendChild(n5);
      n5 = doc.createTextNode("\n" + spc + spc);
      n2.appendChild(n5);
      n4 = doc.createElement(INIT_PARAM);
      n2.appendChild(n4);
      n5 = doc.createTextNode("\n" + spc);
      n2.appendChild(n5);

      n5 = doc.createTextNode("\n" + spc + spc);
      n3.appendChild(n5);
      n5 = doc.createElement(SERVLET_NAME);
      n5.setTextContent(SYSTEM_SERVICE_SERVLET);
      n3.appendChild(n5);
      n5 = doc.createTextNode("\n" + spc + spc);
      n3.appendChild(n5);
      n5 = doc.createElement(URL_PATTERN);
      n5.setTextContent(SPI_URL_PATTERN);
      n3.appendChild(n5);
      n5 = doc.createTextNode("\n" + spc);
      n3.appendChild(n5);

      n5 = doc.createTextNode("\n" + spc + spc + spc);
      n4.appendChild(n5);
      n5 = doc.createElement(PARAM_NAME);
      n5.setTextContent(SERVICES);
      n4.appendChild(n5);
      n5 = doc.createTextNode("\n" + spc + spc + spc);
      n4.appendChild(n5);
      n5 = doc.createElement(PARAM_VALUE);
      n5.setTextContent("");
      n4.appendChild(n5);
      n5 = doc.createTextNode("\n" + spc + spc);
      n4.appendChild(n5);

      return n2;
    }

    /**
     * Checks if a node is an XML element and checks if it has a specific name
     *
     * @param node
     * @param name
     * @return true if matching element, false if name doesn't match OR if node
     * type isn't ELEMENT
     */
    private boolean isElementAndNamed(Node node, String name) {
      if (node == null || name == null) {
        throw new IllegalArgumentException();
      }
      return (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name));
    }

    private void saveFile(Document doc, String filePath)
            throws TransformerFactoryConfigurationError, TransformerException,
            IOException {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new DOMSource(doc), new StreamResult(new File(filePath)));
    }

    /**
     * Update the SystemServiceServlet parameter in web.xml, it doesn't make
     * changes if nothing new is to be added. If changes are required, it will
     * modify the file and save
     *
     * @param document
     * @param systemServiceServletNode
     * @param services
     * @return
     */
    private boolean updateSystemServiceServletParam(Document doc,
            Node systemServiceServletNode, List<String> services) {
      Node initParamNode;
      for (initParamNode = systemServiceServletNode.getFirstChild();
              initParamNode != null;
              initParamNode = initParamNode.getNextSibling()) {
        if (isElementAndNamed(initParamNode, INIT_PARAM)) {
          break;
        }
      }
      if (initParamNode == null) {
        getLog().info("Not a valid web.xml document");
        return false;
      }

      Node paramValueNode;
      for (paramValueNode = initParamNode.getFirstChild();
              paramValueNode != null;
              paramValueNode = paramValueNode.getNextSibling()) {
        if (isElementAndNamed(paramValueNode, PARAM_VALUE)) {
          break;
        }
      }
      if (paramValueNode == null) {
        getLog().info("Not a valid web.xml document");
        return false;
      }

      // get all services the file currently lists,
      // put it in a treeset for sorted order, also removes duplicates
      String serviceXMLString = paramValueNode.getTextContent();
      Set<String> servicesOnFile = new TreeSet<String>();
      if (serviceXMLString != null && !serviceXMLString.trim().isEmpty()) {
        String[] servicesArray = serviceXMLString.split(",");
        for (String s : servicesArray) {
          servicesOnFile.add(s.trim());
        }
      }

      // find all services we need to remove
      List<String> servicesToRemove = new ArrayList<String>();
      for (String s : servicesOnFile) {
        if (!services.contains(s)) {
          servicesToRemove.add(s);
        }
      }

      // find all services we need to add
      List<String> servicesToAdd = new ArrayList<String>();
      for (String s : services) {
        if (!servicesOnFile.contains(s)) {
          servicesToAdd.add(s);
        }
      }

      // if we don't need to make any changes, then return false
      if (servicesToAdd.isEmpty() && servicesToRemove.isEmpty()) {
        return false;
      }

      // remove those marked for removal
      for (String s : servicesToRemove) {
        servicesOnFile.remove(s);
      }

      // add those marked for adding
      for (String s : servicesToAdd) {
        servicesOnFile.add(s);
      }

      // write the appropriate data to the file
      if (servicesOnFile.isEmpty()) {
        paramValueNode.setTextContent("");
      } else {
        Joiner joiner = Joiner.on(COMMA);
        paramValueNode.setTextContent(joiner.join(servicesOnFile));
      }

      // indicate that a save is required
      return true;
    }

    public void updateWebXml(List<String> services, String webXmlPath)
            throws ParserConfigurationException, SAXException, IOException,
            TransformerFactoryConfigurationError, TransformerException {
      boolean saveRequired;

      String spc = " ";
      String delimiter = "\n";

      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document document = docBuilder.parse(webXmlPath);

      Node systemServiceServletNode = findSystemServiceServlet(document);
      if (systemServiceServletNode == null) {
        getLog().info("Not a valid web.xml document");
        return;
      }
      if (isElementAndNamed(systemServiceServletNode, WEB_APP)) {
        systemServiceServletNode = insertSystemServiceServlet(document,
                systemServiceServletNode, spc, delimiter);
        saveRequired = true;
      }

      saveRequired = updateSystemServiceServletParam(document,
              systemServiceServletNode, services);
      String generatedWebInf = outputDirectory + "/WEB-INF";      
      new File(generatedWebInf).mkdirs();
      saveFile(document, generatedWebInf + "/web.xml");
      Files.copy(
              new File(new File(webXmlPath).getParentFile(), "appengine-web.xml"),
              new File(generatedWebInf, "appengine-web.xml"));
    }
  }
}
