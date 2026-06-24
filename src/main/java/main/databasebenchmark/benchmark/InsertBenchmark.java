package main.databasebenchmark.benchmark;

import java.sql.*;

import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;

public class InsertBenchmark {

    private static final int INVOICES_PER_CUSTOMER = 1;
    private static final int ITEMS_PER_INVOICE = 1;

    /** @return total pure database time in milliseconds (sum of the per-operation values logged) */
    public static long runSingle(Db db, int customerCount, int productCount, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) return 0L;
        long durationMs = 0L;
        try {
            con.setAutoCommit(true);
            int invoiceCount = customerCount * INVOICES_PER_CUSTOMER;
            durationMs += insertCustomersSingle(con, customerCount, db, log);
            durationMs += insertProductsSingle(con, productCount, db, log);
            durationMs += insertInvoicesSingle(con, invoiceCount, customerCount, db, log);
            durationMs += insertItemsSingle(con, invoiceCount, productCount, db, log);
        } catch (SQLException e) {
            System.err.println("Single insert error: " + e.getMessage());
        }
        return durationMs;
    }

    /** @return total pure database time in milliseconds (sum of the per-operation values logged) */
    public static long runBatch(Db db, int customerCount, int productCount, int batchSize, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) return 0L;
        long durationMs = 0L;
        try {
            con.setAutoCommit(false);
            int invoiceCount = customerCount * INVOICES_PER_CUSTOMER;
            durationMs += insertCustomersBatch(con, customerCount, batchSize, db, log);
            durationMs += insertProductsBatch(con, productCount, batchSize, db, log);
            durationMs += insertInvoicesBatch(con, invoiceCount, customerCount, batchSize, db, log);
            durationMs += insertItemsBatch(con, invoiceCount, productCount, batchSize, db, log);
            con.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Batch insert error: " + e.getMessage());
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
        return durationMs;
    }

    // SINGLE INSERT

    private static long insertCustomersSingle(Connection con, int count,
                                              Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO CUSTOMER (ID, FIRSTNAME, LASTNAME, STREET, CITY) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.firstName());
                ps.setString(3, DataGenerator.lastName());
                ps.setString(4, DataGenerator.street());
                ps.setString(5, DataGenerator.city());
                long s = System.nanoTime();
                ps.executeUpdate();
                dbNanos += System.nanoTime() - s;
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_SINGLE_CUSTOMER", count, 1);
            return dbNanos / 1_000_000;
        }
    }

    private static long insertProductsSingle(Connection con, int count,
                                             Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO PRODUCT (ID, NAME, PRICE) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.productName());
                ps.setDouble(3, DataGenerator.price());
                long s = System.nanoTime();
                ps.executeUpdate();
                dbNanos += System.nanoTime() - s;
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_SINGLE_PRODUCT", count, 1);
            return dbNanos / 1_000_000;
        }
    }

    private static long insertInvoicesSingle(Connection con, int invoiceCount, int customerCount,
                                             Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO INVOICE (ID, CUSTOMERID, TOTAL) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            for (int i = 0; i < invoiceCount; i++) {
                ps.setInt(1, i);
                ps.setInt(2, i % customerCount);
                ps.setDouble(3, DataGenerator.price() * 10);
                long s = System.nanoTime();
                ps.executeUpdate();
                dbNanos += System.nanoTime() - s;
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_SINGLE_INVOICE", invoiceCount, 1);
            return dbNanos / 1_000_000;
        }
    }

    private static long insertItemsSingle(Connection con, int invoiceCount, int productCount,
                                          Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO ITEM (INVOICEID, ITEM, PRODUCTID, QUANTITY, COST) VALUES (?,?,?,?,?)";
        int totalItems = invoiceCount * ITEMS_PER_INVOICE;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            for (int inv = 0; inv < invoiceCount; inv++) {
                for (int pos = 0; pos < ITEMS_PER_INVOICE; pos++) {
                    ps.setInt(1, inv);
                    ps.setInt(2, pos);
                    ps.setInt(3, (inv * ITEMS_PER_INVOICE + pos) % productCount);
                    ps.setInt(4, DataGenerator.quantity());
                    ps.setDouble(5, DataGenerator.cost());
                    long s = System.nanoTime();
                    ps.executeUpdate();
                    dbNanos += System.nanoTime() - s;
                }
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_SINGLE_ITEM", totalItems, 1);
            return dbNanos / 1_000_000;
        }
    }

    // BATCH INSERT

    private static long insertCustomersBatch(Connection con, int count, int batchSize,
                                             Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO CUSTOMER (ID, FIRSTNAME, LASTNAME, STREET, CITY) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.firstName());
                ps.setString(3, DataGenerator.lastName());
                ps.setString(4, DataGenerator.street());
                ps.setString(5, DataGenerator.city());
                ps.addBatch();
                if ((i + 1) % batchSize == 0) {
                    dbNanos += flush(con, ps);
                }
            }
            if (count % batchSize != 0) {
                dbNanos += flush(con, ps);
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_BATCH_CUSTOMER", count, batchSize);
            return dbNanos / 1_000_000;
        }
    }

    private static long insertProductsBatch(Connection con, int count, int batchSize,
                                            Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO PRODUCT (ID, NAME, PRICE) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.productName());
                ps.setDouble(3, DataGenerator.price());
                ps.addBatch();
                if ((i + 1) % batchSize == 0) {
                    dbNanos += flush(con, ps);
                }
            }
            if (count % batchSize != 0) {
                dbNanos += flush(con, ps);
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_BATCH_PRODUCT", count, batchSize);
            return dbNanos / 1_000_000;
        }
    }

    private static long insertInvoicesBatch(Connection con, int invoiceCount, int customerCount,
                                            int batchSize, Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO INVOICE (ID, CUSTOMERID, TOTAL) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            for (int i = 0; i < invoiceCount; i++) {
                ps.setInt(1, i);
                ps.setInt(2, i % customerCount);
                ps.setDouble(3, DataGenerator.price() * 10);
                ps.addBatch();
                if ((i + 1) % batchSize == 0) {
                    dbNanos += flush(con, ps);
                }
            }
            if (invoiceCount % batchSize != 0) {
                dbNanos += flush(con, ps);
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_BATCH_INVOICE", invoiceCount, batchSize);
            return dbNanos / 1_000_000;
        }
    }

    private static long insertItemsBatch(Connection con, int invoiceCount, int productCount,
                                         int batchSize, Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO ITEM (INVOICEID, ITEM, PRODUCTID, QUANTITY, COST) VALUES (?,?,?,?,?)";
        int totalItems = invoiceCount * ITEMS_PER_INVOICE;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            long dbNanos = 0L;
            int n = 0;
            for (int inv = 0; inv < invoiceCount; inv++) {
                for (int pos = 0; pos < ITEMS_PER_INVOICE; pos++) {
                    ps.setInt(1, inv);
                    ps.setInt(2, pos);
                    ps.setInt(3, (inv * ITEMS_PER_INVOICE + pos) % productCount);
                    ps.setInt(4, DataGenerator.quantity());
                    ps.setDouble(5, DataGenerator.cost());
                    ps.addBatch();
                    if (++n % batchSize == 0) {
                        dbNanos += flush(con, ps);
                    }
                }
            }
            if (n % batchSize != 0) {
                dbNanos += flush(con, ps);
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), dbNanos, "INSERT_BATCH_ITEM", totalItems, batchSize);
            return dbNanos / 1_000_000;
        }
    }

    /**
     * Executes the accumulated batch and commits, timing only these two database
     * calls. Returns the pure database time in nanoseconds.
     */
    private static long flush(Connection con, PreparedStatement ps) throws SQLException {
        long s = System.nanoTime();
        ps.executeBatch();
        con.commit();
        return System.nanoTime() - s;
    }
}
