#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
C_DIR="$PROJECT_ROOT/simulation"

echo "[INFO] Compiling C components..."

if [ ! -f "$C_DIR/Makefile" ]; then
  echo "[WARN] No Makefile found in $C_DIR"
  exit 0
fi

# Builds flight_simulator and flight_tester inside simulation/
make -C "$C_DIR" all

echo "[SUCCESS] C components built in $C_DIR/"
