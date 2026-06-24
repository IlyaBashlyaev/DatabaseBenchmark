package main.databasebenchmark.benchmark;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

public class DataGeneratorTest {
    @Test
    public void testRandomGenerators() {
        assertNotNull(DataGenerator.firstName());
        assertFalse(DataGenerator.firstName().isEmpty());
        
        assertNotNull(DataGenerator.lastName());
        assertFalse(DataGenerator.lastName().isEmpty());
        
        assertNotNull(DataGenerator.city());
        assertFalse(DataGenerator.city().isEmpty());
        
        assertNotNull(DataGenerator.productName());
        assertFalse(DataGenerator.productName().isEmpty());
        
        assertNotNull(DataGenerator.street());
        assertFalse(DataGenerator.street().isEmpty());
        
        double price = DataGenerator.price();
        assertTrue(price >= 1.0 && price <= 100.0);
        
        double cost = DataGenerator.cost();
        assertTrue(cost >= 0.5 && cost <= 50.5);
        
        int qty = DataGenerator.quantity();
        assertTrue(qty >= 1 && qty <= 20);
    }

    @Test
    public void testCreateSchemaAndDeleteAll() throws Exception {
        try (Connection con = DriverManager.getConnection("jdbc:hsqldb:mem:testdb_generator", "SA", "")) {
            DataGenerator.createSchema(con, true);
            
            // Check that tables exist by executing a select query
            try (Statement stmt = con.createStatement()) {
                stmt.execute("INSERT INTO CUSTOMER (ID, FIRSTNAME, LASTNAME, STREET, CITY) VALUES (1, 'A', 'B', 'C', 'D')");
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM CUSTOMER")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
            
            // Delete all and verify
            DataGenerator.deleteAll(con);
            try (Statement stmt = con.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM CUSTOMER")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            }
        }
    }
}
