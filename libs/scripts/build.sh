#!/bin/bash
# =============================================================================
# build.sh
# Builds the entire project (Java + C components)
# Usage: ./build.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "============================================"
echo " AISafe - Building all components"
echo "============================================"

# Build Java (Maven)
echo ""
echo "[1/2] Building Java components..."
cd "$PROJECT_ROOT"
mvn clean install -DskipTests
echo "Java build complete."

# Build C components
echo ""
echo "[2/2] Building C components..."
bash "$SCRIPT_DIR/build_c.sh"

echo ""
echo "============================================"
echo " Build completed successfully!"
echo "============================================"
