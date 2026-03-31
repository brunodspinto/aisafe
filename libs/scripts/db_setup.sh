#!/bin/bash
# =============================================================================
# db_setup.sh
# Sets up the database (PostgreSQL or H2 depending on profile)
# Usage: ./db_setup.sh [profile]
#   profile: dev (default, H2 in-memory) | prod (PostgreSQL)
# =============================================================================

set -e

PROFILE="${1:-dev}"

echo "============================================"
echo " AISafe - Database Setup (profile: $PROFILE)"
echo "============================================"

if [ "$PROFILE" = "prod" ]; then
  echo ""
  echo "[DB] Setting up PostgreSQL database..."

  # Check if psql is available
  if ! command -v psql &> /dev/null; then
    echo "[DB] ERROR: psql not found. Please install PostgreSQL."
    exit 1
  fi

  DB_HOST="${DB_HOST:-localhost}"
  DB_PORT="${DB_PORT:-5432}"
  DB_NAME="${DB_NAME:-aisafe}"
  DB_USER="${DB_USER:-aisafe_user}"
  DB_PASS="${DB_PASS:-aisafe_pass}"

  echo "[DB] Creating database '$DB_NAME'..."
  psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || echo "[DB] Database may already exist."
  psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';" 2>/dev/null || echo "[DB] User may already exist."
  psql -h "$DB_HOST" -p "$DB_PORT" -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"

  echo "[DB] PostgreSQL database setup complete."

else
  echo ""
  echo "[DB] Using H2 in-memory database (dev profile). No setup required."
  echo "[DB] Database will be initialised automatically on application startup."
fi

echo ""
echo "============================================"
echo " Database setup completed!"
echo "============================================"
