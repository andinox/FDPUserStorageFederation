package net.minet.keycloak.spi;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import net.minet.keycloak.spi.entity.ExternalUser;
import java.util.stream.Stream;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

public class FdpSQLUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        UserRegistrationProvider,
        UserQueryProvider {

    protected KeycloakSession session;
    protected EntityManager em;
    protected ComponentModel model;

    public FdpSQLUserStorageProvider(KeycloakSession keycloakSession, ComponentModel componentModel, EntityManager em) {
        this.model = componentModel;
        this.session = keycloakSession;
        this.em = em;
    }

    protected UserModel createAdapter(RealmModel realm, ExternalUser user) {
        return new ExternalUserAdapter(session, realm, model, user);
    }

    @Override
    public void preRemove(RealmModel realm) {
        UserStorageProvider.super.preRemove(realm);
    }

    @Override
    public void close() {
        if (em.isOpen()) {
            em.close();
        }
    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String id) {
        ExternalUser user = null;
        try {
            user = em.find(ExternalUser.class, Long.parseLong(id));
        } catch (NumberFormatException e) {
            return null;
        }
        if (user == null) return null;
        return createAdapter(realmModel, user);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        try {
            ExternalUser user = em.createQuery("select u from ExternalUser u where u.username = :username", ExternalUser.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return createAdapter(realmModel, user);
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String email) {
        try {
            ExternalUser user = em.createQuery("select u from ExternalUser u where u.email = :email", ExternalUser.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return createAdapter(realmModel, user);
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    public boolean supportsCredentialType(String type) {
        return CredentialModel.PASSWORD.equals(type);
    }

    @Override
    public boolean updateCredential(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        if (!supportsCredentialType(credentialInput.getType())) {
            return false;
        }
        ExternalUser user = em.find(ExternalUser.class, Long.parseLong(userModel.getId()));
        if (user == null) {
            return false;
        }
        em.getTransaction().begin();
        user.setPassword(credentialInput.getChallengeResponse());
        em.getTransaction().commit();
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realmModel, UserModel userModel, String s) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realmModel, UserModel userModel) {
        return Stream.empty();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String type) {
        return supportsCredentialType(type);
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        if (!supportsCredentialType(credentialInput.getType())) {
            return false;
        }
        ExternalUser user = em.find(ExternalUser.class, Long.parseLong(userModel.getId()));
        return user != null && credentialInput.getChallengeResponse().equals(user.getPassword());
    }

    @Override
    public UserModel addUser(RealmModel realmModel, String username) {
        ExternalUser user = new ExternalUser();
        user.setUsername(username);
        em.getTransaction().begin();
        em.persist(user);
        em.getTransaction().commit();
        return createAdapter(realmModel, user);
    }

    @Override
    public boolean removeUser(RealmModel realmModel, UserModel userModel) {
        ExternalUser user = em.find(ExternalUser.class, Long.parseLong(userModel.getId()));
        if (user == null) return false;
        em.getTransaction().begin();
        em.remove(user);
        em.getTransaction().commit();
        return true;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realmModel, Map<String, String> map, Integer integer, Integer integer1) {
        return em.createQuery("select u from ExternalUser u", ExternalUser.class)
                .getResultStream()
                .map(user -> createAdapter(realmModel, user));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realmModel, GroupModel groupModel, Integer integer, Integer integer1) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realmModel, String s, String s1) {
        return Stream.empty();
    }

    private static class ExternalUserAdapter extends AbstractUserAdapterFederatedStorage {
        private final ExternalUser user;

        ExternalUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, ExternalUser user) {
            super(session, realm, model);
            this.user = user;
            setEntityId(String.valueOf(user.getId()));
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
