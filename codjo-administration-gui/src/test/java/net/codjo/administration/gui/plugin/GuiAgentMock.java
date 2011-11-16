package net.codjo.administration.gui.plugin;
import net.codjo.agent.GuiAgent;
import net.codjo.agent.GuiEvent;
import net.codjo.test.common.LogString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GuiAgentMock extends GuiAgent {
    private LogString log;


    public GuiAgentMock(LogString log) {
        this.log = log;
    }


    @Override
    public void postGuiEvent(GuiEvent event) {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(event.getType());
        Iterator allParameter = event.getAllParameter();
        while (allParameter.hasNext()) {
            Object parameter = allParameter.next();
            parameters.add(parameter);
        }
        log.call("postGuiEvent", parameters.toArray());
    }


    @Override
    protected void onGuiEvent(GuiEvent guiEvent) {
    }
}
