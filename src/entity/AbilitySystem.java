package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbilitySystem {

    public enum Ability {
        CLAIRVOYANCE ("Clairvoyance", "Reveals the enemy's next move."),
        UNO_REVERSE  ("Uno Reverse",  "Reflects next damage instance back to enemy."),
        HYPNOTIZE    ("Hypnotize",    "Forces enemy to pick Rock next turn."),
        YOU_CHEATER  ("You Cheater",  "If you lose a round, that round is voided."),
        FULL_COUNTER ("Full Counter", "Reflects next damage at 2x multiplier.");

        public final String displayName;
        public final String description;
        Ability(String d, String desc) { displayName=d; description=desc; }
    }

    private final List<Ability> inventory = new ArrayList<>();

    public Ability addRandom(Random rand) {
        Ability[] all = Ability.values();
        Ability ab = all[rand.nextInt(all.length)];
        inventory.add(ab);
        return ab;
    }

    public void remove(Ability a)     { inventory.remove(a); }
    public boolean isEmpty()          { return inventory.isEmpty(); }
    public List<Ability> getAbilities() { return inventory; }

    /** Count how many of this ability are in inventory */
    public int count(Ability a) {
        int n = 0; for (Ability x : inventory) if (x == a) n++; return n;
    }

    /** Unique abilities (no duplicates), preserving first-seen order */
    public List<Ability> getUnique() {
        List<Ability> seen = new ArrayList<>();
        for (Ability a : inventory) if (!seen.contains(a)) seen.add(a);
        return seen;
    }
    public void setAbilities(List<Ability> abs) { inventory.clear(); inventory.addAll(abs); }
}