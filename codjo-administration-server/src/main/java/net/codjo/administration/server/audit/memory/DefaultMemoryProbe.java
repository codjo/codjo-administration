package net.codjo.administration.server.audit.memory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class DefaultMemoryProbe implements MemoryProbe {
    private MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();


    public double getUsedMemory() {
        return (double)memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }


    public double getTotalMemory() {
        return (double)Runtime.getRuntime().totalMemory() / (1024 * 1024);
    }
}
