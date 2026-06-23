package main.databasebenchmark.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger implements AutoCloseable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PrintStream fileOut;
    private final PrintStream originalOut;
    private final PrintStream originalErr;

    public Logger(String filename) throws IOException {
        File file = new File(filename);
        file.getParentFile().mkdirs();

        fileOut     = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, true)), true);
        originalOut = System.out;
        originalErr = System.err;

        System.setOut(new TeeStream(originalOut, fileOut));
        System.setErr(new TeeStream(originalErr, fileOut));

        System.out.println("=== Run started: " + LocalDateTime.now().format(FMT) + " ===");
    }

    @Override
    public void close() {
        System.out.println("=== Run ended:   " + LocalDateTime.now().format(FMT) + " ===\n");
        System.setOut(originalOut);
        System.setErr(originalErr);
        fileOut.close();
    }

    private static class TeeStream extends PrintStream {

        private final PrintStream second;

        TeeStream(PrintStream main, PrintStream second) {
            super(main, true);
            this.second = second;
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            second.write(buf, off, len);
        }

        @Override
        public void write(int b) {
            super.write(b);
            second.write(b);
        }

        @Override
        public void flush() {
            super.flush();
            second.flush();
        }
    }
}
