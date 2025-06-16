package net.minet.keycloak.spi;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceUnit;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class FdpSQLUserStorageProviderFactory implements UserStorageProviderFactory<FdpSQLUserStorageProvider> {
    public static final String PROVIDER_NAME = "fdp-sql";

    @PersistenceUnit(unitName = "federation")
    private EntityManagerFactory emf;

    @Override
    public void init(Config.Scope config) {
        this.emf = Persistence.createEntityManagerFactory("federation");
    }

    @Override
    public FdpSQLUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new FdpSQLUserStorageProvider(session, model, emf.createEntityManager());
    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public void close() {
        if (emf != null) {
            emf.close();
        }
    }
}
