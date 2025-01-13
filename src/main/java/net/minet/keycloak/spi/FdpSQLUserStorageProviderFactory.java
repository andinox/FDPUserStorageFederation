package net.minet.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FdpSQLUserStorageProviderFactory implements
        UserStorageProviderFactory<FdpSQLUserStorageProvider> {

    public static final String PROVIDER_NAME = "FdpSQL";

    @Override
    public FdpSQLUserStorageProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        Properties props = new Properties();
        try {
            InputStream is = new FileInputStream("TEST");
            props.load(is);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new FdpSQLUserStorageProvider(keycloakSession, componentModel, props);
    }

    @Override
    public String getId() {
        return "";
    }
}
