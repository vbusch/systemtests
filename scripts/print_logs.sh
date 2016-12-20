#!/bin/bash
function runcmd {
    echo ''
    echo "$1 : "
    ret=`$1`
    echo ''
    echo '#######################################################################'
    echo $ret
}

for pod in `oc get pods -o jsonpath='{.items[*].metadata.name}'`
do
    for container in `oc get pod $pod -o jsonpath='{.spec.containers[*].name}'`
    do
        runcmd "oc logs -c $container $pod"
        if [ "$container" == "router" ]; then
            runcmd "oc rsh -c $container $pod qdmanage query --type=address"
            runcmd "oc rsh -c $container $pod qdmanage query --type=connection"
            runcmd "oc rsh -c $container $pod qdmanage query --type=connector"
        fi
    done
done

echo "OPENSHIFT LOGS"
cat logs/os.err
cat logs/os.log
