package net.codjo.administration.server.audit.jdbc;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.codjo.administration.server.AbstractJdbcExecutionSpyTest;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.AdministrationLogFileMock;
import net.codjo.administration.server.operation.configuration.Errors;
import net.codjo.administration.server.operation.configuration.Query;
import net.codjo.administration.server.operation.configuration.QueryPlan;
import net.codjo.sql.server.ConnectionPool;
import net.codjo.sql.server.ConnectionPoolFactory;
import net.codjo.sql.spy.ConnectionSpy;
import net.codjo.sql.spy.stats.Statistics;
import net.codjo.util.time.MockTimeSource;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;
import static net.codjo.administration.server.audit.AdministrationLogFile.formatDate;
import static net.codjo.administration.server.operation.configuration.AdministrationServerConfigurationAgentTest.QUERY1;
import static net.codjo.administration.server.operation.configuration.AdministrationServerConfigurationAgentTest.QUERY2;
import static net.codjo.administration.server.operation.configuration.AdministrationServerConfigurationAgentTest.USER1;
import static net.codjo.administration.server.operation.configuration.AdministrationServerConfigurationAgentTest.USER2;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
/**
 *
 */
@RunWith(Theories.class)
public class JdbcExecutionSpyTest extends AbstractJdbcExecutionSpyTest {
    private static final Logger LOG = Logger.getLogger(JdbcExecutionSpyTest.class);

    private static final String USER = "user";

    @DataPoint
    public static QueryPlan QP_SINGLE_CONNECTION = new QueryPlan(new Query("QUERY1",
                                                                           QUERY1, 12L, 0),
                                                                 new Query("QUERY2", QUERY2, 4L, 0),
                                                                 new Query("QUERY1", QUERY1, 15L, 0));
    @DataPoint
    public static QueryPlan QP_TWO_CONNECTIONS = new QueryPlan(new Query("QUERY1", QUERY1, 12L, 0),
                                                               new Query("QUERY2", QUERY2, 4L, 0),
                                                               new Query("QUERY1", QUERY1, 15L, 0),
                                                               new Query("QUERY2", QUERY2, 23L, 1));


