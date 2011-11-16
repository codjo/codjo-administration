package net.codjo.administration.server.operation.systemProperties;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Aid;
import static net.codjo.agent.MessageTemplate.matchContent;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.Story;
import net.codjo.test.common.LogString;
import org.junit.Test;

public class SystemPropertiesAgentTest {
    private Story story = new Story();
    private LogString log = new LogString();


    @Test
    public void test_getSystemProperties() throws Exception {
        startSystemPropertiesAgent();

        story.record().startTester("gui-agent")
              .sendMessage(Performative.REQUEST, RequestProtocol.REQUEST, new Aid("sysProp"),
                           AdministrationOntology.DISPLAY_SYSTEM_PROPERTIES)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchContent("<string>eau=mouille</string>"));

        story.record().addAssert(AgentAssert.log(log, "getSystemProperties()"));

        story.execute();
    }


    @Test
    public void test_getSystemEnvironment() throws Exception {
        startSystemPropertiesAgent();

        story.record().startTester("gui-agent")
              .sendMessage(Performative.REQUEST, RequestProtocol.REQUEST, new Aid("sysProp"),
                           AdministrationOntology.DISPLAY_SYSTEM_ENVIRONMENT)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchContent("<string>feu=brule</string>"));

        story.record().addAssert(AgentAssert.log(log, "getSystemEnvironment()"));

        story.execute();
    }


    private void startSystemPropertiesAgent() {
        story.record().startAgent("sysProp", new SystemPropertiesAgent(new MockSystemProperties(log)));
    }


    private class MockSystemProperties implements SystemProperties {
        private LogString log;


        private MockSystemProperties(LogString log) {
            this.log = log;
        }


        public String getSystemProperties() {
            log.call("getSystemProperties");
            return "eau=mouille";
        }


        public String getSystemEnvironment() {
            log.call("getSystemEnvironment");
            return "feu=brule";
        }
    }
}
