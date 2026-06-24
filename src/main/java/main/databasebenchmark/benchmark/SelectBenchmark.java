package main.databasebenchmark.benchmark;

import java.sql.*;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;

public class SelectBenchmark {

    /** @return total pure database time in milliseconds (sum of the per-query values logged) */
    public static long runAll(Db db, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) return 0L;

        long durationMs = 0L;

        // Q1: customers with invoice count and total spent
        durationMs += run(con, db, log, "SELECT_JOIN_CUSTOMER_INVOICE",
            "SELECT c.ID, c.FIRSTNAME, c.LASTNAME, " +
            "COUNT(i.ID) AS invoice_count, SUM(i.TOTAL) AS total_spent " +
            "FROM CUSTOMER c LEFT JOIN INVOICE i ON c.ID = i.CUSTOMERID " +
            "GROUP BY c.ID, c.FIRSTNAME, c.LASTNAME");

        // Q2: invoice lines with product names
        durationMs += run(con, db, log, "SELECT_JOIN_INVOICE_ITEM_PRODUCT",
            "SELECT i.ID, p.NAME, it.QUANTITY, it.COST, " +
            "(it.QUANTITY * it.COST) AS line_total " +
            "FROM INVOICE i " +
            "JOIN ITEM it ON i.ID = it.INVOICEID " +
            "JOIN PRODUCT p ON it.PRODUCTID = p.ID");

        // Q3: full four-table join with sort
        durationMs += run(con, db, log, "SELECT_JOIN_ALL_FOUR_TABLES",
            "SELECT c.LASTNAME, c.CITY, p.NAME, it.QUANTITY, it.COST " +
            "FROM CUSTOMER c " +
            "JOIN INVOICE i  ON c.ID = i.CUSTOMERID " +
            "JOIN ITEM it    ON i.ID = it.INVOICEID " +
            "JOIN PRODUCT p  ON it.PRODUCTID = p.ID " +
            "ORDER BY c.LASTNAME");

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
            System.err.println("Select error [" + operation + "]: " + e.getMessage());
            return 0L;
        }
    }
}
