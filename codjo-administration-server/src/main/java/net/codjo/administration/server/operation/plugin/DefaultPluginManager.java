package net.codjo.administration.server.operation.plugin;
import net.codjo.plugin.common.ApplicationPlugin;
import net.codjo.plugin.server.ServerCore;
import java.util.ArrayList;
import java.util.List;

public class DefaultPluginManager implements PluginManager {
    private ServerCore serverCore;


    public DefaultPluginManager(ServerCore serverCore) {
        this.serverCore = serverCore;
    }


    public List<String> getPlugins() {
        List<String> plugins = new ArrayList<String>();
        for (ApplicationPlugin applicationPlugin : serverCore.getPlugins()) {
            plugins.add(applicationPlugin.getClass().getName());
        }
        return plugins;
    }


    public void startPlugin(String plugin) throws Exception {
        ApplicationPlugin applicationPlugin = getPlugin(plugin);
        if (applicationPlugin != null) {
            applicationPlugin.start(serverCore.getAgentContainer());
        }
    }


    public void stopPlugin(String plugin) throws Exception {
        ApplicationPlugin applicationPlugin = getPlugin(plugin);
        if (applicationPlugin != null) {
            applicationPlugin.stop();
        }
    }


    private ApplicationPlugin getPlugin(String plugin) {
        for (ApplicationPlugin applicationPlugin : serverCore.getPlugins()) {
            if (applicationPlugin.getClass().getName().equals(plugin)) {
                return applicationPlugin;
            }
        }
        return null;
    }
}
