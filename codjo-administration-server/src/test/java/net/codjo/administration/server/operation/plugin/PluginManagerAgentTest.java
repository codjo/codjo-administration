package net.codjo.administration.server.operation.plugin;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.Constants;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Aid;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchContent;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.Story;
import net.codjo.test.common.LogString;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PluginManagerAgentTest {
    private Story story = new Story();
    private LogString log = new LogString();


    @Test
    public void test_getPlugins() throws Exception {
        startPluginManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.QUERY,
                           RequestProtocol.QUERY,
                           new Aid("bebel"),
                           AdministrationOntology.GET_PLUGINS_ACTION)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchContent(
                    "<list>\n"
                    + "  <string>SecurityManagerPlugin</string>\n"
                    + "  <string>WorkflowServerPlugin</string>\n"
                    + "</list>"));

        story.record()
              .addAssert(AgentAssert.log(log, "getPlugins()"));

        story.execute();
    }


    @Test
    public void test_startPlugin() throws Exception {
        startPluginManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.START_PLUGIN_ACTION + " SecurityManagerPlugin")
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .addAssert(AgentAssert.log(log, "startPlugin(SecurityManagerPlugin)"));

        story.execute();
    }


    @Test
    public void test_startPlugin_invalidArgument() throws Exception {
        startPluginManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.START_PLUGIN_ACTION)
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.FAILURE),
                                         matchContent(
                                               "(Impossible de démarrer le plugin : plugin non renseigné)")));

        story.execute();
    }


    @Test
    public void test_startPlugin_errorDuringStop() throws Exception {
        startPluginManagerAgent(new PluginManagerMock(log, "SecurityManagerPlugin", "WorkflowServerPlugin") {
            @Override
            public void startPlugin(String plugin) {
                throw new RuntimeException("Erreur !!!");
            }
        });

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.START_PLUGIN_ACTION + " SecurityManagerPlugin")
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.FAILURE),
                                         matchContent(
                                               "(Impossible de démarrer le plugin SecurityManagerPlugin : Erreur !!!)")));

        story.execute();
    }


    @Test
    public void test_stopPlugin() throws Exception {
        startPluginManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.STOP_PLUGIN_ACTION + " SecurityManagerPlugin")
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchPerformative(Performative.INFORM));

        story.record()
              .addAssert(AgentAssert.log(log, "stopPlugin(SecurityManagerPlugin)"));

        story.execute();
    }


    @Test
    public void test_stopPlugin_invalidArgument() throws Exception {
        startPluginManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.STOP_PLUGIN_ACTION)
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.FAILURE),
                                         matchContent(
                                               "(Impossible d'arrêter le plugin : plugin non renseigné)")));

        story.execute();
    }


    @Test
    public void test_stopPlugin_errorDuringStop() throws Exception {
        startPluginManagerAgent(new PluginManagerMock(log, "SecurityManagerPlugin", "WorkflowServerPlugin") {
            @Override
            public void stopPlugin(String plugin) {
                throw new RuntimeException("Erreur !!!");
            }
        });

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.STOP_PLUGIN_ACTION + " SecurityManagerPlugin")
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.FAILURE),
                                         matchContent(
                                               "(Impossible d'arrêter le plugin SecurityManagerPlugin : Erreur !!!)")));

        story.execute();
    }


    private void startPluginManagerAgent() {
        startPluginManagerAgent(new PluginManagerMock(log, "SecurityManagerPlugin", "WorkflowServerPlugin"));
    }


    private void startPluginManagerAgent(PluginManagerMock pluginManager) {
        story.record()
              .startAgent("bebel", new PluginManagerAgent(pluginManager));
        story.record()
              .assertAgentWithService(new String[]{"bebel"}, Constants.MANAGE_PLUGINS_SERVICE_TYPE);
    }


    private class PluginManagerMock implements PluginManager {
        private LogString log;
        private List<String> plugins;


        private PluginManagerMock(LogString log, String... plugins) {
            this.log = log;
            this.plugins = Arrays.asList(plugins);
        }


        public List<String> getPlugins() {
            log.call("getPlugins");
            return plugins;
        }


        public void startPlugin(String plugin) {
            log.call("startPlugin", plugin);
        }


        public void stopPlugin(String plugin) {
            log.call("stopPlugin", plugin);
        }
    }
}
