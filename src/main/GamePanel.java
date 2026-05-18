package main;

import entity.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import static main.HitboxColors.Map.*;
import static main.HitboxColors.Ui.*;

/**
 * Pure orchestrator / game-loop controller.
 *
 * Responsibilities kept here:
 *   • Screen/state constants
 *   • Game-loop thread (run/update)
 *   • Resource loading and image-state sync
 *   • Save / load
 *   • Fade / map-transition
 *   • Narration kickoff
 *   • Player spawn
 *   • Public query API used by Player.java
 *   • Rectangle helpers shared across UIRenderer and InputRouter
 *
 * Responsibilities extracted to dedicated classes:
 *   • AudioManager   – music & SFX
 *   • BattleManager  – battle logic (holds BattleContext)
 *   • NarrationManager – cutscene line state
 *   • UIRenderer     – all painting
 *   • InputRouter    – all mouse click/hover
 */
public class GamePanel extends JPanel implements Runnable {

    // ─────────────────────────────────────────────────────────────
    //  SCREEN CONSTANTS
    // ─────────────────────────────────────────────────────────────
    final int originalTileSize = 16;
    final int scale = 10;
    public final int tileSize    = originalTileSize * scale;
    public int screenWidth       = tileSize * 16;
    public int screenHeight      = tileSize * 12;

    // ─────────────────────────────────────────────────────────────
    //  GAME-STATE CONSTANTS
    // ─────────────────────────────────────────────────────────────
    public int gameState;
    public static final int menuState      = 0;
    public static final int menuStartState = 1;
    public static final int menuCharState  = 2;
    public static final int playState      = 3;
    public static final int fadeState      = 4;
    public static final int battleState    = 5;
    public static final int outcomeState   = 6;
    public static final int creditsState   = 7;
    public static final int inventoryState = 8;
    public static final int abilityState   = 9;
    public static final int winState       = 10;
    public static final int loseState      = 11;
    public static final int preBattleState = 12;
    public static final int narrationState = 13;
    public static final int resultState    = 14;
    public static final int loadingState   = 15;

    // ─────────────────────────────────────────────────────────────
    //  MAP CONSTANTS
    // ─────────────────────────────────────────────────────────────
    public static final String GLE_MAP       = "gle";
    public static final String FRONTGATE_MAP = "frontgate";
    public static final String EMALL_MAP     = "emall";

    // ─────────────────────────────────────────────────────────────
    //  CORE SYSTEMS
    // ─────────────────────────────────────────────────────────────
    public  KeyHandler    keyH  = new KeyHandler();
    public  Player        player;
    final   ImageDisplay  imageDisplay = new ImageDisplay();
    Thread gameThread;

    // ─────────────────────────────────────────────────────────────
    //  MANAGERS  (all same package — cross-reference is fine)
    // ─────────────────────────────────────────────────────────────
    final AudioManager     audio      = new AudioManager();
    final BattleManager    battleMgr  = new BattleManager(this);
    final NarrationManager narMgr     = new NarrationManager();
          UIRenderer       renderer;   // set after construction to pass `this`
          InputRouter      inputRouter;
    public final SpeedrunTimer speedrunTimer = new SpeedrunTimer();
    public final Leaderboard   leaderboard   = new Leaderboard();

    // ─────────────────────────────────────────────────────────────
    //  IMAGE STATE  (package-private — UIRenderer reads, InputRouter touches rects)
    // ─────────────────────────────────────────────────────────────
    public  BufferedImage mapImage;
    public  BufferedImage hitboxImage;
    public  String        currentMapName = GLE_MAP;

    BufferedImage menuMainHitbox;
    BufferedImage menuStartHitbox;
    BufferedImage menuCharHitbox;
    BufferedImage battleHitbox;
    BufferedImage battleSpriteHitbox;
    BufferedImage worldGuiHitbox;
    BufferedImage outcomeHitbox;

    BufferedImage menuScreenImg;
    BufferedImage logoImg;
    BufferedImage mapTitleImg;
    BufferedImage battleSceneImg;
    BufferedImage outcomeSceneImg;
    BufferedImage outcomeRPSImg;
    BufferedImage creditsImg;
    BufferedImage winImg;
    java.awt.Image loadingScreenGif;

