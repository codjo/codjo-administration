package net.codjo.administration.server.audit.mad;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.codjo.administration.server.audit.AbstractExecutionSpy;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.mad.server.MadConnectionManager;
import net.codjo.mad.server.MadTransaction;
import net.codjo.mad.server.handler.Handler;
import net.codjo.mad.server.handler.HandlerContext;
import net.codjo.mad.server.handler.HandlerListener;
import net.codjo.security.common.api.User;
import net.codjo.sql.spy.ConnectionSpy;
import net.codjo.util.time.TimeSource;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.PersistenceException;

public class HandlerExecutionSpy extends AbstractExecutionSpy implements HandlerListener {
    private final Map<String, Spy> spies = new HashMap<String, Spy>();


    public HandlerExecutionSpy(AdministrationLogFile administrationLogFile) {
        this(administrationLogFile, null);
    }


    public HandlerExecutionSpy(AdministrationLogFile administrationLogFile, TimeSource timeSource) {
        super(administrationLogFile, timeSource);
    }


    public void handlerStarted(Handler handler, HandlerContext handlerContext) {
        Spy spy = new Spy(handler, handlerContext);
        spies.put(buildKey(handler, handlerContext), spy);
        spy.start();
    }


    public void handlerStopped(Handler handler, HandlerContext handlerContext) {
        spies.remove(buildKey(handler, handlerContext)).stop();
    }


    private String buildKey(Handler handler, HandlerContext handlerContext) {
        return handler.getId() + "\t" + handlerContext.getUserProfil().getId().encode();
    }


    private class Spy extends AbstractSpy {
        private HandlerContextSpy handlerContextSpy;
        private Handler handler;
        private HandlerContext handlerContext;


        private Spy(Handler handler, HandlerContext handlerContext) {
            super("HANDLER");
            this.handler = handler;
            this.handlerContext = handlerContext;
        }


        @Override
        public void start() {
            handlerContextSpy = new HandlerContextSpy(handlerContext);
            handler.setContext(handlerContextSpy);

            logConnections();
            super.start();
        }


        @Override
        public void stop() {
            super.stop();
            handlerContextSpy.logBd();

            handler.setContext(handlerContext);
        }


        private void logConnections() {
            MadConnectionManager connectionManager = handlerContext.getConnectionManager();
            log("CONNECTIONS",
                System.currentTimeMillis(),
                connectionManager.countUnusedConnections() + " unused",
                connectionManager.countUsedConnections() + " used");
        }


        private void log(String tag1, long when, Object... cols) {
            log(tag1, "", when, cols);
        }


        @Override
        protected void log(String tag1, String tag2, long when, Object... cols) {
            List<Object> values = new ArrayList<Object>();
            values.add(handler.getId());
            values.add(handlerContext.getUserProfil().getId().encode());
            values.addAll(Arrays.asList(cols));
            super.log(tag1, tag2, when, values.toArray());
        }


        private class HandlerContextSpy extends HandlerContext {
            private HandlerContext handlercontext;
            private ConnectionSpy connection;
            private ConnectionSpy txConnection;


            private HandlerContextSpy(HandlerContext handlercontext) {
                this.handlercontext = handlercontext;
            }


            public HandlerContext getHandlercontext() {
                return handlercontext;
            }


            @Override
            public void setUser(String user) {
                handlercontext.setUser(user);
            }


            @Override
            public String getUser() {
                return handlercontext.getUser();
            }


            @Override
            public boolean isAllowedTo(String function) {
                return handlercontext.isAllowedTo(function);
            }


            @Override
            public Connection getConnection() throws SQLException {
                if (connection == null) {
                    connection = new ConnectionSpy(handlercontext.getConnection(), null);
                }
                return connection;
            }


            @Override
            public Connection getTxConnection() throws SQLException {
                if (txConnection == null) {
                    txConnection = new ConnectionSpy(handlercontext.getTxConnection(), null);
                }
                return txConnection;
            }


            @Override
            public Database getDatabase() throws PersistenceException {
                return handlercontext.getDatabase();
            }


            @Override
            public MadTransaction getTransaction() {
                return handlercontext.getTransaction();
            }


            @Override
            public MadConnectionManager getConnectionManager() {
                return handlercontext.getConnectionManager();
            }


            @Override
            public User getUserProfil() {
                return handlercontext.getUserProfil();
            }


            @Override
            public void close() {
                handlercontext.close();
            }


            public void logBd() {
                if (connection != null) {
                    Spy.this.logBd(connection);
                }
                if (txConnection != null) {
                    Spy.this.logBd(txConnection);
                }
            }
        }
    }
}
