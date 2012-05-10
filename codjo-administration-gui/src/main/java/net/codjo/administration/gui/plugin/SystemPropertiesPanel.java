package net.codjo.administration.gui.plugin;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.codjo.i18n.gui.InternationalizableContainer;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.i18n.InternationalizationUtil;

public class SystemPropertiesPanel implements InternationalizableContainer {

    private JPanel mainPanel;
    private JTextArea propertiesTextArea;
    private JTextArea environmentTextArea;
    private JPanel systemPropertiesPanel;
    private JPanel environmentPanel;


    public SystemPropertiesPanel() {
        propertiesTextArea.setName("systemPropertiesContent");
        propertiesTextArea.setEditable(false);

        environmentTextArea.setName("systemEnvironmentContent");
        environmentTextArea.setEditable(false);
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }


    public void setSystemProperties(String value) {
        propertiesTextArea.setText(value);
    }


    public void setSystemEnvironment(String value) {
        environmentTextArea.setText(value);
    }


    public void addInternationalizableComponents(TranslationNotifier translationNotifier) {
        translationNotifier.addInternationalizableComponent(systemPropertiesPanel,
                                                            "SystemPropertiesPanel.systemPropertiesPanel.title");
        translationNotifier.addInternationalizableComponent(environmentPanel,
                                                            "SystemPropertiesPanel.environmentPanel.title");
    }


    public void init(GuiContext guiContext) {
        TranslationNotifier translationNotifier = InternationalizationUtil.retrieveTranslationNotifier(guiContext);
        translationNotifier.addInternationalizableContainer(this);
    }
}
