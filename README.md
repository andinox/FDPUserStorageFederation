# FDP User Storage Federation

This module provides a simple Keycloak 26.2.5 user storage provider backed by a MariaDB table named `adherents`.
The table schema is included in [sql/cas_schema.sql](sql/cas_schema.sql).

The `docker-compose.yml` file now also provisions a MariaDB service hosting a
database named `adh6_prod`. The federation provider connects to this database to
retrieve external users.
To build the federation you need Node.js. The recommended way to install it is with [nvm](https://github.com/nvm-sh/nvm).

Build the provider with Maven and then start the provided `docker-compose.yml` stack.
The Keycloak container mounts the built JAR and loads the configuration from `application.properties` so that the federation connects to the MariaDB database.
External users stored in the `adherents` table can then authenticate through Keycloak.
