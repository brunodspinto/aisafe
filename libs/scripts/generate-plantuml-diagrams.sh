#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOCS_ROOT="$REPO_ROOT/docs"

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

detect_os() {
  case "$(uname -s)" in
    Darwin) echo "macos" ;;
    Linux)
      if grep -qi microsoft /proc/version 2>/dev/null; then
        echo "wsl"
      else
        echo "linux"
      fi
      ;;
    CYGWIN*|MINGW*|MSYS*) echo "windows" ;;
    *) echo "unknown" ;;
  esac
}

OS="$(detect_os)"
echo "LOG: Detected OS: $OS"

java_candidates=()
java_candidates+=("$(command -v java 2>/dev/null || true)")
[ -n "${JAVA_HOME:-}" ] && java_candidates+=("$JAVA_HOME/bin/java")

case "$OS" in
  macos)
    java_candidates+=("/usr/local/opt/openjdk/bin/java")
    java_candidates+=("/opt/homebrew/opt/openjdk/bin/java")
    java_candidates+=("/usr/bin/java")
    if [ -d "$HOME/.sdkman/candidates/java" ]; then
      while IFS= read -r j; do
        java_candidates+=("$j")
      done < <(find "$HOME/.sdkman/candidates/java" -name "java" -path "*/bin/java" | sort -rV)
    fi
    ;;
  linux)
    java_candidates+=("/usr/bin/java")
    java_candidates+=("/usr/lib/jvm/default-java/bin/java")
    java_candidates+=("/usr/lib/jvm/java-21-openjdk-amd64/bin/java")
    java_candidates+=("/usr/lib/jvm/java-17-openjdk-amd64/bin/java")
    java_candidates+=("/usr/lib/jvm/java-11-openjdk-amd64/bin/java")
    if [ -d "$HOME/.sdkman/candidates/java" ]; then
      while IFS= read -r j; do
        java_candidates+=("$j")
      done < <(find "$HOME/.sdkman/candidates/java" -name "java" -path "*/bin/java" | sort -rV)
    fi
    ;;
  wsl)
    java_candidates+=("/usr/bin/java")
    java_candidates+=("/usr/lib/jvm/default-java/bin/java")
    if command -v wslpath >/dev/null 2>&1; then
      win_user="$(cmd.exe /c "echo %USERNAME%" 2>/dev/null | tr -d '\r')"
      if [ -n "$win_user" ]; then
        java_candidates+=("/mnt/c/Users/$win_user/.jdks/openjdk-23/bin/java.exe")
        java_candidates+=("/mnt/c/Users/$win_user/.jdks/ms-11/bin/java.exe")
        java_candidates+=("/mnt/c/Program Files/Eclipse Adoptium/jdk-21/bin/java.exe")
        java_candidates+=("/mnt/c/Program Files/Eclipse Adoptium/jdk-17/bin/java.exe")
        java_candidates+=("/mnt/c/Program Files/Eclipse Adoptium/jdk-11/bin/java.exe")
      fi
    fi
    ;;
  windows)
    java_candidates+=("/c/Program Files/Eclipse Adoptium/jdk-21/bin/java.exe")
    java_candidates+=("/c/Program Files/Eclipse Adoptium/jdk-17/bin/java.exe")
    java_candidates+=("/c/Program Files/Eclipse Adoptium/jdk-11/bin/java.exe")
    java_candidates+=("/c/Program Files/Java/jdk-21/bin/java.exe")
    java_candidates+=("/c/Program Files/Java/jdk-17/bin/java.exe")
    java_candidates+=("/c/Program Files/Java/jdk-11/bin/java.exe")
    ;;
esac

JAVA_BIN=""
for candidate in "${java_candidates[@]}"; do
  if [ -n "$candidate" ] && [ -x "$candidate" ] && java_is_compatible "$candidate"; then
    JAVA_BIN="$candidate"
    break
  fi
done

if [ -z "$JAVA_BIN" ]; then
  echo "[ERROR] Java 11+ not found."
  echo "[INFO] Options to fix:"
  echo "  1. Install Java 11+ and ensure it is in your PATH"
  echo "  2. Set JAVA_HOME to your Java 11+ installation"
  echo "  3. Install via package manager:"
  case "$OS" in
    macos)   echo "       brew install openjdk" ;;
    linux)   echo "       sudo apt install openjdk-21-jdk" ;;
    wsl)     echo "       sudo apt install openjdk-21-jdk  (inside WSL)" ;;
    windows) echo "       winget install EclipseAdoptium.Temurin.21.JDK" ;;
  esac
  exit 1
fi

echo "LOG: Using Java: $JAVA_BIN"

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

# Recolher ficheiros válidos (compatível com bash e zsh)
puml_files=()
while IFS= read -r f; do
  puml_files+=("$f")
done < <(find "$DOCS_ROOT" -name "*.puml" -type f | sort)

if [ ${#puml_files[@]} -eq 0 ]; then
  echo "[WARN] No .puml files found in $DOCS_ROOT"
  echo "Finished"
  exit 0
fi

valid_files=()
for aFile in "${puml_files[@]}"; do
  if [ ! -s "$aFile" ]; then
    echo "[WARN] Skipping empty file: $aFile"
  else
    valid_files+=("$aFile")
  fi
done

if [ ${#valid_files[@]} -eq 0 ]; then
  echo "[WARN] All .puml files are empty, nothing to process."
  echo "Finished"
  exit 0
fi

# Substituir o bloco final por isto:
echo "Processing ${#valid_files[@]} file(s) in single JVM..."

# Obter lista única de svg_dirs
svg_dirs=()
for aFile in "${valid_files[@]}"; do
  svg_dir="$(cd "$(dirname "$aFile")" && pwd)/../svg"
  mkdir -p "$svg_dir"
  svg_dir="$(cd "$(dirname "$aFile")/../svg" && pwd)"

  already=0
  for d in "${svg_dirs[@]:-}"; do
    [ "$d" = "$svg_dir" ] && already=1 && break
  done
  [ "$already" -eq 0 ] && svg_dirs+=("$svg_dir")
done

# Uma invocação por svg_dir
for svg_dir in "${svg_dirs[@]}"; do
  puml_dir="$(dirname "$svg_dir")/puml"
  group_files=()
  for aFile in "${valid_files[@]}"; do
    file_svg_dir="$(cd "$(dirname "$aFile")/../svg" && pwd)"
    [ "$file_svg_dir" = "$svg_dir" ] && group_files+=("$(to_java_path "$aFile")")
  done

  "$JAVA_BIN" -jar "$PLANTUML_JAR" $extra -t$exportFormat \
    -o "$(to_java_path "$svg_dir")" \
    "${group_files[@]}"
done

echo "Finished"
