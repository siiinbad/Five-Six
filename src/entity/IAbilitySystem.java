package entity;

import java.util.List;
import java.util.Random;

public interface IAbilitySystem {
    AbilitySystem.Ability addRandom(Random rand);
    void remove(AbilitySystem.Ability a);
    boolean isEmpty();
    List<AbilitySystem.Ability> getAbilities();
    int count(AbilitySystem.Ability a);
    List<AbilitySystem.Ability> getUnique();
    void setAbilities(List<AbilitySystem.Ability> abs);
}
