
# Cloud SDK App Engine Apache Maven Plugin

These are the instructions to run the Integration tests for the Gcloud plugin. The tests will build the plugin, 
then build a simple Java Application, deploy it to a App Engine project (maven-plugin-test-app.appsport.com), 
set it as the default version and run a script () that pings the servlet.

## Usage

mvn clean invoker:integration-test


The build log will be in the target directory: /target/it/gcloud-maven-plugin-test-app/build.log