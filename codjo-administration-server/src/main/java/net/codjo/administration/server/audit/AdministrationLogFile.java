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

    /**
     * This value appear to be a reasonable initial line size to avoid copying the buffer in a call to write. It's
     * slightly bigger than actual maximal size found in a real application.
     */
    private static final int INITIAL_LINE_SIZE = 256;


    public void init(String destinationFile) throws IOException {
        LOGGER.setAdditivity(false);
        LOGGER.removeAppender(APPENDER_NAME);
        RollingFileAppender appender = new RollingFileAppender(new PatternLayout("%m%n"), destinationFile);
        appender.setName(APPENDER_NAME);
        appender.setMaxFileSize("500KB");
        appender.setMaxBackupIndex(10);
        LOGGER.addAppender(appender);
    }


    final public void write(String tag1, String tag2, Object... values) {
        write(tag1, tag2, System.currentTimeMillis(), values);
    }


    final public void write(String tag1, String tag2, long when, Object... values) {
        write(tag1, tag2, (Long)when, values);
    }


    final public void writeWithoutTime(String tag1, String tag2, Object... values) {
        write(tag1, tag2, null, values);
    }


    final public static String formatDate(Long when) {
        String result = "";
        if (when != null) {
            result = DATE_FORMAT.format(when);
        }
        return result;
    }


    protected void write(String tag1, String tag2, Long when, Object... values) {
        StringBuilder log = new StringBuilder(INITIAL_LINE_SIZE)
              .append(tag1)
              .append("\t").append(tag2);
        if (when != null) {
            log.append("\t").append(formatDate(when));
        }
        for (Object value : values) {
            log.append("\t").append(value);
        }
        LOGGER.info(log.toString());
    }
}
