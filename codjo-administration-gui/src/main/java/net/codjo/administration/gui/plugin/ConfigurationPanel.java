package net.codjo.administration.gui.plugin;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.codjo.administration.common.AdministrationOntology;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.i18n.gui.InternationalizableContainer;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.i18n.InternationalizationUtil;

import static net.codjo.administration.gui.plugin.ActionType.DISABLE_SERVICE;
import static net.codjo.administration.gui.plugin.ActionType.ENABLE_SERVICE;
/**
 *
 */
public class ConfigurationPanel implements InternationalizableContainer {
    private JPanel mainPanel;
    private JButton recordHandlerStatisticsButton;
    private JButton recordMemoryUsageButton;
    private javax.swing.JTextField directoryLog;
    private JButton undoButton;
    private JButton applyButton;
    private JButton resetButton;
    private JLabel handlerAuditLabel;
    private JLabel memoryAuditLabel;
    private JLabel logDirectoryLabel;
    private JLabel jdbcAuditLabel;
    private JButton recordJdbcStatisticsButton;
    private JTextField jdbcUsersFilter;
    private JButton undoJdbcButton;
    private JButton applyJdbcButton;
    private JButton resetJdbcButton;
    private JLabel jdbcUsersFilterLabel;
    private ImageIcon enableIcon = new ImageIcon(getClass().getResource("play.png"));
    private ImageIcon disableIcon = new ImageIcon(getClass().getResource("pause.png"));
    private ImageIcon undoIcon = new ImageIcon(getClass().getResource("undo.gif"));
    private ImageIcon applyIcon = new ImageIcon(getClass().getResource("apply.png"));
    private ImageIcon resetIcon = new ImageIcon(getClass().getResource("reload.png"));
    private UndoActionListener undoActionListener;
    private UndoActionListener undoJdbcActionListener;
    private GuiContext guiContext;


    public ConfigurationPanel() {
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }


    public void addInternationalizableComponents(TranslationNotifier translationNotifier) {
        translationNotifier.addInternationalizableComponent(handlerAuditLabel, "ConfigurationPanel.handlerAuditLabel");
        translationNotifier.addInternationalizableComponent(memoryAuditLabel, "ConfigurationPanel.memoryAuditLabel");
        translationNotifier.addInternationalizableComponent(logDirectoryLabel, "ConfigurationPanel.logDirectoryLabel");
        translationNotifier.addInternationalizableComponent(recordHandlerStatisticsButton,
                                                            null,
                                                            "ConfigurationPanel.recordHandlerStatisticsButton.activate.tooltip");
        translationNotifier.addInternationalizableComponent(recordMemoryUsageButton,
                                                            null,
                                                            "ConfigurationPanel.recordMemoryUsageButton.deactivate.tooltip");
        translationNotifier.addInternationalizableComponent(undoButton, null, "ConfigurationPanel.undoButton.tooltip");
        translationNotifier.addInternationalizableComponent(applyButton,
                                                            null,
                                                            "ConfigurationPanel.applyButton.tooltip");
        translationNotifier.addInternationalizableComponent(resetButton,
                                                            null,
                                                            "ConfigurationPanel.resetButton.tooltip");

        translationNotifier.addInternationalizableComponent(jdbcAuditLabel, "ConfigurationPanel.jdbcAuditLabel");
        translationNotifier.addInternationalizableComponent(recordJdbcStatisticsButton,
                                                            null,
                                                            "ConfigurationPanel.recordJdbcStatisticsButton.activate.tooltip");
        translationNotifier.addInternationalizableComponent(jdbcUsersFilterLabel,
                                                            "ConfigurationPanel.jdbcUsersFilterLabel");
        translationNotifier.addInternationalizableComponent(undoJdbcButton,
                                                            null,
                                                            "ConfigurationPanel.undoJdbcButton.tooltip");
        translationNotifier.addInternationalizableComponent(applyJdbcButton,
                                                            null,
                                                            "ConfigurationPanel.applyJdbcButton.tooltip");
        translationNotifier.addInternationalizableComponent(resetJdbcButton,
                                                            null,
                                                            "ConfigurationPanel.resetJdbcButton.tooltip");
    }


    public void init(ActionListener actionListener, GuiContext guiContext) {
        initI18n(guiContext);

        recordHandlerStatisticsButton.addActionListener(actionListener);
        recordJdbcStatisticsButton.addActionListener(actionListener);
        recordMemoryUsageButton.addActionListener(actionListener);
        directoryLog.addKeyListener(createKeyListener(undoButton, applyButton));
        jdbcUsersFilter.addKeyListener(createKeyListener(undoJdbcButton, applyJdbcButton));
        undoButton.setIcon(undoIcon);
        undoActionListener = new UndoActionListener(directoryLog, undoButton, applyButton);
        undoButton.addActionListener(undoActionListener);

        applyButton.setIcon(applyIcon);
        applyButton.setActionCommand(ActionType.CHANGE_LOG_DIR.name());
        applyButton.addActionListener(actionListener);

        resetButton.setIcon(resetIcon);
        resetButton.setActionCommand(ActionType.RESTORE_LOG_DIR.name());
        resetButton.addActionListener(actionListener);

        undoJdbcButton.setIcon(undoIcon);
        undoJdbcActionListener = new UndoActionListener(jdbcUsersFilter, undoJdbcButton, applyJdbcButton);
        undoJdbcButton.addActionListener(undoJdbcActionListener);

        applyJdbcButton.setIcon(applyIcon);
        applyJdbcButton.setActionCommand(ActionType.CHANGE_JDBC_USERS_FILTER.name());
        applyJdbcButton.addActionListener(actionListener);

        resetJdbcButton.setIcon(resetIcon);
        resetJdbcButton.setActionCommand(ActionType.RESTORE_JDBC_USERS_FILTER.name());
        resetJdbcButton.addActionListener(actionListener);

        recordHandlerStatisticsButton.setName(ConfigurationOntology.RECORD_HANDLER_STATISTICS);
        recordJdbcStatisticsButton.setName(ConfigurationOntology.RECORD_JDBC_STATISTICS);
        recordMemoryUsageButton.setName(ConfigurationOntology.RECORD_MEMORY_USAGE);
        directoryLog.setName(ConfigurationOntology.AUDIT_DESTINATION_DIR);
        jdbcUsersFilter.setName(ConfigurationOntology.JDBC_USERS_FILTER);
    }


