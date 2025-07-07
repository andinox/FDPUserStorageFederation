# FDP User Storage Federation

[![Build](https://github.com/andinox/FDPUserStorageFederation/actions/workflows/maven.yml/badge.svg)](https://github.com/andinox/FDPUserStorageFederation/actions)
[![License](https://img.shields.io/github/license/andinox/FDPUserStorageFederation?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/github/v/release/andinox/FDPUserStorageFederation.svg?style=flat-square)](https://github.com/andinox/FDPUserStorageFederation/releases/latest)
[![Issues](https://img.shields.io/github/issues/andinox/FDPUserStorageFederation.svg?style=flat-square)](https://github.com/andinox/FDPUserStorageFederation/issues)

A Keycloak 26.2.5 user storage provider bridging external accounts stored in a MariaDB table.

## 🚀 Features

- Connects to a MariaDB database containing an `adherents` table.
- Works with Keycloak's ephemeral development database or any PostgreSQL instance.
- Uses XA transactions for both datasources.
- Handles credentials hashed with MD4 (UTF-16LE).
- Runs from the official Keycloak image without a custom Dockerfile.

## 📋 Versions

![Java](https://img.shields.io/badge/Java-21-blue?style=flat-square)
![Keycloak](https://img.shields.io/badge/Keycloak-26.2.5-red?style=flat-square)
![Quarkus](https://img.shields.io/badge/Quarkus-3.20.1-orange?style=flat-square)
![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2.x-blue?style=flat-square)
![Node.js](https://img.shields.io/badge/Node.js-via%20nvm-brightgreen?style=flat-square)

The database schema and seed data are located under [`sql`](sql).

All columns from the `adherents` table are exposed as user attributes when a user is retrieved. These values appear in the **Attributes** tab of the administration console without persisting anything back to the database. For example the `is_naina` column becomes the `isNaina` attribute (with a `is_naina` alias) and `created_at` is shown as both the built‑in created timestamp and a custom attribute. You can map any of these attributes to groups or roles as needed.

## 🧰 Prerequisites

- Java 21
- Maven 3.9 or newer

## 🛠️ Build

1. Install the prerequisites.
2. Package the provider using the repository's `application.properties`:

```bash
mvn package
```

The command reads `application.properties` to build `target/UserStorageFederation-0.0.1.jar` with the MariaDB driver included.

The Quarkus configuration resides in `src/main/resources/application.properties` and is mounted into the Keycloak container by Docker Compose.

## 🚢 Docker Compose

Run the development stack:

```bash
docker compose up
```

Keycloak exposes port `8080` and connects to PostgreSQL for its internal data and to MariaDB for federated users. The relevant settings are in [`docker-compose.yml`](docker-compose.yml):

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
      QUARKUS_DATASOURCE_FEDERATION_JDBC_DRIVER: org.mariadb.jdbc.MariaDbDataSource
      QUARKUS_DATASOURCE_FEDERATION_JDBC_TRANSACTIONS: xa
```

For production, launch the stack in Keycloak production mode:

```bash
docker compose -f docker-compose.prod.yml up
```

## 🎨 Keywind Theme

This repository bundles the [Keywind](https://github.com/lukin/keywind) login theme under `themes/keywind`. Only the templates required for signing in and changing a password are included. Both development and production Compose files mount this directory into Keycloak so you can select the `keywind` theme in the administration console.

An account theme with the same styling is provided in `themes/keywind/account`. Mounting this directory lets Keycloak render the account console with the Minet look and feel.

## 📝 Logging

Keycloak relies on Quarkus for its logging system. Log levels can be adjusted by
setting the `quarkus.log.level` property or the `QUARKUS_LOG_LEVEL` environment
variable, e.g. `QUARKUS_LOG_LEVEL=DEBUG` for verbose output. The bundled
`log4j2.properties` reads this value so there is no need to edit the file.
For more advanced customisation provide your own `log4j2.properties` on the
classpath. A sample configuration is included under
[`src/main/resources`](src/main/resources).

## 🩹 Troubleshooting

If login attempts fail with `invalid_user_credentials` even when the password is correct, verify that you are running a version that includes the fix for user ID parsing. Older builds misinterpreted Keycloak storage identifiers and would always reject passwords. The provider also hashes plain-text passwords on-the-fly so it works whether the database stores MD4 digests or raw strings.

## ⚖️ License

This project is released under the [MIT License](LICENSE).
