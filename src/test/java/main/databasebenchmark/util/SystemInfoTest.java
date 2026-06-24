package main.databasebenchmark.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SystemInfoTest {
    @Test
    public void testCollect() {
        SystemInfo info = SystemInfo.collect();
        assertNotNull(info);
        assertNotNull(info.os);
        assertNotNull(info.cpu);
        assertTrue(info.cores > 0, "Cores should be > 0");
        assertTrue(info.clockSpeedMhz >= 0, "Clock speed should be non-negative");
        assertTrue(info.ramMb >= 0, "RAM should be non-negative");
    }
}
