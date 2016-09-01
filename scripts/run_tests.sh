#!/bin/sh
DIR=`dirname $0`
set -x

oc login -u developer -p developer --insecure-skip-tls-verify=true https://localhost:8443
oc delete project enmasse-ci

oc new-project enmasse-ci
oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):deployer
curl -s https://raw.githubusercontent.com/enmasseproject/openshift-configuration/master/enmasse-template.yaml | oc process -f - | oc create -f -
$DIR/wait_until_up.sh 6
$DIR/scale_controller.sh mytopic 2
$DIR/wait_until_up.sh 7

sleep 120

OPENSHIFT_USER=developer OPENSHIFT_TOKEN=`oc config view -o jsonpath='{.users[?(@.name == "developer/localhost:8443")].user.token}'` OPENSHIFT_URL=https://localhost:8443 PN_TRACE_FRM=1 gradle check -i
