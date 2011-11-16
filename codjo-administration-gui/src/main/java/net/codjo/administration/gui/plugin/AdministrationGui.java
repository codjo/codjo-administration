package net.codjo.administration.gui.plugin;
import static net.codjo.administration.gui.plugin.ActionType.CHANGE_LOG_DIR;
import static net.codjo.administration.gui.plugin.ActionType.CLOSE;
import static net.codjo.administration.gui.plugin.ActionType.DISABLE_SERVICE;
import static net.codjo.administration.gui.plugin.ActionType.ENABLE_SERVICE;
import static net.codjo.administration.gui.plugin.ActionType.READ_LOG;
import static net.codjo.administration.gui.plugin.ActionType.RESTORE_LOG_DIR;
import static net.codjo.administration.gui.plugin.ActionType.START_PLUGIN;
import static net.codjo.administration.gui.plugin.ActionType.STOP_PLUGIN;
import net.codjo.agent.GuiAgent;
import net.codjo.agent.GuiEvent;
import net.codjo.gui.toolkit.HelpButton;
import net.codjo.gui.toolkit.LabelledItemPanel;
import net.codjo.gui.toolkit.waiting.WaitingPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

class AdministrationGui extends JInternalFrame implements ActionListener {
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final GuiAgent guiAgent;
    private final GuiLocker guiLocker;
    private final LabelledItemPanel pluginsPanel = new LabelledItemPanel();
    private final AuditLogPanel auditLogPanel = new AuditLogPanel();
    private final ConfigurationPanel configurationPanel = new ConfigurationPanel();
    private final SystemPropertiesPanel systemPropertiesPanel = new SystemPropertiesPanel();
    private static final String URL_CONFLUENCE
          = "http://wp-confluence/confluence/display/framework/Guide+Utilisateur+IHM+de+agf-administration";


    AdministrationGui(GuiAgent guiAgent) {
        this(guiAgent, new WaitingPanel());
    }


    AdministrationGui(GuiAgent guiAgent, WaitingPanel waitingPanel) {
        super("Administration du serveur", true, false, true, true);

        this.guiAgent = guiAgent;
        this.guiLocker = new GuiLocker(waitingPanel);

        initGui();
    }


    public void actionPerformed(ActionEvent event) {
        if (CLOSE.name().equals(event.getActionCommand())) {
            guiAgent.postGuiEvent(new GuiEvent(this, CLOSE.ordinal()));
        }
        else if (START_PLUGIN.name().equals(event.getActionCommand())) {
            postGuiEvent(START_PLUGIN, ((JButton)event.getSource()).getName().replaceAll(".start", ""));
        }
        else if (STOP_PLUGIN.name().equals(event.getActionCommand())) {
            postGuiEvent(STOP_PLUGIN, ((JButton)event.getSource()).getName().replaceAll(".stop", ""));
        }
        else if (READ_LOG.name().equals(event.getActionCommand())) {
            postGuiEvent(READ_LOG, auditLogPanel.getLogs().getSelectedItem());
        }
        else if (ENABLE_SERVICE.name().equals(event.getActionCommand())) {
            postGuiEvent(ENABLE_SERVICE, ((JComponent)event.getSource()).getName().replaceAll(".enable", ""));
        }
        else if (DISABLE_SERVICE.name().equals(event.getActionCommand())) {
            postGuiEvent(DISABLE_SERVICE,
                         ((JComponent)event.getSource()).getName().replaceAll(".disable", ""));
        }
        else if (CHANGE_LOG_DIR.name().equals(event.getActionCommand())) {
            postGuiEvent(CHANGE_LOG_DIR, configurationPanel.getDirectoryLog());
        }
        else if (RESTORE_LOG_DIR.name().equals(event.getActionCommand())) {
            postGuiEvent(RESTORE_LOG_DIR);
        }
    }


    public synchronized void lockGui() {
        guiLocker.lock();
    }


