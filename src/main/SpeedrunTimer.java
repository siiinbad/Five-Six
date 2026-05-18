package main;

import java.io.*;
import java.nio.file.*;

/**
 * Tracks real elapsed play-time in milliseconds.
 * Only ticks during active gameplay states.
 * Pauses automatically on menu, loading, inventory, etc.
 * Persists via a flat file so runs survive app restarts.
 */
public class SpeedrunTimer {

    private static final Path TIMER_FILE =
            Paths.get(System.getProperty("user.home"), "fivesix_timer.dat");

    private long    elapsedMs = 0L;
    private long    tickStart = -1L;
    private boolean running   = false;

    // ── State control ──────────────────────────────────────────

    /** Call every game-loop frame with the current gameState. */
    public void tick(int gameState) {
        boolean shouldRun = isActiveState(gameState);
        if (shouldRun && !running) {
            tickStart = System.currentTimeMillis();
            running   = true;
        } else if (!shouldRun && running) {
            elapsedMs += System.currentTimeMillis() - tickStart;
            running    = false;
        }
    }

    private boolean isActiveState(int gs) {
        return gs == GamePanel.playState
            || gs == GamePanel.battleState
            || gs == GamePanel.outcomeState
            || gs == GamePanel.preBattleState
            || gs == GamePanel.narrationState
            || gs == GamePanel.resultState
            || gs == GamePanel.fadeState;
    }

    /** Current total ms including any currently-running segment. */
    public long getElapsedMs() {
        if (running) return elapsedMs + (System.currentTimeMillis() - tickStart);
        return elapsedMs;
    }

    /** Force-stop the timer (call on game completion). */
    public void stop() {
        if (running) {
            elapsedMs += System.currentTimeMillis() - tickStart;
            running = false;
        }
    }

    // ── Persistence ────────────────────────────────────────────

    public void saveToDisk() {
        try { Files.writeString(TIMER_FILE, String.valueOf(getElapsedMs())); }
        catch (IOException ignored) {}
    }

    public void reset() {
        if (running) {
            elapsedMs += System.currentTimeMillis() - tickStart;
            running = false;
        }
        elapsedMs = 0;
        tickStart = -1;
        running = false;
        try { Files.deleteIfExists(TIMER_FILE); } catch (IOException ignored) {}
    }

    public void loadFromDisk() {
        try {
            if (!Files.exists(TIMER_FILE)) {
                elapsedMs = 0;
                return;
            }
            String raw = Files.readString(TIMER_FILE).trim();
            if (raw.isEmpty()) {
                elapsedMs = 0;
                return;
            }
            elapsedMs = Long.parseLong(raw);
        } catch (Exception ignored) {
            elapsedMs = 0;
        }
    }
    // ── Formatting ─────────────────────────────────────────────

    /** Returns a "HH:MM:SS.cc" string (cc = centiseconds). */
    public static String format(long ms) {
        long centis  = (ms % 1000) / 10;
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / 60_000) % 60;
        long hours   = ms / 3_600_000;
        return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centis);
    }

    public String formatCurrent() { return format(getElapsedMs()); }
}