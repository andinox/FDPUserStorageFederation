package net.minet.keycloak.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 *
 * <p>Mapped attributes are stored under multiple aliases: the Java bean name,
 * a snake_case version and the actual column name. Updates to any alias keep
 * the underlying entity and all other aliases in sync.</p>
 */
public class ExternalUserAdapter extends AbstractUserAdapterFederatedStorage {
    private final ExternalUser user;
    private final DataSource dataSource;
    private static final Logger logger = Logger.getLogger(ExternalUserAdapter.class);

    // Mapping between exposed attribute names and database columns
    private static final Map<String, String> ATTRIBUTE_COLUMNS = Map.ofEntries(
            Map.entry("email", "mail"),
            Map.entry("firstName", "prenom"),
            Map.entry("lastName", "nom"),
            Map.entry("ldapLogin", "ldap_login"),
            Map.entry("createdAt", "created_at"),
            Map.entry("isNaina", "is_naina")
    );

    // Setters used when persisting attribute changes back to the database
    private static final Map<String, BiConsumer<ExternalUser, Object>> ATTRIBUTE_SETTERS = Map.ofEntries(
            Map.entry("email", (u, v) -> u.setEmail((String) v)),
            Map.entry("firstName", (u, v) -> u.setFirstName((String) v)),
            Map.entry("lastName", (u, v) -> u.setLastName((String) v)),
            Map.entry("ldapLogin", (u, v) -> u.setLdapLogin((String) v)),
            Map.entry("createdAt", (u, v) -> u.setCreatedAt((java.time.LocalDateTime) v)),
            Map.entry("isNaina", (u, v) -> u.setIsNaina((Byte) v))
    );

    // Accessors used to populate Keycloak with values from the entity
    private static final Map<String, Function<ExternalUser, Object>> ATTRIBUTE_GETTERS = Map.ofEntries(
            Map.entry("email", ExternalUser::getEmail),
            Map.entry("firstName", ExternalUser::getFirstName),
            Map.entry("lastName", ExternalUser::getLastName),
            Map.entry("ldapLogin", ExternalUser::getLdapLogin),
            Map.entry("createdAt", ExternalUser::getCreatedAt),
            Map.entry("isNaina", ExternalUser::getIsNaina)
    );

    private static final Set<String> DATETIME_ATTRIBUTES = Set.of("createdAt");

    private static final Map<String, Function<String, Object>> VALUE_PARSERS = Map.of(
            "createdAt", ExternalUserAdapter::parseDateTime,
            "isNaina", (String v) -> Byte.valueOf(v)
    );

    private static <T> T get(ExternalUser u, Function<ExternalUser, T> fn) {
        return fn.apply(u);
    }

    private static void set(ExternalUser u, BiConsumer<ExternalUser, Object> fn, Object v) {
        fn.accept(u, v);
    }

