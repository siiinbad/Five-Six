package main;

import entity.*;
import java.util.*;
import static main.HitboxColors.Map.*;

/**
 * Owns all battle logic. Reads/writes GamePanel state through a direct reference
 * (same package). GamePanel holds one instance of this class.
 */
public class BattleManager {

    public final BattleContext ctx;
    private final GamePanel gp;

    public BattleManager(GamePanel gp) {
        this.gp  = gp;
        this.ctx = new BattleContext();
    }

    // ── Start ─────────────────────────────────────────────────────
    public void startBattle() {
        ctx.reset();
        ctx.isFinalBoss         = (gp.pendingBattleEnemyColor == COLOR_FINALBOSS);
        ctx.enemyMaxHP          = gp.enemyStats.getEnemyHP(gp.pendingBattleEnemyColor, gp.completedFights);
        ctx.enemyHP             = ctx.enemyMaxHP;
        ctx.enemyDamageMultiplier = 1.0 + gp.completedFights * 0.025;
        ctx.battleRound         = 1;
        ctx.battleMsg           = "Round 1 - Choose your move!";
        ctx.enemyName           = HitboxColors.enemyName(gp.pendingBattleEnemyColor);

        gp.imageDisplay.loadBattleImages(gp.currentMapName, gp.pendingBattleEnemyColor,
                ctx.isFinalBoss, gp.player != null ? gp.player.characterName : null);
        gp.syncImageState();
        gp.audio.playMusic("battle_sountrack");

        if (ctx.isFinalBoss) {
            String first = gp.player != null ? cap(gp.player.characterName) : "Player";
            gp.narMgr.initPreBattle(DialogueDisplay.finalBossPreBattle(first));
            gp.gameState = GamePanel.preBattleState;
        } else {
            gp.gameState = GamePanel.battleState;
        }
    }

