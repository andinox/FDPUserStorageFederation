package net.minet.keycloak.spi;

import org.mariadb.jdbc.MariaDbDataSource;
import javax.sql.DataSource;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

/**
 * Factory that creates {@link FdpSQLUserStorageProvider} instances and
 * initializes the SQL datasource.
 */
public class FdpSQLUserStorageProviderFactory implements UserStorageProviderFactory<FdpSQLUserStorageProvider> {
    public static final String PROVIDER_NAME = "fdp-sql";

    private DataSource dataSource;

    @Override
    /**
     * Initialise la datasource à partir de la configuration fournie par Keycloak.
     */
    public void init(Config.Scope config) {
        try {
            MariaDbDataSource ds = new MariaDbDataSource();
            ds.setUrl(System.getProperty("quarkus.datasource.federation.jdbc.url",
                    System.getenv("QUARKUS_DATASOURCE_FEDERATION_JDBC_URL")));
            ds.setUser(System.getProperty("quarkus.datasource.federation.username",
                    System.getenv("QUARKUS_DATASOURCE_FEDERATION_USERNAME")));
            ds.setPassword(System.getProperty("quarkus.datasource.federation.password",
                    System.getenv("QUARKUS_DATASOURCE_FEDERATION_PASSWORD")));
            this.dataSource = ds;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize datasource", e);
        }
    }

    @Override
    /**
     * Crée une nouvelle instance du provider utilisée pour traiter les requêtes de Keycloak.
     */
    public FdpSQLUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new FdpSQLUserStorageProvider(session, model, dataSource);
    }

    @Override
    /**
     * Identifiant utilisé par Keycloak pour référencer ce provider.
     */
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    /**
     * Appelée à l'arrêt. Aucun traitement spécifique n'est nécessaire.
     */
    public void close() {
        // nothing to close
    }
}
