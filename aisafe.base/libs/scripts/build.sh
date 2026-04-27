#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
REPO_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"

run_maven() {
  local win_repo

  if command -v mvn >/dev/null 2>&1; then
	mvn -f "$REPO_ROOT/pom.xml" install -DskipTests
  elif command -v mvn.cmd >/dev/null 2>&1; then
	mvn.cmd -f "$REPO_ROOT/pom.xml" install -DskipTests
  elif command -v cmd.exe >/dev/null 2>&1 && command -v wslpath >/dev/null 2>&1; then
	win_repo="$(wslpath -w "$REPO_ROOT")"
	if cmd.exe /c "where mvn" >/dev/null 2>&1; then
	  cmd.exe /c "cd /d $win_repo && mvn install -DskipTests"
	else
	  echo "[ERROR] Maven not found in Windows PATH."
	  echo "[INFO] Install Maven or run from an environment where mvn is available."
	  exit 1
	fi
  else
	echo "[ERROR] Maven not found (mvn/mvn.cmd)."
	exit 1
  fi
}

# 1. Clean workspace
"$PROJECT_ROOT/libs/scripts/clean.sh"

# 2. Build Java modules from the repository aggregator POM
echo "[INFO] Building Java components..."
run_maven

# 3. Build C components (if available in this sprint)
"$PROJECT_ROOT/libs/scripts/build_c.sh"

echo "[COMPLETE] Full system built successfully."