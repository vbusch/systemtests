#!/bin/bash
function runcmd {
    echo ''
    echo "$1 : "
    $1
    echo ''
    echo '#######################################################################'
}

for i in `oc get pods | cut -f 1 -d ' ' | grep -v NAME`
do
    runcmd "oc logs $i"
    if [ "$?" -gt 0 ]
    then
        runcmd "oc logs -c restapi $i"
        runcmd "oc logs -c configserv $i"
        runcmd "oc logs -c ragent $i"
        runcmd "oc logs -c storage-controller $i"
        runcmd "oc logs -c broker $i"
        runcmd "oc logs -c router $i"
        runcmd "oc rsh -c router $i qdmanage query --type=address"
        runcmd "oc rsh -c router $i qdmanage query --type=connection"
        runcmd "oc rsh -c router $i qdmanage query --type=connector"
        runcmd "oc logs -c forwarder $i"
    fi
done

echo "OPENSHIFT LOGS"
cat logs/os.err
cat logs/os.log
