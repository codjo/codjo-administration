package net.codjo.administration.gui.plugin;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.Constants;
import static net.codjo.administration.gui.plugin.ActionType.CHANGE_LOG_DIR;
import static net.codjo.administration.gui.plugin.ActionType.CLOSE;
import static net.codjo.administration.gui.plugin.ActionType.DISABLE_SERVICE;
import static net.codjo.administration.gui.plugin.ActionType.ENABLE_SERVICE;
import static net.codjo.administration.gui.plugin.ActionType.GET_LOGS;
import static net.codjo.administration.gui.plugin.ActionType.GET_PLUGINS;
import static net.codjo.administration.gui.plugin.ActionType.GET_SERVICES;
import static net.codjo.administration.gui.plugin.ActionType.GET_SYSTEM_ENVIRONMENT;
import static net.codjo.administration.gui.plugin.ActionType.GET_SYSTEM_PROPERTIES;
import static net.codjo.administration.gui.plugin.ActionType.READ_LOG;
import static net.codjo.administration.gui.plugin.ActionType.RESTORE_LOG_DIR;
import static net.codjo.administration.gui.plugin.ActionType.START_PLUGIN;
import static net.codjo.administration.gui.plugin.ActionType.STOP_PLUGIN;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Aid;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.AgentDescription;
import net.codjo.agent.DFService.DFServiceException;
import net.codjo.agent.GuiAgent;
import net.codjo.agent.GuiEvent;
import net.codjo.agent.behaviour.SequentialBehaviour;
import net.codjo.agent.protocol.RequestInitiator;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.mad.gui.framework.GuiContext;
import java.util.List;

class AdministrationGuiAgent extends GuiAgent {
    private final Handler handler;


    AdministrationGuiAgent(GuiContext guiContext) {
        handler = new DefaultHandler(guiContext, this);
    }


    AdministrationGuiAgent(Handler handler) {
        this.handler = handler;
    }