    @Test
    public void testCreateConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = createConnection(USER);
            assertNotNull(connection);
            assertTrue("createConnection must return a ConnectionSpy",
                       ConnectionSpy.class.isAssignableFrom(connection.getClass()));
        }
        finally {
            connection.close();
        }
    }


    @Test
    public void testPoolCreated() throws SQLException {
        ConnectionPool pool = ConnectionPoolFactory.createConnectionPool(USER1, spy);

        spy.poolCreated(pool);

        log.assertContent("");
    }


    @Theory
    public void testPoolDestroyed(QueryPlan queryPlan) throws Exception {
        doTestPoolDestroyed(queryPlan, true); //usePool=true
    }


    @Theory
    public void testPoolDestroyed_unusedPool(QueryPlan queryPlan) throws Exception {
        doTestPoolDestroyed(queryPlan, false); //usePool=false
    }


    private void doTestPoolDestroyed(QueryPlan queryPlan, boolean usePool) throws Exception {
        ConnectionPool pool = ConnectionPoolFactory.createConnectionPool(USER1, spy);
        if (usePool) {
            executeQueries(pool, logFile, USER1, timeSource, queryPlan.getQueries());
        }
        spy.poolDestroyed(pool);

        runAssertions(logFile, queryPlan, usePool);
    }


    protected void runAssertions(AdministrationLogFileMock logFile, QueryPlan queryPlan, boolean usePool)
          throws Exception {
        List<String> lines;
        Errors errors = new Errors();

        if (!usePool) {
            // a pool has been created and destroyed for USER2 but no query were done
            lines = extractLines(logFile, 1);
            assertConnectionStatsLine(lines, errors, 0L, 0, USER2, false); // expectPresent=false
            assertPoolHeaderStatsLines(logFile, USER2, 0, errors, false); // expectPresent=false
            failOnErrors(errors, logFile);
        }
        else {
            //
            // checks logs for the single connection
            //
            Set<Integer> connectionsIds = queryPlan.getConnectionIds();
            for (int connectionId : connectionsIds) {
                int nbUniqueQueries = (connectionId == 0) ? 2 : 1; // not generic regarding QueryPlans
                lines = extractLines(logFile, 1 + nbUniqueQueries);
                Errors connectionErrors = new Errors();

                Statistics query1Stats = queryPlan.computeStats(QUERY1, connectionId);
                Statistics query2Stats = queryPlan.computeStats(QUERY2, connectionId);

                if (query1Stats.getCount() > 0) {
                    long expectedWhen = (connectionId == 0) ? 0 : 1; // not generic regarding QueryPlans
                    int spyId = 0;
                    findUser1ConnectionStats(lines, connectionErrors, QUERY1, query1Stats, expectedWhen, spyId);
                }
                if (query2Stats.getCount() > 0) {
                    long expectedWhen = (connectionId == 0) ? 36 : 93; // not generic regarding QueryPlans
                    int spyId = connectionId;
                    findUser1ConnectionStats(lines, connectionErrors, QUERY2, query2Stats, expectedWhen, spyId);
                }
                long expectedWhen = (connectionId == 0) ? 0 : 78; // not generic regarding QueryPlans
                int value = (connectionId == 0) ? 0 : 1; // not generic regarding QueryPlans
                assertConnectionStatsLine(lines, connectionErrors, expectedWhen, value, USER1, true);

                if (!connectionErrors.isEmpty()) {
                    errors.append("=== Errors on connection #" + connectionId + " ===\n", connectionErrors);
                }
            }
            failOnErrors(errors, logFile);

            //
            // checks logs for user's ConnectionPool (compound of connections)
            //
            assertPoolHeaderStatsLines(logFile, USER1, 2, errors, true);

            lines = extractLines(logFile, 2);
            findUserConnectionPoolStats(lines, errors, 0, QUERY1, queryPlan.computeStats(
                  QUERY1, null), USER1);
            findUserConnectionPoolStats(lines, errors, 1, QUERY2, queryPlan.computeStats(
                  QUERY2, null), USER1);
            failOnErrors(errors, logFile);
        }

        logFile.assertNoMoreLines();
    }


    private void executeQueries(ConnectionPool pool,
                                AdministrationLogFile logFile,
                                String applicationUser,
                                MockTimeSource timeSource,
                                Query... queries) throws SQLException {
        // simulate some activity
        Map<Integer, Connection> connections = new java.util.HashMap<Integer, Connection>();
        try {

            timeSource.reset();
            timeSource.setAutoIncrement(0);

            for (Query query : queries) {
                Connection connection = connections.get(query.getConnectionId());
                if (connection == null) {
                    connection = pool.getConnection();
                    connections.put(query.getConnectionId(), connection);
                }

                LOG.info("Connection #" + query.getConnectionId() + ": Executing query " + query.getQuery());
                executeQuery(timeSource, connection, query);
            }

            timeSource.setAutoIncrement(0);
        }
        finally {
            pool.shutdown();
        }
    }


    private void executeQuery(MockTimeSource timeSource, Connection connection, Query query) throws SQLException {
        Statement statement = connection.createStatement();

        timeSource.setAutoIncrement(query.getTime());
        ResultSet resultSet = statement.executeQuery(query.getQuery());

        statement.close();
        resultSet.close();
    }


    @Override
    protected MockTimeSource createTimeSource() {
        MockTimeSource result = new MockTimeSource();
        result.setAutoIncrement(1);
        return result;
    }


    private void assertPoolHeaderStatsLines(AdministrationLogFileMock logFile,
                                            String user,
                                            int nbUniqueQueries,
                                            Errors errors,
                                            boolean expectPresent) {
        String line1 = "write(JDBC, Aggregated statistics for user '" + user + "', " + nbUniqueQueries
                       + " unique queries)";
        String line2 = "write(JDBC, Aggregated statistics, Order (highest Total time first), "
                       + "User, Min time, Max time, Total time, Count, Query)";

        if (expectPresent) {
            logFile.assertLine(line1);
            logFile.assertLine(line2);
        }
        else {
            List<String> lines = extractLines(logFile, 1);
            assertLineAbsent(lines, line1, false, errors);

            lines = extractLines(logFile, 1);
            assertLineAbsent(lines, line2, false, errors);
        }
    }


    private ArrayList<String> extractLines(AdministrationLogFileMock logFile, int nbLines) {
        return new ArrayList<String>(Arrays.asList(logFile.extractLines(nbLines)));
    }


    private void assertConnectionStatsLine(List<String> lines,
                                           Errors errors,
                                           long expectedWhen,
                                           int value, String user, boolean expectPresent) {
        String linePattern = "write\\(JDBC, Temps Total, " + formatDate(expectedWhen) + ", " + value + ", " + user
                             + ", .*\\)";
        assertLineImpl(lines,
                       linePattern,
                       true,
                       errors, expectPresent);
    }


    public void failOnErrors(Errors errors, AdministrationLogFileMock logFile) {
        if (!errors.isEmpty()) {
            StringBuilder buffer = new StringBuilder("There was errors:\n");
            errors.appendTo(buffer);
            logFile.appendLinesTo(buffer);
            fail(buffer.toString());
        }
    }


    private void findUserConnectionPoolStats(List<String> lines, Errors errors, int order, String query,
                                             Statistics queryStats, String user) {
        assertLinePresent(lines,
                          "write(JDBC, Aggregated statistics, " + order + ", " + user +
                          ", " + queryStats.getMinTime() + ", " + queryStats.getMaxTime() + ", " + queryStats.getTime()
                          +
                          ", " + queryStats.getCount() + ", " + query + ")",
                          false,
                          errors);
    }


    private void findUser1ConnectionStats(List<String> lines,
                                          Errors errors,
                                          String query,
                                          Statistics queryStats, long expectedWhen, int expectedSpyId) {
        String expectedDateString = Matcher.quoteReplacement(AdministrationLogFile.formatDate(expectedWhen));
        assertLinePresent(lines,
                          "write\\(JDBC, Temps BD, " + expectedDateString + ", " + expectedSpyId + ", " + USER1 + ", "
                          + escapeRegExpChars(query)
                          + ", "
                          + queryStats.getCount() + ", " + queryStats.getTime() + " ms\\)",
                          true,
                          errors);
    }


    private static String escapeRegExpChars(String query) {
        query = query.replaceAll("\\|", Matcher.quoteReplacement("\\|"));
        query = query.replaceAll("\\(", Matcher.quoteReplacement("\\("));
        query = query.replaceAll("\\)", Matcher.quoteReplacement("\\)"));
        return query;
    }


    private static void assertLinePresent(List<String> lines, String expectedLine, boolean isPattern, Errors errors) {
        assertLineImpl(lines, expectedLine, isPattern, errors, true);
    }


    private static void assertLineAbsent(List<String> lines, String expectedLine, boolean isPattern, Errors errors) {
        assertLineImpl(lines, expectedLine, isPattern, errors, false);
    }


    private static void assertLineImpl(List<String> lines,
                                       String expectedLine,
                                       boolean isPattern,
                                       Errors errors,
                                       boolean expectPresent) {
        int index = -1;
        Pattern pattern = isPattern ? Pattern.compile(expectedLine) : null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean match;
            if (isPattern) {
                Matcher matcher = pattern.matcher(line);
                match = matcher.matches();
            }
            else {
                match = line.equals(expectedLine);
            }

            if (match) {
                index = i;
                break;
            }
        }

        boolean lineFound = (index >= 0);
        if (expectPresent != lineFound) {
            errors.appendAssertLineError(lines, expectedLine, isPattern, expectPresent);
        }
        else {
            if (lineFound) {
                lines.remove(index);
            }
        }
    }
}
