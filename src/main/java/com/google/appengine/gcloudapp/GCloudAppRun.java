/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 */
package com.google.appengine.gcloudapp;

import com.google.appengine.repackaged.com.google.api.client.util.Throwables;
import com.google.appengine.repackaged.com.google.common.io.ByteStreams;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the App Engine development server.
 *
 * @author Ludo
 * @goal run
 * @execute phase="package"
 * @threadSafe false
 */
public class GCloudAppRun extends AbstractGcloudMojo {

  /**
   * The host and port on which to start the API server (in the format
   * host:port)
   *
   * @parameter expression="${gcloud.api_host}"
   */
  private String api_host;

  /**
   * Additional directories containing App Engine modules to be run.
   *
   * @parameter
   */
  private List<String> modules;

  /**
   * The host and port on which to start the local web server (in the format
   * host:port)
   *
   * @parameter expression="${gcloud.host}"
   */
  private String host;

  /**
   * The host and port on which to start the admin server (in the format
   * host:port)
   *
   * @parameter expression="${gcloud.admin_host}"
   */
  private String admin_host;

  /**
   * The default location for storing application data. Can be overridden for
   * specific kinds of data using --datastore-path, --blobstore-path, and/or
   * --logs-path
   *
   * @parameter expression="${gcloud.storage_path}"
   */
  private String storage_path;

  /**
   * The minimum verbosity of logs from your app that will be displayed in the
   * terminal. (debug, info, warning, critical, error) Defaults to current
   * verbosity setting.
   *
   * @parameter expression="${gcloud.log_level}"
   */
  private String log_level;
  /**
   * Path to a file used to store request logs (defaults to a file in
   * --storage-path if not set)
   *
   * @parameter expression="${gcloud.logs_path}"
   */
  private String logs_path;
  /**
   * name of the authorization domain to use (default: gmail.com)
   *
   * @parameter expression="${gcloud.auth_domain}"
   */
  private String auth_domain;

  /**
   * the maximum number of runtime instances that can be started for a
   * particular module - the value can be an integer, in what case all modules
   * are limited to that number of instances or a comma-separated list of
   * module:max_instances e.g. "default:5,backend:3" (default: None)
   *
   * @parameter expression="${gcloud.max_module_instances}"
   */
  private String max_module_instances;

  /**
   * email address associated with a service account that has a downloadable
   * key. May be None for no local application identity. (default: None)
   *
   * @parameter expression="${gcloud.appidentity_email_address}"
   */
  private String appidentity_email_address;

  /**
   * path to private key file associated with service account (.pem format).
   * Must be set if appidentity_email_address is set. (default: None)
   *
   *
   * @parameter expression="${gcloud.appidentity_private_key_path}"
   */
  private String appidentity_private_key_path;

  /**
   * path to gcloud_directory used to store blob contents (defaults to a subdirectory
 of --storage_path if not set) (default: None)
   *
   * @parameter expression="${gcloud.blobstore_path}"
   */
  private String blobstore_path;

  /**
   * path to a file used to store datastore contents (defaults to a file in
   * --storage_path if not set) (default: None)
   *
   * @parameter expression="${gcloud.datastore_path}"
   */
  private String datastore_path;
  /**
   * clear the datastore on startup (default: False)
   *
   *
   * @parameter expression="${gcloud.clear_datastore}"
   */
  private boolean clear_datastore;

  /**
   * make files specified in the app.yaml "skip_files" or "static" handles
   * readable by the application. (default: False)
   *
   * @parameter expression="${gcloud.allow_skipped_files}"
   */
  private boolean allow_skipped_files;

  /**
   * Enable logs collection and display in local Admin Console for Managed VM
   * modules.
   *
   * @parameter expression="${gcloud.enable_mvm_logs}"
   */
  private boolean enable_mvm_logs;

  /**
   * Use the "sendmail" tool to transmit e-mail sent using the Mail API (ignored
   * if --smtp-host is set)
   *
   * @parameter expression="${gcloud.enable_sendmail}"
   */
  private boolean enable_sendmail;
  /**
   * Use mtime polling for detecting source code changes - useful if modifying
   * code from a remote machine using a distributed file system
   *
   * @parameter expression="${gcloud.use_mtime_file_watcher}"
   */
  private boolean use_mtime_file_watcher;
  /**
   * JVM_FLAG Additional arguments to pass to the java command when launching an
   * instance of the app. May be specified more than once. Example:
   * &lt;jvm_flag&gt; &lt;param&gt;-Xmx1024m&lt;/param&gt;
   * &lt;param&gt;-Xms256m&lt;/param&gt; &lt;/jvm_flag&gt; Note: This
   * is not for Java Managed VMs applications. Please use a Dockerfile for that.
   *
   * @parameter
   */
  private List<String> jvm_flag;

