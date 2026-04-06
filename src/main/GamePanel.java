package main;

import entity.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {

  // SCREEN SETTINGS
  final int originalTileSize = 16;
  final int scale = 10;
  public final int tileSize = originalTileSize * scale;
  public int screenWidth  = tileSize * 16;
  public int screenHeight = tileSize * 12;

  // SYSTEM
  public KeyHandler keyH = new KeyHandler(this);
  Thread gameThread;
  public Player player;
  public BufferedImage mapImage, hitboxImage;
  public String currentMapName = "gle";

  // NPC IMAGES
  public BufferedImage jamesStand, alieyandrewStand, kyleStand, johnruStand, adrianStand;

  // COLOR CONSTANTS
  public final int COLOR_WALL        = 0xA349A4;
  public final int COLOR_DOOR        = 0xFF7F27;
  public final int COLOR_SPAWN       = 0x22B14C;
  public final int COLOR_JAMES       = 0x3F48CC;
  public final int COLOR_ALIEYANDREW = 0xFFA1F2;
  public final int COLOR_KYLE        = 0x00A2E8;
  public final int COLOR_JOHNRU      = 0xFFF200;
  public final int COLOR_ADRIAN      = 0xB97A57;

  // GAME STATES
  public int gameState;
  public final int titleState  = 0;
  public final int playState   = 1;
  public final int fadeState   = 2;
  public final int battleState = 3;

  // DEBUG
  public boolean showDebug = false;
  private boolean f1WasPressed = false;

  // DIALOG
  public String currentDialog = "";
  public int dialogStage  = 0;
  public int lastNPCColor = 0;

  // TITLE SCREEN
  Rectangle ivanBtn, nimuelBtn, samBtn, johnfielBtn;
  private int hoveredCharIndex = -1;
  private static final CharacterStats.CharacterType[] CHAR_TYPES = {
          CharacterStats.CharacterType.IVAN,
          CharacterStats.CharacterType.NIMUEL,
          CharacterStats.CharacterType.SAM,
          CharacterStats.CharacterType.JOHNFIEL
  };

  // FADE TRANSITION
  private float fadeAlpha = 0f;
  private boolean fadingIn = true;
  public int pendingBattleEnemyColor = 0;

  // BATTLE
  public EnemyStats enemyStats   = new EnemyStats();
  private int enemyCurrentHP     = 0;
  private int enemyMaxHP         = 0;
  private int battleRound        = 1;
  private String battleMessage   = "";
  private String playerMoveDisplay = "";
  private String enemyMoveDisplay  = "";
  private boolean battleResolved   = false;
  private boolean waitingForNext   = false;
  private String enemyName         = "";

  // BATTLE BUTTONS
  private Rectangle rockBtn, paperBtn, scissorsBtn, continueBtn;

  // RNG
  private final Random rand = new Random();

  public GamePanel() {
    this.setPreferredSize(new Dimension(screenWidth, screenHeight));
    this.setBackground(Color.black);
    this.setDoubleBuffered(true);
    this.addKeyListener(keyH);
    this.setFocusable(true);
    gameState = titleState;

    // Title screen character buttons
    int btnW = 300, btnH = 60;
    int centerX = screenWidth / 2 - btnW / 2;
    ivanBtn     = new Rectangle(centerX, 300, btnW, btnH);
    nimuelBtn   = new Rectangle(centerX, 400, btnW, btnH);
    samBtn      = new Rectangle(centerX, 500, btnW, btnH);
    johnfielBtn = new Rectangle(centerX, 600, btnW, btnH);

    loadImages();

    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();

        if (gameState == titleState) {
          if      (ivanBtn.contains(p))     selectChar("ivan");
          else if (nimuelBtn.contains(p))   selectChar("nimuel");
          else if (samBtn.contains(p))      selectChar("sam");
          else if (johnfielBtn.contains(p)) selectChar("johnfiel");
        }

        if (gameState == fadeState && fadeAlpha >= 1f && !fadingIn) {
          gameState = battleState;
          startBattle();
        }

        if (gameState == battleState) {
          if (!waitingForNext) {
            if      (rockBtn.contains(p))     resolveBattle(BattleSystem.Move.ROCK);
            else if (paperBtn.contains(p))    resolveBattle(BattleSystem.Move.PAPER);
            else if (scissorsBtn.contains(p)) resolveBattle(BattleSystem.Move.SCISSORS);
          } else {
            if (continueBtn.contains(p)) nextRound();
          }
        }
      }
    });

    this.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        if (gameState != titleState) return;
        Point p = e.getPoint();
        int prev = hoveredCharIndex;
        if      (ivanBtn.contains(p))     hoveredCharIndex = 0;
        else if (nimuelBtn.contains(p))   hoveredCharIndex = 1;
        else if (samBtn.contains(p))      hoveredCharIndex = 2;
        else if (johnfielBtn.contains(p)) hoveredCharIndex = 3;
        else                              hoveredCharIndex = -1;
        if (hoveredCharIndex != prev) repaint();
      }
    });
  }

  private void updateBattleButtons() {
    int bw   = 180, bh = 65;
    int midX = getWidth() / 2;
    int btnY = getHeight() - 160;
    rockBtn     = new Rectangle(midX - 290, btnY, bw, bh);
    paperBtn    = new Rectangle(midX - 90,  btnY, bw, bh);
    scissorsBtn = new Rectangle(midX + 110, btnY, bw, bh);
    continueBtn = new Rectangle(midX - 100, btnY, 200, bh);
  }

  // ─── IMAGE LOADING ────────────────────────────────────────────────

  private void loadImages() {
    try {
      mapImage    = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/" + currentMapName + ".png")));
      hitboxImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/" + currentMapName + "Hitboxes.png")));
      jamesStand       = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/james/james_stand.png")));
      alieyandrewStand = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/alieyandrew/alieyandrew_stand.png")));
      kyleStand        = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/kyle/kyle_stand.png")));
      johnruStand      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/johnru/johnru_stand.png")));
      adrianStand      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/adrian/adrian_stand.png")));
    } catch (Exception e) {
      System.out.println("Image loading failed.");
    }
  }

  // ─── CHARACTER SELECT ─────────────────────────────────────────────

  public void selectChar(String name) {
    player = new Player(this, keyH, name);
    setPlayerSpawn();
    gameState = playState;
  }

  public void setPlayerSpawn() {
    if (hitboxImage == null) return;
    for (int y = 0; y < hitboxImage.getHeight(); y++) {
      for (int x = 0; x < hitboxImage.getWidth(); x++) {
        if ((hitboxImage.getRGB(x, y) & 0xFFFFFF) == COLOR_SPAWN) {
          player.x = (x * getWidth())  / hitboxImage.getWidth()  - (tileSize / 2);
          player.y = (y * getHeight()) / hitboxImage.getHeight() - (tileSize / 2);
          player.saveSpawn(player.x, player.y);
          return;
        }
      }
    }
  }

  // ─── FADE TO BLACK ────────────────────────────────────────────────

  public void startFadeToBlack() {
    gameState     = fadeState;
    fadeAlpha     = 0f;
    fadingIn      = true;
    currentDialog = "";
  }

  private void updateFade() {
    if (fadingIn) {
      fadeAlpha += 0.015f;
      if (fadeAlpha >= 1f) {
        fadeAlpha = 1f;
        fadingIn  = false;
      }
    }
  }

  // ─── BATTLE ───────────────────────────────────────────────────────

  private void startBattle() {
    updateBattleButtons();
    enemyMaxHP        = enemyStats.getEnemyHP(pendingBattleEnemyColor);
    enemyCurrentHP    = enemyMaxHP;
    battleRound       = 1;
    playerMoveDisplay = "";
    enemyMoveDisplay  = "";
    battleResolved    = false;
    waitingForNext    = false;
    enemyName         = getEnemyName(pendingBattleEnemyColor);
    battleMessage     = "Round " + battleRound + " — Choose your move!";
  }

  private String getEnemyName(int color) {
    if (color == COLOR_JAMES)       return "James";
    if (color == COLOR_ALIEYANDREW) return "Alieyandrew";
    if (color == COLOR_KYLE)        return "Kyle";
    if (color == COLOR_JOHNRU)      return "Johnru";
    if (color == COLOR_ADRIAN)      return "Adrian";
    return "Enemy";
  }

  private void resolveBattle(BattleSystem.Move playerMove) {
    BattleSystem.Move enemyMove   = BattleSystem.getRandomEnemyMove();
    BattleSystem.BattleResult result = BattleSystem.resolve(playerMove, enemyMove);

    playerMoveDisplay = BattleSystem.getMoveEmoji(playerMove);
    enemyMoveDisplay  = BattleSystem.getMoveEmoji(enemyMove);

    switch (result) {
      case PLAYER_WIN -> {
        int dmg = (int) Math.max(1, (rand.nextInt(10) + 1) * player.damageMultiplier);
        enemyCurrentHP = Math.max(0, enemyCurrentHP - dmg);
        if (enemyCurrentHP <= 0) {
          battleMessage  = "You Win! Dealt " + dmg + " dmg — " + enemyName + " defeated!";
          battleResolved = true;
          enemyStats.markDefeated(pendingBattleEnemyColor);
          player.healPercent(0.20);
        } else {
          battleMessage = "You Win! Dealt " + dmg + " damage to " + enemyName + ".";
        }
        waitingForNext = true;
      }
      case ENEMY_WIN -> {
        int dmg = (int) Math.max(1, (rand.nextInt(10) + 1) / player.damageMultiplier);
        player.currentHP = Math.max(0, player.currentHP - dmg);
        if (player.currentHP <= 0) {
          battleMessage  = "You Lose! Took " + dmg + " dmg — You were defeated! Respawning...";
          battleResolved = true;
        } else {
          battleMessage = "You Lose! Took " + dmg + " damage from " + enemyName + ".";
        }
        waitingForNext = true;
      }
      case DRAW -> {
        battleMessage  = "Draw! No damage dealt.";
        waitingForNext = true;
      }
    }
  }

  private void nextRound() {
    if (battleResolved) {
      if (player.currentHP <= 0) {
        player.respawnWithPenalty();
      }
      // If player won, they stay in place with healed HP — no teleport
      gameState         = playState;
      currentDialog     = "";
      dialogStage       = 0;
      lastNPCColor      = 0;
      battleResolved    = false;
      waitingForNext    = false;
      playerMoveDisplay = "";
      enemyMoveDisplay  = "";
    } else {
      battleRound++;
      battleMessage     = "Round " + battleRound + " — Choose your move!";
      playerMoveDisplay = "";
      enemyMoveDisplay  = "";
      waitingForNext    = false;
    }
  }

  // ─── GAME LOOP ────────────────────────────────────────────────────

  public void startGameThread() {
    gameThread = new Thread(this);
    gameThread.start();
  }

  @Override
  public void run() {
    double drawInterval = 1_000_000_000.0 / 60;
    double delta   = 0;
    long lastTime  = System.nanoTime();

    while (gameThread != null) {
      long currentTime = System.nanoTime();
      delta    += (currentTime - lastTime) / drawInterval;
      lastTime  = currentTime;

      if (delta >= 1) {
        if (gameState == playState) {
          player.update();
          if (keyH.f1Pressed && !f1WasPressed) { showDebug = !showDebug; f1WasPressed = true; }
          if (!keyH.f1Pressed) f1WasPressed = false;
        }
        if (gameState == fadeState) updateFade();
        if (keyH.escPressed && gameState == playState) {
          gameState     = titleState;
          currentDialog = "";
        }
        repaint();
        delta--;
      }
    }
  }

  // ─── NPC DRAWING ─────────────────────────────────────────────────

  public void drawNPCs(Graphics2D g2) {
    if (hitboxImage == null) return;
    HashMap<Integer, int[]> colorBounds = new HashMap<>();

    // 1. Find the boundaries of each NPC color area
    for (int y = 0; y < hitboxImage.getHeight(); y++) {
      for (int x = 0; x < hitboxImage.getWidth(); x++) {
        int color = hitboxImage.getRGB(x, y) & 0xFFFFFF;
        if (color != COLOR_JAMES && color != COLOR_ALIEYANDREW &&
                color != COLOR_KYLE && color != COLOR_JOHNRU && color != COLOR_ADRIAN) continue;

        if (enemyStats.isDefeated(color)) continue;

        if (!colorBounds.containsKey(color)) {
          colorBounds.put(color, new int[]{x, x, y, y}); // minX, maxX, minY, maxY
        } else {
          int[] b = colorBounds.get(color);
          if (x < b[0]) b[0] = x;
          if (x > b[1]) b[1] = x;
          if (y < b[2]) b[2] = y;
          if (y > b[3]) b[3] = y;
        }
      }
    }

    // 2. Calculate center and draw
    for (int color : colorBounds.keySet()) {
      int[] b = colorBounds.get(color);

      // Calculate the center pixel of the color area in the hitbox image
      int centerX = (b[0] + b[1]) / 2;
      int centerY = (b[2] + b[3]) / 2;

      // Scale these coordinates to the actual screen size
      int screenX = (centerX * getWidth()) / hitboxImage.getWidth();
      int screenY = (centerY * getHeight()) / hitboxImage.getHeight();

      // Adjust so the CENTER of the sprite (tileSize) sits on the center of the area
      int drawX = screenX - (tileSize / 2);
      int drawY = screenY - (tileSize / 2);

      BufferedImage img = null;
      if (color == COLOR_JAMES)       img = jamesStand;
      if (color == COLOR_ALIEYANDREW) img = alieyandrewStand;
      if (color == COLOR_KYLE)        img = kyleStand;
      if (color == COLOR_JOHNRU)      img = johnruStand;
      if (color == COLOR_ADRIAN)      img = adrianStand;

      if (img != null) {
        g2.drawImage(img, drawX, drawY, tileSize, tileSize, null);
      }
    }
  }

    // ─── PAINT ────────────────────────────────────────────────────────

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      if      (gameState == titleState)  drawTitleScreen(g2);
      else if (gameState == playState)   drawPlayState(g2);
      else if (gameState == fadeState)   drawFadeState(g2);
      else if (gameState == battleState) drawBattleState(g2);

      g2.dispose();
    }

    // ─── TITLE SCREEN ─────────────────────────────────────────────────

    private void drawTitleScreen(Graphics2D g2) {
      g2.setColor(new Color(30, 30, 60));
      g2.fillRect(0, 0, getWidth(), getHeight());

      g2.setColor(Color.WHITE);
      g2.setFont(new Font("Arial", Font.BOLD, 56));
      String title = "CHARACTER SELECT";
      int tw = g2.getFontMetrics().stringWidth(title);
      g2.drawString(title, getWidth() / 2 - tw / 2, 200);

      g2.setFont(new Font("Arial", Font.ITALIC, 22));
      g2.setColor(new Color(180, 180, 220));
      String sub = "Hover over a character to see their stats";
      int sw = g2.getFontMetrics().stringWidth(sub);
      g2.drawString(sub, getWidth() / 2 - sw / 2, 250);

      Rectangle[] btns  = {ivanBtn, nimuelBtn, samBtn, johnfielBtn};
      String[]    names = {"IVAN", "NIMUEL", "SAM", "JOHNFIEL"};

      for (int i = 0; i < btns.length; i++) {
        boolean hovered = (hoveredCharIndex == i);

        g2.setColor(hovered ? new Color(80, 80, 160) : new Color(50, 50, 100));
        g2.fillRoundRect(btns[i].x, btns[i].y, btns[i].width, btns[i].height, 16, 16);

        g2.setColor(hovered ? Color.WHITE : new Color(120, 120, 200));
        g2.setStroke(new BasicStroke(hovered ? 3 : 1));
        g2.drawRoundRect(btns[i].x, btns[i].y, btns[i].width, btns[i].height, 16, 16);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        int lw = g2.getFontMetrics().stringWidth(names[i]);
        g2.drawString(names[i], btns[i].x + btns[i].width / 2 - lw / 2, btns[i].y + 40);
      }

      if (hoveredCharIndex >= 0) {
        drawStatCard(g2, CHAR_TYPES[hoveredCharIndex]);
      }
    }

    private void drawStatCard(Graphics2D g2, CharacterStats.CharacterType ct) {
      int cardX = screenWidth / 2 + 200;
      int cardY = 280;
      int cardW = 320;
      int cardH = 220;

      g2.setColor(new Color(20, 20, 50, 230));
      g2.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
      g2.setColor(new Color(150, 150, 255));
      g2.setStroke(new BasicStroke(2));
      g2.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

      int tx   = cardX + 24;
      int ty   = cardY + 44;
      int lineH = 42;

      g2.setColor(Color.WHITE);
      g2.setFont(new Font("Arial", Font.BOLD, 26));
      g2.drawString(ct.displayName, tx, ty);

      ty += lineH - 8;
      g2.setColor(new Color(180, 220, 255));
      g2.setFont(new Font("Arial", Font.ITALIC, 20));
      g2.drawString("\" " + ct.description + " \"", tx, ty);

      ty += lineH;
      g2.setColor(new Color(100, 255, 120));
      g2.setFont(new Font("Arial", Font.BOLD, 22));
      g2.drawString("HP:  " + ct.maxHP, tx, ty);

      ty += lineH - 4;
      g2.setColor(new Color(255, 180, 80));
      String dmgText = ct.damageMultiplier >= 999
              ? "DMG: 999 (Unlimited)"
              : String.format("DMG: x%.1f multiplier", ct.damageMultiplier);
      g2.drawString(dmgText, tx, ty);
    }

    // ─── PLAY STATE ───────────────────────────────────────────────────

    private void drawPlayState(Graphics2D g2) {
      if (mapImage != null) g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);

      drawNPCs(g2);

      if (showDebug && hitboxImage != null) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g2.drawImage(hitboxImage, 0, 0, getWidth(), getHeight(), null);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
      }

      player.draw(g2);
      drawHPBar(g2, 20, 20, 220, 22, player.currentHP, player.maxHP, player.characterName);

      if (!currentDialog.isEmpty()) {
        drawDialogBox(g2, currentDialog);
      }
    }

    private void drawHPBar(Graphics2D g2, int x, int y, int w, int h, int current, int max, String label) {
      g2.setColor(new Color(0, 0, 0, 160));
      g2.fillRoundRect(x - 4, y - 4, w + 8, h + 24, 10, 10);

      g2.setColor(new Color(80, 0, 0));
      g2.fillRoundRect(x, y + 16, w, h, 6, 6);

      float ratio = max > 0 ? (float) current / max : 0f;
      Color barColor = ratio > 0.5f ? new Color(60, 200, 80)
              : ratio > 0.25f ? new Color(230, 180, 0)
              : new Color(220, 50, 50);
      g2.setColor(barColor);
      g2.fillRoundRect(x, y + 16, (int)(w * ratio), h, 6, 6);

      g2.setColor(Color.WHITE);
      g2.setFont(new Font("Arial", Font.BOLD, 16));
      g2.drawString(label + "  " + current + " / " + max, x, y + 14);
    }

    private void drawDialogBox(Graphics2D g2, String text) {
      int bx = 100, by = getHeight() - 200, bw = getWidth() - 200, bh = 150;
      g2.setColor(new Color(0, 0, 0, 200));
      g2.fillRoundRect(bx, by, bw, bh, 25, 25);
      g2.setColor(Color.WHITE);
      g2.setStroke(new BasicStroke(3));
      g2.drawRoundRect(bx, by, bw, bh, 25, 25);
      g2.setFont(new Font("Arial", Font.BOLD, 26));
      g2.drawString(text, bx + 40, by + 60);
      g2.setFont(new Font("Arial", Font.ITALIC, 18));
      g2.drawString("Press 'E' to continue...", getWidth() - 360, by + 120);
    }

    // ─── FADE STATE ───────────────────────────────────────────────────

    private void drawFadeState(Graphics2D g2) {
      if (mapImage != null) g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);
      drawNPCs(g2);
      player.draw(g2);

      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
      g2.setColor(Color.BLACK);
      g2.fillRect(0, 0, getWidth(), getHeight());
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

      if (fadeAlpha >= 1f && !fadingIn) {
        int bw = 280, bh = 70;
        int bx = getWidth() / 2 - bw / 2;
        int by = getHeight() / 2 - bh / 2;

        g2.setColor(new Color(180, 30, 30));
        g2.fillRoundRect(bx, by, bw, bh, 16, 16);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(bx, by, bw, bh, 16, 16);
        g2.setFont(new Font("Arial", Font.BOLD, 34));
        String label = "START BATTLE";
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, getWidth() / 2 - lw / 2, by + 46);
      }
    }

    // ─── BATTLE STATE ─────────────────────────────────────────────────

    private void drawBattleState(Graphics2D g2) {
      updateBattleButtons();

      g2.setColor(new Color(15, 15, 35));
      g2.fillRect(0, 0, getWidth(), getHeight());

      int midX = getWidth() / 2;

      // Title
      g2.setColor(new Color(220, 60, 60));
      g2.setFont(new Font("Arial", Font.BOLD, 40));
      String battleTitle = "BATTLE — Round " + battleRound;
      int tw = g2.getFontMetrics().stringWidth(battleTitle);
      g2.drawString(battleTitle, midX - tw / 2, 70);

      // Player name + HP bar
      g2.setColor(Color.WHITE);
      g2.setFont(new Font("Arial", Font.BOLD, 22));
      g2.drawString(player.characterName, 80, 130);
      drawHPBar(g2, 80, 140, 340, 28, player.currentHP, player.maxHP, "");

      // Enemy name + HP bar
      int eHPx = getWidth() - 460;
      g2.setColor(new Color(255, 120, 120));
      g2.setFont(new Font("Arial", Font.BOLD, 22));
      g2.drawString(enemyName, eHPx, 130);
      drawHPBar(g2, eHPx, 140, 340, 28, enemyCurrentHP, enemyMaxHP, "");

      // VS
      g2.setColor(new Color(255, 200, 50));
      g2.setFont(new Font("Arial", Font.BOLD, 46));
      g2.drawString("VS", midX - 24, 170);

      // Move display boxes
      if (!playerMoveDisplay.isEmpty()) {
        int boxY = 250, boxH = 100, boxW = 260;

        g2.setColor(new Color(30, 80, 30));
        g2.fillRoundRect(80, boxY, boxW, boxH, 14, 14);
        g2.setColor(new Color(80, 200, 80));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(80, boxY, boxW, boxH, 14, 14);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.drawString("You: " + playerMoveDisplay, 100, boxY + 60);

        g2.setColor(new Color(80, 20, 20));
        g2.fillRoundRect(eHPx, boxY, boxW, boxH, 14, 14);
        g2.setColor(new Color(220, 80, 80));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(eHPx, boxY, boxW, boxH, 14, 14);
        g2.setColor(Color.WHITE);
        g2.drawString(enemyName + ": " + enemyMoveDisplay, eHPx + 20, boxY + 60);
      }

      // Battle message
      int msgY = 400;
      String[] lines = battleMessage.split("\n");
      g2.setFont(new Font("Arial", Font.BOLD, 24));
      for (String line : lines) {
        int lw = g2.getFontMetrics().stringWidth(line);
        g2.setColor(new Color(255, 230, 100));
        g2.drawString(line, midX - lw / 2, msgY);
        msgY += 36;
      }

      // Buttons
      if (!waitingForNext) {
        // Draw the move selection buttons when it is the player's turn
        drawMoveButton(g2, rockBtn,     "ROCK",     new Color(100, 80,  40),  new Color(200, 160, 80));
        drawMoveButton(g2, paperBtn,    "PAPER",    new Color(40,  80,  120), new Color(80,  160, 220));
        drawMoveButton(g2, scissorsBtn, "SCISSORS", new Color(80,  40,  100), new Color(180, 80,  220));
      } else {
        // Draw the "Next Round" or "Back to Game" button after a move is resolved
        String label = battleResolved ? "Back to Game" : "Next Round";
        g2.setColor(new Color(40, 120, 40));
        g2.fillRoundRect(continueBtn.x, continueBtn.y, continueBtn.width, continueBtn.height, 14, 14);
        g2.setColor(new Color(100, 255, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(continueBtn.x, continueBtn.y, continueBtn.width, continueBtn.height, 14, 14);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, continueBtn.x + continueBtn.width / 2 - lw / 2, continueBtn.y + 42);
      }
    }

  private void drawMoveButton(Graphics2D g2, Rectangle btn, String label, Color fill, Color border) {
    g2.setColor(fill);
    g2.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 14, 14);
    g2.setColor(border);
    g2.setStroke(new BasicStroke(2));
    g2.drawRoundRect(btn.x, btn.y, btn.width, btn.height, 14, 14);
    g2.setColor(Color.WHITE);
    g2.setFont(new Font("Arial", Font.BOLD, 26));
    int lw = g2.getFontMetrics().stringWidth(label);
    g2.drawString(label, btn.x + btn.width / 2 - lw / 2, btn.y + 44);
  }
  }