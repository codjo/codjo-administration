package net.codjo.administration.gui.plugin;
import net.codjo.administration.common.ConfigurationOntology;
import net.codjo.administration.gui.plugin.AdministrationGuiAgent.Handler;
import net.codjo.agent.GuiAgent;
import net.codjo.gui.toolkit.util.ErrorDialog;
import net.codjo.mad.gui.framework.GuiContext;
import java.beans.PropertyVetoException;
import java.util.List;
import javax.swing.SwingUtilities;

class DefaultHandler implements Handler {
    private GuiContext guiContext;
    private AdministrationGui gui;


    DefaultHandler(GuiContext guiContext, GuiAgent guiAgent) {
        this(guiContext, new AdministrationGui(guiAgent));
    }


    DefaultHandler(GuiContext guiContext, AdministrationGui gui) {
        this.guiContext = guiContext;
        this.gui = gui;
    }


    public void handleAgentBorn() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                guiContext.getDesktopPane().add(gui);
                gui.setSize(800, 600);
                gui.setVisible(true);
            }
        });
    }


    public void handleAgentDead() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    gui.setClosed(true);
                }
                catch (PropertyVetoException e) {
                    ;
                }
                finally {
                    gui.dispose();
                }
            }
        });
    }


    public void handleError(final String error) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (gui.isVisible()) {
                    ErrorDialog.show(gui, "", error);
                }
                else {
                    ErrorDialog.show(guiContext.getMainFrame(), "", error);
                }
                gui.unlockGui();
            }
        });
    }


    public void handleCommunicationError(final String error, final Exception e) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (gui.isVisible()) {
                    ErrorDialog.show(gui, "", error, e);
                }
                else {
                    ErrorDialog.show(guiContext.getMainFrame(), "", error, e);
                }
                gui.unlockGui();
            }
        });
    }


    public void handleActionStarted() {
        gui.lockGui();
    }


    public void handlePluginsReceived(final List<String> plugins) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.initPlugins(plugins.toArray(new String[plugins.size()]));
                gui.unlockGui();
            }
        });
    }


    public void handlePluginStarted(String plugin) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.unlockGui();
            }
        });
    }


    public void handlePluginStopped(String plugin) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.unlockGui();
            }
        });
    }


    public void handleLogsReceived(final List<String> logs) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.initLogs(logs.toArray(new String[logs.size()]));
                gui.unlockGui();
            }
        });
    }


    public void handleLogRead(final String content) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.showLog(content);
                gui.unlockGui();
            }
        });
    }


    public void handleEnableService(final String service) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.enableService(service);
                gui.unlockGui();
            }
        });
    }


    public void handleDisableService(final String service) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.disableService(service);
                gui.unlockGui();
            }
        });
    }


    public void handleServicesReceived(final List<String> services) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.initServices(services.toArray(new String[services.size()]));
                gui.unlockGui();
            }
        });
    }


    public void handleLogDirChanged(final String newLogDir) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.initServices(new String[]{ConfigurationOntology.AUDIT_DESTINATION_DIR + " " + newLogDir});
                gui.unlockGui();
            }
        });
    }


    public void handleSystemProperties(final String value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.setSystemProperties(value);
                gui.unlockGui();
            }
        });
    }


    public void handleSystemEnvironment(final String value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                gui.setSystemEnvironment(value);
                gui.unlockGui();
            }
        });
    }
}
