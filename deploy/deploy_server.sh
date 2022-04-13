#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"
source $DIR/../resources/config.properties

numberOfReplicas=$[ $numberOfFaults *3 + 1]
var=1

while (( $numberOfReplicas > 0 ))
do
  osascript -e 'tell app "Terminal" to do script "cd '$DIR'/../bftb-server && mvn compile exec:java <<< '$var'"'
	numberOfReplicas=$((numberOfReplicas - 1))
	var=$((var + 1))
done
