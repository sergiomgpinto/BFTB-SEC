#!/bin/bash
DIR="$( cd "$( dirname "$0" )" && pwd )"

osascript -e 'tell app "Terminal" to do script "cd '$DIR'/../ && mvn flyway:clean && mvn flyway:migrate &&
cd bftb-contract && mvn clean install"' && sleep 22
<<comment
  Sleep of 22 seconds to allow dependencies to be installed and database
  to be cleaned before starting the application.
comment
/bin/bash $DIR/deploy_server.sh
/bin/bash $DIR/deploy_client.sh