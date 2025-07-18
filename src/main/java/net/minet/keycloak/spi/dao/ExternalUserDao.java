package net.minet.keycloak.spi.dao;

import net.minet.keycloak.spi.entity.ExternalUser;
import net.minet.keycloak.spi.ExternalUserMapper;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Simple DAO executing SQL queries to retrieve {@link ExternalUser} entities.
 */
public class ExternalUserDao {
    private static final Logger logger = Logger.getLogger(ExternalUserDao.class);

    // Only retrieve columns we care about from the external DB
    private static final String SELECT_FIELDS = String.join(", ",
            "id", "nom", "prenom", "mail", "login",
            "created_at", "is_naina", "ldap_login");

    private static final String SELECT_BY_ID =
            "SELECT " + SELECT_FIELDS + " FROM adherents WHERE id = ?";
    private static final String SELECT_BY_USERNAME =
            "SELECT " + SELECT_FIELDS + " FROM adherents WHERE login = ?";
    private static final String SELECT_BY_EMAIL =
            "SELECT " + SELECT_FIELDS + " FROM adherents WHERE mail = ?";

    private final DataSource dataSource;

    public ExternalUserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void accept(PreparedStatement ps) throws SQLException;
    }

    private ExternalUser findUser(String query, StatementConfigurer config) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(query)) {
            config.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return ExternalUserMapper.map(rs);
                }
            }
        } catch (SQLException e) {
            logger.warnf("Failed to execute query %s: %s", query, e.getMessage());
        }
        return null;
    }

    public ExternalUser findById(int id) {
        return findUser(SELECT_BY_ID, ps -> ps.setInt(1, id));
    }

    public ExternalUser findByUsername(String username) {
        return findUser(SELECT_BY_USERNAME, ps -> ps.setString(1, username));
    }

    public ExternalUser findByEmail(String email) {
        return findUser(SELECT_BY_EMAIL, ps -> ps.setString(1, email));
    }

    public Stream<ExternalUser> getUsersStream(int first, int max) {
        String query = "SELECT " + SELECT_FIELDS + " FROM adherents LIMIT ? OFFSET ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, max);
            ps.setInt(2, first);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExternalUser> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(ExternalUserMapper.map(rs));
                }
                return list.stream();
            }
        } catch (SQLException e) {
            logger.warn("Failed to list users: " + e.getMessage());
            return Stream.empty();
        }
    }

    public int getUsersCount() {
        String query = "SELECT COUNT(*) FROM adherents";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to count users: " + e.getMessage());
        }
        return 0;
    }

    public Stream<ExternalUser> searchForUserStream(String search, int first, int max) {
        String pattern = "%" + search.toLowerCase() + "%";
        String query = "SELECT " + SELECT_FIELDS + " FROM adherents WHERE lower(login) LIKE ? LIMIT ? OFFSET ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, pattern);
            ps.setInt(2, max);
            ps.setInt(3, first);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExternalUser> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(ExternalUserMapper.map(rs));
                }
                return list.stream();
            }
        } catch (SQLException e) {
            logger.warn("Failed to search users: " + e.getMessage());
            return Stream.empty();
        }
    }
}
