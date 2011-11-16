package net.codjo.administration.server.operation.systemProperties;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class DefaultSystemPropertiesTest {
    private DefaultSystemProperties systemProperties;

    @Before
    public void setUp() {
        systemProperties = new DefaultSystemProperties();
    }


    @Test
    public void test_getSystemProperties() throws Exception {
        assertTrue(0 < systemProperties.getSystemProperties().length());
    }


    @Test
    public void test_getSystemEnvironment() throws Exception {
        assertTrue(0 < systemProperties.getSystemEnvironment().length());
    }
}
