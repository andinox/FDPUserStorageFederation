# FDP User Storage Federation

This module provides a simple Keycloak 26.2.5 user storage provider backed by a MariaDB table named `adherents`.
The table schema is included in [sql/cas_schema.sql](sql/cas_schema.sql).

The `docker-compose.yml` file provisions Keycloak, PostgreSQL and a MariaDB
instance hosting a database named `adh6_prod` that stores external users.
To build the federation you need Node.js. The recommended way to install it is
with [nvm](https://github.com/nvm-sh/nvm).

Build the provider with Maven and build the custom Keycloak image manually with
`docker build -t custom-keycloak .`. Once the image is available locally, start
the stack with `docker compose up`. The provider JAR and
`application.properties` are copied into the image so no extra volumes are
required. External users stored in the `adherents` table can then authenticate
through Keycloak.

When building Keycloak, ensure that the MariaDB JDBC driver is available. The
included compose file passes the
`--spi-connections-jpa-quarkus-additional-dependencies=org.mariadb.jdbc:mariadb-java-client`
option to `kc.sh build` so the driver is automatically downloaded from Maven
Central.

## Optimized Keycloak build

The Dockerfile invokes `kc.sh build` so the MariaDB driver is downloaded
automatically. Keycloak 26 requires XA transactions when more than one
datasource is configured. If you start Keycloak manually instead of using the
provided image, run the following command before `start` and replace the vendor
as needed. The additional CLI options tell Keycloak to download the MariaDB JDBC
driver from Maven Central and enable XA transactions for the default datasource
during the build:

```bash
kc.sh build --db=mssql --transaction-xa-enabled=true \
  --spi-connections-jpa-quarkus-additional-dependencies=org.mariadb.jdbc:mariadb-java-client
```

After building, launch Keycloak with `kc.sh start-dev` or `kc.sh start`.

## Docker image

A `Dockerfile` is provided to build Keycloak with the federation provider included. It downloads Keycloak 26.2.5, copies the built JAR and `application.properties`, then runs `kc.sh build` to fetch the MariaDB driver.

Build the image and run it locally:

```bash
docker build -t custom-keycloak .
docker run --rm -p 8080:8080 custom-keycloak
```
