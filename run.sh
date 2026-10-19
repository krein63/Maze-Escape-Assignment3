#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
javac -encoding UTF-8 -d out src/*.java
java -cp out Main
printf '\nOpen output/maze.html in your browser.\n'
