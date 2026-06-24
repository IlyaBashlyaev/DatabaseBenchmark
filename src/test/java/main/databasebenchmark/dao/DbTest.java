package main.databasebenchmark.dao;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

public class DbTest {
    @Test
    public void testGetConAndClose() throws SQLException {
        try (Db db = new Db()) {
            db.url = "jdbc:hsqldb:mem:testdb_db";
            Connection con = db.getCon();
            assertNotNull(con);
            assertFalse(con.isClosed());
            
            // Closing Db should close connection
            db.close();
            assertTrue(con.isClosed());
        }
    }
    
    @Test
    public void testInvalidDriverOrConnection() {
        try (Db db = new Db()) {
            db.driver = "invalid.DriverClass";
            db.url = "jdbc:invalid:url";
            Connection con = db.getCon();
            assertNull(con);
        }
    }
}
