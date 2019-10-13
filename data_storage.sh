#!/usr/bin/env bash

PROJECT_DIR="Documents/Senior/CS555/Peer2Peer"
DISCOVERY="juneau"
PORT="45467"

cd ${HOME}/${PROJECT_DIR}/build/classes/java/main;
java cs555.p2p.util.DataStorage ${DISCOVERY} ${PORT} "$@";
