package org.mariadb.jdbc;

/**
 * Shim for legacy configuration.
 * This class exists only for backwards compatibility and may be removed in a
 * future release. Use {@link MariaDbDataSource} instead.
 */
public class MariaDbXADataSource extends MariaDbDataSource {

    public MariaDbXADataSource() {
        super();
    }

    public MariaDbXADataSource(String url) throws java.sql.SQLException {
        super(url);
    }
}
