package main.databasebenchmark;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.stream.Collectors;
import main.databasebenchmark.benchmark.DataGenerator;
import main.databasebenchmark.benchmark.DenormBenchmark;
import main.databasebenchmark.benchmark.InsertBenchmark;
import main.databasebenchmark.benchmark.SelectBenchmark;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.BenchmarkStats;
import main.databasebenchmark.util.CsvLogger;
import main.databasebenchmark.util.Logger;
import main.databasebenchmark.util.SystemInfo;

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

    public enum BenchmarkMode {
        HSQLDB_OPTIMAL("HSQLDB (Optimal - Delayed Write)", true, false, false),
        HSQLDB_EQUALIZED("HSQLDB (Equalized - Sync Write)", true, true, false),
        POSTGRESQL_OPTIMAL("PostgreSQL (Optimal - Sync Write)", false, false, false),
        POSTGRESQL_EQUALIZED("PostgreSQL (Equalized - Async & In-Memory)", false, false, true);

        public final String name;
        public final boolean isHsqldb;
        public final boolean isHsqlSync;
        public final boolean isPgAsyncUnlogged;

        BenchmarkMode(String name, boolean isHsqldb, boolean isHsqlSync, boolean isPgAsyncUnlogged) {
            this.name = name;
            this.isHsqldb = isHsqldb;
            this.isHsqlSync = isHsqlSync;
            this.isPgAsyncUnlogged = isPgAsyncUnlogged;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Collecting system information...");
        SystemInfo sysInfo = SystemInfo.collect();

        String actualLogPath;
        try (Logger logger = new Logger(LOG_FILE);
             CsvLogger csvLog = new CsvLogger(CSV_FILE, sysInfo)) {
            actualLogPath = logger.getPath();

            for (BenchmarkMode mode : BenchmarkMode.values()) {
                runBenchmark(mode, csvLog);
            }
        }

        System.out.println("Results written to " + CSV_FILE);
        System.out.println("Output log written to " + actualLogPath);
    }

    private static void runBenchmark(BenchmarkMode mode, CsvLogger log) {
        Db db;
        if (mode.isHsqldb) {
            db = buildHsqlDb("./data/onlineshop");
            db.url += ";shutdown=true";
            if (mode.isHsqlSync) {
                db.url += ";hsqldb.write_delay=false";
            }
        } else {
            db = buildPostgresDb("localhost", "onlineshop", "postgres", "postgres");
            if (mode.isPgAsyncUnlogged) {
                db.url += "?options=-c%20synchronous_commit=off";
            }
        }

        Connection con = db.getCon();
        if (con == null) {
            System.out.println("\nSkipping scenario: " + mode.name + " (database connection not available)\n");
            return;
        }

        System.out.println("\n DB Scenario: " + mode.name);
        System.out.println(" URL: " + db.url);

        try {
            // Explicitly set write delay for HSQLDB to ensure settings take effect even if the DB was already loaded in memory
            if (mode.isHsqldb) {
                try (Statement s = con.createStatement()) {
                    if (mode.isHsqlSync) {
                        s.execute("SET FILES WRITE DELAY FALSE");
                    } else {
                        s.execute("SET FILES WRITE DELAY TRUE");
                    }
                }
            }

            // Drop schema first to ensure clean state and correct table persistence properties
            DataGenerator.dropSchema(con);

            // Create schema with mode-specific table persistence (e.g. UNLOGGED for PG equalized)
            DataGenerator.createSchema(con, mode.isHsqldb, mode.isPgAsyncUnlogged);

            if (!mode.isHsqldb && mode.isPgAsyncUnlogged) {
                try (Statement s = con.createStatement()) {
                    s.execute("SET synchronous_commit = off");
                }
            }

            // SINGLE INSERT
            System.out.printf("%n=== SINGLE INSERT  (%d runs, customers=%d, products=%d) ===%n",
                    REPEAT_COUNT, CUSTOMER_COUNT, PRODUCT_COUNT);

            BenchmarkStats singleStats = new BenchmarkStats("INSERT_SINGLE");
            for (int run = 1; run <= REPEAT_COUNT; run++) {
                System.out.printf("%n  Run %d/%d%n", run, REPEAT_COUNT);
                DataGenerator.deleteAll(con);
                singleStats.add(InsertBenchmark.runSingle(db, CUSTOMER_COUNT, PRODUCT_COUNT, log));
            }
            printAndLogStats(singleStats);

            // BATCH INSERT
            System.out.printf("%n=== BATCH INSERT  (%d runs, batchSize=%d) ===%n",
                    REPEAT_COUNT, BATCH_SIZE);

            BenchmarkStats batchStats = new BenchmarkStats("INSERT_BATCH");
            for (int run = 1; run <= REPEAT_COUNT; run++) {
                System.out.printf("%n  Run %d/%d%n", run, REPEAT_COUNT);
                DataGenerator.deleteAll(con);
                batchStats.add(InsertBenchmark.runBatch(db, CUSTOMER_COUNT, PRODUCT_COUNT, BATCH_SIZE, log));
            }
            printAndLogStats(batchStats);

            // ── SELECT WITH JOINS ──────────────────────────────────────────
            // Data from the last batch-insert run is still present in the DB.
            // Running N SELECT rounds on the same dataset measures query
            // performance variance (caching effects, planner behaviour, etc.).
            System.out.printf("%n=== SELECT WITH JOINS  (%d runs) ===%n", REPEAT_COUNT);

            BenchmarkStats selectStats = new BenchmarkStats("SELECT_ALL");
            for (int run = 1; run <= REPEAT_COUNT; run++) {
                System.out.printf("%n  Run %d/%d%n", run, REPEAT_COUNT);
                selectStats.add(SelectBenchmark.runAll(db, log));
            }
            printAndLogStats(selectStats);

            // ── SELECT on denormalized table ───────────────────────────────
            // Build FLAT_SALES once from the normalized data (same rows the
            // JOIN phase queried). The build is not timed; only the SELECT
            // rounds are, so the comparison is JOIN-read vs flat-read.
            DenormBenchmark.populate(db);

            System.out.printf("%n=== SELECT denormalized  (%d runs) ===%n", REPEAT_COUNT);

            BenchmarkStats denormStats = new BenchmarkStats("SELECT_DENORM_ALL");
            for (int run = 1; run <= REPEAT_COUNT; run++) {
                System.out.printf("%n  Run %d/%d%n", run, REPEAT_COUNT);
                denormStats.add(DenormBenchmark.runAll(db, log));
            }
            printAndLogStats(denormStats);

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
    private static void printAndLogStats(BenchmarkStats stats) {
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
