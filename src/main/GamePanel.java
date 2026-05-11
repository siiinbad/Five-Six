package main;

import entity.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import static main.HitboxColors.Map.*;
import static main.HitboxColors.Ui.*;

public class GamePanel extends JPanel implements Runnable {

    // SCREEN SETTINGS
    final int originalTileSize = 16;
    final int scale = 10;
    public final int tileSize = originalTileSize * scale;
    public int screenWidth  = tileSize * 16;
    public int screenHeight = tileSize * 12;

    // SYSTEM
    public KeyHandler keyH = new KeyHandler();
    Thread gameThread;
    public Player player;
    private final ImageDisplay imageDisplay = new ImageDisplay();

    // MAP
    public BufferedImage mapImage;
    public BufferedImage hitboxImage;       // NPC + wall + spawn colors
    public String currentMapName = "gle";
    public static final String GLE_MAP       = "gle";
    public static final String FRONTGATE_MAP = "frontgate";
    public static final String EMALL_MAP     = "emall";

    // GAME STATES
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
    public static final int resultState = 14;

    // HITBOX IMAGES (reused, small files kept in RAM)
    private BufferedImage menuMainHitbox;
    private BufferedImage menuStartHitbox;
    private BufferedImage menuCharHitbox;
    private BufferedImage battleHitbox;
    private BufferedImage battleSpriteHitbox;
    private BufferedImage worldGuiHitbox;
    private BufferedImage outcomeHitbox;

    // SCENE IMAGES (one loaded at a time)
    private BufferedImage menuScreenImg;
    private BufferedImage logoImg;
    private BufferedImage mapTitleImg;
    private BufferedImage battleSceneImg;
    private BufferedImage outcomeSceneImg;
    private BufferedImage outcomeRPSImg;
    private BufferedImage creditsImg;
    private BufferedImage winImg;

    // BUTTON IMAGES  key -> [idle, hover]
    private final Map<String, BufferedImage[]> btnImgs = new HashMap<>();

    // NPC STAND IMAGES
    private final Map<String, BufferedImage> npcStand = new HashMap<>();

    // CHARACTER SELECT PREVIEW
    private final Map<String, BufferedImage> charSelectImg = new HashMap<>();

    // BATTLE SPRITES
    private BufferedImage playerBattleImg;
    private BufferedImage playerDialogImg;
    private BufferedImage enemyBattleImg;
    private BufferedImage enemyDialogImg;

    // MOUSE
    private Point mouse = new Point(0, 0);
    private String hoveredBtn = null;
    private int    hoveredCharColor = 0;

    // SETTINGS / MUTE
    public boolean settingsOpen = false;
    public boolean isMuted      = false;
    private String musicTrack   = "";
    private Thread musicThread  = null;
    private volatile boolean stopMusicRequested = false;
    public float musicVolume = 0.25f;  // 0.0 to 1.0
    public float sfxVolume   = 0.5f;  // 0.0 to 1.0


    // SAVE
    private SaveData saveData = null;



    private static final int AUTOSAVE_INTERVAL_FRAMES = 180;

    // DEBUG
    public boolean showDebug   = false;
    private boolean f1WasHeld  = false;

    // DIALOG
    public String currentDialog = "";
    public int dialogStage      = 0;
    public int lastNPCColor     = 0;
    private int talkShake       = 0;
    private boolean eWasItemDialogHeld = false;

    // NARRATION (post-vaughn)
    private boolean narrating    = false;
    private String[] narLines    = {};
    private int      narIndex    = 0;
    private boolean eWasNarHeld  = false;

    // PRE-BATTLE DIALOGUE (final boss intro)
    private String[] preBattleLines = {};
    private int      preBattleIndex = 0;
    private boolean  eWasPreBattleHeld = false;

    // FADE
    private float   fadeAlpha  = 0f;
    private boolean fadingIn   = true;
    public int pendingBattleEnemyColor = 0;

    // BATTLE
    public EnemyStats     enemyStats    = new EnemyStats();
    private int   enemyHP               = 0;
    private int   enemyMaxHP            = 0;
    private double enemyDamageMultiplier = 1.0;
    private int   battleRound           = 1;
    private String battleMsg            = "";
    private boolean battleResolved      = false;
    private boolean waitingOutcome      = false;
    private String  enemyName           = "";
    private boolean isFinalBoss         = false;
    private BattleSystem.Move lastPMove, lastEMove;

    // ITEMS / ABILITIES
    private ItemSystem    items     = new ItemSystem();
    private AbilitySystem abilities = new AbilitySystem();
    private String abilMsg          = "";
    private int    abilMsgTimer     = 0;
    private List<String> lastRewardItems    = new ArrayList<>();
    private List<String> lastRewardAbils    = new ArrayList<>();
    private boolean      showRewardsBox     = false;

    // ACTIVE EFFECTS
    private boolean fxUnoReverse   = false;
    private boolean fxHypnotize    = false;
    private boolean fxYouCheater   = false;
    private boolean fxFullCounter  = false;
    private int     fxHealRounds   = 0;
    private int     fxHealAmt      = 0;

    // CLAIRVOYANCE
    private boolean clairVisible   = false;
    private String  clairText      = "";
    private BattleSystem.Move clairMove = null;  // the actual predicted move used in next resolve()

    // Tracks which state opened the item/ability panel so the panel closes correctly
    private int prevStateBeforePanel = playState;
    private int autoSaveCountdown = AUTOSAVE_INTERVAL_FRAMES;
    private int completedFights = 0;
    private boolean eWasPanelHeld = false;
    private boolean quitConfirmOpen = false;
    private boolean quitConfirmToMenu = false;

    private final Random rand = new Random();

    // ─────────────────────────────────────────────────────────────
    //  CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────
    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        gameState = menuState;
        // Disable Swing tooltips (prevents hover text labels from popping up).
        ToolTipManager.sharedInstance().setEnabled(false);

