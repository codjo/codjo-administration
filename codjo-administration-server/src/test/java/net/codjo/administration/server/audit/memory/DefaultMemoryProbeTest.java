package net.codjo.administration.server.audit.memory;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DefaultMemoryProbeTest {

    @Test
    public void test_getUsedMemory() throws Exception {
        DefaultMemoryProbe resourcesManager = new DefaultMemoryProbe();

        assertTrue(resourcesManager.getUsedMemory() > 0);
    }


    @Test
    public void test_getTotalMemory() throws Exception {
        DefaultMemoryProbe resourcesManager = new DefaultMemoryProbe();

        assertTrue(resourcesManager.getTotalMemory() > 0);
    }
}
