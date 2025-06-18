package org.mariadb.jdbc;

/**
 * Compatibility wrapper for {@link MariaDbDataSource} implementing the deprecated
 * {@code MariaDbXADataSource} name used by older code.
 */
public class MariaDbXADataSource extends MariaDbDataSource {

    public MariaDbXADataSource() {
        super();
    }

    public MariaDbXADataSource(String url) throws java.sql.SQLException {
        super(url);
    }
}
