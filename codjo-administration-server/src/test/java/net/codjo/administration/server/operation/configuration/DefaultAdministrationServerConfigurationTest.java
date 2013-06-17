package net.codjo.administration.server.operation.configuration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultAdministrationServerConfigurationTest {
    private static final String EXPECTED_USERS_FILTER = "user1,user2";
    private DefaultAdministrationServerConfiguration configuration
          = new DefaultAdministrationServerConfiguration();


    @Test
    public void test_default() throws Exception {
        assertFalse(configuration.isRecordMemoryUsageSet());
        assertFalse(configuration.isRecordMemoryUsage());

        assertFalse(configuration.isRecordHandlerStatisticsSet());
        assertFalse(configuration.isRecordHandlerStatistics());

        assertFalse(configuration.isRecordJdbcStatisticsSet());
        assertFalse(configuration.isRecordJdbcStatistics());

        assertFalse(configuration.isAuditDestinationDirSet());
        assertNull(configuration.getAuditDestinationDir());

        assertFalse(configuration.isJdbcUsersFilterSet());
        assertNull(configuration.getJdbcUsersFilter());
    }


    @Test
    public void test_defaultRecordMemoryUsage() throws Exception {
        configuration.setDefaultRecordMemoryUsage(true);

        assertTrue(configuration.isRecordMemoryUsageSet());
        assertTrue(configuration.isRecordMemoryUsage());
    }


    @Test
    public void test_setRecordMemoryUsage() throws Exception {
        configuration.setRecordMemoryUsage(true);

        assertTrue(configuration.isRecordMemoryUsageSet());
        assertTrue(configuration.isRecordMemoryUsage());
    }


    @Test
    public void test_restoreRecordMemoryUsage() throws Exception {
        configuration.setDefaultRecordMemoryUsage(true);
        configuration.setRecordMemoryUsage(false);
        configuration.restoreDefaultRecordMemoryUsage();

        assertTrue(configuration.isRecordMemoryUsageSet());
        assertTrue(configuration.isRecordMemoryUsage());
    }


    @Test
    public void test_setDefaultRecordHandlerStatistics() throws Exception {
        configuration.setDefaultRecordHandlerStatistics(false);

        assertTrue(configuration.isRecordHandlerStatisticsSet());
        assertFalse(configuration.isRecordHandlerStatistics());
    }


    @Test
    public void test_setRecordHandlerStatistics() throws Exception {
        configuration.setRecordHandlerStatistics(false);

        assertTrue(configuration.isRecordHandlerStatisticsSet());
        assertFalse(configuration.isRecordHandlerStatistics());
    }


    @Test
    public void test_restoreRecordHandlerStatistics() throws Exception {
        configuration.setDefaultRecordHandlerStatistics(true);
        configuration.setRecordHandlerStatistics(false);
        configuration.restoreDefaultRecordHandlerStatistics();

        assertTrue(configuration.isRecordHandlerStatisticsSet());
        assertTrue(configuration.isRecordHandlerStatistics());
    }


    @Test
    public void test_setDefaultRecordJdbcStatistics() throws Exception {
        configuration.setDefaultRecordJdbcStatistics(false);

        assertTrue(configuration.isRecordJdbcStatisticsSet());
        assertFalse(configuration.isRecordJdbcStatistics());
    }


    @Test
    public void test_setRecordJdbcStatistics() throws Exception {
        configuration.setRecordJdbcStatistics(false);

        assertTrue(configuration.isRecordJdbcStatisticsSet());
        assertFalse(configuration.isRecordJdbcStatistics());
    }


    @Test
    public void test_restoreRecordJdbcStatistics() throws Exception {
        configuration.setDefaultRecordJdbcStatistics(true);
        configuration.setRecordJdbcStatistics(false);
        configuration.restoreDefaultRecordJdbcStatistics();

        assertTrue(configuration.isRecordJdbcStatisticsSet());
        assertTrue(configuration.isRecordJdbcStatistics());
    }


    @Test
    public void test_setDefaultRecordDestinationFile() throws Exception {
        configuration.setDefaultAuditDestinationDir("c:\\dev\temp\\test");

        assertTrue(configuration.isAuditDestinationDirSet());
        assertEquals("c:\\dev\temp\\test", configuration.getAuditDestinationDir());
    }


    @Test
    public void test_setRecordDestinationFile() throws Exception {
        configuration.setAuditDestinationDir("c:\\dev\temp\\test");

        assertTrue(configuration.isAuditDestinationDirSet());
        assertEquals("c:\\dev\temp\\test", configuration.getAuditDestinationDir());
    }


    @Test
    public void test_restoreRecordDestinationFile() throws Exception {
        configuration.setDefaultAuditDestinationDir("c:\\dev\temp\\test");
        configuration.setAuditDestinationDir("newDir");
        configuration.restoreDefaultAuditDestinationDir();

        assertTrue(configuration.isAuditDestinationDirSet());
        assertEquals("c:\\dev\temp\\test", configuration.getAuditDestinationDir());
    }


    @Test
    public void test_setDefaultJdbcUsersFilter() throws Exception {
        configuration.setDefaultJdbcUsersFilter(EXPECTED_USERS_FILTER);

        assertTrue(configuration.isJdbcUsersFilterSet());
        assertEquals(EXPECTED_USERS_FILTER, configuration.getJdbcUsersFilter());
    }


    @Test
    public void test_setJdbcUsersFilter() throws Exception {
        configuration.setJdbcUsersFilter(EXPECTED_USERS_FILTER);

        assertTrue(configuration.isJdbcUsersFilterSet());
        assertEquals(EXPECTED_USERS_FILTER, configuration.getJdbcUsersFilter());
    }


    @Test
    public void test_restoreJdbcUsersFilter() throws Exception {
        configuration.setDefaultJdbcUsersFilter(EXPECTED_USERS_FILTER);
        configuration.setJdbcUsersFilter("user3");
        configuration.restoreDefaultJdbcUsersFilter();

        assertTrue(configuration.isJdbcUsersFilterSet());
        assertEquals(EXPECTED_USERS_FILTER, configuration.getJdbcUsersFilter());
    }
}
