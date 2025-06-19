package net.minet.keycloak.hash;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;

public class Md4Utf16PasswordHashProviderFactory implements PasswordHashProviderFactory {
    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Md4Utf16PasswordHashProvider(1);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return Md4Utf16PasswordHashProvider.ID;
    }
}
