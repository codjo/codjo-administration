package net.codjo.administration.server.audit;
import net.codjo.sql.spy.ConnectionSpy;
import net.codjo.sql.spy.ConnectionSpy.OneQuery;
import net.codjo.util.time.Chronometer;
import net.codjo.util.time.TimeSource;
/**
 *
 */
abstract public class AbstractExecutionSpy {
    protected final AdministrationLogFile administrationLogFile;
    protected final TimeSource timeSource;


    protected AbstractExecutionSpy(AdministrationLogFile administrationLogFile, TimeSource timeSource) {
        this.administrationLogFile = administrationLogFile;
        this.timeSource = timeSource;
    }


    private final void log(String tag1, String tag2, long when, Object... values) {
        administrationLogFile.write(tag1, tag2, when, values);
    }


    protected class AbstractSpy {
        private final Chronometer chronometer;
        private final String type;


        public AbstractSpy(String type) {
            this.type = type;
            this.chronometer = new Chronometer(timeSource);
        }


        public void start() {
            chronometer.start();
        }


        public void stop() {
            chronometer.stop();
            logTotalTime();
        }


        private void logTotalTime() {
            log(type,
                "Temps Total",
                chronometer.getStartTime(),
                chronometer.getDelay() + " ms");
        }


        protected final void logBd(ConnectionSpy connectionSpy) {
            for (OneQuery query : connectionSpy.getAllQueries()) {
                log(type, "Temps BD", query.getWhen(), query.getSql(), query.getCount(), query.getTotalTime() + " ms");
            }
        }


        protected void log(String tag1, String tag2, long when, Object... cols) {
            AbstractExecutionSpy.this.log(tag1, tag2, when, cols);
        }
    }
}