    final Map<String, BufferedImage[]> btnImgs      = new HashMap<>();
    final Map<String, BufferedImage>   npcStand     = new HashMap<>();
    final Map<String, BufferedImage>   charSelectImg = new HashMap<>();

    BufferedImage playerBattleImg;
    BufferedImage playerDialogImg;
    BufferedImage enemyBattleImg;
    BufferedImage enemyDialogImg;

    // ─────────────────────────────────────────────────────────────
    //  INPUT / UI STATE  (package-private — InputRouter / UIRenderer)
    // ─────────────────────────────────────────────────────────────
    Point  mouse          = new Point(0, 0);
    String hoveredBtn     = null;
    int    hoveredCharColor = 0;

    boolean settingsOpen     = false;
    boolean quitConfirmOpen  = false;
    boolean quitConfirmToMenu = false;

    // ─────────────────────────────────────────────────────────────
    //  GAME PROGRESSION  (package-private — managers read/write)
    // ─────────────────────────────────────────────────────────────
    public  EnemyStats    enemyStats  = new EnemyStats();
    ItemSystem   items      = new ItemSystem();
    AbilitySystem abilities = new AbilitySystem();
    int    completedFights  = 0;
    SaveData saveData       = null;
    final Random rand       = new Random();

    // ─────────────────────────────────────────────────────────────
    //  DIALOGUE  (public — Player.java reads/writes)
    // ─────────────────────────────────────────────────────────────
    public String currentDialog = "";
    public int    dialogStage   = 0;
    public int    lastNPCColor  = 0;

    // ─────────────────────────────────────────────────────────────
    //  PANEL STATE
    // ─────────────────────────────────────────────────────────────
    int     prevStateBeforePanel = playState;
    private boolean eWasPanelHeld     = false;
    private boolean eWasItemDialogHeld = false;

    // ─────────────────────────────────────────────────────────────
    //  FADE / TRANSITION
    // ─────────────────────────────────────────────────────────────
    float   fadeAlpha          = 0f;
    private boolean fadingIn   = true;
    private String  pendingFadeMapName = null;
    public  int     pendingBattleEnemyColor = 0;

    // ─────────────────────────────────────────────────────────────
    //  MISC
    // ─────────────────────────────────────────────────────────────
    public  boolean showDebug   = false;
    private boolean f1WasHeld   = false;
    private static final int AUTOSAVE_INTERVAL_FRAMES = 180;
    private int autoSaveCountdown = AUTOSAVE_INTERVAL_FRAMES;
    private long loadingStartedAtMillis;
    private int loadingDurationMillis;

    // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────
    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        ToolTipManager.sharedInstance().setEnabled(false);

        // Build managers that need `this`
        renderer    = new UIRenderer(this);
        inputRouter = new InputRouter(this, renderer);

