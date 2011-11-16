package net.codjo.administration.server.operation.log;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.Constants;
import net.codjo.administration.common.XmlCodec;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.DFService;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;
import net.codjo.agent.protocol.AbstractRequestParticipantHandler;
import net.codjo.agent.protocol.BasicQueryParticipantHandler;
import net.codjo.agent.protocol.FailureException;
import net.codjo.agent.protocol.RequestParticipant;
import net.codjo.agent.protocol.RequestProtocol;
import org.apache.log4j.Logger;

public class LogReaderAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(LogReaderAgent.class);
    private final LogReader logReader;


    public LogReaderAgent(LogReader logReader) {
        this.logReader = logReader;
    }


    public String getLogFiles() {
        return XmlCodec.listToXml(logReader.getLogFiles());
    }


    @Override
    protected void setup() {
        try {
            DFService.register(this, DFService.createAgentDescription(Constants.MANAGE_LOGS_SERVICE_TYPE));
        }
        catch (DFService.DFServiceException e) {
            die();
            LOGGER.error("Impossible d'inscrire l'agent aupres du DF : " + e.getLocalizedMessage(), e);
            return;
        }

        addBehaviour(new RequestParticipant(this,
                                            new BasicQueryParticipantHandler(this),
                                            and(matchPerformative(AclMessage.Performative.QUERY),
                                                matchProtocol(RequestProtocol.QUERY))));
        addBehaviour(new RequestParticipant(this,
                                            new MyAbstractRequestParticipantHandler(),
                                            and(matchPerformative(AclMessage.Performative.REQUEST),
                                                matchProtocol(RequestProtocol.REQUEST))));
    }


    @Override
    protected void tearDown() {
        try {
            DFService.deregister(this);
        }
        catch (DFService.DFServiceException e) {
            LOGGER.error("Impossible de desinscrire l'agent aupres du DF : " + e.getLocalizedMessage(), e);
        }
    }


    private class MyAbstractRequestParticipantHandler extends AbstractRequestParticipantHandler {
        public AclMessage executeRequest(AclMessage request, AclMessage agreement) throws FailureException {
            String content = request.getContent();
            String[] splitted = content.split(" ");
            if (AdministrationOntology.READ_LOG_ACTION.equals(splitted[0])) {
                return read(request, splitted);
            }
            return request.createReply(Performative.INFORM);
        }


        private AclMessage read(AclMessage request, String[] splitted) throws FailureException {
            if (splitted.length <= 1) {
                throw new FailureException("Impossible de lire le fichier de log : fichier non renseigné");
            }

            String logFile = splitted[1];
            try {
                AclMessage response = request.createReply(Performative.INFORM);
                response.setContent(XmlCodec.logToXml(logReader.readLog(logFile)));
                return response;
            }
            catch (Exception e) {
                LOGGER.error("Impossible de lire le fichier de log " + logFile, e);
                throw new FailureException(
                      "Impossible de lire le fichier de log " + logFile + " : " + e.getMessage());
            }
        }
    }
}
