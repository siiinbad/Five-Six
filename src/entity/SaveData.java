package entity;

import java.io.*;
import java.util.*;

public class SaveData implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String SAVE_FILENAME = "fivesix_save.dat";
    private static final String SAVE_PATH = System.getProperty("user.home") + File.separator + SAVE_FILENAME;

    // --- Progressive autosave state (single-slot) ---
    public int version = 2;

    public String characterName = "";
    public String currentMapName = "gle";

    public int    playerHP = 0;
    public int    playerMaxHP = 0;
    public double damageMultiplier = 1.0;

    public Set<Integer> defeatedEnemies = new HashSet<>();
    public List<ItemSystem.Item> items = new ArrayList<>();              // flat list
    public List<AbilitySystem.Ability> abilities = new ArrayList<>();

    public long lastSavedAtMs = 0L;

    public SaveData() { }

    public SaveData(String characterName, String currentMapName, int playerHP, int playerMaxHP,
                    double damageMultiplier, Set<Integer> defeatedEnemies,
                    List<ItemSystem.Item> items, List<AbilitySystem.Ability> abilities) {
        this.characterName = safeName(characterName);
        this.currentMapName = (currentMapName == null || currentMapName.isBlank()) ? "gle" : currentMapName;
        this.playerHP = playerHP;
        this.playerMaxHP = playerMaxHP;
        this.damageMultiplier = damageMultiplier;
        this.defeatedEnemies = (defeatedEnemies == null) ? new HashSet<>() : new HashSet<>(defeatedEnemies);
        this.items = (items == null) ? new ArrayList<>() : new ArrayList<>(items);
        this.abilities = (abilities == null) ? new ArrayList<>() : new ArrayList<>(abilities);
        this.lastSavedAtMs = System.currentTimeMillis();
    }

    public boolean isForCharacter(String name) {
        return safeName(characterName).equalsIgnoreCase(safeName(name));
    }

    private static String safeName(String s) {
        return (s == null) ? "" : s.trim();
    }

    /** Write save to disk (single progressive slot). */
    public void saveToDisk() {
        lastSavedAtMs = System.currentTimeMillis();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_PATH))) {
            oos.writeObject(this);
        } catch (Exception e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    /** Read save from disk; returns null if none exists or the file is incompatible. */
    public static SaveData loadFromDisk() {
        File f = new File(SAVE_PATH);
        if (!f.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            SaveData sd = (SaveData) ois.readObject();
            sd.ensureDefaults();
            return sd;
        } catch (Exception e) {
            // If old/corrupt save exists, treat as no-save instead of crashing.
            System.err.println("Load failed: " + e.getMessage());
            return null;
        }
    }

    /** Best-effort guard against null fields from older saves. */
    private void ensureDefaults() {
        if (characterName == null) characterName = "";
        if (currentMapName == null || currentMapName.isBlank()) currentMapName = "gle";
        if (defeatedEnemies == null) defeatedEnemies = new HashSet<>();
        if (items == null) items = new ArrayList<>();
        if (abilities == null) abilities = new ArrayList<>();
        if (damageMultiplier <= 0) damageMultiplier = 1.0;
    }

    public static boolean saveExists() {
        return new File(SAVE_PATH).exists();
    }
}