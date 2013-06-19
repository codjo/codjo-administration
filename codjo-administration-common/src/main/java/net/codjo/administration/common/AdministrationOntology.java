package net.codjo.administration.common;

public interface AdministrationOntology {
    public static String GET_LOG_FILES_ACTION = "logFiles";
    public static String READ_LOG_ACTION = "read";

    public static String GET_PLUGINS_ACTION = "plugins";
    public static String START_PLUGIN_ACTION = "start";
    public static String STOP_PLUGIN_ACTION = "stop";
    public static String GET_SERVICES_ACTION = "services";
    public static String ENABLE_SERVICE_ACTION = "enable";
    public static String DISABLE_SERVICE_ACTION = "disable";
    public static String CHANGE_LOG_DIR = "setLogDir";
    public static String RESTORE_LOG_DIR = "restoreLogDir";
    public static String CHANGE_JDBC_USERS_FILTER = "setJdbcUsersFilter";
    public static String RESTORE_JDBC_USERS_FILTER = "restoreJdbcUsersFilter";

    public static String DISPLAY_SYSTEM_PROPERTIES = "displaySystemProperties";
    public static String DISPLAY_SYSTEM_ENVIRONMENT = "displaySystemEnvironment";
}
