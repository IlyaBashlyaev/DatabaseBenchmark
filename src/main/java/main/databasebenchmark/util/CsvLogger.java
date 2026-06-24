package main.databasebenchmark.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Writes benchmark results to a CSV log file.
 *
 * One row per sub-operation execution (e.g. INSERT_SINGLE_CUSTOMER).
 * System info (OS, CPU, cores, clock speed, RAM) is captured once at
 * construction time and stamped on every row.
 *
 * Column order:
 *   LocalDateTime, DBMS, DbDriver, DbUrl, t1, t2,
 *   OS, CPU, Cores, ClockSpeed, RAM,
 *   Operation, BatchSize, Rows, Duration, RowsPerSecond
 */
public class CsvLogger implements AutoCloseable {

    private static final String HEADER =
            "LocalDateTime,DBMS,DbDriver,DbUrl,t1,t2," +
            "OS,CPU,Cores,ClockSpeed,RAM," +
            "Operation,BatchSize,Rows,Duration,RowsPerSecond";

    private final BufferedWriter writer;
    private final SystemInfo     sys;

    public CsvLogger(String filename, SystemInfo sys) throws IOException {
        this.sys = sys;
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

    /** Write one row for a single sub-operation execution. */
    public void log(String driver, String url, long t1, long t2,
                    String operation, int rowCount, int batchSize) {
        try {
            long   durationMs = (t2 - t1) / 1_000_000;
            double rowsPerSec = durationMs > 0 ? (rowCount * 1000.0 / durationMs) : 0.0;

            String line = String.format("%s,%s,%s,%s,%d,%d,%s,%s,%d,%d,%d,%s,%d,%d,%d,%.0f",
                    LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                    dbmsName(driver), driver, url,
                    t1, t2,
                    sys.os, sys.cpu, sys.cores, sys.clockSpeedMhz, sys.ramMb,
                    operation, batchSize, rowCount, durationMs, rowsPerSec);
            writer.write(line);
            writer.newLine();
            writer.flush();

            System.out.printf("    [LOG] %-44s  rows=%8d  batch=%5d  %6d ms  %12.0f rows/s%n",
                    operation, rowCount, batchSize, durationMs, rowsPerSec);
        } catch (IOException e) {
            System.err.println("CsvLogger write error: " + e.getMessage());
        }
    }

    private static String dbmsName(String driver) {
        if (driver.contains("hsqldb"))      return "HSQLDB";
        if (driver.contains("postgresql"))  return "PostgreSQL";
        if (driver.contains("sqlite"))      return "SQLite";
        if (driver.contains("mysql"))       return "MySQL";
        if (driver.contains("mariadb"))     return "MariaDB";
        return driver;
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
