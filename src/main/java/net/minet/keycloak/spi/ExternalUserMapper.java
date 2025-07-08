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

/** Utility class for mapping a JDBC {@link ResultSet} row to an {@link ExternalUser}. */
public final class ExternalUserMapper {
    private ExternalUserMapper() {
    }

    @FunctionalInterface
    private interface ColumnSetter {
        void apply(ExternalUser user, ResultSet rs) throws SQLException;
    }

    private record ColumnMapping(String column, ColumnSetter setter) {}

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).intValue();
    }

    private static Byte nullableByte(ResultSet rs, String column) throws SQLException {
        Object o = rs.getObject(column);
        return o == null ? null : ((Number) o).byteValue();
    }

    private static final List<ColumnMapping> MAPPINGS = List.of(
            new ColumnMapping("id", (u, rs) -> u.setId(nullableInt(rs, "id"))),
            new ColumnMapping("nom", (u, rs) -> u.setLastName(rs.getString("nom"))),
            new ColumnMapping("prenom", (u, rs) -> u.setFirstName(rs.getString("prenom"))),
            new ColumnMapping("mail", (u, rs) -> u.setEmail(rs.getString("mail"))),
            new ColumnMapping("login", (u, rs) -> u.setUsername(rs.getString("login"))),
            new ColumnMapping("password", (u, rs) -> u.setPassword(rs.getString("password"))),
            new ColumnMapping("date_de_depart", (u, rs) -> {
                Date dd = rs.getDate("date_de_depart");
                if (dd != null) u.setDepartureDate(dd.toLocalDate());
            }),
            new ColumnMapping("commentaires", (u, rs) -> u.setComments(rs.getString("commentaires"))),
            new ColumnMapping("mode_association", (u, rs) -> u.setModeAssociation(nullableByte(rs, "mode_association"))),
            new ColumnMapping("access_token", (u, rs) -> u.setAccessToken(rs.getString("access_token"))),
            new ColumnMapping("subnet", (u, rs) -> u.setSubnet(rs.getString("subnet"))),
            new ColumnMapping("ip", (u, rs) -> u.setIp(rs.getString("ip"))),
            new ColumnMapping("chambre_id", (u, rs) -> u.setChambreId(nullableInt(rs, "chambre_id"))),
            new ColumnMapping("created_at", (u, rs) -> {
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
            }),
            new ColumnMapping("updated_at", (u, rs) -> {
                Timestamp ts = rs.getTimestamp("updated_at");
                if (ts != null) u.setUpdatedAt(ts.toLocalDateTime());
            }),
            new ColumnMapping("edminet", (u, rs) -> u.setEdminet(nullableByte(rs, "edminet"))),
            new ColumnMapping("is_naina", (u, rs) -> u.setIsNaina(nullableByte(rs, "is_naina"))),
            new ColumnMapping("mailinglist", (u, rs) -> u.setMailingList(nullableByte(rs, "mailinglist"))),
            new ColumnMapping("mail_membership", (u, rs) -> u.setMailMembership(nullableInt(rs, "mail_membership"))),
            new ColumnMapping("ldap_login", (u, rs) -> u.setLdapLogin(rs.getString("ldap_login"))),
            new ColumnMapping("datesignedhosting", (u, rs) -> {
                Timestamp ts = rs.getTimestamp("datesignedhosting");
                if (ts != null) u.setDateSignedHosting(ts.toLocalDateTime());
            }),
            new ColumnMapping("datesignedadhesion", (u, rs) -> {
                Timestamp ts = rs.getTimestamp("datesignedadhesion");
                if (ts != null) u.setDateSignedAdhesion(ts.toLocalDateTime());
            })
    );

    private static Set<String> availableColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Set<String> columns = new java.util.HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            columns.add(meta.getColumnLabel(i).toLowerCase());
        }
        return columns;
    }

    public static ExternalUser map(ResultSet rs) throws SQLException {
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
