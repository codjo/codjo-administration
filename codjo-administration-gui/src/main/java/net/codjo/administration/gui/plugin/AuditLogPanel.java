package net.codjo.administration.gui.plugin;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class AuditLogPanel {
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
}