    private KeyAdapter createKeyListener(final JButton undoButton, final JButton applyButton) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                e.getComponent().setForeground(Color.BLUE);

                if (!undoButton.isEnabled()) {
                    undoButton.setEnabled(true);
                }
                if (!applyButton.isEnabled()) {
                    applyButton.setEnabled(true);
                }
            }
        };
    }


    private void initI18n(GuiContext context) {
        this.guiContext = context;
        TranslationNotifier translationNotifier = InternationalizationUtil.retrieveTranslationNotifier(guiContext);
        translationNotifier.addInternationalizableContainer(this);
    }


    public void initService(String service) {
        String[] spStrings = service.split(" ");
        String parameter1 = (spStrings.length >= 2) ? spStrings[1] : "";

        if (parameter1.equals(AdministrationOntology.ENABLE_SERVICE_ACTION)) {
            enableService(spStrings[0]);
        }
        else if (parameter1.equals(AdministrationOntology.DISABLE_SERVICE_ACTION)) {
            disableService(spStrings[0]);
        }
        else if (spStrings[0].equals(directoryLog.getName())) {
            directoryLog.setText(parameter1);
            undoActionListener.setInitialText(spStrings[1]);
            applyButton.setEnabled(false);
            undoButton.setEnabled(false);
        }
        else if (spStrings[0].equals(jdbcUsersFilter.getName())) {
            jdbcUsersFilter.setText(parameter1);
            undoJdbcActionListener.setInitialText(parameter1);
            applyJdbcButton.setEnabled(false);
            undoJdbcButton.setEnabled(false);
        }
    }


    public void disableService(String serviceName) {
        if (recordHandlerStatisticsButton.getName().equals(serviceName)) {
            changeStateButton(recordHandlerStatisticsButton,
                              enableIcon,
                              ENABLE_SERVICE,
                              "ConfigurationPanel.recordHandlerStatisticsButton.activate.tooltip");
        }
        else if (recordMemoryUsageButton.getName().equals(serviceName)) {
            changeStateButton(recordMemoryUsageButton,
                              enableIcon,
                              ENABLE_SERVICE,
                              "ConfigurationPanel.recordMemoryUsageButton.activate.tooltip");
        }
        else if (recordJdbcStatisticsButton.getName().equals(serviceName)) {
            changeStateButton(recordJdbcStatisticsButton,
                              enableIcon,
                              ENABLE_SERVICE,
                              "ConfigurationPanel.recordJdbcStatisticsButton.activate.tooltip");
        }
    }


    public void enableService(String serviceName) {
        if (recordHandlerStatisticsButton.getName().equals(serviceName)) {
            changeStateButton(recordHandlerStatisticsButton,
                              disableIcon,
                              DISABLE_SERVICE,
                              "ConfigurationPanel.recordHandlerStatisticsButton.deactivate.tooltip");
        }
        else if (recordMemoryUsageButton.getName().equals(serviceName)) {
            changeStateButton(recordMemoryUsageButton,
                              disableIcon,
                              DISABLE_SERVICE,
                              "ConfigurationPanel.recordMemoryUsageButton.deactivate.tooltip");
        }
        else if (recordJdbcStatisticsButton.getName().equals(serviceName)) {
            changeStateButton(recordJdbcStatisticsButton,
                              disableIcon,
                              DISABLE_SERVICE,
                              "ConfigurationPanel.recordJdbcStatisticsButton.deactivate.tooltip");
        }
    }


    private void changeStateButton(JButton button, ImageIcon icon, ActionType command, String key) {
        button.setActionCommand(command.name());
        button.setIcon(icon);
        button.setToolTipText(InternationalizationUtil.translate(key, guiContext));
    }


    private static class UndoActionListener implements ActionListener {
        private final JTextField textField;
        private final JButton undoButton;
        private final JButton applyButton;

        private Color initialForeground;
        private String initialText;


        UndoActionListener(JTextField textField, JButton undoButton, JButton applyButton) {
            this.textField = textField;
            this.undoButton = undoButton;
            this.applyButton = applyButton;
            initialForeground = textField.getForeground();
        }


        public void setInitialText(String initialText) {
            this.initialText = initialText;
        }


        public void actionPerformed(ActionEvent e) {
            textField.setForeground(initialForeground);
            textField.setText(initialText);

            if (undoButton.isEnabled()) {
                undoButton.setEnabled(false);
                undoButton.transferFocus();
            }
            if (applyButton.isEnabled()) {
                applyButton.setEnabled(false);
                applyButton.transferFocus();
            }
        }
    }


    public String getDirectoryLog() {
        return directoryLog.getText();
    }


    public String getJdbcUsersFilter() {
        return jdbcUsersFilter.getText();
    }
}