    public synchronized void unlockGui() {
        guiLocker.unlock();
    }


    public void initPlugins(String... plugins) {
        for (String plugin : plugins) {
            JPanel panel = new JPanel();
            JButton startButton = new JButton("Start");
            startButton.setName(plugin + ".start");
            startButton.setActionCommand(START_PLUGIN.name());
            startButton.addActionListener(this);
            panel.add(startButton);

            JButton stopButton = new JButton("Stop");
            stopButton.setName(plugin + ".stop");
            stopButton.setActionCommand(STOP_PLUGIN.name());
            stopButton.addActionListener(this);
            panel.add(stopButton);

            pluginsPanel.addItem(plugin, panel);
        }
    }


    public void initLogs(String... logs) {
        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel(logs);
        auditLogPanel.getLogs().setModel(comboBoxModel);
    }


    public void showLog(String content) {
        auditLogPanel.getLog().setText(content);
    }


    public void initServices(String[] services) {
        for (String service : services) {
            configurationPanel.initService(service);
        }
    }


    public void enableService(String service) {
        configurationPanel.enableService(service);
    }


    public void disableService(String service) {
        configurationPanel.disableService(service);
    }


    public void setSystemProperties(String value) {
        systemPropertiesPanel.setSystemProperties(value);
    }


    public void setSystemEnvironment(String value) {
        systemPropertiesPanel.setSystemEnvironment(value);
    }


    private void initGui() {
        setGlassPane(guiLocker.getWaitingPanel());
        add(tabbedPane);

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        HelpButton helpButton = new HelpButton();
        helpButton.setHelpUrl(URL_CONFLUENCE);
        JButton closeButton = new JButton("Fermer");
        buttonsPanel.add(helpButton);
        buttonsPanel.add(Box.createGlue());
        closeButton.setActionCommand(CLOSE.name());
        closeButton.setName("close");
        closeButton.addActionListener(this);
        buttonsPanel.add(closeButton);
        add(buttonsPanel, BorderLayout.SOUTH);

        initConfigurationPanel();
        initReadLogsPanel();
        initStartStopPluginsPanel();
        initSystemPropertiesPanel();
    }


    private void initConfigurationPanel() {
        tabbedPane.add("Pilotage", new JScrollPane(configurationPanel.getMainPanel()));
        configurationPanel.init(this);
    }


    private void initReadLogsPanel() {
        tabbedPane.add("Visualisation des logs", auditLogPanel.getMainPanel());

        auditLogPanel.getReadLog().setActionCommand(READ_LOG.name());
        auditLogPanel.getReadLog().addActionListener(this);
    }


    private void initStartStopPluginsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        tabbedPane.add("Arrêt/Relance des plugins", panel);

        panel.add(new JScrollPane(pluginsPanel));
    }


    private void initSystemPropertiesPanel() {
        tabbedPane.add("Propriétés système", systemPropertiesPanel.getMainPanel());
    }


    private void postGuiEvent(ActionType actionType, Object... parameter) {
        GuiEvent guiEvent = new GuiEvent(this, actionType.ordinal());
        for (Object param : parameter) {
            guiEvent.addParameter(param);
        }
        guiAgent.postGuiEvent(guiEvent);
    }


    static class GuiLocker {
        private final WaitingPanel waitingPanel;
        private int counter = 0;


        GuiLocker(WaitingPanel waitingPanel) {
            this.waitingPanel = waitingPanel;
        }


        public WaitingPanel getWaitingPanel() {
            return waitingPanel;
        }


        public synchronized void lock() {
            if (counter == 0) {
                waitingPanel.setVisible(true);
                waitingPanel.startAnimation();
            }
            counter++;
        }


        public synchronized void unlock() {
            counter--;
            if (counter == 0) {
                waitingPanel.stopAnimation();
                waitingPanel.setVisible(false);
            }
        }
    }
}
