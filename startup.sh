#!/usr/bin/env bash

HOME="$( cd "$( dirname "$0" )" && pwd )"

DISCOVERY="juneau"
PORT="45467"
#CLIENT="little-rock"
CHUNK_SERVERS_PER_MACHINE=1

gnome-terminal --geometry=132x43 -e "ssh -t ${DISCOVERY} 'cd ${HOME}/build/classes/java/main; java cs555.p2p.node.DiscoveryNode ${PORT};bash;'"
#gnome-terminal --geometry=132x43 -e "ssh -t ${CLIENT} 'cd ${HOME}/build/classes/java/main; java cs555.dfs.server.ClientServer ${SERVER} ${PORT} ${FAULT_TOLERANCE};bash;'"


SCRIPT="cd ${HOME}/build/classes/java/main; java cs555.p2p.node.PeerNode ${DISCOVERY} ${PORT}"
COMMAND="gnome-terminal --geometry=150x50"

sleep 3
mapfile -t NICKNAMES < nicknames;

nickname=0;
for i in `cat machine_list`; do
	for j in `seq 1 ${CHUNK_SERVERS_PER_MACHINE}`; do
      		echo 'logging into '${i}
      		NAME=${NICKNAMES[${nickname}]}
      		NAME="${NAME// /_}"
      		CURRENT="sleep ${nickname}; ${SCRIPT}"
       		OPTIONS='--tab -e "ssh -t '$i' '$CURRENT' '$NAME';"'
        	COMMAND+=" $OPTIONS"
        	let "nickname++"

    done
done

echo $COMMAND
eval $COMMAND &
