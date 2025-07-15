# FDP User Storage Federation

This project provides a Keycloak user storage provider backed by an external SQL database.
It exposes a subset of database columns as user attributes and synchronises changes back to
that database.

## Building

```
mvn package
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
