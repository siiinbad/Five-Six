package entity;

import java.util.HashSet;
import java.util.Set;

public class EnemyStats {

    private static final int BASE_HP = 50;
    private static final int HP_INCREMENT = 5;

    // Tracks which NPC color codes have been defeated
    private Set<Integer> defeatedEnemies = new HashSet<>();

    public void markDefeated(int npcColor) {
        defeatedEnemies.add(npcColor);
    }

    public boolean isDefeated(int npcColor) {
        return defeatedEnemies.contains(npcColor);
    }

    public int getDefeatedCount() {
        return defeatedEnemies.size();
    }

    // Enemy HP = 50 + (5 * number of enemies already defeated) at time of battle
    public int getEnemyHP(int npcColor) {
        return BASE_HP + (HP_INCREMENT * defeatedEnemies.size());
    }

    public Set<Integer> getDefeatedEnemies() {
        return defeatedEnemies;
    }
}