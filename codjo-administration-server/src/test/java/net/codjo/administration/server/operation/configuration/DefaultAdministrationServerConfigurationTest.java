package net.codjo.administration.server.operation.configuration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import net.codjo.administration.server.operation.configuration.DefaultAdministrationServerConfiguration;

public class DefaultAdministrationServerConfigurationTest {
    private DefaultAdministrationServerConfiguration configuration
          = new DefaultAdministrationServerConfiguration();


    @Test
    public void test_default() throws Exception {
        assertFalse(configuration.isRecordMemoryUsageSet());
        assertFalse(configuration.isRecordMemoryUsage());

        assertFalse(configuration.isRecordHandlerStatisticsSet());
        assertFalse(configuration.isRecordHandlerStatistics());

        assertFalse(configuration.isAuditDestinationDirSet());
        assertNull(configuration.getAuditDestinationDir());
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
        assertEquals("c:\\dev\temp\\test",configuration.getAuditDestinationDir());
    }

}
