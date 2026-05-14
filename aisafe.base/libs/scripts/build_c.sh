#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
C_DIR="$PROJECT_ROOT/simulation"

echo "[INFO] Compiling C components..."

if [ ! -d "$C_DIR" ]; then
  echo "[WARN] No C source directory found at $C_DIR"
  exit 0
fi

C_SOURCES=("$C_DIR"/*.c)
if [ ! -f "${C_SOURCES[0]}" ]; then
  echo "[WARN] No .c files found in $C_DIR"
  exit 0
fi

mkdir -p "$PROJECT_ROOT/bin"
gcc -Wall -Wextra -g "${C_SOURCES[@]}" -I"$C_DIR" \
    -o "$PROJECT_ROOT/bin/simulation" -lm
echo "[SUCCESS] C components built in $PROJECT_ROOT/bin/"
