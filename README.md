
[![Build Status](http://104.154.86.87:8080/buildStatus/icon?job=gcloud-maven-plugin-e2e-test)](http://104.154.86.87:8080/job/gcloud-maven-plugin-e2e-test/)

# Cloud SDK App Engine Apache Maven Plugin

[Apache Maven](http://maven.apache.org/) is a software project management and comprehension tool.  It
is capable of building WAR files for deployment into [App Engine](https://cloud.google.com/appengine/). The App Engine
team provides both a plugin and Maven Archetypes for the purpose of speeding up
development.

## Requirements

To use Maven with Managed VMs, you need to do the following:

0. [Download and install](http://maven.apache.org/) Maven if you don't have it installed.

1. Install the [Google Cloud SDK](https://cloud.google.com/sdk/) on your machine. The new Maven plugin
goals will not download it automatically on build. Make sure you install an
up-to-date version (Beta or later) that has the App Engine Managed VM components
installed.

2. Install the Cloud SDK app-engine-java component:

        gcloud components update app-engine-java

3. You must use Java 7.  If you don't have Java 7, [Download](http://www.java.com/en/download/manual.jsp) and install it. For App Engine Managed VMs images, it is possible to use Java 8 with some customization.

4. Set your `JAVA_HOME` environment variable. If you are a `bash` user, the
following considerations apply:

    * For a typical Linux installation, add a line similar to the following to
your `.bashrc` file:

            export JAVA_HOME=/usr/local/tools/java/jdk1.7.0_45.jdk

   * If you use Mac OS X and the default Terminal app, your shell session
doesn't load `.bashrc` by default. So you may need to add a line similar to the
following to your `.bash_profile`:

            [ -r ~/.bashrc ] && source ~/.bashrc

   * If you use Mac OS X but don't use the default terminal app, for example, you use a
terminal management app such as tmux,  you may need to add a line similar to the
following line to your `.bashrc` file:

            export JAVA_HOME=$(/usr/libexec/java_home -v1.7)

5. If you want to start with a Java Managed VM sample code and tutorial, you can use the GitHub project: [https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras](https://github.com/GoogleCloudPlatform/appengine-java-vm-guestbook-extras)

6. The Maven plugin relies on configuration done by the cloud SDK. For example, the project name
you want to use for a deployment step can be defined with:

    gcloud config set project <your project name>

### Adding the Cloud SDK App Engine Maven plugin to an existing Maven project

To add the Cloud SDK Google App Engine Maven plugin to an existing Maven project, add the
following into the `plugins` section in the project `pom.xml` file:

    <plugin>
       <groupId>com.google.appengine</groupId>
       <artifactId>gcloud-maven-plugin</artifactId>
       <version>2.0.9.84.v20151030</version>

## Compile and build your project using Maven

To build an Java App Engine Web Application using Maven with its `pom.xml` file:

1. Change directory to the main directory for your project, for example, `guestbook/`

2. Invoke Maven as follows:

        mvn clean install

3. Wait for the project to build. When the project successfully finishes you will see a message similar to this one:

        BUILD SUCCESS
         Total time: 10.724s
         Finished at: Mon Jul 14 14:50:06 PST 2014
         Final Memory: 24M/213M

4. Optionally, run and test the application locally using the Cloud SDK
development server, as described in the next section.

## Run and test your app with the Cloud SDK development server

During the development phase, you can run and test your app at any time in
the development server by invoking the Cloud SDK App Engine Maven plugin.

To run your app in the development server:

1. Build your app if you haven't already done so:

        mvn clean install

2. Run your app by changing directory to the top level of your project (for
example, to `myapp`) and invoking Maven as follows:

        $ mvn gcloud:run

3. Wait for the the server to start and use your browser to visit `http://localhost:8080/` to access your app.

4. Shut down the app and the development server by pressing **Control+C** in the
Windows/Linux terminal window where you started it, or **CMD+C** on the Mac.


### App Engine Maven plugin goals for the Cloud SDK

The App Engine Maven plugin goals have been extended to work with a local Cloud
SDK installation that enable Managed VMs development and deployment. These goals
are listed in the following sections.

#### Development server goals

These are the Cloud SDK App Engine development server goals:

| Goal  |Description           |
| ------- |-------------|
| `gcloud:run` | Runs the Beta Cloud SDK App Engine development server for Managed VMs applications as well as non Managed VMs application.|
| `gcloud:run_start` | Performs an asynchronous start for the devserver and then returns to the command line. When this goal runs, the behavior is the same as the `run` goal except that Maven continues processing goals and exits after the server is up and running.
| `gcloud:run_stop` |  Stops the development server. Available only if you started the development server with `gcloud:run_start`.

  Available parameters for all gcloud:* goals:

| Parameter   |Description           |
| ------------|-------------|
| `gcloud_directory` | The location of the Cloud SDK to use from Maven. (Default is `~/google-cloud-sdk`)|

  Available parameters, corresponding to [gcloud app run command line flags](https://cloud.google.com/sdk/gcloud/reference/preview/app/run):

| Parameter   |Description           |
| ------------|-------------|
| `admin_host` | The host and port on which to start the admin server (in the format host:port)
| `allow_skipped_files`| Make files specified in the app.yaml "skip_files" or "static" clauses readable by the application.
| `api_host`| The host and port on which to start the API server for the dev server (in the format `host:port`).
| `appidentity_email_address`| Email address associated with a service account that has a downloadable key. May be None for no local application identity.
| `appidentity_private_key_path`| Path to private key file associated with service account (.pem format). Must be set if `appidentity_email_address` is set.
| `auth_domain`| Name of the authorization domain to use
| `blobstore_path`| Path to directory used to store blob contents (defaults to a subdirectory of `storage_path` if not set)
| `clear_datastore`| Clear the datastore on startup
| `datastore_consistency_policy` | The policy to apply when deciding whether a datastore write should appear in global queries (default is "time")
| `datastore_path` | Path to a file used to store datastore contents (defaults to a file in `storage_path` if not set)
| `default_gcs_bucket_name`| Default Google Cloud Storage bucket name
| `enable_sendmail`| Use the "sendmail" tool to transmit e-mail sent using the Mail API (ignored if `smtp_host` is set)
| `host`|The host and port on which to start the local web server (in the format host:port)
| `jvm_flag`| Additional arguments to pass to the java command when launching an instance of the app. May be specified more than once. Example: `<jvm_flag><param>-Xmx1024m</param> <param>-Xms256m</param></jvm_flag>` Note: This is not for Java Managed VMs applications. Please use a Dockerfile for that.
| `log_level`| The minimum verbosity of logs from your app that will be displayed in the terminal. (debug, info, warning, critical, error)  Defaults to current verbosity setting.
| `logs_path`| Path to a file used to store request logs (defaults to a file in `storage_path` if not set)
| `max_module_instances`| The maximum number of runtime instances that can be started for a particular module - the value can be an integer, in which case all modules are limited to that number of instances, or a comma-separated list of module:max_instances, e.g. `default:5,backend:3`
| `php_executable_path`| The full path to the PHP executable to use to run your PHP module.
| `python_startup_script`| The script to run at the startup of new Python runtime instances (useful for tools such as debuggers)
| `require_indexes`| Generate an error on datastore queries that require a composite index not found in index.yaml
| `show_mail_body`| Logs the contents of e-mails sent using the Mail API
| `smtp_allow_tls`| Allow TLS to be used when the SMTP server announces TLS support (ignored if --smtp-host is not set)
| `smtp_host`| The host and port of an SMTP server to use to transmit e-mail sent using the Mail API, in the format host:port
| `smtp_password`| Password to use when connecting to the SMTP server specified with `smtp_host`
| `smtp_user`| Username to use when connecting to the SMTP server specified with `smtp_host`
| `storage_path`| The default location for storing application data. Can be overridden for specific kinds of data using `datastore_path`, `blobstore-path`, and/or `logs_path`
| `use_mtime_file_watcher`| Use mtime polling for detecting source code changes - useful if modifying code from a remote machine using a distributed file system
| `custom_entrypoint`| Specify an entrypoint for custom runtime modules. This is required when such modules are present. Include "{port}" in the string (without quotes) to pass the port number in as an argument. For instance: `--custom_entrypoint="gunicorn -b localhost:{port} mymodule:application"`



The following example shows how to use some of these settings:

      <plugin>
        <groupId>com.google.appengine</groupId>
        <artifactId>gcloud-maven-plugin</artifactId>
        <version>>2.0.9.84.v20151030</version>
        <configuration>
          <gcloud_directory>/usr/foo/private/google-cloud-sdk</gcloud_directory>
          <verbosity>debug</verbosity>
          <version>specific_version</version>
          <set_default>true</set_default>
          <log_level>info</log_level>
          <max_module_instances>2</max_module_instances>
        </configuration>
      </plugin>


#### Application deployment goal

You can deploy an App Engine Application via the goal `gcloud:deploy`.

        $ mvn gcloud:deploy

Available parameters, corresponding to [gcloud app deploy command line flags](https://cloud.google.com/sdk/gcloud/reference/preview/app/deploy):

| Parameter   |Description           |
| ------------|-------------|
|`compile-encoding`|Set the encoding to be used when compiling Java source files (default "UTF-8")
|`delete_jsps`|Delete the JSP source files after compilation
|`disable_jar_jsps`| Do not jar the classes generated from JSPs
|`enable_jar_classes`| Jar the WEB-INF/classes content
|`enable_jar_splitting`| Split large jar files (> 32M) into smaller fragments
|`env_vars`|Environment variable overrides for your app.
|`force`|Force deploying, overriding any previous in-progress deployments to this version.
|`jar_splitting_excludes` | When `enable-jar-splitting` is specified and jar_splitting_excludes specifies a comma-separated list of suffixes, a file in a jar whose name ends with one of the suffixes will not be included in the split jar fragments
|`no_symlinks`| Do not use symbolic links when making the temporary (staging) directory used in uploading Java apps
|`retain_upload_dir`| Do not delete temporary (staging) directory used in uploading Java apps
|`server` | The App Engine server to connect to. You will not typically need to change this value.
|`set_default`| Set the deployed version to be the default serving version.
|`version`| The version of the app that will be created or replaced by this deployment.
|`docker_build`| Perform a hosted (`remote`) or `local` Docker build. To perform a local build, you must have your local docker environment configured correctly.

#### Application management goals

The application and project management goals are listed in the following
table:

Goal | Description
---- | -----------
`gcloud:module_cancel_deployment` | Cancel (rollback) the current deployment. Useful when the deployment has been interrupted for some reason.
`gcloud:module_delete` | Delete the module defined by the maven project.
`gcloud:module_set_default` | Set as default the module.
`gcloud:module_set_managed_by` | Configure the specified backend.
`gcloud:module_start` | Start the module defined by the maven project.
`gcloud:module_stop` | Stop the module defined by the maven project.

 Available parameters corresponding to [gcloud app modules command line flags](https://cloud.google.com/sdk/gcloud/reference/preview/app/modules):

| Parameter   |Description           |
| ------------|-------------|
|`server` |The App Engine server to connect to. You will not typically need to change this value.
|`version`|The version of the app that will be created or replaced by this deployment.


#### Configuration elements override via the command line interface

Instead of editing the pom.xml with the desired configuration, all the configuration parameters can be defined in the mvn command line, following the simple pattern `-Dgcloud.PARAM_NAME=PARAM_VALUE`.
For example:

      # To start the development server with debug flag:
      $ mvn gcloud:run -Dgcloud.verbosity=debug
      # To start the development server listening to 0.0.0.0 so it can be accessed outside of localhost:
      $ mvn gcloud:run -Dgcloud.host=0.0.0.0:8081
      # To specify a non default Cloud SDK installation directory:
      $ mvn gcloud:run -Dgcloud.gcloud_directory=YOUR_OWN_SPECIFIC_INSTALLATION_PATH
      # To specify the project ID and version at deployment time:
      $ mvn gcloud:deploy -Dgcloud.gcloud_project=myproject  -Dgcloud.version=myversion

Note: The configuration parameters cannot be overriden, due to this [Maven bug](https://issues.apache.org/jira/browse/MNG-4979).

#### Enterprise Archive (EAR) support

You can use the Cloud SDK Maven plugin on App Engine projects of type war and ear.
