import net.minet.keycloak.spi.FdpSQLUserStorageProvider;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class FdpSQLUserStorageProviderTest {

    private DataSource ds;

    @BeforeEach
    public void setupDb() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        ds = h2;
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE adherents (id INT AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(50), prenom VARCHAR(50), mail VARCHAR(50), login VARCHAR(50), password VARCHAR(64), ldap_login VARCHAR(50))");
            st.execute("INSERT INTO adherents (nom, prenom, mail, login, password, ldap_login) VALUES ('Doe', 'John', 'john@example.com', 'jdoe', 'pass', 'jdoe')");
        }
    }

    @Test
    public void testGetUserByUsername() {
        KeycloakSession session = Mockito.mock(KeycloakSession.class);
        ComponentModel model = Mockito.mock(ComponentModel.class);
        RealmModel realm = Mockito.mock(RealmModel.class);
        FdpSQLUserStorageProvider provider = new FdpSQLUserStorageProvider(session, model, ds);
        UserModel user = provider.getUserByUsername(realm, "jdoe");
        assertNotNull(user);
        assertEquals("jdoe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
    }

    @Test
    public void testIsValidWithPlainPassword() {
        KeycloakSession session = Mockito.mock(KeycloakSession.class);
        ComponentModel model = Mockito.mock(ComponentModel.class);
        RealmModel realm = Mockito.mock(RealmModel.class);
        FdpSQLUserStorageProvider provider = new FdpSQLUserStorageProvider(session, model, ds);
        UserModel user = provider.getUserByUsername(realm, "jdoe");
        CredentialInput cred = new CredentialInput() {
            @Override public String getType() { return CredentialModel.PASSWORD; }
            @Override public String getChallengeResponse() { return "pass"; }
        };
        assertTrue(provider.isValid(realm, user, cred));
    }
}
