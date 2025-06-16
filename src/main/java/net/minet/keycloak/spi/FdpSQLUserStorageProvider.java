package net.minet.keycloak.spi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
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
    @PersistenceContext(unitName = "federation")
    protected EntityManager em;
    protected ComponentModel model;

    public FdpSQLUserStorageProvider(KeycloakSession session, ComponentModel model, EntityManager em) {
        this.session = session;
        this.model = model;
        this.em = em;
    }

    protected UserModel createAdapter(RealmModel realm, ExternalUser user) {
        return new ExternalUserAdapter(session, realm, model, user);
    }

    @Override
    public void close() {
        if (em.isOpen()) {
            em.close();
        }
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        ExternalUser user = null;
        try {
            user = em.find(ExternalUser.class, Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return null;
        }
        if (user == null) return null;
        return createAdapter(realm, user);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        try {
            ExternalUser user = em.createQuery("select u from ExternalUser u where u.username = :username", ExternalUser.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return createAdapter(realm, user);
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        try {
            ExternalUser user = em.createQuery("select u from ExternalUser u where u.email = :email", ExternalUser.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return createAdapter(realm, user);
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return CredentialModel.PASSWORD.equals(type);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return false;
        ExternalUser ext = em.find(ExternalUser.class, Integer.parseInt(user.getId()));
        if (ext == null) return false;
        em.getTransaction().begin();
        ext.setPassword(input.getChallengeResponse());
        em.getTransaction().commit();
        return true;
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
        ExternalUser ext = em.find(ExternalUser.class, Integer.parseInt(user.getId()));
        return ext != null && input.getChallengeResponse().equals(ext.getPassword());
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        ExternalUser user = new ExternalUser();
        user.setUsername(username);
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
        return createAdapter(realm, user);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        ExternalUser ext = em.find(ExternalUser.class, Integer.parseInt(user.getId()));
        if (ext == null) return false;
        em.getTransaction().begin();
        em.remove(ext);
        em.getTransaction().commit();
        return true;
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, int first, int max) {
        return em.createQuery("select u from ExternalUser u", ExternalUser.class)
                .setFirstResult(first)
                .setMaxResults(max)
                .getResultStream()
                .map(u -> createAdapter(realm, u));
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        Long count = em.createQuery("select count(u) from ExternalUser u", Long.class)
                .getSingleResult();
        return count.intValue();
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, int first, int max) {
        String pattern = "%" + search.toLowerCase() + "%";
        return em.createQuery("select u from ExternalUser u where lower(u.username) like :pattern", ExternalUser.class)
                .setParameter("pattern", pattern)
                .setFirstResult(first)
                .setMaxResults(max)
                .getResultStream()
                .map(u -> createAdapter(realm, u));
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer first, Integer max) {
        return em.createQuery("select u from ExternalUser u", ExternalUser.class)
                .getResultStream()
                .map(u -> createAdapter(realm, u));
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
    }
}
