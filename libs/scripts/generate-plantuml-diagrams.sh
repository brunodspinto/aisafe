#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "LOG: Generate Plantuml Diagrams"
exportFormat="svg"
extra="-SdefaultFontSize=20"

java_is_compatible() {
  local version_line version major
  version_line="$($1 -version 2>&1 | head -n 1)"
  version="${version_line#*\"}"
  version="${version%%\"*}"
  major="${version%%.*}"
  if [ "$major" = "1" ]; then
    major="${version#1.}"
    major="${major%%.*}"
  fi
  [ "$major" -ge 11 ]
}

JAVA_BIN=""
for candidate in \
  "$(command -v java 2>/dev/null || true)" \
  "${JAVA_HOME:-}/bin/java" \
  "/mnt/c/Users/jorge/.jdks/openjdk-23.0.1/bin/java.exe" \
  "/mnt/c/Users/jorge/.jdks/ms-11.0.30/bin/java.exe" \
  "/mnt/c/Program Files/JetBrains/IntelliJ IDEA 2024.3.1/jbr/bin/java.exe" \
  "/mnt/c/Users/jorge/.vscode/extensions/redhat.java-1.53.0-win32-x64/jre/21.0.10-win32-x86_64/bin/java.exe" \
  "/mnt/c/Program Files (x86)/Common Files/Oracle/Java/java8path/java.exe" \
  "/c/Program Files (x86)/Common Files/Oracle/Java/java8path/java.exe"; do
  if [ -n "$candidate" ] && [ -x "$candidate" ] && java_is_compatible "$candidate"; then
    JAVA_BIN="$candidate"
    break
  fi
done

if [ -z "$JAVA_BIN" ]; then
  echo "[ERROR] java is not available in PATH, JAVA_HOME, or the known Windows Java fallback path."
  echo "[INFO] PlantUML requires Java 11+ for libs/plantuml-1.2026.2.jar."
  echo "[INFO] Install Java 11+ or expose it to bash before running this script."
  exit 1
fi

if [[ "$JAVA_BIN" == *.exe ]] && command -v wslpath >/dev/null 2>&1; then
  PLANTUML_JAR="$(wslpath -w "$REPO_ROOT/libs/plantuml-1.2026.2.jar")"
  to_java_path() { wslpath -w "$1"; }
else
  PLANTUML_JAR="$REPO_ROOT/libs/plantuml-1.2026.2.jar"
  to_java_path() { printf '%s\n' "$1"; }
fi

if [ ! -f "$REPO_ROOT/libs/plantuml-1.2026.2.jar" ]; then
  echo "[ERROR] PlantUML jar not found at $REPO_ROOT/libs/plantuml-1.2026.2.jar"
  exit 1
fi

while IFS= read -r aFile; do
  echo "Processing file: $aFile"
  "$JAVA_BIN" -jar "$PLANTUML_JAR" $extra -t$exportFormat "$(to_java_path "$aFile")"
done < <(find "$REPO_ROOT/docs" -name "*.puml" -type f | sort)

echo "Finished"