#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
C_SOURCE="$PROJECT_ROOT/src/main/c/simulation.c"

echo "[INFO] Compiling C components..."

if [ ! -f "$C_SOURCE" ]; then
  echo "[WARN] No C source found at $C_SOURCE"
  exit 0
fi

mkdir -p "$PROJECT_ROOT/bin"
gcc "$C_SOURCE" -o "$PROJECT_ROOT/bin/simulation"
echo "[SUCCESS] C components built in $PROJECT_ROOT/bin/"
