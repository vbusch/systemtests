#!/bin/sh
HOST=${1:-localhost}
NAMESPACE=${2-myproject}
USER=${3-developer}

OC_CONFIG_NAME=$USER/`echo ${HOST} | sed -r 's/\./-/g'`

export OPENSHIFT_USER=$USER
export OPENSHIFT_TOKEN=`oc config view -o jsonpath="{.users[?(@.name == \"${OC_CONFIG_NAME}:8443\")].user.token}"`
export OPENSHIFT_MASTER_URL=https://${HOST}:8443
export OPENSHIFT_NAMESPACE=${NAMESPACE}
