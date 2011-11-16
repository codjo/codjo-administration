package net.codjo.administration.server.audit.memory;
import net.codjo.administration.common.Constants;
import net.codjo.agent.Agent;
import net.codjo.agent.DFService;
import net.codjo.agent.behaviour.TickerBehaviour;
import org.apache.log4j.Logger;

public class MemoryWatcherAgent extends Agent {
    private static final Logger LOGGER = Logger.getLogger(MemoryWatcherAgent.class);
    private static final int DEFAULT_PERIOD = 5 * 60 * 1000;
    private MemoryProbe memoryProbe;
    private Handler handler;
    private int period;


    public MemoryWatcherAgent(MemoryProbe memoryProbe, Handler handler) {
        this.memoryProbe = memoryProbe;
        this.handler = handler;

        setPeriod(DEFAULT_PERIOD);
    }


    public int getPeriod() {
        return period;
    }


    public void setPeriod(int period) {
        this.period = period;
    }


    @Override
    protected void setup() {
        try {
            DFService.register(this,
                               DFService.createAgentDescription(Constants.MANAGE_RESOURCES_SERVICE_TYPE));
        }
        catch (DFService.DFServiceException e) {
            die();
            LOGGER.error("Impossible d'inscrire l'agent auprès du DF : " + e.getLocalizedMessage(), e);
            return;
        }

        addBehaviour(new ReadResourcesBehaviour(period));
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


    public interface Handler {
        void memory(double used, double total);
    }

    private class ReadResourcesBehaviour extends TickerBehaviour {
        private ReadResourcesBehaviour(int period) {
            super(MemoryWatcherAgent.this, period);
        }


        @Override
        public void onTick() {
            handler.memory(memoryProbe.getUsedMemory(), memoryProbe.getTotalMemory());
        }
    }
}
