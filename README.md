# FDP User Storage Federation

This module provides a simple Keycloak 26.2.5 user storage provider backed by a MariaDB table named `adherents`.
The table schema is included in [sql/cas_schema.sql](sql/cas_schema.sql).
Sample users are inserted automatically from [sql/cas_data.sql](sql/cas_data.sql).

The `docker-compose.yml` file provisions Keycloak together with PostgreSQL for
the Keycloak internal database and a MariaDB instance hosting a database named
`adh6_prod` that stores external users. Both the default datasource and the
federation datasource use **XA** transactions. Keycloak's ephemeral development
database already supports XA, and the
federation datasource is created programmatically using
`MariaDbXADataSource`.
To build the federation you need Node.js. The recommended way to install it is
with [nvm](https://github.com/nvm-sh/nvm).

Build the provider with Maven and then start the stack with `docker compose up`.
The compose file mounts the built JAR and `application.properties` into the
official Keycloak image so no custom Dockerfile is required. External users
stored in the `adherents` table can then authenticate through Keycloak.

The shaded JAR already embeds the MariaDB JDBC driver so no additional build
options are necessary.

The provider also exposes the `ldap_login` column as a `ldapLogin` attribute on
Keycloak user profiles. This attribute can be read and updated through the
standard user attribute APIs.

This provider performs direct SQL queries through JDBC and does not use JPA.
The plugin can therefore operate without JPA. A complete `persistence.xml`
showing a typical Hibernate configuration is provided under `META-INF/services`
for reference, but it is not required for normal operation.

Passwords in the external table are stored as MD4 hashes of the UTF‑16LE
representation of the clear-text password. The federation module automatically
computes this hash when validating or updating credentials.

## Running Keycloak

Simply run `docker compose up` after packaging the provider. Keycloak uses its
ephemeral development database and connects to the external MariaDB instance for
federated users.

The relevant environment settings in `docker-compose.yml` look like this:

```yaml
  keycloak:
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: secret
      QUARKUS_DATASOURCE_JDBC_TRANSACTIONS: xa
      QUARKUS_DATASOURCE_FEDERATION_JDBC_URL: jdbc:mariadb://mariadb:3306/adh6_prod
      QUARKUS_DATASOURCE_FEDERATION_USERNAME: keycloak
      QUARKUS_DATASOURCE_FEDERATION_PASSWORD: password
      QUARKUS_DATASOURCE_FEDERATION_JDBC_DRIVER: org.mariadb.jdbc.MariaDbXADataSource
      QUARKUS_DATASOURCE_FEDERATION_JDBC_TRANSACTIONS: xa
```

For production you can use `docker compose -f docker-compose.prod.yml up` which
starts Keycloak in production mode (`start` command) but otherwise uses the same
services.
