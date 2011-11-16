package net.codjo.administration.gui.plugin;
import net.codjo.administration.gui.plugin.AdministrationGuiAgent.Handler;
import net.codjo.test.common.LogString;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
/**
 *
 */
public class HandlerMock implements Handler {
    private LogString log;
    private List<String> logsReceived;
    private List<String> servicesReceived;


    public HandlerMock(LogString log) {
        this.log = log;
    }


    public void handleAgentBorn() {
        log.call("handleAgentBorn");
    }


    public void handleAgentDead() {
        log.call("handleAgentDead");
    }


    public void handleError(String error) {
        log.call("handleError", error);
    }


    public void handleCommunicationError(String error, Exception e) {
        log.call("handleError", error, e.getMessage());
    }


    public void handleActionStarted() {
        log.call("handleActionStarted");
    }


    public void handlePluginsReceived(List<String> plugins) {
        log.call("handlePluginsReceived", plugins.toArray());
    }


    public void handlePluginStarted(String plugin) {
        log.call("handlePluginStarted", plugin);
    }


    public void handlePluginStopped(String plugin) {
        log.call("handlePluginStopped", plugin);
    }


    public void handleLogRead(String content) {
        log.call("handleLogRead", content);
    }


    public void handleLogsReceived(List<String> logs) {
        logsReceived = logs;
        log.call("handleLogsReceived", logsReceived.toArray());
    }


    public void assertLogsReceived(String... expected) {
        assertEquals(Arrays.asList(expected), logsReceived);
    }


    public void handleEnableService(String service) {
        log.call("handleEnableService", service);
    }


    public void handleDisableService(String service) {
        log.call("handleDisableService", service);
    }


    public void handleServicesReceived(List<String> services) {
        servicesReceived = services;
        log.call("handleServicesReceived", this.servicesReceived.toArray());
    }


    public void assertServicesReceived(String... expected) {
        assertEquals(Arrays.asList(expected), servicesReceived);
    }


    public void handleLogDirChanged(String newLogDir) {
        log.call("handleLogDirChanged", newLogDir);
    }


    public void handleSystemProperties(String value) {
        log.call("handleSystemProperties", value);
    }


    public void handleSystemEnvironment(String value) {
        log.call("handleSystemEnvironment", value);
    }
}
