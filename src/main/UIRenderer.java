package main;

import entity.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import static main.HitboxColors.Map.*;
import static main.HitboxColors.Ui.*;

/**
 * Owns all rendering logic extracted from GamePanel.
 * Reads state through the GamePanel reference (same package).
 * GamePanel.paintComponent() delegates here.
 */
public class UIRenderer {

    private final GamePanel gp;

    // Bounds-lookup cache — invalidated when screen size changes
    private final Map<Long, Rectangle> boundsCache = new HashMap<>();
    private int cachedW = 0, cachedH = 0;

    // NPC sprite-position cache — rebuilt on each map load
    private final Map<Integer, int[]> npcBoundsCache = new HashMap<>();
    private String npcBoundsCacheMap = "";

    public UIRenderer(GamePanel gp) {
        this.gp = gp;
    }

    /** Call this whenever a new map is loaded so NPCs are never drawn from stale hitbox data. */
    public void clearNpcCache() {
        npcBoundsCache.clear();
        npcBoundsCacheMap = "";
    }

    // ── Entry point ───────────────────────────────────────────────
    public void render(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        switch (gp.gameState) {
            case GamePanel.menuState      -> paintMenu(g2);
            case GamePanel.menuStartState -> paintMenuStart(g2);
            case GamePanel.menuCharState  -> paintCharSelect(g2);
            case GamePanel.playState      -> paintPlay(g2);
            case GamePanel.fadeState      -> paintFade(g2);
            case GamePanel.battleState    -> paintBattle(g2);
            case GamePanel.outcomeState   -> paintOutcome(g2);
            case GamePanel.creditsState   -> paintCredits(g2);
            case GamePanel.inventoryState -> paintInventory(g2);
            case GamePanel.abilityState   -> paintAbility(g2);
            case GamePanel.winState       -> paintWin(g2);
            case GamePanel.loseState      -> paintLose(g2);
            case GamePanel.preBattleState -> paintPreBattle(g2);
            case GamePanel.narrationState -> paintNarration(g2);
            case GamePanel.resultState    -> paintResult(g2);
            case GamePanel.loadingState   -> paintLoading(g2);
        }

        if (gp.bottomNavVisible()) drawBottomNav(g2);
        if (gp.quitConfirmOpen)    paintQuitConfirm(g2);
    }

    // ─────────────────────────────────────────────────────────────
    //  MENU SCREENS
    // ─────────────────────────────────────────────────────────────
    private void paintLoading(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        if (gp.loadingScreenGif != null) {
            g2.drawImage(gp.loadingScreenGif, 0, 0, gp.getWidth(), gp.getHeight(), gp);
            return;
        }
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        String text = "LOADING...";
        g2.drawString(text, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(text) / 2, gp.getHeight() / 2);
    }
    private void paintMenu(Graphics2D g2) {
        fill(g2, gp.menuScreenImg);
        if (gp.logoImg != null) {
            Rectangle lr = bounds(gp.menuMainHitbox, BC_LOGO);
            if (lr != null) g2.drawImage(gp.logoImg, lr.x, lr.y, lr.width, lr.height, null);
        }
        drawBtn(g2, gp.menuMainHitbox, BC_START,  "start");
        drawBtn(g2, gp.menuMainHitbox, BC_CREDIT, "credits");
        drawBtn(g2, gp.menuMainHitbox, BC_QUIT,   "quit");
        drawMenuSettingsButton(g2);
        if (gp.settingsOpen) paintSettingsPanel(g2);
    }

    private void paintMenuStart(Graphics2D g2) {
        fill(g2, gp.menuScreenImg);
        drawLogo(g2, gp.menuStartHitbox);
        drawBtn(g2, gp.menuStartHitbox, BC_CONTINUE, "continue");
        drawBtn(g2, gp.menuStartHitbox, BC_SELCHAR,  "selchar");
        drawBtn(g2, gp.menuStartHitbox, BC_SETTINGS, "settings");
        if (gp.settingsOpen) paintSettingsPanel(g2);
    }

    private void paintCharSelect(Graphics2D g2) {
        fill(g2, gp.menuScreenImg);
        drawLogo(g2, gp.menuCharHitbox);

        Dimension btnSize = characterButtonDrawSize();
        for (String name : List.of("ivan", "sam", "nimuel", "johnfiel")) {
            BufferedImage[] arr = gp.btnImgs.get(name);
            Rectangle r = charButtonDrawRect(name, btnSize);
            if (arr == null || r == null) continue;
            BufferedImage img = name.equals(gp.hoveredBtn) && arr[1] != null ? arr[1] : arr[0];
            if (img != null) drawImageFit(g2, img, r);
        }

        String hcn = HitboxColors.charName(gp.hoveredCharColor);
        if (hcn == null && gp.hoveredBtn != null && HitboxColors.charColor(gp.hoveredBtn) != 0)
            hcn = gp.hoveredBtn;

        if (hcn != null) {
            BufferedImage prev = gp.charSelectImg.get(hcn);
            Rectangle preview  = gp.charPreviewRect();
            if (prev != null) {
                double ar = prev.getWidth() / (double) Math.max(1, prev.getHeight());
                int th = preview.height;
                int tw = Math.max(1, (int) Math.round(th * ar));
                if (tw > preview.width) { tw = preview.width; th = Math.max(1, (int) Math.round(tw / ar)); }
                int x = preview.x + (preview.width  - tw) / 2;
                int y = preview.y + (preview.height - th) / 2;
                g2.drawImage(prev, x, y, tw, th, null);
            }
        }
    }

    private void paintCredits(Graphics2D g2) {
        if (gp.creditsImg != null) { fill(g2, gp.creditsImg); return; }
        drawPlaceholderScreen(g2, "CREDITS PLACEHOLDER");
    }

