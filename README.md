# FDP User Storage Federation

This module provides a simple Keycloak 26 user storage provider backed by a MariaDB table named `adherents`.
The table schema is included in [sql/cas_schema.sql](sql/cas_schema.sql).

Build the provider with Maven and copy the resulting JAR into Keycloak's `providers` directory.
It exposes the external users to Keycloak and allows authentication against the `adherents` table.
