package main.databasebenchmark.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates duration samples for one benchmark phase across N repeated runs
 * and computes descriptive statistics (avg, stddev, min, max).
 *
 * Usage pattern (mirrors the guide's JUnit example):
 *   BenchmarkStats stats = new BenchmarkStats("INSERT_SINGLE");
 *   for (int run = 1; run <= REPEAT_COUNT; run++) {
 *       long t1 = System.nanoTime();
 *       // ... run the benchmark ...
 *       stats.add((System.nanoTime() - t1) / 1_000_000);
 *   }
 *   // stats now holds avg, stddev, min, max across all runs
 */
public class BenchmarkStats {

    private final String operation;
    private final List<Long> samples = new ArrayList<>();

    private double sum   = 0;
    private double sumSq = 0;
    private long   min   = Long.MAX_VALUE;
    private long   max   = Long.MIN_VALUE;

    public BenchmarkStats(String operation) {
        this.operation = operation;
    }

    /** Record one sample duration (in milliseconds). */
    public void add(long durationMs) {
        samples.add(durationMs);
        sum   += durationMs;
        sumSq += (double) durationMs * durationMs;
        if (durationMs < min) min = durationMs;
        if (durationMs > max) max = durationMs;
    }

    public String     getOperation() { return operation; }
    public int        getCount()     { return samples.size(); }
    public long       getMinMs()     { return samples.isEmpty() ? 0 : min; }
    public long       getMaxMs()     { return samples.isEmpty() ? 0 : max; }
    public List<Long> getSamples()   { return Collections.unmodifiableList(samples); }

    public double getAvgMs() {
        int n = samples.size();
        return n > 0 ? sum / n : 0.0;
    }

    /**
     * Population standard deviation of the recorded samples.
     * Returns 0 if fewer than 2 samples have been added.
     */
    public double getStdDevMs() {
        int n = samples.size();
        if (n < 2) return 0.0;
        double avg = sum / n;
        return Math.sqrt(sumSq / n - avg * avg);
    }
}
