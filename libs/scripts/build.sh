#!/bin/bash
set -e

# 1. Limpa tudo
./libs/scripts/clean.sh

# 2. Compila Java (Maven)
echo "[INFO] Building Java components..."
mvn install -DskipTests

# 3. Compila C
./libs/scripts/build_c.sh

echo "[COMPLETE] Full system built successfully."