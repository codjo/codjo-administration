package net.codjo.administration.gui.plugin;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.ContainerFailureException;
import net.codjo.gui.toolkit.util.ErrorDialog;
import net.codjo.mad.gui.framework.GuiContext;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

class AdministrationAction extends AbstractAction {
    private GuiContext guiContext;
    private AgentContainer agentContainer;


    AdministrationAction(GuiContext guiContext, AgentContainer agentContainer) {
        super("Administration du serveur");
        this.guiContext = guiContext;
        this.agentContainer = agentContainer;

        setEnabled(guiContext.getUser().isAllowedTo(AdministrationFunctions.ADMINISTRATE_SERVER));
    }


    public void actionPerformed(ActionEvent event) {
        if (!guiContext.getUser().isAllowedTo(AdministrationFunctions.ADMINISTRATE_SERVER)) {
            ErrorDialog.show(guiContext.getMainFrame(),
                             "Erreur de sécurité",
                             "Vous n'avez pas les droits pour administrer les plugins.");
            return;
        }

        AdministrationGuiAgent agent = new AdministrationGuiAgent(guiContext);
        try {
            agentContainer.acceptNewAgent("administration-gui-agent", agent).start();
        }
        catch (ContainerFailureException e) {
            ErrorDialog.show(guiContext.getMainFrame(), "Impossible d'ouvrir la fenêtre.", e);
        }
    }
}
