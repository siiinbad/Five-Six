package entity;

import java.util.Set;

public interface IEnemyStats {
    void markDefeated(int color);
    void unmarkDefeated(int color);
    boolean isDefeated(int color);
    int getDefeatedCount();
    Set<Integer> getDefeatedEnemies();
    void setDefeated(Set<Integer> d);
    int getEnemyHP(int color);
    int getEnemyHP(int color, int completedFights);
}
