package main.databasebenchmark.util;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;

public class LoggerTest {
    @Test
    public void testLoggerRedirectionAndOutput() throws IOException {
        String testDirHint = "target/test-logs/output.log";
        
        // Clean up previous test directory if it exists
        Path testDir = Path.of("target/test-logs");
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }

        String actualPath;
        try (Logger logger = new Logger(testDirHint)) {
            actualPath = logger.getPath();
            assertNotNull(actualPath);
            assertTrue(new File(actualPath).exists());

            // Write to System.out and System.err
            System.out.println("Test log message 1");
            System.err.println("Test log message 2");
        }

        // After closing, original streams should be restored.
        // Let's check the contents of the log file.
        File logFile = new File(actualPath);
        assertTrue(logFile.exists());
        String content = Files.readString(logFile.toPath());
        
        assertTrue(content.contains("=== Run started:"));
        assertTrue(content.contains("Test log message 1"));
        assertTrue(content.contains("Test log message 2"));
        assertTrue(content.contains("=== Run ended:"));
        
        // Check that timestamps are present (e.g. matching ISO date time prefix)
        assertTrue(content.matches("(?s).*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2} Test log message 1.*"));
    }
}
