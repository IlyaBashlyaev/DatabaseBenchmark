package main.databasebenchmark.benchmark;

import java.sql.*;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;

public class SelectBenchmark {

    public static void runAll(Db db, CsvLogger log) {
        Connection con = db.getCon();
        if (con == null) return;

        // Q1: customers with invoice count and total spent
        run(con, db, log, "SELECT_JOIN_CUSTOMER_INVOICE",
            "SELECT c.ID, c.FIRSTNAME, c.LASTNAME, " +
            "COUNT(i.ID) AS invoice_count, SUM(i.TOTAL) AS total_spent " +
            "FROM CUSTOMER c LEFT JOIN INVOICE i ON c.ID = i.CUSTOMERID " +
            "GROUP BY c.ID, c.FIRSTNAME, c.LASTNAME");

        // Q2: invoice lines with product names
        run(con, db, log, "SELECT_JOIN_INVOICE_ITEM_PRODUCT",
            "SELECT i.ID, p.NAME, it.QUANTITY, it.COST, " +
            "(it.QUANTITY * it.COST) AS line_total " +
            "FROM INVOICE i " +
            "JOIN ITEM it ON i.ID = it.INVOICEID " +
            "JOIN PRODUCT p ON it.PRODUCTID = p.ID");

        // Q3: full four-table join with sort
        run(con, db, log, "SELECT_JOIN_ALL_FOUR_TABLES",
            "SELECT c.LASTNAME, c.CITY, p.NAME, it.QUANTITY, it.COST " +
            "FROM CUSTOMER c " +
            "JOIN INVOICE i  ON c.ID = i.CUSTOMERID " +
            "JOIN ITEM it    ON i.ID = it.INVOICEID " +
            "JOIN PRODUCT p  ON it.PRODUCTID = p.ID " +
            "ORDER BY c.LASTNAME");
    }

    private static void run(Connection con, Db db, CsvLogger log,
                             String operation, String sql) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            long t1 = System.nanoTime();
            int rows = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows++;
            }
            log.log(db.driver, db.url, t1, System.nanoTime(), operation, rows, 0);
        } catch (SQLException e) {
            System.err.println("Select error [" + operation + "]: " + e.getMessage());
        }
    }
}
