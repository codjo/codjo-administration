package net.codjo.administration.server.operation.plugin;
import java.util.List;

public interface PluginManager {
    List<String> getPlugins();


    void startPlugin(String plugin) throws Exception;


    void stopPlugin(String plugin) throws Exception;
}
