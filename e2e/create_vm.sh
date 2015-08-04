#!/bin/sh
# TODO: change this to local ansible task (see google cloud compute module)
gcloud compute --project "maven-end-to-end" \
instances create "deploy-vm" \
--zone "us-central1-f" \
--machine-type "n1-standard-1" \
--network "default" \
--maintenance-policy "MIGRATE" \
--scopes "https://www.googleapis.com/auth/cloud-platform,\
https://www.googleapis.com/auth/userinfo.email,\
https://www.googleapis.com/auth/compute,\
https://www.googleapis.com/auth/devstorage.full_control,\
https://www.googleapis.com/auth/logging.write,\
https://www.googleapis.com/auth/cloud-platform,\
https://www.googleapis.com/auth/appengine.admin" \
--image "https://www.googleapis.com/compute/v1/projects/ubuntu-os-cloud/global/images/ubuntu-1504-vivid-v20150616a" \
--boot-disk-type "pd-standard" \
--boot-disk-device-name "deploy-vm"