    // ── Resolve a round ───────────────────────────────────────────
    public void resolve(BattleSystem.Move pm) {
        BattleSystem.Move em;
        if (ctx.fxHypnotize) {
            em = BattleSystem.Move.ROCK;
            ctx.fxHypnotize = false;
        } else if (ctx.clairMove != null) {
            em = ctx.clairMove;
            ctx.clairMove = null;
        } else {
            em = BattleSystem.getRandomEnemyMove();
        }
        ctx.clairVisible = false;
        ctx.lastPMove = pm; ctx.lastEMove = em;

        BattleSystem.BattleResult result = BattleSystem.resolve(pm, em);
        ctx.lastBattleResult  = result;
        ctx.lastPlayerDamage  = 0;
        ctx.lastEnemyDamage   = 0;
        ctx.lastPlayerHeal    = 0;

        // You Cheater: voids an enemy-winning round
        if (result == BattleSystem.BattleResult.ENEMY_WIN && ctx.fxYouCheater) {
            ctx.fxYouCheater = false;
            ctx.lastBattleResult = null;
            ctx.battleMsg = "You Cheater activated! Round voided - pick again.";
            loadOutcomeAndSwitch(pm, em);
            return;
        }

        // Ticking heal
        if (ctx.fxHealRounds > 0) {
            gp.player.currentHP = Math.min(gp.player.currentHP + ctx.fxHealAmt, gp.player.maxHP);
            ctx.fxHealRounds--;
        }

        double dm = gp.player.damageMultiplier;

        switch (result) {
            case PLAYER_WIN -> {
                int dmg = (int) Math.ceil((gp.rand.nextInt(11) + 5) * dm);
                if (ctx.fxFullCounter) { dmg *= 2; ctx.fxFullCounter = false; }
                ctx.fxUnoReverse = false;
                ctx.enemyHP = Math.max(0, ctx.enemyHP - dmg);
                ctx.lastPlayerDamage = dmg;
                if (ctx.enemyHP <= 0) {
                    int healed = healAfterDefeat();
                    ctx.lastPlayerHeal = healed;
                    ctx.battleMsg = "You Win! Dealt " + dmg + " dmg — " + ctx.enemyName + " defeated!";
                    if (healed > 0) ctx.battleMsg += " Healed " + healed + " HP.";
                    ctx.battleResolved = true;
                    gp.enemyStats.markDefeated(gp.pendingBattleEnemyColor);
                    grantRewards();
                } else {
                    ctx.battleMsg = "You Win! Dealt " + dmg + " damage to " + ctx.enemyName + ".";
                }
            }
            case ENEMY_WIN -> {
                int dmg = (int) Math.ceil(((gp.rand.nextInt(15) + 1) * ctx.enemyDamageMultiplier)
                        / Math.max(0.1, gp.player.damageMultiplier));
                if (ctx.isFinalBoss) dmg = (int)(dmg * 1.5);

                if (ctx.fxUnoReverse || ctx.fxFullCounter) {
                    int ref = ctx.fxFullCounter ? dmg * 2 : dmg;
                    ctx.fxUnoReverse = false; ctx.fxFullCounter = false;
                    ctx.enemyHP = Math.max(0, ctx.enemyHP - ref);
                    ctx.lastPlayerDamage = ref;
                    ctx.battleMsg = "Reflected " + ref + " damage back!";
                    if (ctx.enemyHP <= 0) {
                        int healed = healAfterDefeat();
                        ctx.lastPlayerHeal = healed;
                        ctx.battleMsg += " " + ctx.enemyName + " defeated!";
                        if (healed > 0) ctx.battleMsg += " Healed " + healed + " HP.";
                        ctx.battleResolved = true;
                        gp.enemyStats.markDefeated(gp.pendingBattleEnemyColor);
                        grantRewards();
                    }
                } else {
                    gp.player.currentHP = Math.max(0, gp.player.currentHP - dmg);
                    ctx.lastEnemyDamage  = dmg;
                    if (gp.player.currentHP <= 0) {
                        ctx.battleMsg = "You Lose! Took " + dmg + " dmg — Defeated!";
                        ctx.battleResolved = true;
                    } else {
                        ctx.battleMsg = "You Lose! Took " + dmg + " damage from " + ctx.enemyName + ".";
                    }
                }
            }
            case DRAW -> ctx.battleMsg = "Draw! No damage dealt.";
        }

        loadOutcomeAndSwitch(pm, em);
    }

    // ── Outcome → next state ──────────────────────────────────────
    public void nextRound() {
        ctx.showRewardsBox = false;
        if (!ctx.battleResolved) {
            gp.gameState = GamePanel.battleState;
            ctx.battleRound++;
            ctx.battleMsg = "Round " + ctx.battleRound + " - Choose your move!";
            ctx.waitingOutcome = false;
            return;
        }
        boolean won = gp.player != null && gp.player.currentHP > 0;

        if (ctx.isFinalBoss && won) {
            saveFinalBossCheckpoint();
            gp.speedrunTimer.stop();
            
            // Don't record John Fiel runs
            if (gp.player != null && !"johnfiel".equalsIgnoreCase(gp.player.characterName)) {
                String defaultName = BattleManager.cap(gp.player.characterName);
                String playerName = javax.swing.JOptionPane.showInputDialog(gp, 
                    "Congratulations! Enter your name for the Leaderboard:", 
                    defaultName);
                if (playerName == null || playerName.trim().isEmpty()) {
                    playerName = defaultName;
                } else {
                    playerName = playerName.trim();
                }
                gp.leaderboard.addTime(playerName, gp.speedrunTimer.getElapsedMs());
            }
            
            gp.speedrunTimer.saveToDisk();
            gp.audio.stopMusic();
            gp.audio.playSFX("Win_Final_Boss");
            gp.gameState = GamePanel.winState;
            return;
        }
        if (ctx.isFinalBoss && !won) {
            gp.audio.stopMusic();
            gp.audio.playSFX("Loss_Final_Boss");
            gp.enemyStats = new EnemyStats();
            gp.items = new ItemSystem();
            gp.abilities = new AbilitySystem();
            gp.completedFights = 0;
            gp.saveData = null;
            new java.io.File(System.getProperty("user.home")
                    + java.io.File.separator + "fivesix_save.dat").delete();

            // Reset timer on total loss
            gp.speedrunTimer.reset();

            gp.gameState = GamePanel.loseState;
            return;
        }
        if (won)  { gp.completedFights++; gp.autoSave(); gp.audio.stopMusic(); gp.audio.playSFX("Win_Normal"); }
        if (!won) { gp.audio.stopMusic(); gp.audio.playSFX("Loss_Normal"); }
        gp.gameState = GamePanel.resultState;
    }

