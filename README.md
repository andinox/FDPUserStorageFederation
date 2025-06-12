# FDP User Storage Federation 🚀

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Keycloak 26.2.5](https://img.shields.io/badge/Keycloak-26.2.5-blue)](https://www.keycloak.org/)
[![Java 21](https://img.shields.io/badge/Java-21-blue)](https://openjdk.org/)
[![Version 0.0.1](https://img.shields.io/badge/version-0.0.1-orange)](pom.xml)
[![Created January&nbsp;2025](https://img.shields.io/badge/created-Jan_2025-success)](https://github.com/)

Sample Keycloak user storage federation provider that demonstrates how to expose
users stored in an external MariaDB database. It targets Keycloak 26 and can be
used as a starting point for your own integrations.

👉 Découvrez aussi notre site [Minet.net](https://minet.net) !

## Features

- Java 21 provider compatible with Keycloak 26.2.5
- JPA configuration for MariaDB
- `ExternalUser` entity mapped to the `adherents` table
- Docker Compose environment with two MariaDB containers and Keycloak

## Directory layout

```
src/
  main/java/net/minet/keycloak/spi/    -- Federation provider implementation
  main/java/net/minet/keycloak/spi/entity/ExternalUser.java
  main/resources/META-INF/             -- persistence configuration
sql/cas_schema.sql                     -- SQL schema for the external users
```

## Quick start

1. **Build the provider**

   ```bash
   make build
   ```

   The shaded JAR is created under `target/UserStorageFederation-0.0.1.jar` and
   already includes the MariaDB driver. Copy it to Keycloak's `providers`
   directory.

2. **Launch the demo environment**

   ```bash
   docker compose -f docker-compose.dev.yml up
   ```

   This spins up:
   - `keycloak-db`: MariaDB database used by Keycloak
   - `adh6-local-db`: MariaDB database seeded with `sql/cas_schema.sql`
   - `keycloak`: Keycloak 26 with the federation provider mounted

   Access Keycloak at [http://localhost:8080](http://localhost:8080) with
   `admin` / `admin`.

   If you need extra JVM options, edit the `JAVA_OPTS_APPEND` variable in
   `docker-compose.dev.yml`.

## Pourquoi choisir Keycloak plutôt qu'Apereo CAS ? 💡

- ✅ Interface d'administration moderne et conviviale
- ✅ Large écosystème d'extensions et communauté active
- ✅ Support natif d'OpenID Connect et SAML sans configuration complexe

En bref, Keycloak offre une expérience plus intégrée et facile à prendre en
main que *Apereo CAS*.

## License

Released under the [MIT](LICENSE) license.
