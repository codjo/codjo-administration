package net.codjo.administration.server.operation.configuration;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.Assert;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.administration.common.Constants;
import net.codjo.administration.server.AbstractJdbcExecutionSpyTest;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.AdministrationLogFileMock;
import net.codjo.administration.server.audit.jdbc.JdbcExecutionSpy;
import net.codjo.administration.server.audit.mad.HandlerExecutionSpy;
import net.codjo.administration.server.operation.log.DefaultLogReader;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Aid;
import net.codjo.agent.UserId;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.test.AgentAssert.Assertion;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.TesterAgentRecorder;
import net.codjo.mad.common.Log;
import net.codjo.mad.server.plugin.MadServerOperations;
import net.codjo.sql.server.ConnectionPool;
import net.codjo.sql.server.ConnectionPoolConfiguration;
import net.codjo.sql.server.JdbcManager;
import net.codjo.sql.spy.stats.Statistics;
import net.codjo.test.common.Directory.NotDeletedException;
import net.codjo.test.common.LogString;
import net.codjo.util.time.MockTimeSource;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static net.codjo.administration.common.AdministrationOntology.CHANGE_JDBC_USERS_FILTER;
import static net.codjo.administration.common.AdministrationOntology.RESTORE_JDBC_USERS_FILTER;
import static net.codjo.administration.server.audit.AdministrationLogFile.formatDate;
import static net.codjo.administration.server.audit.AdministrationLogFileMock.ALL_COLUMNS;
import static net.codjo.administration.server.operation.configuration.AdministrationServerConfigurationAgent.USER_LIST_SEPARATOR;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchContent;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static org.apache.commons.lang.StringUtils.join;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Theories.class)
public class AdministrationServerConfigurationAgentTest {
    private static final Logger LOG = Logger.getLogger(AdministrationServerConfigurationAgentTest.class);

    private static final String AUDIT_DIR = "c:\\dev\\temp\\test";
    private static final String USER1 = "user1";
    private static final String USER2 = "user2";
    private static final String USER3 = "user3";
    private static final String QUERY1 = "SELECT GETDATE()";
    private static final String QUERY2 = "SELECT 'date:'||CONVERT(VARCHAR(19),GETDATE())";
    private static final String USERS_FILTER = USER1 + USER_LIST_SEPARATOR + USER2;

    @DataPoint
    public static QueryPlan QP_SINGLE_CONNECTION = new QueryPlan(new Query(QUERY1, 12L, 0),
                                                                 new Query(QUERY2, 4L, 0),
                                                                 new Query(QUERY1, 15L, 0));
    @DataPoint
    public static QueryPlan QP_TWO_CONNECTIONS = new QueryPlan(new Query(QUERY1, 12L, 0),
                                                               new Query(QUERY2, 4L, 0),
                                                               new Query(QUERY1, 15L, 0),
                                                               new Query(QUERY2, 23L, 1));

    private Story story = new Story();
    private AgentContainerFixture fixture = new AgentContainerFixture();

    private LogString log = new LogString();
    private MadServerOperations madServerOperations = mock(MadServerOperations.class);
    private DefaultAdministrationServerConfiguration configuration;
    private final JdbcManager jdbcManager = mock(JdbcManager.class);
    private static final String RESOURCE_AGENT = Constants.MANAGE_RESOURCES_SERVICE_TYPE;


    @Before
    public void setUp() throws NotDeletedException {
        fixture.doSetUp();
        configuration = new DefaultAdministrationServerConfiguration();
        log.clear();
    }


    @After
    public void tearDown() throws Exception {
        fixture.doTearDown();
    }


