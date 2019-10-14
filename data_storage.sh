#!/usr/bin/env bash

HOME="$( cd "$( dirname "$0" )" && pwd )"
TEST_DIR="/s/bach/g/under/evanjs/PastryTesting/"
DISCOVERY="juneau"
PORT="45467"

cd ${HOME}/build/classes/java/main;
java cs555.p2p.util.DataStorage ${DISCOVERY} ${PORT} "$@";