    // ─────────────────────────────────────────────────────────────
    //  PLAY STATE
    // ─────────────────────────────────────────────────────────────
    private void paintPlay(Graphics2D g2) {
        if (gp.mapImage != null) {
            fill(g2, gp.mapImage);
        } else if (GamePanel.EMALL_MAP.equals(gp.currentMapName)) {
            BufferedImage emallBg = gp.imageDisplay.getEmallBattleBackground();
            if (emallBg != null) {
                fill(g2, emallBg);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
            } else {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
            }
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        }

        if (gp.showDebug && gp.hitboxImage != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            fill(g2, gp.hitboxImage);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        paintNPCs(g2);
        if (gp.player != null) gp.player.draw(g2);
        paintWorldGUI(g2);
        if (!gp.currentDialog.isEmpty()) dialog(g2, gp.currentDialog);
        if (gp.settingsOpen) paintSettingsPanel(g2);
    }

    private void paintWorldGUI(Graphics2D g2) {
        if (gp.player != null)
            hpBar(g2, 20, 20, 220, 22, gp.player.currentHP, gp.player.maxHP, gp.player.characterName);
        if (gp.worldGuiHitbox == null) return;
        drawBtn(g2, gp.worldGuiHitbox, BC_ITEMS,   "item_inv");
        drawBtn(g2, gp.worldGuiHitbox, BC_ABILINV, "abil_inv");
        drawBtn(g2, gp.worldGuiHitbox, BC_BACK,    "backmenu");
        drawBtn(g2, gp.worldGuiHitbox, BC_SAVE,    "wsave");
        drawWorldSettingsButton(g2);
        drawMapTitle(g2);
    }

    private void paintNPCs(Graphics2D g2) {
        if (gp.hitboxImage == null) return;
        if (!gp.currentMapName.equals(npcBoundsCacheMap)) {
            npcBoundsCache.clear();
            npcBoundsCacheMap = gp.currentMapName;
            Set<Integer> npcColors = HitboxColors.mapNpcColors(gp.currentMapName);
            for (int py = 0; py < gp.hitboxImage.getHeight(); py++)
                for (int px = 0; px < gp.hitboxImage.getWidth(); px++) {
                    int c = gp.hitboxImage.getRGB(px, py) & 0xFFFFFF;
                    if (!npcColors.contains(c)) continue;
                    final int fpx = px, fpy = py;
                    int[] b = npcBoundsCache.computeIfAbsent(c, k -> new int[]{fpx, fpx, fpy, fpy});
                    if (px < b[0]) b[0] = px; if (px > b[1]) b[1] = px;
                    if (py < b[2]) b[2] = py; if (py > b[3]) b[3] = py;
                }
        }
        for (Map.Entry<Integer, int[]> e : npcBoundsCache.entrySet()) {
            int c = e.getKey(); int[] b = e.getValue();
            if (gp.enemyStats.isDefeated(c)) continue;
            if (c == COLOR_BROKENDOOR) continue;
            int sx = ((b[0] + b[1]) / 2) * gp.getWidth()  / gp.hitboxImage.getWidth();
            int sy = ((b[2] + b[3]) / 2) * gp.getHeight() / gp.hitboxImage.getHeight();
            String f = HitboxColors.enemyFolder(c);
            if (f == null) continue;
            BufferedImage nim = gp.npcStand.get(f);
            if (nim != null) {
                int dx = sx - gp.tileSize / 2, dy = sy - gp.tileSize / 2;
                g2.setColor(new Color(0, 0, 0, 100));
                int sw = (int)(gp.tileSize * .6), sh = (int)(gp.tileSize * .15);
                g2.fillOval(dx + (gp.tileSize - sw) / 2, dy + gp.tileSize - sh - 15, sw, sh);
                g2.drawImage(nim, dx, dy, gp.tileSize, gp.tileSize, null);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FADE STATE
    // ─────────────────────────────────────────────────────────────
    private void paintFade(Graphics2D g2) {
        fill(g2, gp.mapImage);
        paintNPCs(g2);
        if (gp.player != null) gp.player.draw(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, gp.fadeAlpha));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // ─────────────────────────────────────────────────────────────
    //  BATTLE STATE
    // ─────────────────────────────────────────────────────────────
    private void paintBattle(Graphics2D g2) {
        BattleContext bc = gp.battleMgr.ctx;
        fill(g2, gp.battleSceneImg);

        if (gp.battleSpriteHitbox != null) {
            if (gp.playerBattleImg != null) {
                Rectangle r = bounds(gp.battleSpriteHitbox, BC_PLRBAT);
                if (r != null) g2.drawImage(gp.playerBattleImg, r.x, r.y, r.width, r.height, null);
            }
            if (gp.enemyBattleImg != null) {
                Rectangle r = bounds(gp.battleSpriteHitbox, BC_ENMBAT);
                if (r != null) g2.drawImage(gp.enemyBattleImg, r.x, r.y, r.width, r.height, null);
            }
        }

        if (gp.player != null) {
            hpBar(g2, 40, 30, 300, 22, gp.player.currentHP, gp.player.maxHP, gp.player.characterName);
            damageMultiplierBox(g2, 40, 84, 300, gp.player.damageMultiplier);
        }
        drawBattleHeader(g2);
        int ehx = gp.getWidth() - 360;
        hpBar(g2, ehx, 30, 300, 22, bc.enemyHP, bc.enemyMaxHP, bc.enemyName);
        damageMultiplierBox(g2, ehx, 84, 300, bc.enemyDamageMultiplier);

        if (!bc.waitingOutcome) {
            Dimension ms = battleMoveButtonSize();
            drawBattleMoveBtn(g2, BC_ROCKBTN,  "rock",     ms);
            drawBattleMoveBtn(g2, BC_PAPERBTN, "paper",    ms);
            drawBattleMoveBtn(g2, BC_SCISSBTN, "scissors", ms);
            drawBtn(g2, gp.battleHitbox, BC_USEITEM, "useitem");
            drawBtn(g2, gp.battleHitbox, BC_USEABIL, "useabil");
        }

        drawActiveEffects(g2);
        if (!gp.currentDialog.isEmpty()) dialog(g2, gp.currentDialog);
    }

    private void paintOutcome(Graphics2D g2) {
        fill(g2, gp.outcomeSceneImg);
        if (gp.outcomeRPSImg != null && gp.outcomeHitbox != null) {
            Rectangle r = bounds(gp.outcomeHitbox, BC_OUTCZONE);
            if (r != null) g2.drawImage(gp.outcomeRPSImg, r.x, r.y, r.width, r.height, null);
        }
        drawBtn(g2, gp.outcomeHitbox, BC_CONTBAT, "contbat");
        drawOutcomePanel(g2);
    }

    // ─────────────────────────────────────────────────────────────
    //  INVENTORY / ABILITY PANELS
    // ─────────────────────────────────────────────────────────────
    private void paintInventory(Graphics2D g2) {
        if (gp.prevStateBeforePanel == GamePanel.battleState) {
            paintBattle(g2);
        } else {
            paintPlay(g2);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        }
        int pw = 520, ph = 420, px = gp.getWidth() / 2 - pw / 2, py = gp.getHeight() / 2 - ph / 2;
        panelHeader(g2, px, py, pw, ph, "ITEMS");

        List<ItemSystem.Item> list = gp.items.getItems();
        int iy = py + 88;
        g2.setFont(pixelFont(Font.BOLD, 20));
        String hoverText = null;
        for (ItemSystem.Item item : list) {
            int cnt = gp.items.count(item);
            boolean h = new Rectangle(px + 20, iy - 28, pw - 40, 38).contains(gp.mouse);
            if (h) hoverText = item.displayName + ": " + item.description;
            g2.setColor(h ? popupRowHover() : popupRow());
            g2.fillRoundRect(px + 20, iy - 28, pw - 40, 38, 8, 8);
            g2.setColor(new Color(52, 35, 24));
            String label = item.displayName + "  x" + cnt;
            g2.drawString(label, px + pw / 2 - g2.getFontMetrics().stringWidth(label) / 2, iy);
            iy += 48;
            if (iy > py + ph - 84) break;
        }
        if (list.isEmpty()) {
            g2.setColor(new Color(80, 56, 38));
            g2.setFont(pixelFont(Font.ITALIC, 22));
            g2.drawString("No items", px + pw / 2 - 44, py + ph / 2);
        }
        drawPanelContinueHint(g2, px, py, pw, ph);
        drawHoverText(g2, hoverText);
    }

    private void paintAbility(Graphics2D g2) {
        if (gp.prevStateBeforePanel == GamePanel.battleState) {
            paintBattle(g2);
        } else {
            paintPlay(g2);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        }
        boolean inBattle = gp.prevStateBeforePanel == GamePanel.battleState;
        int pw = 560, ph = 440, px = gp.getWidth() / 2 - pw / 2, py = gp.getHeight() / 2 - ph / 2;
        drawPopupBox(g2, px, py, pw, ph);

        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 28));
        String title = "ABILITIES";
        g2.drawString(title, px + pw / 2 - g2.getFontMetrics().stringWidth(title) / 2, py + 42);
        g2.setColor(new Color(112, 83, 54, 120));
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(px + 20, py + 54, px + pw - 20, py + 54);

        List<AbilitySystem.Ability> unique = gp.abilities.getUnique();
        int ay = py + 88;
        g2.setFont(pixelFont(Font.BOLD, 18));
        String hoverText = null;
        for (AbilitySystem.Ability ab : unique) {
            int cnt = gp.abilities.count(ab);
            boolean hovered = inBattle && new Rectangle(px + 16, ay - 26, pw - 32, 36).contains(gp.mouse);
            if (hovered) hoverText = ab.displayName + ": " + ab.description;
            g2.setColor(hovered ? popupRowHover() : popupRow());
            g2.fillRoundRect(px + 16, ay - 26, pw - 32, 36, 8, 8);
            g2.setColor(inBattle ? new Color(52, 35, 24) : new Color(96, 76, 58));
            String label = ab.displayName + "  x" + cnt;
            g2.drawString(label, px + pw / 2 - g2.getFontMetrics().stringWidth(label) / 2, ay);
            ay += 46;
            if (ay > py + ph - 86) break;
        }
        if (unique.isEmpty()) {
            g2.setColor(new Color(80, 56, 38));
            g2.setFont(pixelFont(Font.ITALIC, 20));
            g2.drawString("No abilities", px + pw / 2 - 55, py + ph / 2);
        }
        
        if (!inBattle) {
            g2.setColor(new Color(244, 210, 154));
            g2.setFont(pixelFont(Font.BOLD, 18));
            String msg = "You can't use abilities outside of a battle";
            g2.drawString(msg, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(msg) / 2, py - 15);
        }
        
        drawPanelContinueHint(g2, px, py, pw, ph);
        drawHoverText(g2, hoverText);
    }

    // ─────────────────────────────────────────────────────────────
    //  WIN / LOSE / CREDITS
    // ─────────────────────────────────────────────────────────────
    private void paintWin(Graphics2D g2) {
        if (gp.winImg != null) { fill(g2, gp.winImg); }
        else { g2.setColor(new Color(198, 158, 104)); g2.fillRect(0, 0, gp.getWidth(), gp.getHeight()); }

        int bw = Math.min(gp.getWidth() - 160, 820), bh = 230;
        int bx = gp.getWidth() / 2 - bw / 2, by = gp.getHeight() - bh - 54;
        drawPopupBox(g2, bx, by, bw, bh);

        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 34));
        String title = "CONGRATULATIONS!";
        g2.drawString(title, bx + bw / 2 - g2.getFontMetrics().stringWidth(title) / 2, by + 48);
        g2.setColor(new Color(112, 83, 54, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(bx + 24, by + 62, bx + bw - 24, by + 62);

        g2.setColor(new Color(80, 56, 38));
        g2.setFont(pixelFont(Font.BOLD, 18));
        String msg = "After collecting all your money back and defeating the mysterious person "
                   + "you finally get to enjoy your long awaited Jolibee meal!";
        int msgY = by + 98;
        for (String line : wrap(g2, msg, bw - 56)) { g2.drawString(line, bx + 28, msgY); msgY += 28; }

        g2.setFont(pixelFont(Font.ITALIC, 15));
        String prompt = "Click anywhere to return to the main menu";
        g2.drawString(prompt, bx + bw / 2 - g2.getFontMetrics().stringWidth(prompt) / 2, by + bh - 28);
    }

    private void paintLose(Graphics2D g2) {
        g2.setColor(new Color(20, 0, 0)); g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        g2.setColor(new Color(180, 0, 0, 60)); g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, gp.getHeight() / 2 - 120, gp.getWidth(), 280);

        g2.setFont(new Font("Arial", Font.BOLD, 52));
        g2.setColor(new Color(220, 30, 30));
        String title = "GAME OVER";
        g2.drawString(title, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(title) / 2, gp.getHeight() / 2 - 50);

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        String msg = "The beggar took your money. You never made it to Jollibee.";
        for (String ln : wrap(g2, msg, gp.getWidth() - 100))
            g2.drawString(ln, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(ln) / 2, gp.getHeight() / 2 + 10);

        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(new Color(200, 200, 200));
        String sub = "Your progress has been erased.";
        g2.drawString(sub, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(sub) / 2, gp.getHeight() / 2 + 55);

        g2.setFont(new Font("Arial", Font.ITALIC, 18));
        g2.setColor(new Color(160, 160, 160));
        String back = "Click to return to the main menu and start over";
        g2.drawString(back, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(back) / 2, gp.getHeight() - 55);
    }

    // ─────────────────────────────────────────────────────────────
    //  RESULT STATE (post-battle win/loss box)
    // ─────────────────────────────────────────────────────────────
    private void paintResult(Graphics2D g2) {
        if (gp.mapImage != null) fill(g2, gp.mapImage);
        else { g2.setColor(Color.BLACK); g2.fillRect(0, 0, gp.getWidth(), gp.getHeight()); }

        BattleContext bc = gp.battleMgr.ctx;
        if (bc.battleResolved && gp.player != null && gp.player.currentHP > 0) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());

            int rowCount = Math.max(1, bc.lastRewardItems.size()) + bc.lastRewardAbils.size();
            int bw = 440, bh = 80 + 30 + rowCount * 30 + 70;
            int bx = gp.getWidth() / 2 - bw / 2, by = gp.getHeight() / 2 - bh / 2;
            drawPopupBox(g2, bx, by, bw, bh);

            g2.setFont(new Font("Arial", Font.BOLD, 46));
            FontMetrics fmw = g2.getFontMetrics();
            String win = "YOU WIN!";
            g2.setColor(new Color(0, 0, 0, 180));
            g2.drawString(win, bx + bw / 2 - fmw.stringWidth(win) / 2 + 3, by + 56 + 3);
            g2.setColor(new Color(52, 35, 24));
            g2.drawString(win, bx + bw / 2 - fmw.stringWidth(win) / 2, by + 56);

            g2.setColor(new Color(112, 83, 54, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(bx + 20, by + 68, bx + bw - 20, by + 68);

            g2.setFont(pixelFont(Font.BOLD, 15));
            g2.setColor(new Color(80, 56, 38));
            g2.drawString("Rewards earned:", bx + 26, by + 90);

            int ry = by + 116;
            g2.setFont(pixelFont(Font.BOLD, 16));
            LinkedHashMap<String, Integer> itemCounts = new LinkedHashMap<>();
            for (String n : bc.lastRewardItems) itemCounts.merge(n, 1, Integer::sum);

            if (itemCounts.isEmpty()) {
                g2.setColor(new Color(112, 83, 54));
                g2.drawString("No items", bx + 26, ry); ry += 30;
            } else {
                for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                    g2.setColor(new Color(52, 35, 24));
                    String lbl = entry.getValue() > 1 ? "x" + entry.getValue() + "  " + entry.getKey()
                                                      : "+ " + entry.getKey();
                    g2.drawString(lbl, bx + 26, ry); ry += 30;
                }
            }
            for (String n : bc.lastRewardAbils) {
                g2.setColor(new Color(74, 55, 92));
                g2.drawString("+ " + n + "  (ability)", bx + 26, ry); ry += 30;
            }
            g2.setColor(new Color(112, 83, 54, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(bx + 20, by + bh - 46, bx + bw - 20, by + bh - 46);
            g2.setFont(pixelFont(Font.ITALIC, 13));
            g2.setColor(new Color(80, 56, 38));
            g2.drawString("Added to your inventory", bx + 26, by + bh - 26);
            g2.setFont(pixelFont(Font.ITALIC, 17));
            g2.setColor(new Color(244, 210, 154));
            String prompt = "Click anywhere to continue";
            g2.drawString(prompt, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(prompt) / 2, by + bh + 34);

        } else if (bc.battleResolved && gp.player != null && gp.player.currentHP <= 0) {
            g2.setColor(new Color(100, 0, 0, 180));
            g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());

            Font defeatFont = new Font("Arial", Font.BOLD, 100);
            g2.setFont(defeatFont);
            String defeatText = "DEFEAT";
            FontMetrics fmd = g2.getFontMetrics();
            int dx = gp.getWidth() / 2 - fmd.stringWidth(defeatText) / 2;
            int dy = gp.getHeight() / 2 + 30;

            for (int spread = 14; spread >= 1; spread--) {
                float alpha = 0.05f * (15 - spread);
                g2.setColor(new Color(1f, 0f, 0f, Math.min(1f, alpha)));
                for (int oy = -spread; oy <= spread; oy += spread)
                    for (int ox = -spread; ox <= spread; ox += spread)
                        if (ox != 0 || oy != 0) g2.drawString(defeatText, dx + ox, dy + oy);
            }
            g2.setColor(Color.BLACK);
            for (int oy = -4; oy <= 4; oy++)
                for (int ox = -4; ox <= 4; ox++)
                    if (ox != 0 || oy != 0) g2.drawString(defeatText, dx + ox, dy + oy);
            g2.setColor(new Color(225, 30, 30));
            g2.drawString(defeatText, dx, dy);

            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(new Color(220, 150, 150));
            String sub = "You were defeated by " + bc.enemyName + "...";
            g2.drawString(sub, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(sub) / 2, dy + 58);

            g2.setFont(new Font("Arial", Font.ITALIC, 17));
            g2.setColor(new Color(180, 180, 180));
            String click = "Click anywhere to continue";
            g2.drawString(click, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(click) / 2, dy + 108);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  NARRATION / PRE-BATTLE (shared dialog-box helper)
    // ─────────────────────────────────────────────────────────────
    private void paintNarration(Graphics2D g2) {
        if (gp.battleSceneImg != null) {
            fill(g2, gp.battleSceneImg);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        }
        String line        = gp.currentDialog;
        String playerFirst = gp.player != null ? BattleManager.cap(gp.player.characterName) : "Player";
        boolean isBossLine = line.startsWith("Beggar:") || line.startsWith("???:")
                          || line.startsWith(gp.battleMgr.ctx.enemyName + ":");

        int bx = 50, by = gp.getHeight() - 210, bw = gp.getWidth() - 100, bh = 165;
        drawPopupBox(g2, bx, by, bw, bh);

        boolean isPlayerLine = line.startsWith(playerFirst + ":");
        BufferedImage port   = isPlayerLine ? gp.playerDialogImg : (isBossLine ? gp.enemyDialogImg : null);
        Rectangle fallback   = new Rectangle(bx + 14, by + (bh - 115) / 2, 82, 115);
        drawDialogPortrait(g2, port, fallback);
        Rectangle textRect = fullDialogTextRect(new Rectangle(bx, by, bw, bh));

        g2.setFont(pixelFont(Font.BOLD, 20));
        g2.setColor(new Color(52, 35, 24));
        int ty = textRect.y + 12;
        for (String ln : wrap(g2, line, textRect.width)) { g2.drawString(ln, textRect.x, ty); ty += 34; }

        g2.setFont(pixelFont(Font.ITALIC, 16));
        g2.setColor(new Color(80, 56, 38));
        String prompt = gp.narMgr.narIsLast() ? "Press 'E' to start the fight!" : "Press 'E' to continue...";
        g2.drawString(prompt, bx + bw - 285, by + bh - 14);
    }

    private void paintPreBattle(Graphics2D g2) {
        fill(g2, gp.battleSceneImg);
        if (gp.battleSpriteHitbox != null) {
            if (gp.playerBattleImg != null) {
                Rectangle r = bounds(gp.battleSpriteHitbox, BC_PLRBAT);
                if (r != null) g2.drawImage(gp.playerBattleImg, r.x, r.y, r.width, r.height, null);
            }
            if (gp.enemyBattleImg != null) {
                Rectangle r = bounds(gp.battleSpriteHitbox, BC_ENMBAT);
                if (r != null) g2.drawImage(gp.enemyBattleImg, r.x, r.y, r.width, r.height, null);
            }
        }
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, gp.getHeight() - 220, gp.getWidth(), 220);

        String line        = gp.narMgr.preBatCurrentLine();
        String playerFirst = gp.player != null ? BattleManager.cap(gp.player.characterName) : "Player";
        boolean isNarLine  = line.startsWith("—") || line.startsWith("-");
        boolean isPlayerLine = !isNarLine && line.startsWith(playerFirst + ":");
        boolean isBossLine   = !isNarLine && (line.startsWith("Beggar:") || line.startsWith("???:"));

        int bx = 60, by = gp.getHeight() - 200, bw = gp.getWidth() - 120, bh = 150;
        drawPopupBox(g2, bx, by, bw, bh);

        BufferedImage port = isPlayerLine ? gp.playerDialogImg : (isBossLine ? gp.enemyDialogImg : null);
        Rectangle fallback = new Rectangle(bx + 12, by + (bh - 110) / 2, 80, 110);
        drawDialogPortrait(g2, port, fallback);
        Rectangle textRect = fullDialogTextRect(new Rectangle(bx, by, bw, bh));

        g2.setFont(pixelFont(Font.BOLD, 20));
        g2.setColor(new Color(52, 35, 24));
        int ty = textRect.y + 10;
        for (String ln : wrap(g2, line, textRect.width)) { g2.drawString(ln, textRect.x, ty); ty += 32; }

        g2.setFont(pixelFont(Font.ITALIC, 16));
        g2.setColor(new Color(80, 56, 38));
        String prompt = gp.narMgr.preBatIsLast() ? "Press 'E' to begin the battle!" : "Press 'E' to continue...";
        g2.drawString(prompt, bx + bw - 280, by + bh - 18);
    }

    // ─────────────────────────────────────────────────────────────
    //  SETTINGS / QUIT-CONFIRM OVERLAYS
    // ─────────────────────────────────────────────────────────────
    private void paintSettingsPanel(Graphics2D g2) {
        Rectangle panel = gp.settingsPanelRect();
        int px = panel.x, py = panel.y, pw = panel.width, ph = panel.height;
        drawPopupBox(g2, px, py, pw, ph);

        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 26));
        String title = "SETTINGS";
        g2.drawString(title, px + pw / 2 - g2.getFontMetrics().stringWidth(title) / 2, py + 40);
        g2.setColor(new Color(112, 83, 54, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(px + 20, py + 50, px + pw - 20, py + 50);

        String mk = gp.audio.isMuted ? "wmuted" : "wmute";
        BufferedImage[] ma = gp.btnImgs.get(mk);
        boolean hoverMute = gp.settingsMuteRect().contains(gp.mouse);
        BufferedImage muteImg = (ma != null) ? (hoverMute && ma[1] != null ? ma[1] : ma[0]) : null;
        if (muteImg != null) {
            Rectangle mr = gp.settingsMuteRect();
            g2.drawImage(muteImg, mr.x, mr.y, mr.width, mr.height, null);
        }
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.PLAIN, 18));
        g2.drawString(gp.audio.isMuted ? "Unmute" : "Mute", px + 190, py + 88);

        drawSlider(g2, px + 30, py + 120, pw - 60, "Music Volume", gp.audio.musicVolume);
        drawSlider(g2, px + 30, py + 190, pw - 60, "SFX Volume",   gp.audio.sfxVolume);

        g2.setFont(pixelFont(Font.ITALIC, 13));
        g2.setColor(new Color(80, 56, 38));
        String hint = "Click outside to close  |  Drag sliders to adjust";
        g2.drawString(hint, px + pw / 2 - g2.getFontMetrics().stringWidth(hint) / 2, py + ph - 12);
    }

