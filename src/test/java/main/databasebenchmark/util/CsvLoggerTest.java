package main.databasebenchmark.util;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

public class CsvLoggerTest {
    @Test
    public void testCsvLogging() throws IOException {
        File csvFile = new File("target/test-logs/test_results.csv");
        if (csvFile.exists()) {
            csvFile.delete();
        }
        
        SystemInfo sys = SystemInfo.collect();
        try (CsvLogger csvLogger = new CsvLogger(csvFile.getPath(), sys)) {
            // Write a log entry
            csvLogger.log(
                "org.hsqldb.jdbc.JDBCDriver",
                "jdbc:hsqldb:mem:testdb",
                1000L,
                2000L,
                500_000_000L, // 500ms
                "INSERT_TEST",
                100,
                10
            );
        }
        
        assertTrue(csvFile.exists());
        String content = Files.readString(csvFile.toPath());
        String[] lines = content.split("\\r?\\n");
        
        // Should have header + 1 log line
        assertTrue(lines.length >= 2);
        assertTrue(lines[0].startsWith("LocalDateTime,DBMS,DbDriver,DbUrl,t1,t2,OS,"));
        
        String dataLine = lines[1];
        assertTrue(dataLine.contains("HSQLDB"));
        assertTrue(dataLine.contains("org.hsqldb.jdbc.JDBCDriver"));
        assertTrue(dataLine.contains("jdbc:hsqldb:mem:testdb"));
        assertTrue(dataLine.contains("1000,2000"));
        assertTrue(dataLine.contains("INSERT_TEST,10,100,500")); // Operation, BatchSize, Rows, Duration (500 ms)
    }
}
