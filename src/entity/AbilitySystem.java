package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AbilitySystem implements IAbilitySystem {

    public enum Ability {
        CLAIRVOYANCE ("Clairvoyance", "Reveals the enemy's next move."),
        UNO_REVERSE  ("Uno Reverse",  "Reflects next damage instance back to enemy."),
        HYPNOTIZE    ("Hypnotize",    "Forces enemy to pick Rock next turn."),
        YOU_CHEATER  ("You Cheater",  "Voids the next enemy-winning round."),
        FULL_COUNTER ("Full Counter", "Reflects next damage at 2x multiplier.");

        public final String displayName;
        public final String description;
        Ability(String d, String desc) { displayName=d; description=desc; }
    }

    private final List<Ability> inventory = new ArrayList<>();

    @Override
    public Ability addRandom(Random rand) {
        Ability[] all = {Ability.CLAIRVOYANCE, Ability.UNO_REVERSE, Ability.HYPNOTIZE, Ability.YOU_CHEATER, Ability.FULL_COUNTER};
        Ability ab = all[rand.nextInt(all.length)];
        inventory.add(ab);
        return ab;
    }

    @Override
    public void remove(Ability a)     { inventory.remove(a); }
    @Override
    public boolean isEmpty()          { return inventory.isEmpty(); }
    @Override
    public List<Ability> getAbilities() { return inventory; }

    /** Count how many of this ability are in inventory */
    @Override
    public int count(Ability a) {
        int n = 0; for (Ability x : inventory) if (x == a) n++; return n;
    }

    /** Unique abilities (no duplicates), preserving first-seen order */
    @Override
    public List<Ability> getUnique() {
        List<Ability> seen = new ArrayList<>();
        for (Ability a : inventory) {
            if (!seen.contains(a)) seen.add(a);
        }
        return seen;
    }
    @Override
    public void setAbilities(List<Ability> abs) {
        inventory.clear();
        inventory.addAll(abs);
    }
}
