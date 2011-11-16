package net.codjo.administration.gui.plugin;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class SystemPropertiesPanel {

    private JPanel mainPanel;
    private JTextArea propertiesTextArea;
    private JTextArea environmentTextArea;


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
}
