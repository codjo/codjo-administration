package net.codjo.administration.server.operation.configuration;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;
import net.codjo.administration.server.AbstractJdbcExecutionSpyTest;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.jdbc.JdbcExecutionSpy;
import net.codjo.agent.UserId;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.mad.common.Log;
import net.codjo.sql.server.ConnectionPool;
import net.codjo.sql.server.ConnectionPoolConfiguration;
import net.codjo.sql.server.JdbcManager;
import net.codjo.util.time.MockTimeSource;
import org.apache.log4j.Logger;
/**
 *
 */
public class ConnectionAction implements AgentContainerFixture.Runnable {
    private static final Logger LOG = Logger.getLogger(ConnectionAction.class);

    private final AdministrationLogFile logFile;
    private final String applicationUser;
    private final Query[] queries;
    private final MockTimeSource timeSource;


    public ConnectionAction(AdministrationLogFile logFile, String applicationUser, MockTimeSource timeSource,
                            Query... queries) {
        this.logFile = logFile;
        this.applicationUser = applicationUser;
        this.queries = queries;
        this.timeSource = timeSource;
    }


    public final void run() throws Exception {
        try {
            // simulate some activity
            String password = "password";
            final UserId userId = UserId.createId(applicationUser, password);
            ConnectionPoolConfiguration config = AbstractJdbcExecutionSpyTest.createConfiguration();
            JdbcManager manager = new JdbcManager(config);
            manager.setDefaultConnectionFactory(new JdbcExecutionSpy(logFile, timeSource));

            ConnectionPool pool = manager.createPool(userId, config.getUser(), config.getPassword());
            Map<Integer, Connection> connections = new java.util.HashMap<Integer, Connection>();
            try {

                timeSource.reset();
                timeSource.setAutoIncrement(0);

                for (Query query : queries) {
                    Connection connection = connections.get(query.connectionId);
                    if (connection == null) {
                        connection = pool.getConnection();
                        connections.put(query.connectionId, connection);
                    }

                    LOG.info("Connection #" + query.connectionId + ": Executing query " + query.getQuery());
                    executeQuery(connection, query);
                }

                timeSource.setAutoIncrement(0);
            }
            finally {
                for (int connectionId : new TreeSet<Integer>(connections.keySet())) {
                    LOG.debug("closing connection #" + connectionId);
                    pool.releaseConnection(connections.get(connectionId));
                }

                // this will also close the connections
                manager.destroyPool(userId);
            }
        }
        catch (Exception e) {
            Log.error(e.getMessage(), e);
            throw e;
        }
    }


    private void executeQuery(Connection connection, Query query) throws SQLException {
        Statement statement = connection.createStatement();

        timeSource.setAutoIncrement(query.getTime());
        ResultSet resultSet = statement.executeQuery(query.getQuery());

        statement.close();
        resultSet.close();
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ConnectionAction[").append(applicationUser);
        sb.append(',').append(queries == null ? "null" : Arrays.asList(queries).toString());
        sb.append(']');
        return sb.toString();
    }
}
