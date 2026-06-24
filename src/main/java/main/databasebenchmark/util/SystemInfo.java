package main.databasebenchmark.util;

import java.io.*;
import java.util.stream.Collectors;

/**
 * Collects hardware and OS information once at program startup.
 * Uses OS-specific commands (sysctl on macOS, /proc on Linux, wmic on Windows)
 * with no external dependencies.
 */
public class SystemInfo {

    public final String os;
    public final String cpu;
    public final int    cores;
    public final long   clockSpeedMhz;
    public final long   ramMb;

    private SystemInfo(String os, String cpu, int cores, long clockSpeedMhz, long ramMb) {
        this.os            = os;
        this.cpu           = cpu;
        this.cores         = cores;
        this.clockSpeedMhz = clockSpeedMhz;
        this.ramMb         = ramMb;
    }

    public static SystemInfo collect() {
        String os         = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String cpu        = detectCpu();
        int    cores      = Runtime.getRuntime().availableProcessors();
        long   clockMhz   = detectClockSpeedMhz();
        long   ramMb      = detectRamMb();
        return new SystemInfo(sanitize(os), sanitize(cpu), cores, clockMhz, ramMb);
    }

    private static String detectCpu() {
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            if (osName.contains("mac")) {
                return exec("sysctl", "-n", "machdep.cpu.brand_string").trim();
            } else if (osName.contains("linux")) {
                try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("model name")) {
                            return line.substring(line.indexOf(':') + 1).trim();
                        }
                    }
                }
            } else if (osName.contains("windows")) {
                return exec("wmic", "cpu", "get", "Name").lines()
                        .map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.equalsIgnoreCase("name"))
                        .findFirst().orElse("Unknown");
            }
        } catch (Exception ignored) {}
        return System.getProperty("os.arch", "Unknown");
    }

    private static long detectClockSpeedMhz() {
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            if (osName.contains("mac")) {
                // hw.cpufrequency works on Intel; hw.cpufrequency_max on some systems
                for (String key : new String[]{"hw.cpufrequency", "hw.cpufrequency_max"}) {
                    try {
                        String val = exec("sysctl", "-n", key).trim();
                        if (!val.isEmpty()) return Long.parseLong(val) / 1_000_000;
                    } catch (Exception ignored) {}
                }
            } else if (osName.contains("linux")) {
                File f = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
                if (f.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        return Long.parseLong(br.readLine().trim()) / 1000; // kHz -> MHz
                    }
                }
                try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("cpu MHz")) {
                            return (long) Double.parseDouble(line.substring(line.indexOf(':') + 1).trim());
                        }
                    }
                }
            } else if (osName.contains("windows")) {
                String val = exec("wmic", "cpu", "get", "MaxClockSpeed").lines()
                        .map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.equalsIgnoreCase("maxclockspeed"))
                        .findFirst().orElse("0");
                return Long.parseLong(val);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static long detectRamMb() {
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            if (osName.contains("mac")) {
                String val = exec("sysctl", "-n", "hw.memsize").trim();
                return Long.parseLong(val) / (1024 * 1024);
            } else if (osName.contains("linux")) {
                try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("MemTotal")) {
                            String[] parts = line.split("\\s+");
                            return Long.parseLong(parts[1]) / 1024; // kB -> MB
                        }
                    }
                }
            } else if (osName.contains("windows")) {
                String val = exec("wmic", "computersystem", "get", "TotalPhysicalMemory").lines()
                        .map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.equalsIgnoreCase("totalphysicalmemory"))
                        .findFirst().orElse("0");
                return Long.parseLong(val) / (1024 * 1024);
            }
        } catch (Exception ignored) {}
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    private static String exec(String... cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String output;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            output = br.lines().collect(Collectors.joining("\n"));
        }
        p.waitFor();
        return output;
    }

    /** Remove commas so the value is safe to embed in a CSV field without quoting. */
    private static String sanitize(String value) {
        return value.replace(",", " ");
    }
}
