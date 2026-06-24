package main.databasebenchmark.dao;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {
    @Test
    public void testToString() {
        Product p = new Product();
        p.id = 456;
        p.name = "Widget";
        p.price = new BigDecimal("19.99");
        String expected = String.format("%5d  %-20s  %8.2f", 456, "Widget", new BigDecimal("19.99"));
        assertEquals(expected, p.toString());
    }

    @Test
    public void testCreateFromResultSet() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:testdb_product", "SA", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE PRODUCT (ID INTEGER, NAME VARCHAR(20), PRICE DECIMAL(10,2))");
                stmt.execute("INSERT INTO PRODUCT VALUES (456, 'Widget', 19.99)");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM PRODUCT")) {
                    assertTrue(rs.next());
                    Product p = Product.create(rs);
                    assertEquals(456, p.id);
                    assertEquals("Widget", p.name);
                    assertEquals(new BigDecimal("19.99"), p.price);
                }
            }
        }
    }
}
