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

public class DenormBenchmarkTest {
    @Test
    public void testDenormBenchmark() throws Exception {
        File csvFile = new File("target/test-logs/denorm_benchmark_results.csv");
        if (csvFile.exists()) {
            csvFile.delete();
        }
        
        SystemInfo sys = SystemInfo.collect();
        try (Db db = new Db();
             CsvLogger csvLogger = new CsvLogger(csvFile.getPath(), sys)) {
            
            db.url = "jdbc:hsqldb:mem:testdb_denorm";
            Connection con = db.getCon();
            
            DataGenerator.createSchema(con, true);
            InsertBenchmark.runBatch(db, 10, 5, 5, csvLogger);
            
            DenormBenchmark.populate(db);
            
            // Verify FLAT_SALES is not empty
            try (Statement s = con.createStatement();
                 ResultSet rs = s.executeQuery("SELECT count(*) FROM FLAT_SALES")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) > 0);
            }
            
            long selectTime = DenormBenchmark.runAll(db, csvLogger);
            assertTrue(selectTime >= 0);
        }
    }
}
