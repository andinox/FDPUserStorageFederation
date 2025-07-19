package net.minet.keycloak.spi;

import javax.sql.DataSource;
import java.sql.*;
import net.minet.keycloak.hash.Md4Util;
import net.minet.keycloak.spi.entity.ExternalUser;
import net.minet.keycloak.spi.ExternalUserAdapter;
import net.minet.keycloak.spi.dao.ExternalUserDao;
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
 * User storage provider backed by an external SQL database.
 *
 * <p>Users are loaded via JDBC and wrapped in {@link ExternalUserAdapter} so
 * that Keycloak can query and update them. Credential operations are handled
 * through {@link #isValid(RealmModel, UserModel, CredentialInput)} and
 * {@link #updateCredential(RealmModel, UserModel, CredentialInput)}.</p>
 */
public class FdpSQLUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        UserRegistrationProvider,
        UserQueryProvider {

    private static final Logger logger = Logger.getLogger(FdpSQLUserStorageProvider.class);


    private final ExternalUserDao userDao;

    protected KeycloakSession session;
    protected DataSource dataSource;
    protected ComponentModel model;

    /**
     * Instancié par Keycloak lors de l'initialisation du composant de federation.
     * Les connexions JDBC et le DAO sont prêts à être utilisés pour toutes les opérations.
     */
    public FdpSQLUserStorageProvider(KeycloakSession session, ComponentModel model, DataSource dataSource) {
        this.session = session;
        this.model = model;
        this.dataSource = dataSource;
        this.userDao = new ExternalUserDao(dataSource);
    }

    /**
     * Crée l'adaptateur {@link ExternalUserAdapter} qui expose l'entité externe à Keycloak.
     */
    protected UserModel createAdapter(RealmModel realm, ExternalUser user) {
        return new ExternalUserAdapter(session, realm, model, user, dataSource);
    }


    /**
     * Extrait l'identifiant numérique stocké dans l'ID Keycloak.
     * Cette valeur sert à interroger la base externe.
     */
    private int extractUserId(String id) {
        String externalId = org.keycloak.storage.StorageId.externalId(id);
        return Integer.parseInt(externalId);
    }

    @Override
    /**
     * Libère les ressources si nécessaire. Ici rien n'est requis mais la méthode est appelée par Keycloak lors de l'arrêt.
     */
    public void close() {
        // nothing to close
    }


    @Override
    /**
     * Recherche un utilisateur par son identifiant interne fourni par Keycloak.
     */
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

        ExternalUser user = userDao.findById(userId);
        return user == null ? null : createAdapter(realm, user);
    }

    @Override
    /**
     * Chargement d'un utilisateur par son nom. Utilisé notamment lors de la connexion.
     */
    public UserModel getUserByUsername(RealmModel realm, String username) {
        ExternalUser user = userDao.findByUsername(username);
        return user == null ? null : createAdapter(realm, user);
    }

    @Override
    /**
     * Récupère un utilisateur via son email si celui-ci est unique.
     */
    public UserModel getUserByEmail(RealmModel realm, String email) {
        ExternalUser user = userDao.findByEmail(email);
        return user == null ? null : createAdapter(realm, user);
    }

    @Override
    /**
     * Indique à Keycloak que ce provider gère uniquement des mots de passe.
     */
    public boolean supportsCredentialType(String type) {
        return CredentialModel.PASSWORD.equals(type);
    }

    @Override
    /**
     * Enregistre un nouveau mot de passe fourni par Keycloak dans la base externe.
     */
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
    /**
     * Aucun type d'identifiant ne peut être désactivé via ce provider.
     */
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    @Override
    /**
     * Keycloak appelle cette méthode pour savoir si l'utilisateur possède un mot de passe géré par ce provider.
     */
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String type) {
        return supportsCredentialType(type);
    }

    @Override
    /**
     * Vérifie la validité d'un mot de passe lors de l'authentification.
     */
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
    /**
     * Ajoute un nouvel utilisateur minimal dans la base externe lorsque Keycloak en crée un.
     */
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
    /**
     * Supprime l'utilisateur de la base externe quand Keycloak le désactive.
     */
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

    /**
     * Retourne un flux paginé d'utilisateurs pour les besoins de l'interface d'administration Keycloak.
     */
    public Stream<UserModel> getUsersStream(RealmModel realm, int first, int max) {
        return userDao.getUsersStream(first, max)
                .map(u -> createAdapter(realm, u));
    }

    @Override
    /**
     * Nombre total d'utilisateurs présent dans la base externe.
     */
    public int getUsersCount(RealmModel realm) {
        return userDao.getUsersCount();
    }

    @Override
    /**
     * Recherche d'utilisateurs depuis l'interface d'administration.
     */
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer first, Integer max) {
        return userDao.searchForUserStream(search, first == null ? 0 : first,
                max == null ? Integer.MAX_VALUE : max)
                .map(u -> createAdapter(realm, u));
    }

    @Override
    /**
     * Keycloak peut fournir un ensemble de critères, ici ils ne sont pas pris en compte.
     */
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer first, Integer max) {
        return getUsersStream(realm, first == null ? 0 : first, max == null ? Integer.MAX_VALUE : max);
    }

    @Override
    /**
     * La gestion des groupes n'est pas implémentée dans ce provider.
     */
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer first, Integer max) {
        return Stream.empty();
    }

    @Override
    /**
     * Recherche par attribut non prise en charge.
     */
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attr, String value) {
        return Stream.empty();
    }


}