        imageDisplay.loadAll();
        syncImageState();
        debugAudio();
        saveData = SaveData.loadFromDisk();  // restore save across sessions
        playMusic("menu_sountrack");

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onClick(e.getPoint()); }
        });
        addMouseMotionListener(new MouseAdapter() {
    @Override public void mouseMoved(MouseEvent e) {
        mouse = e.getPoint();
        refreshHover();
        repaint();
    }
    @Override public void mouseDragged(MouseEvent e) {
        mouse = e.getPoint();
        if (settingsOpen) {
            if (musicSliderTrack().contains(e.getPoint()) ||
                (e.getPoint().y >= musicSliderTrack().y && e.getPoint().y <= musicSliderTrack().y + 16)) {
                Rectangle t = musicSliderTrack();
                if (e.getPoint().x >= t.x && e.getPoint().x <= t.x + t.width) {
                    musicVolume = sliderValue(t, e.getPoint());
                    applyMusicVolume();
                }
            }
            if (sfxSliderTrack().contains(e.getPoint()) ||
                (e.getPoint().y >= sfxSliderTrack().y && e.getPoint().y <= sfxSliderTrack().y + 16)) {
                Rectangle t = sfxSliderTrack();
                if (e.getPoint().x >= t.x && e.getPoint().x <= t.x + t.width) {
                    sfxVolume = sliderValue(t, e.getPoint());
                }
            }
        }
        repaint();
    }
});
    }

    // ─────────────────────────────────────────────────────────────
    //  RESOURCE LOADING
    // ─────────────────────────────────────────────────────────────
    private void syncImageState() {
        mapImage = imageDisplay.getMapImage();
        hitboxImage = imageDisplay.getHitboxImage();
        menuMainHitbox = imageDisplay.getMenuMainHitbox();
        menuStartHitbox = imageDisplay.getMenuStartHitbox();
        menuCharHitbox = imageDisplay.getMenuCharHitbox();
        battleHitbox = imageDisplay.getBattleHitbox();
        battleSpriteHitbox = imageDisplay.getBattleSpriteHitbox();
        worldGuiHitbox = imageDisplay.getWorldGuiHitbox();
        outcomeHitbox = imageDisplay.getOutcomeHitbox();
        menuScreenImg = imageDisplay.getMenuScreenImg();
        logoImg = imageDisplay.getLogoImg();
        mapTitleImg = imageDisplay.getMapTitleImg();
        battleSceneImg = imageDisplay.getBattleSceneImg();
        outcomeSceneImg = imageDisplay.getOutcomeSceneImg();
        outcomeRPSImg = imageDisplay.getOutcomeRPSImg();
        creditsImg = imageDisplay.getCreditsImg();
        winImg = imageDisplay.getWinImg();
        btnImgs.clear();
        btnImgs.putAll(imageDisplay.getButtonImages());
        npcStand.clear();
        npcStand.putAll(imageDisplay.getNpcStandImages());
        charSelectImg.clear();
        charSelectImg.putAll(imageDisplay.getCharacterSelectImages());
        playerBattleImg = imageDisplay.getPlayerBattleImg();
        playerDialogImg = imageDisplay.getPlayerDialogImg();
        enemyBattleImg = imageDisplay.getEnemyBattleImg();
        enemyDialogImg = imageDisplay.getEnemyDialogImg();
    }

    public void loadMapImages(String mapName) {
        currentMapName = mapName;
        currentDialog  = "";
        dialogStage    = 0;
        lastNPCColor   = 0;
        if (!EMALL_MAP.equals(mapName)) pendingBattleEnemyColor = 0;
        settingsOpen   = false;
        narrating      = false;

        imageDisplay.loadMapImages(mapName);
        syncImageState();

        switch (mapName) {
            case GLE_MAP -> {
                playMusic("gle_soundtrack");
            }
            case FRONTGATE_MAP -> {
                playMusic("frontgate_soundtrack");
            }
            case EMALL_MAP -> { }
        }
        if (player != null) setPlayerSpawn();
        autoSave();
        repaint();
    }

    // ─────────────────────────────────────────────────────────────
    //  MUSIC
    // ─────────────────────────────────────────────────────────────
    private volatile Clip activeMusicClip = null;


    private void playMusic(String name) {
    if (name == null || name.isBlank()) return;
    if (name.equals(musicTrack) && musicThread != null && musicThread.isAlive()) return;
    stopMusic();
    musicTrack = name;
    if (isMuted) return;
    stopMusicRequested = false;
    musicThread = new Thread(() -> {
        while (!stopMusicRequested) {
            try {
                InputStream raw = openMusicStream(name);
                if (raw == null) return;
                AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float)(Math.log10(Math.max(0.0001, musicVolume)) * 20.0);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }
                activeMusicClip = clip;
                clip.start();
                // Wait until clip finishes or stop is requested
                while (!stopMusicRequested) {
                    Thread.sleep(100);
                    if (!clip.isRunning() && !clip.isActive()) break;
                }
                clip.stop();
                clip.close();
                activeMusicClip = null;
            } catch (Exception e) {
                activeMusicClip = null;
                return;
            }
        }
    }, "music-" + name);
    musicThread.setDaemon(true);
    musicThread.start();
}

    private void stopMusic() {
    stopMusicRequested = true;
    Clip clip = activeMusicClip;
    if (clip != null) {
        try { clip.stop(); clip.close(); } catch (Exception ignored) {}
    }
    Thread t = musicThread;
    if (t != null && t.isAlive()) {
        try { t.join(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        if (t.isAlive()) t.interrupt();
    }
    musicThread = null;
    activeMusicClip = null;
}

    private void applyVolume() {
    if (isMuted) {
        stopMusic();
    } else {
        // Restart music from scratch when unmuting
        String track = musicTrack;
        musicTrack = ""; // force playMusic to not skip due to same track check
        playMusic(track);
    }
}

    public void toggleMute() { isMuted = !isMuted; applyVolume(); }

    private void playSFX(String name) {
    if (isMuted || sfxVolume <= 0f) return;
    Thread t = new Thread(() -> {
        try {
            InputStream raw = getClass().getResourceAsStream("/res/soundtrack/" + name + ".wav");
            if (raw == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float)(Math.log10(Math.max(0.0001, sfxVolume)) * 20.0);
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
            }
            clip.start();
            // Wait for clip to finish instead of drain()
            while (!clip.isRunning()) Thread.sleep(10);
            while (clip.isRunning()) Thread.sleep(10);
            clip.close();
        } catch (Exception ignored) {}
    }, "sfx-" + name);
    t.setDaemon(true);
    t.start();
}



    private InputStream openMusicStream(String name) {
    String[] paths = {
        "/res/soundtrack/" + name + ".wav",
        "/res/soundTrack/" + name + ".wav",
        "/res/soundtrack/" + name + ".mp3",
        "/res/soundTrack/" + name + ".mp3"
    };
    for (String path : paths) {
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) return stream;
    }
    return null;
}

    // ─────────────────────────────────────────────────────────────
    //  SAVE / LOAD
    // ─────────────────────────────────────────────────────────────
    public void saveGame() {
        autoSave();
    }

    private void autoSave() {
        if (player == null) return;
        saveData = new SaveData(player.characterName, currentMapName,
                player.currentHP, player.maxHP, player.damageMultiplier,
                new HashSet<>(enemyStats.getDefeatedEnemies()),
                new ArrayList<>(items.getFlatList()),
                new ArrayList<>(abilities.getAbilities()),
                completedFights);
        saveData.saveToDisk();
    }

    private void loadSave() {
        if (saveData == null) return;
        enemyStats = new EnemyStats();
        items      = new ItemSystem();
        abilities  = new AbilitySystem();
        enemyStats.setDefeated(saveData.defeatedEnemies);
        items.setItems(saveData.items);
        abilities.setAbilities(saveData.abilities);
        completedFights = Math.max(0, saveData.completedFights);
        player = new Player(this, keyH, saveData.characterName);
        loadPlayerDialogImages();
        player.currentHP        = saveData.playerHP;
        player.maxHP            = saveData.playerMaxHP;
        player.damageMultiplier = saveData.damageMultiplier;
        loadMapImages(saveData.currentMapName);
        gameState = playState;
    }

    // ─────────────────────────────────────────────────────────────
    //  HITBOX COLOR LOOKUP
    // ─────────────────────────────────────────────────────────────
    private int colorAt(BufferedImage hb, Point p) {
        if (hb == null) return 0;
        int ix = p.x * hb.getWidth()  / Math.max(1, getWidth());
        int iy = p.y * hb.getHeight() / Math.max(1, getHeight());
        if (ix < 0 || ix >= hb.getWidth() || iy < 0 || iy >= hb.getHeight()) return 0;
        return hb.getRGB(ix, iy) & 0xFFFFFF;
    }

    private void refreshHover() {
    String prevHovered = hoveredBtn;
    hoveredBtn = null;
    hoveredCharColor = 0;
    if (settingsOpen && settingsMuteRect().contains(mouse)) {
        hoveredBtn = isMuted ? "wmuted" : "wmute";
    } else if ((gameState == menuState || gameState == menuCharState) && fixedMenuSettingsRect().contains(mouse)) {
        hoveredBtn = "settings";
    } else if (gameState == menuCharState) {
        String charHover = charButtonAt(mouse);
        if (charHover != null) {
            hoveredBtn = charHover;
            hoveredCharColor = charColor(charHover);
        }
    } else if (gameState == playState || gameState == inventoryState || gameState == abilityState) {
        hoveredBtn = worldButtonAt(mouse);
    } else {
        int c = switch (gameState) {
            case menuState      -> colorAt(menuMainHitbox,  mouse);
            case menuStartState -> colorAt(menuStartHitbox, mouse);
            case menuCharState  -> colorAt(menuCharHitbox,  mouse);
            case battleState    -> colorAt(battleHitbox,    mouse);
            case outcomeState   -> colorAt(outcomeHitbox,   mouse);
            default -> 0;
        };
        if (gameState == menuCharState && c == BC_HOVCHAR) {
            hoveredCharColor = nearestCharColor(mouse);
        }
        hoveredBtn = c2k(c);
    }

    // Play hover SFX only when moving onto a NEW button
    if (hoveredBtn != null && !hoveredBtn.equals(prevHovered)) {
        playSFX("hover");
    }
}
    /** Scan nearby hitbox pixels to find which character slot is actually nearest */
    private int nearestCharColor(Point p) {
        if (menuCharHitbox == null) return 0;
        int cx = p.x * menuCharHitbox.getWidth()  / Math.max(1,getWidth());
        int cy = p.y * menuCharHitbox.getHeight() / Math.max(1,getHeight());
        int[] ccs = HitboxColors.characterButtonColors();
        for (int r = 0; r <= 10; r++) {
            for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) {
                if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                int px = cx+dx, py = cy+dy;
                if (px<0||px>=menuCharHitbox.getWidth()||py<0||py>=menuCharHitbox.getHeight()) continue;
                int col = menuCharHitbox.getRGB(px,py)&0xFFFFFF;
                for (int cc : ccs) if (col==cc) return cc;
            }
        }
        return 0;
    }

    private String c2k(int c) {
        return switch (c) {
            case BC_START    -> "start";
            case BC_CREDIT   -> "credits";
            case BC_QUIT     -> "quit";
            case BC_SETTINGS -> "settings";
            case BC_MUTE     -> (isMuted ? "wmuted" : "wmute");
            case BC_SAVE     -> "save";
            case BC_CONTINUE -> "continue";
            case BC_SELCHAR  -> "selchar";
            case BC_IVAN     -> "ivan";
            case BC_SAM      -> "sam";
            case BC_NIMUEL   -> "nimuel";
            case BC_JOHNFIEL -> "johnfiel";
            case BC_HOVCHAR  -> "ivan"; // placeholder; actual char from hoveredCharColor
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

    private String worldButtonAt(Point p) {
        Rectangle settingsR = settingsWorldRect();
        if (settingsR != null && settingsR.contains(p)) return "wset";
        int c = colorAt(worldGuiHitbox, p);
        return switch (c) {
            case BC_ITEMS  -> "item_inv";
            case BC_ABILINV -> "abil_inv";
            case BC_BACK   -> "backmenu";
            case BC_SAVE   -> "wsave";
            default -> null;
        };
    }

    // ─────────────────────────────────────────────────────────────
    //  MOUSE CLICK
    // ─────────────────────────────────────────────────────────────
private void onClick(Point p) {
    playSFX("click");
    if (quitConfirmOpen) {
        quitConfirmClick(p);
        return;
    }
    if (settingsOpen) {
        settingsClick(p);
        return;
    }
    if ((gameState == menuState || gameState == menuCharState) && fixedMenuSettingsRect().contains(p)) {
        settingsOpen = true;
        return;
    }
    if (gameState == inventoryState) {
        handleItemClick(p);
        return;
    }
    if (gameState == abilityState) {
        handleAbilityClick(p);
        return;
    }
    int c;
    switch (gameState) {
        case menuState      -> { c = colorAt(menuMainHitbox, p);  menuClick(c); }
        case menuStartState -> { c = colorAt(menuStartHitbox, p); menuStartClick(c); }
        case menuCharState  -> { c = colorAt(menuCharHitbox, p);  charClick(c, p); }
        case playState      -> worldClick(p);
        case fadeState      -> { }
        case battleState    -> { c = colorAt(battleHitbox, p); battleClick(c, p); }
        case outcomeState   -> { if (colorAt(outcomeHitbox, p) == BC_CONTBAT) nextRound(); }
        case creditsState   -> gameState = menuState;
        case winState       -> { resetToMenu(); }
        case loseState      -> { resetToMenu(); }
        case resultState    -> resultClick(p);
    }
}

    private void resetToMenu() {
        stopMusic();
        player = null;
        enemyStats = new EnemyStats();
        items = new ItemSystem();
        abilities = new AbilitySystem();
        completedFights = 0;
        saveData = null;
        gameState = menuState;
        playMusic("menu_sountrack");
    }

    private void menuClick(int c) {
        switch (c) {
            case BC_START  -> gameState = menuStartState;
            case BC_CREDIT -> gameState = creditsState;
            case BC_QUIT   -> requestQuit(false);
        }
    }

    private void menuStartClick(int c) {
        switch (c) {
            case BC_CONTINUE -> { if (saveData != null) loadSave(); }
            case BC_SELCHAR  -> gameState = menuCharState;
            case BC_SETTINGS -> settingsOpen = !settingsOpen;
        }
    }

    private void charClick(int c, Point p) {
        String name = charButtonAt(p);
        if (name == null) {
            name = switch (c) {
                case BC_IVAN     -> "ivan";
                case BC_SAM      -> "sam";
                case BC_NIMUEL   -> "nimuel";
                case BC_JOHNFIEL -> "johnfiel";
                case BC_HOVCHAR  -> charName(nearestCharColor(p));
                default -> null;
            };
        }
        if (name != null) selectChar(name);
    }

    private void worldClick(Point p) {
    if (gameState == inventoryState || gameState == abilityState) return;
    String key = worldButtonAt(p);
    if (key == null) return;
    switch (key) {
        case "item_inv" -> { prevStateBeforePanel = playState; toggleState(inventoryState); }
        case "abil_inv" -> { prevStateBeforePanel = playState; toggleState(abilityState); }
        case "backmenu" -> requestQuit(true);
        case "wsave"    -> { autoSave(); abilMsg = "Game saved!"; abilMsgTimer = 120; }
        case "wset" -> settingsOpen = true;
    }
}

    private void toggleState(int s) {
        gameState = (gameState == s) ? playState : s;
    }

    private void battleClick(int c, Point p) {
    if (gameState == inventoryState) return;
    if (gameState == abilityState) return;
    if (!currentDialog.isEmpty()) {
        currentDialog = "";
        return;
    }
    if (!waitingOutcome) {
        switch (c) {
            case BC_ROCKBTN  -> resolve(BattleSystem.Move.ROCK);
            case BC_PAPERBTN -> resolve(BattleSystem.Move.PAPER);
            case BC_SCISSBTN -> resolve(BattleSystem.Move.SCISSORS);
            case BC_USEITEM  -> { if (!items.isEmpty()) { prevStateBeforePanel = battleState; gameState = inventoryState; } }
            case BC_USEABIL  -> { if (!abilities.isEmpty()) { prevStateBeforePanel = battleState; gameState = abilityState; } }
        }
    }
}

    private void settingsClick(Point p) {
    Rectangle panelR = settingsPanelRect();
    if (settingsMuteRect().contains(p)) {
        toggleMute();
    } else if (musicSliderTrack().contains(p)) {
        musicVolume = sliderValue(musicSliderTrack(), p);
        applyMusicVolume();
    } else if (sfxSliderTrack().contains(p)) {
        sfxVolume = sliderValue(sfxSliderTrack(), p);
    } else if (!panelR.contains(p)) {
        settingsOpen = false;
    }
}

private float sliderValue(Rectangle track, Point p) {
    float v = (float)(p.x - track.x) / track.width;
    return Math.max(0f, Math.min(1f, v));
}

private Rectangle musicSliderTrack() {
    Rectangle panel = settingsPanelRect();
    return new Rectangle(panel.x + 30, panel.y + 134, panel.width - 60, 16);
}

    private Rectangle sfxSliderTrack() {
    Rectangle panel = settingsPanelRect();
    return new Rectangle(panel.x + 30, panel.y + 204, panel.width - 60, 16);
}

    private void requestQuit(boolean toMenu) {
        quitConfirmOpen = true;
        quitConfirmToMenu = toMenu;
        settingsOpen = false;
    }

    private void quitConfirmClick(Point p) {
        if (quitYesRect().contains(p)) {
            if (quitConfirmToMenu) {
                autoSave();
                gameState = menuState;
                stopMusic();
                playMusic("menu_sountrack");
                currentDialog = "";
            } else {
                System.exit(0);
            }
            quitConfirmOpen = false;
        } else if (quitNoRect().contains(p) || !quitConfirmRect().contains(p)) {
            quitConfirmOpen = false;
        }
    }

    private Rectangle quitConfirmRect() {
        int w = 520, h = 220;
        return new Rectangle(getWidth() / 2 - w / 2, getHeight() / 2 - h / 2, w, h);
    }

    private Rectangle quitYesRect() {
        Rectangle r = quitConfirmRect();
        return new Rectangle(r.x + 70, r.y + 145, 160, 44);
    }

    private Rectangle quitNoRect() {
        Rectangle r = quitConfirmRect();
        return new Rectangle(r.x + r.width - 230, r.y + 145, 160, 44);
    }

    private void resultClick(Point p) {
    if (battleResolved) {
        if (player.currentHP <= 0) {
            player.respawnWithPenalty();
        }
        if (pendingBattleEnemyColor == COLOR_VAUGHN && player.currentHP > 0) {
            startNarration(); return;
        }
        gameState = playState;
        currentDialog = ""; dialogStage = 0; lastNPCColor = 0;
        battleResolved = false; waitingOutcome = false;
        playMusic(GLE_MAP.equals(currentMapName) ? "gle_soundtrack" : "frontgate_soundtrack");
        autoSave();
    }
}
    private void handleItemClick(Point p) {
    int pw = 520, ph = 420;
    int px = getWidth() / 2 - pw / 2;
    int py = getHeight() / 2 - ph / 2;

    List<ItemSystem.Item> list = items.getItems();
    int iy = py + 88; // matches paintInventory
    for (ItemSystem.Item item : list) {
        if (new Rectangle(px + 20, iy - 28, pw - 40, 38).contains(p)) {
            useItem(item);
            return;
        }
        iy += 48;
        if (iy > py + ph - 44) break;
    }
    // Inside panel but missed all rows — keep open
    if (new Rectangle(px, py, pw, ph).contains(p)) return;
    // Outside panel — close
    gameState = prevStateBeforePanel;
}

    private void handleAbilityClick(Point p) {
    int pw = 560, ph = 440;
    int px = getWidth() / 2 - pw / 2;
    int py = getHeight() / 2 - ph / 2;

    List<AbilitySystem.Ability> unique = abilities.getUnique();
    int ay = py + 88; // matches paintAbility
    for (AbilitySystem.Ability ab : unique) {
        if (new Rectangle(px + 16, ay - 26, pw - 32, 36).contains(p)) {
            useAbility(ab);
            return;
        }
        ay += 46;
        if (ay > py + ph - 40) break;
    }
    // Inside panel but missed all rows — keep open
    if (new Rectangle(px, py, pw, ph).contains(p)) return;
    // Outside panel — close
    gameState = prevStateBeforePanel;
}

    // ─────────────────────────────────────────────────────────────
    //  CHARACTER SELECT
    // ─────────────────────────────────────────────────────────────
    public void selectChar(String name) {
    // Always start fresh — Continue button is for loading saves
    enemyStats = new EnemyStats();
    items      = new ItemSystem();
    abilities  = new AbilitySystem();
    completedFights = 0;
    player = new Player(this, keyH, name);
    loadPlayerDialogImages();
    loadMapImages(GLE_MAP);
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
                    player.x = x * getWidth()  / hitboxImage.getWidth()  - tileSize/2;
                    player.y = y * getHeight() / hitboxImage.getHeight() - tileSize/2;
                    player.saveSpawn(player.x, player.y);
                    return;
                }
            }
        }
    }

    private String charName(int c) {
        return HitboxColors.charName(c);
    }

    private int charColor(String name) {
        return HitboxColors.charColor(name);
    }

    private Rectangle charPreviewRect() {
        // Prefer the dedicated preview area from the hitbox PNG (green area).
        if (menuCharHitbox != null) {
            Rectangle r = bounds(menuCharHitbox, BC_HOVCHAR);
            if (r != null) return r;
        }
        // Fallback: centered preview area.
        int w = Math.max(1, (int) (getWidth() * 0.34));
        int h = Math.max(1, (int) (getHeight() * 0.62));
        int x = getWidth() / 2 - w / 2;
        int y = Math.max(1, (int) (getHeight() * 0.20));
        return new Rectangle(x, y, w, h);
    }

    private Rectangle charButtonRect(String name) {
        // Preferred: use the hitbox PNG regions so the layout matches the reference screen.
        if (menuCharHitbox != null) {
            int c = charColor(name);
            if (c != 0) {
                Rectangle r = bounds(menuCharHitbox, c);
                if (r != null) return r;
            }
        }
        // Fallback: a simple vertical list.
        int x = 42 * getWidth() / 500;
        int w = 92 * getWidth() / 500;
        int h = 28 * getHeight() / 342;
        int gap = 10 * getHeight() / 342;
        int top = 76 * getHeight() / 342;
        int index = switch (name) {
            case "ivan" -> 0;
            case "sam" -> 1;
            case "nimuel" -> 2;
            case "johnfiel" -> 3;
            default -> -1;
        };
        if (index < 0) return null;
        return new Rectangle(x, top + index * (h + gap), Math.max(1, w), Math.max(1, h));
    }

    private String charButtonAt(Point p) {
        for (String name : List.of("ivan", "sam", "nimuel", "johnfiel")) {
            Rectangle r = charButtonRect(name);
            if (r != null && r.contains(p)) return name;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  FADE
    // ─────────────────────────────────────────────────────────────
    private Rectangle charButtonDrawRect(String name) {
        Rectangle assigned = charButtonRect(name);
        if (assigned == null) return null;
        Dimension size = characterButtonDrawSize();
        int w = Math.min(assigned.width, size.width);
        int h = Math.min(assigned.height, size.height);
        return new Rectangle(assigned.x + (assigned.width - w) / 2,
                assigned.y + (assigned.height - h) / 2, w, h);
    }

    private Dimension characterButtonDrawSize() {
        int w = Integer.MAX_VALUE;
        int h = Integer.MAX_VALUE;
        for (String name : List.of("ivan", "sam", "nimuel", "johnfiel")) {
            Rectangle r = charButtonRect(name);
            if (r == null) continue;
            w = Math.min(w, r.width);
            h = Math.min(h, r.height);
        }
        if (w == Integer.MAX_VALUE || h == Integer.MAX_VALUE) {
            return new Dimension(Math.max(1, 92 * getWidth() / 500),
                    Math.max(1, 28 * getHeight() / 342));
        }
        return new Dimension(w, h);
    }

    public void startFadeToBlack() {
        gameState = fadeState; fadeAlpha = 0f; fadingIn = true; currentDialog = "";
    }

    private void updateFade() {
        if (!fadingIn) return;
        fadeAlpha = Math.min(1f, fadeAlpha + 0.015f);
        if (fadeAlpha >= 1f) {
            fadingIn = false;
            startBattle();
            // NOTE: gameState is set inside startBattle() — do NOT override it here
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  BATTLE
    // ─────────────────────────────────────────────────────────────
    private void startBattle() {
        enemyMaxHP     = enemyStats.getEnemyHP(pendingBattleEnemyColor, completedFights);
        enemyHP        = enemyMaxHP;
        enemyDamageMultiplier = 1.0 + completedFights * 0.025;
        battleRound    = 1;
        battleMsg      = "Round " + battleRound + " - Choose your move!";
        battleResolved = false;
        waitingOutcome = false;
        isFinalBoss    = (pendingBattleEnemyColor == COLOR_FINALBOSS);
        enemyName      = enemyName(pendingBattleEnemyColor);

        imageDisplay.loadBattleImages(currentMapName, pendingBattleEnemyColor, isFinalBoss,
                player != null ? player.characterName : null);
        syncImageState();
        playMusic("battle_sountrack");

        // Final boss gets a pre-battle dialogue before the fight begins
        if (isFinalBoss) {
            String playerFirst = (player != null) ? capitalize(player.characterName) : "Player";
            preBattleLines = DialogueDisplay.finalBossPreBattle(playerFirst);
            preBattleIndex = 0;
            eWasPreBattleHeld = false;
            gameState = preBattleState;
        } else {
            gameState = battleState;
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String enemyName(int c) {
        return HitboxColors.enemyName(c);
    }

    private String enemyFolder(int c) {
        return HitboxColors.enemyFolder(c);
    }

    private void resolve(BattleSystem.Move pm) {
        BattleSystem.Move em;
        if (fxHypnotize) {
            em = BattleSystem.Move.ROCK;
            fxHypnotize = false;
        } else if (clairMove != null) {
            em = clairMove;   // clairvoyance locked in the enemy's move
            clairMove = null;
        } else {
            em = BattleSystem.getRandomEnemyMove();
        }
        clairVisible = false;
        lastPMove = pm; lastEMove = em;

        BattleSystem.BattleResult result = BattleSystem.resolve(pm, em);

        if (result == BattleSystem.BattleResult.ENEMY_WIN && fxYouCheater) {
            fxYouCheater = false;
            battleMsg = "You Cheater activated! Round voided - pick again.";
            return;
        }

        if (fxHealRounds > 0) {
            player.currentHP = Math.min(player.currentHP + fxHealAmt, player.maxHP);
            fxHealRounds--;
        }

        double dm = player.damageMultiplier;

        switch (result) {
            case PLAYER_WIN -> {
                int dmg = (int) Math.max(5, (rand.nextInt(10)+1) * dm);
                if (fxFullCounter) { dmg *= 2; fxFullCounter = false; }
                fxUnoReverse = false;
                enemyHP = Math.max(0, enemyHP - dmg);
                if (enemyHP <= 0) {
                    int healed = healPlayerAfterEnemyDefeat();
                    battleMsg = "You Win! Dealt " + dmg + " dmg — " + enemyName + " defeated!";
                    if (healed > 0) battleMsg += " Healed " + healed + " HP.";
                    battleResolved = true;
                    enemyStats.markDefeated(pendingBattleEnemyColor);
                    grantRewards();
                } else {
                    battleMsg = "You Win! Dealt " + dmg + " damage to " + enemyName + ".";
                }
            }
            case ENEMY_WIN -> {
                int dmg = (int) Math.max(4, Math.ceil(((rand.nextInt(10)+1) * enemyDamageMultiplier) / Math.max(0.1, player.damageMultiplier)));
                if (isFinalBoss) dmg = (int)(dmg * 1.5);
                if (fxUnoReverse || fxFullCounter) {
                    int ref = fxFullCounter ? dmg*2 : dmg;
                    fxUnoReverse = false; fxFullCounter = false;
                    enemyHP = Math.max(0, enemyHP - ref);
                    battleMsg = "Reflected " + ref + " damage back!";
                    if (enemyHP <= 0) {
                        int healed = healPlayerAfterEnemyDefeat();
                        battleMsg += " " + enemyName + " defeated!";
                        if (healed > 0) battleMsg += " Healed " + healed + " HP.";
                        battleResolved = true;
                        enemyStats.markDefeated(pendingBattleEnemyColor);
                        grantRewards();
                    }
                } else {
                    player.currentHP = Math.max(0, player.currentHP - dmg);
                    if (player.currentHP <= 0) { battleMsg = "You Lose! Took " + dmg + " dmg — Defeated!"; battleResolved = true; }
                    else battleMsg = "You Lose! Took " + dmg + " damage from " + enemyName + ".";
                }
            }
            case DRAW -> battleMsg = "Draw! No damage dealt.";
        }

        String pp = pm.name().toLowerCase(), ep = em.name().toLowerCase();
        imageDisplay.loadOutcomeImage(pp, ep);
        syncImageState();
        waitingOutcome = true;
        playSFX("Move_Sound");
        gameState = outcomeState;
    }

    private int healPlayerAfterEnemyDefeat() {
        if (player == null || player.currentHP <= 0) return 0;
        int before = player.currentHP;
        int amount = Math.max(1, (int) Math.ceil(player.maxHP * 0.05));
        player.currentHP = Math.min(player.maxHP, player.currentHP + amount);
        return player.currentHP - before;
    }

    private void grantRewards() {
    lastRewardItems.clear();
    lastRewardAbils.clear();

    if (!isFinalBoss) {
        int ic = rand.nextInt(6) + 1;
        for (int i = 0; i < ic; i++) {
            ItemSystem.Item it = items.addRandom(rand);
            if (it != null) lastRewardItems.add(it.displayName);
        }
        int ac = rand.nextDouble() < 0.3 ? rand.nextInt(4) + 1 : 1;
        for (int i = 0; i < ac; i++) {
            AbilitySystem.Ability ab = abilities.addRandom(rand);
            if (ab != null) lastRewardAbils.add(ab.displayName);
        }
    }

    showRewardsBox = !isFinalBoss;
    autoSave();
}

    private void nextRound() {
    showRewardsBox = false;
    if (battleResolved) {
        boolean playerWon = player != null && player.currentHP > 0;

        // Final boss win
        if (isFinalBoss && playerWon) {
            completedFights++;
            autoSave();
            stopMusic();
            playSFX("Win_Final_Boss");
            new java.io.File(System.getProperty("user.home") + java.io.File.separator + "fivesix_save.dat").delete();
            saveData = null;
            gameState = winState;
            return;
        }
        // Final boss loss
        if (isFinalBoss && !playerWon) {
            stopMusic();
            playSFX("Loss_Final_Boss");
            enemyStats = new EnemyStats();
            items      = new ItemSystem();
            abilities  = new AbilitySystem();
            completedFights = 0;
            saveData   = null;
            new java.io.File(System.getProperty("user.home") + java.io.File.separator + "fivesix_save.dat").delete();
            gameState = loseState;
            return;
        }
        // Normal win
        if (playerWon) {
            completedFights++;
            autoSave();
            stopMusic();
            playSFX("Win_Normal");
        }
        // Normal loss
        if (!playerWon) {
            stopMusic();
            playSFX("Loss_Normal");
        }
        gameState = resultState;
        return;
    }
    gameState = battleState;
    battleRound++;
    battleMsg = "Round " + battleRound + " - Choose your move!";
    waitingOutcome = false;
}

    // ─────────────────────────────────────────────────────────────
    //  NARRATION (post-Vaughn → final boss)
    // ─────────────────────────────────────────────────────────────
    private void startNarration() {
        currentMapName = EMALL_MAP;
        // Pre-load the emall battle scene so paintNarration and paintPreBattle both have it
        String playerKey = (player != null) ? player.characterName : null;
        imageDisplay.loadBattleImages(EMALL_MAP, COLOR_FINALBOSS, true, playerKey);
        syncImageState();

        narrating = true; narIndex = 0; eWasNarHeld = false;
        String playerFirst = (player != null) ? capitalize(player.characterName) : "Player";
        narLines = DialogueDisplay.finalBossNarration(playerFirst);
        currentDialog = narLines[0];
        gameState = narrationState;
    }


    // ─────────────────────────────────────────────────────────────
    //  ITEMS / ABILITIES
    // ─────────────────────────────────────────────────────────────
    private void useItem(ItemSystem.Item item) {
    boolean inBattle = prevStateBeforePanel == battleState;

    if (item == ItemSystem.Item.GREENCROSS && !inBattle) {
        currentDialog = "Greencross can only be used during battle.";
        lastNPCColor = 0;
        gameState = prevStateBeforePanel;
        return;
    }

    if (isHealingItem(item) && item != ItemSystem.Item.GREENCROSS
            && player != null && player.currentHP >= player.maxHP) {
        currentDialog = "You are already at full HP!";
        lastNPCColor = 0;
        gameState = prevStateBeforePanel;
        return;
    }

    items.remove(item);
    switch (item) {
        case WATER         -> player.currentHP = Math.min(player.currentHP + player.maxHP / 10, player.maxHP);
        case BARNUTS       -> player.currentHP = Math.min(player.currentHP + 5, player.maxHP);
        case GREENCROSS    -> { fxHealRounds = 3; fxHealAmt = 5; player.currentHP = Math.max(1, player.currentHP - 2); }
        case COFFEE        -> player.maxHP += 10;
        case ENERGY_DRINK  -> player.damageMultiplier += 0.05;
        case SLEEPING_MASK -> player.currentHP = player.maxHP;
    }
    currentDialog = capitalize(player.characterName) + " used " + item.displayName + ". " + item.description;
    lastNPCColor = 0;
    gameState = prevStateBeforePanel;
    autoSave();
}

    private boolean isHealingItem(ItemSystem.Item item) {
        return switch (item) {
            case WATER, BARNUTS, GREENCROSS, SLEEPING_MASK -> true;
            case COFFEE, ENERGY_DRINK -> false;
        };
    }

    private void useAbility(AbilitySystem.Ability ability) {
    if (prevStateBeforePanel != battleState) return;

    abilities.remove(ability);
    String effect = ability.description;
    switch (ability) {
        case CLAIRVOYANCE -> {
            clairMove = BattleSystem.getRandomEnemyMove();
            String moveName = switch (clairMove) {
                case ROCK     -> "Rock";
                case PAPER    -> "Paper";
                case SCISSORS -> "Scissors";
            };
            clairText = enemyName + " will pick " + moveName + " next turn.";
            effect = enemyName + " will pick " + moveName + " next turn.";
            clairVisible = true;
        }
        case UNO_REVERSE  -> fxUnoReverse  = true;
        case HYPNOTIZE    -> fxHypnotize   = true;
        case YOU_CHEATER  -> fxYouCheater  = true;
        case FULL_COUNTER -> fxFullCounter = true;
    }
    currentDialog = capitalize(player.characterName) + " used " + ability.displayName + ". " + effect;
    lastNPCColor = 0;
    gameState = prevStateBeforePanel;
    autoSave();
}

    private void showPlayerUseDialog(String name, String effect) {
        if (playerDialogImg == null) loadPlayerDialogImages();
        String who = (player != null) ? capitalize(player.characterName) : "Player";
        currentDialog = who + " used " + name + ". " + effect;
        lastNPCColor = 0;
    }

    // ─────────────────────────────────────────────────────────────
    //  GAME LOOP
    // ─────────────────────────────────────────────────────────────
    public void startGameThread() { gameThread = new Thread(this); gameThread.start(); }

    @Override
    public void run() {
        double interval = 1_000_000_000.0 / 60;
        double delta = 0; long last = System.nanoTime();
        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / interval; last = now;
            if (delta >= 1) { update(); repaint(); delta--; }
        }
    }

    private void update() {
        talkShake++;
        if (abilMsgTimer > 0) abilMsgTimer--;
        if (quitConfirmOpen) return;
        if ((gameState == inventoryState || gameState == abilityState) && keyH.ePressed && !eWasPanelHeld) {
            eWasPanelHeld = true;
            gameState = prevStateBeforePanel;
            return;
        }
        if (!keyH.ePressed) eWasPanelHeld = false;
        if (gameState == playState && player != null) {
    player.update();
    if (keyH.f1Pressed && !f1WasHeld) { showDebug = !showDebug; f1WasHeld = true; }
    if (!keyH.f1Pressed) f1WasHeld = false;
}
        if (gameState == battleState && !currentDialog.isEmpty() && keyH.ePressed) {
            currentDialog = "";
        }
        if (gameState == playState && !currentDialog.isEmpty() && lastNPCColor == 0) {
    if (keyH.ePressed && !eWasItemDialogHeld) {
        eWasItemDialogHeld = true;
        currentDialog = "";
        if (player != null) player.eWasPressed = true; // block NPC trigger on same keypress
    }
}
if (!keyH.ePressed) eWasItemDialogHeld = false;
        if (!keyH.ePressed) eWasItemDialogHeld = false;
        // Narration advance (dedicated state — player cannot move here)
        if (gameState == narrationState) {
            if (keyH.ePressed && !eWasNarHeld) {
                eWasNarHeld = true;
                narIndex++;
                if (narIndex >= narLines.length) {
                    narrating = false; currentDialog = "";
                    pendingBattleEnemyColor = COLOR_FINALBOSS;
                    startFadeToBlack();
                } else {
                    currentDialog = narLines[narIndex];
                }
            }
            if (!keyH.ePressed) eWasNarHeld = false;
        }
        // Pre-battle dialogue advance (final boss intro)
        if (gameState == preBattleState) {
            if (keyH.ePressed && !eWasPreBattleHeld) {
                eWasPreBattleHeld = true;
                preBattleIndex++;
                if (preBattleIndex >= preBattleLines.length) {
                    // Done — start the actual battle
                    gameState = battleState;
                }
            }
            if (!keyH.ePressed) eWasPreBattleHeld = false;
        }
        if (gameState == fadeState) updateFade();
        if (player != null && --autoSaveCountdown <= 0) {
            autoSave();
            autoSaveCountdown = AUTOSAVE_INTERVAL_FRAMES;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PAINT DISPATCH
    // ─────────────────────────────────────────────────────────────
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        switch (gameState) {
            case menuState      -> paintMenu(g2);
            case menuStartState -> paintMenuStart(g2);
            case menuCharState  -> paintCharSelect(g2);
            case playState      -> paintPlay(g2);
            case fadeState      -> paintFade(g2);
            case battleState    -> paintBattle(g2);
            case outcomeState   -> paintOutcome(g2);
            case creditsState   -> paintCredits(g2);
            case inventoryState -> paintInventory(g2);
            case abilityState   -> paintAbility(g2);
            case winState       -> paintWin(g2);
            case loseState      -> paintLose(g2);
            case preBattleState -> paintPreBattle(g2);
            case narrationState -> paintNarration(g2);
            case resultState -> paintResult(g2);
        }
        if (quitConfirmOpen) paintQuitConfirm(g2);
        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────
    //  DRAWING HELPERS
    // ─────────────────────────────────────────────────────────────
    private void fill(Graphics2D g2, BufferedImage im) {
        if (im != null) g2.drawImage(im, 0, 0, getWidth(), getHeight(), null);
    }

    private Color popupFill() { return new Color(198, 158, 104, 242); }
    private Color popupBorder() { return new Color(112, 83, 54); }
    private Color popupRow() { return new Color(226, 190, 134, 235); }
    private Color popupRowHover() { return new Color(244, 210, 154, 245); }
    private Font pixelFont(int style, int size) { return new Font("Monospaced", style, size); }

    private void drawPopupBox(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(popupFill());
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(popupBorder());
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x, y, w, h, 10, 10);
    }

    private void drawImageFit(Graphics2D g2, BufferedImage image, Rectangle box) {
        if (image == null || box == null || box.width <= 0 || box.height <= 0) return;
        double scale = Math.min(box.width / (double) image.getWidth(), box.height / (double) image.getHeight());
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int x = box.x + (box.width - w) / 2;
        int y = box.y + (box.height - h) / 2;
        g2.drawImage(image, x, y, w, h, null);
    }

    private Rectangle dialogPortraitRect() {
        return (worldGuiHitbox != null) ? bounds(worldGuiHitbox, BC_CHARDIAL) : null;
    }

    private boolean drawDialogPortrait(Graphics2D g2, BufferedImage image, Rectangle fallback) {
        if (image == null) return false;
        Rectangle dialR = dialogPortraitRect();
        if (dialR != null) {
            drawImageFit(g2, image, dialR);
        } else {
            drawImageFit(g2, image, fallback);
        }
        return true;
    }

    private Rectangle dialogTextRect(Rectangle box, Rectangle portraitRect, boolean hasPortrait) {
        int pad = 20;
        Rectangle text = new Rectangle(box.x + pad, box.y + 40, box.width - pad * 2, box.height - 64);
        if (!hasPortrait || portraitRect == null || !portraitRect.intersects(box)) return text;

        int leftW = portraitRect.x - text.x - 18;
        int rightX = portraitRect.x + portraitRect.width + 18;
        int rightW = text.x + text.width - rightX;
        if (rightW >= leftW && rightW > 160) {
            text.x = rightX;
            text.width = rightW;
        } else if (leftW > 160) {
            text.width = leftW;
        }
        return text;
    }

    private boolean isFinalBossLine(String line) {
        return line.startsWith("Beggar:") || line.startsWith("???:") || line.startsWith(enemyName + ":");
    }

    private void drawHoverText(Graphics2D g2, String text) {
        if (text == null || text.isBlank()) return;
        int tw = 500, th = 96, tx = getWidth() / 2 - tw / 2, ty = getHeight() - th - 34;
        drawPopupBox(g2, tx, ty, tw, th);
        g2.setFont(pixelFont(Font.BOLD, 16));
        g2.setColor(new Color(56, 36, 24));
        int y = ty + 30;
        for (String ln : wrap(g2, text, tw - 32)) {
            g2.drawString(ln, tx + 16, y);
            y += 22;
        }
    }

    /** Draw a button image (idle or hover) stretched to fill its hitbox color region */
    private void drawBtn(Graphics2D g2, BufferedImage hb, int color, String key) {
        if (hb == null) return;
        BufferedImage[] arr = btnImgs.get(key);
        if (arr == null) return;
        boolean hov = key.equals(hoveredBtn);
        BufferedImage bim = (hov && arr[1] != null) ? arr[1] : arr[0];
        if (bim == null) return;
        Rectangle r = bounds(hb, color);
        if (r != null) g2.drawImage(bim, r.x, r.y, r.width, r.height, null);
    }

    private void drawBattleMoveBtn(Graphics2D g2, int color, String key, Dimension size) {
        BufferedImage[] arr = btnImgs.get(key);
        if (arr == null || battleHitbox == null) return;
        Rectangle r = bounds(battleHitbox, color);
        if (r == null) return;
        BufferedImage bim = (key.equals(hoveredBtn) && arr[1] != null) ? arr[1] : arr[0];
        if (bim == null) return;
        int w = Math.min(r.width, size.width);
        int h = Math.min(r.height, size.height);
        int x = r.x + (r.width - w) / 2;
        int y = r.y + (r.height - h) / 2;
        g2.drawImage(bim, x, y, w, h, null);
    }

    // Cache bounds lookups — recomputed only when screen size changes
    private final Map<Long, Rectangle> boundsCache = new HashMap<>();
    private int cachedW = 0, cachedH = 0;

    /** Compute screen-space bounding rectangle of all pixels of a given color in a hitbox image */
    private Rectangle bounds(BufferedImage hb, int targetColor) {
        if (hb == null) return null;
        if (getWidth() != cachedW || getHeight() != cachedH) {
            boundsCache.clear(); cachedW = getWidth(); cachedH = getHeight();
        }
        long key = ((long) System.identityHashCode(hb) << 32) | (targetColor & 0xFFFFFFFFL);
        return boundsCache.computeIfAbsent(key, k -> computeBounds(hb, targetColor));
    }

    private Rectangle computeBounds(BufferedImage hb, int targetColor) {
        int mnX=Integer.MAX_VALUE, mxX=0, mnY=Integer.MAX_VALUE, mxY=0; boolean f=false;
        for (int y=0;y<hb.getHeight();y++) for (int x=0;x<hb.getWidth();x++) {
            if ((hb.getRGB(x,y)&0xFFFFFF)==targetColor) {
                if(x<mnX)mnX=x; if(x>mxX)mxX=x; if(y<mnY)mnY=y; if(y>mxY)mxY=y; f=true;
            }
        }
        if (!f) return null;
        int W=getWidth(), H=getHeight(), hw=hb.getWidth(), hh=hb.getHeight();
        return new Rectangle(mnX*W/hw, mnY*H/hh, (mxX-mnX+1)*W/hw, (mxY-mnY+1)*H/hh);
    }

    private void hpBar(Graphics2D g2, int x, int y, int w, int h, int cur, int max, String lbl) {
        g2.setColor(new Color(0,0,0,160));
        g2.fillRoundRect(x-4,y-4,w+8,h+24,10,10);
        g2.setColor(new Color(80,0,0));
        g2.fillRoundRect(x,y+16,w,h,6,6);
        float r = max>0 ? (float)cur/max : 0;
        g2.setColor(r>.5f?new Color(60,200,80):r>.25f?new Color(230,180,0):new Color(220,50,50));
        g2.fillRoundRect(x,y+16,(int)(w*r),h,6,6);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial",Font.BOLD,16));
        String txt = lbl.isEmpty() ? cur+"/"+max : lbl+"  "+cur+"/"+max;
        g2.drawString(txt, x, y+14);
    }

    private void dialog(Graphics2D g2, String text) {
        int bx=60, by=getHeight()-200, bw=getWidth()-120, bh=150;
        drawPopupBox(g2,bx,by,bw,bh);

        // Portrait — stretched to fit #6377FF region in worldGuiHitbox if available,
        // otherwise fall back to fixed position inside dialog box
        BufferedImage port = null;
        if (lastNPCColor != 0) {
            String f = enemyFolder(lastNPCColor);
            if (f != null) port = imageDisplay.getNpcDialogImage(f);
        } else if (player != null) {
            port = playerDialogImg;
        }

        Rectangle fallbackPort = new Rectangle(bx + 12, by + (bh - 110) / 2, 80, 110);
        boolean hasPortrait = drawDialogPortrait(g2, port, fallbackPort);

        Rectangle dialogBox = new Rectangle(bx, by, bw, bh);
        Rectangle dialR = dialogPortraitRect();
        Rectangle portraitRect = (dialR != null) ? dialR : fallbackPort;
        Rectangle textRect = dialogTextRect(dialogBox, portraitRect, hasPortrait);
        textRect.y = by + 18;
        textRect.height = bh - 52;
        drawDialogText(g2, text, textRect);
        g2.setFont(pixelFont(Font.ITALIC,16)); g2.setColor(new Color(80, 56, 38));
        g2.drawString("Press 'E' to continue...", bx+bw-260, by+bh-18);
    }

    private void drawDialogText(Graphics2D g2, String text, Rectangle textRect) {
        int size = 21;
        List<String> lines;
        FontMetrics fm;
        do {
            g2.setFont(pixelFont(Font.BOLD, size));
            fm = g2.getFontMetrics();
            lines = wrap(g2, text, textRect.width);
            if (lines.size() * (fm.getHeight() + 3) <= textRect.height || size <= 15) break;
            size--;
        } while (true);

        g2.setColor(new Color(52, 35, 24));
        int lineH = fm.getHeight() + 3;
        int y = textRect.y + fm.getAscent();
        for (String ln : lines) {
            if (y > textRect.y + textRect.height) break;
            g2.drawString(ln, textRect.x, y);
            y += lineH;
        }
    }

    private List<String> wrap(Graphics2D g2, String text, int maxW) {
        List<String> out=new ArrayList<>();
        if(text==null||text.isEmpty()) return out;
        FontMetrics fm=g2.getFontMetrics();
        for (String para : text.split("\n")) {
            StringBuilder cur=new StringBuilder();
            for (String w : para.split(" ")) {
                String t=cur.length()==0?w:cur+" "+w;
                if(fm.stringWidth(t)>maxW){if(cur.length()>0)out.add(cur.toString());cur=new StringBuilder(w);}
                else cur=new StringBuilder(t);
            }
            if(cur.length()>0)out.add(cur.toString());
        }
        return out;
    }

    private void statCard(Graphics2D g2, CharacterStats.CharacterType ct) {
        int cx=getWidth()-360,cy=280,cw=320,ch=220;
        g2.setColor(new Color(20,20,50,230)); g2.fillRoundRect(cx,cy,cw,ch,20,20);
        g2.setColor(new Color(150,150,255)); g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(cx,cy,cw,ch,20,20);
        int tx=cx+24,ty=cy+44,lh=42;
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial",Font.BOLD,26)); g2.drawString(ct.displayName,tx,ty);
        ty+=lh-8; g2.setColor(new Color(180,220,255)); g2.setFont(new Font("Arial",Font.ITALIC,19));
        g2.drawString("\" "+ct.description+" \"",tx,ty);
        ty+=lh; g2.setColor(new Color(100,255,120)); g2.setFont(new Font("Arial",Font.BOLD,22));
        g2.drawString("HP:  "+ct.maxHP,tx,ty);
        ty+=lh-4; g2.setColor(new Color(255,180,80));
        g2.drawString(ct.damageMultiplier>=999?"DMG: 999 (Unlimited)":String.format("DMG: x%.1f",ct.damageMultiplier),tx,ty);
    }

    // ─────────────────────────────────────────────────────────────
    //  MENU SCREENS
    // ─────────────────────────────────────────────────────────────
    private void paintMenu(Graphics2D g2) {
        fill(g2, menuScreenImg);
        // Logo (not a clickable button — just image)
        if (logoImg != null) {
            Rectangle lr = bounds(menuMainHitbox, BC_LOGO);
            if (lr != null) g2.drawImage(logoImg, lr.x, lr.y, lr.width, lr.height, null);
        }
        drawBtn(g2, menuMainHitbox, BC_START,  "start");
        drawBtn(g2, menuMainHitbox, BC_CREDIT, "credits");
        drawBtn(g2, menuMainHitbox, BC_QUIT,   "quit");
        drawMenuSettingsButton(g2);
        if (settingsOpen) paintSettingsPanel(g2);
    }

    private void paintMenuStart(Graphics2D g2) {
        fill(g2, menuScreenImg);
        drawLogo(g2, menuStartHitbox);
        drawBtn(g2, menuStartHitbox, BC_CONTINUE, "continue");
        drawBtn(g2, menuStartHitbox, BC_SELCHAR,  "selchar");
        drawBtn(g2, menuStartHitbox, BC_SETTINGS, "settings");
        if (settingsOpen) paintSettingsPanel(g2);
    }

    private void paintCharSelect(Graphics2D g2) {
        fill(g2, menuScreenImg);
        drawLogo(g2, menuCharHitbox);

        for (String name : List.of("ivan", "sam", "nimuel", "johnfiel")) {
            BufferedImage[] arr = btnImgs.get(name);
            Rectangle r = charButtonDrawRect(name);
            if (arr == null || r == null) continue;
            BufferedImage img = name.equals(hoveredBtn) && arr[1] != null ? arr[1] : arr[0];
            if (img != null) drawImageFit(g2, img, r);
        }

        String hcn = charName(hoveredCharColor);
        if (hcn == null && hoveredBtn != null && charColor(hoveredBtn) != 0) hcn = hoveredBtn;

        // Only show the character selection preview while hovering a character slot/button.
        if (hcn != null) {
            BufferedImage prev = charSelectImg.get(hcn);
            Rectangle preview = charPreviewRect();
            if (prev != null) {
                // Keep aspect ratio and center it.
                double ar = prev.getWidth() / (double) Math.max(1, prev.getHeight());
                int th = preview.height;
                int tw = Math.max(1, (int) Math.round(th * ar));
                if (tw > preview.width) { tw = preview.width; th = Math.max(1, (int) Math.round(tw / ar)); }
                int x = preview.x + (preview.width - tw) / 2;
                int y = preview.y + (preview.height - th) / 2;
                g2.drawImage(prev, x, y, tw, th, null);
            }
        }

        // Intentionally no hover text overlay here (no "TANK/HP/DMG" labels).
    }

    private void drawLogo(Graphics2D g2, BufferedImage hitbox) {
        if (logoImg == null || hitbox == null) return;
        Rectangle lr = bounds(hitbox, BC_LOGO);
        if (lr != null) g2.drawImage(logoImg, lr.x, lr.y, lr.width, lr.height, null);
    }

    private void drawOutlinedText(Graphics2D g2, String text, int centerX, int y, Font font) {
        if (text == null || text.isEmpty()) return;
        g2.setFont(font);
        int w = g2.getFontMetrics().stringWidth(text);
        int x = centerX - w / 2;
        g2.setColor(Color.BLACK);
        for (int oy = -3; oy <= 3; oy++) for (int ox = -3; ox <= 3; ox++) {
            if (ox == 0 && oy == 0) continue;
            if (Math.abs(ox) + Math.abs(oy) > 4) continue;
            g2.drawString(text, x + ox, y + oy);
        }
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
    }

    private void paintCredits(Graphics2D g2) {
        if (creditsImg != null) {
            fill(g2, creditsImg);
            return;
        }
        drawPlaceholderScreen(g2, "CREDITS PLACEHOLDER");
    }

    private void paintSettingsPanel(Graphics2D g2) {
    Rectangle panel = settingsPanelRect();
    int px = panel.x, py = panel.y, pw = panel.width, ph = panel.height;

    drawPopupBox(g2, px, py, pw, ph);

    // Title
    g2.setColor(new Color(52, 35, 24));
    g2.setFont(pixelFont(Font.BOLD, 26));
    String title = "SETTINGS";
    g2.drawString(title, px + pw/2 - g2.getFontMetrics().stringWidth(title)/2, py + 40);

    // Divider
    g2.setColor(new Color(112, 83, 54, 120));
    g2.setStroke(new BasicStroke(1f));
    g2.drawLine(px + 20, py + 50, px + pw - 20, py + 50);

    // Mute button
    String mk = isMuted ? "wmuted" : "wmute";
    BufferedImage[] ma = btnImgs.get(mk);
    boolean hoverMute = settingsMuteRect().contains(mouse);
    BufferedImage muteImg = (ma != null) ? ((hoverMute && ma[1] != null) ? ma[1] : ma[0]) : null;
    if (muteImg != null) {
        Rectangle muteRect = settingsMuteRect();
        g2.drawImage(muteImg, muteRect.x, muteRect.y, muteRect.width, muteRect.height, null);
    }
    g2.setColor(new Color(52, 35, 24));
    g2.setFont(pixelFont(Font.PLAIN, 18));
    g2.drawString(isMuted ? "Unmute" : "Mute", px + 190, py + 88);

    // Music volume slider
    drawSlider(g2, px + 30, py + 120, pw - 60, "Music Volume", musicVolume);

    // SFX volume slider
    drawSlider(g2, px + 30, py + 190, pw - 60, "SFX Volume", sfxVolume);

    // Dismiss hint
    g2.setFont(pixelFont(Font.ITALIC, 13));
    g2.setColor(new Color(80, 56, 38));
    String hint = "Click outside to close  |  Drag sliders to adjust";
    g2.drawString(hint, px + pw/2 - g2.getFontMetrics().stringWidth(hint)/2, py + ph - 12);
}

private void drawSlider(Graphics2D g2, int x, int y, int w, String label, float value) {
    // Label
    g2.setFont(pixelFont(Font.BOLD, 16));
    g2.setColor(new Color(52, 35, 24));
    g2.drawString(label, x, y);

    int sy = y + 14;
    int sh = 8;

    // Track background
    g2.setColor(new Color(146, 106, 68));
    g2.fillRoundRect(x, sy, w, sh, sh, sh);

    // Filled portion
    int filled = (int)(w * value);
    g2.setColor(new Color(92, 66, 42));
    g2.fillRoundRect(x, sy, filled, sh, sh, sh);

    // Thumb
    int thumbX = x + filled - 8;
    int thumbY = sy - 6;
    g2.setColor(new Color(244, 210, 154));
    g2.fillOval(thumbX, thumbY, 20, 20);
    g2.setColor(popupBorder());
    g2.setStroke(new BasicStroke(2f));
    g2.drawOval(thumbX, thumbY, 20, 20);

    // Percentage
    g2.setFont(pixelFont(Font.PLAIN, 14));
    g2.setColor(new Color(80, 56, 38));
    g2.drawString((int)(value * 100) + "%", x + w + 8, sy + sh);
}

    private void paintQuitConfirm(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(0, 0, getWidth(), getHeight());

        Rectangle box = quitConfirmRect();
        drawPopupBox(g2, box.x, box.y, box.width, box.height);

        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 26));
        String title = quitConfirmToMenu ? "QUIT TO MENU?" : "QUIT GAME?";
        g2.drawString(title, box.x + box.width / 2 - g2.getFontMetrics().stringWidth(title) / 2, box.y + 46);

        g2.setColor(new Color(112, 83, 54, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(box.x + 24, box.y + 60, box.x + box.width - 24, box.y + 60);

        g2.setColor(new Color(80, 56, 38));
        g2.setFont(pixelFont(Font.BOLD, 18));
        String msg = quitConfirmToMenu ? "Save and return to the main menu?" : "Are you sure you want to close Five-Six?";
        g2.drawString(msg, box.x + box.width / 2 - g2.getFontMetrics().stringWidth(msg) / 2, box.y + 104);

        drawTextButton(g2, quitYesRect(), "YES");
        drawTextButton(g2, quitNoRect(), "NO");
    }

    private void drawTextButton(Graphics2D g2, Rectangle r, String label) {
        boolean hovered = r.contains(mouse);
        g2.setColor(hovered ? popupRowHover() : popupRow());
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(popupBorder());
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(new Color(52, 35, 24));
        g2.setFont(pixelFont(Font.BOLD, 20));
        g2.drawString(label, r.x + r.width / 2 - g2.getFontMetrics().stringWidth(label) / 2, r.y + 29);
    }

    private void drawMenuSettingsButton(Graphics2D g2) {
        BufferedImage[] imgs = btnImgs.get("settings");
        if (imgs == null || imgs[0] == null) return;
        Rectangle r = fixedMenuSettingsRect();
        BufferedImage img = "settings".equals(hoveredBtn) && imgs[1] != null ? imgs[1] : imgs[0];
        g2.drawImage(img, r.x, r.y, r.width, r.height, null);
    }

    private Rectangle fixedMenuSettingsRect() {
        int x = 16 * getWidth() / 500;
        int y = 309 * getHeight() / 342;
        int w = Math.max(1, 62 * getWidth() / 500);
        int h = Math.max(1, 22 * getHeight() / 342);
        return new Rectangle(x, y, w, h);
    }

    private Rectangle settingsPanelRect() {
    return new Rectangle(getWidth()/2 - 220, getHeight()/2 - 160, 440, 320);
}

    private Rectangle settingsMuteRect() {
    Rectangle panel = settingsPanelRect();
    return new Rectangle(panel.x + 50, panel.y + 62, 120, 44);
}

    // ─────────────────────────────────────────────────────────────
    //  PLAY STATE
    //  Layer order: gle.png > world_gui_hitbox > buttons/npcs on top
    // ─────────────────────────────────────────────────────────────
    private void paintPlay(Graphics2D g2) {
        if (mapImage != null) {
            fill(g2, mapImage);
        } else if (EMALL_MAP.equals(currentMapName)) {
            // No map image for emall — show the emall battle scene as atmosphere during narration
            BufferedImage emallBg = imageDisplay.getEmallBattleBackground();
            if (emallBg != null) {
                fill(g2, emallBg);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        if (showDebug && hitboxImage != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            fill(g2, hitboxImage);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        paintNPCs(g2);
        if (player != null) player.draw(g2);
        paintWorldGUI(g2);
        if (!currentDialog.isEmpty()) dialog(g2, currentDialog);
        if (settingsOpen) paintSettingsPanel(g2);
    }

    private void paintWorldGUI(Graphics2D g2) {
        if (player != null) hpBar(g2, 20, 20, 220, 22, player.currentHP, player.maxHP, player.characterName);
        if (worldGuiHitbox == null) return;
        drawBtn(g2, worldGuiHitbox, BC_ITEMS,   "item_inv");
        drawBtn(g2, worldGuiHitbox, BC_ABILINV, "abil_inv");
        drawBtn(g2, worldGuiHitbox, BC_BACK,    "backmenu");
        drawBtn(g2, worldGuiHitbox, BC_SAVE,    "wsave");
        drawWorldSettingsButton(g2);
        drawMapTitle(g2);
    }

    private void drawWorldSettingsButton(Graphics2D g2) {
        BufferedImage[] arr = btnImgs.get("wset");
        Rectangle r = settingsWorldRect();
        if (arr == null || r == null) return;
        BufferedImage img = ("wset".equals(hoveredBtn) && arr[1] != null) ? arr[1] : arr[0];
        if (img != null) g2.drawImage(img, r.x, r.y, r.width, r.height, null);
    }

    private Rectangle settingsWorldRect() {
        Rectangle r = bounds(worldGuiHitbox, BC_MUTE);
        if (r == null) return null;
        int w = Math.min(r.width, Math.max(1, (int)(r.height * 2.4)));
        return new Rectangle(r.x + (r.width - w) / 2, r.y, w, r.height);
    }

    private void drawMapTitle(Graphics2D g2) {
        if (mapTitleImg == null) return;
        Rectangle itemRect = bounds(worldGuiHitbox, BC_ITEMS);
        if (itemRect == null) return;
        int targetH = Math.max(28, itemRect.height - 8);
        int targetW = Math.max(1, mapTitleImg.getWidth() * targetH / Math.max(1, mapTitleImg.getHeight()));
        int x = Math.max(260, itemRect.x - targetW - 20);
        int y = itemRect.y + (itemRect.height - targetH) / 2;
        g2.drawImage(mapTitleImg, x, y, targetW, targetH, null);
    }

    // NPC bounding box cache — recomputed only on map load
    private Map<Integer,int[]> npcBoundsCache = new HashMap<>();
    private String npcBoundsCacheMap = "";

    private void paintNPCs(Graphics2D g2) {
        if (hitboxImage == null) return;
        // Rebuild cache only when map changes
        if (!currentMapName.equals(npcBoundsCacheMap)) {
            npcBoundsCache.clear();
            npcBoundsCacheMap = currentMapName;
            Set<Integer> npcColors = mapNPCColors();
            for (int py=0;py<hitboxImage.getHeight();py++) for (int px=0;px<hitboxImage.getWidth();px++) {
                int c = hitboxImage.getRGB(px,py)&0xFFFFFF;
                if (!npcColors.contains(c)) continue;
                int[] b = npcBoundsCache.get(c);
                if (b == null) { b = new int[]{px,px,py,py}; npcBoundsCache.put(c,b); }
                if(px<b[0])b[0]=px; if(px>b[1])b[1]=px; if(py<b[2])b[2]=py; if(py>b[3])b[3]=py;
            }
        }
        for (Map.Entry<Integer,int[]> e : npcBoundsCache.entrySet()) {
            int c=e.getKey(); int[] b=e.getValue();
            if (enemyStats.isDefeated(c)) continue;
            if (c == COLOR_BROKENDOOR) continue; // broken door has no sprite
            int sx=((b[0]+b[1])/2)*getWidth()/hitboxImage.getWidth();
            int sy=((b[2]+b[3])/2)*getHeight()/hitboxImage.getHeight();
            String f = enemyFolder(c); if (f==null) continue;
            BufferedImage nim = npcStand.get(f);
            if (nim!=null) {
                int dx=sx-tileSize/2, dy=sy-tileSize/2;
                g2.setColor(new Color(0,0,0,100));
                int sw=(int)(tileSize*.6),sh=(int)(tileSize*.15);
                g2.fillOval(dx+(tileSize-sw)/2, dy+tileSize-sh-15, sw, sh);
                g2.drawImage(nim, dx, dy, tileSize, tileSize, null);
            }
        }
    }

    private Set<Integer> mapNPCColors() {
        return HitboxColors.mapNpcColors(currentMapName);
    }

    // ─────────────────────────────────────────────────────────────
    //  FADE STATE
    // ─────────────────────────────────────────────────────────────
    private void paintFade(Graphics2D g2) {
        fill(g2, mapImage);
        paintNPCs(g2);
        if (player != null) player.draw(g2);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
        g2.setColor(Color.BLACK); g2.fillRect(0,0,getWidth(),getHeight());
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
    }

    // ─────────────────────────────────────────────────────────────
    //  BATTLE STATE
    //  Layer: battle_scene > battle_sprite_hitbox (sprites) > battle_hitbox (buttons) on top
    //  HP: player top-left, round top-middle, enemy top-right
    // ─────────────────────────────────────────────────────────────
    private void paintBattle(Graphics2D g2) {
        fill(g2, battleSceneImg);
        // Sprites
        if (battleSpriteHitbox != null) {
            if (playerBattleImg != null) {
                Rectangle r = bounds(battleSpriteHitbox, BC_PLRBAT);
                if (r!=null) g2.drawImage(playerBattleImg, r.x, r.y, r.width, r.height, null);
            }
            if (enemyBattleImg != null) {
                Rectangle r = bounds(battleSpriteHitbox, BC_ENMBAT);
                if (r!=null) g2.drawImage(enemyBattleImg, r.x, r.y, r.width, r.height, null);
            }
        }
        // HP bars
        if (player != null) hpBar(g2, 40, 30, 300, 22, player.currentHP, player.maxHP, player.characterName);
        // Round counter (top middle)
        String rnd = "Round " + battleRound;
        g2.setFont(new Font("Arial",Font.BOLD,28));
        int rw=g2.getFontMetrics().stringWidth(rnd);
        g2.setColor(new Color(0,0,0,150)); g2.fillRoundRect(getWidth()/2-rw/2-10,5,rw+20,38,10,10);
        g2.setColor(new Color(255,220,80)); g2.drawString(rnd,getWidth()/2-rw/2,36);
        if (!waitingOutcome) {
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            int mw = g2.getFontMetrics().stringWidth(battleMsg);
            g2.drawString(battleMsg, getWidth()/2 - mw/2, 66);
        }
        // Enemy HP
        hpBar(g2, getWidth()-360, 30, 300, 22, enemyHP, enemyMaxHP, enemyName);
        // Buttons (from battle_hitbox) — only show when player's turn
        if (!waitingOutcome) {
            Dimension moveSize = battleMoveButtonSize();
            drawBattleMoveBtn(g2, BC_ROCKBTN,  "rock", moveSize);
            drawBattleMoveBtn(g2, BC_PAPERBTN, "paper", moveSize);
            drawBattleMoveBtn(g2, BC_SCISSBTN, "scissors", moveSize);
            drawBtn(g2, battleHitbox, BC_USEITEM,  "useitem");
            drawBtn(g2, battleHitbox, BC_USEABIL,  "useabil");
        }
        // Active effects
        drawActiveEffects(g2);
        // Clairvoyance
        if (false && clairVisible) {
            // Persistent prediction banner — stays until the player picks a move
            g2.setColor(new Color(30, 10, 80, 220));
            g2.fillRoundRect(getWidth()/2 - 280, 18, 560, 48, 16, 16);
            g2.setColor(new Color(180, 120, 255));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(getWidth()/2 - 280, 18, 560, 48, 16, 16);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            String ct = clairText;
            g2.drawString(ct, getWidth()/2 - g2.getFontMetrics().stringWidth(ct)/2, 50);
        }
        if (!currentDialog.isEmpty()) dialog(g2, currentDialog);
    }

    private List<String> activeEffects() {
        List<String> out=new ArrayList<>();
        if(clairVisible && clairMove != null) {
            String moveName = switch (clairMove) {
                case ROCK -> "Rock";
                case PAPER -> "Paper";
                case SCISSORS -> "Scissors";
            };
            out.add("Clairvoyance: " + moveName);
        }
        if(fxUnoReverse)   out.add("Uno Reverse: ON");
        if(fxHypnotize)    out.add("Hypnotize: ON");
        if(fxYouCheater)   out.add("You Cheater: ON");
        if(fxFullCounter)  out.add("Full Counter: ON");
        if(fxHealRounds>0) out.add("Ticking Heal: "+fxHealRounds+" rounds");
        return out;
    }

    private void drawActiveEffects(Graphics2D g2) {
        List<String> effects = activeEffects();
        if (effects.isEmpty()) return;
        int w = 260;
        int h = 34 + effects.size() * 24;
        int x = 32;
        int y = 76;
        drawPopupBox(g2, x, y, w, h);
        g2.setFont(pixelFont(Font.BOLD, 15));
        g2.setColor(new Color(52, 35, 24));
        g2.drawString("ACTIVE", x + 16, y + 24);
        g2.setFont(pixelFont(Font.BOLD, 14));
        int ty = y + 50;
        for (String fx : effects) {
            g2.drawString(fx, x + 16, ty);
            ty += 24;
        }
    }

    private Dimension battleMoveButtonSize() {
        Rectangle r = bounds(battleHitbox, BC_ROCKBTN);
        Rectangle p = bounds(battleHitbox, BC_PAPERBTN);
        Rectangle s = bounds(battleHitbox, BC_SCISSBTN);
        int w = Integer.MAX_VALUE, h = Integer.MAX_VALUE;
        for (Rectangle rect : new Rectangle[]{r, p, s}) {
            if (rect == null) continue;
            w = Math.min(w, rect.width);
            h = Math.min(h, rect.height);
        }
        if (w == Integer.MAX_VALUE || h == Integer.MAX_VALUE) return new Dimension(1, 1);
        return new Dimension(w, h);
    }

    // ─────────────────────────────────────────────────────────────
    //  OUTCOME STATE
    //  Layer: outcome_scene > RPS image (BC_OUTCZONE region) > continue btn
    // ─────────────────────────────────────────────────────────────
    private void paintOutcome(Graphics2D g2) {
    fill(g2, outcomeSceneImg);

    if (outcomeRPSImg != null && outcomeHitbox != null) {
        Rectangle r = bounds(outcomeHitbox, BC_OUTCZONE);
        if (r != null) g2.drawImage(outcomeRPSImg, r.x, r.y, r.width, r.height, null);
    }

    // Only show Continue button if the round isn't over yet
    if (!battleResolved) {
        drawBtn(g2, outcomeHitbox, BC_CONTBAT, "contbat");
    } else {
        // Final blow — replace continue button with a "See Results" prompt
        drawBtn(g2, outcomeHitbox, BC_CONTBAT, "contbat");
    }

    g2.setFont(new Font("Arial", Font.BOLD, 26));
    int msgY = (outcomeHitbox != null && bounds(outcomeHitbox, BC_OUTCZONE) != null)
        ? bounds(outcomeHitbox, BC_OUTCZONE).y - 40 : 60;

    for (String ln : wrap(g2, battleMsg, getWidth() - 200)) {
        int lw = g2.getFontMetrics().stringWidth(ln);
        g2.setColor(new Color(255, 230, 100));
        g2.drawString(ln, getWidth() / 2 - lw / 2, msgY);
        msgY += 34;
    }
}
    // ─────────────────────────────────────────────────────────────
    //  INVENTORY / ABILITY
    // ─────────────────────────────────────────────────────────────
    private void paintInventory(Graphics2D g2) {
    if (prevStateBeforePanel == battleState) {
        paintBattle(g2);
    } else {
        paintPlay(g2);
        g2.setColor(new Color(0,0,0,100));
        g2.fillRect(0,0,getWidth(),getHeight());
    }
    int pw=520,ph=420,px=getWidth()/2-pw/2,py=getHeight()/2-ph/2;
    panel(g2,px,py,pw,ph,"ITEMS");
    List<ItemSystem.Item> list = items.getItems();
    int iy=py+88; g2.setFont(pixelFont(Font.BOLD,20));
    String hoverText = null;
    for (ItemSystem.Item item : list) {
        int cnt = items.count(item);
        boolean h = new Rectangle(px+20,iy-28,pw-40,38).contains(mouse);
        if (h) hoverText = item.displayName + ": " + item.description;
        g2.setColor(h ? popupRowHover() : popupRow());
        g2.fillRoundRect(px+20,iy-28,pw-40,38,8,8);
        g2.setColor(new Color(52, 35, 24));
        String label = item.displayName + "  x" + cnt;
        g2.drawString(label, px + pw / 2 - g2.getFontMetrics().stringWidth(label) / 2, iy);
        iy+=48; if(iy>py+ph-44) break;
    }
    if (list.isEmpty()) { g2.setColor(new Color(80,56,38)); g2.setFont(pixelFont(Font.ITALIC,22)); g2.drawString("No items",px+pw/2-44,py+ph/2); }
    drawHoverText(g2, hoverText);
}

    private void paintAbility(Graphics2D g2) {
    if (prevStateBeforePanel == battleState) {
        paintBattle(g2);
    } else {
        paintPlay(g2);
        g2.setColor(new Color(0,0,0,100));
        g2.fillRect(0,0,getWidth(),getHeight());
    }
    boolean inBattle = prevStateBeforePanel == battleState;
    int pw = 560, ph = 440, px = getWidth()/2 - pw/2, py = getHeight()/2 - ph/2;

    drawPopupBox(g2, px, py, pw, ph);

    g2.setColor(new Color(52, 35, 24));
    g2.setFont(pixelFont(Font.BOLD, 28));
    String title = "ABILITIES";
    g2.drawString(title, px + pw/2 - g2.getFontMetrics().stringWidth(title)/2, py + 42);

    g2.setColor(new Color(112, 83, 54, 120));
    g2.setStroke(new BasicStroke(1));
    g2.drawLine(px + 20, py + 54, px + pw - 20, py + 54);

    List<AbilitySystem.Ability> unique = abilities.getUnique();
    int ay = py + 88;
    g2.setFont(pixelFont(Font.BOLD, 18));
    String hoverText = null;
    for (AbilitySystem.Ability ab : unique) {
        int cnt = abilities.count(ab);
        boolean hovered = inBattle && new Rectangle(px+16, ay-26, pw-32, 36).contains(mouse);
        if (hovered) hoverText = ab.displayName + ": " + ab.description;
        g2.setColor(hovered ? popupRowHover() : popupRow());
        g2.fillRoundRect(px+16, ay-26, pw-32, 36, 8, 8);
        g2.setColor(inBattle ? new Color(52,35,24) : new Color(96,76,58));
        String label = ab.displayName + "  x" + cnt;
        g2.drawString(label, px + pw / 2 - g2.getFontMetrics().stringWidth(label) / 2, ay);
        ay += 46;
        if (ay > py + ph - 40) break;
    }
    if (unique.isEmpty()) {
        g2.setColor(new Color(80,56,38));
        g2.setFont(pixelFont(Font.ITALIC, 20));
        g2.drawString("No abilities", px + pw/2 - 55, py + ph/2);
    }

    drawHoverText(g2, hoverText);
}

    private void panel(Graphics2D g2, int px, int py, int pw, int ph, String title) {
        drawPopupBox(g2, px, py, pw, ph);
        g2.setColor(new Color(52,35,24)); g2.setFont(pixelFont(Font.BOLD,30));
        g2.drawString(title, px+pw/2-g2.getFontMetrics().stringWidth(title)/2, py+44);
    }

    // ─────────────────────────────────────────────────────────────
    //  WIN SCREEN
    // ─────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────
    //  NARRATION SCREEN (post-Vaughn, before final boss)
    // ─────────────────────────────────────────────────────────────
    private void paintNarration(Graphics2D g2) {
        // Background: emall battle scene, dimmed for atmosphere
        if (battleSceneImg != null) {
            fill(g2, battleSceneImg);
            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        // Dialog box at bottom
        String line = currentDialog;
        String playerFirst = (player != null) ? capitalize(player.characterName) : "Player";
        boolean isPlayerLine = line.startsWith(playerFirst + ":");
        boolean isBossLine = isFinalBossLine(line);

        int bx = 50, by = getHeight() - 210, bw = getWidth() - 100, bh = 165;
        drawPopupBox(g2, bx, by, bw, bh);

        BufferedImage port = isPlayerLine ? playerDialogImg : (isBossLine ? enemyDialogImg : null);
        Rectangle dialogBox = new Rectangle(bx, by, bw, bh);
        Rectangle fallbackPort = new Rectangle(bx + 14, by + (bh - 115) / 2, 82, 115);
        boolean hasPortrait = drawDialogPortrait(g2, port, fallbackPort);
        Rectangle portraitRect = dialogPortraitRect() != null ? dialogPortraitRect() : fallbackPort;
        Rectangle textRect = dialogTextRect(dialogBox, portraitRect, hasPortrait);

        // Text
        g2.setFont(pixelFont(Font.BOLD, 20));
        g2.setColor(new Color(52, 35, 24));
        int ty = textRect.y + 12;
        for (String ln : wrap(g2, line, textRect.width)) {
            g2.drawString(ln, textRect.x, ty);
            ty += 34;
        }

        // Prompt
        g2.setFont(pixelFont(Font.ITALIC, 16));
        g2.setColor(new Color(80, 56, 38));
        boolean isLast = narIndex >= narLines.length - 1;
        String prompt = isLast ? "Press 'E' to start the fight!" : "Press 'E' to continue...";
        g2.drawString(prompt, bx + bw - 285, by + bh - 14);
    }

    // ─────────────────────────────────────────────────────────────
    //  PRE-BATTLE DIALOGUE (final boss only)
    //  Shows both sprites on battle scene with a dialogue exchange
    //  before the actual battle begins.
    // ─────────────────────────────────────────────────────────────
    private void paintPreBattle(Graphics2D g2) {
        // Background: emall battle scene
        fill(g2, battleSceneImg);

        // Draw both sprites using the same hitbox regions as the real battle
        if (battleSpriteHitbox != null) {
            if (playerBattleImg != null) {
                Rectangle r = bounds(battleSpriteHitbox, BC_PLRBAT);
                if (r != null) g2.drawImage(playerBattleImg, r.x, r.y, r.width, r.height, null);
            }
            if (enemyBattleImg != null) {
                Rectangle r = bounds(battleSpriteHitbox, BC_ENMBAT);
                if (r != null) g2.drawImage(enemyBattleImg, r.x, r.y, r.width, r.height, null);
            }
        }

        // Dark overlay at the bottom to make the dialog readable
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, getHeight() - 220, getWidth(), 220);

        // Current dialogue line
        String line = (preBattleIndex < preBattleLines.length)
            ? preBattleLines[preBattleIndex] : "";

        // Choose portrait: player speaks on even lines, enemy on odd lines
        // last line (narration) shows no portrait
        boolean isNarration = line.startsWith("—") || line.startsWith("-");
        String playerFirst = (player != null) ? capitalize(player.characterName) : "Player";
        boolean isPlayerLine = !isNarration && line.startsWith(playerFirst + ":");
        boolean isBossLine = !isNarration && isFinalBossLine(line);

        int bx = 60, by = getHeight() - 200, bw = getWidth() - 120, bh = 150;
        drawPopupBox(g2, bx, by, bw, bh);

        // Portrait
        BufferedImage port = isPlayerLine ? playerDialogImg : (isBossLine ? enemyDialogImg : null);
        Rectangle dialogBox = new Rectangle(bx, by, bw, bh);
        Rectangle fallbackPort = new Rectangle(bx + 12, by + (bh - 110) / 2, 80, 110);
        boolean hasPortrait = drawDialogPortrait(g2, port, fallbackPort);
        Rectangle portraitRect = dialogPortraitRect() != null ? dialogPortraitRect() : fallbackPort;
        Rectangle textRect = dialogTextRect(dialogBox, portraitRect, hasPortrait);

        // Dialogue text
        g2.setFont(pixelFont(Font.BOLD, 20));
        g2.setColor(new Color(52, 35, 24));
        int ty = textRect.y + 10;
        for (String ln : wrap(g2, line, textRect.width)) {
            g2.drawString(ln, textRect.x, ty); ty += 32;
        }

        // Prompt
        g2.setFont(pixelFont(Font.ITALIC, 16));
        g2.setColor(new Color(80, 56, 38));
        boolean isLast = preBattleIndex >= preBattleLines.length - 1;
        String prompt = isLast ? "Press 'E' to begin the battle!" : "Press 'E' to continue...";
        g2.drawString(prompt, bx + bw - 280, by + bh - 18);
    }

    private void paintWin(Graphics2D g2) {
        if (winImg != null) {
            fill(g2, winImg);
            return;
        }
        drawPlaceholderScreen(g2, "WIN SCREEN PLACEHOLDER");
    }

    private void drawPlaceholderScreen(Graphics2D g2, String title) {
        g2.setColor(new Color(198, 158, 104));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(112, 83, 54));
        g2.setStroke(new BasicStroke(8f));
        g2.drawRect(28, 28, getWidth() - 56, getHeight() - 56);
        g2.setFont(pixelFont(Font.BOLD, 48));
        g2.setColor(new Color(52, 35, 24));
        g2.drawString(title, getWidth()/2 - g2.getFontMetrics().stringWidth(title)/2, getHeight()/2);
    }

    private void paintLose(Graphics2D g2) {
        g2.setColor(new Color(20, 0, 0)); g2.fillRect(0,0,getWidth(),getHeight());
        // Dim red overlay
        g2.setColor(new Color(180, 0, 0, 60)); g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(new Color(0,0,0,180)); g2.fillRect(0,getHeight()/2-120,getWidth(),280);
        // Game Over title
        g2.setFont(new Font("Arial", Font.BOLD, 52)); g2.setColor(new Color(220, 30, 30));
        String title = "GAME OVER";
        g2.drawString(title, getWidth()/2 - g2.getFontMetrics().stringWidth(title)/2, getHeight()/2 - 50);
        // Message
        g2.setFont(new Font("Arial", Font.BOLD, 22)); g2.setColor(Color.WHITE);
        String msg = "The beggar took your money. You never made it to Jollibee.";
        for (String ln : wrap(g2, msg, getWidth()-100)) {
            g2.drawString(ln, getWidth()/2 - g2.getFontMetrics().stringWidth(ln)/2, getHeight()/2 + 10);
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 18)); g2.setColor(new Color(200, 200, 200));
        String sub = "Your progress has been erased.";
        g2.drawString(sub, getWidth()/2 - g2.getFontMetrics().stringWidth(sub)/2, getHeight()/2 + 55);
        // Prompt
        g2.setFont(new Font("Arial", Font.ITALIC, 18)); g2.setColor(new Color(160,160,160));
        String back = "Click to return to the main menu and start over";
        g2.drawString(back, getWidth()/2 - g2.getFontMetrics().stringWidth(back)/2, getHeight()-55);
    }

    // ─────────────────────────────────────────────────────────────
    //  NPC interaction access (used by Player.java)
    // ─────────────────────────────────────────────────────────────
    public boolean isGleMap()       { return GLE_MAP.equals(currentMapName); }
    public boolean isFrontgateMap() { return FRONTGATE_MAP.equals(currentMapName); }

    /** Called by Player when Johnru or Vaughn is the miniboss and player tries to fight before clearing others */
    public boolean allOtherGleEnemiesDefeated() {
        return enemyStats.isDefeated(COLOR_JAMES) && enemyStats.isDefeated(COLOR_ALIEYANDREW)
            && enemyStats.isDefeated(COLOR_KYLE)  && enemyStats.isDefeated(COLOR_ADRIAN);
    }

    public boolean allOtherFrontgateEnemiesDefeated() {
        return enemyStats.isDefeated(COLOR_DARRYLL) && enemyStats.isDefeated(COLOR_GIO)
            && enemyStats.isDefeated(COLOR_YOHANN)  && enemyStats.isDefeated(COLOR_DIRK)
            && enemyStats.isDefeated(COLOR_JAKE);
    }

    private void paintResult(Graphics2D g2) {
    // Background: the map they were just on
    if (mapImage != null) {
        fill(g2, mapImage);
    } else {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    if (battleResolved && player != null && player.currentHP > 0) {
        // ── WIN REWARDS BOX ────────────────────────────────────────
        // ── WIN REWARDS BOX ────────────────────────────────────────
g2.setColor(new Color(0, 0, 0, 160));
g2.fillRect(0, 0, getWidth(), getHeight());

int rowCount = Math.max(1, lastRewardItems.size()) + lastRewardAbils.size();
int bw = 440, bh = 80 + 30 + rowCount * 30 + 70;
int bx = getWidth() / 2 - bw / 2;
int by = getHeight() / 2 - bh / 2;

// Panel background
drawPopupBox(g2, bx, by, bw, bh);

// "YOU WIN!" header — clean gold, no glow blob
g2.setFont(new Font("Arial", Font.BOLD, 46));
FontMetrics fmw = g2.getFontMetrics();
String win = "YOU WIN!";
// Thin black shadow only
g2.setColor(new Color(0, 0, 0, 180));
g2.drawString(win, bx + bw/2 - fmw.stringWidth(win)/2 + 3, by + 56 + 3);
// Gold text
g2.setColor(new Color(52, 35, 24));
g2.drawString(win, bx + bw/2 - fmw.stringWidth(win)/2, by + 56);

// Divider
g2.setColor(new Color(112, 83, 54, 120));
g2.setStroke(new BasicStroke(1f));
g2.drawLine(bx + 20, by + 68, bx + bw - 20, by + 68);

// "Rewards earned:" label
g2.setFont(new Font("Arial", Font.BOLD, 15));
g2.setColor(new Color(200, 200, 200));
g2.drawString("Rewards earned:", bx + 26, by + 90);

int ry = by + 116;
g2.setFont(new Font("Arial", Font.PLAIN, 16));

// Deduplicate items with counts
java.util.LinkedHashMap<String, Integer> itemCounts = new java.util.LinkedHashMap<>();
for (String name : lastRewardItems) itemCounts.merge(name, 1, Integer::sum);

if (itemCounts.isEmpty()) {
    g2.setColor(new Color(150, 150, 150));
    g2.drawString("  No items", bx + 26, ry);
    ry += 30;
} else {
    for (java.util.Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
        g2.setColor(new Color(255, 210, 60));
        String label = entry.getValue() > 1
            ? "  x" + entry.getValue() + "  " + entry.getKey()
            : "  + " + entry.getKey();
        g2.drawString(label, bx + 26, ry);
        ry += 30;
    }
}

// Abilities
for (String name : lastRewardAbils) {
    g2.setColor(new Color(160, 200, 255));
    g2.drawString("  + " + name + "  (ability)", bx + 26, ry);
    ry += 30;
}

// Divider before footer
g2.setColor(new Color(80, 80, 80, 160));
g2.setStroke(new BasicStroke(1f));
g2.drawLine(bx + 20, by + bh - 46, bx + bw - 20, by + bh - 46);

// Footer
g2.setFont(new Font("Arial", Font.ITALIC, 13));
g2.setColor(new Color(130, 130, 130));
g2.drawString("Added to your inventory", bx + 26, by + bh - 26);

// Click to continue prompt — below the box
g2.setFont(new Font("Arial", Font.ITALIC, 17));
g2.setColor(new Color(200, 200, 200));
String prompt = "Click anywhere to continue";
g2.drawString(prompt, getWidth()/2 - g2.getFontMetrics().stringWidth(prompt)/2, by + bh + 34);
    } else if (battleResolved && player != null && player.currentHP <= 0) {
        // ── DEFEAT OVERLAY ─────────────────────────────────────────
        g2.setColor(new Color(100, 0, 0, 180));
        g2.fillRect(0, 0, getWidth(), getHeight());

        Font defeatFont = new Font("Arial", Font.BOLD, 100);
        g2.setFont(defeatFont);
        String defeatText = "DEFEAT";
        FontMetrics fmd = g2.getFontMetrics();
        int dx = getWidth() / 2 - fmd.stringWidth(defeatText) / 2;
        int dy = getHeight() / 2 + 30;

        // Red glow layers
        for (int spread = 14; spread >= 1; spread--) {
            float alpha = 0.05f * (15 - spread);
            g2.setColor(new Color(1f, 0f, 0f, Math.min(1f, alpha)));
            for (int oy = -spread; oy <= spread; oy += spread)
                for (int ox = -spread; ox <= spread; ox += spread)
                    if (ox != 0 || oy != 0)
                        g2.drawString(defeatText, dx + ox, dy + oy);
        }

        // Black outline
        g2.setColor(Color.BLACK);
        for (int oy = -4; oy <= 4; oy++)
            for (int ox = -4; ox <= 4; ox++)
                if (ox != 0 || oy != 0)
                    g2.drawString(defeatText, dx + ox, dy + oy);

        // Main red text
        g2.setColor(new Color(225, 30, 30));
        g2.drawString(defeatText, dx, dy);

        // Sub-message
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        g2.setColor(new Color(220, 150, 150));
        String sub = "You were defeated by " + enemyName + "...";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, getWidth() / 2 - sw / 2, dy + 58);

        // Click to continue
        g2.setFont(new Font("Arial", Font.ITALIC, 17));
        g2.setColor(new Color(180, 180, 180));
        String prompt = "Click anywhere to continue";
        g2.drawString(prompt, getWidth()/2 - g2.getFontMetrics().stringWidth(prompt)/2, dy + 108);
    }
}

private void applyMusicVolume() {
    if (isMuted) return;
    Clip clip = activeMusicClip;
    if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float dB = (float)(Math.log10(Math.max(0.0001, musicVolume)) * 20.0);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
    }
}

    private void debugAudio() {
        // Test music stream
        InputStream ms = openMusicStream("menu_sountrack");
        System.out.println("Music stream: " + (ms != null ? "FOUND" : "NOT FOUND"));

        // Test SFX stream
        InputStream ss = getClass().getResourceAsStream("/res/soundtrack/click.wav");
        System.out.println("SFX click.wav: " + (ss != null ? "FOUND" : "NOT FOUND"));

        // Test AudioSystem
        try {
            if (ss == null) ss = getClass().getResourceAsStream("/res/soundtrack/click.wav");
            if (ss != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(ss));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                System.out.println("Clip opened OK, frames: " + clip.getFrameLength());
                clip.close();
            }
        } catch (Exception e) {
            System.out.println("Clip error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
