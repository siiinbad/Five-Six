package entity;

public class CharacterStats {

    public enum CharacterType {
        IVAN("Ivan", "Balanced All Around", 100, 1.0),
        NIMUEL("Nimuel", "Glass Cannon", 70, 1.5),
        SAM("Sam", "Tank", 130, 0.7),
        JOHNFIEL("Johnfiel", "Cheat", 999, 999.0);

        public final String displayName;
        public final String description;
        public final int maxHP;
        public final double damageMultiplier;

        CharacterType(String displayName, String description, int maxHP, double damageMultiplier) {
            this.displayName = displayName;
            this.description = description;
            this.maxHP = maxHP;
            this.damageMultiplier = damageMultiplier;
        }

        public static CharacterType fromName(String name) {
            for (CharacterType c : values()) {
                if (c.displayName.equalsIgnoreCase(name) || name.equalsIgnoreCase(c.name())) {
                    return c;
                }
            }
            return IVAN; // default fallback
        }
    }
}