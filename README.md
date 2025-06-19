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
      QUARKUS_DATASOURCE_FEDERATION_JDBC_DRIVER: org.mariadb.jdbc.MariaDbXADataSource
      QUARKUS_DATASOURCE_FEDERATION_JDBC_TRANSACTIONS: xa
```

For production, launch the stack in Keycloak production mode:

```bash
docker compose -f docker-compose.prod.yml up
```

## ⚖️ License

This project is released under the [MIT License](LICENSE).
