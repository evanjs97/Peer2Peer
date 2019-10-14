#!/usr/bin/env bash

HOME="$( cd "$( dirname "$0" )" && pwd )"

DISCOVERY="juneau"
PORT="45467"
PEERS_PER_MACHINE=$1
SPECIFY_IDENTIFIERS=$2

echo $HOME
gnome-terminal --geometry=132x43 -e "ssh -t ${DISCOVERY} 'cd ${HOME}/build/classes/java/main; java cs555.p2p.node.DiscoveryNode ${PORT};bash;'"
#gnome-terminal --geometry=132x43 -e "ssh -t ${CLIENT} 'cd ${HOME}/build/classes/java/main; java cs555.dfs.server.ClientServer ${SERVER} ${PORT} ${FAULT_TOLERANCE};bash;'"


SCRIPT="cd ${HOME}/build/classes/java/main; java cs555.p2p.node.PeerNode ${DISCOVERY} ${PORT}"
COMMAND="gnome-terminal --geometry=150x50"

sleep 3
mapfile -t NICKNAMES < ${HOME}/nicknames;
mapfile -t IDENTIFIERS < ${HOME}/identifiers;

nickname=0;
identifier=0;
for i in `cat ${HOME}/machine_list`; do
	for j in `seq 1 ${PEERS_PER_MACHINE}`; do
      		echo 'logging into '${i}
      		NAME=${NICKNAMES[${nickname}]}
      		NAME="${NAME// /_}"
      		ID=''
      if [[ $SPECIFY_IDENTIFIERS = true ]]
      	then
      		ID=${IDENTIFIERS[identifier]}
      		ID="id=$ID"
      fi

      CURRENT="sleep ${nickname}; ${SCRIPT}"
      OPTIONS='--tab -e "ssh -t '$i' '$CURRENT' 'name=$NAME' '$ID';"'
      COMMAND+=" $OPTIONS"
      let "nickname++"
      let "identifier++"

    done
done

echo $COMMAND
eval $COMMAND &
