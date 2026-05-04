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
import javax.swing.SwingUtilities;

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
    public BufferedImage menuBackgroundImage, menuLogoImage;
    public BufferedImage startIdleImage, startHoverImage;
    public BufferedImage creditsIdleImage, creditsHoverImage;
    public BufferedImage quitIdleImage, quitHoverImage;
    public BufferedImage continueIdleImage, continueHoverImage;
    public BufferedImage selectCharacterIdleImage, selectCharacterHoverImage;
    public BufferedImage muteIdleImage, muteHoverImage;
    public BufferedImage settingsIdleImage, settingsHoverImage;
    public BufferedImage[] characterButtonIdleImages, characterButtonHoverImages;
    public String currentMapName = "gle";
    public static final String START_MAP = "gle";
    public static final String WALKWAY_MAP = "walkway";

    // NPC IMAGES
    public BufferedImage jamesStand, alieyandrewStand, kyleStand, johnruStand, adrianStand;

    // PLAYER CHARACTER IMAGES
    public BufferedImage[] ivanStands, nimuelStands, samStands, johnfielStands;

    // COLOR CONSTANTS
    public final int COLOR_WALL        = 0xA349A4;
    public final int COLOR_DOOR        = 0xFF7F27;
    public final int COLOR_SPAWN       = 0x22B14C;
    public final int COLOR_JAMES       = 0x3F48CC;
    public final int COLOR_ALIEYANDREW = 0xFFA1F2;
    public final int COLOR_KYLE        = 0x00A2E8;
    public final int COLOR_JOHNRU      = 0xFFF200;
    public final int COLOR_ADRIAN      = 0xB97A57;
    public final int COLORNEXTAREA     = 0xED1C24;
    public final int COLOR_MENU_START  = 0x51FFD5;
    public final int COLOR_MENU_CREDIT = 0x00FF7D;
    public final int COLOR_MENU_QUIT   = 0x0DA100;
    public final int COLOR_CONTINUE    = 0xCE74FF;
    public final int COLOR_SELECT_CHAR = 0x47B2FF;
    public final int COLOR_CHAR_IVAN   = 0xFF0000;
    public final int COLOR_CHAR_SAM    = 0xFFE679;
    public final int COLOR_CHAR_NIMUEL = 0xFFADAD;
    public final int COLOR_CHAR_JOHNFIEL = 0x0EFC7B;

    // GAME STATES
    public int gameState;
    public final int titleState  = 0;
    public final int playState   = 1;
    public final int fadeState   = 2;
    public final int battleState = 3;
    public final int startMenuState = 4;
    public final int characterSelectState = 5;

    // DEBUG
    public boolean showDebug = false;
    private boolean f1WasPressed = false;
    private boolean escWasPressed = false;

    // DIALOG
    public String currentDialog = "";
    public int dialogStage  = 0;
    public int lastNPCColor = 0;

    // TITLE SCREEN
    Rectangle startBtn, creditsBtn, quitBtn, continueMenuBtn, selectCharacterBtn;
    Rectangle muteBtn, settingsBtn, ivanBtn, samBtn, nimuelBtn, johnfielBtn;
    private int hoveredCharIndex = -1;
    private int hoveredMenuColor = 0;
    private String hoveredMenuButton = "";
    private int hoverFrameCounter = 0;
    private int lastHoveredIndex = -1;
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
    private float menuFadeAlpha = 0f;
    private boolean menuFadeActive = false;
    private boolean menuFadingOut = true;
    private int menuFadeTargetState = titleState;
    private static final float MENU_FADE_SPEED = 0.08f;

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

        startBtn = new Rectangle(178, 155, 144, 25);
        creditsBtn = new Rectangle(178, 187, 144, 25);
        quitBtn = new Rectangle(178, 219, 144, 25);
        continueMenuBtn = new Rectangle(179, 170, 142, 25);
        selectCharacterBtn = new Rectangle(179, 202, 142, 25);
        muteBtn = new Rectangle(23, 280, 25, 20);
        settingsBtn = new Rectangle(16, 309, 62, 22);
        ivanBtn = new Rectangle(83, 120, 74, 24);
        samBtn = new Rectangle(164, 120, 76, 24);
        nimuelBtn = new Rectangle(250, 120, 75, 24);
        johnfielBtn = new Rectangle(334, 120, 81, 24);

        loadImages();

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (menuFadeActive) return;
                Point p = e.getPoint();

                if (gameState == titleState) {
                    String button = getMainMenuButtonAt(p);
                    if ("start".equals(button)) {
                        clearMenuHover();
                        startMenuFade(startMenuState);
                        return;
                    } else if ("quit".equals(button)) {
                        quitGame();
                        return;
                    }
                }

                if (gameState == startMenuState) {
                    String button = getStartMenuButtonAt(p);
                    if ("continue".equals(button) && player != null) {
                        clearMenuHover();
                        startMenuFade(playState);
                        return;
                    } else if ("continue".equals(button)) {
                        clearMenuHover();
                        startMenuFade(characterSelectState);
                        return;
                    } else if ("selectCharacter".equals(button)) {
                        clearMenuHover();
                        startMenuFade(characterSelectState);
                        return;
                    }
                }

                if (gameState == characterSelectState) {
                    String selectedCharacter = getCharacterNameAt(p);
                    if (selectedCharacter != null) {
                        selectChar(selectedCharacter);
                    }
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
                if (menuFadeActive) return;
                Point p = e.getPoint();
                String prevButton = hoveredMenuButton;
                int prevChar = hoveredCharIndex;
                if (gameState == titleState) {
                    hoveredMenuButton = getMainMenuButtonAt(p);
                    hoveredCharIndex = -1;
                    if (!hoveredMenuButton.equals(prevButton) || hoveredCharIndex != prevChar) repaint();
                    return;
                }
                if (gameState == startMenuState) {
                    hoveredMenuButton = getStartMenuButtonAt(p);
                    hoveredCharIndex = -1;
                    if (!hoveredMenuButton.equals(prevButton) || hoveredCharIndex != prevChar) repaint();
                    return;
                }
                if (gameState != characterSelectState) return;
                hoveredMenuButton = "";
                hoveredCharIndex = getCharacterIndexAt(p);
                if (hoveredCharIndex != prevChar || !hoveredMenuButton.equals(prevButton)) repaint();
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

    private void loadImages() {
        try {
            mapImage    = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/" + currentMapName + ".png")));
            hitboxImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/" + currentMapName + "Hitboxes.png")));

            jamesStand       = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/james/james_stand.png")));
            alieyandrewStand = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/alieyandrew/alieyandrew_stand.png")));
            kyleStand        = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/kyle/kyle_stand.png")));
            johnruStand      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/johnru/johnru_stand.png")));
            adrianStand      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/adrian/adrian_stand.png")));

            ivanStands = new BufferedImage[4];
            ivanStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/ivan/ivan_front_stand.png")));
            ivanStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/ivan/ivan_left_stand.png")));
            ivanStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/ivan/ivan_back_stand.png")));
            ivanStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/ivan/ivan_right_stand.png")));

            nimuelStands = new BufferedImage[4];
            nimuelStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/nimuel/nimuel_front_stand.png")));
            nimuelStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/nimuel/nimuel_left_stand.png")));
            nimuelStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/nimuel/nimuel_back_stand.png")));
            nimuelStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/nimuel/nimuel_right_stand.png")));

            samStands = new BufferedImage[4];
            samStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/sam/sam_front_stand.png")));
            samStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/sam/sam_left_stand.png")));
            samStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/sam/sam_back_stand.png")));
            samStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/sam/sam_right_stand.png")));

            johnfielStands = new BufferedImage[4];
            johnfielStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/johnfiel/johnfiel_front_stand.png")));
            johnfielStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/johnfiel/johnfiel_left_stand.png")));
            johnfielStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/johnfiel/johnfiel_back_stand.png")));
            johnfielStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/johnfiel/johnfiel_right_stand.png")));
        } catch (Exception e) {
            System.out.println("Image loading failed.");
        }
    }

    private BufferedImage readImage(String path) throws Exception {
        return ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    private BufferedImage makeWhiteTransparent(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                Color color = new Color(argb, true);
                if (color.getRed() > 245 && color.getGreen() > 245 && color.getBlue() > 245) {
                    result.setRGB(x, y, 0x00000000);
                } else {
                    result.setRGB(x, y, argb);
                }
            }
        }
        return result;
    }

    private Point toBasePoint(Point p) {
        int baseX = p.x * 500 / Math.max(1, getWidth());
        int baseY = p.y * 342 / Math.max(1, getHeight());
        return new Point(baseX, baseY);
    }

    private Rectangle toScreenRect(Rectangle baseRect) {
        int x = baseRect.x * getWidth() / 500;
        int y = baseRect.y * getHeight() / 342;
        int w = Math.max(1, baseRect.width * getWidth() / 500);
        int h = Math.max(1, baseRect.height * getHeight() / 342);
        return new Rectangle(x, y, w, h);
    }

    private boolean containsBasePoint(Rectangle baseRect, Point screenPoint) {
        return baseRect.contains(toBasePoint(screenPoint));
    }

    private String getMainMenuButtonAt(Point p) {
        if (containsBasePoint(startBtn, p)) return "start";
        if (containsBasePoint(creditsBtn, p)) return "credits";
        if (containsBasePoint(quitBtn, p)) return "quit";
        if (containsBasePoint(muteBtn, p)) return "mute";
        if (containsBasePoint(settingsBtn, p)) return "settings";
        return "";
    }

    private String getStartMenuButtonAt(Point p) {
        if (containsBasePoint(continueMenuBtn, p)) return "continue";
        if (containsBasePoint(selectCharacterBtn, p)) return "selectCharacter";
        if (containsBasePoint(muteBtn, p)) return "mute";
        if (containsBasePoint(settingsBtn, p)) return "settings";
        return "";
    }

    private int getCharacterIndexAt(Point p) {
        if (containsBasePoint(ivanBtn, p)) return 0;
        if (containsBasePoint(nimuelBtn, p)) return 1;
        if (containsBasePoint(samBtn, p)) return 2;
        if (containsBasePoint(johnfielBtn, p)) return 3;
        return -1;
    }

    private String getCharacterNameAt(Point p) {
        return switch (getCharacterIndexAt(p)) {
            case 0 -> "ivan";
            case 1 -> "nimuel";
            case 2 -> "sam";
            case 3 -> "johnfiel";
            default -> null;
        };
    }

    private void clearMenuHover() {
        hoveredMenuColor = 0;
        hoveredMenuButton = "";
        hoveredCharIndex = -1;
    }

    public void loadMap(String mapName) {
        currentMapName = mapName;
        currentDialog = "";
        dialogStage = 0;
        lastNPCColor = 0;
        pendingBattleEnemyColor = 0;

        loadImages();
        setPlayerSpawn();
        repaint();
    }

    private BufferedImage[] getHoveredImages(int index) {
        switch (index) {
            case 0: return ivanStands;
            case 1: return nimuelStands;
            case 2: return samStands;
            case 3: return johnfielStands;
            default: return null;
        }
    }

    private int getMenuColorAt(Point p, BufferedImage menuHitbox) {
        if (menuHitbox == null || getWidth() <= 0 || getHeight() <= 0) return 0;
        if (p.x < 0 || p.y < 0 || p.x >= getWidth() || p.y >= getHeight()) return 0;

        int imageX = Math.min(menuHitbox.getWidth() - 1, p.x * menuHitbox.getWidth() / getWidth());
        int imageY = Math.min(menuHitbox.getHeight() - 1, p.y * menuHitbox.getHeight() / getHeight());
        return menuHitbox.getRGB(imageX, imageY) & 0xFFFFFF;
    }

    private Rectangle getColorBounds(BufferedImage image, int targetColor) {
        if (image == null) return null;

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == targetColor) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < minX || maxY < minY) return null;
        int sx = minX * getWidth() / image.getWidth();
        int sy = minY * getHeight() / image.getHeight();
        int sw = Math.max(1, (maxX - minX + 1) * getWidth() / image.getWidth());
        int sh = Math.max(1, (maxY - minY + 1) * getHeight() / image.getHeight());
        return new Rectangle(sx, sy, sw, sh);
    }

    private void quitGame() {
        gameThread = null;
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        System.exit(0);
        Runtime.getRuntime().halt(0);
    }

    public void selectChar(String name) {
        if (player != null && player.characterName.equals(name)) {
            clearMenuHover();
            startMenuFade(playState);
            return;
        }

        resetRunForNewCharacter();
        player = new Player(this, keyH, name);
        setPlayerSpawn();
        clearMenuHover();
        startMenuFade(playState);



    }

    private String getCharacterNameForColor(int color) {
        if (color == COLOR_CHAR_IVAN) return "ivan";
        if (color == COLOR_CHAR_NIMUEL) return "nimuel";
        if (color == COLOR_CHAR_SAM) return "sam";
        if (color == COLOR_CHAR_JOHNFIEL) return "johnfiel";
        return null;
    }

    private void resetRunForNewCharacter() {
        currentMapName = START_MAP;
        enemyStats = new EnemyStats();
        currentDialog = "";
        dialogStage = 0;
        lastNPCColor = 0;
        pendingBattleEnemyColor = 0;
        enemyCurrentHP = 0;
        enemyMaxHP = 0;
        battleRound = 1;
        battleMessage = "";
        playerMoveDisplay = "";
        enemyMoveDisplay = "";
        battleResolved = false;
        waitingForNext = false;
        enemyName = "";
        loadImages();
        
    }

    public void setPlayerSpawn() {
        if (hitboxImage == null) return;
        int scaledTileSize = getScaledTileSize();
        for (int y = 0; y < hitboxImage.getHeight(); y++) {
            for (int x = 0; x < hitboxImage.getWidth(); x++) {
                if ((hitboxImage.getRGB(x, y) & 0xFFFFFF) == COLOR_SPAWN) {
                    player.x = (x * getWidth())  / hitboxImage.getWidth()  - (scaledTileSize / 2);
                    player.y = (y * getHeight()) / hitboxImage.getHeight() - (scaledTileSize / 2);
                    player.saveSpawn(player.x, player.y);
                    return;
                }
            }
        }
    }

    public void startFadeToBlack() {
        gameState     = fadeState;
        fadeAlpha     = 0f;
        fadingIn      = true;
        currentDialog = "";
    }

    private void startMenuFade(int targetState) {
        if (gameState == targetState) return;
        menuFadeTargetState = targetState;
        menuFadeAlpha = 0f;
        menuFadingOut = true;
        menuFadeActive = true;
    }

    private void updateMenuFade() {
        if (!menuFadeActive) return;

        if (menuFadingOut) {
            menuFadeAlpha += MENU_FADE_SPEED;
            if (menuFadeAlpha >= 1f) {
                menuFadeAlpha = 1f;
                gameState = menuFadeTargetState;
                clearMenuHover();
                menuFadingOut = false;
            }
        } else {
            menuFadeAlpha -= MENU_FADE_SPEED;
            if (menuFadeAlpha <= 0f) {
                menuFadeAlpha = 0f;
                menuFadeActive = false;
            }
        }
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
        battleMessage     = "Round " + battleRound + " - Choose your move!";
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
                    battleMessage  = "You Win! Dealt " + dmg + " dmg - " + enemyName + " defeated!";
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
                    battleMessage  = "You Lose! Took " + dmg + " dmg - You were defeated! Respawning...";
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
            battleMessage     = "Round " + battleRound + " - Choose your move!";
            playerMoveDisplay = "";
            enemyMoveDisplay  = "";
            waitingForNext    = false;
        }
    }

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
                    if (keyH.f1Pressed && !f1WasPressed) { 
                        showDebug = !showDebug; f1WasPressed = true; }
                    if (!keyH.f1Pressed) f1WasPressed = false;
                }
                if (gameState == fadeState) updateFade();
                if (keyH.escPressed && !escWasPressed) {
                    handleEscapePressed();
                    escWasPressed = true;
                }
                if (!keyH.escPressed) escWasPressed = false;
                updateMenuFade();
                repaint();
                delta--;
            }
        }
    }



    private void handleEscapePressed() {
        if (menuFadeActive) return;

        if (gameState == characterSelectState) {
            clearMenuHover();
            startMenuFade(startMenuState);
        } else if (gameState == startMenuState) {
            clearMenuHover();
            startMenuFade(titleState);
        } else if (gameState == playState) {
            currentDialog = "";
            clearMenuHover();
            startMenuFade(startMenuState);
        }
    }

    public void drawNPCs(Graphics2D g2) {
        if (!START_MAP.equals(currentMapName)) return;
        if (hitboxImage == null) return;

        HashMap<Integer, int[]> colorBounds = new HashMap<>();

        for (int y = 0; y < hitboxImage.getHeight(); y++) {
            for (int x = 0; x < hitboxImage.getWidth(); x++) {
                int color = hitboxImage.getRGB(x, y) & 0xFFFFFF;
                if (color != COLOR_JAMES && color != COLOR_ALIEYANDREW &&
                        color != COLOR_KYLE && color != COLOR_JOHNRU && color != COLOR_ADRIAN) continue;

                if (enemyStats.isDefeated(color)) continue;

                if (!colorBounds.containsKey(color)) {
                    colorBounds.put(color, new int[]{x, x, y, y});
                } else {
                    int[] b = colorBounds.get(color);
                    if (x < b[0]) b[0] = x;
                    if (x > b[1]) b[1] = x;
                    if (y < b[2]) b[2] = y;
                    if (y > b[3]) b[3] = y;
                }
            }
        }

        for (int color : colorBounds.keySet()) {
            int[] b = colorBounds.get(color);
            int scaledTileSize = getScaledTileSize();

            int centerX = (b[0] + b[1]) / 2;
            int centerY = (b[2] + b[3]) / 2;

            int screenX = (centerX * getWidth()) / hitboxImage.getWidth();
            int screenY = (centerY * getHeight()) / hitboxImage.getHeight();

            int drawX = screenX - (scaledTileSize / 2);
            int drawY = screenY - (scaledTileSize / 2);

            BufferedImage img = null;
            if (color == COLOR_JAMES)       img = jamesStand;
            if (color == COLOR_ALIEYANDREW) img = alieyandrewStand;
            if (color == COLOR_KYLE)        img = kyleStand;
            if (color == COLOR_JOHNRU)      img = johnruStand;
            if (color == COLOR_ADRIAN)      img = adrianStand;

            if (img != null) {
                g2.setColor(new Color(0, 0, 0, 100));
                int shadowWidth = (int)(scaledTileSize * 0.6);
                int shadowHeight = (int)(scaledTileSize * 0.15);
                int shadowX = drawX + (scaledTileSize - shadowWidth) / 2;

                int shadowOffset = scaleUniform(15);
                if (color == COLOR_KYLE || color == COLOR_JOHNRU) {
                    shadowOffset = scaleUniform(25);
                } else if (color == COLOR_JAMES) {
                    shadowOffset = scaleUniform(20);
                }

                int shadowY = drawY + scaledTileSize - shadowHeight - shadowOffset;
                g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);

                g2.drawImage(img, drawX, drawY, scaledTileSize, scaledTileSize, null);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if      (gameState == titleState)  drawTitleScreen(g2);
        else if (gameState == startMenuState) drawStartMenuScreen(g2);
        else if (gameState == characterSelectState) drawCharacterSelectScreen(g2);
        else if (gameState == playState)   drawPlayState(g2);
        else if (gameState == fadeState)   drawFadeState(g2);
        else if (gameState == battleState) drawBattleState(g2);

        drawMenuFade(g2);
        g2.dispose();
    }

    private void drawMenuFade(Graphics2D g2) {
        if (!menuFadeActive || menuFadeAlpha <= 0f) return;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, menuFadeAlpha));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawTitleScreen(Graphics2D g2) {
        drawMenuBase(g2);
        drawMenuButton(g2, startBtn, startIdleImage, startHoverImage, "start");
        drawMenuButton(g2, creditsBtn, creditsIdleImage, creditsHoverImage, "credits");
        drawMenuButton(g2, quitBtn, quitIdleImage, quitHoverImage, "quit");
        drawMenuIconButton(g2, muteBtn, muteIdleImage, muteHoverImage, "mute");
        drawMenuIconButton(g2, settingsBtn, settingsIdleImage, settingsHoverImage, "settings");
    }

    private void drawStartMenuScreen(Graphics2D g2) {
        drawMenuBase(g2);
        drawMenuButton(g2, continueMenuBtn, continueIdleImage, continueHoverImage, "continue");
        drawMenuButton(g2, selectCharacterBtn, selectCharacterIdleImage, selectCharacterHoverImage, "selectCharacter");
        drawMenuIconButton(g2, muteBtn, muteIdleImage, muteHoverImage, "mute");
        drawMenuIconButton(g2, settingsBtn, settingsIdleImage, settingsHoverImage, "settings");
    }

    private void drawCharacterSelectScreen(Graphics2D g2) {
        drawMenuBase(g2);
        drawCharacterButton(g2, ivanBtn, 0);
        drawCharacterButton(g2, samBtn, 2);
        drawCharacterButton(g2, nimuelBtn, 1);
        drawCharacterButton(g2, johnfielBtn, 3);
        drawMenuIconButton(g2, muteBtn, muteIdleImage, muteHoverImage, "mute");
        drawMenuIconButton(g2, settingsBtn, settingsIdleImage, settingsHoverImage, "settings");
    }

    private void drawMenuBase(Graphics2D g2) {
        drawMenuImage(g2, menuBackgroundImage);
        drawBaseImage(g2, menuLogoImage, new Rectangle(160, 38, 180, 56));
    }

    private void drawMenuButton(Graphics2D g2, Rectangle bounds, BufferedImage idle, BufferedImage hover, String name) {
        drawBaseImage(g2, name.equals(hoveredMenuButton) ? hover : idle, bounds);
    }

    private void drawMenuIconButton(Graphics2D g2, Rectangle bounds, BufferedImage idle, BufferedImage hover, String name) {
        drawBaseImage(g2, name.equals(hoveredMenuButton) ? hover : idle, bounds);
    }

    private void drawCharacterButton(Graphics2D g2, Rectangle bounds, int index) {
        if (characterButtonIdleImages == null || characterButtonHoverImages == null) return;
        BufferedImage image = hoveredCharIndex == index ? characterButtonHoverImages[index] : characterButtonIdleImages[index];
        drawBaseImage(g2, image, bounds);
    }

    private void drawBaseImage(Graphics2D g2, BufferedImage image, Rectangle baseRect) {
        if (image == null) return;
        Rectangle screenRect = toScreenRect(baseRect);
        g2.drawImage(image, screenRect.x, screenRect.y, screenRect.width, screenRect.height, null);
    }

    private void drawMenuImage(Graphics2D g2, BufferedImage image) {
        if (image != null) {
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void drawMenuHoverOutline(Graphics2D g2, BufferedImage hitboxImage) {
        if (hoveredMenuColor == 0) return;
        Rectangle bounds = getColorBounds(hitboxImage, hoveredMenuColor);
        if (bounds == null) return;

        int pad = Math.max(2, scaleUniform(2));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(Math.max(2, scaleUniform(2))));
        g2.drawRect(bounds.x - pad, bounds.y - pad, bounds.width + pad * 2, bounds.height + pad * 2);
    }

    private void drawStatCard(Graphics2D g2, CharacterStats.CharacterType ct) {
        int cardX = screenWidth / 2 - 200;
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

    private void drawPlayState(Graphics2D g2) {
        if (mapImage != null) {
            g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

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

        // Draw map name GUI
        if (player != null) {
            drawMapNameGUI(g2);
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

    private void drawFadeState(Graphics2D g2) {
        if (mapImage != null) {
            g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

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

    private void drawBattleState(Graphics2D g2) {
        updateBattleButtons();

        g2.setColor(new Color(15, 15, 35));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int midX = getWidth() / 2;

        g2.setColor(new Color(220, 60, 60));
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        String battleTitle = "BATTLE - Round " + battleRound;
        int tw = g2.getFontMetrics().stringWidth(battleTitle);
        g2.drawString(battleTitle, midX - tw / 2, 70);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString(player.characterName, 80, 130);
        drawHPBar(g2, 80, 140, 340, 28, player.currentHP, player.maxHP, "");

        int eHPx = getWidth() - 460;
        g2.setColor(new Color(255, 120, 120));
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.drawString(enemyName, eHPx, 130);
        drawHPBar(g2, eHPx, 140, 340, 28, enemyCurrentHP, enemyMaxHP, "");

        g2.setColor(new Color(255, 200, 50));
        g2.setFont(new Font("Arial", Font.BOLD, 46));
        g2.drawString("VS", midX - 24, 170);

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

        int msgY = 400;
        String[] lines = battleMessage.split("\n");
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        for (String line : lines) {
            int lw = g2.getFontMetrics().stringWidth(line);
            g2.setColor(new Color(255, 230, 100));
            g2.drawString(line, midX - lw / 2, msgY);
            msgY += 36;
        }

        if (!waitingForNext) {
            drawMoveButton(g2, rockBtn,     "ROCK",     new Color(100, 80,  40),  new Color(200, 160, 80));
            drawMoveButton(g2, paperBtn,    "PAPER",    new Color(40,  80,  120), new Color(80,  160, 220));
            drawMoveButton(g2, scissorsBtn, "SCISSORS", new Color(80,  40,  100), new Color(180, 80,  220));
        } else {
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