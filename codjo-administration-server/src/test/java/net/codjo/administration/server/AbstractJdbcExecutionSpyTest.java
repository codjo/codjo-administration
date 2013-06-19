package net.codjo.administration.server;
import java.sql.Connection;
import java.sql.SQLException;
import net.codjo.administration.server.audit.AdministrationLogFileMock;
import net.codjo.administration.server.audit.jdbc.JdbcExecutionSpy;
import net.codjo.database.common.api.DatabaseFactory;
import net.codjo.database.common.api.DatabaseHelper;
import net.codjo.database.common.api.JdbcFixture;
import net.codjo.sql.server.ConnectionPoolConfiguration;
import net.codjo.test.common.LogString;
import net.codjo.util.time.TimeSource;
import org.junit.After;
import org.junit.Before;
/**
 *
 */
abstract public class AbstractJdbcExecutionSpyTest {
    protected final LogString log = new LogString();

    private JdbcExecutionSpy spy;
    private Connection connection;

    private ConnectionPoolConfiguration config;


    abstract protected TimeSource createTimeSource();


    @Before
    public void setUp() throws SQLException {
        spy = new JdbcExecutionSpy(new AdministrationLogFileMock(log), null, createTimeSource());

        config = createConfiguration();
    }


    public static ConnectionPoolConfiguration createConfiguration() throws SQLException {
        JdbcFixture jdbc = new DatabaseFactory().createJdbcFixture();
        jdbc.doSetUp();

        try {
            DatabaseHelper databaseHelper = new DatabaseFactory().createDatabaseHelper();
            String catalog = jdbc.advanced().getConnectionMetadata().getCatalog();
            String url = databaseHelper.getConnectionUrl(jdbc.advanced().getConnectionMetadata());
            String driver = databaseHelper.getDriverClassName();

            ConnectionPoolConfiguration result = new ConnectionPoolConfiguration();
            result.setCatalog(catalog);
            result.setUrl(url);
            result.setClassDriver(driver);
            result.setUser(jdbc.advanced().getConnectionMetadata().getUser());
            result.setPassword(jdbc.advanced().getConnectionMetadata().getPassword());
            result.setHostname(jdbc.advanced().getConnectionMetadata().getHostname());

            return result;
        }
        finally {
            jdbc.doTearDown();
        }
    }


    @After
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        spy = null;
    }


    protected final Connection createConnection(String user) throws SQLException {
        return spy.createConnection(config, user);
    }
}
