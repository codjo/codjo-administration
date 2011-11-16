package net.codjo.administration.server.operation.log;
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

public class LogReaderAgentTest {
    private Story story = new Story();
    private LogString log = new LogString();


    @Test
    public void test_getLogs() throws Exception {
        startLogManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.QUERY,
                           RequestProtocol.QUERY,
                           new Aid("bebel"),
                           AdministrationOntology.GET_LOG_FILES_ACTION)
              .then()
              .receiveMessage()
              .assertReceivedMessage(matchContent(
                    "<list>\n"
                    + "  <string>server.log</string>\n"
                    + "  <string>mad.log</string>\n"
                    + "</list>"));

        story.record()
              .addAssert(AgentAssert.log(log, "getLogFiles()"));

        story.execute();
    }


    @Test
    public void test_readLog() throws Exception {
        startLogManagerAgent();

        story.record()
              .startTester("gui-agent")
              .sendMessage(Performative.REQUEST,
                           RequestProtocol.REQUEST,
                           new Aid("bebel"),
                           AdministrationOntology.READ_LOG_ACTION + " server.log")
              .then()
              .receiveMessage()
              .assertReceivedMessage(and(matchPerformative(Performative.INFORM),
                                         matchContent("<string>Content of server.log</string>")));

        story.record()
              .addAssert(AgentAssert.log(log, "readLog(server.log)"));

        story.execute();
    }


    private void startLogManagerAgent() {
        startPluginManagerAgent(new LogReaderMock(log, "server.log", "mad.log"));
    }


    private void startPluginManagerAgent(LogReaderMock logManager) {
        story.record()
              .startAgent("bebel", new LogReaderAgent(logManager));
        story.record()
              .assertAgentWithService(new String[]{"bebel"}, Constants.MANAGE_LOGS_SERVICE_TYPE);
    }


    class LogReaderMock implements LogReader {
        private LogString log;
        private List<String> logs;


        private LogReaderMock(LogString log, String... logs) {
            this.log = log;
            this.logs = Arrays.asList(logs);
        }


        public List<String> getLogFiles() {
            log.call("getLogFiles");
            return logs;
        }


        public String readLog(String logFile) {
            log.call("readLog", logFile);
            return "Content of " + logFile;
        }
    }
}
