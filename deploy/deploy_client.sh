#!/bin/bash
DIR="$( cd "$( dirname "$0" )" && pwd )"
source $DIR/../resources/config.properties
clients=$(echo "$numberOfClients"| tr -d $'\r' | bc)
while [ $clients -gt 0 ]
do
  osascript -e 'tell app "Terminal" to do script "cd '$DIR'/../bftb-client && mvn compile exec:java"'
	clients=$((clients - 1))
done