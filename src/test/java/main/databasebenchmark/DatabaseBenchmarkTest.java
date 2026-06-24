package main.databasebenchmark;

import org.junit.jupiter.api.Test;
import main.databasebenchmark.dao.Db;
import java.io.File;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseBenchmarkTest {
    @Test
    public void testBuildHsqlDb() throws Exception {
        // We use reflection since the method is private in DatabaseBenchmark
        Method method = DatabaseBenchmark.class.getDeclaredMethod("buildHsqlDb", String.class);
        method.setAccessible(true);
        
        String path = "target/test-db/mydb";
        Db db = (Db) method.invoke(null, path);
        assertNotNull(db);
        assertEquals("org.hsqldb.jdbc.JDBCDriver", db.driver);
        assertEquals("jdbc:hsqldb:file:" + path, db.url);
        assertEquals("SA", db.usr);
        assertEquals("", db.passwd);
    }
    
    @Test
    public void testBuildPostgresDb() throws Exception {
        Method method = DatabaseBenchmark.class.getDeclaredMethod("buildPostgresDb", String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        
        Db db = (Db) method.invoke(null, "localhost", "onlineshop", "postgres", "postgres");
        assertNotNull(db);
        assertEquals("org.postgresql.Driver", db.driver);
        assertEquals("jdbc:postgresql://localhost/onlineshop", db.url);
        assertEquals("postgres", db.usr);
        assertEquals("postgres", db.passwd);
    }
}
