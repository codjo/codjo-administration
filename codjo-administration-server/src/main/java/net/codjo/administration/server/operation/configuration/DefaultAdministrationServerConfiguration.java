package net.codjo.administration.server.operation.configuration;

public class DefaultAdministrationServerConfiguration implements AdministrationServerConfiguration {
    private final Parameter<Boolean> defaultRecordMemoryUsage = new Parameter<Boolean>(false);
    private final Parameter<Boolean> recordMemoryUsage = new Parameter<Boolean>(false);
    private final Parameter<Boolean> defaultRecordHandlerStatistics = new Parameter<Boolean>(false);
    private final Parameter<Boolean> recordHandlerStatistics = new Parameter<Boolean>(false);
    private final Parameter<String> defaultAuditDestinationDir = new Parameter<String>(null);
    private final Parameter<String> auditDestinationDir = new Parameter<String>(null);


    public boolean isRecordMemoryUsageSet() {
        return recordMemoryUsage.isSet();
    }


    public boolean isRecordMemoryUsage() {
        return recordMemoryUsage.getValue();
    }


    public void setDefaultRecordMemoryUsage(boolean recordMemoryUsage) {
        this.defaultRecordMemoryUsage.setValue(recordMemoryUsage);
        setRecordMemoryUsage(recordMemoryUsage);
    }


    public void setRecordMemoryUsage(boolean recordMemoryUsage) {
        this.recordMemoryUsage.setValue(recordMemoryUsage);
    }

    public void restoreDefaultRecordMemoryUsage(){
        setRecordMemoryUsage(defaultRecordMemoryUsage.getValue());
    }

    public boolean isRecordHandlerStatisticsSet() {
        return recordHandlerStatistics.isSet();
    }


    public boolean isRecordHandlerStatistics() {
        return recordHandlerStatistics.getValue();
    }


    public void setDefaultRecordHandlerStatistics(boolean recordHandlerStatistics) {
        this.defaultRecordHandlerStatistics.setValue(recordHandlerStatistics);
        setRecordHandlerStatistics(recordHandlerStatistics);
    }


    public void setRecordHandlerStatistics(boolean recordHandlerStatistics) {
        this.recordHandlerStatistics.setValue(recordHandlerStatistics);
    }


    public void restoreDefaultRecordHandlerStatistics() {
        setRecordHandlerStatistics(defaultRecordHandlerStatistics.getValue());
    }


    public boolean isAuditDestinationDirSet() {
        return auditDestinationDir.isSet();
    }


    public String getAuditDestinationDir() {
        return auditDestinationDir.getValue();
    }




    public void setDefaultAuditDestinationDir(String logDirValue) {
        this.defaultAuditDestinationDir.setValue(logDirValue);
        setAuditDestinationDir(logDirValue);
    }


    public void setAuditDestinationDir(String destinationFile) {
        this.auditDestinationDir.setValue(destinationFile);
    }


    public void restoreDefaultAuditDestinationDir() {
        setAuditDestinationDir(defaultAuditDestinationDir.getValue());
    }


    class Parameter<T> {
        T value;
        boolean set;


        Parameter(T value) {
            this.value = value;
        }


        public T getValue() {
            return value;
        }


        public void setValue(T value) {
            this.value = value;
            set = true;
        }


        public boolean isSet() {
            return set;
        }
    }
}