  /**
   * default Google Cloud Storage bucket name (default: None)
   *
   * @parameter expression="${gcloud.default_gcs_bucket_name}"
   */
  private String default_gcs_bucket_name;
  /**
   * enable_cloud_datastore
   *
   * @parameter expression="${gcloud.enable_cloud_datastore}"
   */
  private boolean enable_cloud_datastore;

  /**
   * datastore_consistency_policy The policy to apply when deciding whether a
   * datastore write should appear in global queries (default="time")
   *
   * @parameter expression="${gcloud.datastore_consistency_policy}"
   */
  private String datastore_consistency_policy;

  /**
   * The full path to the PHP executable to use to run your PHP module
   *
   * @parameter expression="${gcloud.php_executable_path}"
   */
  private String php_executable_path;
  /**
   * The script to run at the startup of new Python runtime instances (useful
   * for tools such as debuggers)
   *
   * @parameter expression="${gcloud.python_startup_script}"
   */
  private String python_startup_script;
  /**
   * Generate an error on datastore queries that require a composite index not
   * found in index.yaml
   *
   * @parameter expression="${gcloud.require_indexes}"
   */
  private boolean require_indexes;
  /**
   * Logs the contents of e-mails sent using the Mail API
   *
   * @parameter expression="${gcloud.show_mail_body}"
   */
  private boolean show_mail_body;
  /**
   * Allow TLS to be used when the SMTP server announces TLS support (ignored if
   * --smtp-host is not set)
   *
   * @parameter expression="${gcloud.smtp_allow_tls}"
   */
  private boolean smtp_allow_tls;
  /**
   * The host and port of an SMTP server to use to transmit e-mail sent using
   * the Mail API, in the format host:port
   *
   * @parameter expression="${gcloud.smtp_host}"
   */
  private String smtp_host;
  /**
   * Password to use when connecting to the SMTP server specified with
   * --smtp-host
   *
   * @parameter expression="${gcloud.smtp_password}"
   */
  private String smtp_password;
  /**
   * Username to use when connecting to the SMTP server specified with
   * --smtp-host
   *
   * @parameter expression="${gcloud.smtp_user}"
   */
  private String smtp_user;

  /**
   * The location of the appengine application to run.
   *
   * @parameter expression="${gcloud.application_directory}"
   */
  protected String application_directory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("");
    if (application_directory == null) {
      application_directory = maven_project.getBuild().getDirectory() + "/" + maven_project.getBuild().getFinalName();
    }
    File appDirFile = new File(application_directory);
    if (!appDirFile.exists()) {
      throw new MojoExecutionException("The application directory does not exist : " + application_directory);
    }
    if (!appDirFile.isDirectory()) {
      throw new MojoExecutionException("The application directory is not a directory : " + application_directory);
    }
    //Just before starting, just to make sure, shut down any running devserver on this port.
    stopDevAppServer();

