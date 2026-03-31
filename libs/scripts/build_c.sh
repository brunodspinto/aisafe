#!/bin/bash
# =============================================================================
# build_c.sh
# Builds all C components (simulation, flight control)
# Usage: ./build_c.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
C_SRC="$PROJECT_ROOT/src/main/c"

echo "[C Build] Building C components..."

if [ ! -d "$C_SRC" ]; then
  echo "[C Build] No C source directory found at $C_SRC. Skipping."
  exit 0
fi

cd "$C_SRC"

if [ -f "Makefile" ]; then
  make clean && make all
else
  echo "[C Build] No Makefile found. Compiling manually..."
  gcc -Wall -o simulation simulation.c -lpthread
fi

echo "[C Build] C components built successfully."
