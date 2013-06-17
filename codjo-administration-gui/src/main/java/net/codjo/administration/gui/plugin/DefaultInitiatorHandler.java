package net.codjo.administration.gui.plugin;
import net.codjo.administration.common.XmlCodec;
import net.codjo.administration.gui.plugin.AdministrationGuiAgent.Handler;
import net.codjo.agent.AclMessage;
import net.codjo.agent.protocol.InitiatorHandler;

class DefaultInitiatorHandler implements InitiatorHandler {
    protected Handler handler;
    protected final ActionType actionType;
    protected Object[] parameters;


    DefaultInitiatorHandler(Handler handler, ActionType actionType, Object... parameters) {
        this.handler = handler;
        this.actionType = actionType;
        this.parameters = parameters;
    }


    public void handleAgree(AclMessage agree) {
    }


    public void handleRefuse(AclMessage refuse) {
        triggerError(refuse);
    }


    public void handleFailure(AclMessage failure) {
        handler.handleError(failure.getContent());
    }


    public void handleOutOfSequence(AclMessage outOfSequenceMessage) {
        triggerError(outOfSequenceMessage);
    }


    public void handleNotUnderstood(AclMessage notUnderstoodMessage) {
        triggerError(notUnderstoodMessage);
    }


    private void triggerError(AclMessage error) {
        handler.handleError("Erreur technique : " + error.toFipaACLString());
    }


    public void handleInform(AclMessage inform) {
        try {
            switch (actionType) {
                case GET_PLUGINS:
                    handler.handlePluginsReceived(XmlCodec.listFromXml(inform.getContent()));
                    break;

                case START_PLUGIN:
                    handler.handlePluginStarted((String)parameters[0]);
                    break;

                case STOP_PLUGIN:
                    handler.handlePluginStopped((String)parameters[0]);
                    break;

                case GET_LOGS:
                    handler.handleLogsReceived(XmlCodec.listFromXml(inform.getContent()));
                    break;

                case READ_LOG:
                    handler.handleLogRead(XmlCodec.logFromXml(inform.getContent()));
                    break;

                case GET_SERVICES:
                    handler.handleServicesReceived(XmlCodec.listFromXml(inform.getContent()));
                    break;

                case ENABLE_SERVICE:
                    handler.handleEnableService((String)parameters[0]);
                    break;

                case DISABLE_SERVICE:
                    handler.handleDisableService((String)parameters[0]);
                    break;

                case CHANGE_LOG_DIR:
                    handler.handleLogDirChanged(XmlCodec.logFromXml(inform.getContent()));
                    break;

                case RESTORE_LOG_DIR:
                    handler.handleLogDirChanged(XmlCodec.logFromXml(inform.getContent()));
                    break;

                case CHANGE_JDBC_USERS_FILTER:
                    handler.handleJdbcUsersFilterChanged(XmlCodec.logFromXml(inform.getContent()));
                    break;

                case RESTORE_JDBC_USERS_FILTER:
                    handler.handleJdbcUsersFilterChanged(XmlCodec.logFromXml(inform.getContent()));
                    break;

                case GET_SYSTEM_PROPERTIES:
                    handler.handleSystemProperties(XmlCodec.logFromXml(inform.getContent()));
                    break;

                case GET_SYSTEM_ENVIRONMENT:
                    handler.handleSystemEnvironment(XmlCodec.logFromXml(inform.getContent()));
                    break;

                case CLOSE:
                    break;
            }
        }
        catch (Throwable throwable) {
            RuntimeException exception = new RuntimeException(throwable);
            handler.handleCommunicationError("Erreur lors du traitement suivant : " + actionType.name(),
                                             exception);
            throw exception;
        }
    }
}
