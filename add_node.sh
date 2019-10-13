#!/usr/bin/env bash

HOME="$( cd "$( dirname "$0" )" && pwd )"
PROJECT_DIR="Documents/Senior/CS555/Peer2Peer"
DISCOVERY="juneau"
PORT="45467"

SCRIPT="cd ${HOME}/build/classes/java/main; java cs555.p2p.node.PeerNode ${DISCOVERY} ${PORT}"

if [[ "$1" != "" ]]
	then
	HOST=$1
else
	echo "Usage [peerHost] options: [id=String] [name=String]"
	exit 1
fi

ID=""
NAME=""
if [[ "$2" != "" ]]
	then
	if [[ $2 == "id=*" ]]
		then
		ID=$2
	else [[ $2 == "name=*" ]]
		NAME=$2
	fi
fi

if [[ "$3" != "" ]]
	then
	if [[ $3 == "id=*" ]]
		then
		ID=$3
	else [[ $2 == "name=*" ]]
		NAME=$3
	fi
fi

SCRIPT+=" ${ID} ${NAME}"
gnome-terminal --geometry=150x50 -e "ssh -t ${HOST} '${SCRIPT};bash;'"