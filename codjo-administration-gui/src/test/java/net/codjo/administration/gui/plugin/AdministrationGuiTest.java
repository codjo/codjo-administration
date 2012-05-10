package net.codjo.administration.gui.plugin;
import net.codjo.administration.common.ConfigurationOntology;
import static net.codjo.administration.gui.plugin.ActionType.CHANGE_LOG_DIR;
import static net.codjo.administration.gui.plugin.ActionType.CLOSE;
import static net.codjo.administration.gui.plugin.ActionType.READ_LOG;
import static net.codjo.administration.gui.plugin.ActionType.START_PLUGIN;
import static net.codjo.administration.gui.plugin.ActionType.STOP_PLUGIN;

import net.codjo.administration.gui.AdministrationGuiContext;
import net.codjo.gui.toolkit.waiting.WaitingPanel;
import net.codjo.test.common.LogString;
import javax.swing.JButton;
import org.uispec4j.Key;
import org.uispec4j.UISpecTestCase;
import org.uispec4j.Window;

public class AdministrationGuiTest extends UISpecTestCase {
    private static final String AUDIT_LOGS_TAB = "Visualisation des logs";
    private AdministrationGui gui;
    private Window window;
    private LogString log = new LogString();


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        log = new LogString();
        gui = new AdministrationGui(new AdministrationGuiContext(), new GuiAgentMock(log), new WaitingPanelMock(log));
        window = new Window(gui);
    }


    public void test_close() throws Exception {
        gui.initPlugins("workflow", "security");

        window.getButton("close").click();

        log.assertContent("postGuiEvent(" + CLOSE.ordinal() + ")");
    }


    public void test_initServices() throws Exception {
        gui.initServices(new String[]{ConfigurationOntology.AUDIT_DESTINATION_DIR + " c:/dev/tmp",
                                      ConfigurationOntology.RECORD_MEMORY_USAGE + " enable",
                                      ConfigurationOntology.RECORD_HANDLER_STATISTICS + " disable"});

        assertTrue("c:/dev/tmp".equals(window.getTextBox(ConfigurationOntology.AUDIT_DESTINATION_DIR).getText()));
        JButton button = (JButton)window.getButton(ConfigurationOntology.RECORD_MEMORY_USAGE)
              .getAwtComponent();
        assertTrue("Désactiver".equals(button.getToolTipText()));
        button = (JButton)window.getButton(ConfigurationOntology.RECORD_HANDLER_STATISTICS).getAwtComponent();
        assertTrue("Activer".equals(button.getToolTipText()));
    }


    public void test_initPlugins() throws Exception {
        gui.initPlugins("workflow", "security");

        assertTrue(window.containsLabel("workflow"));
        assertTrue(window.containsLabel("security"));
        assertNotNull(window.getButton("workflow.start"));
        assertNotNull(window.getButton("workflow.stop"));
        assertNotNull(window.getButton("security.start"));
        assertNotNull(window.getButton("security.stop"));
    }


    public void test_lockUnlockGui() throws Exception {
        gui.lockGui();
        gui.lockGui();
        gui.unlockGui();
        gui.unlockGui();

        log.assertContent("startAnimation(), stopAnimation()");
    }


    public void test_initLogs() throws Exception {
        window.getTabGroup().selectTab(AUDIT_LOGS_TAB);

        gui.initLogs("server.log", "mad.log");

        assertTrue(window.getComboBox("logs").contains("server.log"));
        assertTrue(window.getComboBox("logs").contains("mad.log"));
    }


    public void test_showLog() throws Exception {
        window.getTabGroup().selectTab(AUDIT_LOGS_TAB);

        assertTrue(window.getTextBox("logContent").textEquals(""));

        gui.showLog("Content of a file");
        gui.showLog("Content of another file");

        assertTrue(window.getTextBox("logContent").textEquals("Content of another file"));
    }


    public void test_postGuiEvent_startPlugin() throws Exception {
        gui.initPlugins("workflow", "security");

        window.getButton("workflow.start").click();

        log.assertContent("postGuiEvent(" + START_PLUGIN.ordinal() + ", workflow)");
    }


    public void test_postGuiEvent_stopPlugin() throws Exception {
        gui.initPlugins("workflow", "security");

        window.getButton("security.stop").click();

        log.assertContent("postGuiEvent(" + STOP_PLUGIN.ordinal() + ", security)");
    }


    public void test_postGuiEvent_showLog() throws Exception {
        window.getTabGroup().selectTab(AUDIT_LOGS_TAB);

        gui.initLogs("server.log", "mad.log");

        window.getComboBox("logs").select("mad.log");
        window.getButton("readLog").click();

        log.assertContent("postGuiEvent(" + READ_LOG.ordinal() + ", mad.log)");
    }


    public void test_postGuiEvent_changeLogDir() throws Exception {
        //permet de prendre le focus et activer les boutons :)
        window.getTextBox(ConfigurationOntology.AUDIT_DESTINATION_DIR).pressKey(Key.A);
        //

        window.getTextBox(ConfigurationOntology.AUDIT_DESTINATION_DIR).setText("newTempsDir/newLogDir");
        window.getButton("apply").click();

        log.assertContent("postGuiEvent(" + CHANGE_LOG_DIR.ordinal() + ", newTempsDir/newLogDir)");
    }


    private class WaitingPanelMock extends WaitingPanel {
        private LogString log;


        private WaitingPanelMock(LogString log) {
            this.log = log;
        }


        @Override
        public void startAnimation() {
            log.call("startAnimation");
        }


        @Override
        public void stopAnimation() {
            log.call("stopAnimation");
        }
    }
}
