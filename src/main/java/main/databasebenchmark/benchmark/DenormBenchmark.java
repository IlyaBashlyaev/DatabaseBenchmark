package main.databasebenchmark.benchmark;

import java.sql.*;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;

public class DenormBenchmark {

    /**
     * Builds FLAT_SALES once from the already-inserted normalized tables so the
     * denormalized SELECTs run on exactly the same rows the JOIN phase queried.
     * This build is setup, not a measured benchmark, so it is not logged.
     */
    public static void populate(Db db) {
        Connection con = db.getCon();
        if (con == null) return;
        try (Statement s = con.createStatement()) {
            s.execute("DELETE FROM FLAT_SALES");
            s.execute(
                "INSERT INTO FLAT_SALES " +
                "(CUSTOMERID, FIRSTNAME, LASTNAME, CITY, INVOICEID, INVOICE_TOTAL, " +
                "PRODUCT_NAME, QUANTITY, COST) " +
                "SELECT c.ID, c.FIRSTNAME, c.LASTNAME, c.CITY, i.ID, i.TOTAL, " +
                "p.NAME, it.QUANTITY, it.COST " +
                "FROM CUSTOMER c " +
                "JOIN INVOICE i  ON c.ID = i.CUSTOMERID " +
                "JOIN ITEM it    ON i.ID = it.INVOICEID " +
                "JOIN PRODUCT p  ON it.PRODUCTID = p.ID");
        } catch (SQLException e) {
            System.err.println("Denorm populate error: " + e.getMessage());
        }
    }

    /** @return total pure database time in milliseconds (sum of the per-query values logged) */
    public static long runAll(Db db, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) return 0L;

        long durationMs = 0L;

        // Q1: customers with invoice count and total spent
        durationMs += run(con, db, log, "SELECT_DENORM_CUSTOMER_INVOICE",
            "SELECT CUSTOMERID, FIRSTNAME, LASTNAME, " +
            "COUNT(INVOICEID) AS invoice_count, SUM(INVOICE_TOTAL) AS total_spent " +
            "FROM FLAT_SALES " +
            "GROUP BY CUSTOMERID, FIRSTNAME, LASTNAME");

        // Q2: invoice lines with product names
        durationMs += run(con, db, log, "SELECT_DENORM_INVOICE_ITEM_PRODUCT",
            "SELECT INVOICEID, PRODUCT_NAME, QUANTITY, COST, " +
            "(QUANTITY * COST) AS line_total " +
            "FROM FLAT_SALES");

        // Q3: all columns with sort
        durationMs += run(con, db, log, "SELECT_DENORM_ALL_COLUMNS",
            "SELECT LASTNAME, CITY, PRODUCT_NAME, QUANTITY, COST " +
            "FROM FLAT_SALES " +
            "ORDER BY LASTNAME");

        return durationMs;
    }

    /** @return pure database time in milliseconds (executeQuery + result iteration), matching the logged value */
    private static long run(Connection con, Db db, CsvLogger log,
                             String operation, String sql) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            int rows = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows++;
            }
            long t2 = System.nanoTime();
            long dbNanos = t2 - t1;
            log.log(db.driver, db.url, t1, t2, dbNanos, operation, rows, 0);
            return dbNanos / 1_000_000;
        } catch (SQLException e) {
            System.err.println("Denorm select error [" + operation + "]: " + e.getMessage());
            return 0L;
        }
    }
}