    ArrayList<String> devAppServerCommand = getCommand(application_directory);
    startCommand(appDirFile, devAppServerCommand, WaitDirective.WAIT_SERVER_STOPPED);
  }

  @Override
  protected ArrayList<String> getCommand(String appDir) throws MojoExecutionException {

    getLog().info("Running gcloud app run...");

    ArrayList<String> devAppServerCommand = new ArrayList<>();
    setupInitialCommands(devAppServerCommand);

    devAppServerCommand.add("run");

    File f = new File(appDir, "WEB-INF/appengine-web.xml");
    if (!f.exists()) { // EAR project possibly, add all modules one by one:
      File ear = new File(appDir);
      for (File w : ear.listFiles()) {
        if (new File(w, "WEB-INF/appengine-web.xml").exists()) {
          devAppServerCommand.add(w.getAbsolutePath());

        }
      }

    } else {
      // Point to our application
      devAppServerCommand.add(appDir);
    }

    if ((modules != null) && !modules.isEmpty()) {
      for (String modDir : modules) {
        getLog().info("Running gcloud app run with extra module in " + modDir);
        devAppServerCommand.add(modDir);

      }

    }
    setupExtraCommands(devAppServerCommand);

    // Add in additional options for starting the DevAppServer
    if (admin_host != null) {
      devAppServerCommand.add("--admin-host=" + admin_host);
    }
    if (api_host != null) {
      devAppServerCommand.add("--api-host=" + api_host);
    }

    if (storage_path != null) {
      devAppServerCommand.add("--storage-path=" + storage_path);
    }
    if (host != null) {
      devAppServerCommand.add("--host=" + host);
    }
    if (admin_host != null) {
      devAppServerCommand.add("--admin-host=" + admin_host);
    }
    if (storage_path != null) {
      devAppServerCommand.add("--storage-path=" + storage_path);
    }
    if (log_level != null) {
      devAppServerCommand.add("--log-level=" + log_level);
    }
    if (logs_path != null) {
      devAppServerCommand.add("--logs-path=" + logs_path);
    }
    if (auth_domain != null) {
      devAppServerCommand.add("--auth-domain=" + auth_domain);
    }
    if (max_module_instances != null) {
      devAppServerCommand.add("--max-module-instances=" + max_module_instances);
    }
    if (appidentity_email_address != null) {
      devAppServerCommand.add("--appidentity-email-address=" + appidentity_email_address);
    }

    if (appidentity_private_key_path != null) {
      devAppServerCommand.add("--appidentity-private-key-path=" + appidentity_private_key_path);
    }
    if (blobstore_path != null) {
      devAppServerCommand.add("--blobstore-path=" + blobstore_path);
    }
    if (datastore_path != null) {
      devAppServerCommand.add("--datastore-path=" + datastore_path);
    }

    if (clear_datastore) {
      devAppServerCommand.add("--clear-datastore");
    }
    if (allow_skipped_files) {
      devAppServerCommand.add("--allow-skipped-files");
    }
    if (enable_mvm_logs) {
      devAppServerCommand.add("--enable-mvm-logs");
    }
    if (enable_sendmail) {
      devAppServerCommand.add("--enable-sendmail");
    }
    if (use_mtime_file_watcher) {
      devAppServerCommand.add("--use-mtime-file-watcher");
    }
    if ((jvm_flag != null) && !jvm_flag.isEmpty()) {
      for (String opt : jvm_flag) {
        devAppServerCommand.add("--jvm-flag=" + opt);
      }
    }
    if (default_gcs_bucket_name != null) {
      devAppServerCommand.add("--default-gcs-bucket-name=" + default_gcs_bucket_name);
    }
    if (enable_cloud_datastore) {
      devAppServerCommand.add("--enable-cloud-datastore");
    }
    if (datastore_consistency_policy != null) {
      devAppServerCommand.add("--datastore-consistency-policy=" + datastore_consistency_policy);
    }
    if (php_executable_path != null) {
      devAppServerCommand.add("--php-executable-path=" + php_executable_path);
    }
    if (python_startup_script != null) {
      devAppServerCommand.add("--python-startup-script=" + python_startup_script);
    }
    if (require_indexes) {
      devAppServerCommand.add("--require-indexes");
    }
    if (show_mail_body) {
      devAppServerCommand.add("--show-mail-body");
    }
    if (smtp_allow_tls) {
      devAppServerCommand.add("--smtp-allow-tls");
    }
    if (smtp_host != null) {
      devAppServerCommand.add("--smtp-host=" + smtp_host);
    }
    if (smtp_password != null) {
      devAppServerCommand.add("--smtp-password=" + smtp_password);
    }
    if (smtp_user != null) {
      devAppServerCommand.add("--smtp-user=" + smtp_user);
    }
    return devAppServerCommand;
  }

  protected void stopDevAppServer() throws MojoExecutionException {
    HttpURLConnection connection;
    try {
      String ad = "localhost";
      if (host != null) {
        String[] parts = host.split(":");
        ad = parts[0];
      }
      URL url = new URL("http", ad, 8000, "/quit");
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setDoInput(true);
      connection.setRequestMethod("GET");
      //     connection.getOutputStream().write(110);
      ByteStreams.toByteArray(connection.getInputStream());
//      connection.getOutputStream().flush();
//      connection.getOutputStream().close();
//      connection.getInputStream().close();
      connection.disconnect();

      getLog().info("Shutting down Cloud SDK Server on port " + 8000 +
              " and waiting 4 seconds...");
      Thread.sleep(4000);
    } catch (MalformedURLException e) {
      throw new MojoExecutionException("URL malformed attempting to stop the devserver : " + e.getMessage());
    } catch (IOException e) {
      getLog().debug("Was not able to contact the devappserver to shut it down.  Most likely this is due to it simply not running anymore. ", e);
    } catch (InterruptedException e) {
      Throwables.propagate(e);
    }
  }

}
