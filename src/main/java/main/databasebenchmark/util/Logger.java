package main.databasebenchmark.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Redirects System.out and System.err to both the console and a dated log file.
 *
 * The log file is placed in a subdirectory named after the current date so that
 * each day's output is kept separate:
 *
 *   log/YYYY-MM-DD/output.log
 *
 * Every line written to the file is prefixed with an ISO 8601 timestamp
 * (YYYY-MM-DDThh:mm:ss). The console output is left unchanged.
 */
public class Logger implements AutoCloseable {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Colons are illegal in filenames on Windows, so use underscores for the time part.
    private static final DateTimeFormatter FILE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss");

    private final PrintStream fileOut;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final String logFilePath;

    /**
     * @param filename  Path hint such as "log/output.log". The parent directory
     *                  is kept; the file itself is placed inside a YYYY-MM-DD
     *                  subdirectory with a per-run timestamp in the filename,
     *                  e.g. "log/2026-06-24/output_2026-06-24T10_35_42.log".
     *                  Each run creates a new file — no appending.
     */
    public Logger(String filename) throws IOException {
        File hint      = new File(filename);
        String base    = hint.getParent() != null ? hint.getParent() : "log";
        String name    = hint.getName();

        // Strip extension so we can insert the timestamp before it.
        int dot        = name.lastIndexOf('.');
        String stem    = dot >= 0 ? name.substring(0, dot) : name;
        String ext     = dot >= 0 ? name.substring(dot)    : "";

        LocalDateTime now = LocalDateTime.now();
        String stamp   = now.format(FILE_FMT);
        String dated   = stem + "_" + stamp + ext;   // e.g. output_2026-06-24T10_35_42.log

        File dateDir   = new File(base, now.toLocalDate().toString());
        dateDir.mkdirs();

        File logFile   = new File(dateDir, dated);
        logFilePath    = logFile.getPath();

        // false = never append; every run owns its own file.
        OutputStream raw        = new FileOutputStream(logFile, false);
        OutputStream buffered   = new BufferedOutputStream(raw);
        OutputStream timestamped = new TimestampedOutputStream(buffered);
        fileOut = new PrintStream(timestamped, true, StandardCharsets.UTF_8);

        originalOut = System.out;
        originalErr = System.err;

        System.setOut(new TeeStream(originalOut, fileOut));
        System.setErr(new TeeStream(originalErr, fileOut));

        System.out.println("=== Run started: " + LocalDateTime.now().format(ISO_FMT) + " ===");
    }

    /** Returns the actual path of the log file being written to. */
    public String getPath() {
        return logFilePath;
    }

    @Override
    public void close() {
        System.out.println("=== Run ended:   " + LocalDateTime.now().format(ISO_FMT) + " ===\n");
        System.setOut(originalOut);
        System.setErr(originalErr);
        fileOut.close();
    }

    // ── Inner classes ──────────────────────────────────────────────────────────

    /**
     * Prepends an ISO 8601 timestamp to the first byte of every new line.
     * Only the file stream is wrapped with this; the console stream is not,
     * so console output stays clean.
     */
    private static class TimestampedOutputStream extends OutputStream {

        private final OutputStream delegate;
        private boolean atLineStart = true;

        TimestampedOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            if (atLineStart) {
                writeTimestamp();
            }
            delegate.write(b);
            atLineStart = (b == '\n');
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                write(buf[i] & 0xFF);
            }
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        private void writeTimestamp() throws IOException {
            byte[] ts = (LocalDateTime.now().format(ISO_FMT) + " ").getBytes(StandardCharsets.UTF_8);
            delegate.write(ts);
        }
    }

    /** Writes every byte to two streams: the original console and the log file. */
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
