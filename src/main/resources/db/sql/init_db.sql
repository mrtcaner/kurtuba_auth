
-- psql -h localhost -p 5433 -U postgres -d postgres -f init_db.sql

-- don't run if db already exists!
CREATE DATABASE kurtuba_auth
    OWNER postgres
    ENCODING 'UTF8'
    LC_COLLATE = 'C.UTF-8'
    LC_CTYPE = 'C.UTF-8'
    TEMPLATE template0;

-- this won't work on GUI clients. Use psql or change the db on the GUI client manually. Otherwise, schema auth will be created in the default db.
\connect kurtuba_auth

REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE USAGE  ON SCHEMA public FROM PUBLIC;

-- flyway migration role
CREATE ROLE kurtuba_auth_migrator WITH LOGIN PASSWORD '12345';

GRANT USAGE, CREATE ON SCHEMA public TO kurtuba_auth_migrator;
GRANT CONNECT ON DATABASE kurtuba_auth TO kurtuba_auth_migrator;

-- application runtime role
CREATE ROLE kurtuba_auth_user WITH LOGIN PASSWORD '12345';

GRANT USAGE ON SCHEMA public TO kurtuba_auth_user;
GRANT CONNECT ON DATABASE kurtuba_auth TO kurtuba_auth_user;

-- existing objects (if rerun / dev reset cases where objects already exist)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO kurtuba_auth_user;
GRANT USAGE, SELECT, UPDATE          ON ALL SEQUENCES IN SCHEMA public TO kurtuba_auth_user;

-- default privileges for objects created by the migrator in schema auth
ALTER DEFAULT PRIVILEGES
    FOR ROLE kurtuba_auth_migrator
    IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES
    TO kurtuba_auth_user;

ALTER DEFAULT PRIVILEGES
    FOR ROLE kurtuba_auth_migrator
    IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES
    TO kurtuba_auth_user;

-- if app needs to call functions created by migrations
ALTER DEFAULT PRIVILEGES
    FOR ROLE kurtuba_auth_migrator
    IN SCHEMA public
    GRANT EXECUTE ON FUNCTIONS
    TO kurtuba_auth_user;



--DROP DATABASE kurtuba_auth;
--drop role kurtuba_auth_user;
--drop role kurtuba_auth_migrator;
