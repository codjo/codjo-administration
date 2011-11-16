package net.codjo.administration.server.operation.configuration;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.administration.common.Constants;
import static net.codjo.administration.common.Constants.MANAGE_RESOURCES_SERVICE_TYPE;
import net.codjo.administration.common.XmlCodec;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.mad.HandlerExecutionSpy;
import net.codjo.administration.server.audit.memory.DefaultMemoryProbe;
import net.codjo.administration.server.audit.memory.MemoryWatcherAgent;
import net.codjo.administration.server.audit.memory.MemoryWatcherAgent.Handler;
import net.codjo.administration.server.operation.log.DefaultLogReader;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.AgentController;
import net.codjo.agent.Aid;
import net.codjo.agent.BadControllerException;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.DFService;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;
import net.codjo.agent.behaviour.HitmanBehaviour;
import net.codjo.agent.protocol.AbstractRequestParticipantHandler;
import net.codjo.agent.protocol.BasicQueryParticipantHandler;
import net.codjo.agent.protocol.FailureException;
import net.codjo.agent.protocol.RequestParticipant;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.mad.server.handler.HandlerListener;
import net.codjo.mad.server.plugin.MadServerOperations;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class AdministrationServerConfigurationAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(AdministrationServerConfigurationAgent.class);
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String AUDIT_LOG = "audit.log";
    private DefaultAdministrationServerConfiguration configuration;
    private MadServerOperations madServerOperations;
    private AdministrationLogFile administrationLogFile;
    private AgentController resourcesAgentController;
    private HandlerListener handlerListener;
    private MyAbstractRequestParticipantHandler participantHandler;
    private DefaultLogReader logReader;


    public AdministrationServerConfigurationAgent(DefaultAdministrationServerConfiguration configuration,
                               MadServerOperations madServerOperations, DefaultLogReader logReader) {
        this(configuration, madServerOperations, new AdministrationLogFile(), logReader);
    }


    public AdministrationServerConfigurationAgent(DefaultAdministrationServerConfiguration configuration,
                               MadServerOperations madServerOperations,
                               AdministrationLogFile logFile,
                               DefaultLogReader logReader) {
        this.configuration = configuration;
        this.madServerOperations = madServerOperations;
        this.administrationLogFile = logFile;
        this.logReader = logReader;

        if (configuration.isRecordHandlerStatistics() || configuration.isRecordMemoryUsage()) {
            try {
                administrationLogFile.init(
                      configuration.getAuditDestinationDir() + FILE_SEPARATOR + AUDIT_LOG);
            }
            catch (IOException e) {
                LOGGER.error("impossible d'initialiser le repertoire de log", e);
            }
        }
    }


    public String getServices() {
        List<String> services = new ArrayList<String>();
        services.add(ConfigurationOntology.AUDIT_DESTINATION_DIR + " " +
                     configuration.getAuditDestinationDir());
        services.add(serviceWithState(ConfigurationOntology.RECORD_HANDLER_STATISTICS,
                                      configuration.isRecordHandlerStatistics()));
        services.add(serviceWithState(ConfigurationOntology.RECORD_MEMORY_USAGE,
                                      configuration.isRecordMemoryUsage()));
        return XmlCodec.listToXml(services);
    }


    private String serviceWithState(String service, boolean isEnable) {
        String state = AdministrationOntology.DISABLE_SERVICE_ACTION;
        if (isEnable) {
            state = AdministrationOntology.ENABLE_SERVICE_ACTION;
        }
        return service + " " + state;
    }


    @Override
    protected void setup() {
        try {
            DFService.register(this, DFService.createAgentDescription(Constants.MANAGE_SERVICE_TYPE));
        }
        catch (DFService.DFServiceException e) {
            die();
            LOGGER.error("Impossible d'inscrire l'agent auprès du DF : " + e.getLocalizedMessage(), e);
            return;
        }

        participantHandler = new MyAbstractRequestParticipantHandler();
        participantHandler.initServices();

        addBehaviour(new RequestParticipant(this,
                                            new BasicQueryParticipantHandler(this),
                                            and(matchPerformative(Performative.QUERY),
                                                matchProtocol(RequestProtocol.QUERY))));

        addBehaviour(new RequestParticipant(this,
                                            participantHandler,
                                            and(matchPerformative(Performative.REQUEST),
                                                matchProtocol(RequestProtocol.REQUEST))));
    }


    @Override
    protected void tearDown() {
        try {
            participantHandler.closeServices();
            DFService.deregister(this);
        }
        catch (BadControllerException e) {
            LOGGER.error(
                  "Impossible de tuer l'agent gérant l'audit de la mémoire : " + e.getLocalizedMessage(), e);
        }
        catch (DFService.DFServiceException e) {
            LOGGER.error("Impossible de se desinscrire l'agent auprès du DF : " + e.getLocalizedMessage(), e);
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

    private class MyAbstractRequestParticipantHandler extends AbstractRequestParticipantHandler {
        private static final String DISABLE_SERVICE_ERROR = "Impossible de désactiver le service ";
        private static final String ENABLE_SERVICE_ERROR = "Impossible d'activer le service ";
        private Aid resourcesAgentAid;


        public AclMessage executeRequest(AclMessage request, AclMessage agreement) throws FailureException {
            String content = request.getContent();
            String[] splitted = content.split(" ");
            AclMessage response = request.createReply(Performative.INFORM);
            if (AdministrationOntology.ENABLE_SERVICE_ACTION.equals(splitted[0])) {
                enableService(splitted);
                response.setContent(splitted[1] + " " + splitted[0]);
            }
            else if (AdministrationOntology.DISABLE_SERVICE_ACTION.equals(splitted[0])) {
                disableService(splitted);
                response.setContent(splitted[1] + " " + splitted[0]);
            }
            else if (AdministrationOntology.CHANGE_LOG_DIR.equals(splitted[0])) {
                configuration.setAuditDestinationDir(splitted[1]);
                initLogs();
                response.setContent(XmlCodec.logToXml(configuration.getAuditDestinationDir()));
            }
            else if (AdministrationOntology.RESTORE_LOG_DIR.equals(splitted[0])) {
                configuration.restoreDefaultAuditDestinationDir();
                initLogs();
                response.setContent(XmlCodec.logToXml(configuration.getAuditDestinationDir()));
            }

            return response;
        }


        private void enableService(String[] splitted) throws FailureException {
            String serviceName = findServiceName(splitted, ENABLE_SERVICE_ERROR);
            try {
                if (ConfigurationOntology.RECORD_HANDLER_STATISTICS.equals(serviceName)) {
                    if (!configuration.isRecordHandlerStatistics()) {
                        configuration.setRecordHandlerStatistics(true);
                        startRecordingHandlerStatistics();
                    }
                }
                else if (ConfigurationOntology.RECORD_MEMORY_USAGE.equals(serviceName)) {
                    if (!configuration.isRecordMemoryUsage()) {
                        configuration.setRecordMemoryUsage(true);
                        startRecordingMemoryUsage();
                    }
                }
                else {
                    throw new Exception("service inconnu");
                }
            }
            catch (Exception e) {
                processError(serviceName, e, ENABLE_SERVICE_ERROR);
            }
        }


        private void disableService(String[] splitted) throws FailureException {
            String serviceName = findServiceName(splitted, DISABLE_SERVICE_ERROR);
            try {
                if (ConfigurationOntology.RECORD_HANDLER_STATISTICS.equals(serviceName)) {
                    if (configuration.isRecordHandlerStatistics()) {
                        configuration.setRecordHandlerStatistics(false);
                        stopRecordingHandlerStatistics();
                    }
                }
                else if (ConfigurationOntology.RECORD_MEMORY_USAGE.equals(serviceName)) {
                    if (resourcesAgentController != null) {
                        configuration.setRecordMemoryUsage(false);
                        stopRecordingMemoryUsage();
                    }
                }
                else {
                    throw new Exception("service inconnu");
                }
            }
            catch (Exception e) {
                processError(serviceName, e, DISABLE_SERVICE_ERROR);
            }
        }


        private String findServiceName(String[] splitted, String errorMessage) throws FailureException {
            if (splitted.length <= 1) {
                throw new FailureException(errorMessage + ": service non renseigné");
            }

            return splitted[1];
        }


        private void initLogs() {
            String logDir = configuration.getAuditDestinationDir();
            logReader.setPath(logDir);
            try {
                administrationLogFile.init(logDir + FILE_SEPARATOR + AUDIT_LOG);
            }
            catch (IOException e) {
                LOGGER.error("Impossible de changer le répertoire de destination des log.", e);
            }
        }


        private void processError(String serviceName, Exception e, String errorMessage)
              throws FailureException {
            LOGGER.error(errorMessage + serviceName, e);
            throw new FailureException(errorMessage + "'" + serviceName + "' : " + e.getMessage());
        }


        private void initServices() {
            if (configuration.isRecordHandlerStatistics()) {
                startRecordingHandlerStatistics();
            }

            if (configuration.isRecordMemoryUsage()) {
                try {
                    startRecordingMemoryUsage();
                }
                catch (ContainerFailureException e) {
                    LOGGER.error(
                          ENABLE_SERVICE_ERROR + ConfigurationOntology.RECORD_MEMORY_USAGE
                          + ConfigurationOntology.RECORD_MEMORY_USAGE,
                          e);
                }
            }
        }


        public void closeServices() throws BadControllerException {
            if (configuration.isRecordHandlerStatistics()) {
                configuration.setRecordHandlerStatistics(false);
                stopRecordingHandlerStatistics();
            }
            if (configuration.isRecordMemoryUsage()) {
                configuration.setRecordMemoryUsage(false);
                resourcesAgentController.kill();
            }
        }


        private void startRecordingHandlerStatistics() {
            handlerListener = new HandlerExecutionSpy(administrationLogFile);
            madServerOperations.addHandlerListener(handlerListener);
        }


        private void stopRecordingHandlerStatistics() {
            madServerOperations.removeHandlerListener(handlerListener);
            handlerListener = null;
        }


        private void startRecordingMemoryUsage() throws ContainerFailureException {
            MemoryWatcherAgent watcherAgent = new MemoryWatcherAgent(new DefaultMemoryProbe(),
                                                                     new ResourcesAgentHandler(
                                                                           administrationLogFile));

            resourcesAgentController = getAgentContainer().acceptNewAgent(
                  MANAGE_RESOURCES_SERVICE_TYPE,
                  watcherAgent);
            resourcesAgentController.start();
            resourcesAgentAid = watcherAgent.getAID();
        }


        private void stopRecordingMemoryUsage() throws BadControllerException {
            addBehaviour(new HitmanBehaviour(AdministrationServerConfigurationAgent.this, resourcesAgentAid));
        }
    }
}