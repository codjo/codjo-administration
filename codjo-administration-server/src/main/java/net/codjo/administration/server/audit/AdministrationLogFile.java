package net.codjo.administration.server.audit;
import java.io.IOException;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class AdministrationLogFile {
    private static final Logger LOGGER = Logger.getLogger(AdministrationLogFile.class);
    private static final String APPENDER_NAME = "rollingFileAppender";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");


    public void init(String destinationFile) throws IOException {
        LOGGER.setAdditivity(false);
        LOGGER.removeAppender(APPENDER_NAME);
        RollingFileAppender appender = new RollingFileAppender(new PatternLayout("%m%n"), destinationFile);
        appender.setName(APPENDER_NAME);
        appender.setMaxFileSize("500KB");
        appender.setMaxBackupIndex(10);
        LOGGER.addAppender(appender);
    }


    public void write(String tag1, String tag2, Object... values) {
        write(tag1, tag2, System.currentTimeMillis(), values);
    }


    public void write(String tag1, String tag2, long when, Object... values) {
        StringBuilder log = new StringBuilder()
              .append(tag1)
              .append("\t").append(tag2)
              .append("\t").append(DATE_FORMAT.format(when));
        for (Object value : values) {
            log.append("\t").append(value);
        }
        LOGGER.info(log.toString());
    }
}
