KEYCLOAK_VERSION ?= 22.0.5
JAR_NAME = UserStorageFederation-0.0.1.jar

build:
\tmvn -DskipTests package

run-db:
\tdocker run -d --name federation-mariadb \\
\t\t-e MARIADB_USER=keycloak \\
\t\t-e MARIADB_PASSWORD=password \\
\t\t-e MARIADB_ROOT_PASSWORD=root \\
\t\t-e MARIADB_DATABASE=keycloak_external \\
\t\t-p 3306:3306 mariadb:latest

run-keycloak: build
\tdocker run --rm -it -p 8080:8080 \\
\t\t-e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \\
\t\t-v $(PWD)/target/$(JAR_NAME):/opt/keycloak/providers/$(JAR_NAME) \\
\t\tquay.io/keycloak/keycloak:$(KEYCLOAK_VERSION) start-dev

clean:
\tdocker rm -f federation-mariadb || true
\trm -rf target

.PHONY: build run-db run-keycloak clean
