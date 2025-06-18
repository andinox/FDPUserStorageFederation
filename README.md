# FDP User Storage Federation

This module provides a simple Keycloak 26.2.5 user storage provider backed by a MariaDB table named `adherents`.
The table schema is included in [sql/cas_schema.sql](sql/cas_schema.sql).

The `docker-compose.yml` file provisions Keycloak and a MariaDB instance
hosting a database named `adh6_prod` that stores external users.
To build the federation you need Node.js. The recommended way to install it is
with [nvm](https://github.com/nvm-sh/nvm).

Build the provider with Maven and then start the stack with `docker compose up`.
The compose file mounts the built JAR and `application.properties` into the
official Keycloak image so no custom Dockerfile is required. External users
stored in the `adherents` table can then authenticate through Keycloak.

The shaded JAR already embeds the MariaDB JDBC driver so no additional build
options are necessary.

This provider performs direct SQL queries through JDBC and does not use JPA.
Therefore no `persistence.xml` is required.

## Running Keycloak

Simply run `docker compose up` after packaging the provider. Keycloak uses its
ephemeral development database and connects to the external MariaDB instance for
federated users.
