package net.minet.keycloak.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BiConsumer;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Date;
import org.jboss.logging.Logger;

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
    private final DataSource dataSource;
    private static final Logger logger = Logger.getLogger(ExternalUserAdapter.class);

    private static final Map<String, String> ATTR_COLUMNS = Map.ofEntries(
            Map.entry("email", "mail"),
            Map.entry("firstName", "prenom"),
            Map.entry("lastName", "nom"),
            Map.entry("ldapLogin", "ldap_login"),
            Map.entry("general", "commentaires"),
            Map.entry("departureDate", "date_de_depart"),
            Map.entry("modeAssociation", "mode_association"),
            Map.entry("accessToken", "access_token"),
            Map.entry("subnet", "subnet"),
            Map.entry("ip", "ip"),
            Map.entry("chambreId", "chambre_id"),
            Map.entry("createdAt", "created_at"),
            Map.entry("updatedAt", "updated_at"),
            Map.entry("edminet", "edminet"),
            Map.entry("isNaina", "is_naina"),
            Map.entry("mailingList", "mailinglist"),
            Map.entry("mailMembership", "mail_membership"),
            Map.entry("dateSignedHosting", "datesignedhosting"),
            Map.entry("dateSignedAdhesion", "datesignedadhesion")
    );

    private static final Map<String, BiConsumer<ExternalUser, Object>> ATTR_SETTERS = Map.ofEntries(
            Map.entry("email", (u, v) -> u.setEmail((String) v)),
            Map.entry("firstName", (u, v) -> u.setFirstName((String) v)),
            Map.entry("lastName", (u, v) -> u.setLastName((String) v)),
            Map.entry("ldapLogin", (u, v) -> u.setLdapLogin((String) v)),
            Map.entry("general", (u, v) -> u.setComments((String) v)),
            Map.entry("departureDate", (u, v) -> u.setDepartureDate((java.time.LocalDate) v)),
            Map.entry("modeAssociation", (u, v) -> u.setModeAssociation((Byte) v)),
            Map.entry("accessToken", (u, v) -> u.setAccessToken((String) v)),
            Map.entry("subnet", (u, v) -> u.setSubnet((String) v)),
            Map.entry("ip", (u, v) -> u.setIp((String) v)),
            Map.entry("chambreId", (u, v) -> u.setChambreId((Integer) v)),
            Map.entry("createdAt", (u, v) -> u.setCreatedAt((java.time.LocalDateTime) v)),
            Map.entry("updatedAt", (u, v) -> u.setUpdatedAt((java.time.LocalDateTime) v)),
            Map.entry("edminet", (u, v) -> u.setEdminet((Byte) v)),
            Map.entry("isNaina", (u, v) -> u.setIsNaina((Byte) v)),
            Map.entry("mailingList", (u, v) -> u.setMailingList((Byte) v)),
            Map.entry("mailMembership", (u, v) -> u.setMailMembership((Integer) v)),
            Map.entry("dateSignedHosting", (u, v) -> u.setDateSignedHosting((java.time.LocalDateTime) v)),
            Map.entry("dateSignedAdhesion", (u, v) -> u.setDateSignedAdhesion((java.time.LocalDateTime) v))
    );

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

    private static void set(ExternalUser u, BiConsumer<ExternalUser, Object> fn, Object v) {
        fn.accept(u, v);
    }

    private static Object parseValue(String name, String value) {
        if (value == null) return null;
        return switch (name) {
            case "departureDate" -> java.time.LocalDate.parse(value);
            case "modeAssociation", "edminet", "isNaina", "mailingList" -> Byte.valueOf(value);
            case "chambreId", "mailMembership" -> Integer.valueOf(value);
            case "createdAt", "updatedAt", "dateSignedHosting", "dateSignedAdhesion" -> java.time.LocalDateTime.parse(value);
            default -> value;
        };
    }

    private void updateColumn(String column, Object value) {
        logger.debugf("updateColumn %s=%s", column, value);
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE adherents SET " + column + "=? WHERE id=?")) {
            if (value instanceof java.time.LocalDate ld) {
                ps.setDate(1, Date.valueOf(ld));
            } else if (value instanceof java.time.LocalDateTime ldt) {
                ps.setTimestamp(1, Timestamp.valueOf(ldt));
            } else {
                ps.setObject(1, value);
            }
            ps.setInt(2, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Failed to update column " + column + " for user " + user.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Update an attribute both in the wrapped {@link ExternalUser} and in the
     * federated storage. Mapped columns are persisted to the external database
     * and all alias keys (camelCase, snake_case and column name) are kept in
     * sync.
     */
    private void updateAttribute(String name, Object value) {
        logger.debugf("updateAttribute %s=%s", name, value);
        BiConsumer<ExternalUser, Object> setter = ATTR_SETTERS.get(name);
        String column = ATTR_COLUMNS.get(name);
        if (setter != null && column != null) {
            set(user, setter, value);
            updateColumn(column, value);
            String str = value == null ? null : value.toString();
            super.setSingleAttribute(name, str);
            logger.debugf(" -> %s=%s", name, str);
            String alias = camelToSnake(name);
            if (!alias.equals(name)) {
                super.setSingleAttribute(alias, str);
                logger.debugf(" -> %s=%s", alias, str);
            }
            if (!column.equals(name) && !column.equals(alias)) {
                super.setSingleAttribute(column, str);
                logger.debugf(" -> %s=%s", column, str);
            }
        } else {
            super.setSingleAttribute(name, value == null ? null : value.toString());
        }
    }

    public ExternalUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, ExternalUser user, DataSource dataSource) {
        super(session, realm, model);
        this.user = user;
        this.dataSource = dataSource;
        this.storageId = new org.keycloak.storage.StorageId(model.getId(), String.valueOf(user.getId()));
        addDefaults();
    }

    private static String camelToSnake(String s) {
        return s.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    /**
     * Populate this adapter's attribute map from the wrapped {@link ExternalUser}.
     * <p>
     * Built-in fields such as first name and last name are assigned via their
     * dedicated setters and also stored as attributes under multiple aliases
     * (e.g. {@code firstName}, {@code first_name}, and the database column name
     * {@code prenom}). Other columns are only exposed as attributes without
     * calling specialised setters.
     */
    private void addDefaults() {
        ATTR_GETTERS.forEach((name, fn) -> {
            Object val = fn.apply(user);
            if (val != null) {
                logger.debugf("addDefaults %s=%s", name, val);
                switch (name) {
                    case "email" -> {
                        setEmail((String) val);
                        super.setSingleAttribute(name, (String) val);
                        logger.debugf(" -> %s=%s", name, val);
                    }
                    case "firstName" -> {
                        setFirstName((String) val);
                        super.setSingleAttribute(name, (String) val);
                        logger.debugf(" -> %s=%s", name, val);
                    }
                    case "lastName" -> {
                        setLastName((String) val);
                        super.setSingleAttribute(name, (String) val);
                        logger.debugf(" -> %s=%s", name, val);
                    }
                    case "createdAt" -> {
                        setCreatedTimestamp(((java.time.LocalDateTime) val)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli());
                        super.setSingleAttribute(name, val.toString());
                        logger.debugf(" -> %s=%s", name, val);
                    }
                    default -> super.setSingleAttribute(name, val.toString());
                }
                String alias = camelToSnake(name);
                if (!alias.equals(name)) {
                    super.setSingleAttribute(alias, val.toString());
                    logger.debugf(" -> %s=%s", alias, val);
                }
                String columnAlias = ATTR_COLUMNS.get(name);
                if (columnAlias != null &&
                        !columnAlias.equals(name) &&
                        !columnAlias.equals(alias)) {
                    super.setSingleAttribute(columnAlias, val.toString());
                    logger.debugf(" -> %s=%s", columnAlias, val);
                }
            }
        });
    }

    @Override
    public String getUsername() {
        return get(user, ExternalUser::getUsername);
    }

    @Override
    public void setUsername(String username) {
        set(user, (u,v) -> u.setUsername((String)v), username);
        updateColumn("login", username);
    }

    @Override
    public String getEmail() {
        return get(user, ExternalUser::getEmail);
    }

    @Override
    public void setEmail(String email) {
        updateAttribute("email", email);
    }

    @Override
    public String getFirstName() {
        return get(user, ExternalUser::getFirstName);
    }

    @Override
    public void setFirstName(String firstName) {
        updateAttribute("firstName", firstName);
    }

    @Override
    public String getLastName() {
        return get(user, ExternalUser::getLastName);
    }

    @Override
    public void setLastName(String lastName) {
        updateAttribute("lastName", lastName);
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
        Object val = parseValue(name, value);
        updateAttribute(name, val);
    }

    @Override
    public void removeAttribute(String name) {
        updateAttribute(name, null);
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
