package main.databasebenchmark.dao;

import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

public class CustomerTest {
    @Test
    public void testToString() {
        Customer c = new Customer();
        c.id = 123;
        c.firstName = "John";
        c.lastName = "Doe";
        c.street = "123 Main St";
        c.city = "New York";
        String expected = String.format("%5d  %-15s %-15s %-20s %-15s", 123, "John", "Doe", "123 Main St", "New York");
        assertEquals(expected, c.toString());
    }

    @Test
    public void testCreateFromResultSet() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:hsqldb:mem:testdb_customer", "SA", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE CUSTOMER (ID INTEGER, FIRSTNAME VARCHAR(20), LASTNAME VARCHAR(20), STREET VARCHAR(20), CITY VARCHAR(20))");
                stmt.execute("INSERT INTO CUSTOMER VALUES (1, 'Alice', 'Smith', '456 Oak Ave', 'Chicago')");
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM CUSTOMER")) {
                    assertTrue(rs.next());
                    Customer c = Customer.create(rs);
                    assertEquals(1, c.id);
                    assertEquals("Alice", c.firstName);
                    assertEquals("Smith", c.lastName);
                    assertEquals("456 Oak Ave", c.street);
                    assertEquals("Chicago", c.city);
                }
            }
        }
    }
}
