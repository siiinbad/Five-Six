package main;

import entity.AbilitySystem;
import entity.BattleSystem;
import entity.ItemSystem;
import java.awt.*;
import static main.HitboxColors.Ui.*;

/**
 * Handles all mouse click and hover logic extracted from GamePanel.
 * Reads/writes GamePanel state through a direct reference (same package).
 */
public class InputRouter {

    private final GamePanel  gp;
    private final UIRenderer renderer;

    public InputRouter(GamePanel gp, UIRenderer renderer) {
        this.gp       = gp;
        this.renderer = renderer;
    }

    // ── Main entry points ─────────────────────────────────────────
    public void onClick(Point p) {
        gp.audio.playSFX("click");

        if (gp.quitConfirmOpen) { quitConfirmClick(p); return; }
        if (gp.settingsOpen)    { settingsClick(p);     return; }

        if ((gp.gameState == GamePanel.menuState || gp.gameState == GamePanel.menuStartState)
                && gp.menuLeaderboardBtnRect().contains(p)) {
            gp.leaderboardOpen = !gp.leaderboardOpen;
            return;
        }

        if ((gp.gameState == GamePanel.menuState || gp.gameState == GamePanel.menuCharState)
                && gp.fixedMenuSettingsRect().contains(p)) {
            gp.settingsOpen = true; return;
        }
        if (gp.bottomNavVisible() && gp.bottomNavRect().contains(p)) {
            bottomNavClick(); return;
        }
        if (gp.gameState == GamePanel.inventoryState) { handleItemClick(p); return; }
        if (gp.gameState == GamePanel.abilityState)   { handleAbilityClick(p); return; }

        switch (gp.gameState) {
            case GamePanel.menuState      -> menuClick(colorAt(gp.menuMainHitbox,  p));
            case GamePanel.menuStartState -> menuStartClick(colorAt(gp.menuStartHitbox, p));
            case GamePanel.menuCharState  -> charClick(colorAt(gp.menuCharHitbox,  p), p);
            case GamePanel.playState      -> worldClick(p);
            case GamePanel.fadeState      -> { /* ignore */ }
            case GamePanel.battleState    -> battleClick(colorAt(gp.battleHitbox, p), p);
            case GamePanel.outcomeState   -> { if (colorAt(gp.outcomeHitbox, p) == BC_CONTBAT) gp.battleMgr.nextRound(); }
            case GamePanel.creditsState   -> gp.gameState = GamePanel.menuState;
            case GamePanel.winState       -> gp.completeReset();
            case GamePanel.loseState      -> gp.resetToMenu();
            case GamePanel.resultState    -> resultClick(p);
        }
    }

    public void refreshHover() {
        String prev = gp.hoveredBtn;
        gp.hoveredBtn      = null;
        gp.hoveredCharColor = 0;

        if (gp.settingsOpen && gp.settingsMuteRect().contains(gp.mouse)) {
            gp.hoveredBtn = gp.audio.isMuted ? "wmuted" : "wmute";
        } else if (gp.bottomNavVisible() && gp.bottomNavRect().contains(gp.mouse)) {
            gp.hoveredBtn = "bottomnav";
        } else if ((gp.gameState == GamePanel.menuState || gp.gameState == GamePanel.menuCharState)
                && gp.fixedMenuSettingsRect().contains(gp.mouse)) {
            gp.hoveredBtn = "settings";
        } else if ((gp.gameState == GamePanel.menuState || gp.gameState == GamePanel.menuStartState)
                && gp.menuLeaderboardBtnRect().contains(gp.mouse)) {
            gp.hoveredBtn = "leaderboard";
        } else if (gp.gameState == GamePanel.menuCharState) {
            String ch = gp.charButtonAt(gp.mouse);
            if (ch != null) {
                gp.hoveredBtn       = ch;
                gp.hoveredCharColor = HitboxColors.charColor(ch);
            }
        } else if (gp.gameState == GamePanel.playState
                || gp.gameState == GamePanel.inventoryState
                || gp.gameState == GamePanel.abilityState) {
            gp.hoveredBtn = worldButtonAt(gp.mouse);
        } else {
            int c = switch (gp.gameState) {
                case GamePanel.menuState      -> colorAt(gp.menuMainHitbox,  gp.mouse);
                case GamePanel.menuStartState -> colorAt(gp.menuStartHitbox, gp.mouse);
                case GamePanel.menuCharState  -> colorAt(gp.menuCharHitbox,  gp.mouse);
                case GamePanel.battleState    -> colorAt(gp.battleHitbox,    gp.mouse);
                case GamePanel.outcomeState   -> colorAt(gp.outcomeHitbox,   gp.mouse);
                default -> 0;
            };
            if (gp.gameState == GamePanel.menuCharState && c == BC_HOVCHAR)
                gp.hoveredCharColor = nearestCharColor(gp.mouse);
            gp.hoveredBtn = c2k(c);
        }

        if (gp.hoveredBtn != null && !gp.hoveredBtn.equals(prev))
            gp.audio.playSFX("hover");
    }

