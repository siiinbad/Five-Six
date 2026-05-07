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

    public void addRandom(Random rand) {
        Ability[] all = Ability.values();
        inventory.add(all[rand.nextInt(all.length)]);
    }

    public void remove(Ability a)     { inventory.remove(a); }
    public boolean isEmpty()          { return inventory.isEmpty(); }
    public List<Ability> getAbilities() { return inventory; }
    public void setAbilities(List<Ability> abs) { inventory.clear(); inventory.addAll(abs); }
}
