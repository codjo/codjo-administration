package net.codjo.administration.server.operation.configuration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.Assert;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.administration.common.Constants;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.AdministrationLogFileMock;
import net.codjo.administration.server.audit.jdbc.JdbcExecutionSpy;
import net.codjo.administration.server.audit.jdbc.JdbcExecutionSpyTest;
import net.codjo.administration.server.audit.mad.HandlerExecutionSpy;
import net.codjo.administration.server.operation.log.DefaultLogReader;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Aid;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.test.AgentAssert.Assertion;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.ReceiveMessageStep;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.TesterAgentRecorder;
import net.codjo.mad.server.plugin.MadServerOperations;
import net.codjo.sql.server.JdbcManager;
import net.codjo.test.common.Directory.NotDeletedException;
import net.codjo.test.common.LogString;
import net.codjo.util.time.MockTimeSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static net.codjo.administration.common.AdministrationOntology.CHANGE_JDBC_USERS_FILTER;
import static net.codjo.administration.common.AdministrationOntology.RESTORE_JDBC_USERS_FILTER;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AdministrationServerConfigurationAgentTest {
    private static final Logger LOG = Logger.getLogger(AdministrationServerConfigurationAgentTest.class);

    private static final String AUDIT_DIR = "c:\\dev\\temp\\test";
    public static final String USER1 = "user1";
    public static final String USER2 = "user2";
    private static final String USER3 = "user3";
    public static final String QUERY1 = "SELECT GETDATE()";
    public static final String QUERY2 = "SELECT 'date:'||CONVERT(VARCHAR(19),GETDATE())";
    private static final String USERS_FILTER = USER1 + USER_LIST_SEPARATOR + USER2;

    private Story story = new Story();
    private AgentContainerFixture fixture = new AgentContainerFixture();

    private LogString log = new LogString();
    private MadServerOperations madServerOperations = mock(MadServerOperations.class);
    DefaultAdministrationServerConfiguration configuration;
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
        startServiceManagerAgent(true);

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).addHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });
        story.record()
              .addAssert(assertSetConnectionFactories(true, null));
        story.record()
              .assertContainsAgent(RESOURCE_AGENT);

        story.execute();
    }


    public Assertion assertSetConnectionFactories(final boolean setDefault, final String[] usersToSpy) {
        return assertSetConnectionFactories(setDefault, usersToSpy, 1);
    }


    private Assertion assertSetConnectionFactories(final boolean setDefault,
                                                   final String[] usersToSpy,
                                                   final int nbCallsToClear) {
        return new Assertion() {
            public void check() throws Throwable {
                verifySetConnectionFactories(setDefault, usersToSpy, nbCallsToClear);
            }
        };
    }


    public void verifySetConnectionFactories(final boolean setDefault,
                                             final String[] usersToSpy,
                                             final int nbCallsToClear) {
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
              .addAssert(assertSetConnectionFactories(false, new String[]{USER1, USER2}));

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
        assertSetConnectionFactories(true, newUsers);
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
        startServiceManagerAgent(true);

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
              .addAssert(assertAllSpiesAreDisabled(2));

        story.execute();
    }


    private Assertion assertAllSpiesAreDisabled(int nbCallsToClear) {
        return assertSetConnectionFactories(false, null, nbCallsToClear);
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

        startServiceManagerAgent(logFile, false); //needsInitInteractions=false

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
        startServiceManagerAgent(logFile, false); //needsInitInteractions=false

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
        doTestJdbcUsersFilter(CHANGE_JDBC_USERS_FILTER + " " + USERS_FILTER, USERS_FILTER, false, null);
    }


    @Test
    public void test_restoreJdbcUsersFilter() throws Exception {
        configuration.setDefaultJdbcUsersFilter(USERS_FILTER);
        configuration.setJdbcUsersFilter(USER3);

        doTestJdbcUsersFilter(RESTORE_JDBC_USERS_FILTER, USERS_FILTER, false, null);
    }


    @Test
    public void test_JdbcUsersFilter_spyUser1() throws Exception {
        test_JdbcUsersFilter(USER1, false, true);
    }


    @Test
    public void test_JdbcUsersFilter_spyUser1_unusedPoolForUser2() throws Exception {
        test_JdbcUsersFilter(USER1, true, true);
    }


    @Test
    public void test_JdbcUsersFilter_spyUser2() throws Exception {
        test_JdbcUsersFilter(USER2, false, true);
    }


    @Test
    public void test_JdbcUsersFilter_spyAll() throws Exception {
        test_JdbcUsersFilter(null, false, true);
    }


    @Test
    public void test_JdbcUsersFilter_spyDisabled() throws Exception {
        test_JdbcUsersFilter(null, false, false);
    }


    private void test_JdbcUsersFilter(String userToSpy, boolean createUnusedPoolForUser2, boolean enabled)
          throws Exception {
        LOG.info("*** starting test_JdbcUsersFilter " + userToSpy);
        if (userToSpy == null) {
            // spy all users
            doTestJdbcUsersFilter(CHANGE_JDBC_USERS_FILTER, null, createUnusedPoolForUser2, enabled);
        }
        else {
            // spy only the user given by userToSpy
            doTestJdbcUsersFilter(CHANGE_JDBC_USERS_FILTER + " " + userToSpy,
                                  userToSpy,
                                  createUnusedPoolForUser2,
                                  enabled);
        }
    }


    private void doTestJdbcUsersFilter(String messageContent,
                                       final String expectedJdbcUsersFilter,
                                       boolean createUnusedPoolForUser2, final Boolean expectServiceActivated)
          throws Exception {
        if (expectServiceActivated != null) {
            configuration.setRecordJdbcStatistics(expectServiceActivated);
        }

        final LogString log = new LogString();
        final AdministrationLogFileMock logFile = new AdministrationLogFileMock(log, ALL_COLUMNS);
        startServiceManagerAgent(logFile, false); //needsInitInteractions=false

        TesterAgentRecorder tar = story.record().startTester("gui-agent");

        if (expectServiceActivated != null) {
            addAssertServiceActivated(story, expectServiceActivated);
        }

        if (messageContent != null) {
            addSendMessageAndCheckReceivedSteps(messageContent, tar, expectedJdbcUsersFilter);
        }

        addConnectionAction(story, logFile, USER1, JdbcExecutionSpyTest.QP_SINGLE_CONNECTION.getQueries());
        if (createUnusedPoolForUser2) {
            addConnectionAction(story, logFile, USER2); // queries.length==0
        }
        story.execute();
        Assert.assertEquals(expectedJdbcUsersFilter, configuration.getJdbcUsersFilter());

        InOrder orderedExecution = Mockito.inOrder(jdbcManager);
        orderedExecution.verify(jdbcManager, times(1)).clearConnectionFactories();
        orderedExecution.verify(jdbcManager, times(1)).clearConnectionPoolListeners();
        ArgumentCaptor<JdbcExecutionSpy> argument = ArgumentCaptor.forClass(JdbcExecutionSpy.class);
        if (spyAllUsers()) {
            orderedExecution.verify(jdbcManager, times(1)).setDefaultConnectionFactory(argument.capture());
            orderedExecution.verify(jdbcManager, times(1)).addConnectionPoolListener(argument.capture());
        }
        verifyUserIsSpiedWhenEnabled(orderedExecution, argument, USER1);
        verifyUserIsSpiedWhenEnabled(orderedExecution, argument, USER2);
        verifySingleInstanceOrNone(argument);
        orderedExecution.verifyNoMoreInteractions();
    }


    private void verifySingleInstanceOrNone(ArgumentCaptor<JdbcExecutionSpy> argument) {
        for (int i = 1; i < argument.getAllValues().size(); i++) {
            assertEquals(argument.getAllValues().get(i - 1), argument.getAllValues().get(i));
        }
    }


    private void verifyUserIsSpiedWhenEnabled(InOrder orderedExecution,
                                              ArgumentCaptor<JdbcExecutionSpy> argument,
                                              String user) {
        if (spyUser(user)) {
            if (!spyAllUsers()) {
                orderedExecution.verify(jdbcManager, times(1)).setConnectionFactory(eq(user), argument.capture());
                orderedExecution.verify(jdbcManager, times(1)).addConnectionPoolListener(argument.capture(), eq(user));
            }
        }
    }


    private boolean spyAllUsers() {
        return configuration.isRecordJdbcStatistics() && StringUtils.isBlank(configuration.getJdbcUsersFilter());
    }


    @Test
    public void test_killAgent() throws Exception {
        configuration.setRecordHandlerStatistics(true);
        configuration.setRecordJdbcStatistics(true);
        configuration.setRecordMemoryUsage(true);
        startServiceManagerAgent(true); //needsInitInteractions=true

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).addHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });
        story.record()
              .addAssert(assertSetConnectionFactories(true, null));
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
              .addAssert(assertAllSpiesAreDisabled(2));
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
        return startServiceManagerAgent(false); //needsInitInteractions=false
    }


    private AdministrationServerConfigurationAgent startServiceManagerAgent(boolean needsInitInteractions)
          throws Exception {
        return startServiceManagerAgent(new AdministrationLogFile(), needsInitInteractions);
    }


    AdministrationServerConfigurationAgent startServiceManagerAgent(AdministrationLogFile logFile,
                                                                    boolean needsInitInteractions) {
        AdministrationServerConfigurationAgent agent = createAgent(logFile);
        story.record()
              .startAgent("bebel", agent);
        story.record()
              .assertAgentWithService(new String[]{"bebel"}, Constants.MANAGE_SERVICE_TYPE);

        if (!needsInitInteractions) {
            story.record().addAction(new AgentContainerFixture.Runnable() {
                public void run() {
                    // clear interactions with the mock at initialization time
                    Mockito.reset(jdbcManager);
                }
            });
        }
        return agent;
    }


    private AdministrationServerConfigurationAgent createAgent(AdministrationLogFile logFile) {
        DefaultLogReader logReader = new DefaultLogReader(configuration.getAuditDestinationDir());

        AdministrationServerConfigurationAgent agent = new AdministrationServerConfigurationAgent(configuration,
                                                                                                  madServerOperations,
                                                                                                  logFile,
                                                                                                  logReader,
                                                                                                  jdbcManager);
        return spy(agent);
    }


    private void addAssertServiceActivated(Story story,
                                           final Boolean expectServiceActivated) {
        story.record().addAssert(new Assertion() {
            public void check() throws Throwable {
                assertEquals("recordJdbcStatistics",
                             expectServiceActivated,
                             configuration.isRecordJdbcStatistics());
            }


            @Override
            public String toString() {
                return "assertRecordJdbcStatistics(enabled=" + expectServiceActivated + ")";
            }
        });
    }


    private boolean spyUser(String user) {
        String filter = configuration.getJdbcUsersFilter();
        return configuration.isRecordJdbcStatistics() && (spyAllUsers() || user.equals(filter));
    }


    private void addConnectionAction(Story story,
                                     final AdministrationLogFileMock logFile,
                                     final String user,
                                     final Query... queries) {
        final MockTimeSource timeSource = new MockTimeSource();
        story.record().addAction(new ConnectionAction(logFile, user, timeSource, queries));
    }


    private void addSendMessageAndCheckReceivedSteps(String messageContent,
                                                     TesterAgentRecorder tar,
                                                     String expectedJdbcUsersFilter) {
        ReceiveMessageStep step = tar.sendMessage(Performative.REQUEST,
                                                  RequestProtocol.REQUEST,
                                                  new Aid("bebel"),
                                                  messageContent)
              .then()
              .receiveMessage();
        step.assertReceivedMessage(and(
              matchPerformative(Performative.INFORM),
              matchContent((expectedJdbcUsersFilter == null) ?
                           "<null/>" :
                           "<string>" + expectedJdbcUsersFilter + "</string>")));
    }


    public void failOnErrors(Errors errors, AdministrationLogFileMock logFile) {
        if (!errors.isEmpty()) {
            StringBuilder buffer = new StringBuilder("There was errors:\n");
            errors.appendTo(buffer);
            logFile.appendLinesTo(buffer);
            fail(buffer.toString());
        }
    }
}

