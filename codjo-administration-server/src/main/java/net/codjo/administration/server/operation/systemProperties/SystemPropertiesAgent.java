package net.codjo.administration.server.operation.systemProperties;
import net.codjo.agent.Agent;
import net.codjo.agent.DFService;
import net.codjo.agent.AclMessage;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import static net.codjo.agent.MessageTemplate.matchProtocol;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.protocol.AbstractRequestParticipantHandler;
import net.codjo.agent.protocol.FailureException;
import net.codjo.agent.protocol.RequestParticipant;
import net.codjo.agent.protocol.RequestProtocol;
import net.codjo.administration.common.Constants;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.XmlCodec;
import org.apache.log4j.Logger;
/**
 *
 */
public class SystemPropertiesAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(SystemPropertiesAgent.class);
    private final SystemProperties systemProperties;

    public SystemPropertiesAgent(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
    }

    @Override
    protected void setup() {
        try {
            DFService.register(this, DFService.createAgentDescription(Constants.MANAGE_SYSTEM_PROPERTIES_TYPE));
        }
        catch (DFService.DFServiceException e) {
            die();
            LOGGER.error("Impossible d'inscrire l'agent aupres du DF : " + e.getLocalizedMessage(), e);
            return;
        }

        addBehaviour(new RequestParticipant(this,
                                            new SystemPropertiesHandler(),
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


    private class SystemPropertiesHandler extends AbstractRequestParticipantHandler {
        public AclMessage executeRequest(AclMessage request, AclMessage agreement) throws FailureException {
            String content = request.getContent();

            if (AdministrationOntology.DISPLAY_SYSTEM_PROPERTIES.equals(content)) {
                AclMessage response = request.createReply(Performative.INFORM);
                response.setContent(XmlCodec.logToXml(systemProperties.getSystemProperties()));
                return response;
            } else if (AdministrationOntology.DISPLAY_SYSTEM_ENVIRONMENT.equals(content)) {
                AclMessage response = request.createReply(Performative.INFORM);
                response.setContent(XmlCodec.logToXml(systemProperties.getSystemEnvironment()));
                return response;
            }

            throw new FailureException("Impossible de lire les propriétés système.");
        }
    }
}
