package entity;

import java.util.HashSet;
import java.util.Set;

public class EnemyStats {
    private static final int BASE_HP = 50;
    private static final int HP_INC  = 10;
    private Set<Integer> defeated = new HashSet<>();

    public void markDefeated(int color)   { defeated.add(color); }
    public void unmarkDefeated(int color) { defeated.remove(color); }
    public boolean isDefeated(int color)  { return defeated.contains(color); }
    public int  getDefeatedCount()        { return defeated.size(); }
    public Set<Integer> getDefeatedEnemies() { return defeated; }
    public void setDefeated(Set<Integer> d)  { defeated = new HashSet<>(d); }

    /** HP scales up with every enemy already beaten. */
    public int getEnemyHP(int color) {
        return getEnemyHP(color, defeated.size());
    }

    /** HP scales up with completed fights so enemies keep getting tougher over time. */
    public int getEnemyHP(int color, int completedFights) {
        int fights = Math.max(0, completedFights);
        if (color == 0xFFFFFF) {
            return 300 + HP_INC * fights;
        }
        return BASE_HP + HP_INC * fights;
    }
}
