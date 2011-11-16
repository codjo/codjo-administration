package net.codjo.administration.server.plugin;
import net.codjo.administration.common.ConfigurationOntology;
import static net.codjo.administration.common.Constants.MANAGE_LOGS_SERVICE_TYPE;
import static net.codjo.administration.common.Constants.MANAGE_PLUGINS_SERVICE_TYPE;
import static net.codjo.administration.common.Constants.MANAGE_SERVICE_TYPE;
import static net.codjo.administration.common.Constants.MANAGE_SYSTEM_PROPERTIES_TYPE;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.memory.MemoryWatcherAgent.Handler;
import net.codjo.administration.server.operation.log.DefaultLogReader;
import net.codjo.administration.server.operation.log.LogReaderAgent;
import net.codjo.administration.server.operation.plugin.DefaultPluginManager;
import net.codjo.administration.server.operation.plugin.PluginManagerAgent;
import net.codjo.administration.server.operation.configuration.AdministrationServerConfiguration;
import net.codjo.administration.server.operation.configuration.DefaultAdministrationServerConfiguration;
import net.codjo.administration.server.operation.configuration.AdministrationServerConfigurationAgent;
import net.codjo.administration.server.operation.systemProperties.DefaultSystemProperties;
import net.codjo.administration.server.operation.systemProperties.SystemPropertiesAgent;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.AgentController;
import net.codjo.agent.BadControllerException;
import net.codjo.agent.ContainerConfiguration;
import net.codjo.mad.server.plugin.MadServerOperations;
import net.codjo.mad.server.plugin.MadServerPlugin;
import net.codjo.plugin.server.AbstractServerPlugin;
import net.codjo.plugin.server.ServerCore;

public class AdministrationServerPlugin extends AbstractServerPlugin {
    private static final String LOG_DIR_PROPERTY = "log.dir";
    private final DefaultAdministrationServerConfiguration configuration
          = new DefaultAdministrationServerConfiguration();
    private final ServerCore serverCore;
    private final MadServerOperations madServerOperations;
    private AgentController pluginManagerAgentController;
    private AgentController logManagerAgentController;
    private AgentController serviceAgentController;
    private AgentController systemPropertiesController;


    public AdministrationServerPlugin(ServerCore serverCore, MadServerPlugin madServerPlugin) {
        this.serverCore = serverCore;
        madServerOperations = madServerPlugin.getOperations();
    }


    public AdministrationServerConfiguration getConfiguration() {
        return configuration;
    }


    @Override
    public void initContainer(ContainerConfiguration containerConfiguration) throws Exception {
        if (containerConfiguration.getParameter(ConfigurationOntology.AUDIT_DESTINATION_DIR) != null) {
            configuration.setDefaultAuditDestinationDir(containerConfiguration.getParameter(
                  ConfigurationOntology.AUDIT_DESTINATION_DIR));
        }
        else if (!configuration.isAuditDestinationDirSet()) {
            String logDirValue = System.getProperty(LOG_DIR_PROPERTY);
            if (logDirValue != null) {
                configuration.setDefaultAuditDestinationDir(logDirValue);
            }
        }

        if (containerConfiguration.getParameter(ConfigurationOntology.RECORD_HANDLER_STATISTICS) != null) {
            configuration.setDefaultRecordHandlerStatistics(readBoolean(containerConfiguration,
                                                                        ConfigurationOntology.RECORD_HANDLER_STATISTICS));
        }

        if (containerConfiguration.getParameter(ConfigurationOntology.RECORD_MEMORY_USAGE) != null) {
            configuration.setDefaultRecordMemoryUsage(readBoolean(containerConfiguration,
                                                                  ConfigurationOntology.RECORD_MEMORY_USAGE));
        }
    }


    @Override
    public void start(AgentContainer agentContainer) throws Exception {
        checkConfiguration();

        pluginManagerAgentController = agentContainer.acceptNewAgent(
              MANAGE_PLUGINS_SERVICE_TYPE, new PluginManagerAgent(new DefaultPluginManager(serverCore)));
        pluginManagerAgentController.start();

        DefaultLogReader defaultLogReader = new DefaultLogReader(configuration.getAuditDestinationDir());
        logManagerAgentController = agentContainer.acceptNewAgent(MANAGE_LOGS_SERVICE_TYPE, new LogReaderAgent(defaultLogReader));
        logManagerAgentController.start();

        serviceAgentController = agentContainer.acceptNewAgent(MANAGE_SERVICE_TYPE,
               new AdministrationServerConfigurationAgent(configuration, madServerOperations, defaultLogReader));
        serviceAgentController.start();

        DefaultSystemProperties defaultSystemProperties = new DefaultSystemProperties();
        systemPropertiesController = agentContainer.acceptNewAgent(MANAGE_SYSTEM_PROPERTIES_TYPE,
               new SystemPropertiesAgent(defaultSystemProperties));
        systemPropertiesController.start();
    }


    @Override
    public void stop() throws Exception {
        killAgent(pluginManagerAgentController);
        killAgent(logManagerAgentController);
        killAgent(serviceAgentController);
        killAgent(systemPropertiesController);

        pluginManagerAgentController = null;
        logManagerAgentController = null;
        serviceAgentController = null;
        systemPropertiesController = null;
    }


    private void killAgent(AgentController agentController) throws BadControllerException {
        if (agentController != null) {
            agentController.kill();
        }
    }


    private boolean readBoolean(ContainerConfiguration containerConfiguration, String key) {
        String text = containerConfiguration.getParameter(key);
        return text != null && ("true".equals(text.toLowerCase()) || "on".equals(text.toLowerCase()));
    }


    private void checkConfiguration() {
        String auditDestinationDir = configuration.getAuditDestinationDir();
        if ((configuration.isRecordHandlerStatistics() || configuration.isRecordMemoryUsage())
            && (auditDestinationDir == null || "".equals(auditDestinationDir))) {
            throw new RuntimeException(ConfigurationOntology.AUDIT_DESTINATION_DIR + " not set");
        }
    }


    static class ResourcesAgentHandler implements Handler {
        private AdministrationLogFile administrationLogFile;


        ResourcesAgentHandler(AdministrationLogFile administrationLogFile) {
            this.administrationLogFile = administrationLogFile;
        }


        public void memory(double used, double total) {
            administrationLogFile.write("MEMORY", "", used, total);
        }
    }
}
