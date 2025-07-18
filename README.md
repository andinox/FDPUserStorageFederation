# FDP User Storage Federation

This project provides a Keycloak user storage provider backed by an external SQL database.
It exposes a subset of database columns as user attributes and synchronises changes back to
that database.

## Building

```
mvn package
```

## Running with Docker Compose

1. Build the project so the jar is available for Keycloak:

   ```
   mvn package
   ```

2. Start the environment:

   ```
   docker compose up
   ```

   This brings up Keycloak along with MariaDB and PostgreSQL using the
   configuration from `docker-compose.yml`. Keycloak will be available at
   `http://localhost:8080`.

## Database schema

The `sql/` directory contains the scripts used to initialize the MariaDB
database. `cas_schema.sql` creates a table called `adherents` with all the
columns expected by the provider and `cas_data.sql` inserts two example rows.
These files are mounted into the MariaDB container via
`./sql:/docker-entrypoint-initdb.d` so they run automatically on the first
startup.

If you need to reset the database simply remove the `mariadb_data` volume and
restart the compose environment.

## Running tests

Execute the unit tests using Maven:

```
mvn test
```

## Configuration

The provider expects a Keycloak configuration file `conf/user-profile.json` to be mounted so
the additional attributes (`ldapLogin`, `createdAt`, `isNaina`, etc.) are visible in the admin
console.

## Attribute mapping

The adapter maps the following attributes to database columns:

| Attribute  | Column      |
|------------|-------------|
| `email`    | `mail`      |
| `firstName`| `prenom`    |
| `lastName` | `nom`       |
| `ldapLogin`| `ldap_login`|
| `createdAt`| `created_at`|
| `isNaina`  | `is_naina`  |

`createdAt` is converted to and from milliseconds since epoch when stored in Keycloak.
