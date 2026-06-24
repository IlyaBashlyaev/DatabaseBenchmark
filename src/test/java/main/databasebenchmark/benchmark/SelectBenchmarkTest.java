package main.databasebenchmark.benchmark;

import org.junit.jupiter.api.Test;
import main.databasebenchmark.dao.Db;
import main.databasebenchmark.util.CsvLogger;
import main.databasebenchmark.util.SystemInfo;
import java.io.File;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

public class SelectBenchmarkTest {
    @Test
    public void testSelectBenchmark() throws Exception {
        File csvFile = new File("target/test-logs/select_benchmark_results.csv");
        if (csvFile.exists()) {
            csvFile.delete();
        }
        
        SystemInfo sys = SystemInfo.collect();
        try (Db db = new Db();
             CsvLogger csvLogger = new CsvLogger(csvFile.getPath(), sys)) {
            
            db.url = "jdbc:hsqldb:mem:testdb_select";
            Connection con = db.getCon();
            
            DataGenerator.createSchema(con, true);
            InsertBenchmark.runBatch(db, 10, 5, 5, csvLogger);
            
            long selectTime = SelectBenchmark.runAll(db, csvLogger);
            assertTrue(selectTime >= 0);
        }
    }
}
