package net.codjo.administration.gui.plugin;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.codjo.i18n.gui.InternationalizableContainer;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.i18n.InternationalizationUtil;

public class AuditLogPanel implements InternationalizableContainer {
    private JPanel mainPanel;
    private JComboBox logs;
    private JButton readLog;
    private JTextArea log;


    public AuditLogPanel() {
        log.setTabSize(12);

        logs.setName("logs");
        readLog.setName("readLog");
        log.setName("logContent");
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }


    public JComboBox getLogs() {
        return logs;
    }


    public JButton getReadLog() {
        return readLog;
    }


    public JTextArea getLog() {
        return log;
    }


    public void init(GuiContext guiContext) {
        TranslationNotifier translationNotifier = InternationalizationUtil.retrieveTranslationNotifier(guiContext);
        translationNotifier.addInternationalizableContainer(this);
    }


    public void addInternationalizableComponents(TranslationNotifier translationNotifier) {
        translationNotifier.addInternationalizableComponent(readLog, "AuditLogPanel.readLogButton", null);
    }
}