    // ── Leave battle mid-fight ────────────────────────────────────
    public void leaveBattle() {
        ctx.enemyHP = ctx.enemyMaxHP;
        ctx.battleRound = 1;
        ctx.battleMsg   = "";
        ctx.resetCombatState();
        gp.currentDialog = "";
        gp.dialogStage   = 0;
        gp.lastNPCColor  = 0;
        gp.gameState     = GamePanel.playState;
        gp.audio.stopMusic();
        gp.audio.playMusic(GamePanel.GLE_MAP.equals(gp.currentMapName)
                ? "gle_soundtrack" : "frontgate_soundtrack");
    }

    // ── Item / Ability use ────────────────────────────────────────
    public void useItem(ItemSystem.Item item) {
        boolean inBattle = gp.prevStateBeforePanel == GamePanel.battleState;

        if (item == ItemSystem.Item.GREENCROSS && !inBattle) {
            gp.currentDialog = "Greencross can only be used during battle.";
            gp.lastNPCColor  = 0;
            gp.gameState     = gp.prevStateBeforePanel;
            return;
        }
        if (isHealingItem(item) && item != ItemSystem.Item.GREENCROSS
                && gp.player != null && gp.player.currentHP >= gp.player.maxHP) {
            gp.currentDialog = "You are already at full HP!";
            gp.lastNPCColor  = 0;
            gp.gameState     = gp.prevStateBeforePanel;
            return;
        }

        gp.items.remove(item);
        switch (item) {
            case WATER         -> gp.player.currentHP = Math.min(gp.player.currentHP + gp.player.maxHP / 10, gp.player.maxHP);
            case BARNUTS       -> gp.player.currentHP = Math.min(gp.player.currentHP + 10, gp.player.maxHP);
            case GREENCROSS    -> { ctx.fxHealRounds = 3; ctx.fxHealAmt = 5;
                                    gp.player.currentHP = Math.max(1, gp.player.currentHP - 2); }
            case COFFEE        -> gp.player.maxHP += 10;
            case ENERGY_DRINK  -> gp.player.damageMultiplier += 0.05;
            case SLEEPING_MASK -> gp.player.currentHP = gp.player.maxHP;
        }
        gp.currentDialog = cap(gp.player.characterName) + " used " + item.displayName + ". " + item.description;
        gp.lastNPCColor  = 0;
        gp.gameState     = gp.prevStateBeforePanel;
        gp.autoSave();
    }

    public void useAbility(AbilitySystem.Ability ability) {
        if (gp.prevStateBeforePanel != GamePanel.battleState) return;
        gp.abilities.remove(ability);
        String effect = ability.description;
        switch (ability) {
            case CLAIRVOYANCE -> {
                ctx.clairMove = BattleSystem.getRandomEnemyMove();
                String mn = switch (ctx.clairMove) {
                    case ROCK -> "Rock"; case PAPER -> "Paper"; case SCISSORS -> "Scissors";
                };
                ctx.clairText    = ctx.enemyName + " will pick " + mn + " next turn.";
                effect           = ctx.clairText;
                ctx.clairVisible = true;
            }
            case UNO_REVERSE  -> ctx.fxUnoReverse  = true;
            case HYPNOTIZE    -> ctx.fxHypnotize    = true;
            case YOU_CHEATER  -> ctx.fxYouCheater   = true;
            case FULL_COUNTER -> ctx.fxFullCounter  = true;
        }
        gp.currentDialog = cap(gp.player.characterName) + " used " + ability.displayName + ". " + effect;
        gp.lastNPCColor  = 0;
        gp.gameState     = gp.prevStateBeforePanel;
        gp.autoSave();
    }