    public void handleDrag(Point p) {
        if (!gp.settingsOpen) return;
        Rectangle mt = gp.musicSliderTrack();
        if (mt.contains(p) || (p.y >= mt.y && p.y <= mt.y + 16)) {
            if (p.x >= mt.x && p.x <= mt.x + mt.width) {
                gp.audio.musicVolume = sliderValue(mt, p);
                gp.audio.applyMusicVolume();
            }
        }
        Rectangle st = gp.sfxSliderTrack();
        if (st.contains(p) || (p.y >= st.y && p.y <= st.y + 16)) {
            if (p.x >= st.x && p.x <= st.x + st.width)
                gp.audio.sfxVolume = sliderValue(st, p);
        }
    }

    // ── State-specific click handlers ─────────────────────────────
    private void menuClick(int c) {
        switch (c) {
            case BC_START  -> gp.gameState = GamePanel.menuStartState;
            case BC_CREDIT -> gp.gameState = GamePanel.creditsState;
            case BC_QUIT   -> gp.requestQuit(false);
        }
    }

    private void menuStartClick(int c) {
        switch (c) {
            case BC_CONTINUE -> { if (gp.saveData != null) gp.loadSave(); }
            case BC_SELCHAR  -> gp.gameState = GamePanel.menuCharState;
            case BC_SETTINGS -> gp.settingsOpen = !gp.settingsOpen;
        }
    }

    private void charClick(int c, Point p) {
        String name = gp.charButtonAt(p);
        if (name == null) {
            name = switch (c) {
                case BC_IVAN     -> "ivan";
                case BC_SAM      -> "sam";
                case BC_NIMUEL   -> "nimuel";
                case BC_JOHNFIEL -> "johnfiel";
                case BC_HOVCHAR  -> HitboxColors.charName(nearestCharColor(p));
                default -> null;
            };
        }
        if (name != null) gp.selectChar(name);
    }

    private void worldClick(Point p) {
        String key = worldButtonAt(p);
        if (key == null) return;
        switch (key) {
            case "item_inv" -> { gp.prevStateBeforePanel = GamePanel.playState; toggleState(GamePanel.inventoryState); }
            case "abil_inv" -> { gp.prevStateBeforePanel = GamePanel.playState; toggleState(GamePanel.abilityState); }
            case "backmenu" -> gp.requestQuit(true);
            case "wsave"    -> gp.autoSave();
            case "wset"     -> gp.settingsOpen = true;
        }
    }

