# Keycloak image with FDP federation provider

FROM eclipse-temurin:21-jdk AS keycloak
ARG KEYCLOAK_VERSION=26.2.5
ENV KC_HOME=/opt/keycloak
RUN curl -fsSLo /tmp/keycloak.tar.gz https://github.com/keycloak/keycloak/releases/download/${KEYCLOAK_VERSION}/keycloak-${KEYCLOAK_VERSION}.tar.gz \
    && mkdir -p ${KC_HOME} \
    && tar -xzf /tmp/keycloak.tar.gz --strip-components=1 -C ${KC_HOME} \
    && rm /tmp/keycloak.tar.gz
ENV PATH="${KC_HOME}/bin:${PATH}"
COPY target/UserStorageFederation-0.0.1.jar ${KC_HOME}/providers/UserStorageFederation.jar
COPY src/main/resources/application.properties ${KC_HOME}/conf/application.properties
RUN kc.sh build --spi-connections-jpa-quarkus-additional-dependencies=org.mariadb.jdbc:mariadb-java-client

EXPOSE 8080
ENTRYPOINT ["kc.sh"]
CMD ["start-dev"]
