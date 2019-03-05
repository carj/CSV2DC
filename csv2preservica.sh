#!/bin/bash

if type -p java; then
    java -cp .:lib/* CSV2Metadata $@ 
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    $JAVA_HOME/bin/java -cp .:lib/* CSV2Metadata -u preservica.properties $@
else
    echo "No Java found in Path"
fi


