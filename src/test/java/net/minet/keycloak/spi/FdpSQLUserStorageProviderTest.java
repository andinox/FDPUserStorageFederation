package net.minet.keycloak.spi;

import net.minet.keycloak.hash.Md4Util;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class FdpSQLUserStorageProviderTest {
    private DataSource ds;
    private FdpSQLUserStorageProvider provider;
    private KeycloakSession session;
    private ComponentModel model;
    private RealmModel realm;

    @BeforeEach
    public void setup() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:test;MODE=MariaDB;DB_CLOSE_DELAY=-1");
        this.ds = h2;
        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("CREATE TABLE adherents (" +
                    "id INT PRIMARY KEY, " +
                    "nom VARCHAR(255), " +
                    "prenom VARCHAR(255), " +
                    "mail VARCHAR(255), " +
                    "login VARCHAR(255), " +
                    "password VARCHAR(255), " +
                    "created_at TIMESTAMP, " +
                    "is_naina TINYINT, " +
                    "ldap_login VARCHAR(255))");
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO adherents (id, nom, prenom, mail, login, password, created_at, is_naina, ldap_login) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, 1);
            ps.setString(2, "Dupont");
            ps.setString(3, "Jean");
            ps.setString(4, "jean.dupont@example.com");
            ps.setString(5, "jdupont");
            ps.setString(6, Md4Util.md4Hex("secret"));
            ps.setTimestamp(7, Timestamp.valueOf("2025-01-01 10:00:00"));
            ps.setByte(8, (byte) 1);
            ps.setString(9, "jdupont");
            ps.executeUpdate();

            ps.setInt(1, 2);
            ps.setString(2, "Martin");
            ps.setString(3, "Marie");
            ps.setString(4, "marie.martin@example.com");
            ps.setString(5, "mmartin");
            ps.setString(6, Md4Util.md4Hex("passwd"));
            ps.setTimestamp(7, Timestamp.valueOf("2025-02-01 11:00:00"));
            ps.setByte(8, (byte) 1);
            ps.setString(9, "mmartin");
            ps.executeUpdate();
        }
        this.session = Mockito.mock(KeycloakSession.class, Mockito.RETURNS_DEEP_STUBS);
        this.model = Mockito.mock(ComponentModel.class);
        this.realm = Mockito.mock(RealmModel.class);
        Mockito.when(model.getId()).thenReturn("model-id");
        this.provider = new FdpSQLUserStorageProvider(session, model, ds);
    }

    @Test
    public void testGetUserByUsername() {
        UserModel user = provider.getUserByUsername(realm, "jdupont");
        assertNotNull(user);
        assertEquals("jdupont", user.getUsername());
        assertEquals("jean.dupont@example.com", user.getEmail());
        assertEquals("Jean", user.getFirstName());
        assertEquals("Dupont", user.getLastName());
    }

    @Test
    public void testPasswordValidation() {
        UserModel user = provider.getUserByUsername(realm, "jdupont");
        CredentialInput cred = Mockito.mock(CredentialInput.class);
        Mockito.when(cred.getType()).thenReturn(CredentialModel.PASSWORD);
        Mockito.when(cred.getChallengeResponse()).thenReturn("secret");
        assertTrue(provider.isValid(realm, user, cred));
        Mockito.when(cred.getChallengeResponse()).thenReturn("wrong");
        assertFalse(provider.isValid(realm, user, cred));
    }

    @Test
    public void testAttributeSynchronization() throws Exception {
        ExternalUserAdapter user = (ExternalUserAdapter) provider.getUserByUsername(realm, "jdupont");
        user.setSingleAttribute("lastName", "Durand");
        user.setSingleAttribute("createdAt", "2030-01-01T12:30");

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT nom, created_at FROM adherents WHERE id = 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Durand", rs.getString("nom"));
                assertEquals(Timestamp.valueOf("2030-01-01 12:30:00"), rs.getTimestamp("created_at"));
            }
        }
        assertEquals("Durand", user.getLastName());
        assertEquals(Timestamp.valueOf("2030-01-01 12:30:00").getTime(), user.getCreatedTimestamp());
    }
}
