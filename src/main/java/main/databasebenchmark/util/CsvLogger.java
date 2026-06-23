package main.databasebenchmark.util;

import java.io.*;
import java.time.LocalDateTime;

public class CsvLogger implements AutoCloseable {

    private static final String HEADER =
            "LocalDateTime,DbDriver,DbUrl,t1Ns,t2Ns,Operation,RowCount,BatchSize,DurationMs,RowsPerSec";

    private final BufferedWriter writer;

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

    public void log(String driver, String url, long t1Ns, long t2Ns,
                    String operation, int rowCount, int batchSize) {
        try {
            long   durationMs  = (t2Ns - t1Ns) / 1_000_000;
            double rowsPerSec  = durationMs > 0 ? (rowCount * 1000.0 / durationMs) : 0.0;

            String line = String.format("%s,%s,%s,%d,%d,%s,%d,%d,%d,%.0f",
                    LocalDateTime.now(), driver, url,
                    t1Ns, t2Ns, operation,
                    rowCount, batchSize, durationMs, rowsPerSec);
            writer.write(line);
            writer.newLine();
            writer.flush();

            System.out.printf("[LOG] %-40s  rows=%8d  batch=%5d  %6d ms  %12.0f rows/s%n",
                    operation, rowCount, batchSize, durationMs, rowsPerSec);
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
