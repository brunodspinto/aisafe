#!/bin/bash
# =============================================================================
# clean.sh
# Removes all build artifacts (Java + C)
# Usage: ./clean.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
C_SRC="$PROJECT_ROOT/src/main/c"

echo "============================================"
echo " AISafe - Cleaning build artifacts"
echo "============================================"

echo ""
echo "[1/2] Cleaning Java artifacts..."
cd "$PROJECT_ROOT"
mvn clean
echo "Java clean complete."

echo ""
echo "[2/2] Cleaning C artifacts..."
if [ -d "$C_SRC" ] && [ -f "$C_SRC/Makefile" ]; then
  cd "$C_SRC"
  make clean
  echo "C clean complete."
else
  echo "No C Makefile found. Skipping."
fi

echo ""
echo "============================================"
echo " Clean completed successfully!"
echo "============================================"
