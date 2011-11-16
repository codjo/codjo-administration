package net.codjo.administration.server.operation.plugin;
import net.codjo.agent.AgentContainer;
import net.codjo.plugin.common.ApplicationPlugin;
import net.codjo.plugin.server.AbstractServerPlugin;
import net.codjo.plugin.server.ServerCoreMock;
import net.codjo.test.common.LogString;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class DefaultPluginManagerTest {
    private DefaultPluginManager pluginManager;
    private LogString log = new LogString();
    private final String firstPlugin = MyFirstPlugin.class.getName();
    private final String secondPlugin = MySecondPlugin.class.getName();


    @Before
    public void setUp() {
        pluginManager = new DefaultPluginManager(new MyServerCoreMock(
              new MyFirstPlugin(log),
              new MySecondPlugin(log)));
    }


    @Test
    public void test_getPlugins() throws Exception {
        assertEquals(2, pluginManager.getPlugins().size());
        assertEquals(firstPlugin, pluginManager.getPlugins().get(0));
        assertEquals(secondPlugin, pluginManager.getPlugins().get(1));
    }


    @Test
    public void test_startPlugin() throws Exception {
        pluginManager.startPlugin(firstPlugin);

        log.assertContent(firstPlugin + ".start()");
    }


    @Test
    public void test_stopPlugin() throws Exception {
        pluginManager.stopPlugin(secondPlugin);

        log.assertContent(secondPlugin + ".stop()");
    }


    private static class MyServerCoreMock extends ServerCoreMock {
        private List<ApplicationPlugin> plugins;


        private MyServerCoreMock(ApplicationPlugin... plugins) {
            this.plugins = Arrays.asList(plugins);
        }


        @Override
        public List<ApplicationPlugin> getPlugins() {
            return plugins;
        }
    }

    private static class MyAbstractServerPlugin extends AbstractServerPlugin {
        private String pluginName;
        private LogString log;


        MyAbstractServerPlugin(LogString log) {
            this.pluginName = getClass().getName();
            this.log = log;
        }


        public String getPluginName() {
            return pluginName;
        }


        @Override
        public void start(AgentContainer agentContainer) throws Exception {
            log.call(pluginName + ".start");
        }


        @Override
        public void stop() throws Exception {
            log.call(pluginName + ".stop");
        }
    }

    private class MyFirstPlugin extends MyAbstractServerPlugin {

        MyFirstPlugin(LogString log) {
            super(log);
        }
    }

    private class MySecondPlugin extends MyAbstractServerPlugin {

        MySecondPlugin(LogString log) {
            super(log);
        }
    }
}