    @Test
    public void test_initServices() throws Exception {
        configuration.setRecordHandlerStatistics(true);
        configuration.setRecordJdbcStatistics(true);
        configuration.setRecordMemoryUsage(true);
        startServiceManagerAgent();

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).addHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });
        story.record()
              .addAssert(verifySetConnectionFactories(true, null));
        story.record()
              .assertContainsAgent(RESOURCE_AGENT);

        story.execute();
    }


    private Assertion verifySetConnectionFactories(final boolean setDefault, final String[] usersToSpy) {
        return verifySetConnectionFactories(setDefault, usersToSpy, 1);
    }


    private Assertion verifySetConnectionFactories(final boolean setDefault,
                                                   final String[] usersToSpy,
                                                   final int nbCallsToClear) {
        return new Assertion() {
            public void check() throws Throwable {
                verify(jdbcManager, times(nbCallsToClear)).clearConnectionFactories();

                if ((usersToSpy == null) || (usersToSpy.length == 0)) {
                    if (setDefault) {
                        verify(jdbcManager).setDefaultConnectionFactory(isA(JdbcExecutionSpy.class));
                    }
                }
                else {
                    for (String user : usersToSpy) {
                        verify(jdbcManager, times(1)).setConnectionFactory(eq(user), any(JdbcExecutionSpy.class));
                    }
                }
            }
        };
    }


    @Test
    public void test_getServices() throws Exception {
        configuration.setDefaultAuditDestinationDir(AUDIT_DIR);
        configuration.setDefaultJdbcUsersFilter(USERS_FILTER);
        configuration.setRecordHandlerStatistics(true);
        configuration.setRecordJdbcStatistics(true);
        startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.QUERY,
                           RequestProtocol.QUERY,
                           new Aid("bebel"),
                           AdministrationOntology.GET_SERVICES_ACTION)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchContent(
                    "<list>\n"
                    + "  <string>" + ConfigurationOntology.AUDIT_DESTINATION_DIR + " " + AUDIT_DIR
                    + "</string>\n"
                    + "  <string>" + ConfigurationOntology.JDBC_USERS_FILTER + " " + USERS_FILTER
                    + "</string>\n"
                    + "  <string>" + ConfigurationOntology.RECORD_HANDLER_STATISTICS + " "
                    + AdministrationOntology.ENABLE_SERVICE_ACTION
                    + "</string>\n"
                    + "  <string>" + ConfigurationOntology.RECORD_JDBC_STATISTICS + " "
                    + AdministrationOntology.ENABLE_SERVICE_ACTION
                    + "</string>\n"
                    + "  <string>" + ConfigurationOntology.RECORD_MEMORY_USAGE + " "
                    + AdministrationOntology.DISABLE_SERVICE_ACTION
                    + "</string>\n"
                    + "</list>"));

        story.execute();
    }


    @Test
    public void test_enableService_recordHandlerStatistics() throws Exception {
        startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.ENABLE_SERVICE_ACTION + " "
                           + ConfigurationOntology.RECORD_HANDLER_STATISTICS)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).addHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });

        story.execute();
    }


    @Test
    public void test_enableService_recordJdbcStatistics() throws Exception {
        configuration.setJdbcUsersFilter(USERS_FILTER);
        AdministrationServerConfigurationAgent agent = startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.ENABLE_SERVICE_ACTION + " "
                           + ConfigurationOntology.RECORD_JDBC_STATISTICS)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .addAssert(verifySetConnectionFactories(false, new String[]{USER1, USER2}));

        story.execute();
    }


    @Test
    public void test_enableService_recordMemory() throws Exception {
        startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.ENABLE_SERVICE_ACTION + " "
                           + ConfigurationOntology.RECORD_MEMORY_USAGE)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .assertContainsAgent(RESOURCE_AGENT);

        story.execute();
    }


    @Test
    public void test_enableService_invalidArgument() throws Exception {
        startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.ENABLE_SERVICE_ACTION + " newService")
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.FAILURE),
                                         matchContent(
                                               "(Impossible d'activer le service 'newService' : service inconnu)")));

        story.execute();
    }


    @Test
    public void test_enableService_noArgument() throws Exception {
        startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.ENABLE_SERVICE_ACTION)
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.FAILURE),
                                         matchContent(
                                               "(Impossible d'activer le service : service non renseigné)")));

        story.execute();
    }


    @Test
    public void test_disableService_recordHandlerStatistics() throws Exception {
        configuration.setRecordHandlerStatistics(true);
        startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.DISABLE_SERVICE_ACTION + " "
                           + ConfigurationOntology.RECORD_HANDLER_STATISTICS)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).removeHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });

        story.execute();
    }


    @Test
    public void testUpdateJdbcUsersFilter_noUser() {
        testUpdateJdbcUsersFilter();
    }


    @Test
    public void testUpdateJdbcUsersFilter_user1() {
        testUpdateJdbcUsersFilter(USER1);
    }


    @Test
    public void testUpdateJdbcUsersFilter_user1_user2() {
        testUpdateJdbcUsersFilter(USER1, USER2);
    }


    @Test
    public void testUpdateJdbcUsersFilter_user1_user2_user3() {
        testUpdateJdbcUsersFilter(USER1, USER2, USER3);
    }


    @Test
    public void testUpdateJdbcUsersFilter_user2_user3() {
        testUpdateJdbcUsersFilter(USER2, USER3);
    }


    private void testUpdateJdbcUsersFilter(String... newUsers) {
        // preparation
        List<String> initialUserList = Arrays.asList(USER1, USER2);
        String oldFilter = join(initialUserList, USER_LIST_SEPARATOR);
        configuration.setJdbcUsersFilter(oldFilter);
        List<String> newUserList = Arrays.asList(newUsers);

        AdministrationServerConfigurationAgent agent = createAgent(new AdministrationLogFile());

        final List<JdbcExecutionSpy> execSpies = new ArrayList<JdbcExecutionSpy>();
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                execSpies.add((JdbcExecutionSpy)invocation.getArguments()[1]);
                return null;
            }
        }).when(jdbcManager).setConnectionFactory(any(String.class), any(JdbcExecutionSpy.class));

        // test
        configuration.setJdbcUsersFilter(join(newUserList, USER_LIST_SEPARATOR));
        agent.updateJdbcUsersFilter();

        // verifications
        verify(agent).updateJdbcUsersFilter();
        verifySetConnectionFactories(true, newUsers);
        verifyNoMoreInteractions(agent);

        // verify that a singleton spy is used, or null
        if (!execSpies.isEmpty()) {
            JdbcExecutionSpy spy = execSpies.get(0);
            for (JdbcExecutionSpy s : execSpies) {
                if (s != null) {
                    assertEquals(spy, s);
                }
            }
        }
    }


    @Test
    public void test_disableService_recordJdbcStatistics() throws Exception {
        configuration.setRecordJdbcStatistics(true);
        startServiceManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.DISABLE_SERVICE_ACTION + " "
                           + ConfigurationOntology.RECORD_JDBC_STATISTICS)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .addAssert(verifyAllSpiesAreDisabled(1));

        story.execute();
    }


    private Assertion verifyAllSpiesAreDisabled(int nbCallsToClear) {
        return verifySetConnectionFactories(false, null, nbCallsToClear);
    }


    @Test
    public void test_disableService_recordMemory() throws Exception {
        configuration.setRecordMemoryUsage(true);
        startServiceManagerAgent();
        story.record()
              .assertContainsAgent(RESOURCE_AGENT);

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.DISABLE_SERVICE_ACTION + " "
                           + ConfigurationOntology.RECORD_MEMORY_USAGE)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .assertNotContainsAgent(RESOURCE_AGENT);
        story.record()
              .assertNumberOfAgentWithService(0, RESOURCE_AGENT);

        story.execute();
    }


    @Test
    public void test_changeLogDir() throws Exception {
        final AdministrationLogFile logFile = mock(AdministrationLogFile.class);

        startServiceManagerAgent(logFile);

        final String logDir = "C:\\dev\\tmp";

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.CHANGE_LOG_DIR + " " + logDir)
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(
                    matchPerformative(Performative.INFORM),
                    matchContent("<string>" + logDir + "</string>")));

        story.record().addAssert(new Assertion() {
            public void check() throws Throwable {
                Assert.assertEquals(logDir, configuration.getAuditDestinationDir());
                Mockito.verify(logFile).init(logDir + "\\audit.log");
            }
        });

        story.execute();
    }


    @Test
    public void test_restoreLogDir() throws Exception {
        final AdministrationLogFile logFile = mock(AdministrationLogFile.class);
        final String defaultLogDir = "C:\\dev\\tmp";
        configuration.setDefaultAuditDestinationDir(defaultLogDir);
        configuration.setAuditDestinationDir("anotherLogDir");
        startServiceManagerAgent(logFile);

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.RESTORE_LOG_DIR)
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(
                    matchPerformative(Performative.INFORM),
                    matchContent("<string>" + defaultLogDir + "</string>")));

        story.record().addAssert(new Assertion() {
            public void check() throws Throwable {
                Assert.assertEquals(defaultLogDir, configuration.getAuditDestinationDir());
                Mockito.verify(logFile).init(defaultLogDir + "\\audit.log");
            }
        });

        story.execute();
    }


    @Test
    public void test_changeJdbcUsersFilter() throws Exception {
        doTestJdbcUsersFilter(CHANGE_JDBC_USERS_FILTER + " " + USERS_FILTER, USERS_FILTER, null);
    }


    @Test
    public void test_restoreJdbcUsersFilter() throws Exception {
        configuration.setDefaultJdbcUsersFilter(USERS_FILTER);
        configuration.setJdbcUsersFilter(USER3);

        doTestJdbcUsersFilter(RESTORE_JDBC_USERS_FILTER, USERS_FILTER, null);
    }


    @Theory
    public void test_JdbcUsersFilter_spyUser1(QueryPlan user1QueryPlan) throws Exception {
        test_JdbcUsersFilter(USER1, user1QueryPlan);
    }


    @Test
    public void test_JdbcUsersFilter_spyUser2() throws Exception {
        test_JdbcUsersFilter(USER2, null);
    }


    @Test
    public void test_JdbcUsersFilter_noSpy() throws Exception {
        test_JdbcUsersFilter(null, null);
    }


    private void test_JdbcUsersFilter(String userToSpy, QueryPlan user1QueryPlan) throws Exception {
        Log.info("*** starting test_JdbcUsersFilter " + userToSpy);
        if (userToSpy == null) {
            doTestJdbcUsersFilter(CHANGE_JDBC_USERS_FILTER, null, null);
        }
        else {
            doTestJdbcUsersFilter(CHANGE_JDBC_USERS_FILTER + " " + userToSpy, userToSpy, user1QueryPlan);
        }
    }


    private void doTestJdbcUsersFilter(String messageContent, final String expectedJdbcUsersFilter, QueryPlan queryPlan)
          throws Exception {
        queryPlan = (queryPlan == null) ? QP_SINGLE_CONNECTION : queryPlan;

        final LogString log = new LogString();
        final AdministrationLogFileMock logFile = new AdministrationLogFileMock(log, ALL_COLUMNS);
        final AdministrationServerConfigurationAgent agent = startServiceManagerAgent(logFile);
        final String oldFilter = configuration.getJdbcUsersFilter();

        TesterAgentRecorder tar = story.record().startTester("gui-agent");
        if (messageContent != null) {
            tar.sendMessage(Performative.REQUEST,
                            RequestProtocol.REQUEST,
                            new Aid("bebel"),
                            messageContent)
                  .then()
                  .receiveMessage()
                  .assertReceivedMessage(and(
                        matchPerformative(Performative.INFORM),
                        matchContent((expectedJdbcUsersFilter == null) ?
                                     "<null/>" :
                                     "<string>" + expectedJdbcUsersFilter + "</string>")));
        }

        Query[] queries = queryPlan.queries;
        MockTimeSource timeSource = new MockTimeSource();
        story.record().addAction(new ConnectionAction(logFile, USER1, timeSource, queries));
        story.execute();

        Assert.assertEquals(expectedJdbcUsersFilter, configuration.getJdbcUsersFilter());

        //
        // checks logs for the single connection
        //
        Set<Integer> connectionsIds = queryPlan.getConnectionIds();
        List<String> lines;
        StringBuilder errors = new StringBuilder();
        for (int connectionId : connectionsIds) {
            int nbUniqueQueries = (connectionId == 0) ? 2 : 1; // not generic regarding QueryPlans
            lines = new ArrayList<String>(Arrays.asList(logFile.extractLines(1 + nbUniqueQueries)));
            StringBuilder connectionErrors = new StringBuilder();

            Statistics query1Stats = queryPlan.computeStats(QUERY1, connectionId);
            Statistics query2Stats = queryPlan.computeStats(QUERY2, connectionId);

            if (USER1.equals(expectedJdbcUsersFilter)) {
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
            }
            long expectedWhen = (connectionId == 0) ? 0 : 78; // not generic regarding QueryPlans
            int value = (connectionId == 0) ? 0 : 1; // not generic regarding QueryPlans
            findLine(lines,
                     "write\\(JDBC, Temps Total, " + formatDate(expectedWhen) + ", " + value + ", " + USER1 + ", .*\\)",
                     true,
                     connectionErrors);

            if (connectionErrors.length() > 0) {
                errors.append("=== Errors on connection #" + connectionId + " ===\n");
                errors.append(connectionErrors);
            }
        }
        failOnErrors(errors);

        //
        // checks logs for user's ConnectionPool (compound of connections)
        //
        logFile.assertLine(
              "write(JDBC, Aggregated statistics for user '" + USER1 + "', 2 unique queries)");
        logFile.assertLine("write(JDBC, Aggregated statistics, Order (highest Total time first), "
                           + "User, Min time, Max time, Total time, Count, Query)");

        lines = new ArrayList<String>(Arrays.asList(logFile.extractLines(2)));

        errors.setLength(0); // clear
        if (USER1.equals(expectedJdbcUsersFilter)) {
            findUser1ConnectionPoolStats(lines, errors, 0, QUERY1, queryPlan.computeStats(
                  QUERY1, null));
            findUser1ConnectionPoolStats(lines, errors, 1, QUERY2, queryPlan.computeStats(
                  QUERY2, null));
        }
        failOnErrors(errors);

        logFile.assertNoMoreLines();
    }


    private static class QueryPlan {
        private final Query[] queries;


        public QueryPlan(Query... queries) {
            this.queries = queries;
        }


        public Set<Integer> getConnectionIds() {
            Set<Integer> result = new TreeSet<Integer>();
            for (Query query : queries) {
                result.add(query.connectionId);
            }
            return result;
        }


        public Statistics computeStats(String query1, Integer connectionId)
              throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
            Statistics result = new Statistics();

            for (int i = 0; i < queries.length; i++) {
                Query query = queries[i];
                if (query.query.equals(query1) && ((connectionId == null) || (connectionId.intValue()
                                                                              == query.connectionId))) {
                    result.inc();
                    result.addTime(queries[i].time);
                }
            }

            return result;
        }
    }

    static class Query {
        private final String query;
        private final long time;
        final int connectionId;


        public Query(String query, long time, int connectionId) {
            this.query = query;
            this.time = time;
            this.connectionId = connectionId;
        }
    }


    private void failOnErrors(StringBuilder errors) {
        if (errors.length() > 0) {
            fail("There was errors:\n" + errors.toString());
        }
    }


    private void findUser1ConnectionPoolStats(List<String> lines, StringBuilder errors, int order, String query,
                                              Statistics queryStats) {
        findLine(lines,
                 "write(JDBC, Aggregated statistics, " + order + ", " + USER1 +
                 ", " + queryStats.getMinTime() + ", " + queryStats.getMaxTime() + ", " + queryStats.getTime() +
                 ", " + queryStats.getCount() + ", " + query + ")",
                 false,
                 errors);
    }


    private void findUser1ConnectionStats(List<String> lines,
                                          StringBuilder errors,
                                          String query,
                                          Statistics queryStats, long expectedWhen, int expectedSpyId) {
        String expectedDateString = Matcher.quoteReplacement(AdministrationLogFile.formatDate(expectedWhen));
        findLine(lines,
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


    private static void findLine(List<String> lines, String expectedLine, boolean isPattern, StringBuilder errors) {
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

        if (index < 0) {
            if (errors.length() == 0) {
                errors.append("Actual lines:\n");
                for (int i = 0; i < lines.size(); i++) {
                    errors.append("\tLine L+").append(i).append(": ").append(lines.get(i)).append('\n');
                }

                errors.append("Expected lines:\n");
            }
            if (isPattern) {
                errors.append("\tLine with pattern '");
            }
            else {
                errors.append("\tLine '");
            }
            errors.append(expectedLine).append("' not found\n");
        }
        else {
            lines.remove(index);
        }
    }


    private class ConnectionAction implements AgentContainerFixture.Runnable {
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
                manager.setDefaultConnectionFactory(new JdbcExecutionSpy(logFile, manager, timeSource));

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

                        LOG.info("Connection #" + query.connectionId + ": Executing query " + query.query);
                        executeQuery(connection, query);
                    }

                    timeSource.setAutoIncrement(0);
                }
                finally {
                    int[] connectionIds = new int[connections.size()];
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

            timeSource.setAutoIncrement(query.time);
            ResultSet resultSet = statement.executeQuery(query.query);

            statement.close();
            resultSet.close();
        }
    }


    @Test
    public void test_killAgent() throws Exception {
        configuration.setRecordHandlerStatistics(true);
        configuration.setRecordJdbcStatistics(true);
        configuration.setRecordMemoryUsage(true);
        startServiceManagerAgent();

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).addHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });
        story.record()
              .addAssert(verifySetConnectionFactories(true, null));
        story.record()
              .assertContainsAgent(RESOURCE_AGENT);

        story.record().addAction(killAgent("bebel"));

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).removeHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });
        story.record()
              .addAssert(verifyAllSpiesAreDisabled(2));
        story.record()
              .assertNumberOfAgentWithService(0, RESOURCE_AGENT);
        story.record()
              .assertNotContainsAgent(RESOURCE_AGENT);

        story.execute();
    }


    private AgentContainerFixture.Runnable killAgent(final String agentName) {
        return new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                story.getAgent(agentName).kill();
            }
        };
    }


    private AdministrationServerConfigurationAgent startServiceManagerAgent() throws Exception {
        return startServiceManagerAgent(new AdministrationLogFile());
    }


    private AdministrationServerConfigurationAgent startServiceManagerAgent(AdministrationLogFile logFile) {
        AdministrationServerConfigurationAgent agent = createAgent(logFile);
        story.record()
              .startAgent("bebel", agent);
        story.record()
              .assertAgentWithService(new String[]{"bebel"}, Constants.MANAGE_SERVICE_TYPE);
        return agent;
    }


    private AdministrationServerConfigurationAgent createAgent(AdministrationLogFile logFile) {
        DefaultLogReader logReader = new DefaultLogReader(configuration.getAuditDestinationDir());

        AdministrationServerConfigurationAgent agent = new AdministrationServerConfigurationAgent(configuration,
                                                                                                  madServerOperations,
                                                                                                  logFile,
                                                                                                  logReader,
                                                                                                  jdbcManager);
        AdministrationServerConfigurationAgent result = spy(agent);
        doCallRealMethod().when(result).updateJdbcUsersFilter();
        return result;
    }
}

