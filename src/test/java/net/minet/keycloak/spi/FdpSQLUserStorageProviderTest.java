package net.minet.keycloak.spi;

import net.minet.keycloak.hash.Md4Util;
import net.minet.keycloak.spi.entity.ExternalUser;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class FdpSQLUserStorageProviderTest {

    private DataSource dataSource;
    private FdpSQLUserStorageProvider provider;
    private RealmModel realm;
    private ComponentModel model;
    private KeycloakSession session;

    @BeforeEach
    public void setup() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;MODE=MYSQL;DB_CLOSE_DELAY=-1");
        this.dataSource = ds;

        try (Connection c = ds.getConnection()) {
            c.createStatement().execute("""
                CREATE TABLE adherents (
                    id INT PRIMARY KEY,
                    nom VARCHAR(100),
                    prenom VARCHAR(100),
                    mail VARCHAR(100),
                    login VARCHAR(100),
                    password VARCHAR(100),
                    created_at TIMESTAMP,
                    is_naina TINYINT,
                    ldap_login VARCHAR(100)
                )
                """);

            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO adherents (id, nom, prenom, mail, login, password, created_at, is_naina, ldap_login) " +
                "VALUES (?,?,?,?,?,?,?,?,?)");
            LocalDateTime created = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
            ps.setInt(1, 1);
            ps.setString(2, "Doe");
            ps.setString(3, "John");
            ps.setString(4, "john@example.com");
            ps.setString(5, "jdoe");
            ps.setString(6, "secret");
            ps.setTimestamp(7, Timestamp.valueOf(created));
            ps.setByte(8, (byte)1);
            ps.setString(9, "jdoeLDAP");
            ps.executeUpdate();

            // second row for attribute update tests
            ps.setInt(1, 2);
            ps.setString(2, "Bar");
            ps.setString(3, "Foo");
            ps.setString(4, "foo@example.com");
            ps.setString(5, "foo");
            ps.setString(6, "secret");
            ps.setTimestamp(7, Timestamp.valueOf(created));
            ps.setByte(8, (byte)0);
            ps.setString(9, "fooLDAP");
            ps.executeUpdate();
        }

        this.session = Mockito.mock(KeycloakSession.class);
        this.model = Mockito.mock(ComponentModel.class);
        this.realm = Mockito.mock(RealmModel.class);
        Mockito.when(model.getId()).thenReturn("comp");

        this.provider = new FdpSQLUserStorageProvider(session, model, ds);
    }

    @Test
    public void testMD4hash() {
        assertEquals("c79fd641cfa02d5fa374284887ae53a0", Md4Util.md4Hex("1234zefekghn"));
    }

    @Test
    public void testGetUserByIdMapsRow() {
        UserModel user = provider.getUserById(realm, "comp:1");
        assertNotNull(user, "User should be found");
        assertEquals("jdoe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());

        long expectedTs = LocalDateTime.of(2024, 1, 2, 3, 4, 5)
                .atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        assertEquals(expectedTs, user.getCreatedTimestamp());

        assertEquals("jdoeLDAP", user.getFirstAttribute("ldapLogin"));
        assertEquals("1", user.getFirstAttribute("isNaina"));
        assertEquals("" + expectedTs, user.getFirstAttribute("createdAt"));
        assertEquals("" + expectedTs, user.getFirstAttribute("created_at"));
    }

    @Test
    public void testCreatedAtParsing() {
        ExternalUser ext = new ExternalUser();
        ext.setId(2);
        ExternalUserAdapter adapter = new ExternalUserAdapter(session, realm, model, ext, dataSource);

        adapter.setSingleAttribute("createdAt", "2025-02-03T04:05");
        LocalDateTime dt = LocalDateTime.of(2025, 2, 3, 4, 5);
        long ts = dt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        assertEquals(dt, ext.getCreatedAt());
        assertEquals(ts, adapter.getCreatedTimestamp());

        assertEquals(String.valueOf(ts), adapter.getFirstAttribute("createdAt"));

        adapter.setSingleAttribute("created_at", "1714694400000");
        LocalDateTime dt2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(1714694400000L), ZoneOffset.UTC);
        assertEquals(dt2, ext.getCreatedAt());
    }

    @Test
    public void testRemoveUserDeletesRow() throws Exception {
        UserModel user = provider.getUserById(realm, "comp:2");
        assertNotNull(user);
        assertTrue(provider.removeUser(realm, user));
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM adherents WHERE id=?")) {
            ps.setInt(1, 2);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    @Test
    public void testRemoveUserInvalidId() throws Exception {
        UserModel user = Mockito.mock(UserModel.class);
        Mockito.when(user.getId()).thenReturn("comp:abc");
        assertFalse(provider.removeUser(realm, user));
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM adherents")) {
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }
        }
    }

    @Test
    public void testRemoveUserSqlError() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            c.createStatement().execute("DROP TABLE adherents");
        }
        UserModel user = Mockito.mock(UserModel.class);
        Mockito.when(user.getId()).thenReturn("comp:1");
        assertFalse(provider.removeUser(realm, user));
    }

    @Test
    public void testUpdateCredential() throws Exception {
        UserModel user = provider.getUserById(realm, "comp:1");
        assertNotNull(user);
        CredentialInput input = new CredentialInput() {
            @Override public String getType() { return CredentialModel.PASSWORD; }
            @Override public String getChallengeResponse() { return "newpass"; }
        };
        assertTrue(provider.updateCredential(realm, user, input));
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password FROM adherents WHERE id=?")) {
            ps.setInt(1, 1);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(Md4Util.md4Hex("newpass"), rs.getString(1));
            }
        }
    }

    @Test
    public void testUpdateCredentialInvalidId() throws Exception {
        UserModel user = Mockito.mock(UserModel.class);
        Mockito.when(user.getId()).thenReturn("comp:abc");
        CredentialInput input = new CredentialInput() {
            @Override public String getType() { return CredentialModel.PASSWORD; }
            @Override public String getChallengeResponse() { return "newpass"; }
        };
        assertFalse(provider.updateCredential(realm, user, input));
    }

    @Test
    public void testUpdateCredentialSqlError() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            c.createStatement().execute("DROP TABLE adherents");
        }
        UserModel user = Mockito.mock(UserModel.class);
        Mockito.when(user.getId()).thenReturn("comp:1");
        CredentialInput input = new CredentialInput() {
            @Override public String getType() { return CredentialModel.PASSWORD; }
            @Override public String getChallengeResponse() { return "newpass"; }
        };
        assertFalse(provider.updateCredential(realm, user, input));
    }
}
