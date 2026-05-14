package main;

import entity.BattleSystem;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure data holder for all in-flight battle state.
 * No logic — BattleManager owns the logic, GamePanel owns this object.
 */
public class BattleContext {

    public int    enemyHP              = 0;
    public int    enemyMaxHP           = 0;
    public double enemyDamageMultiplier = 1.0;
    public int    battleRound          = 1;
    public String battleMsg            = "";
    public boolean battleResolved      = false;
    public boolean waitingOutcome      = false;
    public String  enemyName           = "";
    public boolean isFinalBoss         = false;

    public BattleSystem.Move        lastPMove       = null;
    public BattleSystem.Move        lastEMove       = null;
    public BattleSystem.BattleResult lastBattleResult = null;
    public int lastPlayerDamage = 0;
    public int lastEnemyDamage  = 0;
    public int lastPlayerHeal   = 0;

    // Active ability effects
    public boolean fxUnoReverse  = false;
    public boolean fxHypnotize   = false;
    public boolean fxYouCheater  = false;
    public boolean fxFullCounter = false;
    public int     fxHealRounds  = 0;
    public int     fxHealAmt     = 0;

    // Clairvoyance
    public boolean            clairVisible = false;
    public String             clairText    = "";
    public BattleSystem.Move  clairMove    = null;

    // Rewards shown after a win
    public final List<String> lastRewardItems = new ArrayList<>();
    public final List<String> lastRewardAbils = new ArrayList<>();
    public boolean showRewardsBox = false;

    /** Reset per-round transient state (called at the start of each new battle). */
    public void reset() {
        enemyHP = 0; enemyMaxHP = 0; enemyDamageMultiplier = 1.0;
        battleRound = 1; battleMsg = ""; battleResolved = false; waitingOutcome = false;
        isFinalBoss = false; enemyName = ""; lastPMove = null; lastEMove = null;
        lastBattleResult = null; lastPlayerDamage = 0; lastEnemyDamage = 0; lastPlayerHeal = 0;
        fxUnoReverse = false; fxHypnotize = false; fxYouCheater = false;
        fxFullCounter = false; fxHealRounds = 0; fxHealAmt = 0;
        clairVisible = false; clairText = ""; clairMove = null;
        lastRewardItems.clear(); lastRewardAbils.clear(); showRewardsBox = false;
    }

    /** Reset only the per-round combat trackers when leaving battle early. */
    public void resetCombatState() {
        battleResolved = false; waitingOutcome = false; lastBattleResult = null;
        lastPlayerDamage = 0; lastEnemyDamage = 0; lastPlayerHeal = 0;
        fxUnoReverse = false; fxHypnotize = false; fxYouCheater = false;
        fxFullCounter = false; clairVisible = false; clairMove = null;
    }
}
