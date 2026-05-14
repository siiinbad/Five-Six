package main;

/**
 * Manages the narration (post-Vaughn cutscene) and the pre-battle
 * dialogue (final boss intro). Holds all line state; GamePanel's
 * update() drives advancement and keeps currentDialog in sync.
 */
public class NarrationManager {

    private String[] narLines    = {};
    private int      narIndex    = 0;
    private boolean  narHeld     = false;

    private String[] preBatLines = {};
    private int      preBatIndex = 0;
    private boolean  preBatHeld  = false;

    // ── Narration (post-Vaughn) ───────────────────────────────────
    public void initNarration(String[] lines) {
        narLines = lines;
        narIndex = 0;
        narHeld  = false;
    }

    public String narCurrentLine() {
        return (narIndex < narLines.length) ? narLines[narIndex] : "";
    }

    public boolean narIsLast() {
        return narIndex >= narLines.length - 1;
    }

    /**
     * Call from update() each frame.
     * @return true when the last line has been advanced past (start fade to final boss)
     */
    public boolean advanceNarration(boolean ePressed) {
        if (ePressed && !narHeld) {
            narHeld = true;
            narIndex++;
            return narIndex >= narLines.length;
        }
        if (!ePressed) narHeld = false;
        return false;
    }

    // ── Pre-battle dialogue (final boss intro) ────────────────────
    public void initPreBattle(String[] lines) {
        preBatLines = lines;
        preBatIndex = 0;
        preBatHeld  = false;
    }

    public String preBatCurrentLine() {
        return (preBatIndex < preBatLines.length) ? preBatLines[preBatIndex] : "";
    }

    public boolean preBatIsLast() {
        return preBatIndex >= preBatLines.length - 1;
    }

    /**
     * Call from update() each frame.
     * @return true when all pre-battle lines are done (transition to battleState)
     */
    public boolean advancePreBattle(boolean ePressed) {
        if (ePressed && !preBatHeld) {
            preBatHeld = true;
            preBatIndex++;
            return preBatIndex >= preBatLines.length;
        }
        if (!ePressed) preBatHeld = false;
        return false;
    }
}
