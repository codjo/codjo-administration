package net.codjo.administration.server.plugin;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.administration.common.Constants;
import net.codjo.administration.server.audit.memory.MemoryWatcherAgent;
import net.codjo.administration.server.operation.configuration.AdministrationServerConfigurationAgent;
import net.codjo.administration.server.operation.configuration.DefaultAdministrationServerConfiguration;
import net.codjo.administration.server.operation.log.LogReaderAgent;
import net.codjo.administration.server.operation.plugin.PluginManagerAgent;
import net.codjo.administration.server.operation.systemProperties.SystemPropertiesAgent;
import net.codjo.agent.AgentContainerMock;
import net.codjo.agent.ContainerConfiguration;
import net.codjo.agent.ContainerConfigurationMock;
import net.codjo.mad.server.plugin.MadServerOperations;
import net.codjo.mad.server.plugin.MadServerPlugin;
import net.codjo.plugin.server.ServerCoreMock;
import net.codjo.test.common.LogString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdministrationServerPluginTest {
    private static final String LOG_DIR_PROPERTY = "log.dir";
    private MadServerOperations madServerOperations = mock(MadServerOperations.class);
    private LogString log = new LogString();
    private AdministrationServerPlugin plugin;


    @Before
    public void setUp() throws Exception {
        MadServerPlugin madServerPlugin = mock(MadServerPlugin.class);
        when(madServerPlugin.getOperations()).thenReturn(madServerOperations);
        plugin = new AdministrationServerPlugin(new ServerCoreMock(), madServerPlugin);
    }


    @After
    public void tearDown() {
        System.clearProperty(LOG_DIR_PROPERTY);
    }


    @Test
    public void test_startStop() throws Exception {
        plugin.initContainer(new ContainerConfigurationMock(log));

        plugin.start(new AgentContainerMock(log));

        assertLogContent("acceptNewAgent(%plugin-agent%, %plugin-agent-class%), "
                         + "%plugin-agent%.start(), "
                         + "acceptNewAgent(%log-agent%, %log-agent-class%), "
                         + "%log-agent%.start(), "
                         + "acceptNewAgent(%service-agent%, %service-agent-class%), "
                         + "%service-agent%.start(), "
                         + "acceptNewAgent(%properties-agent%, %properties-agent-class%), "
                         + "%properties-agent%.start()");
        log.clear();

        plugin.stop();

        assertLogContent("%plugin-agent%.kill(), "
                         + "%log-agent%.kill(), "
                         + "%service-agent%.kill(), "
                         + "%properties-agent%.kill()");
    }


    @Test
    public void test_configuration() throws Exception {
        assertNotNull(plugin.getConfiguration());
    }


    @Test
    public void test_recordMemoryUsage_configFromDefault() throws Exception {
        plugin.initContainer(new ContainerConfiguration());

        assertFalse(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordMemoryUsage());
    }


    @Test
    public void test_recordMemoryUsage_configFromConfiguration() throws Exception {
        plugin.getConfiguration().setRecordMemoryUsage(true);

        plugin.initContainer(new ContainerConfiguration());

        assertTrue(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordMemoryUsage());
    }


    @Test
    public void test_recordMemoryUsage_configFromContainer() throws Exception {
        ContainerConfiguration configuration = new ContainerConfiguration();
        configuration.setParameter(ConfigurationOntology.RECORD_MEMORY_USAGE, "true");
        plugin.getConfiguration().setRecordMemoryUsage(false);

        plugin.initContainer(configuration);

        assertTrue(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordMemoryUsage());
    }


    @Test
    public void test_recordJdbcStatistics_configFromDefault() throws Exception {
        plugin.initContainer(new ContainerConfiguration());

        assertFalse(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordJdbcStatistics());
    }


    @Test
    public void test_recordJdbcStatistics_configFromConf() throws Exception {
        plugin.getConfiguration().setRecordJdbcStatistics(true);

        plugin.initContainer(new ContainerConfiguration());

        assertTrue(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordJdbcStatistics());
    }


    @Test
    public void test_recordJdbcStatistics_configFromContainer() throws Exception {
        ContainerConfiguration configuration = new ContainerConfiguration();
        configuration.setParameter(ConfigurationOntology.RECORD_JDBC_STATISTICS, "true");
        plugin.getConfiguration().setRecordJdbcStatistics(false);

        plugin.initContainer(configuration);

        assertTrue(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordJdbcStatistics());
    }


    @Test
    public void test_recordHandlerStatistics_configFromDefault() throws Exception {
        plugin.initContainer(new ContainerConfiguration());

        assertFalse(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordHandlerStatistics());
    }


    @Test
    public void test_recordHandlerStatistics_configFromConf() throws Exception {
        plugin.getConfiguration().setRecordHandlerStatistics(true);

        plugin.initContainer(new ContainerConfiguration());

        assertTrue(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordHandlerStatistics());
    }


    @Test
    public void test_recordHandlerStatistics_configFromContainer() throws Exception {
        ContainerConfiguration configuration = new ContainerConfiguration();
        configuration.setParameter(ConfigurationOntology.RECORD_HANDLER_STATISTICS, "true");
        plugin.getConfiguration().setRecordHandlerStatistics(false);

        plugin.initContainer(configuration);

        assertTrue(((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).isRecordHandlerStatistics());
    }


    @Test
    public void test_auditDestinationDir_configFromDefault() throws Exception {
        System.setProperty(LOG_DIR_PROPERTY, "c:\\dirFromSystem");

        plugin.initContainer(new ContainerConfiguration());
        plugin.start(new AgentContainerMock(log));

        assertEquals("c:\\dirFromSystem",
                     ((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).getAuditDestinationDir());
    }


    @Test
    public void test_auditDestinationDir_configFromConfiguration() throws Exception {
        plugin.getConfiguration().setAuditDestinationDir("c:\\dirFromConfiguration");
        System.setProperty(LOG_DIR_PROPERTY, "c:\\dirFromSystem");

        plugin.initContainer(new ContainerConfiguration());
        plugin.start(new AgentContainerMock(log));

        assertEquals("c:\\dirFromConfiguration",
                     ((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).getAuditDestinationDir());
    }


    @Test
    public void test_auditDestinationDir_configFromContainer() throws Exception {
        ContainerConfiguration containerConfiguration = new ContainerConfiguration();
        containerConfiguration.setParameter(ConfigurationOntology.AUDIT_DESTINATION_DIR,
                                            "c:\\dirFromContainerConfig");
        plugin.getConfiguration().setAuditDestinationDir("c:\\dirFromConfiguration");
        System.setProperty(LOG_DIR_PROPERTY, "c:\\dirFromSystem");

        plugin.initContainer(containerConfiguration);
        plugin.start(new AgentContainerMock(log));

        assertEquals("c:\\dirFromContainerConfig",
                     ((DefaultAdministrationServerConfiguration)plugin.getConfiguration()).getAuditDestinationDir());
    }


    @Test
    public void test_auditDestinationDir_notSet() throws Exception {
        plugin.getConfiguration().setRecordMemoryUsage(true);
        plugin.initContainer(new ContainerConfiguration());

        try {
            plugin.start(new AgentContainerMock(log));
            fail();
        }
        catch (Exception e) {
            assertEquals(ConfigurationOntology.AUDIT_DESTINATION_DIR + " not set",
                         e.getMessage());
        }
    }


    private void assertLogContent(String expectedContent) {
        log.assertContent(expectedContent
                                .replaceAll("%plugin-agent-class%", PluginManagerAgent.class.getSimpleName())
                                .replaceAll("%plugin-agent%", Constants.MANAGE_PLUGINS_SERVICE_TYPE)
                                .replaceAll("%log-agent-class%", LogReaderAgent.class.getSimpleName())
                                .replaceAll("%log-agent%", Constants.MANAGE_LOGS_SERVICE_TYPE)
                                .replaceAll("%resource-agent-class%", MemoryWatcherAgent.class.getSimpleName())
                                .replaceAll("%resource-agent%", Constants.MANAGE_RESOURCES_SERVICE_TYPE)
                                .replaceAll("%service-agent-class%",
                                            AdministrationServerConfigurationAgent.class.getSimpleName())
                                .replaceAll("%service-agent%", Constants.MANAGE_SERVICE_TYPE)
                                .replaceAll("%properties-agent-class%", SystemPropertiesAgent.class.getSimpleName())
                                .replaceAll("%properties-agent%", Constants.MANAGE_SYSTEM_PROPERTIES_TYPE)
        );
    }
}
