#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

run_maven() {
  local goal="$1"
  local win_project

  if command -v mvn >/dev/null 2>&1; then
	mvn -f "$PROJECT_ROOT/pom.xml" "$goal"
  elif command -v mvn.cmd >/dev/null 2>&1; then
	mvn.cmd -f "$PROJECT_ROOT/pom.xml" "$goal"
  elif command -v cmd.exe >/dev/null 2>&1 && command -v wslpath >/dev/null 2>&1; then
	win_project="$(wslpath -w "$PROJECT_ROOT")"
    if cmd.exe /c "where mvn" >/dev/null 2>&1; then
      cmd.exe /c "cd /d $win_project && mvn $goal"
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

echo "[INFO] Cleaning project..."
run_maven clean
rm -rf "${PROJECT_ROOT:?}/bin/"
if command -v make >/dev/null 2>&1 && [ -f "$PROJECT_ROOT/simulation/Makefile" ]; then
  make -C "$PROJECT_ROOT/simulation" clean
fi
echo "[SUCCESS] Project cleaned."