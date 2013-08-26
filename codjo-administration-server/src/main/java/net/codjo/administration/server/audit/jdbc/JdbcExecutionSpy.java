package net.codjo.administration.server.audit.jdbc;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.codjo.administration.server.audit.AbstractExecutionSpy;
import net.codjo.administration.server.audit.AdministrationLogFile;
import net.codjo.sql.server.ConnectionFactory;
import net.codjo.sql.server.ConnectionPool;
import net.codjo.sql.server.ConnectionPoolConfiguration;
import net.codjo.sql.server.ConnectionPoolListener;
import net.codjo.sql.server.DefaultConnectionFactory;
import net.codjo.sql.spy.ConnectionSpy;
import net.codjo.sql.spy.ConnectionSpy.OneQuery;
import net.codjo.util.time.TimeSource;

import static java.util.Collections.sort;
/**
 *
 */
public class JdbcExecutionSpy extends AbstractExecutionSpy implements ConnectionFactory, ConnectionPoolListener {
    private static final String TYPE = "JDBC";

    private final ConnectionFactorySpy factory = new ConnectionFactorySpy();

    private final Map<Integer, Spy> spies = new HashMap<Integer, Spy>();


    public JdbcExecutionSpy(AdministrationLogFile administrationLogFile) {
        this(administrationLogFile, null);
    }


    public JdbcExecutionSpy(AdministrationLogFile administrationLogFile,
                            TimeSource timeSource) {
        super(administrationLogFile, timeSource);
    }


    public Connection createConnection(ConnectionPoolConfiguration configuration, String applicationUser)
          throws SQLException {
        return factory.createConnection(configuration, applicationUser);
    }


    public void poolCreated(ConnectionPool pool) {
        // do nothing
    }


    public void poolDestroyed(ConnectionPool pool) {
        String applicationUser = pool.getApplicationUser();

        List<Spy> userSpies = new ArrayList<Spy>();
        synchronized (spies) {
            for (Spy spy : spies.values()) {
                if (spy.applicationUser.equals(applicationUser)) {
                    userSpies.add(spy);
                }
            }
        }

        Map<String, OneQuery> aggregatedQueries = new HashMap<String, OneQuery>();
        for (Spy spy : userSpies) {
            for (OneQuery cpQuery : spy.connectionSpy.getAllQueries()) {
                OneQuery query = aggregatedQueries.get(cpQuery.getSql());
                if (query == null) {
                    query = new OneQuery(cpQuery.getSql(), timeSource);
                }

                query = query.aggregate(cpQuery);
                aggregatedQueries.put(query.getSql(), query);
            }
        }
        administrationLogFile.writeWithoutTime(TYPE,
                                               "Aggregated statistics for user '" + applicationUser + "', "
                                               + aggregatedQueries.size() + " unique queries");
        administrationLogFile.writeWithoutTime(TYPE, "Aggregated statistics", "Order (highest Total time first)",
                                               "User", "Min time", "Max time", "Total time", "Count", "Query");

        Comparator<OneQuery> comparator = new Comparator<OneQuery>() {
            public int compare(OneQuery o1, OneQuery o2) {
                int result = (int)(o1.getTotalTime() - o2.getTotalTime());
                return -result; // reverse order (=max time first)
            }
        };
        List<OneQuery> sortedQueries = new ArrayList<OneQuery>(aggregatedQueries.values());
        sort(sortedQueries, comparator);

        int order = 0;
        for (OneQuery query : sortedQueries) {
            administrationLogFile.writeWithoutTime(TYPE,
                                                   "Aggregated statistics",
                                                   order,
                                                   applicationUser,
                                                   query.getMinTime(),
                                                   query.getMaxTime(),
                                                   query.getTotalTime(),
                                                   query.getCount(),
                                                   query.getSql());
            order++;
        }
    }


    public class ConnectionFactorySpy extends DefaultConnectionFactory {
        private final AtomicInteger spyId = new AtomicInteger();


        @Override
        public Connection createConnection(ConnectionPoolConfiguration configuration, String applicationUser)
              throws SQLException {
            Connection connection = super.createConnection(configuration, applicationUser);

            final int id = spyId.getAndIncrement();
            ConnectionSpy result = new ConnectionSpy(connection, timeSource) {
                @Override
                public void close() throws SQLException {
                    super.close();

                    Spy spy = spies.get(id);
                    spy.stop();
                }
            };
            Spy spy = new Spy(result, id, applicationUser);

            synchronized (spies) {
                spies.put(id, spy);
            }

            spy.start();

            return result;
        }
    }

    private class Spy extends AbstractSpy {
        private final ConnectionSpy connectionSpy;
        private final int id;
        private final String applicationUser;


        private Spy(ConnectionSpy connectionSpy, int id, String applicationUser) {
            super(TYPE);
            this.connectionSpy = connectionSpy;
            this.id = id;
            this.applicationUser = applicationUser;
        }


        @Override
        public void stop() {
            super.stop();
            logBd(connectionSpy);
        }


        @Override
        protected void log(String tag1, String tag2, long when, Object... cols) {
            List<Object> values = new ArrayList<Object>();
            values.add(id);
            values.add(applicationUser);
            values.addAll(Arrays.asList(cols));
            super.log(tag1, tag2, when, values.toArray());
        }
    }
}
