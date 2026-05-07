package main;

import java.util.Set;

public final class HitboxColors {

    private static final int[] CHARACTER_BUTTON_COLORS = {
            Ui.BC_IVAN, Ui.BC_SAM, Ui.BC_NIMUEL, Ui.BC_JOHNFIEL
    };

    private static final int[] ENEMY_COLORS = {
            Map.COLOR_JAMES, Map.COLOR_ALIEYANDREW, Map.COLOR_KYLE, Map.COLOR_JOHNRU,
            Map.COLOR_ADRIAN, Map.COLOR_DARRYLL, Map.COLOR_GIO, Map.COLOR_YOHANN,
            Map.COLOR_DIRK, Map.COLOR_JAKE, Map.COLOR_VAUGHN
    };

    private static final Set<Integer> ALL_NPC_COLORS = Set.of(
            Map.COLOR_JAMES, Map.COLOR_ALIEYANDREW, Map.COLOR_KYLE, Map.COLOR_JOHNRU,
            Map.COLOR_ADRIAN, Map.COLOR_DARRYLL, Map.COLOR_GIO, Map.COLOR_YOHANN,
            Map.COLOR_DIRK, Map.COLOR_JAKE, Map.COLOR_VAUGHN, Map.COLOR_BROKENDOOR
    );

    private static final Set<Integer> GLE_NPC_COLORS = Set.of(
            Map.COLOR_JAMES, Map.COLOR_ALIEYANDREW, Map.COLOR_KYLE,
            Map.COLOR_JOHNRU, Map.COLOR_ADRIAN, Map.COLOR_BROKENDOOR
    );

    private static final Set<Integer> FRONTGATE_NPC_COLORS = Set.of(
            Map.COLOR_DARRYLL, Map.COLOR_GIO, Map.COLOR_YOHANN,
            Map.COLOR_DIRK, Map.COLOR_JAKE, Map.COLOR_VAUGHN, Map.COLOR_BROKENDOOR
    );

    private HitboxColors() {}

    public static final class Map {
        public static final int COLOR_WALL        = 0xA349A4;
        public static final int COLOR_SPAWN       = 0x22B14C;
        public static final int COLOR_JAMES       = 0x3F48CC;
        public static final int COLOR_ALIEYANDREW = 0xFFA1F2;
        public static final int COLOR_KYLE        = 0x00A2E8;
        public static final int COLOR_JOHNRU      = 0xFFF200;
        public static final int COLOR_ADRIAN      = 0xB97A57;
        public static final int COLOR_NEXTAREA    = 0xED1C24;
        public static final int COLOR_BROKENDOOR  = 0xFF7F27;
        public static final int COLOR_JAKE        = 0xC8BFE7;
        public static final int COLOR_DARRYLL     = 0xFFF848;
        public static final int COLOR_GIO         = 0x7D0000;
        public static final int COLOR_YOHANN      = 0x3C7D39;
        public static final int COLOR_DIRK        = 0x6D2183;
        public static final int COLOR_VAUGHN      = 0x7094FF;
        public static final int COLOR_FINALBOSS   = 0xFFFFFF;

        private Map() {}
    }

    public static final class Ui {
        public static final int BC_LOGO     = 0xBA8D7D;
        public static final int BC_START    = 0x51FFD5;
        public static final int BC_CREDIT   = 0x00FF7D;
        public static final int BC_QUIT     = 0x0DA100;
        public static final int BC_SETTINGS = 0x8FAEEA;
        public static final int BC_MUTE     = 0x000C78;
        public static final int BC_SAVE     = 0xA5FFB6;
        public static final int BC_CONTINUE = 0xCE74FF;
        public static final int BC_SELCHAR  = 0x47B2FF;
        public static final int BC_IVAN     = 0xFF0000;
        public static final int BC_SAM      = 0xFFE679;
        public static final int BC_NIMUEL   = 0xFFADAD;
        public static final int BC_JOHNFIEL = 0x0EFC7B;
        public static final int BC_HOVCHAR  = 0x078F00;
        public static final int BC_ITEMS    = 0xFEA800;
        public static final int BC_ABILINV  = 0xA600FF;
        public static final int BC_BACK     = 0x830043;
        public static final int BC_ROCKBTN  = 0xA4A29E;
        public static final int BC_PAPERBTN = 0x77FF88;
        public static final int BC_SCISSBTN = 0xC00000;
        public static final int BC_OUTCZONE = 0xBC9995;
        public static final int BC_CONTBAT  = 0xFF00C7;
        public static final int BC_PLRBAT   = 0x444757;
        public static final int BC_ENMBAT   = 0x692323;
        public static final int BC_USEITEM  = 0x5C4B29;
        public static final int BC_USEABIL  = 0x688544;
        public static final int BC_CHARDIAL = 0x6377FF;

        private Ui() {}
    }

    public static int[] characterButtonColors() {
        return CHARACTER_BUTTON_COLORS.clone();
    }

    public static String charName(int color) {
        return switch (color) {
            case Ui.BC_IVAN -> "ivan";
            case Ui.BC_SAM -> "sam";
            case Ui.BC_NIMUEL -> "nimuel";
            case Ui.BC_JOHNFIEL -> "johnfiel";
            default -> null;
        };
    }

    public static int charColor(String name) {
        if (name == null) return 0;
        return switch (name) {
            case "ivan" -> Ui.BC_IVAN;
            case "sam" -> Ui.BC_SAM;
            case "nimuel" -> Ui.BC_NIMUEL;
            case "johnfiel" -> Ui.BC_JOHNFIEL;
            default -> 0;
        };
    }

    public static boolean isNpcColor(int color) {
        return ALL_NPC_COLORS.contains(color);
    }

    public static int[] enemyColors() {
        return ENEMY_COLORS.clone();
    }

    public static Set<Integer> mapNpcColors(String mapName) {
        return switch (mapName) {
            case "gle" -> GLE_NPC_COLORS;
            case "frontgate" -> FRONTGATE_NPC_COLORS;
            default -> Set.of();
        };
    }

    public static String enemyName(int color) {
        return switch (color) {
            case Map.COLOR_JAMES -> "James";
            case Map.COLOR_ALIEYANDREW -> "Alieyandrew";
            case Map.COLOR_KYLE -> "Kyle";
            case Map.COLOR_JOHNRU -> "Johnru";
            case Map.COLOR_ADRIAN -> "Adrian";
            case Map.COLOR_DARRYLL -> "Darryll";
            case Map.COLOR_GIO -> "Gio";
            case Map.COLOR_YOHANN -> "Yohann";
            case Map.COLOR_DIRK -> "Dirk";
            case Map.COLOR_JAKE -> "Jake";
            case Map.COLOR_VAUGHN -> "Vaughn";
            case Map.COLOR_FINALBOSS -> "Mysterious Person";
            default -> "Enemy";
        };
    }

    public static String enemyFolder(int color) {
        return switch (color) {
            case Map.COLOR_JAMES -> "james";
            case Map.COLOR_ALIEYANDREW -> "alieyandrew";
            case Map.COLOR_KYLE -> "kyle";
            case Map.COLOR_JOHNRU -> "johnru";
            case Map.COLOR_ADRIAN -> "adrian";
            case Map.COLOR_DARRYLL -> "darryll";
            case Map.COLOR_GIO -> "gio";
            case Map.COLOR_YOHANN -> "yohann";
            case Map.COLOR_DIRK -> "dirk";
            case Map.COLOR_JAKE -> "jake";
            case Map.COLOR_VAUGHN -> "vaughn";
            default -> null;
        };
    }
}
