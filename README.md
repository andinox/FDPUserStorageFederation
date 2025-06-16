# FDP User Storage Federation

This module provides a simple Keycloak 26.2.5 user storage provider backed by a MariaDB table named `adherents`.
The table schema is included in [sql/cas_schema.sql](sql/cas_schema.sql).

The `docker-compose.yml` file now also provisions a MariaDB service hosting a
database named `adh6_prod`. The federation provider connects to this database to
retrieve external users.

Build the provider with Maven and copy the resulting JAR into Keycloak's `providers` directory.
It exposes the external users to Keycloak and allows authentication against the `adherents` table.
