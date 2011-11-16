package net.codjo.administration.gui.plugin;
import net.codjo.mad.gui.base.AbstractGuiPlugin;
import net.codjo.mad.gui.base.GuiConfiguration;
import net.codjo.mad.gui.i18n.AbstractInternationalizableGuiPlugin;
import net.codjo.plugin.common.ApplicationCore;
import net.codjo.i18n.common.TranslationManager;
import net.codjo.i18n.common.Language;

public class AdministrationGuiPlugin extends AbstractInternationalizableGuiPlugin {
    private ApplicationCore applicationCore;


    public AdministrationGuiPlugin(ApplicationCore applicationCore) {
        this.applicationCore = applicationCore;
    }


    @Override
    protected void registerLanguageBundles(TranslationManager translationManager) {
        translationManager.addBundle("net.codjo.administration.gui.i18n", Language.FR);
        translationManager.addBundle("net.codjo.administration.gui.i18n", Language.EN);
    }


    @Override
    public void initGui(GuiConfiguration configuration) throws Exception {
        super.initGui(configuration);
        AdministrationAction administrationAction =
              new AdministrationAction(configuration.getGuiContext(), applicationCore.getAgentContainer());
        configuration.registerAction(this, "AdministrationAction", administrationAction);
    }
}
