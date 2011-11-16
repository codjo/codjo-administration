package net.codjo.administration.server.audit;
import net.codjo.test.common.PathUtil;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AdministrationLogFileTest {

    @Test
    public void test_setDestinationFile() throws Exception {
        Logger logger = Logger.getLogger(AdministrationLogFile.class);
        logger.removeAllAppenders();

        AdministrationLogFile administrationLogFile = new AdministrationLogFile();

        assertFalse(logger.getAllAppenders().hasMoreElements());

        administrationLogFile.init(PathUtil.findTargetDirectory(getClass()) + "/test");

        assertTrue(logger.getAllAppenders().hasMoreElements());
    }
}
