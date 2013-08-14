package net.codjo.sql.server;
import java.sql.SQLException;
import java.util.Properties;
import net.codjo.database.common.api.ConnectionMetadata;
import net.codjo.database.common.api.DatabaseFactory;
import net.codjo.database.common.api.DatabaseHelper;
import net.codjo.database.common.api.JdbcFixture;
/**
 *
 */
public class ConnectionPoolFactory {
    public static ConnectionPoolConfiguration createConfiguration(boolean numericTruncationWarning)
          throws SQLException {
        JdbcFixture jdbc = new DatabaseFactory().createJdbcFixture();
        ConnectionMetadata metadata = jdbc.advanced().getConnectionMetadata();
        DatabaseHelper helper = new DatabaseFactory().createDatabaseHelper();

        Properties properties = new Properties();
        properties.put("user", metadata.getUser());
        properties.put("password", metadata.getPassword());

        return new ConnectionPoolConfiguration(helper.getDriverClassName(),
                                               helper.getConnectionUrl(metadata),
                                               metadata.getCatalog(),
                                               properties,
                                               numericTruncationWarning);
    }


    public static ConnectionPool createConnectionPool(String login, ConnectionFactory factory) throws SQLException {
        ConnectionFactoryConfiguration cfConfiguration = new ConnectionFactoryConfiguration();
        if (factory != null) {
            cfConfiguration.setConnectionFactory(login, factory);
        }
        return new ConnectionPool(createConfiguration(true), cfConfiguration, login);
    }
}
