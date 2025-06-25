package net.minet.keycloak.spi;

import javax.sql.DataSource;
import java.sql.*;
import net.minet.keycloak.hash.Md4Util;
import net.minet.keycloak.spi.entity.ExternalUser;
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
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.storage.UserStoragePrivateUtil;

import java.util.HashMap;
import java.util.List;
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

    protected KeycloakSession session;
    protected DataSource dataSource;
    protected ComponentModel model;

    public FdpSQLUserStorageProvider(KeycloakSession session, ComponentModel model, DataSource dataSource) {
        this.session = session;
        this.model = model;
        this.dataSource = dataSource;
    }

    protected UserModel createAdapter(RealmModel realm, ExternalUser user) {
        return new ExternalUserAdapter(session, realm, model, user);
    }

    private ExternalUser mapUser(ResultSet rs) throws SQLException {
        ExternalUser user = new ExternalUser();
        user.setId(rs.getInt("id"));
        user.setLastName(rs.getString("nom"));
        user.setFirstName(rs.getString("prenom"));
        user.setEmail(rs.getString("mail"));
        user.setUsername(rs.getString("login"));
        user.setPassword(rs.getString("password"));
        try {
            java.sql.Date dd = rs.getDate("date_de_depart");
            if (dd != null) {
                user.setDepartureDate(dd.toLocalDate());
            }
        } catch (SQLException ignore) {
            // column may not exist
        }
        try {
            user.setComments(rs.getString("commentaires"));
        } catch (SQLException ignore) {
            // column may not exist
        }
        try {
            user.setModeAssociation(rs.getByte("mode_association"));
        } catch (SQLException ignore) {
        }
        try {
            user.setAccessToken(rs.getString("access_token"));
        } catch (SQLException ignore) {
        }
        try {
            user.setSubnet(rs.getString("subnet"));
        } catch (SQLException ignore) {
        }
        try {
            user.setIp(rs.getString("ip"));
        } catch (SQLException ignore) {
        }
        try {
            user.setChambreId(rs.getInt("chambre_id"));
        } catch (SQLException ignore) {
        }
        try {
            java.sql.Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) {
                user.setCreatedAt(ts.toLocalDateTime());
            }
        } catch (SQLException ignore) {
        }
        try {
            java.sql.Timestamp ts = rs.getTimestamp("updated_at");
            if (ts != null) {
                user.setUpdatedAt(ts.toLocalDateTime());
            }
        } catch (SQLException ignore) {
        }
        try {
            user.setEdminet(rs.getByte("edminet"));
        } catch (SQLException ignore) {
        }
        try {
            user.setIsNaina(rs.getByte("is_naina"));
        } catch (SQLException ignore) {
        }
        try {
            user.setMailingList(rs.getByte("mailinglist"));
        } catch (SQLException ignore) {
        }
        try {
            user.setMailMembership(rs.getInt("mail_membership"));
        } catch (SQLException ignore) {
        }
        try {
            user.setLdapLogin(rs.getString("ldap_login"));
        } catch (SQLException ignore) {
        }
        try {
            java.sql.Timestamp ts = rs.getTimestamp("datesignedhosting");
            if (ts != null) {
                user.setDateSignedHosting(ts.toLocalDateTime());
            }
        } catch (SQLException ignore) {
        }
        try {
            java.sql.Timestamp ts = rs.getTimestamp("datesignedadhesion");
            if (ts != null) {
                user.setDateSignedAdhesion(ts.toLocalDateTime());
            }
        } catch (SQLException ignore) {
        }
        return user;
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
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, date_de_depart, commentaires, mode_association, access_token, subnet, ip, chambre_id, created_at, updated_at, edminet, is_naina, mailinglist, mail_membership, ldap_login, datesignedhosting, datesignedadhesion FROM adherents WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, mapUser(rs));
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
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, date_de_depart, commentaires, mode_association, access_token, subnet, ip, chambre_id, created_at, updated_at, edminet, is_naina, mailinglist, mail_membership, ldap_login, datesignedhosting, datesignedadhesion FROM adherents WHERE login = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, mapUser(rs));
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
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, date_de_depart, commentaires, mode_association, access_token, subnet, ip, chambre_id, created_at, updated_at, edminet, is_naina, mailinglist, mail_membership, ldap_login, datesignedhosting, datesignedadhesion FROM adherents WHERE mail = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, mapUser(rs));
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
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, date_de_depart, commentaires, mode_association, access_token, subnet, ip, chambre_id, created_at, updated_at, edminet, is_naina, mailinglist, mail_membership, ldap_login, datesignedhosting, datesignedadhesion FROM adherents LIMIT ? OFFSET ?")) {
            ps.setInt(1, max);
            ps.setInt(2, first);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<UserModel> list = new java.util.ArrayList<>();
                while (rs.next()) {
                    list.add(createAdapter(realm, mapUser(rs)));
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
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, date_de_depart, commentaires, mode_association, access_token, subnet, ip, chambre_id, created_at, updated_at, edminet, is_naina, mailinglist, mail_membership, ldap_login, datesignedhosting, datesignedadhesion FROM adherents WHERE lower(login) LIKE ? LIMIT ? OFFSET ?")) {
            ps.setString(1, pattern);
            ps.setInt(2, max);
            ps.setInt(3, first);
            try (ResultSet rs = ps.executeQuery()) {
                java.util.List<UserModel> list = new java.util.ArrayList<>();
                while (rs.next()) {
                    list.add(createAdapter(realm, mapUser(rs)));
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


    private static class ExternalUserAdapter extends AbstractUserAdapterFederatedStorage {
        private final ExternalUser user;

        ExternalUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, ExternalUser user) {
            super(session, realm, model);
            this.user = user;
            this.storageId = new org.keycloak.storage.StorageId(model.getId(), String.valueOf(user.getId()));
            this.addDefaults();
        }

        private void addDefaults() {
            if (user.getEmail() != null) {
                setEmail(user.getEmail());
            }
            if (user.getFirstName() != null) {
                setFirstName(user.getFirstName());
            }
            if (user.getLastName() != null) {
                setLastName(user.getLastName());
            }
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public void setUsername(String username) {
            user.setUsername(username);
        }

        @Override
        public String getEmail() {
            return user.getEmail();
        }

        @Override
        public void setEmail(String email) {
            user.setEmail(email);
        }

        @Override
        public String getFirstName() {
            return user.getFirstName();
        }

        @Override
        public void setFirstName(String firstName) {
            user.setFirstName(firstName);
        }

        @Override
        public String getLastName() {
            return user.getLastName();
        }

        @Override
        public void setLastName(String lastName) {
            user.setLastName(lastName);
        }

        @Override
        public Long getCreatedTimestamp() {
            java.time.LocalDateTime ts = user.getCreatedAt();
            if (ts != null) {
                return ts.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            return super.getCreatedTimestamp();
        }

        @Override
        public void setCreatedTimestamp(Long timestamp) {
            if (timestamp != null) {
                java.time.Instant i = java.time.Instant.ofEpochMilli(timestamp);
                user.setCreatedAt(java.time.LocalDateTime.ofInstant(i, java.time.ZoneId.systemDefault()));
            } else {
                user.setCreatedAt(null);
            }
            super.setCreatedTimestamp(timestamp);
        }

        @Override
        public java.util.stream.Stream<String> getAttributeStream(String name) {
            Map<String, List<String>> all = getAttributes();
            if (all.containsKey(name)) {
                return all.get(name).stream();
            }
            return super.getAttributeStream(name);
        }

        @Override
        public void setSingleAttribute(String name, String value) {
            if ("ldapLogin".equals(name)) {
                user.setLdapLogin(value);
            } else if ("general".equals(name)) {
                user.setComments(value);
            } else {
                super.setSingleAttribute(name, value);
            }
        }

        @Override
        public void removeAttribute(String name) {
            if ("ldapLogin".equals(name)) {
                user.setLdapLogin(null);
            } else if ("general".equals(name)) {
                user.setComments(null);
            } else {
                super.removeAttribute(name);
            }
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            HashMap<String, List<String>> attrs = new HashMap<>(super.getAttributes());

            if (user.getEmail() != null) {
                attrs.put("email", List.of(user.getEmail()));
            }
            if (user.getFirstName() != null) {
                attrs.put("firstName", List.of(user.getFirstName()));
            }
            if (user.getLastName() != null) {
                attrs.put("lastName", List.of(user.getLastName()));
            }
            if (user.getLdapLogin() != null) {
                attrs.put("ldapLogin", List.of(user.getLdapLogin()));
            }
            if (user.getComments() != null) {
                attrs.put("general", List.of(user.getComments()));
            }

            if (user.getDepartureDate() != null) {
                attrs.put("departureDate", List.of(user.getDepartureDate().toString()));
            }
            if (user.getModeAssociation() != null) {
                attrs.put("modeAssociation", List.of(Byte.toString(user.getModeAssociation())));
            }
            if (user.getAccessToken() != null) {
                attrs.put("accessToken", List.of(user.getAccessToken()));
            }
            if (user.getSubnet() != null) {
                attrs.put("subnet", List.of(user.getSubnet()));
            }
            if (user.getIp() != null) {
                attrs.put("ip", List.of(user.getIp()));
            }
            if (user.getChambreId() != null) {
                attrs.put("chambreId", List.of(Integer.toString(user.getChambreId())));
            }
            if (user.getCreatedAt() != null) {
                attrs.put("createdAt", List.of(user.getCreatedAt().toString()));
            }
            if (user.getUpdatedAt() != null) {
                attrs.put("updatedAt", List.of(user.getUpdatedAt().toString()));
            }
            if (user.getEdminet() != null) {
                attrs.put("edminet", List.of(Byte.toString(user.getEdminet())));
            }
            if (user.getIsNaina() != null) {
                attrs.put("isNaina", List.of(Byte.toString(user.getIsNaina())));
            }
            if (user.getMailingList() != null) {
                attrs.put("mailingList", List.of(Byte.toString(user.getMailingList())));
            }
            if (user.getMailMembership() != null) {
                attrs.put("mailMembership", List.of(Integer.toString(user.getMailMembership())));
            }
            if (user.getDateSignedHosting() != null) {
                attrs.put("dateSignedHosting", List.of(user.getDateSignedHosting().toString()));
            }
            if (user.getDateSignedAdhesion() != null) {
                attrs.put("dateSignedAdhesion", List.of(user.getDateSignedAdhesion().toString()));
            }

            return attrs;
        }

        @Override
        public boolean isEmailVerified() {
            return true;
        }

        @Override
        public void setEmailVerified(boolean verified) {
            // always verified, ignore
        }
    }
}
