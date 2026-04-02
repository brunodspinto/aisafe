#!/bin/bash
set -e
echo "[INFO] Cleaning project..."
mvn clean
rm -rf bin/
echo "[SUCCESS] Project cleaned."