    private void paintQuitConfirm(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());

        Rectangle box = gp.quitConfirmRect();
        drawPopupBox(g2, box.x, box.y, box.width, box.height);

        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 26));
        String title = gp.quitConfirmToMenu ? "QUIT TO MENU?" : "QUIT GAME?";
        g2.drawString(title, box.x + box.width / 2 - g2.getFontMetrics().stringWidth(title) / 2, box.y + 46);
        g2.setColor(new Color(112, 83, 54, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(box.x + 24, box.y + 60, box.x + box.width - 24, box.y + 60);

        g2.setColor(new Color(80, 56, 38));
        g2.setFont(pixelFont(Font.BOLD, 18));
        String msg = gp.quitConfirmToMenu ? "Save and return to the main menu?"
                                          : "Are you sure you want to close Five-Six?";
        g2.drawString(msg, box.x + box.width / 2 - g2.getFontMetrics().stringWidth(msg) / 2, box.y + 104);

        drawTextButton(g2, gp.quitYesRect(), "YES");
        drawTextButton(g2, gp.quitNoRect(),  "NO");
    }

    // ─────────────────────────────────────────────────────────────
    //  DRAWING HELPERS
    // ─────────────────────────────────────────────────────────────
    private void fill(Graphics2D g2, BufferedImage im) {
        if (im != null) g2.drawImage(im, 0, 0, gp.getWidth(), gp.getHeight(), null);
    }

    private void drawBtn(Graphics2D g2, BufferedImage hb, int color, String key) {
        if (hb == null) return;
        BufferedImage[] arr = gp.btnImgs.get(key);
        if (arr == null) return;
        boolean hov = key.equals(gp.hoveredBtn);
        BufferedImage bim = (hov && arr[1] != null) ? arr[1] : arr[0];
        if (bim == null) return;
        Rectangle r = bounds(hb, color);
        if (r != null) g2.drawImage(bim, r.x, r.y, r.width, r.height, null);
    }

    private void drawBattleMoveBtn(Graphics2D g2, int color, String key, Dimension size) {
        BufferedImage[] arr = gp.btnImgs.get(key);
        if (arr == null || gp.battleHitbox == null) return;
        Rectangle r = bounds(gp.battleHitbox, color);
        if (r == null) return;
        BufferedImage bim = key.equals(gp.hoveredBtn) && arr[1] != null ? arr[1] : arr[0];
        if (bim == null) return;
        int w = Math.min(r.width, size.width), h = Math.min(r.height, size.height);
        g2.drawImage(bim, r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h, null);
    }

    private void drawLogo(Graphics2D g2, BufferedImage hitbox) {
        if (gp.logoImg == null || hitbox == null) return;
        Rectangle lr = bounds(hitbox, BC_LOGO);
        if (lr != null) g2.drawImage(gp.logoImg, lr.x, lr.y, lr.width, lr.height, null);
    }

    private void drawMenuSettingsButton(Graphics2D g2) {
        BufferedImage[] imgs = gp.btnImgs.get("settings");
        if (imgs == null || imgs[0] == null) return;
        Rectangle r   = gp.fixedMenuSettingsRect();
        BufferedImage img = "settings".equals(gp.hoveredBtn) && imgs[1] != null ? imgs[1] : imgs[0];
        g2.drawImage(img, r.x, r.y, r.width, r.height, null);
    }

    private void drawWorldSettingsButton(Graphics2D g2) {
        BufferedImage[] arr = gp.btnImgs.get("wset");
        Rectangle r = gp.settingsWorldRect();
        if (arr == null || r == null) return;
        BufferedImage img = "wset".equals(gp.hoveredBtn) && arr[1] != null ? arr[1] : arr[0];
        if (img != null) g2.drawImage(img, r.x, r.y, r.width, r.height, null);
    }

    private void drawMapTitle(Graphics2D g2) {
        if (gp.mapTitleImg == null) return;
        Rectangle itemRect = bounds(gp.worldGuiHitbox, BC_ITEMS);
        if (itemRect == null) return;
        int targetH = Math.max(28, itemRect.height - 8);
        int targetW = Math.max(1, gp.mapTitleImg.getWidth() * targetH / Math.max(1, gp.mapTitleImg.getHeight()));
        int x = Math.max(260, itemRect.x - targetW - 20);
        int y = itemRect.y + (itemRect.height - targetH) / 2;
        g2.drawImage(gp.mapTitleImg, x, y, targetW, targetH, null);
    }

    private void hpBar(Graphics2D g2, int x, int y, int w, int h, int cur, int max, String lbl) {
        g2.setColor(popupFill());
        g2.fillRoundRect(x - 8, y - 8, w + 16, h + 32, 10, 10);
        g2.setColor(popupBorder());
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x - 8, y - 8, w + 16, h + 32, 10, 10);
        g2.setColor(new Color(112, 52, 40));
        g2.fillRoundRect(x, y + 16, w, h, 6, 6);
        float r = max > 0 ? (float) cur / max : 0;
        g2.setColor(r > .5f ? new Color(62, 146, 78) : r > .25f ? new Color(184, 132, 42) : new Color(168, 62, 50));
        g2.fillRoundRect(x, y + 16, (int)(w * r), h, 6, 6);
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 16));
        String txt = lbl.isEmpty() ? cur + "/" + max : lbl + "  " + cur + "/" + max;
        g2.drawString(txt, x, y + 14);
    }

    private void damageMultiplierBox(Graphics2D g2, int x, int y, int w, double multiplier) {
        drawPopupBox(g2, x, y, w, 34);
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 15));
        String txt = multiplier >= 999 ? "DMG: 999 (Unlimited)" : String.format("DMG: x%.2f", multiplier);
        g2.drawString(txt, x + w / 2 - g2.getFontMetrics().stringWidth(txt) / 2, y + 23);
    }

    private void dialog(Graphics2D g2, String text) {
        int bx = 60, by = gp.getHeight() - 200, bw = gp.getWidth() - 120, bh = 150;
        drawPopupBox(g2, bx, by, bw, bh);

        BufferedImage port = null;
        if (gp.lastNPCColor != 0) {
            String f = HitboxColors.enemyFolder(gp.lastNPCColor);
            if (f != null) port = gp.imageDisplay.getNpcDialogImage(f);
        } else if (gp.player != null) {
            port = gp.playerDialogImg;
        }

        Rectangle fallbackPort = new Rectangle(bx + 12, by + (bh - 110) / 2, 80, 110);
        boolean hasPortrait = drawDialogPortrait(g2, port, fallbackPort);

        Rectangle dialogBox   = new Rectangle(bx, by, bw, bh);
        Rectangle dialR       = dialogPortraitRect();
        Rectangle portraitRect = dialR != null ? dialR : fallbackPort;
        Rectangle textRect    = dialogTextRect(dialogBox, portraitRect, hasPortrait);
        textRect.y = by + 18; textRect.height = bh - 52;
        drawDialogText(g2, text, textRect);

        g2.setFont(pixelFont(Font.ITALIC, 16));
        g2.setColor(new Color(80, 56, 38));
        g2.drawString("Press 'E' to continue...", bx + bw - 260, by + bh - 18);
    }

    private void drawDialogText(Graphics2D g2, String text, Rectangle textRect) {
        int size = 21;
        List<String> lines;
        FontMetrics fm;
        do {
            g2.setFont(pixelFont(Font.BOLD, size));
            fm    = g2.getFontMetrics();
            lines = wrap(g2, text, textRect.width);
            if (lines.size() * (fm.getHeight() + 3) <= textRect.height || size <= 15) break;
            size--;
        } while (true);
        g2.setColor(new Color(52, 35, 24));
        int lineH = fm.getHeight() + 3;
        int y     = textRect.y + fm.getAscent();
        for (String ln : lines) {
            if (y > textRect.y + textRect.height) break;
            g2.drawString(ln, textRect.x, y); y += lineH;
        }
    }

    private boolean drawDialogPortrait(Graphics2D g2, BufferedImage image, Rectangle fallback) {
        if (image == null) return false;
        Rectangle dialR = dialogPortraitRect();
        drawImageFit(g2, image, dialR != null ? dialR : fallback);
        return true;
    }

    private Rectangle dialogPortraitRect() {
        return gp.worldGuiHitbox != null ? bounds(gp.worldGuiHitbox, BC_CHARDIAL) : null;
    }

    private Rectangle dialogTextRect(Rectangle box, Rectangle portraitRect, boolean hasPortrait) {
        int pad = 20;
        Rectangle text = new Rectangle(box.x + pad, box.y + 40, box.width - pad * 2, box.height - 64);
        if (!hasPortrait || portraitRect == null || !portraitRect.intersects(box)) return text;
        int afterX = box.x + pad;
        if (portraitRect.x <= box.x + box.width / 3)
            afterX = Math.max(afterX, portraitRect.x + portraitRect.width + 18);
        text.x     = afterX;
        text.width = box.x + box.width - pad - text.x;
        return text;
    }

    private Rectangle fullDialogTextRect(Rectangle box) {
        int pad = 20;
        return new Rectangle(box.x + pad, box.y + 18, box.width - pad * 2, box.height - 52);
    }

    private void drawActiveEffects(Graphics2D g2) {
        List<String> effects = gp.battleMgr.activeEffects();
        if (effects.isEmpty()) return;
        int w = 260, h = 34 + effects.size() * 24, x = 40, y = 124;
        drawPopupBox(g2, x, y, w, h);
        g2.setFont(pixelFont(Font.BOLD, 15));
        g2.setColor(new Color(52, 35, 24));
        g2.drawString("ACTIVE", x + 16, y + 24);
        g2.setFont(pixelFont(Font.BOLD, 14));
        int ty = y + 50;
        for (String fx : effects) { g2.drawString(fx, x + 16, ty); ty += 24; }
    }

    private void drawBattleHeader(Graphics2D g2) {
        BattleContext bc = gp.battleMgr.ctx;
        int w = Math.min(440, Math.max(260, gp.getWidth() / 3));
        int h = 78, x = gp.getWidth() / 2 - w / 2, y = 14;
        drawPopupBox(g2, x, y, w, h);
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 22));
        String round = "ROUND " + bc.battleRound;
        g2.drawString(round, x + w / 2 - g2.getFontMetrics().stringWidth(round) / 2, y + 28);
        g2.setColor(new Color(112, 83, 54, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(x + 18, y + 38, x + w - 18, y + 38);
        g2.setColor(new Color(80, 56, 38));
        g2.setFont(pixelFont(Font.BOLD, 14));
        String msg = bc.battleMsg == null || bc.battleMsg.isBlank() ? "Choose your move!" : bc.battleMsg;
        int ty = y + 58;
        for (String line : wrap(g2, msg, w - 28)) {
            g2.drawString(line, x + w / 2 - g2.getFontMetrics().stringWidth(line) / 2, ty);
            ty += 18; if (ty > y + h - 8) break;
        }
    }

    private void drawOutcomePanel(Graphics2D g2) {
        BattleContext bc = gp.battleMgr.ctx;
        int w = Math.min(560, gp.getWidth() - 160), h = 156;
        int x = gp.getWidth() / 2 - w / 2, y = 28;
        drawPopupBox(g2, x, y, w, h);

        String title;
        if      (bc.lastBattleResult == BattleSystem.BattleResult.PLAYER_WIN)
            title = bc.enemyHP <= 0 ? "ROUND WON - ENEMY DEFEATED" : "ROUND WON";
        else if (bc.lastBattleResult == BattleSystem.BattleResult.ENEMY_WIN)
            title = gp.player != null && gp.player.currentHP <= 0 ? "ROUND LOST - DEFEATED" : "ROUND LOST";
        else if (bc.lastBattleResult == BattleSystem.BattleResult.DRAW)
            title = "DRAW";
        else
            title = "ROUND VOIDED";

        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 20));
        g2.drawString(title, x + w / 2 - g2.getFontMetrics().stringWidth(title) / 2, y + 30);
        g2.setColor(new Color(112, 83, 54, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(x + 20, y + 42, x + w - 20, y + 42);

        int rowY = y + 70;
        g2.setFont(pixelFont(Font.BOLD, 16));
        g2.setColor(new Color(80, 56, 38));
        g2.drawString("You dealt: " + bc.lastPlayerDamage + " damage", x + 26, rowY);
        g2.drawString(bc.enemyName + " dealt: " + bc.lastEnemyDamage + " damage", x + 26, rowY + 26);
        if (bc.lastPlayerHeal > 0)
            g2.drawString("Healed: " + bc.lastPlayerHeal + " HP", x + 26, rowY + 52);

        g2.setFont(pixelFont(Font.PLAIN, 13));
        g2.setColor(new Color(80, 56, 38));
        int msgY = y + h - 18;
        for (String line : wrap(g2, bc.battleMsg, w - 52)) {
            g2.drawString(line, x + w / 2 - g2.getFontMetrics().stringWidth(line) / 2, msgY);
            break;
        }
    }

    private void drawSlider(Graphics2D g2, int x, int y, int w, String label, float value) {
        g2.setFont(pixelFont(Font.BOLD, 16));
        g2.setColor(new Color(52, 35, 24));
        g2.drawString(label, x, y);
        String pct = (int)(value * 100) + "%";
        g2.setFont(pixelFont(Font.PLAIN, 14));
        int pctW   = g2.getFontMetrics().stringWidth(pct);
        int trackW = Math.max(40, w - pctW - 16);
        int sy = y + 14, sh = 8;
        g2.setColor(new Color(146, 106, 68));
        g2.fillRoundRect(x, sy, trackW, sh, sh, sh);
        int filled = (int)(trackW * value);
        g2.setColor(new Color(92, 66, 42));
        g2.fillRoundRect(x, sy, filled, sh, sh, sh);
        int thumbX = x + filled - 8, thumbY = sy - 6;
        g2.setColor(new Color(244, 210, 154));
        g2.fillOval(thumbX, thumbY, 20, 20);
        g2.setColor(popupBorder());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(thumbX, thumbY, 20, 20);
        g2.setColor(new Color(80, 56, 38));
        g2.drawString(pct, x + w - pctW, sy + sh);
    }

    private void drawTextButton(Graphics2D g2, Rectangle r, String label) {
        boolean hovered = r.contains(gp.mouse);
        g2.setColor(hovered ? popupRowHover() : popupRow());
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(popupBorder());
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(label, r.x + (r.width  - fm.stringWidth(label)) / 2,
                             r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent());
    }

    private void drawBottomNav(Graphics2D g2) {
        String label = gp.bottomNavLabel();
        if (!label.isEmpty()) drawTextButton(g2, gp.bottomNavRect(), label);
    }

    private void drawPanelContinueHint(Graphics2D g2, int px, int py, int pw, int ph) {
        String hint = "Press E Or Click Anywhere To Continue";
        int w = 300, h = 34, x = px + 18, y = py + ph - h - 14;
        g2.setColor(popupRow());
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(popupBorder());
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(hint, x + (w - fm.stringWidth(hint)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    private void drawHoverText(Graphics2D g2, String text) {
        if (text == null || text.isBlank()) return;
        int tw = 500, th = 96, tx = gp.getWidth() / 2 - tw / 2, ty = gp.getHeight() - th - 34;
        drawPopupBox(g2, tx, ty, tw, th);
        g2.setFont(pixelFont(Font.BOLD, 16));
        g2.setColor(new Color(56, 36, 24));
        int y = ty + 30;
        for (String ln : wrap(g2, text, tw - 32)) { g2.drawString(ln, tx + 16, y); y += 22; }
    }

    void drawPopupBox(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(popupFill());
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(popupBorder());
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x, y, w, h, 10, 10);
    }

    private void panelHeader(Graphics2D g2, int px, int py, int pw, int ph, String title) {
        drawPopupBox(g2, px, py, pw, ph);
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 30));
        g2.drawString(title, px + pw / 2 - g2.getFontMetrics().stringWidth(title) / 2, py + 44);
    }

    private void drawPlaceholderScreen(Graphics2D g2, String title) {
        g2.setColor(new Color(198, 158, 104));
        g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());
        g2.setColor(new Color(112, 83, 54));
        g2.setStroke(new BasicStroke(8f));
        g2.drawRect(28, 28, gp.getWidth() - 56, gp.getHeight() - 56);
        g2.setFont(pixelFont(Font.BOLD, 48));
        g2.setColor(new Color(52, 35, 24));
        g2.drawString(title, gp.getWidth() / 2 - g2.getFontMetrics().stringWidth(title) / 2, gp.getHeight() / 2);
    }

    private void drawImageFit(Graphics2D g2, BufferedImage image, Rectangle box) {
        if (image == null || box == null || box.width <= 0 || box.height <= 0) return;
        double scale = Math.min(box.width / (double) image.getWidth(),
                                box.height / (double) image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth()  * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        g2.drawImage(image, box.x + (box.width - w) / 2, box.y + (box.height - h) / 2, w, h, null);
    }

    private List<String> wrap(Graphics2D g2, String text, int maxW) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        FontMetrics fm = g2.getFontMetrics();
        for (String para : text.split("\n")) {
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                String t = cur.length() == 0 ? word : cur + " " + word;
                if (fm.stringWidth(t) > maxW) {
                    if (cur.length() > 0) out.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    cur = new StringBuilder(t);
                }
            }
            if (cur.length() > 0) out.add(cur.toString());
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────
    //  BOUNDS CACHE
    // ─────────────────────────────────────────────────────────────
    Rectangle bounds(BufferedImage hb, int targetColor) {
        if (hb == null) return null;
        if (gp.getWidth() != cachedW || gp.getHeight() != cachedH) {
            boundsCache.clear(); cachedW = gp.getWidth(); cachedH = gp.getHeight();
        }
        long key = ((long) System.identityHashCode(hb) << 32) | (targetColor & 0xFFFFFFFFL);
        return boundsCache.computeIfAbsent(key, k -> computeBounds(hb, targetColor));
    }

    private Rectangle computeBounds(BufferedImage hb, int targetColor) {
        int mnX = Integer.MAX_VALUE, mxX = 0, mnY = Integer.MAX_VALUE, mxY = 0;
        boolean found = false;
        for (int y = 0; y < hb.getHeight(); y++) {
            for (int x = 0; x < hb.getWidth(); x++) {
                if ((hb.getRGB(x, y) & 0xFFFFFF) == targetColor) {
                    if (x < mnX) mnX = x; if (x > mxX) mxX = x;
                    if (y < mnY) mnY = y; if (y > mxY) mxY = y;
                    found = true;
                }
            }
        }
        if (!found) return null;
        int W = gp.getWidth(), H = gp.getHeight(), hw = hb.getWidth(), hh = hb.getHeight();
        return new Rectangle(mnX * W / hw, mnY * H / hh,
                             (mxX - mnX + 1) * W / hw, (mxY - mnY + 1) * H / hh);
    }

    // ─────────────────────────────────────────────────────────────
    //  CHAR-SELECT HELPERS (layout)
    // ─────────────────────────────────────────────────────────────
    private Dimension characterButtonDrawSize() {
        int w = Integer.MAX_VALUE, h = Integer.MAX_VALUE;
        for (String name : List.of("ivan", "sam", "nimuel", "johnfiel")) {
            Rectangle r = gp.charButtonRect(name);
            if (r == null) continue;
            w = Math.min(w, r.width); h = Math.min(h, r.height);
        }
        if (w == Integer.MAX_VALUE || h == Integer.MAX_VALUE)
            return new Dimension(Math.max(1, 92 * gp.getWidth() / 500),
                                 Math.max(1, 28 * gp.getHeight() / 342));
        return new Dimension(w, h);
    }

    private Rectangle charButtonDrawRect(String name, Dimension size) {
        Rectangle r = gp.charButtonRect(name);
        if (r == null) return null;
        int w = Math.min(r.width,  size.width);
        int h = Math.min(r.height, size.height);
        return new Rectangle(r.x + (r.width - w) / 2, r.y + (r.height - h) / 2, w, h);
    }

    private Dimension battleMoveButtonSize() {
        Rectangle r = bounds(gp.battleHitbox, BC_ROCKBTN);
        Rectangle p = bounds(gp.battleHitbox, BC_PAPERBTN);
        Rectangle s = bounds(gp.battleHitbox, BC_SCISSBTN);
        int w = Integer.MAX_VALUE, h = Integer.MAX_VALUE;
        for (Rectangle rect : new Rectangle[]{r, p, s}) {
            if (rect == null) continue;
            w = Math.min(w, rect.width); h = Math.min(h, rect.height);
        }
        if (w == Integer.MAX_VALUE || h == Integer.MAX_VALUE) return new Dimension(1, 1);
        return new Dimension(w, h);
    }

    // ─────────────────────────────────────────────────────────────
    //  STYLE CONSTANTS
    // ─────────────────────────────────────────────────────────────
    private Color popupFill()     { return new Color(198, 158, 104, 242); }
    private Color popupBorder()   { return new Color(112,  83,  54); }
    private Color popupRow()      { return new Color(226, 190, 134, 235); }
    private Color popupRowHover() { return new Color(244, 210, 154, 245); }
    private Font  pixelFont(int style, int size) { return new Font("Monospaced", style, size); }
}
