#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mvn clean verify
mkdir -p results
java -Xmx2g -jar target/assignment3-graphs-1.0.jar > results/benchmark.log
cat results/benchmark.log
python3 scripts/analyze.py
