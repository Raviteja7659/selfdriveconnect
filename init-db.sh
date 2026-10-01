#!/bin/bash
# Runs once, on first container startup, to create one database
# per microservice (database-per-service pattern). sdc_vehicle_db
# is already created via POSTGRES_DB env var; this adds the rest.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE sdc_booking_db;
    CREATE DATABASE sdc_user_db;
    CREATE DATABASE sdc_payment_db;
    CREATE DATABASE sdc_review_db;
EOSQL
