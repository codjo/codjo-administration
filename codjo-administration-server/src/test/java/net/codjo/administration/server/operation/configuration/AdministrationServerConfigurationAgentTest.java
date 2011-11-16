package net.codjo.administration.server.operation.configuration;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.administration.common.Constants;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.mad.HandlerExecutionSpy;
import net.codjo.administration.server.operation.log.DefaultLogReader;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Aid;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchContent;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.test.AgentAssert.Assertion;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.Story;
import net.codjo.mad.server.plugin.MadServerOperations;
import net.codjo.test.common.Directory.NotDeletedException;
import net.codjo.test.common.LogString;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.isA;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AdministrationServerConfigurationAgentTest {
    private Story story = new Story();
    private AgentContainerFixture fixture = new AgentContainerFixture();

    private LogString log = new LogString();
    private MadServerOperations madServerOperations = mock(MadServerOperations.class);
    private DefaultAdministrationServerConfiguration configuration;
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
        configuration.setRecordMemoryUsage(true);
        startServiceManagerAgent();

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).addHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });
        story.record()
              .assertContainsAgent(RESOURCE_AGENT);

        story.execute();
    }


    @Test
    public void test_getServices() throws Exception {
        configuration.setDefaultAuditDestinationDir("c:\\dev\\temp\\test");
        configuration.setRecordHandlerStatistics(true);
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
                    + "  <string>" + ConfigurationOntology.AUDIT_DESTINATION_DIR + " " + "c:\\dev\\temp\\test"
                    + "</string>\n"
                    + "  <string>" + ConfigurationOntology.RECORD_HANDLER_STATISTICS + " "
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
    public void test_enableService_noArgument() throws Exception {
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
    public void test_enableService_invalidArgument() throws Exception {
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
    public void test_killAgent() throws Exception {
        configuration.setRecordHandlerStatistics(true);
        configuration.setRecordMemoryUsage(true);
        startServiceManagerAgent();

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      verify(madServerOperations).addHandlerListener(isA(HandlerExecutionSpy.class));
                  }
              });
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


    private void startServiceManagerAgent() throws Exception {
        startServiceManagerAgent(new AdministrationLogFile());
    }


    private void startServiceManagerAgent(AdministrationLogFile logFile) {
        DefaultLogReader logReader = new DefaultLogReader(configuration.getAuditDestinationDir());
        story.record()
              .startAgent("bebel",
                          new AdministrationServerConfigurationAgent(configuration,
                                                                     madServerOperations,
                                                                     logFile,
                                                                     logReader));
        story.record()
              .assertAgentWithService(new String[]{"bebel"}, Constants.MANAGE_SERVICE_TYPE);
    }
}