        imageDisplay.loadAll();
        syncImageState();
        audio.debugAudio();
        saveData = SaveData.loadFromDisk();
        leaderboard.loadFromDisk();
        speedrunTimer.loadFromDisk();
        gameState = loadingState;
        loadingStartedAtMillis = System.currentTimeMillis();
        loadingDurationMillis = imageDisplay.getLoadingScreenDurationMillis();
        audio.playMusic("menu_sountrack");

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                inputRouter.onClick(e.getPoint());
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint();
                inputRouter.refreshHover();
                repaint();
            }
            @Override public void mouseDragged(MouseEvent e) {
                mouse = e.getPoint();
                inputRouter.handleDrag(mouse);
                repaint();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  RESOURCE LOADING
    // ─────────────────────────────────────────────────────────────
    void syncImageState() {
        mapImage           = imageDisplay.getMapImage();
        hitboxImage        = imageDisplay.getHitboxImage();
        menuMainHitbox     = imageDisplay.getMenuMainHitbox();
        menuStartHitbox    = imageDisplay.getMenuStartHitbox();
        menuCharHitbox     = imageDisplay.getMenuCharHitbox();
        battleHitbox       = imageDisplay.getBattleHitbox();
        battleSpriteHitbox = imageDisplay.getBattleSpriteHitbox();
        worldGuiHitbox     = imageDisplay.getWorldGuiHitbox();
        outcomeHitbox      = imageDisplay.getOutcomeHitbox();
        menuScreenImg      = imageDisplay.getMenuScreenImg();
        logoImg            = imageDisplay.getLogoImg();
        mapTitleImg        = imageDisplay.getMapTitleImg();
        battleSceneImg     = imageDisplay.getBattleSceneImg();
        outcomeSceneImg    = imageDisplay.getOutcomeSceneImg();
        outcomeRPSImg      = imageDisplay.getOutcomeRPSImg();
        creditsImg         = imageDisplay.getCreditsImg();
        winImg             = imageDisplay.getWinImg();
        loadingScreenGif   = imageDisplay.getLoadingScreenGif();
        btnImgs.clear();      btnImgs.putAll(imageDisplay.getButtonImages());
        npcStand.clear();     npcStand.putAll(imageDisplay.getNpcStandImages());
        charSelectImg.clear();charSelectImg.putAll(imageDisplay.getCharacterSelectImages());
        playerBattleImg = imageDisplay.getPlayerBattleImg();
        playerDialogImg = imageDisplay.getPlayerDialogImg();
        enemyBattleImg  = imageDisplay.getEnemyBattleImg();
        enemyDialogImg  = imageDisplay.getEnemyDialogImg();
    }

    public void loadMapImages(String mapName) {
        currentMapName  = mapName;
        currentDialog   = "";
        dialogStage     = 0;
        lastNPCColor    = 0;
        if (!EMALL_MAP.equals(mapName)) pendingBattleEnemyColor = 0;
        settingsOpen    = false;

        imageDisplay.loadMapImages(mapName);
        syncImageState();
        renderer.clearNpcCache();

        switch (mapName) {
            case GLE_MAP       -> audio.playMusic("gle_soundtrack");
            case FRONTGATE_MAP -> audio.playMusic("frontgate_soundtrack");
            default            -> { /* EMALL_MAP has no music */ }
        }
        if (player != null) setPlayerSpawn();
        autoSave();
        repaint();
    }

    // ─────────────────────────────────────────────────────────────
    //  SAVE / LOAD
    // ─────────────────────────────────────────────────────────────
    public void saveGame() { autoSave(); }

    void autoSave() {
        if (player == null) return;
        saveData = new SaveData(player.characterName, currentMapName,
                player.currentHP, player.maxHP, player.damageMultiplier,
                new HashSet<>(enemyStats.getDefeatedEnemies()),
                new ArrayList<>(items.getFlatList()),
                new ArrayList<>(abilities.getAbilities()),
                completedFights);
        saveData.saveToDisk();
        speedrunTimer.saveToDisk();
    }

    void loadSave() {
        if (saveData == null) return;
        enemyStats  = new EnemyStats();
        items       = new ItemSystem();
        abilities   = new AbilitySystem();
        enemyStats.setDefeated(saveData.defeatedEnemies);
        items.setItems(saveData.items);
        abilities.setAbilities(saveData.abilities);
        completedFights = Math.max(0, saveData.completedFights);
        loadMapImages(saveData.currentMapName);
        player = new Player(this, keyH, saveData.characterName);
        loadPlayerDialogImages();
        player.currentHP        = saveData.playerHP;
        player.maxHP            = saveData.playerMaxHP;
        player.damageMultiplier = saveData.damageMultiplier;
        repairPlayerHpBounds();
        setPlayerSpawn();
        speedrunTimer.loadFromDisk();
        gameState = playState;
    }

    // ─────────────────────────────────────────────────────────────
    //  CHARACTER SELECT
    // ─────────────────────────────────────────────────────────────
    public void selectChar(String name) {
        completeReset();
        
        enemyStats = new EnemyStats();
        items = new ItemSystem();
        abilities = new AbilitySystem();
        completedFights = 0;
        loadMapImages(GLE_MAP);
        player = new Player(this, keyH, name);
        loadPlayerDialogImages();
        setPlayerSpawn();
        gameState = playState;
        autoSave();
    }

    private void loadPlayerDialogImages() {
        if (player == null) return;
        imageDisplay.loadPlayerImages(player.characterName);
        syncImageState();
    }

    public void setPlayerSpawn() {
        if (hitboxImage == null || player == null) return;
        for (int y = 0; y < hitboxImage.getHeight(); y++) {
            for (int x = 0; x < hitboxImage.getWidth(); x++) {
                if ((hitboxImage.getRGB(x, y) & 0xFFFFFF) == COLOR_SPAWN) {
                    player.x = x * getWidth()  / hitboxImage.getWidth()  - tileSize / 2;
                    player.y = y * getHeight() / hitboxImage.getHeight() - tileSize / 2;
                    player.saveSpawn(player.x, player.y);
                    return;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FADE / TRANSITION
    // ─────────────────────────────────────────────────────────────
    public void startFadeToBlack() {
        pendingFadeMapName = null;
        gameState = fadeState; fadeAlpha = 0f; fadingIn = true; currentDialog = "";
    }

    public void startMapTransition(String targetMapName) {
        if (targetMapName == null || targetMapName.isBlank()) return;
        pendingFadeMapName = targetMapName;
        gameState = fadeState; fadeAlpha = 0f; fadingIn = true; currentDialog = "";
    }

    private void updateFade() {
        if (pendingFadeMapName != null) {
            if (fadingIn) {
                fadeAlpha = Math.min(1f, fadeAlpha + 0.03f);
                if (fadeAlpha >= 1f) {
                    String target = pendingFadeMapName;
                    fadingIn = false;
                    loadMapImages(target);
                    gameState = fadeState;
                    fadeAlpha = 1f;
                }
            } else {
                fadeAlpha = Math.max(0f, fadeAlpha - 0.03f);
                if (fadeAlpha <= 0f) { pendingFadeMapName = null; gameState = playState; }
            }
            return;
        }
        if (!fadingIn) return;
        fadeAlpha = Math.min(1f, fadeAlpha + 0.015f);
        if (fadeAlpha >= 1f) { fadingIn = false; battleMgr.startBattle(); }
    }

    // ─────────────────────────────────────────────────────────────
    //  NARRATION
    // ─────────────────────────────────────────────────────────────
    void startNarration() {
        currentMapName = EMALL_MAP;
        imageDisplay.loadBattleImages(EMALL_MAP, COLOR_FINALBOSS, true,
                player != null ? player.characterName : null);
        syncImageState();
        String first = player != null ? BattleManager.cap(player.characterName) : "Player";
        narMgr.initNarration(DialogueDisplay.finalBossNarration(first));
        currentDialog = narMgr.narCurrentLine();
        gameState = narrationState;
    }

    // ─────────────────────────────────────────────────────────────
    //  GAME LOOP
    // ─────────────────────────────────────────────────────────────
    public void startGameThread() { gameThread = new Thread(this); gameThread.start(); }

    @Override
    public void run() {
        double interval = 1_000_000_000.0 / 60;
        double delta = 0;
        long last = System.nanoTime();
        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / interval;
            last = now;
            if (delta >= 1) { update(); repaint(); delta--; }
        }
    }

    private void update() {
        speedrunTimer.tick(gameState);
        if (quitConfirmOpen) return;
        
        if (gameState == loadingState) {
            if (loadingFinished()) {
                gameState = menuState;
            }
            return;
        }

        // Close inventory / ability panel with E key
        if ((gameState == inventoryState || gameState == abilityState)
                && keyH.ePressed && !eWasPanelHeld) {
            eWasPanelHeld = true;
            gameState = prevStateBeforePanel;
            return;
        }
        if (!keyH.ePressed) eWasPanelHeld = false;

        // World movement
        if (gameState == playState && player != null) {
            player.update();
            if (keyH.f1Pressed && !f1WasHeld) { showDebug = !showDebug; f1WasHeld = true; }
            if (!keyH.f1Pressed) f1WasHeld = false;
        }

        // Dismiss battle dialog with E
        if (gameState == battleState && !currentDialog.isEmpty() && keyH.ePressed)
            currentDialog = "";

        // Dismiss item-use feedback dialog in play state with E
        if (gameState == playState && !currentDialog.isEmpty() && lastNPCColor == 0) {
            if (keyH.ePressed && !eWasItemDialogHeld) {
                eWasItemDialogHeld = true;
                currentDialog = "";
                if (player != null) player.eWasPressed = true;
            }
        }
        if (!keyH.ePressed) eWasItemDialogHeld = false;

        // Advance narration lines
        if (gameState == narrationState) {
            boolean done = narMgr.advanceNarration(keyH.ePressed);
            if (done) {
                currentDialog = "";
                pendingBattleEnemyColor = COLOR_FINALBOSS;
                startFadeToBlack();
            } else {
                currentDialog = narMgr.narCurrentLine();
            }
        }

        // Advance pre-battle dialogue (final boss intro)
        if (gameState == preBattleState) {
            if (narMgr.advancePreBattle(keyH.ePressed))
                gameState = battleState;
        }

        if (gameState == fadeState) updateFade();

        // Periodic autosave
        if (player != null && --autoSaveCountdown <= 0) {
            autoSave();
            autoSaveCountdown = AUTOSAVE_INTERVAL_FRAMES;
        }
    }

    private boolean loadingFinished() {
        if (loadingScreenGif == null) return true;
        int duration = Math.max(1000, loadingDurationMillis);
        return System.currentTimeMillis() - loadingStartedAtMillis >= duration;
    }

    // ─────────────────────────────────────────────────────────────
    //  PAINT  (delegates entirely to UIRenderer)
    // ─────────────────────────────────────────────────────────────
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        renderer.render(g2);
        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────
    //  RESET
    // ─────────────────────────────────────────────────────────────
    void resetToMenu() {
        audio.stopMusic();
        player          = null;
        enemyStats      = new EnemyStats();
        items           = new ItemSystem();
        abilities       = new AbilitySystem();
        completedFights = 0;
        saveData        = SaveData.loadFromDisk();
        speedrunTimer.saveToDisk();
        gameState       = menuState;
        audio.playMusic("menu_sountrack");
    }

    void requestQuit(boolean toMenu) {
        quitConfirmOpen  = true;
        quitConfirmToMenu = toMenu;
        settingsOpen      = false;
    }

    public void completeReset() {
        speedrunTimer.stop();
        speedrunTimer.reset();
        
        if (saveData != null) {
            new java.io.File(System.getProperty("user.home") 
                + java.io.File.separator + "fivesix_save.dat").delete();
        }
        saveData = null;
        
        enemyStats = new EnemyStats();
        items = new ItemSystem();
        abilities = new AbilitySystem();
        completedFights = 0;
        player = null;
        battleMgr.ctx.reset();
        currentDialog = "";
        dialogStage = 0;
        lastNPCColor = 0;
        gameState = menuState;
        audio.stopMusic();
        audio.playMusic("menu_sountrack");
        autoSave();
    }
    // ─────────────────────────────────────────────────────────────
    //  HP REPAIR  (called by BattleManager)
    // ─────────────────────────────────────────────────────────────
    void repairPlayerHpBounds() {
        if (player == null) return;
        int baseMax = CharacterStats.CharacterType.fromName(player.characterName).maxHP;
        if (player.maxHP < baseMax) player.maxHP = baseMax;
        player.currentHP = Math.max(0, Math.min(player.currentHP, player.maxHP));
    }

    // ─────────────────────────────────────────────────────────────
    //  PUBLIC QUERY API  (called by Player.java)
    // ─────────────────────────────────────────────────────────────
    public boolean isGleMap()       { return GLE_MAP.equals(currentMapName); }
    public boolean isFrontgateMap() { return FRONTGATE_MAP.equals(currentMapName); }

    public boolean allOtherGleEnemiesDefeated() {
        return enemyStats.isDefeated(COLOR_JAMES) && enemyStats.isDefeated(COLOR_ALIEYANDREW)
            && enemyStats.isDefeated(COLOR_KYLE)  && enemyStats.isDefeated(COLOR_ADRIAN);
    }

    public boolean allOtherFrontgateEnemiesDefeated() {
        return enemyStats.isDefeated(COLOR_DARRYLL) && enemyStats.isDefeated(COLOR_GIO)
            && enemyStats.isDefeated(COLOR_YOHANN)  && enemyStats.isDefeated(COLOR_DIRK)
            && enemyStats.isDefeated(COLOR_JAKE);
    }

    // ─────────────────────────────────────────────────────────────
    //  BOTTOM NAV  (shared by UIRenderer + InputRouter)
    // ─────────────────────────────────────────────────────────────
    boolean bottomNavVisible() {
        if (settingsOpen || quitConfirmOpen) return false;
        if (battleMgr.ctx.isFinalBoss
                && (gameState == battleState || gameState == outcomeState || gameState == preBattleState))
            return false;
        return gameState == menuStartState || gameState == menuCharState
            || gameState == battleState   || gameState == outcomeState
            || gameState == preBattleState;
    }

    Rectangle bottomNavRect() {
        int w = Math.max(122, getWidth() / 11);
        int h = Math.max(34,  getHeight() / 25);
        int pad = Math.max(12, getWidth() / 80);
        return new Rectangle(getWidth() - w - pad, getHeight() - h - pad, w, h);
    }

    String bottomNavLabel() {
        return switch (gameState) {
            case menuStartState -> "MAIN MENU";
            case menuCharState  -> "BACK";
            case battleState, outcomeState, preBattleState -> "LEAVE";
            default -> "";
        };
    }

    // ─────────────────────────────────────────────────────────────
    //  SETTINGS / QUIT RECTS  (shared helpers)
    // ─────────────────────────────────────────────────────────────
    Rectangle settingsPanelRect() {
        return new Rectangle(getWidth() / 2 - 220, getHeight() / 2 - 160, 440, 320);
    }

    Rectangle settingsMuteRect() {
        Rectangle p = settingsPanelRect();
        return new Rectangle(p.x + 50, p.y + 62, 120, 44);
    }

    Rectangle musicSliderTrack() {
        Rectangle p = settingsPanelRect();
        return new Rectangle(p.x + 30, p.y + 134, p.width - 108, 16);
    }

    Rectangle sfxSliderTrack() {
        Rectangle p = settingsPanelRect();
        return new Rectangle(p.x + 30, p.y + 204, p.width - 108, 16);
    }

    Rectangle quitConfirmRect() {
        int w = 520, h = 220;
        return new Rectangle(getWidth() / 2 - w / 2, getHeight() / 2 - h / 2, w, h);
    }

    Rectangle quitYesRect() {
        Rectangle r = quitConfirmRect();
        return new Rectangle(r.x + 70, r.y + 145, 160, 44);
    }

    Rectangle quitNoRect() {
        Rectangle r = quitConfirmRect();
        return new Rectangle(r.x + r.width - 230, r.y + 145, 160, 44);
    }

    Rectangle fixedMenuSettingsRect() {
        int x = 16  * getWidth()  / 500;
        int y = 309 * getHeight() / 342;
        int w = Math.max(1, 62 * getWidth()  / 500);
        int h = Math.max(1, 22 * getHeight() / 342);
        return new Rectangle(x, y, w, h);
    }

    Rectangle settingsWorldRect() {
        Rectangle r = renderer.bounds(worldGuiHitbox, BC_MUTE);
        if (r == null) return null;
        int w = Math.min(r.width, Math.max(1, (int)(r.height * 2.4)));
        return new Rectangle(r.x + (r.width - w) / 2, r.y, w, r.height);
    }

    // ─────────────────────────────────────────────────────────────
    //  CHARACTER-BUTTON HELPERS  (shared by UIRenderer + InputRouter)
    // ─────────────────────────────────────────────────────────────
    String charButtonAt(Point p) {
        for (String name : java.util.List.of("ivan", "sam", "nimuel", "johnfiel")) {
            Rectangle r = charButtonRect(name);
            if (r != null && r.contains(p)) return name;
        }
        return null;
    }

    Rectangle charButtonRect(String name) {
        if (menuCharHitbox != null) {
            int c = HitboxColors.charColor(name);
            if (c != 0) {
                Rectangle r = renderer.bounds(menuCharHitbox, c);
                if (r != null) return r;
            }
        }
        int x   = 42 * getWidth()  / 500;
        int w   = 92 * getWidth()  / 500;
        int h   = 28 * getHeight() / 342;
        int gap = 10 * getHeight() / 342;
        int top = 76 * getHeight() / 342;
        int index = switch (name) {
            case "ivan" -> 0; case "sam" -> 1; case "nimuel" -> 2; case "johnfiel" -> 3; default -> -1;
        };
        if (index < 0) return null;
        return new Rectangle(x, top + index * (h + gap), Math.max(1, w), Math.max(1, h));
    }

    Rectangle charPreviewRect() {
        if (menuCharHitbox != null) {
            Rectangle r = renderer.bounds(menuCharHitbox, BC_HOVCHAR);
            if (r != null) return r;
        }
        int w = Math.max(1, (int)(getWidth()  * 0.34));
        int h = Math.max(1, (int)(getHeight() * 0.62));
        return new Rectangle(getWidth() / 2 - w / 2, Math.max(1, (int)(getHeight() * 0.20)), w, h);
    }
}
