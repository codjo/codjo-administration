package net.codjo.administration.common;
import net.codjo.test.common.XmlUtil;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class XmlCodecTest {

    @Test
    public void test_pluginsFromXml() throws Exception {
        assertEquals(Arrays.asList("workflow", "import"),
                     XmlCodec.listFromXml("<list>"
                                          + "  <string>workflow</string>"
                                          + "  <string>import</string>"
                                          + "</list>"));
    }


    @Test
    public void test_pluginsToXml() throws Exception {
        XmlUtil.assertEquals("<list>"
                             + "  <string>workflow</string>"
                             + "  <string>import</string>"
                             + "</list>",
                             XmlCodec.listToXml(Arrays.asList("workflow", "import")));
    }


    @Test
    public void test_logFromXml() throws Exception {
        assertEquals("Contenu d'un fichier de log",
                     XmlCodec.logFromXml("<string>Contenu d'un fichier de log</string>"));
    }


    @Test
    public void test_logToXml() throws Exception {
        assertEquals("<string>Content of a file &lt;&gt;</string>",
                     XmlCodec.logToXml("Content of a file <>"));
    }


    @Test
    public void test_logsFromXml() throws Exception {
        assertEquals(Arrays.asList("server.log", "mad.log"),
                     XmlCodec.listFromXml("<list>"
                                          + "  <string>server.log</string>"
                                          + "  <string>mad.log</string>"
                                          + "</list>"));
    }
}
