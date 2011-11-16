package net.codjo.administration.gui.plugin;
import net.codjo.administration.common.AdministrationOntology;
import static net.codjo.administration.common.Constants.MANAGE_LOGS_SERVICE_TYPE;
import static net.codjo.administration.common.Constants.MANAGE_PLUGINS_SERVICE_TYPE;
import static net.codjo.administration.common.Constants.MANAGE_SERVICE_TYPE;
import static net.codjo.administration.common.Constants.MANAGE_SYSTEM_PROPERTIES_TYPE;
import static net.codjo.administration.gui.plugin.ActionType.CHANGE_LOG_DIR;
import static net.codjo.administration.gui.plugin.ActionType.CLOSE;
import static net.codjo.administration.gui.plugin.ActionType.ENABLE_SERVICE;
import static net.codjo.administration.gui.plugin.ActionType.READ_LOG;
import static net.codjo.administration.gui.plugin.ActionType.RESTORE_LOG_DIR;
import static net.codjo.administration.gui.plugin.ActionType.START_PLUGIN;
import static net.codjo.administration.gui.plugin.ActionType.STOP_PLUGIN;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.GuiEvent;
import static net.codjo.agent.MessageTemplate.matchContent;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.test.AgentAssert.Assertion;
import static net.codjo.agent.test.AgentAssert.logAndClear;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.AgentContainerFixture.Runnable;
import net.codjo.agent.test.ReceiveMessageStep;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.StoryPart;
import net.codjo.agent.test.TesterAgentRecorder;
import net.codjo.test.common.LogString;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AdministrationGuiAgentTest {
    private static final String PLUGIN_TESTER_AGENT = MANAGE_PLUGINS_SERVICE_TYPE + "-tester";
    private static final String LOG_TESTER_AGENT = MANAGE_LOGS_SERVICE_TYPE + "-tester";
    private static final String SERVICE_TESTER_AGENT = MANAGE_SERVICE_TYPE + "-tester";
    private static final String SYSTEM_PROPERTIES_TESTER_AGENT = MANAGE_SYSTEM_PROPERTIES_TYPE + "-tester";
    private static final String GUI_AGENT = "gui-agent";
    private Story story = new Story();
    private LogString log = new LogString();
    private HandlerMock handlerMock = new HandlerMock(log);


    @Test
    public void test_query_plugins() throws Exception {
        initManagePluginsAgent();
        story.record().assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        initAndCheckSilentAgent(LOG_TESTER_AGENT, MANAGE_LOGS_SERVICE_TYPE);
        initAndCheckSilentAgent(SERVICE_TESTER_AGENT, MANAGE_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);

        story.record().startAgent(GUI_AGENT, new AdministrationGuiAgent(handlerMock));

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handlePluginsReceived(workflow)");

        story.execute();
    }


    @Test
    public void test_query_plugins_error() throws Exception {
        story.record().startAgent(GUI_AGENT, new AdministrationGuiAgent(handlerMock));

        story.record().startTester(LOG_TESTER_AGENT)
              .registerToDF(MANAGE_LOGS_SERVICE_TYPE);
        story.record().assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        registerManageServiceAgent();

        recordAssertLog(
              "handleError(Impossible de trouver l'agent gérant les plugins (absent de la plateforme))");

        story.execute();
    }


    @Test
    public void test_close() throws Exception {
        story.record()
              .startTester(PLUGIN_TESTER_AGENT)
              .registerToDF(MANAGE_PLUGINS_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        story.record()
              .startTester(LOG_TESTER_AGENT)
              .registerToDF(MANAGE_LOGS_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        story.record()
              .startTester(SYSTEM_PROPERTIES_TESTER_AGENT)
              .registerToDF(MANAGE_SYSTEM_PROPERTIES_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_SYSTEM_PROPERTIES_TYPE);

        registerManageServiceAgent();

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record()
              .startAgent(GUI_AGENT, agent);
        story.record()
              .assertContainsAgent(GUI_AGENT);

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, CLOSE.ordinal());
                      agent.postGuiEvent(guiEvent);
                  }
              });

        story.record()
              .assertNotContainsAgent(GUI_AGENT);

        story.execute();
    }


    @Test
    public void test_dieProperly_afterException() throws Exception {
        story.record()
              .startTester(PLUGIN_TESTER_AGENT)
              .registerToDF(MANAGE_PLUGINS_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        story.record()
              .startTester(LOG_TESTER_AGENT)
              .registerToDF(MANAGE_LOGS_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        story.record()
              .startTester(SYSTEM_PROPERTIES_TESTER_AGENT)
              .registerToDF(MANAGE_SYSTEM_PROPERTIES_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_SYSTEM_PROPERTIES_TYPE);

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record()
              .startAgent(GUI_AGENT, agent);
        story.record()
              .assertContainsAgent(GUI_AGENT);

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, READ_LOG.ordinal());
                      Object wrongParameterType = new Object();
                      guiEvent.addParameter(wrongParameterType);
                      agent.postGuiEvent(guiEvent);
                  }
              });

        story.record()
              .assertNotContainsAgent(GUI_AGENT);

        story.record()
              .addAssert(new Assertion() {
                  public void check() throws Throwable {
                      assertTrue(log.getContent().endsWith("handleAgentDead()"));
                  }
              });

        story.execute();
    }


    @Test
    public void test_startPlugin() throws Exception {
        initManagePluginsAgent()
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.REQUEST))
              .assertReceivedMessage(matchProtocol(RequestProtocol.REQUEST))
              .assertReceivedMessage(matchContent(AdministrationOntology.START_PLUGIN_ACTION + " workflow"))
              .replyWith(Performative.INFORM, "Done");
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        initAndCheckSilentAgent(LOG_TESTER_AGENT, MANAGE_LOGS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        registerManageServiceAgent();

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record()
              .startAgent(GUI_AGENT, agent);
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handlePluginsReceived(workflow)");

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, START_PLUGIN.ordinal());
                      guiEvent.addParameter("workflow");
                      agent.postGuiEvent(guiEvent);
                  }
              });

        recordAssertLog("handleActionStarted(), "
                        + "handlePluginStarted(workflow)");

        story.execute();
    }


    @Test
    public void test_startPlugin_failure() throws Exception {
        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        Runnable runnable = new Runnable() {
            public void run() throws Exception {
                GuiEvent guiEvent = new GuiEvent(this, START_PLUGIN.ordinal());
                guiEvent.addParameter("workflow");
                agent.postGuiEvent(guiEvent);
            }
        };

        recordAssertFailure(agent, runnable);
    }


    @Test
    public void test_stopPlugin() throws Exception {
        initManagePluginsAgent()
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.REQUEST))
              .assertReceivedMessage(matchProtocol(RequestProtocol.REQUEST))
              .assertReceivedMessage(matchContent(AdministrationOntology.STOP_PLUGIN_ACTION + " workflow"))
              .replyWith(Performative.INFORM, "Done");
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        initAndCheckSilentAgent(LOG_TESTER_AGENT, MANAGE_LOGS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        registerManageServiceAgent();

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record()
              .startAgent(GUI_AGENT, agent);
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handlePluginsReceived(workflow)");

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, STOP_PLUGIN.ordinal());
                      guiEvent.addParameter("workflow");
                      agent.postGuiEvent(guiEvent);
                  }
              });

        recordAssertLog("handleActionStarted(), "
                        + "handlePluginStopped(workflow)");

        story.execute();
    }


    @Test
    public void test_stopPlugin_failure() throws Exception {
        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        Runnable runnable = new Runnable() {
            public void run() throws Exception {
                GuiEvent guiEvent = new GuiEvent(this, STOP_PLUGIN.ordinal());
                guiEvent.addParameter("workflow");
                agent.postGuiEvent(guiEvent);
            }
        };

        recordAssertFailure(agent, runnable);
    }


    @Test
    public void test_query_logs() throws Exception {
        initAndCheckSilentAgent(PLUGIN_TESTER_AGENT, MANAGE_PLUGINS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        initManageLogsAgent()
              .assertReceivedMessage(matchPerformative(Performative.QUERY))
              .assertReceivedMessage(matchProtocol(RequestProtocol.QUERY))
              .assertReceivedMessage(matchContent(AdministrationOntology.GET_LOG_FILES_ACTION))
              .replyWith(Performative.INFORM, "<list>"
                                              + "  <string>server.log</string>"
                                              + "  <string>mad.log</string>"
                                              + "</list>");
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        registerManageServiceAgent();

        story.record()
              .startAgent(GUI_AGENT, new AdministrationGuiAgent(handlerMock));
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleLogsReceived(server.log, mad.log)");

        story.execute();
    }


    @Test
    public void test_query_logs_error() throws Exception {
        story.record()
              .startTester(PLUGIN_TESTER_AGENT)
              .registerToDF(MANAGE_PLUGINS_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        story.record()
              .startTester(SYSTEM_PROPERTIES_TESTER_AGENT)
              .registerToDF(MANAGE_SYSTEM_PROPERTIES_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_SYSTEM_PROPERTIES_TYPE);

        registerManageServiceAgent();

        story.record()
              .startAgent(GUI_AGENT, new AdministrationGuiAgent(handlerMock));
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog(
              "handleError(Impossible de trouver l'agent gérant les logs (absent de la plateforme))");

        story.execute();
    }


    @Test
    public void test_readLog() throws Exception {
        initAndCheckSilentAgent(PLUGIN_TESTER_AGENT, MANAGE_PLUGINS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        initManageLogsAgent()
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.REQUEST))
              .assertReceivedMessage(matchProtocol(RequestProtocol.REQUEST))
              .assertReceivedMessage(matchContent(
                    AdministrationOntology.READ_LOG_ACTION + " toto.log"))
              .replyWith(Performative.INFORM, "<string>Ceci est le contenu d'un fichier de log.</string>");
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        registerManageServiceAgent();

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record()
              .startAgent(GUI_AGENT, agent);
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleLogsReceived(server.log, mad.log)");

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, READ_LOG.ordinal());
                      guiEvent.addParameter("toto.log");
                      agent.postGuiEvent(guiEvent);
                  }
              });

        recordAssertLog("handleActionStarted(), "
                        + "handleLogRead(Ceci est le contenu d'un fichier de log.)");

        story.execute();
    }


    @Test
    public void test_query_services() throws Exception {
        initAndCheckSilentAgent(PLUGIN_TESTER_AGENT, MANAGE_PLUGINS_SERVICE_TYPE);
        initAndCheckSilentAgent(LOG_TESTER_AGENT, MANAGE_LOGS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        initManageServicesAgent();
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_SERVICE_TYPE);

        story.record()
              .startAgent(GUI_AGENT, new AdministrationGuiAgent(handlerMock));
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleServicesReceived(recordHandlerStatistics.enable)");

        story.execute();
    }


    @Test
    public void test_query_services_error() throws Exception {

        story.record()
              .startTester(PLUGIN_TESTER_AGENT)
              .registerToDF(MANAGE_PLUGINS_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        story.record()
              .startTester(LOG_TESTER_AGENT)
              .registerToDF(MANAGE_LOGS_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        story.record()
              .startTester(SYSTEM_PROPERTIES_TESTER_AGENT)
              .registerToDF(MANAGE_SYSTEM_PROPERTIES_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_SYSTEM_PROPERTIES_TYPE);

        story.record()
              .startAgent(GUI_AGENT, new AdministrationGuiAgent(handlerMock));
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog(
              "handleError(Impossible de trouver l'agent gérant la configuration de plugin d'administration (absent de la plateforme))");

        story.execute();
    }


    @Test
    public void test_changeLogDir() throws Exception {
        initAndCheckSilentAgent(PLUGIN_TESTER_AGENT, MANAGE_PLUGINS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        initManageLogsAgent().then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.QUERY))
              .assertReceivedMessage(matchProtocol(RequestProtocol.QUERY))
              .assertReceivedMessage(matchContent(AdministrationOntology.GET_LOG_FILES_ACTION))
              .replyWith(Performative.INFORM, "<list>"
                                              + "  <string>audit.log</string>"
                                              + "</list>");
        story.record().assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        initManageServicesAgent().then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.REQUEST))
              .assertReceivedMessage(matchProtocol(RequestProtocol.REQUEST))
              .assertReceivedMessage(matchContent(
                    AdministrationOntology.CHANGE_LOG_DIR + " c:\\dev\\tempsLog"))
              .replyWith(Performative.INFORM, "<string>c:\\dev\\tempsLog</string>");
        story.record().assertNumberOfAgentWithService(1, MANAGE_SERVICE_TYPE);

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record().startAgent(GUI_AGENT, agent);
        story.record().assertContainsAgent(GUI_AGENT);

        story.record().addAssert(assertLogsReceived("server.log", "mad.log"));
        story.record().addAssert(assertServicesReceived("recordHandlerStatistics.enable"));
        story.record().addAction(clearLog());

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, CHANGE_LOG_DIR.ordinal());
                      guiEvent.addParameter("c:\\dev\\tempsLog");
                      agent.postGuiEvent(guiEvent);
                  }
              });

        recordAssertLog("handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleLogDirChanged(c:\\dev\\tempsLog), "
                        + "handleLogsReceived(audit.log)");

        story.execute();
    }


    @Test
    public void test_restoreLogDir() throws Exception {
        initAndCheckSilentAgent(PLUGIN_TESTER_AGENT, MANAGE_PLUGINS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        initManageLogsAgent().then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.QUERY))
              .assertReceivedMessage(matchProtocol(RequestProtocol.QUERY))
              .assertReceivedMessage(matchContent(AdministrationOntology.GET_LOG_FILES_ACTION))
              .replyWith(Performative.INFORM, "<list>"
                                              + "  <string>mad.log</string>"
                                              + "  <string>server.log</string>"
                                              + "</list>");
        story.record().assertNumberOfAgentWithService(1, MANAGE_LOGS_SERVICE_TYPE);

        initManageServicesAgent().then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.REQUEST))
              .assertReceivedMessage(matchProtocol(RequestProtocol.REQUEST))
              .assertReceivedMessage(matchContent(AdministrationOntology.RESTORE_LOG_DIR))
              .replyWith(Performative.INFORM, "<string>c:\\dev\\temp</string>");
        story.record().assertNumberOfAgentWithService(1, MANAGE_SERVICE_TYPE);

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record().startAgent(GUI_AGENT, agent);

        story.record().addAssert(assertLogsReceived("server.log", "mad.log"));
        story.record().addAssert(assertServicesReceived("recordHandlerStatistics.enable"));
        story.record().addAction(clearLog());

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, RESTORE_LOG_DIR.ordinal());
                      agent.postGuiEvent(guiEvent);
                  }
              });

        recordAssertLog("handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleLogDirChanged(c:\\dev\\temp), "
                        + "handleLogsReceived(mad.log, server.log)");

        story.execute();
    }


    @Test
    public void test_enableService() throws Exception {
        initManageServicesAgent()
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.REQUEST))
              .assertReceivedMessage(matchProtocol(RequestProtocol.REQUEST))
              .assertReceivedMessage(matchContent(
                    AdministrationOntology.ENABLE_SERVICE_ACTION + " recordHandlerStatistics"))
              .replyWith(Performative.INFORM, "Done");
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_SERVICE_TYPE);

        initAndCheckSilentAgent(PLUGIN_TESTER_AGENT, MANAGE_PLUGINS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        initAndCheckSilentAgent(LOG_TESTER_AGENT, MANAGE_LOGS_SERVICE_TYPE);

        final AdministrationGuiAgent agent = new AdministrationGuiAgent(handlerMock);
        story.record()
              .startAgent(GUI_AGENT, agent);
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleServicesReceived(recordHandlerStatistics.enable)");

        story.record()
              .addAction(new AgentContainerFixture.Runnable() {
                  public void run() throws Exception {
                      GuiEvent guiEvent = new GuiEvent(this, ENABLE_SERVICE.ordinal());
                      guiEvent.addParameter("recordHandlerStatistics");
                      agent.postGuiEvent(guiEvent);
                  }
              });

        recordAssertLog("handleActionStarted(), "
                        + "handleEnableService(recordHandlerStatistics)");

        story.execute();
    }


    private ReceiveMessageStep initManagePluginsAgent() {
        return story.record()
              .startTester(PLUGIN_TESTER_AGENT)
              .registerToDF(MANAGE_PLUGINS_SERVICE_TYPE)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.QUERY))
              .assertReceivedMessage(matchProtocol(RequestProtocol.QUERY))
              .assertReceivedMessage(matchContent(AdministrationOntology.GET_PLUGINS_ACTION))
              .replyWith(Performative.INFORM, "<list>"
                                              + "  <string>workflow</string>"
                                              + "</list>");
    }


    private ReceiveMessageStep initManageLogsAgent() {
        return story.record()
              .startTester(LOG_TESTER_AGENT).registerToDF(MANAGE_LOGS_SERVICE_TYPE).then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.QUERY))
              .assertReceivedMessage(matchProtocol(RequestProtocol.QUERY))
              .assertReceivedMessage(matchContent(AdministrationOntology.GET_LOG_FILES_ACTION))
              .replyWith(Performative.INFORM, "<list>"
                                              + "  <string>server.log</string>"
                                              + "  <string>mad.log</string>"
                                              + "</list>");
    }


    private ReceiveMessageStep initManageServicesAgent() {
        return story.record()
              .startTester(SERVICE_TESTER_AGENT).registerToDF(MANAGE_SERVICE_TYPE).then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.QUERY))
              .assertReceivedMessage(matchProtocol(RequestProtocol.QUERY))
              .assertReceivedMessage(matchContent(AdministrationOntology.GET_SERVICES_ACTION))
              .replyWith(Performative.INFORM, "<list>"
                                              + "  <string>recordHandlerStatistics.enable</string>"
                                              + "</list>");
    }


    private void initAndCheckSilentAgent(String agent, String serviceType) {
        story.record().startTester(agent).play(doNotReply(serviceType));
        story.record().assertNumberOfAgentWithService(1, serviceType);
    }


    private StoryPart doNotReply(final String serviceType) {
        return new StoryPart() {
            public void record(TesterAgentRecorder recorder) {
                recorder.registerToDF(serviceType)
                      .then()
                      .receiveMessage();
            }
        };
    }


    private void recordAssertLog(String expected) {
        story.record().addAssert(logAndClear(log, expected));
    }


    private void recordAssertFailure(AdministrationGuiAgent agent, Runnable runnable)
          throws ContainerFailureException {
        initManagePluginsAgent()
              .then()
              .receiveMessage()
              .replyWith(Performative.FAILURE, "Erreur !!!");
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_PLUGINS_SERVICE_TYPE);

        initAndCheckSilentAgent(LOG_TESTER_AGENT, MANAGE_LOGS_SERVICE_TYPE);
        initAndCheckSilentAgent(SYSTEM_PROPERTIES_TESTER_AGENT, MANAGE_SYSTEM_PROPERTIES_TYPE);
        registerManageServiceAgent();

        story.record()
              .startAgent(GUI_AGENT, agent);
        story.record()
              .assertContainsAgent(GUI_AGENT);

        recordAssertLog("handleAgentBorn(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handleActionStarted(), "
                        + "handlePluginsReceived(workflow)");

        story.record()
              .addAction(runnable);

        recordAssertLog("handleActionStarted(), "
                        + "handleError(Erreur !!!)");

        story.execute();
    }


    private void registerManageServiceAgent() {
        story.record()
              .startTester(SERVICE_TESTER_AGENT)
              .registerToDF(MANAGE_SERVICE_TYPE);
        story.record()
              .assertNumberOfAgentWithService(1, MANAGE_SERVICE_TYPE);
    }


    private Assertion assertLogsReceived(final String... expected) {
        return new Assertion() {
            public void check() throws Throwable {
                handlerMock.assertLogsReceived(expected);
            }
        };
    }


    private Assertion assertServicesReceived(final String... expected) {
        return new Assertion() {
            public void check() throws Throwable {
                handlerMock.assertServicesReceived(expected);
            }
        };
    }


    private Runnable clearLog() {
        return new Runnable() {
            public void run() throws Exception {
                log.clear();
            }
        };
    }
}
