#!/bin/bash
cd "$(dirname "$0")"
# Update or add persistence.repositoryFactory while preserving other properties
PROPS=src/main/resources/application.properties
TMP=${PROPS}.tmp
echo "persistence.repositoryFactory=aisafe.infrastructure.persistence.jpa.JpaRepositoryFactory" > "$TMP"
if [ -f "$PROPS" ]; then
	grep -v '^persistence.repositoryFactory=' "$PROPS" >> "$TMP" || true
fi
mv -f "$TMP" "$PROPS"
# Run bootstrap (AiSafeBootstrap)
mvn clean compile exec:java -Dexec.mainClass="aisafe.app.console.AiSafeBootstrap"
