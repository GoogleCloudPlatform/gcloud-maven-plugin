End to End test uses Ansible playbooks to accoumplish the following:
* Grab and build the lastest gcloud-maven-plugin code from Github
* Grab and install the lastest released gcloud sdk
* Deploy a sample application via 'mvn gcloud:deploy'

#### Prerequisites
The test assumes that you have:
* GCP project
* gcloud SDK installed locally, and pointing to your GCP project
* You are logged in via gcloud (gcloud auth login)

> Note, that if the test runs from one of the VMs in your GCP project, these two steps would be configured for you automatically.

* VM named 'deploy-vm' based on Ubuntu image with wide enough scopes (see scripts/create_vm.sh)
* Pair of SSH keys to be able to ssh to 'deploy-vm' (Public key should be registered in Compute Engine metadata with the user name used by ansible)

#### Ansible roles
* ansible-playbook -i hosts --list-tasks site.yml
** 'gce' play will obtain the IP address of your 'deploy-vm' and will populate dynamic host group 'compute'
** 'compute' play will perform the end to end test on 'deploy-vm'
