package net.minet.keycloak.spi;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ExternalUserMapperTest {
    private javax.sql.DataSource ds;

    @BeforeEach
    public void setup() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:test;MODE=MariaDB;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        ds = h2;
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(
                "CREATE TABLE adherents (" +
                "id INT PRIMARY KEY, " +
                "nom VARCHAR(255), " +
                "prenom VARCHAR(255), " +
                "mail VARCHAR(255), " +
                "login VARCHAR(255), " +
                "password VARCHAR(255), " +
                "created_at DATETIME, " +
                "is_naina TINYINT, " +
                "ldap_login VARCHAR(255))");
            s.executeUpdate(
                "INSERT INTO adherents (id, nom, prenom, mail, login, password, created_at, is_naina, ldap_login) " +
                "VALUES (1, 'Dupont', 'Jean', 'jean@example.com', 'jdupont', 'pass', '2025-01-01 10:00:00', 1, 'jdupont_ldap')");
        }
    }

    @Test
    public void testMapResultSet() throws Exception {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM adherents WHERE id=1")) {
            assertTrue(rs.next());
            ExternalUser user = ExternalUserMapper.map(rs);
            assertEquals(1, user.getId());
            assertEquals("Dupont", user.getLastName());
            assertEquals("Jean", user.getFirstName());
            assertEquals("jean@example.com", user.getEmail());
            assertEquals("jdupont", user.getUsername());
            assertEquals(Byte.valueOf((byte)1), user.getIsNaina());
            assertEquals("jdupont_ldap", user.getLdapLogin());
            assertNotNull(user.getCreatedAt());
        }
    }
}
