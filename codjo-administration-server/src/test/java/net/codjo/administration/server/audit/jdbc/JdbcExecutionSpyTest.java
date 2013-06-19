package net.codjo.administration.server.audit.jdbc;
import java.sql.Connection;
import java.sql.SQLException;
import net.codjo.administration.server.AbstractJdbcExecutionSpyTest;
import net.codjo.sql.spy.ConnectionSpy;
import net.codjo.util.time.TimeSource;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
/**
 *
 */
public class JdbcExecutionSpyTest extends AbstractJdbcExecutionSpyTest {
    private static final String USER = "user";


    @Test
    public void testCreateConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = createConnection(USER);
            assertNotNull(connection);
            assertTrue("createConnection must return a ConnectionSpy",
                       ConnectionSpy.class.isAssignableFrom(connection.getClass()));
        }
        finally {
            connection.close();
        }
    }


    @Test
    public void testStartStop() throws SQLException {
        Connection connection = null;
        try {
            connection = createConnection(USER);
        }
        finally {
            connection.close();
        }

        log.assertContent("write(JDBC, Temps Total)");
    }


    @Override
    protected TimeSource createTimeSource() {
        return null;
    }
}
