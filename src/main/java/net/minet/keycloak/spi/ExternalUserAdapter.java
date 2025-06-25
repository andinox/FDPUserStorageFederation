package net.minet.keycloak.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minet.keycloak.spi.entity.ExternalUser;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

/**
 * Adapter that exposes an {@link ExternalUser} to Keycloak.
 */
public class ExternalUserAdapter extends AbstractUserAdapterFederatedStorage {
    private final ExternalUser user;

    private static final Map<String, Function<ExternalUser, Object>> ATTR_GETTERS = Map.ofEntries(
            Map.entry("email", ExternalUser::getEmail),
            Map.entry("firstName", ExternalUser::getFirstName),
            Map.entry("lastName", ExternalUser::getLastName),
            Map.entry("ldapLogin", ExternalUser::getLdapLogin),
            Map.entry("general", ExternalUser::getComments),
            Map.entry("departureDate", u -> u.getDepartureDate()),
            Map.entry("modeAssociation", u -> u.getModeAssociation()),
            Map.entry("accessToken", ExternalUser::getAccessToken),
            Map.entry("subnet", ExternalUser::getSubnet),
            Map.entry("ip", ExternalUser::getIp),
            Map.entry("chambreId", u -> u.getChambreId()),
            Map.entry("createdAt", u -> u.getCreatedAt()),
            Map.entry("updatedAt", u -> u.getUpdatedAt()),
            Map.entry("edminet", u -> u.getEdminet()),
            Map.entry("isNaina", u -> u.getIsNaina()),
            Map.entry("mailingList", u -> u.getMailingList()),
            Map.entry("mailMembership", u -> u.getMailMembership()),
            Map.entry("dateSignedHosting", u -> u.getDateSignedHosting()),
            Map.entry("dateSignedAdhesion", u -> u.getDateSignedAdhesion())
    );

    private static <T> T get(ExternalUser u, Function<ExternalUser, T> fn) {
        return fn.apply(u);
    }

    private static void set(ExternalUser u, java.util.function.BiConsumer<ExternalUser, String> fn, String v) {
        fn.accept(u, v);
    }

    public ExternalUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, ExternalUser user) {
        super(session, realm, model);
        this.user = user;
        this.storageId = new org.keycloak.storage.StorageId(model.getId(), String.valueOf(user.getId()));
        addDefaults();
    }

    private void addDefaults() {
        if (user.getEmail() != null) setEmail(user.getEmail());
        if (user.getFirstName() != null) setFirstName(user.getFirstName());
        if (user.getLastName() != null) setLastName(user.getLastName());
    }

    @Override
    public String getUsername() {
        return get(user, ExternalUser::getUsername);
    }

    @Override
    public void setUsername(String username) {
        set(user, ExternalUser::setUsername, username);
    }

    @Override
    public String getEmail() {
        return get(user, ExternalUser::getEmail);
    }

    @Override
    public void setEmail(String email) {
        set(user, ExternalUser::setEmail, email);
    }

    @Override
    public String getFirstName() {
        return get(user, ExternalUser::getFirstName);
    }

    @Override
    public void setFirstName(String firstName) {
        set(user, ExternalUser::setFirstName, firstName);
    }

    @Override
    public String getLastName() {
        return get(user, ExternalUser::getLastName);
    }

    @Override
    public void setLastName(String lastName) {
        set(user, ExternalUser::setLastName, lastName);
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
        switch (name) {
            case "ldapLogin" -> user.setLdapLogin(value);
            case "general" -> user.setComments(value);
            default -> super.setSingleAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        switch (name) {
            case "ldapLogin" -> user.setLdapLogin(null);
            case "general" -> user.setComments(null);
            default -> super.removeAttribute(name);
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        HashMap<String, List<String>> attrs = new HashMap<>(super.getAttributes());
        ATTR_GETTERS.forEach((key, fn) -> {
            Object val = fn.apply(user);
            if (val != null) attrs.put(key, List.of(val.toString()));
        });
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
