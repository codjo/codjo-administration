package net.codjo.administration.server.operation.plugin;
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

public class PluginManagerAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(PluginManagerAgent.class);
    private final PluginManager pluginManager;


    public PluginManagerAgent(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }


    public String getPlugins() {
        return XmlCodec.listToXml(pluginManager.getPlugins());
    }


    @Override
    protected void setup() {
        try {
            DFService.register(this, DFService.createAgentDescription(Constants.MANAGE_PLUGINS_SERVICE_TYPE));
        }
        catch (DFService.DFServiceException e) {
            die();
            LOGGER.error("Impossible d'inscrire l'agent auprès du DF : " + e.getLocalizedMessage(), e);
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
            LOGGER.error("Impossible de se desinscrire l'agent auprès du DF : " + e.getLocalizedMessage(), e);
        }
    }


    private class MyAbstractRequestParticipantHandler extends AbstractRequestParticipantHandler {
        public AclMessage executeRequest(AclMessage request, AclMessage agreement) throws FailureException {
            String content = request.getContent();
            String[] splitted = content.split(" ");
            if (AdministrationOntology.START_PLUGIN_ACTION.equals(splitted[0])) {
                startPlugin(splitted);
            }
            else if (AdministrationOntology.STOP_PLUGIN_ACTION.equals(splitted[0])) {
                stopPlugin(splitted);
            }
            return request.createReply(Performative.INFORM);
        }


        private void startPlugin(String[] splitted) throws FailureException {
            if (splitted.length <= 1) {
                throw new FailureException("Impossible de démarrer le plugin : plugin non renseigné");
            }

            String pluginName = splitted[1];
            try {
                pluginManager.startPlugin(pluginName);
            }
            catch (Exception e) {
                LOGGER.error("Impossible de démarrer le plugin " + pluginName, e);
                throw new FailureException(
                      "Impossible de démarrer le plugin " + pluginName + " : " + e.getMessage());
            }
        }


        private void stopPlugin(String[] splitted) throws FailureException {
            if (splitted.length <= 1) {
                throw new FailureException("Impossible d'arrêter le plugin : plugin non renseigné");
            }

            String pluginName = splitted[1];
            try {
                pluginManager.stopPlugin(pluginName);
            }
            catch (Exception e) {
                LOGGER.error("Impossible d'arrêter le plugin " + pluginName, e);
                throw new FailureException(
                      "Impossible d'arrêter le plugin " + pluginName + " : " + e.getMessage());
            }
        }
    }
}
