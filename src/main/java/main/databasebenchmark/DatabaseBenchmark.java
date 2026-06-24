package main.databasebenchmark;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.stream.Collectors;
import main.databasebenchmark.benchmark.DataGenerator;
import main.databasebenchmark.benchmark.InsertBenchmark;
import main.databasebenchmark.benchmark.SelectBenchmark;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.BenchmarkStats;
import main.databasebenchmark.util.CsvLogger;
import main.databasebenchmark.util.Logger;

public class DatabaseBenchmark {

    // Scale up to 100_000+ for real measurements (10_000 is fast for development)
    private static final int CUSTOMER_COUNT = 10_000;
    private static final int PRODUCT_COUNT  = 10_000;
    private static final int BATCH_SIZE     = 1_000;

    // Number of times each benchmark phase is repeated.
    // The guide recommends multiple runs to capture measurement variance
    // and enable standard-deviation analysis (Standardabweichung).
    private static final int REPEAT_COUNT = 10;

    private static final String CSV_FILE = "log/benchmark_results.csv";
    private static final String LOG_FILE = "log/output.log";

    private static final String DIVIDER =
            "  " + "─".repeat(72);

    public static void main(String[] args) throws IOException {
        Db hsqlDb = buildHsqlDb("./data/onlineshop");
        Db pgDb   = buildPostgresDb("localhost", "onlineshop", "postgres", "postgres");

        String actualLogPath;
        try (Logger logger = new Logger(LOG_FILE);
             CsvLogger csvLog = new CsvLogger(CSV_FILE)) {
            actualLogPath = logger.getPath();
            runBenchmark(hsqlDb, csvLog);
            runBenchmark(pgDb,   csvLog);
        }

        System.out.println("Results written to " + CSV_FILE);
        System.out.println("Output log written to " + actualLogPath);
    }

    private static void runBenchmark(Db db, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) {
            System.out.println("\nSkipping " + db.url + " (not available)\n");
            return;
        }

        System.out.println("\n DB : " + db.url);
        boolean isHsqldb = db.url.contains("hsqldb");

        try {
            DataGenerator.createSchema(con, isHsqldb);

            // SINGLE INSERT
            System.out.printf("%n=== SINGLE INSERT  (%d runs, customers=%d, products=%d) ===%n",
                    REPEAT_COUNT, CUSTOMER_COUNT, PRODUCT_COUNT);

            BenchmarkStats singleStats = new BenchmarkStats("INSERT_SINGLE");
            for (int run = 1; run <= REPEAT_COUNT; run++) {
                System.out.printf("%n  Run %d/%d%n", run, REPEAT_COUNT);
                DataGenerator.deleteAll(con);
                log.setRunNo(run);
                long t1 = System.nanoTime();
                InsertBenchmark.runSingle(db, CUSTOMER_COUNT, PRODUCT_COUNT, log);
                singleStats.add((System.nanoTime() - t1) / 1_000_000);
            }
            printAndLogStats(singleStats, db, log);

            // BATCH INSERT
            System.out.printf("%n=== BATCH INSERT  (%d runs, batchSize=%d) ===%n",
                    REPEAT_COUNT, BATCH_SIZE);

            BenchmarkStats batchStats = new BenchmarkStats("INSERT_BATCH");
            for (int run = 1; run <= REPEAT_COUNT; run++) {
                System.out.printf("%n  Run %d/%d%n", run, REPEAT_COUNT);
                DataGenerator.deleteAll(con);
                log.setRunNo(run);
                long t1 = System.nanoTime();
                InsertBenchmark.runBatch(db, CUSTOMER_COUNT, PRODUCT_COUNT, BATCH_SIZE, log);
                batchStats.add((System.nanoTime() - t1) / 1_000_000);
            }
            printAndLogStats(batchStats, db, log);

            // ── SELECT WITH JOINS ──────────────────────────────────────────
            // Data from the last batch-insert run is still present in the DB.
            // Running N SELECT rounds on the same dataset measures query
            // performance variance (caching effects, planner behaviour, etc.).
            System.out.printf("%n=== SELECT WITH JOINS  (%d runs) ===%n", REPEAT_COUNT);

            BenchmarkStats selectStats = new BenchmarkStats("SELECT_ALL");
            for (int run = 1; run <= REPEAT_COUNT; run++) {
                System.out.printf("%n  Run %d/%d%n", run, REPEAT_COUNT);
                log.setRunNo(run);
                long t1 = System.nanoTime();
                SelectBenchmark.runAll(db, log);
                selectStats.add((System.nanoTime() - t1) / 1_000_000);
            }
            printAndLogStats(selectStats, db, log);

        } catch (SQLException e) {
            System.err.println("Benchmark failed [" + db.url + "]: " + e.getMessage());
        } finally {
            db.close();
        }
    }

    /**
     * Prints the guide-style statistics summary to the console (and therefore
     * also to the text log via the TeeStream in Logger) and writes a STATS row
     * to the CSV file.
     *
     * Console output example (mirroring the guide's table style):
     *
     *   ── Individual durations [ms]: 4823, 4751, 4812, 4798, 4841
     *   ── avg = 4805.0 ms  |  stddev = 30.3 ms  |  min = 4751 ms  |  max = 4841 ms
     */
    private static void printAndLogStats(BenchmarkStats stats, Db db, CsvLogger log) {
        String samples = stats.getSamples().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        System.out.println(DIVIDER);
        System.out.printf("  Individual durations [ms]: %s%n", samples);
        System.out.printf(
                "  avg = %.1f ms  |  stddev = %.1f ms  |  min = %d ms  |  max = %d ms%n",
                stats.getAvgMs(), stats.getStdDevMs(),
                stats.getMinMs(), stats.getMaxMs());
        System.out.println(DIVIDER);

        log.logStats(db.driver, db.url, stats);
    }

    private static Db buildHsqlDb(String filePath) {
        // A stale .lck file is left behind when the JVM is force-stopped or the
        // data/ folder is removed from git while the DB was open. HSQLDB refuses
        // to connect until it is gone.
        File lockFile = new File(filePath + ".lck");
        if (lockFile.exists() && lockFile.delete()) {
            System.out.println("Removed stale HSQLDB lock file: " + lockFile.getPath());
        }

        Db db = new Db();
        db.driver = "org.hsqldb.jdbc.JDBCDriver";
        db.url    = "jdbc:hsqldb:file:" + filePath;
        db.usr    = "SA";
        db.passwd = "";
        return db;
    }

    private static Db buildPostgresDb(String host, String dbName, String user, String password) {
        Db db = new Db();
        db.driver = "org.postgresql.Driver";
        db.url    = "jdbc:postgresql://" + host + "/" + dbName;
        db.usr    = user;
        db.passwd = password;
        return db;
    }
}
