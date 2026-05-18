package main;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Stores top-10 fastest completion times (ms only, no names).
 * Saved as plain text — one long per line, sorted ascending.
 */
public class Leaderboard {

    private static final Path LB_FILE =
            Paths.get(System.getProperty("user.home"), "fivesix_leaderboard.dat");
    private static final int MAX_ENTRIES = 10;

    private final List<Long> times = new ArrayList<>();

    // ── Mutation ───────────────────────────────────────────────

    /** Add a completed run time and persist immediately. */
    public void addTime(long ms) {
        times.add(ms);
        times.sort(Comparator.naturalOrder());
        if (times.size() > MAX_ENTRIES) times.subList(MAX_ENTRIES, times.size()).clear();
        saveToDisk();
    }

    public List<Long> getTimes() { return Collections.unmodifiableList(times); }

    /** Returns true if ms is faster than or equal to the current best. */
    public boolean isPersonalBest(long ms) {
        return times.isEmpty() || ms <= times.get(0);
    }

    // ── Persistence ────────────────────────────────────────────

    public void loadFromDisk() {
        times.clear();
        try {
            if (!Files.exists(LB_FILE)) return;
            for (String line : Files.readAllLines(LB_FILE)) {
                line = line.trim();
                if (!line.isEmpty()) times.add(Long.parseLong(line));
            }
            times.sort(Comparator.naturalOrder());
        } catch (Exception ignored) {}
    }

    private void saveToDisk() {
        try {
            List<String> lines = new ArrayList<>();
            for (long t : times) lines.add(String.valueOf(t));
            Files.write(LB_FILE, lines);
        } catch (IOException ignored) {}
    }
}