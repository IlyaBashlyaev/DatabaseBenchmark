package main.databasebenchmark;

import java.io.IOException;
import java.sql.*;
import main.databasebenchmark.benchmark.DataGenerator;
import main.databasebenchmark.benchmark.InsertBenchmark;
import main.databasebenchmark.benchmark.SelectBenchmark;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;
import main.databasebenchmark.util.Logger;

public class DatabaseBenchmark {

    // Scale up to 100_000+ for real measurements (10_000 is fast for development)
    private static final int CUSTOMER_COUNT = 10_000;
    private static final int PRODUCT_COUNT  = 10_000;
    private static final int BATCH_SIZE     = 500;

    private static final String CSV_FILE = "log/benchmark_results.csv";
    private static final String LOG_FILE = "log/output.log";

    public static void main(String[] args) throws IOException {
        Db hsqlDb = buildHsqlDb("./data/onlineshop");
        Db pgDb   = buildPostgresDb("localhost", "onlineshop", "postgres", "postgres");

        try (Logger _ = new Logger(LOG_FILE);
             CsvLogger csv_log = new CsvLogger(CSV_FILE)) {
            runBenchmark(hsqlDb, csv_log);
            runBenchmark(pgDb, csv_log);
        }

        System.out.println("Results written to " + CSV_FILE);
        System.out.println("Output log written to " + LOG_FILE);
    }

    private static void runBenchmark(Db db, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) {
            System.out.println("\nSkipping " + db.url + " (not available)\n");
            return;
        }

        System.out.println(" DB : " + db.url);
        boolean isHsqldb = db.url.contains("hsqldb");
        
        try {
            DataGenerator.createSchema(con, isHsqldb);

            System.out.println("\n=== Single INSERT ===");
            DataGenerator.deleteAll(con);
            InsertBenchmark.runSingle(db, CUSTOMER_COUNT, PRODUCT_COUNT, log);

            System.out.println("\n=== Batch INSERT  (batchSize=" + BATCH_SIZE + ") ===");
            DataGenerator.deleteAll(con);
            InsertBenchmark.runBatch(db, CUSTOMER_COUNT, PRODUCT_COUNT, BATCH_SIZE, log);

            System.out.println("\n=== SELECT with JOINs ===");
            SelectBenchmark.runAll(db, log);

        } catch (SQLException e) {
            System.err.println("Benchmark failed [" + db.url + "]: " + e.getMessage());
        } finally {
            db.close();
        }
    }

    private static Db buildHsqlDb(String filePath) {
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
