package net.codjo.administration.server.operation.configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.administration.common.Constants;
import net.codjo.administration.common.XmlCodec;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.administration.server.audit.jdbc.JdbcExecutionSpy;
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
import net.codjo.agent.behaviour.HitmanBehaviour;
import net.codjo.agent.protocol.AbstractRequestParticipantHandler;
import net.codjo.agent.protocol.BasicQueryParticipantHandler;
import net.codjo.agent.protocol.FailureException;
import net.codjo.agent.protocol.RequestParticipant;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.mad.server.handler.HandlerListener;
import net.codjo.mad.server.plugin.MadServerOperations;
import net.codjo.sql.server.JdbcManager;
import net.codjo.util.time.TimeSource;
import org.apache.log4j.Logger;

import static net.codjo.administration.common.Constants.MANAGE_RESOURCES_SERVICE_TYPE;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;

public class AdministrationServerConfigurationAgent extends Agent {
    static final String USER_LIST_SEPARATOR = ",";

    private static final Logger LOGGER = Logger.getLogger(AdministrationServerConfigurationAgent.class);
    private static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static final String AUDIT_LOG = "audit.log";

    private DefaultAdministrationServerConfiguration configuration;
    private MadServerOperations madServerOperations;
    private final AdministrationLogFile administrationLogFile;
    private AgentController resourcesAgentController;
    private HandlerListener handlerListener;
    private MyAbstractRequestParticipantHandler participantHandler;
    private DefaultLogReader logReader;
    private final JdbcManager jdbcManager;
    private final JdbcExecutionSpy jdbcExecutionSpy;


    public AdministrationServerConfigurationAgent(DefaultAdministrationServerConfiguration configuration,
                                                  MadServerOperations madServerOperations,
                                                  DefaultLogReader logReader,
                                                  JdbcManager jdbcManager) {
        this(configuration, madServerOperations, new AdministrationLogFile(), logReader, jdbcManager);
    }


    public AdministrationServerConfigurationAgent(DefaultAdministrationServerConfiguration configuration,
                                                  MadServerOperations madServerOperations,
                                                  AdministrationLogFile logFile,
                                                  DefaultLogReader logReader, JdbcManager jdbcManager) {
        this.configuration = configuration;
        this.madServerOperations = madServerOperations;
        this.administrationLogFile = logFile;
        this.jdbcExecutionSpy = new JdbcExecutionSpy(administrationLogFile, createTimeSource());
        this.logReader = logReader;
        this.jdbcManager = jdbcManager;

        if (configuration.isRecordHandlerStatistics() || configuration.isRecordMemoryUsage()
            || configuration.isRecordJdbcStatistics()) {
            try {
                administrationLogFile.init(
                      configuration.getAuditDestinationDir() + FILE_SEPARATOR + AUDIT_LOG);
            }
            catch (IOException e) {
                LOGGER.error("impossible d'initialiser le repertoire de log", e);
            }
        }
    }


    protected TimeSource createTimeSource() {
        return null;
    }


    public String getServices() {
        List<String> services = new ArrayList<String>();
        services.add(ConfigurationOntology.AUDIT_DESTINATION_DIR + " " +
                     configuration.getAuditDestinationDir());
        String filter = configuration.getJdbcUsersFilter();
        services.add(ConfigurationOntology.JDBC_USERS_FILTER + ((filter == null) ? "" : " " +
                                                                                        filter));
        services.add(serviceWithState(ConfigurationOntology.RECORD_HANDLER_STATISTICS,
                                      configuration.isRecordHandlerStatistics()));
        services.add(serviceWithState(ConfigurationOntology.RECORD_JDBC_STATISTICS,
                                      configuration.isRecordJdbcStatistics()));
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


    void updateJdbcUsersFilter() {
        jdbcManager.clearConnectionFactories();
        jdbcManager.clearConnectionPoolListeners();

        LOGGER.debug("updateJdbcUsersFilter: isRecordJdbcStatistics=" + configuration.isRecordJdbcStatistics());
        if (configuration.isRecordJdbcStatistics()) {
            List<String> currentUsers = toList(configuration.getJdbcUsersFilter());
            LOGGER.debug("updateJdbcUsersFilter: currentUsers=" + (currentUsers.isEmpty() ?
                                                                   "ALL_USERS" :
                                                                   configuration.getJdbcUsersFilter()));

            if (currentUsers.isEmpty()) {
                jdbcManager.setDefaultConnectionFactory(jdbcExecutionSpy);
                jdbcManager.addConnectionPoolListener(jdbcExecutionSpy); // listener for all users
            }
            else {
                for (String user : currentUsers) {
                    jdbcManager.setConnectionFactory(user, jdbcExecutionSpy);
                    jdbcManager.addConnectionPoolListener(jdbcExecutionSpy, user);
                }
            }
        }

        LOGGER.debug("updateJdbcUsersFilter: jdbcManager=" + jdbcManager);
    }


    private List<String> toList(String list) {
        List<String> result = Collections.<String>emptyList();
        if (list != null) {
            list = list.trim();
            if (list.length() > 0) {
                result = Arrays.asList(list.split(USER_LIST_SEPARATOR));
            }
        }
        return result;
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
            else if (AdministrationOntology.CHANGE_JDBC_USERS_FILTER.equals(splitted[0])) {
                String newFilter = (splitted.length > 1) ? splitted[1] : null;
                configuration.setJdbcUsersFilter(newFilter);
                updateJdbcUsersFilter();
                response.setContent(XmlCodec.logToXml(configuration.getJdbcUsersFilter()));
            }
            else if (AdministrationOntology.RESTORE_JDBC_USERS_FILTER.equals(splitted[0])) {
                configuration.restoreDefaultJdbcUsersFilter();
                updateJdbcUsersFilter();
                response.setContent(XmlCodec.logToXml(configuration.getJdbcUsersFilter()));
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
                else if (ConfigurationOntology.RECORD_JDBC_STATISTICS.equals(serviceName)) {
                    if (!configuration.isRecordJdbcStatistics()) {
                        configuration.setRecordJdbcStatistics(true);
                        updateJdbcUsersFilter();
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
                else if (ConfigurationOntology.RECORD_JDBC_STATISTICS.equals(serviceName)) {
                    if (configuration.isRecordJdbcStatistics()) {
                        configuration.setRecordJdbcStatistics(false);
                        updateJdbcUsersFilter();
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

            if (configuration.isRecordJdbcStatistics()) {
                updateJdbcUsersFilter();
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
            if (configuration.isRecordJdbcStatistics()) {
                configuration.setRecordJdbcStatistics(false);
                updateJdbcUsersFilter();
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