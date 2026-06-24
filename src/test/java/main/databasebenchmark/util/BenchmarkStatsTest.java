package main.databasebenchmark.util;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BenchmarkStatsTest {
    @Test
    public void testEmptyStats() {
        BenchmarkStats stats = new BenchmarkStats("TEST");
        assertEquals("TEST", stats.getOperation());
        assertEquals(0, stats.getCount());
        assertEquals(0L, stats.getMinMs());
        assertEquals(0L, stats.getMaxMs());
        assertEquals(0.0, stats.getAvgMs());
        assertEquals(0.0, stats.getStdDevMs());
        assertTrue(stats.getSamples().isEmpty());
    }

    @Test
    public void testSingleSample() {
        BenchmarkStats stats = new BenchmarkStats("TEST");
        stats.add(100L);
        assertEquals(1, stats.getCount());
        assertEquals(100L, stats.getMinMs());
        assertEquals(100L, stats.getMaxMs());
        assertEquals(100.0, stats.getAvgMs());
        assertEquals(0.0, stats.getStdDevMs()); // fewer than 2 samples -> 0.0
        assertEquals(List.of(100L), stats.getSamples());
    }

    @Test
    public void testMultipleSamples() {
        BenchmarkStats stats = new BenchmarkStats("TEST");
        stats.add(10L);
        stats.add(20L);
        stats.add(30L);
        assertEquals(3, stats.getCount());
        assertEquals(10L, stats.getMinMs());
        assertEquals(30L, stats.getMaxMs());
        assertEquals(20.0, stats.getAvgMs());
        
        // Sum = 60, n = 3, avg = 20
        // SumSq = 10^2 + 20^2 + 30^2 = 100 + 400 + 900 = 1400
        // Variance = (1400 / 3) - 20^2 = 466.666... - 400 = 66.666...
        // StdDev = sqrt(66.666...) ≈ 8.1649658
        assertEquals(8.1649658, stats.getStdDevMs(), 0.0001);
        assertEquals(List.of(10L, 20L, 30L), stats.getSamples());
    }
}
