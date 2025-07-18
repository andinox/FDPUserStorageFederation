package net.minet.keycloak.spi;

import javax.sql.DataSource;
import java.sql.*;
import net.minet.keycloak.hash.Md4Util;
import net.minet.keycloak.spi.entity.ExternalUser;
import net.minet.keycloak.spi.ExternalUserMapper;
import net.minet.keycloak.spi.ExternalUserAdapter;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.storage.UserStoragePrivateUtil;

import java.util.Map;
import java.util.stream.Stream;

/**
 * User storage provider backed by an external SQL database. It exposes users
 * to Keycloak and supports credential validation and updates.
 */
public class FdpSQLUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        UserRegistrationProvider,
        UserQueryProvider {

    private static final Logger logger = Logger.getLogger(FdpSQLUserStorageProvider.class);

    // Only retrieve columns we care about from the external DB
    private static final String SELECT_FIELDS = String.join(", ",
            "id", "nom", "prenom", "mail", "login", "password",
            "created_at", "is_naina", "ldap_login");

    protected KeycloakSession session;
    protected DataSource dataSource;
    protected ComponentModel model;

    public FdpSQLUserStorageProvider(KeycloakSession session, ComponentModel model, DataSource dataSource) {
        this.session = session;
        this.model = model;
        this.dataSource = dataSource;
    }

    protected UserModel createAdapter(RealmModel realm, ExternalUser user) {
        return new ExternalUserAdapter(session, realm, model, user, dataSource);
    }


    /**
     * Extracts the numeric external identifier from the Keycloak-formatted id.
     */
    private int extractUserId(String id) {
        String externalId = org.keycloak.storage.StorageId.externalId(id);
        return Integer.parseInt(externalId);
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        Integer userId = null;
        try {
            userId = extractUserId(id);
        } catch (NumberFormatException nfe) {
            UserModel local = UserStoragePrivateUtil.userLocalStorage(session)
                    .getUserById(realm, id);
            if (local != null) {
                return getUserByUsername(realm, local.getUsername());
            }
            logger.warn("Failed to parse external id " + id + ": " + nfe.getMessage());
            return null;
        }

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + SELECT_FIELDS + " FROM adherents WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, ExternalUserMapper.map(rs));
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to get user by id " + id + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + SELECT_FIELDS + " FROM adherents WHERE login = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, ExternalUserMapper.map(rs));
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to get user by username " + username + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + SELECT_FIELDS + " FROM adherents WHERE mail = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, ExternalUserMapper.map(rs));
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to get user by email " + email + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return CredentialModel.PASSWORD.equals(type);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return false;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE adherents SET password = ? WHERE id = ?")) {
            ps.setString(1, Md4Util.md4Hex(input.getChallengeResponse()));
            ps.setInt(2, extractUserId(user.getId()));
            return ps.executeUpdate() > 0;
        } catch (NumberFormatException | SQLException e) {
            logger.warn("Failed to update credential for user " + user.getId() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        // not implemented
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String type) {
        return supportsCredentialType(type);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return false;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password FROM adherents WHERE id = ?")) {
            ps.setInt(1, extractUserId(user.getId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String providedHash = Md4Util.md4Hex(input.getChallengeResponse());
                    String storedHash = rs.getString(1);
                    logger.debugf("Checking provided hash %s against stored hash %s", providedHash, storedHash);
                    if (storedHash != null && !storedHash.matches("[0-9a-fA-F]{32}")) {
                        storedHash = Md4Util.md4Hex(storedHash);
                    }
                    return providedHash.equalsIgnoreCase(storedHash);
                }
            }
        } catch (NumberFormatException | SQLException e) {
            logger.warn("Failed to validate credential for user " + user.getId() + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO adherents (login) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                ExternalUser user = new ExternalUser();
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
                user.setUsername(username);
                return createAdapter(realm, user);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM adherents WHERE id = ?")) {
            ps.setInt(1, extractUserId(user.getId()));
            return ps.executeUpdate() > 0;
        } catch (NumberFormatException | SQLException e) {
            logger.warn("Failed to remove user " + user.getId() + ": " + e.getMessage());
            return false;
        }
    }

    public Stream<UserModel> getUsersStream(RealmModel realm, int first, int max) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + SELECT_FIELDS + " FROM adherents LIMIT ? OFFSET ?")) {
            ps.setInt(1, max);
            ps.setInt(2, first);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<UserModel> list = new java.util.ArrayList<>();
                while (rs.next()) {
                    list.add(createAdapter(realm, ExternalUserMapper.map(rs)));
                }
                return list.stream();
            }
        } catch (SQLException e) {
            return Stream.empty();
        }
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM adherents")) {
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

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer first, Integer max) {
        String pattern = "%" + search.toLowerCase() + "%";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + SELECT_FIELDS + " FROM adherents WHERE lower(login) LIKE ? LIMIT ? OFFSET ?")) {
            ps.setString(1, pattern);
            ps.setInt(2, max);
            ps.setInt(3, first);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<UserModel> list = new java.util.ArrayList<>();
                while (rs.next()) {
                    list.add(createAdapter(realm, ExternalUserMapper.map(rs)));
                }
                return list.stream();
            }
        } catch (SQLException e) {
            return Stream.empty();
        }
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer first, Integer max) {
        return getUsersStream(realm, first == null ? 0 : first, max == null ? Integer.MAX_VALUE : max);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer first, Integer max) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attr, String value) {
        return Stream.empty();
    }


}