    private void battleClick(int c, Point p) {
        if (gp.gameState == GamePanel.inventoryState || gp.gameState == GamePanel.abilityState) return;
        if (!gp.currentDialog.isEmpty()) { gp.currentDialog = ""; return; }
        if (!gp.battleMgr.ctx.waitingOutcome) {
            switch (c) {
                case BC_ROCKBTN  -> gp.battleMgr.resolve(BattleSystem.Move.ROCK);
                case BC_PAPERBTN -> gp.battleMgr.resolve(BattleSystem.Move.PAPER);
                case BC_SCISSBTN -> gp.battleMgr.resolve(BattleSystem.Move.SCISSORS);
                case BC_USEITEM  -> { if (!gp.items.isEmpty()) { gp.prevStateBeforePanel = GamePanel.battleState; gp.gameState = GamePanel.inventoryState; } }
                case BC_USEABIL  -> { if (!gp.abilities.isEmpty()) { gp.prevStateBeforePanel = GamePanel.battleState; gp.gameState = GamePanel.abilityState; } }
            }
        }
    }

    private void settingsClick(Point p) {
        if (gp.settingsMuteRect().contains(p)) {
            gp.audio.toggleMute();
        } else if (gp.musicSliderTrack().contains(p)) {
            gp.audio.musicVolume = sliderValue(gp.musicSliderTrack(), p);
            gp.audio.applyMusicVolume();
        } else if (gp.sfxSliderTrack().contains(p)) {
            gp.audio.sfxVolume = sliderValue(gp.sfxSliderTrack(), p);
        } else if (!gp.settingsPanelRect().contains(p)) {
            gp.settingsOpen = false;
        }
    }

    private void quitConfirmClick(Point p) {
        if (gp.quitYesRect().contains(p)) {
            if (gp.quitConfirmToMenu) {
                gp.autoSave();
                gp.gameState = GamePanel.menuState;
                gp.audio.stopMusic();
                gp.audio.playMusic("menu_sountrack");
                gp.currentDialog = "";
            } else {
                System.exit(0);
            }
            gp.quitConfirmOpen = false;
        } else if (gp.quitNoRect().contains(p) || !gp.quitConfirmRect().contains(p)) {
            gp.quitConfirmOpen = false;
        }
    }

    private void resultClick(Point p) {
        if (!gp.battleMgr.ctx.battleResolved) return;
        boolean wasDefeated = gp.player.currentHP <= 0;
        if (wasDefeated) gp.player.respawnWithPenalty();
        if (gp.pendingBattleEnemyColor == HitboxColors.Map.COLOR_VAUGHN && !wasDefeated) {
            gp.startNarration(); return;
        }
        gp.gameState     = GamePanel.playState;
        gp.currentDialog = ""; gp.dialogStage = 0; gp.lastNPCColor = 0;
        gp.battleMgr.ctx.battleResolved  = false;
        gp.battleMgr.ctx.waitingOutcome  = false;
        gp.audio.playMusic(GamePanel.GLE_MAP.equals(gp.currentMapName)
                ? "gle_soundtrack" : "frontgate_soundtrack");
        gp.autoSave();
    }

    private void handleItemClick(Point p) {
        int pw = 520, ph = 420, px = gp.getWidth() / 2 - pw / 2, py = gp.getHeight() / 2 - ph / 2;
        int iy = py + 88;
        for (ItemSystem.Item item : gp.items.getItems()) {
            if (new Rectangle(px + 20, iy - 28, pw - 40, 38).contains(p)) {
                gp.battleMgr.useItem(item); return;
            }
            iy += 48;
            if (iy > py + ph - 84) break;
        }
        gp.gameState = gp.prevStateBeforePanel;
    }

    private void handleAbilityClick(Point p) {
        int pw = 560, ph = 440, px = gp.getWidth() / 2 - pw / 2, py = gp.getHeight() / 2 - ph / 2;
        int ay = py + 88;
        for (AbilitySystem.Ability ab : gp.abilities.getUnique()) {
            if (new Rectangle(px + 16, ay - 26, pw - 32, 36).contains(p)) {
                gp.battleMgr.useAbility(ab); return;
            }
            ay += 46;
            if (ay > py + ph - 86) break;
        }
        gp.gameState = gp.prevStateBeforePanel;
    }

