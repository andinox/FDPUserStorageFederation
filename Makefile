KEYCLOAK_VERSION ?= 22.0.5
JAR_NAME = UserStorageFederation-0.0.1.jar

build:
	mvn -DskipTests package

run-db:
	docker run -d --name federation-mariadb \
		-e MARIADB_USER=keycloak \
		-e MARIADB_PASSWORD=password \
		-e MARIADB_ROOT_PASSWORD=root \
		-e MARIADB_DATABASE=keycloak_external \
		-p 3306:3306 mariadb:latest

run-cas-db:
	docker run -d --name cas-mariadb \
		-e MARIADB_USER=casuser \
		-e MARIADB_PASSWORD=caspass \
		-e MARIADB_ROOT_PASSWORD=root \
		-e MARIADB_DATABASE=adh6_prod \
		-v $(PWD)/sql:/docker-entrypoint-initdb.d \
		-p 3307:3306 mariadb:latest

run-keycloak: build
	docker run --rm -it -p 8080:8080 \
		-e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
		-v $(PWD)/target/$(JAR_NAME):/opt/keycloak/providers/$(JAR_NAME) \
		quay.io/keycloak/keycloak:$(KEYCLOAK_VERSION) start-dev

clean:
	docker rm -f federation-mariadb || true
	docker rm -f cas-mariadb || true
	rm -rf target

.PHONY: build run-db run-cas-db run-keycloak clean
