#!/bin/bash
DIR="$( cd "$( dirname "$0" )" && pwd )"
source $DIR/../resources/config.properties
declare -i x=3
declare -i y=1
numberOfReplicas=$(echo "$numberOfFaults * $x + $y"| tr -d $'\r' | bc)
declare -i var=1

while [ $numberOfReplicas -gt 0 ]
do
  osascript -e 'tell app "Terminal" to do script "cd '$DIR'/../bftb-server && mvn compile exec:java <<< '$var'"'
	numberOfReplicas=$((numberOfReplicas - 1))
	var=$((var + 1))
done
