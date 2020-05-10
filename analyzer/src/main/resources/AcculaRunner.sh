#!/bin/bash

ulimit -s hard

# Path to the fat jar
cd /home/markus/Projects/accula/analyzer/build/libs || exit

# Execute Accula Clone Detector
~/.jdks/openjdk-14.0.1/bin/java -jar analyzer-fat-1.0-SNAPSHOT.jar "$1" java 0.8 10