    // ── Active effects list (used by UIRenderer) ──────────────────
    public List<String> activeEffects() {
        List<String> out = new ArrayList<>();
        if (ctx.clairVisible && ctx.clairMove != null) {
            String mn = switch (ctx.clairMove) {
                case ROCK -> "Rock"; case PAPER -> "Paper"; case SCISSORS -> "Scissors";
            };
            out.add("Clairvoyance: " + mn);
        }
        if (ctx.fxUnoReverse)    out.add("Uno Reverse: ON");
        if (ctx.fxHypnotize)     out.add("Hypnotize: ON");
        if (ctx.fxYouCheater)    out.add("You Cheater: ON");
        if (ctx.fxFullCounter)   out.add("Full Counter: ON");
        if (ctx.fxHealRounds > 0) out.add("Ticking Heal: " + ctx.fxHealRounds + " rounds");
        return out;
    }

    // ── Private helpers ───────────────────────────────────────────
    private void loadOutcomeAndSwitch(BattleSystem.Move pm, BattleSystem.Move em) {
        gp.imageDisplay.loadOutcomeImage(pm.name().toLowerCase(), em.name().toLowerCase());
        gp.syncImageState();
        ctx.waitingOutcome = true;
        gp.audio.playSFX("Move_Sound");
        gp.gameState = GamePanel.outcomeState;
    }

    private int healAfterDefeat() {
        if (gp.player == null || gp.player.currentHP <= 0) return 0;
        gp.repairPlayerHpBounds();
        int before = gp.player.currentHP;
        int amount = Math.max(1, (int) Math.ceil(gp.player.maxHP * 0.20));
        gp.player.currentHP = Math.min(gp.player.maxHP, gp.player.currentHP + amount);
        return gp.player.currentHP - before;
    }

    private void grantRewards() {
        ctx.lastRewardItems.clear();
        ctx.lastRewardAbils.clear();
        if (!ctx.isFinalBoss) {
            int ic = gp.rand.nextInt(6) + 1;
            for (int i = 0; i < ic; i++) {
                ItemSystem.Item it = gp.items.addRandom(gp.rand);
                if (it != null) ctx.lastRewardItems.add(it.displayName);
            }
            int ac = gp.rand.nextDouble() < 0.3 ? gp.rand.nextInt(4) + 1 : 1;
            for (int i = 0; i < ac; i++) {
                AbilitySystem.Ability ab = gp.abilities.addRandom(gp.rand);
                if (ab != null) ctx.lastRewardAbils.add(ab.displayName);
            }
        }
        ctx.showRewardsBox = !ctx.isFinalBoss;
        gp.autoSave();
    }

    void saveFinalBossCheckpoint() {
        if (gp.player == null) return;
        gp.enemyStats = new EnemyStats();
        int[] beaten = {
            COLOR_JAMES, COLOR_ALIEYANDREW, COLOR_KYLE, COLOR_ADRIAN, COLOR_JOHNRU,
            COLOR_DARRYLL, COLOR_GIO, COLOR_YOHANN, COLOR_DIRK, COLOR_JAKE
        };
        for (int c : beaten) gp.enemyStats.markDefeated(c);
        gp.completedFights  = beaten.length;
        gp.currentMapName   = GamePanel.FRONTGATE_MAP;
        gp.pendingBattleEnemyColor = 0;
        ctx.battleResolved  = false;
        ctx.waitingOutcome  = false;
        gp.currentDialog    = "";
        gp.dialogStage      = 0;
        gp.lastNPCColor     = 0;
        gp.player.currentHP = Math.max(1, Math.min(gp.player.currentHP, gp.player.maxHP));
        gp.autoSave();
    }

    private boolean isHealingItem(ItemSystem.Item item) {
        return switch (item) {
            case WATER, BARNUTS, GREENCROSS, SLEEPING_MASK -> true;
            case COFFEE, ENERGY_DRINK -> false;
        };
    }

    static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
