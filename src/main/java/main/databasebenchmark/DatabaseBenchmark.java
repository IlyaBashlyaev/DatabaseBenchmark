package main.databasebenchmark;

import java.io.IOException;
import java.sql.*;
import main.databasebenchmark.benchmark.DataGenerator;
import main.databasebenchmark.benchmark.InsertBenchmark;
import main.databasebenchmark.benchmark.SelectBenchmark;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;

public class DatabaseBenchmark {

    // Scale up to 100_000+ for real measurements (10_000 is fast for development)
    private static final int CUSTOMER_COUNT = 10_000;
    private static final int PRODUCT_COUNT  = 10_000;
    private static final int BATCH_SIZE     = 500;

    private static final String CSV_FILE = "benchmark_results.csv";

    public static void main(String[] args) throws IOException {
        Db hsqlDb = buildHsqlDb("./data/benchmark");
        Db pgDb   = buildPostgresDb("localhost", "onlineshop", "postgres", "postgres");

        try (CsvLogger log = new CsvLogger(CSV_FILE)) {
            runBenchmark(hsqlDb, log);
            runBenchmark(pgDb,   log);
        }

        System.out.println("\nResults written to " + CSV_FILE);
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

            System.out.println("\nSingle INSERT");
            DataGenerator.truncateAll(con);
            InsertBenchmark.runSingle(db, CUSTOMER_COUNT, PRODUCT_COUNT, log);

            System.out.println("\nBatch INSERT  (batchSize=" + BATCH_SIZE + ")");
            DataGenerator.truncateAll(con);
            InsertBenchmark.runBatch(db, CUSTOMER_COUNT, PRODUCT_COUNT, BATCH_SIZE, log);

            System.out.println("\nSELECT with JOINs");
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
