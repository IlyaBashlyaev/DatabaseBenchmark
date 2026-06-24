package main.databasebenchmark.benchmark;

import org.junit.jupiter.api.Test;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;
import main.databasebenchmark.util.SystemInfo;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

public class InsertBenchmarkTest {
    @Test
    public void testInsertBenchmark() throws Exception {
        File csvFile = new File("target/test-logs/insert_benchmark_results.csv");
        if (csvFile.exists()) {
            csvFile.delete();
        }
        
        SystemInfo sys = SystemInfo.collect();
        try (Db db = new Db();
             CsvLogger csvLogger = new CsvLogger(csvFile.getPath(), sys)) {
            
            db.url = "jdbc:hsqldb:mem:testdb_insert";
            Connection con = db.getCon();
            assertNotNull(con);
            
            DataGenerator.createSchema(con, true);
            
            // Test runSingle
            long durationSingle = InsertBenchmark.runSingle(db, 10, 5, csvLogger);
            assertTrue(durationSingle >= 0);
            
            // Verify count
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT count(*) FROM CUSTOMER")) {
                assertTrue(rs.next());
                assertEquals(10, rs.getInt(1));
            }
            
            // Clear and test runBatch
            DataGenerator.deleteAll(con);
            long durationBatch = InsertBenchmark.runBatch(db, 10, 5, 2, csvLogger);
            assertTrue(durationBatch >= 0);
            
            // Verify count
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT count(*) FROM CUSTOMER")) {
                assertTrue(rs.next());
                assertEquals(10, rs.getInt(1));
            }
        }
    }
}
