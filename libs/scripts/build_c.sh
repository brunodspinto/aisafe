#!/bin/bash
set -e
echo "[INFO] Compiling C components..."
mkdir -p bin
gcc src/main/c/simulation.c -o bin/simulation
echo "[SUCCESS] C components built in bin/"