    private static java.time.LocalDateTime parseDateTime(String value) {
        if (value.matches("-?\\d+")) {
            long ms = Long.parseLong(value);
            return java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(ms),
                    java.time.ZoneOffset.UTC);
        }
        String v = value.replace(' ', 'T');
        java.time.format.DateTimeFormatter[] fmts = {
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };
        for (java.time.format.DateTimeFormatter fmt : fmts) {
            try {
                return java.time.LocalDateTime.parse(v, fmt);
            } catch (java.time.format.DateTimeParseException ignore) {
            }
        }
        throw new java.time.format.DateTimeParseException("Unparseable date", value, 0);
    }

    /**
     * Parse a string representation of an attribute value.
     *
     * <p>For {@code createdAt} the method accepts several formats:
     * <ul>
     *   <li>milliseconds since epoch</li>
     *   <li>ISO-8601 date-time strings (e.g. {@code 2025-01-02T10:00})</li>
     *   <li>"yyyy-MM-dd HH:mm:ss"</li>
     *   <li>"yyyy-MM-dd"</li>
     * </ul>
     */
    private static Object parseValue(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        Function<String, Object> parser = VALUE_PARSERS.get(name);
        if (parser != null) {
            try {
                return parser.apply(value);
            } catch (RuntimeException e) {
                logger.warnf("Failed to parse value for %s: %s", name, value);
                return null;
            }
        }
        return value;
    }

    private static String toAttributeString(String name, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.time.LocalDateTime ldt && DATETIME_ATTRIBUTES.contains(name)) {
            long ms = ldt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            return String.valueOf(ms);
        }
        return value.toString();
    }

    private void setAttributeWithAliases(String name, String value) {
        super.setSingleAttribute(name, value);
        logger.debugf(" -> %s=%s", name, value);
        String alias = camelToSnake(name);
        if (!alias.equals(name)) {
            super.setSingleAttribute(alias, value);
            logger.debugf(" -> %s=%s", alias, value);
        }
        String columnAlias = ATTRIBUTE_COLUMNS.get(name);
        if (columnAlias != null && !columnAlias.equals(name) && !columnAlias.equals(alias)) {
            super.setSingleAttribute(columnAlias, value);
            logger.debugf(" -> %s=%s", columnAlias, value);
        }
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
        BiConsumer<ExternalUser, Object> setter = ATTRIBUTE_SETTERS.get(name);
        String column = ATTRIBUTE_COLUMNS.get(name);
        if (setter != null && column != null) {
            set(user, setter, value);
            updateColumn(column, value);
            String str = toAttributeString(name, value);
            setAttributeWithAliases(name, str);
        } else {
            super.setSingleAttribute(name, value == null ? null : value.toString());
        }
    }

    /**
     * Constructeur utilisé par Keycloak pour créer l'adaptateur lors du chargement d'un utilisateur.
     * Les informations de l'utilisateur externe sont conservées et exposées via l'API {@link UserModel}.
     */
    public ExternalUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, ExternalUser user, DataSource dataSource) {
        super(session, realm, model);
        this.user = user;
        this.dataSource = dataSource;
        this.storageId = new org.keycloak.storage.StorageId(model.getId(), String.valueOf(user.getId()));
        addDefaults();
    }

    /**
     * Convertit un nom d'attribut en notation camelCase vers son équivalent snake_case.
     * Keycloak n'appelle pas directement cette méthode mais elle sert à exposer
     * les attributs sous plusieurs alias.
     */
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
        ATTRIBUTE_GETTERS.forEach((name, fn) -> {
            Object val = fn.apply(user);
            if (val != null) {
                logger.debugf("addDefaults %s=%s", name, val);
                String str = toAttributeString(name, val);
                switch (name) {
                    case "email" -> setEmail((String) val);
                    case "firstName" -> setFirstName((String) val);
                    case "lastName" -> setLastName((String) val);
                    case "createdAt" -> setCreatedTimestamp(Long.parseLong(str));
                }
                setAttributeWithAliases(name, str);
            }
        });
    }

    @Override
    /**
     * Retourne le nom d'utilisateur. Keycloak l'utilise pour l'identification et l'affichage.
     */
    public String getUsername() {
        return get(user, ExternalUser::getUsername);
    }

    @Override
    /**
     * Met à jour le nom d'utilisateur côté base externe et dans Keycloak lorsqu'il change.
     */
    public void setUsername(String username) {
        set(user, (u,v) -> u.setUsername((String)v), username);
        updateColumn("login", username);
    }

    @Override
    /**
     * Récupère l'adresse email depuis l'entité externe pour que Keycloak puisse l'exposer.
     */
    public String getEmail() {
        return get(user, ExternalUser::getEmail);
    }

    @Override
    /**
     * Enregistre la nouvelle adresse email et synchronise la valeur en base.
     */
    public void setEmail(String email) {
        updateAttribute("email", email);
    }

    @Override
    /**
     * Renvoie le prénom stocké en base externe. Keycloak l'affiche dans son interface.
     */
    public String getFirstName() {
        return get(user, ExternalUser::getFirstName);
    }

    @Override
    /**
     * Met à jour le prénom de l'utilisateur dans la base externe et dans les attributs Keycloak.
     */
    public void setFirstName(String firstName) {
        updateAttribute("firstName", firstName);
    }

    @Override
    /**
     * Renvoie le nom de famille afin que Keycloak puisse le présenter et le synchroniser.
     */
    public String getLastName() {
        return get(user, ExternalUser::getLastName);
    }

    @Override
    /**
     * Modifie le nom de famille côté base et dans Keycloak.
     */
    public void setLastName(String lastName) {
        updateAttribute("lastName", lastName);
    }

    @Override
    /**
     * Date de création de l'utilisateur convertie au format attendu par Keycloak.
     */
    public Long getCreatedTimestamp() {
        java.time.LocalDateTime ts = user.getCreatedAt();
        if (ts != null) {
            return ts.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        }
        return super.getCreatedTimestamp();
    }

    @Override
    /**
     * Positionne la date de création depuis la valeur fournie par Keycloak et persiste la mise à jour.
     */
    public void setCreatedTimestamp(Long timestamp) {
        java.time.LocalDateTime ldt = null;
        if (timestamp != null) {
            java.time.Instant i = java.time.Instant.ofEpochMilli(timestamp);
            ldt = java.time.LocalDateTime.ofInstant(i, java.time.ZoneOffset.UTC);
        }
        updateAttribute("createdAt", ldt);
        super.setCreatedTimestamp(timestamp);
    }

    /**
     * Méthode utilitaire acceptant une date sous forme de chaîne.
     * Keycloak peut envoyer des valeurs sous ce format via les APIs d'administration.
     *
     * @param timestamp date de création sous forme textuelle, par exemple "2025-01-02T10:00".
     */
    public void setCreatedTimestamp(String timestamp) {
        Object val = parseValue("createdAt", timestamp);
        java.time.LocalDateTime ldt = (java.time.LocalDateTime) val;
        Long ts = null;
        if (ldt != null) {
            ts = ldt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        }
        setCreatedTimestamp(ts);
    }

    @Override
    /**
     * Retourne un flux des valeurs d'attribut demandées. Keycloak l'utilise pour récupérer les attributs personnalisés.
     */
    public java.util.stream.Stream<String> getAttributeStream(String name) {
        Map<String, List<String>> all = getAttributes();
        if (all.containsKey(name)) {
            return all.get(name).stream();
        }
        return super.getAttributeStream(name);
    }

    @Override
    /**
     * Définit une valeur d'attribut en tenant compte des alias. Utilisé par Keycloak lors des mises à jour via son API.
     */
    public void setSingleAttribute(String name, String value) {
        if ("createdAt".equals(name) || "created_at".equals(name)) {
            Object val = parseValue("createdAt", value);
            java.time.LocalDateTime ldt = (java.time.LocalDateTime) val;
            Long ts = null;
            if (ldt != null) {
                ts = ldt.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            }
            setCreatedTimestamp(ts);
        } else {
            Object val = parseValue(name, value);
            updateAttribute(name, val);
        }
    }

    @Override
    /**
     * Supprime un attribut de l'utilisateur et de la base externe.
     */
    public void removeAttribute(String name) {
        updateAttribute(name, null);
    }

    @Override
    /**
     * Fournit l'ensemble des attributs disponibles pour que Keycloak puisse les renvoyer via ses APIs.
     */
    public Map<String, List<String>> getAttributes() {
        HashMap<String, List<String>> attrs = new HashMap<>(super.getAttributes());
        ATTRIBUTE_GETTERS.forEach((key, fn) -> {
            Object val = fn.apply(user);
            if (val != null) {
                String str = toAttributeString(key, val);
                attrs.put(key, List.of(str));
            }
        });
        return attrs;
    }

    @Override
    /**
     * Dans ce module tous les emails sont considérés comme vérifiés.
     */
    public boolean isEmailVerified() {
        return true;
    }

    @Override
    /**
     * L'état de vérification est fixé à vrai et ne peut être modifié par Keycloak.
     */
    public void setEmailVerified(boolean verified) {
        // always verified, ignore
    }
}