    @Override
    protected void setup() {
        Aid pluginManager = findPluginManager();
        if (pluginManager == null) {
            return;
        }

        Aid logManager = findLogManager();
        if (logManager == null) {
            return;
        }

        Aid serviceManager = findServiceManager();
        if (serviceManager == null) {
            return;
        }

        Aid systemPropertiesManager = findSystemPropertiesManager();
        if (systemPropertiesManager == null) {
            return;
        }

        handler.handleAgentBorn();

        handler.handleActionStarted();
        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, GET_PLUGINS),
                                          queryPluginsMessage(pluginManager)));

        handler.handleActionStarted();
        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, GET_LOGS),
                                          queryLogsMessage(logManager)));

        handler.handleActionStarted();
        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, GET_SERVICES),
                                          queryServicesMessage(serviceManager)));

        handler.handleActionStarted();
        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, GET_SYSTEM_PROPERTIES),
                                          querySystemPropertiesMessage(systemPropertiesManager)));

        handler.handleActionStarted();
        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, GET_SYSTEM_ENVIRONMENT),
                                          querySystemEnvironmentMessage(systemPropertiesManager)));
    }


    @Override
    protected void tearDown() {
        handler.handleAgentDead();
    }


    @Override
    protected void onGuiEvent(GuiEvent guiEvent) {
        handler.handleActionStarted();

        if (guiEvent.getType() == CLOSE.ordinal()) {
            die();
        }
        else if (guiEvent.getType() == START_PLUGIN.ordinal()) {
            startPlugin((String)guiEvent.getParameter(0));
        }
        else if (guiEvent.getType() == STOP_PLUGIN.ordinal()) {
            stopPlugin((String)guiEvent.getParameter(0));
        }
        else if (guiEvent.getType() == READ_LOG.ordinal()) {
            readLog((String)guiEvent.getParameter(0));
        }
        else if (guiEvent.getType() == ENABLE_SERVICE.ordinal()) {
            enableService((String)guiEvent.getParameter(0));
        }
        else if (guiEvent.getType() == DISABLE_SERVICE.ordinal()) {
            disableService((String)guiEvent.getParameter(0));
        }
        else if (guiEvent.getType() == CHANGE_LOG_DIR.ordinal()) {
            changeLogDir((String)guiEvent.getParameter(0));
        }
        else if (guiEvent.getType() == RESTORE_LOG_DIR.ordinal()) {
            restoreLogDir();
        }
    }


    private void startPlugin(String plugin) {
        AclMessage aclMessage = new AclMessage(Performative.REQUEST, RequestProtocol.REQUEST);
        aclMessage.addReceiver(findPluginManager());
        aclMessage.setContent(AdministrationOntology.START_PLUGIN_ACTION + " " + plugin);

        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, START_PLUGIN, plugin),
                                          aclMessage));
    }


    private void stopPlugin(String plugin) {
        AclMessage aclMessage = new AclMessage(Performative.REQUEST, RequestProtocol.REQUEST);
        aclMessage.addReceiver(findPluginManager());
        aclMessage.setContent(AdministrationOntology.STOP_PLUGIN_ACTION + " " + plugin);

        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, STOP_PLUGIN, plugin),
                                          aclMessage));
    }


    private void readLog(String logFile) {
        AclMessage aclMessage = new AclMessage(Performative.REQUEST, RequestProtocol.REQUEST);
        aclMessage.addReceiver(findLogManager());
        aclMessage.setContent(AdministrationOntology.READ_LOG_ACTION + " " + logFile);

        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, READ_LOG),
                                          aclMessage));
    }


    private void enableService(String service) {
        AclMessage aclMessage = new AclMessage(Performative.REQUEST, RequestProtocol.REQUEST);
        aclMessage.addReceiver(findServiceManager());
        aclMessage.setContent(AdministrationOntology.ENABLE_SERVICE_ACTION + " " + service);

        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, ENABLE_SERVICE, service),
                                          aclMessage));
    }


    private void disableService(String service) {
        AclMessage aclMessage = new AclMessage(Performative.REQUEST, RequestProtocol.REQUEST);
        aclMessage.addReceiver(findServiceManager());
        aclMessage.setContent(AdministrationOntology.DISABLE_SERVICE_ACTION + " " + service);

        addBehaviour(new RequestInitiator(this,
                                          new DefaultInitiatorHandler(handler, DISABLE_SERVICE, service),
                                          aclMessage));
    }


    private void changeLogDir(String newLogDir) {
        AclMessage aclMessage = new AclMessage(Performative.REQUEST, RequestProtocol.REQUEST);
        aclMessage.addReceiver(findServiceManager());
        aclMessage.setContent(AdministrationOntology.CHANGE_LOG_DIR + " " + newLogDir);

        RequestInitiator changeLogDirBehaviour = new RequestInitiator(this,
                                                                      new DefaultInitiatorHandler(handler,
                                                                                                  CHANGE_LOG_DIR,
                                                                                                  newLogDir),
                                                                      aclMessage);
        addBehaviour(SequentialBehaviour.wichStartsWith(changeLogDirBehaviour).andThen(createReadLogBehaviour()));
    }


    private void restoreLogDir() {
        AclMessage aclMessage = new AclMessage(Performative.REQUEST, RequestProtocol.REQUEST);
        aclMessage.addReceiver(findServiceManager());
        aclMessage.setContent(AdministrationOntology.RESTORE_LOG_DIR);

        RequestInitiator restoreLogDirBehaviour = new RequestInitiator(this,
                                                                       new DefaultInitiatorHandler(handler,
                                                                                                   RESTORE_LOG_DIR),
                                                                       aclMessage);
        addBehaviour(SequentialBehaviour.wichStartsWith(restoreLogDirBehaviour).andThen(createReadLogBehaviour()));
    }


    private RequestInitiator createReadLogBehaviour() {
        handler.handleActionStarted();
        return new RequestInitiator(this,
                                    new DefaultInitiatorHandler(handler, GET_LOGS),
                                    queryLogsMessage(findLogManager()));
    }


    private AclMessage queryPluginsMessage(Aid pluginManager) {
        AclMessage message = new AclMessage(Performative.QUERY);
        message.setProtocol(RequestProtocol.QUERY);
        message.setContent(AdministrationOntology.GET_PLUGINS_ACTION);
        message.addReceiver(pluginManager);
        return message;
    }


    private AclMessage queryLogsMessage(Aid pluginManager) {
        AclMessage message = new AclMessage(Performative.QUERY);
        message.setProtocol(RequestProtocol.QUERY);
        message.setContent(AdministrationOntology.GET_LOG_FILES_ACTION);
        message.addReceiver(pluginManager);
        return message;
    }


    private AclMessage queryServicesMessage(Aid serviceManager) {
        AclMessage message = new AclMessage(Performative.QUERY);
        message.setProtocol(RequestProtocol.QUERY);
        message.setContent(AdministrationOntology.GET_SERVICES_ACTION);
        message.addReceiver(serviceManager);
        return message;
    }


    private AclMessage querySystemPropertiesMessage(Aid systemPropertiesManager) {
        AclMessage message = new AclMessage(Performative.REQUEST);
        message.setProtocol(RequestProtocol.REQUEST);
        message.setContent(AdministrationOntology.DISPLAY_SYSTEM_PROPERTIES);
        message.addReceiver(systemPropertiesManager);
        return message;
    }


    private AclMessage querySystemEnvironmentMessage(Aid systemPropertiesManager) {
        AclMessage message = new AclMessage(Performative.REQUEST);
        message.setProtocol(RequestProtocol.REQUEST);
        message.setContent(AdministrationOntology.DISPLAY_SYSTEM_ENVIRONMENT);
        message.addReceiver(systemPropertiesManager);
        return message;
    }


    private Aid findPluginManager() {
        return findManagerAgent(Constants.MANAGE_PLUGINS_SERVICE_TYPE,
                                "Impossible de trouver l'agent gérant les plugins");
    }


    private Aid findLogManager() {
        return findManagerAgent(Constants.MANAGE_LOGS_SERVICE_TYPE,
                                "Impossible de trouver l'agent gérant les logs");
    }


    private Aid findServiceManager() {
        return findManagerAgent(Constants.MANAGE_SERVICE_TYPE,
                                "Impossible de trouver l'agent gérant la configuration de plugin d'administration");
    }


    private Aid findSystemPropertiesManager() {
        return findManagerAgent(Constants.MANAGE_SYSTEM_PROPERTIES_TYPE,
                                "Impossible de trouver l'agent des propriétés système");
    }


    private Aid findManagerAgent(String serviceType, String errorMessage) {
        try {
            AgentDescription[] descriptions = DFService.searchForService(this, serviceType);

            if (descriptions.length == 0) {
                handler.handleError(errorMessage + " (absent de la plateforme)");
                return null;
            }

            return descriptions[0].getAID();
        }
        catch (DFServiceException e) {
            handler.handleError(errorMessage + " " + e.getMessage());
            return null;
        }
    }


    public static interface Handler {

        void handleAgentBorn();


        void handleAgentDead();


        void handleError(String error);


        void handleCommunicationError(String error, Exception e);


        void handleActionStarted();


        void handlePluginsReceived(List<String> plugins);


        void handlePluginStarted(String plugin);


        void handlePluginStopped(String plugin);


        void handleLogsReceived(List<String> logs);


        void handleLogRead(String content);


        void handleEnableService(String service);


        void handleDisableService(String service);


        void handleServicesReceived(List<String> services);


        void handleLogDirChanged(String newLogDir);


        void handleSystemProperties(String value);


        void handleSystemEnvironment(String value);
    }
}
