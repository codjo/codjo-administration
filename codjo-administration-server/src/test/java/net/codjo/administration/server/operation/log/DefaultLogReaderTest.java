package net.codjo.administration.server.operation.log;
import net.codjo.test.common.fixture.DirectoryFixture;
import static net.codjo.test.common.matcher.JUnitMatchers.*;
import net.codjo.util.file.FileUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultLogReaderTest {
    private static final DirectoryFixture directoryFixture = DirectoryFixture.newTemporaryDirectoryFixture();
    private static final String SERVER_LOG_FILE_CONTENT = "Content of server log file";
    private DefaultLogReader logManager;


    @BeforeClass
    public static void globalSetUp() throws Exception {
        directoryFixture.doSetUp();

        new File(directoryFixture, "directory").mkdir();
        FileUtil.saveContent(new File(directoryFixture, "server.log"), SERVER_LOG_FILE_CONTENT);
        FileUtil.saveContent(new File(directoryFixture, "mad.log"), "Some logs about mad and handlers");
        FileUtil.saveContent(new File(directoryFixture, "aaa.log"), "Some logs about mad and handlers");
        FileUtil.saveContent(new File(directoryFixture, "zzz.log"), "Some logs about mad and handlers");
    }


    @AfterClass
    public static void globalTearDown() throws Exception {
        directoryFixture.doTearDown();
    }


    @Before
    public void setUp() {
        logManager = new DefaultLogReader(directoryFixture.getAbsolutePath());
    }


    @Test
    public void test_getLogFiles() throws Exception {
        List<String> logs = logManager.getLogFiles();

        assertEquals("[aaa.log, mad.log, server.log, zzz.log]", logs.toString());
    }


    @Test
    public void test_getLogFiles_undefinedLogDir() throws Exception {
        logManager = new DefaultLogReader("undefinedLogDir");
        assertEquals(0, logManager.getLogFiles().size());
    }


    @Test
    public void test_getLogFiles_nullLogDir() throws Exception {
        logManager = new DefaultLogReader(null);
        assertEquals(0, logManager.getLogFiles().size());
    }


    @Test
    public void test_readLog() throws Exception {
        assertThat(logManager.readLog("server.log"),
                   equalTo(SERVER_LOG_FILE_CONTENT));
    }


    @Test(expected = FileNotFoundException.class)
    public void test_readLog_fileNotFound() throws Exception {
        logManager.readLog("unknown-file.log");
    }
}
