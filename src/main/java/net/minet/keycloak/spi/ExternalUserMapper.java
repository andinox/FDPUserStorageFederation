package net.minet.keycloak.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minet.keycloak.spi.entity.ExternalUser;

/**
 * Utility class for mapping a `ResultSet` to an `ExternalUser` object.
 * This class is final and cannot be instantiated.
 */
public final class ExternalUserMapper {

    /**
     * Functional interface for setting a property of an `ExternalUser` object
     * based on a column value from a `ResultSet`.
     */
    @FunctionalInterface
    private interface ColumnSetter {
        /**
         * Applies a value from the `ResultSet` to the `ExternalUser` object.
         *
         * @param user The `ExternalUser` object to modify.
         * @param rs   The `ResultSet` containing the data.
         * @throws SQLException If an SQL error occurs.
         */
        void apply(ExternalUser user, ResultSet rs) throws SQLException;
    }

    /**
     * Record representing a mapping between a database column and a setter function.
     *
     * @param column The name of the database column.
     * @param setter The function to set the corresponding property on the `ExternalUser` object.
     */
    private record ColumnMapping(String column, ColumnSetter setter) {}

    /**
     * Retrieves an `Integer` value from a nullable column in the `ResultSet`.
     *
     * @param rs     The `ResultSet` containing the data.
     * @param column The name of the column.
     * @return The `Integer` value, or `null` if the column value is `null`.
     * @throws SQLException If an SQL error occurs.
     */
    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).intValue();
    }

    /**
     * Retrieves a `Byte` value from a nullable column in the `ResultSet`.
     *
     * @param rs     The `ResultSet` containing the data.
     * @param column The name of the column.
     * @return The `Byte` value, or `null` if the column value is `null`.
     * @throws SQLException If an SQL error occurs.
     */
    private static Byte nullableByte(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).byteValue();
    }

    /**
     * List of mappings between database columns and `ExternalUser` properties.
     */
    private static final List<ColumnMapping> MAPPINGS = List.of(
            new ColumnMapping("id", (u, rs) -> u.setId(nullableInt(rs, "id"))),
            new ColumnMapping("nom", (u, rs) -> u.setLastName(rs.getString("nom"))),
            new ColumnMapping("prenom", (u, rs) -> u.setFirstName(rs.getString("prenom"))),
            new ColumnMapping("mail", (u, rs) -> u.setEmail(rs.getString("mail"))),
            new ColumnMapping("login", (u, rs) -> u.setUsername(rs.getString("login"))),
            new ColumnMapping("created_at", (u, rs) -> {
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
            }),
            new ColumnMapping("is_naina", (u, rs) -> u.setIsNaina(nullableByte(rs, "is_naina"))),
            new ColumnMapping("ldap_login", (u, rs) -> u.setLdapLogin(rs.getString("ldap_login")))
    );

    /**
     * Retrieves the set of available column names from the `ResultSet`.
     *
     * @param rs The `ResultSet` containing the data.
     * @return A set of column names (in lowercase).
     * @throws SQLException If an SQL error occurs.
     */
    private static Set<String> availableColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Set<String> columns = new java.util.HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            columns.add(meta.getColumnLabel(i).toLowerCase());
        }
        return columns;
    }

    /**
     * Maps a `ResultSet` row to an `ExternalUser` object.
     *
     * @param rs The `ResultSet` containing the data.
     * @return An `ExternalUser` object populated with the data from the current row.
     * @throws SQLException If an SQL error occurs.
     */
    public static ExternalUser map(ResultSet rs) throws SQLException {
        // Keycloak utilise ce mapper pour convertir les résultats SQL en objets utilisateurs.
        ExternalUser user = new ExternalUser();
        Set<String> columns = availableColumns(rs);
        for (ColumnMapping m : MAPPINGS) {
            if (columns.contains(m.column())) {
                m.setter().apply(user, rs);
            }
        }
        return user;
    }
}