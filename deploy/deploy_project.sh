#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"

osascript -e 'tell app "Terminal" to do script "cd '$DIR'/../bftb-contract && mvn clean install"' && sleep 10
/bin/bash $DIR/deploy_server.sh
/bin/bash $DIR/deploy_client.sh