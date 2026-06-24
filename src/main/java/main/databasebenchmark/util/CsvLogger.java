package main.databasebenchmark.util;

import java.io.*;
import java.time.LocalDateTime;

/**
 * Writes benchmark results to a CSV log file.
 *
 * Two row types share the same file:
 *
 *   RUN rows   – one row per sub-operation per run (e.g. INSERT_SINGLE_CUSTOMER).
 *                RunNo = 1..N; AvgMs/StdDevMs/MinMs/MaxMs are empty.
 *
 *   STATS rows – one row per phase after all N runs complete.
 *                Operation is prefixed with "STATS_"; RunNo = 0;
 *                AvgMs/StdDevMs/MinMs/MaxMs are filled.
 *
 * Callers must invoke setRunNo(run) before each repeated run so the
 * current run number is stamped on every RUN row written in that run.
 */
public class CsvLogger implements AutoCloseable {

    private static final String HEADER =
            "LocalDateTime,DbDriver,DbUrl,t1Ns,t2Ns," +
            "Operation,RowCount,BatchSize,DurationMs,RowsPerSec," +
            "RunNo,AvgMs,StdDevMs,MinMs,MaxMs";

    private final BufferedWriter writer;
    private int currentRunNo = 1;

    public CsvLogger(String filename) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs();
        boolean writeHeader = !file.exists() || file.length() == 0;
        writer = new BufferedWriter(new FileWriter(file, true));
        if (writeHeader) {
            writer.write(HEADER);
            writer.newLine();
            writer.flush();
        }
    }

    /**
     * Set the run number that will be stamped on all subsequent log() calls.
     * Call this once per repeat iteration before running the benchmark phase.
     */
    public void setRunNo(int runNo) {
        this.currentRunNo = runNo;
    }

    /** Write one RUN row for a single sub-operation execution. */
    public void log(String driver, String url, long t1Ns, long t2Ns,
                    String operation, int rowCount, int batchSize) {
        try {
            long   durationMs = (t2Ns - t1Ns) / 1_000_000;
            double rowsPerSec = durationMs > 0 ? (rowCount * 1000.0 / durationMs) : 0.0;

            // RUN row – stats columns left empty
            String line = String.format("%s,%s,%s,%d,%d,%s,%d,%d,%d,%.0f,%d,,,,",
                    LocalDateTime.now(), driver, url,
                    t1Ns, t2Ns, operation,
                    rowCount, batchSize, durationMs, rowsPerSec,
                    currentRunNo);
            writer.write(line);
            writer.newLine();
            writer.flush();

            System.out.printf("    [LOG] %-44s  rows=%8d  batch=%5d  %6d ms  %12.0f rows/s%n",
                    operation, rowCount, batchSize, durationMs, rowsPerSec);
        } catch (IOException e) {
            System.err.println("CsvLogger write error: " + e.getMessage());
        }
    }

    /**
     * Write one STATS row summarising N repeated runs of a benchmark phase.
     * Called once per phase after the repeat loop completes.
     */
    public void logStats(String driver, String url, BenchmarkStats stats) {
        try {
            long avgRounded = Math.round(stats.getAvgMs());

            // STATS row – t1/t2 are 0, DurationMs = avg, RunNo = 0 (marker)
            String line = String.format("%s,%s,%s,0,0,STATS_%s,%d,0,%d,0,0,%.1f,%.1f,%d,%d",
                    LocalDateTime.now(), driver, url,
                    stats.getOperation(),
                    stats.getCount(),
                    avgRounded,
                    stats.getAvgMs(),
                    stats.getStdDevMs(),
                    stats.getMinMs(),
                    stats.getMaxMs());
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("CsvLogger write error: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("CsvLogger close error: " + e.getMessage());
        }
    }
}
