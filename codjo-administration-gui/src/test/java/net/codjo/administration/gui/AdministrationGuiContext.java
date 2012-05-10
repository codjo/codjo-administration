package net.codjo.administration.gui;
import net.codjo.i18n.common.Language;
import net.codjo.i18n.common.TranslationManager;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
import net.codjo.mad.gui.util.InternationalizableGuiContext;
/**
 *
 */
public class AdministrationGuiContext extends InternationalizableGuiContext {
    public AdministrationGuiContext() {
        TranslationManager translationManager = InternationalizationUtil.retrieveTranslationManager(this);
        translationManager.addBundle("net.codjo.administration.gui.i18n", Language.FR);
        translationManager.addBundle("net.codjo.administration.gui.i18n", Language.EN);
    }
}
