package main;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Stores top-10 fastest completion times with player names.
 * Saved as plain text — time:name per line, sorted ascending.
 */
public class Leaderboard {

    public static record Entry(String name, long time) {}

    private static final Path LB_FILE =
            Paths.get(System.getProperty("user.home"), "fivesix_leaderboard.dat");
    private static final int MAX_ENTRIES = 10;

    private final List<Entry> entries = new ArrayList<>();

    // ── Mutation ───────────────────────────────────────────────

    /** Add a completed run time and persist immediately. */
    public void addTime(String name, long ms) {
        entries.add(new Entry(name, ms));
        entries.sort(Comparator.comparingLong(Entry::time));
        if (entries.size() > MAX_ENTRIES) entries.subList(MAX_ENTRIES, entries.size()).clear();
        saveToDisk();
    }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }

    /** Retained for backwards compatibility if needed. */
    public List<Long> getTimes() {
        List<Long> times = new ArrayList<>();
        for (Entry e : entries) times.add(e.time());
        return times;
    }

    /** Returns true if ms is faster than or equal to the current best. */
    public boolean isPersonalBest(long ms) {
        return entries.isEmpty() || ms <= entries.get(0).time();
    }

    // ── Persistence ────────────────────────────────────────────

    public void loadFromDisk() {
        entries.clear();
        try {
            if (!Files.exists(LB_FILE)) return;
            for (String line : Files.readAllLines(LB_FILE)) {
                line = line.trim();
                if (!line.isEmpty()) {
                    int colonIdx = line.indexOf(':');
                    if (colonIdx != -1) {
                        long time = Long.parseLong(line.substring(0, colonIdx));
                        String name = line.substring(colonIdx + 1);
                        entries.add(new Entry(name, time));
                    } else {
                        long time = Long.parseLong(line);
                        entries.add(new Entry("Player", time));
                    }
                }
            }
            entries.sort(Comparator.comparingLong(Entry::time));
        } catch (Exception ignored) {}
    }

    private void saveToDisk() {
        try {
            List<String> lines = new ArrayList<>();
            for (Entry e : entries) {
                lines.add(e.time() + ":" + e.name());
            }
            Files.write(LB_FILE, lines);
        } catch (IOException ignored) {}
    }
}