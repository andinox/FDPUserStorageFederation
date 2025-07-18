package net.minet.keycloak.spi;

import net.minet.keycloak.spi.entity.ExternalUser;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class ExternalUserAdapterTest {
    private DataSource ds;
    private ExternalUser user;
    private ExternalUserAdapter adapter;

    @BeforeEach
    public void setup() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:adapter;MODE=MariaDB;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        ds = h2;
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE adherents (id INT PRIMARY KEY, created_at DATETIME)");
            s.executeUpdate("INSERT INTO adherents (id, created_at) VALUES (1, NULL)");
        }
        user = new ExternalUser();
        user.setId(1);
        KeycloakSession session = Mockito.mock(KeycloakSession.class);
        RealmModel realm = Mockito.mock(RealmModel.class);
        ComponentModel model = Mockito.mock(ComponentModel.class);
        Mockito.when(model.getId()).thenReturn("comp");
        adapter = new ExternalUserAdapter(session, realm, model, user, ds);
    }

    @Test
    public void testSetCreatedTimestampString() throws Exception {
        adapter.setCreatedTimestamp("2025-01-01T10:00");
        long expected = LocalDateTime.of(2025,1,1,10,0)
                .atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        assertEquals(expected, adapter.getCreatedTimestamp());
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT created_at FROM adherents WHERE id=1")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertNotNull(rs.getTimestamp(1));
        }
    }
}
