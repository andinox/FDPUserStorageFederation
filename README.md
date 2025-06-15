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

## Version bumping

Before each commit the script `scripts/bump_version.sh` automatically
increments the patch version found in `pom.xml` and updates all references
to the provider JAR in the repository. The Git hook lives in
`.githooks/pre-commit`.

Activate it once after cloning:

```bash
git config core.hooksPath .githooks
```


On each merge to the `main` branch, the GitHub Actions workflow
`.github/workflows/update-env.yml` runs the same bump script and commits
an updated `.env` file containing the new version number.


## Getting started

1. **Build the provider**

   ```bash
   make build
   ```

   The JAR is created under `target/UserStorageFederation-0.0.5.jar`.

   It bundles the MariaDB JDBC driver using the Maven Shade plugin,
   so you can copy it directly into Keycloak's `providers` directory.

   **Note:** the JAR must exist before starting the containers. If it is
   missing when you run `docker compose`, Docker will try to mount a
   directory in its place and fail with an error similar to:

   ```
   Error response from daemon: failed to create task for container: ...
   Are you trying to mount a directory onto a file (or vice-versa)?
   ```

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

## Troubleshooting

If Keycloak fails during the `build` phase with an error similar to:

```
ERROR: io.smallrye.config.ConfigSourceFactory: io.smallrye.config.PropertiesLocationConfigSourceFactory not a subtype
```

it usually means that the provider JAR contains its own copy of Quarkus libraries.
To avoid classloading conflicts, ensure the Maven Shade plugin only bundles the
MariaDB JDBC driver. The provided `pom.xml` already defines this configuration.
Rebuild the project and copy the resulting JAR to Keycloak's `providers` directory.

Another error you might encounter during the Keycloak build phase is:

```
ERROR: Multiple datasources are configured but more than 1 is using non-XA transactions
```

This happens when Keycloak detects several datasources but at least two of them
are not using XA transactions. When using the sample `docker-compose.dev.yml`
you can avoid this by enabling XA transactions globally:

```yaml
  keycloak:
    environment:
      KC_TRANSACTION_XA_ENABLED: "true"
```

Alternatively, ensure that all datasources except one have
`quarkus.datasource.<name>.jdbc.transactions=xa` set.

## License

Released under the MIT License. See [LICENSE](LICENSE) for details.
