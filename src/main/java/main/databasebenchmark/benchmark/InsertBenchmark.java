package main.databasebenchmark.benchmark;

import java.sql.*;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;

public class InsertBenchmark {

    private static final int INVOICES_PER_CUSTOMER = 3;
    private static final int ITEMS_PER_INVOICE     = 5;

    public static void runSingle(Db db, int customerCount, int productCount, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) return;
        try {
            con.setAutoCommit(true);
            int invoiceCount = customerCount * INVOICES_PER_CUSTOMER;
            insertCustomersSingle(con, customerCount, db, log);
            insertProductsSingle(con, productCount, db, log);
            insertInvoicesSingle(con, invoiceCount, customerCount, db, log);
            insertItemsSingle(con, invoiceCount, productCount, db, log);
        } catch (SQLException e) {
            System.err.println("Single insert error: " + e.getMessage());
        }
    }

    public static void runBatch(Db db, int customerCount, int productCount, int batchSize, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) return;
        try {
            con.setAutoCommit(false);
            int invoiceCount = customerCount * INVOICES_PER_CUSTOMER;
            insertCustomersBatch(con, customerCount, batchSize, db, log);
            insertProductsBatch(con, productCount, batchSize, db, log);
            insertInvoicesBatch(con, invoiceCount, customerCount, batchSize, db, log);
            insertItemsBatch(con, invoiceCount, productCount, batchSize, db, log);
            con.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Batch insert error: " + e.getMessage());
            try { con.rollback(); con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
    
    // SINGLE INSERT

    private static void insertCustomersSingle(Connection con, int count,
                                               Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO CUSTOMER (ID, FIRSTNAME, LASTNAME, STREET, CITY) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.firstName());
                ps.setString(3, DataGenerator.lastName());
                ps.setString(4, DataGenerator.street());
                ps.setString(5, DataGenerator.city());
                ps.executeUpdate();
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_SINGLE_CUSTOMER", count, 1);
        }
    }

    private static void insertProductsSingle(Connection con, int count,
                                              Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO PRODUCT (ID, NAME, PRICE) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.productName());
                ps.setDouble(3, DataGenerator.price());
                ps.executeUpdate();
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_SINGLE_PRODUCT", count, 1);
        }
    }

    private static void insertInvoicesSingle(Connection con, int invoiceCount, int customerCount,
                                              Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO INVOICE (ID, CUSTOMERID, TOTAL) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            for (int i = 0; i < invoiceCount; i++) {
                ps.setInt(1, i);
                ps.setInt(2, i % customerCount);
                ps.setDouble(3, DataGenerator.price() * 10);
                ps.executeUpdate();
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_SINGLE_INVOICE", invoiceCount, 1);
        }
    }

    private static void insertItemsSingle(Connection con, int invoiceCount, int productCount,
                                           Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO ITEM (INVOICEID, ITEM, PRODUCTID, QUANTITY, COST) VALUES (?,?,?,?,?)";
        int totalItems = invoiceCount * ITEMS_PER_INVOICE;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            for (int inv = 0; inv < invoiceCount; inv++) {
                for (int pos = 0; pos < ITEMS_PER_INVOICE; pos++) {
                    ps.setInt(1, inv);
                    ps.setInt(2, pos);
                    ps.setInt(3, (inv * ITEMS_PER_INVOICE + pos) % productCount);
                    ps.setInt(4, DataGenerator.quantity());
                    ps.setDouble(5, DataGenerator.cost());
                    ps.executeUpdate();
                }
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_SINGLE_ITEM", totalItems, 1);
        }
    }

    // BATCH INSERT

    private static void insertCustomersBatch(Connection con, int count, int batchSize,
                                              Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO CUSTOMER (ID, FIRSTNAME, LASTNAME, STREET, CITY) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.firstName());
                ps.setString(3, DataGenerator.lastName());
                ps.setString(4, DataGenerator.street());
                ps.setString(5, DataGenerator.city());
                ps.addBatch();
                if ((i + 1) % batchSize == 0) { ps.executeBatch(); con.commit(); }
            }
            ps.executeBatch(); con.commit();
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_BATCH_CUSTOMER", count, batchSize);
        }
    }

    private static void insertProductsBatch(Connection con, int count, int batchSize,
                                             Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO PRODUCT (ID, NAME, PRICE) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            for (int i = 0; i < count; i++) {
                ps.setInt(1, i);
                ps.setString(2, DataGenerator.productName());
                ps.setDouble(3, DataGenerator.price());
                ps.addBatch();
                if ((i + 1) % batchSize == 0) { ps.executeBatch(); con.commit(); }
            }
            ps.executeBatch(); con.commit();
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_BATCH_PRODUCT", count, batchSize);
        }
    }

    private static void insertInvoicesBatch(Connection con, int invoiceCount, int customerCount,
                                             int batchSize, Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO INVOICE (ID, CUSTOMERID, TOTAL) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            for (int i = 0; i < invoiceCount; i++) {
                ps.setInt(1, i);
                ps.setInt(2, i % customerCount);
                ps.setDouble(3, DataGenerator.price() * 10);
                ps.addBatch();
                if ((i + 1) % batchSize == 0) { ps.executeBatch(); con.commit(); }
            }
            ps.executeBatch(); con.commit();
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_BATCH_INVOICE", invoiceCount, batchSize);
        }
    }

    private static void insertItemsBatch(Connection con, int invoiceCount, int productCount,
                                          int batchSize, Db db, CsvLogger log) throws SQLException {
        String sql = "INSERT INTO ITEM (INVOICEID, ITEM, PRODUCTID, QUANTITY, COST) VALUES (?,?,?,?,?)";
        int totalItems = invoiceCount * ITEMS_PER_INVOICE;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            int n = 0;
            for (int inv = 0; inv < invoiceCount; inv++) {
                for (int pos = 0; pos < ITEMS_PER_INVOICE; pos++) {
                    ps.setInt(1, inv);
                    ps.setInt(2, pos);
                    ps.setInt(3, (inv * ITEMS_PER_INVOICE + pos) % productCount);
                    ps.setInt(4, DataGenerator.quantity());
                    ps.setDouble(5, DataGenerator.cost());
                    ps.addBatch();
                    if (++n % batchSize == 0) { ps.executeBatch(); con.commit(); }
                }
            }
            ps.executeBatch(); con.commit();
            log.log(db.driver, db.url, t1, System.nanoTime(), "INSERT_BATCH_ITEM", totalItems, batchSize);
        }
    }
}
