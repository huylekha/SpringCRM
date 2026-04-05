-- Create separate databases for each microservice
-- This script is executed by docker-entrypoint-initdb.d on first container start.
-- POSTGRES_DB=springcrm_auth is created automatically; we only need the crm database.

SELECT 'CREATE DATABASE springcrm_crm OWNER crm_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'springcrm_crm')\gexec
