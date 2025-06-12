# FDP User Storage Federation

This repository contains a sample [Keycloak](https://www.keycloak.org/) user storage federation provider. It illustrates how to integrate an external SQL database with Keycloak 26 using JPA and MariaDB.

The provider exposes the users stored in the `adherents` table and allows Keycloak to authenticate against that data source. It is mainly intended as a learning example or starting point for your own user federation implementation.

## Features

* Built with Java 21 and Keycloak 26.2.5
* JPA persistence configured for MariaDB
* Simple `ExternalUser` entity mapped to the `adherents` table
* Docker Compose setup to run Keycloak and two MariaDB instances for development

## Directory layout

```
src/
  main/java/net/minet/keycloak/spi/    -- Federation provider implementation
  main/java/net/minet/keycloak/spi/entity/ExternalUser.java
  main/resources/META-INF/             -- persistence configuration
sql/cas_schema.sql                     -- SQL schema for the external users
```

## Getting started

1. **Build the provider**

   ```bash
   make build
   ```

   The JAR is created under `target/UserStorageFederation-0.0.1.jar`.

2. **Start the development environment**

   ```bash
   docker compose -f docker-compose.dev.yml up
   ```

   This command launches:
   - a MariaDB container for Keycloak (`keycloak-db`)
   - a MariaDB container seeded with `sql/cas_schema.sql` (`adh6-local-db`)
   - a Keycloak 26 instance with the federation provider mounted

   Keycloak will be accessible at [http://localhost:8080](http://localhost:8080) with the admin credentials `admin`/`admin`.

   The provider connects to the `adh6-local-db` container using the JPA configuration found in `src/main/resources/META-INF/persistence.xml`.

   To pass additional JVM options to Keycloak, append them to the `JAVA_OPTS_APPEND` variable in `docker-compose.dev.yml`. Example:

   ```yaml
   JAVA_OPTS_APPEND: "-Dnet.bytebuddy.experimental=true -Dmy.custom.property=value"
   ```

## License

Released under the MIT License. See [LICENSE](LICENSE) for details.
