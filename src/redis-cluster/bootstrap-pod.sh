#!/bin/sh
set -e

# Find which member of the Stateful Set this pod is running
# e.g. "redis-cluster-0" -> "0"
PET_ORDINAL=$(cat /etc/podinfo/pod_name | rev | cut -d- -f1)

redis-server /conf/redis.conf &
# Discover peers
wget https://storage.googleapis.com/kubernetes-release/pets/peer-finder -O /bin/peer-finder
chmod u+x /bin/peer-finder
peer-finder -on-start 'tee > /etc/podinfo/initial_peers' -service redis-cluster -ns $POD_NAMESPACE

# TODO: Wait until redis-server process is ready
sleep 1

if [ $PET_ORDINAL = "0" ]; then
  # The first member of the cluster should control all slots initially
  redis-cli cluster addslots $(seq 0 16383)
else
  # Other members of the cluster join as slaves
  # TODO: Get list of peers using the peer finder using an init container
  PEER_IP=$(perl -MSocket -e 'print inet_ntoa(scalar(gethostbyname("redis-cluster-0.redis-cluster.default.svc.cluster.local")))')
  redis-cli cluster meet $PEER_IP 6379
  sleep 1

  #echo redis-cli --csv cluster slots
  #redis-cli --csv cluster slots

  # Become the slave of a random master node
  MASTER_ID=$(redis-cli --csv cluster slots | cut -d, -f 5 | sed -e 's/^"//'  -e 's/"$//')
  redis-cli cluster replicate $MASTER_ID
fi

wait
