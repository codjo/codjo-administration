package net.codjo.administration.server.audit.memory;
import net.codjo.administration.common.Constants;
import net.codjo.administration.server.audit.memory.MemoryWatcherAgent.Handler;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.Story;
import net.codjo.test.common.LogString;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MemoryWatcherAgentTest {
    private Story story = new Story();
    private LogString log = new LogString();


    @Test
    public void test_nominal() throws Exception {
        MemoryWatcherAgent agent = new MemoryWatcherAgent(new MemoryProbeMock(Arrays.asList(555, 777),
                                                                              Arrays.asList(666, 888)),
                                                          new HandlerMock(log));
        agent.setPeriod(100);
        story.record()
              .startAgent("resources-agent",
                          agent);
        story.record()
              .assertAgentWithService(new String[]{"resources-agent"},
                                      Constants.MANAGE_RESOURCES_SERVICE_TYPE);

        story.record()
              .addAssert(AgentAssert.log(log, "memory(555.0, 666.0), memory(777.0, 888.0)"));

        story.execute();
    }


    private static class MemoryProbeMock implements MemoryProbe {
        private List<Integer> values1;
        private int cpt1 = 0;
        private List<Integer> values2;
        private int cpt2 = 0;


        private MemoryProbeMock(List<Integer> values1, List<Integer> values2) {
            this.values1 = values1;
            this.values2 = values2;
        }


        public double getUsedMemory() {
            return values1.get(cpt1++);
        }


        public double getTotalMemory() {
            return values2.get(cpt2++);
        }
    }

    private static class HandlerMock implements Handler {
        private LogString log;


        private HandlerMock(LogString log) {
            this.log = log;
        }


        public void memory(double used, double total) {
            log.call("memory", used, total);
        }
    }
}
