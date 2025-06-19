package net.minet.keycloak.spi;

import javax.sql.DataSource;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import net.minet.keycloak.spi.entity.ExternalUser;
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
import java.util.Map;
import java.util.stream.Stream;

public class FdpSQLUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        UserRegistrationProvider,
        UserQueryProvider {

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
            user.setLdapLogin(rs.getString("ldap_login"));
        } catch (SQLException ignore) {
            // column may not exist
        }
        return user;
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        try {
            int userId = Integer.parseInt(id);
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, ldap_login FROM adherents WHERE id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return createAdapter(realm, mapUser(rs));
                    }
                }
            }
        } catch (NumberFormatException | SQLException e) {
            // ignore
        }
        return null;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, ldap_login FROM adherents WHERE login = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, mapUser(rs));
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return null;
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, ldap_login FROM adherents WHERE mail = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return createAdapter(realm, mapUser(rs));
                }
            }
        } catch (SQLException e) {
            // ignore
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
            ps.setString(1, md4Hex(input.getChallengeResponse()));
            ps.setInt(2, Integer.parseInt(user.getId()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
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
            ps.setInt(1, Integer.parseInt(user.getId()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return md4Hex(input.getChallengeResponse()).equalsIgnoreCase(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            // ignore
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
            ps.setInt(1, Integer.parseInt(user.getId()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public Stream<UserModel> getUsersStream(RealmModel realm, int first, int max) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, ldap_login FROM adherents LIMIT ? OFFSET ?")) {
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
            // ignore
        }
        return 0;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer first, Integer max) {
        String pattern = "%" + search.toLowerCase() + "%";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, nom, prenom, mail, login, password, ldap_login FROM adherents WHERE lower(login) LIKE ? LIMIT ? OFFSET ?")) {
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

    private String md4Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD4");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_16LE));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD4 algorithm not available", e);
        }
    }

    private static class ExternalUserAdapter extends AbstractUserAdapterFederatedStorage {
        private final ExternalUser user;

        ExternalUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, ExternalUser user) {
            super(session, realm, model);
            this.user = user;
            this.storageId = new org.keycloak.storage.StorageId(model.getId(), String.valueOf(user.getId()));
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
        public java.util.stream.Stream<String> getAttributeStream(String name) {
            if ("ldapLogin".equals(name)) {
                return user.getLdapLogin() == null ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(user.getLdapLogin());
            }
            return super.getAttributeStream(name);
        }

        @Override
        public void setSingleAttribute(String name, String value) {
            if ("ldapLogin".equals(name)) {
                user.setLdapLogin(value);
            } else {
                super.setSingleAttribute(name, value);
            }
        }

        @Override
        public void removeAttribute(String name) {
            if ("ldapLogin".equals(name)) {
                user.setLdapLogin(null);
            } else {
                super.removeAttribute(name);
            }
        }

        @Override
        public java.util.Map<String, java.util.List<String>> getAttributes() {
            java.util.Map<String, java.util.List<String>> attrs = new java.util.HashMap<>(super.getAttributes());
            if (user.getLdapLogin() != null) {
                attrs.put("ldapLogin", java.util.List.of(user.getLdapLogin()));
            }
            return attrs;
        }
    }
}
