package main;

import static main.HitboxColors.Map.*;

public final class DialogueDisplay {

    private DialogueDisplay() {}

    public static String brokenDoor() {
        return "The door seems to be broken... it won't open.";
    }

    public static String nextAreaLocked() {
        return "Defeat Johnru first to proceed.";
    }

    public static String blockedMiniboss(int color) {
        return switch (color) {
            case COLOR_JOHNRU -> "Johnru: Come back when you've beaten the others.";
            case COLOR_VAUGHN -> "Vaughn: Take care of my boys first.";
            default -> "";
        };
    }

    public static String enemyDialogue(int color, int stage, String playerName) {
        return switch (color) {
            case COLOR_JAMES -> switch (stage) {
                case 1 -> "James: Yo " + playerName + ", you want your money back?";
                case 2 -> "James: Okay, but you got to win first. Let's battle!";
                default -> "";
            };
            case COLOR_ALIEYANDREW -> switch (stage) {
                case 1 -> "Alieyandrew: Oh hi there, " + playerName + "!";
                case 2 -> "Alieyandrew: So you want your money back?";
                case 3 -> "Alieyandrew: Okay, beat me and I'll give it to you.";
                default -> "";
            };
            case COLOR_KYLE -> switch (stage) {
                case 1 -> "Kyle: Oi!! " + playerName + ", you're here to get your money?";
                case 2 -> "Kyle: HAHAHA! Come beat me first!";
                default -> "";
            };
            case COLOR_JOHNRU -> switch (stage) {
                case 1 -> "Johnru: Oh, you think you're strong now, " + playerName + "?";
                case 2 -> "Johnru: Let's go then - I'll let you get out of the room.";
                default -> "";
            };
            case COLOR_ADRIAN -> switch (stage) {
                case 1 -> "Adrian: Hi there " + playerName + ", class is over.";
                case 2 -> "Adrian: Oh, you want your money I owe you?";
                case 3 -> "Adrian: Okay, no problem - let's play a game first.";
                default -> "";
            };
            case COLOR_DARRYLL -> switch (stage) {
                case 1 -> "Darryll: Oh? You came all the way here for your money, " + playerName + "?";
                case 2 -> "Darryll: You'll have to pry it from my hands!";
                default -> "";
            };
            case COLOR_GIO -> switch (stage) {
                case 1 -> "Gio: Don't even bother, " + playerName + ". You won't win.";
                case 2 -> "Gio: Alright, let's settle this the hard way.";
                default -> "";
            };
            case COLOR_YOHANN -> switch (stage) {
                case 1 -> "Yohann: Hey " + playerName + ", long time no see!";
                case 2 -> "Yohann: Sorry but I'm keeping your money unless you beat me.";
                default -> "";
            };
            case COLOR_DIRK -> switch (stage) {
                case 1 -> "Dirk: Hmph. So you made it this far, " + playerName + ".";
                case 2 -> "Dirk: Don't think I'll go easy on you.";
                case 3 -> "Dirk: En garde.";
                default -> "";
            };
            case COLOR_JAKE -> switch (stage) {
                case 1 -> "Jake: Wooah, " + playerName + "! You're really here?";
                case 2 -> "Jake: I respect the hustle. Let's battle!";
                case 3 -> "Jake: May the best one win!";
                default -> "";
            };
            case COLOR_VAUGHN -> switch (stage) {
                case 1 -> "Vaughn: So you really made it, " + playerName + ". Impressive.";
                case 2 -> "Vaughn: But this ends here. Let's go!";
                default -> "";
            };
            default -> "";
        };
    }

    public static String[] finalBossPreBattle(String playerName) {
        return new String[] {
                playerName + ": \"Back off! I'm not giving you anything!\"",
                "Beggar: \"You think you can just walk away? !!!HAND IT OVER NOW!!!\"",
                playerName + ": \"You want it? Come and get it then.\"",
                "Beggar: \"Fine. Don't say I didn't warn you...\"",
                "!!!Both of you get into stance. There's no turning back.!!!"
        };
    }

    public static String[] finalBossNarration(String playerName) {
        return new String[] {
                "After a long day of fighting, you've finally reclaimed every last peso...",
                playerName + ": \"Finally... I have all my money back. Jollibee, here I come-\"",
                "???: \"Excuse me po, sir...\"",
                playerName + ": \"Huh?\"",
                "Beggar: \"Sir, pwede po bang makahingi ng barya? Gutom na gutom na po ako.\"",
                playerName + ": \"Sorry, I can't. I only have just enough for what I need.\"",
                "Beggar: \"...Just enough? No, no. I saw how much you have. Don't lie to me.\"",
                playerName + ": \"I'm serious, I don't have anything extra\"",
                "Beggar: \"THEN I'LL TAKE IT FROM YOU!!\"",
                "The beggar lunges forward. You have no choice but to fight!"
        };
    }
}
