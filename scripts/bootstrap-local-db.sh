#!/usr/bin/env bash

set -euo pipefail

POSTGRES_SERVICE="${POSTGRES_SERVICE:-postgres}"
POSTGRES_SUPERUSER="${POSTGRES_SUPERUSER:-postgres}"
POSTGRES_SUPERUSER_PASSWORD="${POSTGRES_SUPERUSER_PASSWORD:-postgres}"

DB_NAME="${DB_NAME:-kurtuba_auth}"
DB_OWNER="${DB_OWNER:-postgres}"

FLYWAY_USERNAME="${FLYWAY_USERNAME:-kurtuba_auth_migrator}"
FLYWAY_PASSWORD="${FLYWAY_PASSWORD:-12345}"
DB_USERNAME="${DB_USERNAME:-kurtuba_auth_user}"
DB_PASSWORD="${DB_PASSWORD:-12345}"

export PGPASSWORD="${POSTGRES_SUPERUSER_PASSWORD}"

run_psql() {
  docker compose exec -T "${POSTGRES_SERVICE}" \
    psql -v ON_ERROR_STOP=1 -U "${POSTGRES_SUPERUSER}" -d postgres "$@"
}

run_db_psql() {
  docker compose exec -T "${POSTGRES_SERVICE}" \
    psql -v ON_ERROR_STOP=1 -U "${POSTGRES_SUPERUSER}" -d "${DB_NAME}" "$@"
}

wait_for_postgres() {
  until docker compose exec -T "${POSTGRES_SERVICE}" \
    pg_isready -U "${POSTGRES_SUPERUSER}" -d postgres >/dev/null 2>&1; do
    sleep 1
  done
}

echo "Waiting for PostgreSQL..."
wait_for_postgres

echo "Ensuring database ${DB_NAME} exists..."
if ! run_psql -tAc "SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}'" | grep -q 1; then
  run_psql -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_OWNER} ENCODING 'UTF8' LC_COLLATE='C.UTF-8' LC_CTYPE='C.UTF-8' TEMPLATE template0"
fi

echo "Ensuring role ${FLYWAY_USERNAME} exists..."
run_psql -c "DO \$\$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${FLYWAY_USERNAME}') THEN
    CREATE ROLE ${FLYWAY_USERNAME} WITH LOGIN PASSWORD '${FLYWAY_PASSWORD}';
  ELSE
    ALTER ROLE ${FLYWAY_USERNAME} WITH LOGIN PASSWORD '${FLYWAY_PASSWORD}';
  END IF;
END \$\$;"

echo "Ensuring role ${DB_USERNAME} exists..."
run_psql -c "DO \$\$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${DB_USERNAME}') THEN
    CREATE ROLE ${DB_USERNAME} WITH LOGIN PASSWORD '${DB_PASSWORD}';
  ELSE
    ALTER ROLE ${DB_USERNAME} WITH LOGIN PASSWORD '${DB_PASSWORD}';
  END IF;
END \$\$;"

echo "Applying database grants..."
run_db_psql -c "REVOKE CREATE ON SCHEMA public FROM PUBLIC"
run_db_psql -c "REVOKE USAGE ON SCHEMA public FROM PUBLIC"
run_psql -c "GRANT CONNECT ON DATABASE ${DB_NAME} TO ${FLYWAY_USERNAME}"
run_psql -c "GRANT CONNECT ON DATABASE ${DB_NAME} TO ${DB_USERNAME}"
run_db_psql -c "GRANT USAGE, CREATE ON SCHEMA public TO ${FLYWAY_USERNAME}"
run_db_psql -c "GRANT USAGE ON SCHEMA public TO ${DB_USERNAME}"
run_db_psql -c "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${DB_USERNAME}"
run_db_psql -c "GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO ${DB_USERNAME}"
run_db_psql -c "ALTER DEFAULT PRIVILEGES FOR ROLE ${FLYWAY_USERNAME} IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${DB_USERNAME}"
run_db_psql -c "ALTER DEFAULT PRIVILEGES FOR ROLE ${FLYWAY_USERNAME} IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO ${DB_USERNAME}"
run_db_psql -c "ALTER DEFAULT PRIVILEGES FOR ROLE ${FLYWAY_USERNAME} IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO ${DB_USERNAME}"

echo "Database bootstrap complete."
echo "App user: ${DB_USERNAME}"
echo "Flyway user: ${FLYWAY_USERNAME}"
