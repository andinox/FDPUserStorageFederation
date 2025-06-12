# FDP User Storage Federation

This project provides a sample Keycloak 26 user storage federation provider backed by MariaDB.

## License

Released under the MIT License. See [LICENSE](LICENSE) for details.

## Development

1. Build the federation provider:

   ```bash
   make build
   ```

2. Start the development environment:

   ```bash
   docker compose -f docker-compose.dev.yml up
   ```

The compose file starts Keycloak 26 with the provider JAR, a MariaDB instance for
Keycloak itself, and another MariaDB instance seeded with the `adherents` table
for testing.