    private void bottomNavClick() {
        switch (gp.gameState) {
            case GamePanel.menuStartState -> gp.gameState = GamePanel.menuState;
            case GamePanel.menuCharState  -> gp.gameState = GamePanel.menuStartState;
            case GamePanel.battleState, GamePanel.outcomeState, GamePanel.preBattleState -> gp.battleMgr.leaveBattle();
        }
    }

    // ── Hover helpers ─────────────────────────────────────────────
    private String worldButtonAt(Point p) {
        Rectangle settingsR = gp.settingsWorldRect();
        if (settingsR != null && settingsR.contains(p)) return "wset";
        int c = colorAt(gp.worldGuiHitbox, p);
        return switch (c) {
            case BC_ITEMS   -> "item_inv";
            case BC_ABILINV -> "abil_inv";
            case BC_BACK    -> "backmenu";
            case BC_SAVE    -> "wsave";
            default -> null;
        };
    }

    private String c2k(int c) {
        return switch (c) {
            case BC_START    -> "start";
            case BC_CREDIT   -> "credits";
            case BC_QUIT     -> "quit";
            case BC_SETTINGS -> "settings";
            case BC_MUTE     -> (gp.audio.isMuted ? "wmuted" : "wmute");
            case BC_SAVE     -> "save";
            case BC_CONTINUE -> "continue";
            case BC_SELCHAR  -> "selchar";
            case BC_IVAN     -> "ivan";
            case BC_SAM      -> "sam";
            case BC_NIMUEL   -> "nimuel";
            case BC_JOHNFIEL -> "johnfiel";
            case BC_HOVCHAR  -> "ivan";
            case BC_ITEMS    -> "item_inv";
            case BC_ABILINV  -> "abil_inv";
            case BC_BACK     -> "backmenu";
            case BC_ROCKBTN  -> "rock";
            case BC_PAPERBTN -> "paper";
            case BC_SCISSBTN -> "scissors";
            case BC_CONTBAT  -> "contbat";
            case BC_USEITEM  -> "useitem";
            case BC_USEABIL  -> "useabil";
            default -> null;
        };
    }

    private int nearestCharColor(Point p) {
        if (gp.menuCharHitbox == null) return 0;
        int cx = p.x * gp.menuCharHitbox.getWidth()  / Math.max(1, gp.getWidth());
        int cy = p.y * gp.menuCharHitbox.getHeight() / Math.max(1, gp.getHeight());
        int[] ccs = HitboxColors.characterButtonColors();
        for (int r = 0; r <= 10; r++) {
            for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) {
                if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                int px = cx + dx, py = cy + dy;
                if (px < 0 || px >= gp.menuCharHitbox.getWidth()
                        || py < 0 || py >= gp.menuCharHitbox.getHeight()) continue;
                int col = gp.menuCharHitbox.getRGB(px, py) & 0xFFFFFF;
                for (int cc : ccs) if (col == cc) return cc;
            }
        }
        return 0;
    }

    int colorAt(java.awt.image.BufferedImage hb, Point p) {
        if (hb == null) return 0;
        int ix = p.x * hb.getWidth()  / Math.max(1, gp.getWidth());
        int iy = p.y * hb.getHeight() / Math.max(1, gp.getHeight());
        if (ix < 0 || ix >= hb.getWidth() || iy < 0 || iy >= hb.getHeight()) return 0;
        return hb.getRGB(ix, iy) & 0xFFFFFF;
    }

    private float sliderValue(Rectangle track, Point p) {
        return Math.max(0f, Math.min(1f, (float)(p.x - track.x) / track.width));
    }

    private void toggleState(int s) {
        gp.gameState = (gp.gameState == s) ? GamePanel.playState : s;
    }
}
