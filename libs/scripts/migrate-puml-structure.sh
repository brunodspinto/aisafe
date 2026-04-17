#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOCS_ROOT="$REPO_ROOT/docs"

echo "LOG: Migrating .puml files to convention docs/*/puml/"

while IFS= read -r aFile; do
  puml_dir="$(dirname "$aFile")"

  # Já está na convenção correta
  if [ "$(basename "$puml_dir")" = "puml" ]; then
    continue
  fi

  # Encontrar a pasta us* diretamente dentro de docs/
  relative="${aFile#$DOCS_ROOT/}"       # ex: us011/aggregate-3_1/aggregate-3_1.puml
  us_folder="${relative%%/*}"           # ex: us011
  us_dir="$DOCS_ROOT/$us_folder"        # ex: /path/docs/us011

  dest_dir="$us_dir/puml"
  dest_file="$dest_dir/$(basename "$aFile")"

  if [ "$aFile" = "$dest_file" ]; then
    continue
  fi

  mkdir -p "$dest_dir"
  echo "Moving: $aFile → $dest_file"
  mv "$aFile" "$dest_file"

  # Apagar pasta original se ficou vazia (compatível com macOS)
  if [ -d "$puml_dir" ] && [ -z "$(ls -A "$puml_dir")" ]; then
    rmdir "$puml_dir"
  fi

done < <(find "$DOCS_ROOT" -name "*.puml" -type f | sort)

echo "Migration finished"