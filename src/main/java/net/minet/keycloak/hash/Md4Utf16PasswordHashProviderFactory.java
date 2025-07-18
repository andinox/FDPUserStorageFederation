package net.minet.keycloak.hash;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;

/**
 * Factory class for creating instances of `Md4Utf16PasswordHashProvider`.
 * This class implements the `PasswordHashProviderFactory` interface.
 */
public class Md4Utf16PasswordHashProviderFactory implements PasswordHashProviderFactory {

    /**
     * Creates a new instance of `Md4Utf16PasswordHashProvider`.
     *
     * @param session The `KeycloakSession` instance.
     * @return A new `Md4Utf16PasswordHashProvider` instance.
     */
    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new Md4Utf16PasswordHashProvider(1);
    }

    /**
     * Initializes the factory with the given configuration.
     *
     * @param config The configuration scope.
     */
    @Override
    public void init(Config.Scope config) {
    }

    /**
     * Performs post-initialization tasks for the factory.
     *
     * @param factory The `KeycloakSessionFactory` instance.
     */
    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    /**
     * Closes the factory and releases any resources.
     */
    @Override
    public void close() {
    }

    /**
     * Returns the unique identifier for this password hash provider factory.
     *
     * @return The ID of the factory.
     */
    @Override
    public String getId() {
        return Md4Utf16PasswordHashProvider.ID;
    }
}