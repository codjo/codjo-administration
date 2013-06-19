package net.codjo.administration.server.operation.configuration;

public interface AdministrationServerConfiguration {

    void setRecordMemoryUsage(boolean record);


    void setRecordJdbcStatistics(boolean record);


    void setJdbcUsersFilter(String userList);


    void setRecordHandlerStatistics(boolean record);


    void setAuditDestinationDir(String destinationDir);
}
