package main;

import static main.HitboxColors.Map.*;
import static main.HitboxColors.Ui.*;

import entity.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.sound.sampled.*;

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
    private volatile javazoom.jl.player.Player activeMusicPlayer = null;

    // SAVE
    private SaveData saveData = null;



    private static final int AUTOSAVE_INTERVAL_FRAMES = 180;

    // DEBUG
    public boolean showDebug   = false;
    private boolean f1WasHeld  = false;
    private boolean escWasHeld = false;

    // DIALOG
    public String currentDialog = "";
    public int dialogStage      = 0;
    public int lastNPCColor     = 0;
    private int talkShake       = 0;

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

    // ACTIVE EFFECTS
    private boolean fxUnoReverse   = false;
    private boolean fxHypnotize    = false;
    private boolean fxYouCheater   = false;
    private boolean fxFullCounter  = false;
    private int     fxHealRounds   = 0;
    private int     fxHealAmt      = 0;
    private int     fxEnergyRounds = 0;

    // CLAIRVOYANCE
    private boolean clairVisible   = false;
    private String  clairText      = "";
    private BattleSystem.Move clairMove = null;  // the actual predicted move used in next resolve()

    // Tracks which state opened the item/ability panel so ESC returns correctly
    private int prevStateBeforePanel = playState;
    private int autoSaveCountdown = AUTOSAVE_INTERVAL_FRAMES;

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

        loadAll();
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
        });
    }

    // ─────────────────────────────────────────────────────────────
    //  RESOURCE LOADING
    // ─────────────────────────────────────────────────────────────
    private void loadAll() {
        menuScreenImg = img("/res/gui/pixelart/menu/menu_screen.png");
        logoImg       = img("/res/gui/pixelart/menu/fixsix_log.png");

        menuMainHitbox  = img("/res/gui/button_hitbox/menu_main_hitbox.png");
        menuStartHitbox = img("/res/gui/button_hitbox/menu_start_hitbox.png");
        menuCharHitbox  = img("/res/gui/button_hitbox/menu_characterselect_hitbox.png");
        battleHitbox    = img("/res/gui/button_hitbox/battle_hitbox.png");
        battleSpriteHitbox = img("/res/gui/button_hitbox/battle_sprite_hitbox.png");
        worldGuiHitbox  = img("/res/gui/button_hitbox/world_gui_hitbox.png");
        outcomeHitbox   = img("/res/gui/button_hitbox/outcome_hitbox.png");

        // Menu buttons
        btn("start",    "/res/gui/buttons/menu/start_idle.png",           "/res/gui/buttons/menu/start_hover.png");
        btn("credits",  "/res/gui/buttons/menu/credits_idle.png",         "/res/gui/buttons/menu/credits_hover.png");
        btn("quit",     "/res/gui/buttons/menu/quit_idle.png",            "/res/gui/buttons/menu/quit_hover.png");
        btn("settings", "/res/gui/buttons/menu/settings_idle.png",        "/res/gui/buttons/menu/settings_hover.png");
        btn("mute",     "/res/gui/buttons/menu/mute_idle.png",            "/res/gui/buttons/menu/mute_hover.png");
        btn("muted",    "/res/gui/buttons/menu/muted_idle.png",           "/res/gui/buttons/menu/muted_hover.png");
        btn("save",     "/res/gui/buttons/menu/save_idle.png",            "/res/gui/buttons/menu/save_hover.png");
        btn("continue", "/res/gui/buttons/menu/continue_idle.png",        "/res/gui/buttons/menu/continue_hover.png");
        btn("selchar",  "/res/gui/buttons/menu/selectcharacter_idle.png", "/res/gui/buttons/menu/selectcharacter_hover.png");

        // Player select
        btn("ivan",     "/res/gui/buttons/player/ivan_idle.png",     "/res/gui/buttons/player/ivan_hover.png");
        btn("sam",      "/res/gui/buttons/player/sam_idle.png",      "/res/gui/buttons/player/sam_hover.png");
        btn("nimuel",   "/res/gui/buttons/player/nimuel_idle.png",   "/res/gui/buttons/player/nimuel_hover.png");
        btn("johnfiel", "/res/gui/buttons/player/johnfiel_idle.png", "/res/gui/buttons/player/johnfiel_hover.png");

        // World GUI
        btn("item_inv",  "/res/gui/buttons/world/item_idle.png",       "/res/gui/buttons/world/item_hover.png");
        btn("abil_inv",  "/res/gui/buttons/world/ability_idle.png",     "/res/gui/buttons/world/ability_hover.png");
        btn("backmenu",  "/res/gui/buttons/world/backtomenu_idle.png",  "/res/gui/buttons/world/backtomenu_hover.png");
        btn("wsave",     "/res/gui/buttons/world/save_idle.png",        "/res/gui/buttons/world/save_hover.png");
        btn("wset",      "/res/gui/buttons/world/settings_idle.png",    "/res/gui/buttons/world/settings_hover.png");
        btn("wmute",     "/res/gui/buttons/world/mute_idle.png",        "/res/gui/buttons/world/mute_hover.png");
        btn("wmuted",    "/res/gui/buttons/world/muted_idle.png",       "/res/gui/buttons/world/muted_hover.png");

        // Battle
        btn("rock",     "/res/gui/buttons/battle/rock_idle.png",        "/res/gui/buttons/battle/rock_hover.png");
        btn("paper",    "/res/gui/buttons/battle/paper_idle.png",        "/res/gui/buttons/battle/paper_hover.png");
        btn("scissors", "/res/gui/buttons/battle/scissors_idle.png",     "/res/gui/buttons/battle/scissors_hover.png");
        btn("contbat",  "/res/gui/buttons/battle/continue_idle.png",     "/res/gui/buttons/battle/continue_hover.png");
        btn("useitem",  "/res/gui/buttons/battle/items_idle.png",        "/res/gui/buttons/battle/items_hover.png");
        btn("useabil",  "/res/gui/buttons/battle/abilities_idle.png",    "/res/gui/buttons/battle/abilities_hover.png");

        // Character previews
        // Character-select preview images
        charSelectImg.put("ivan",     img("/res/sprites/player/ivan/ivan_selectcharacter.png"));
        charSelectImg.put("sam",      img("/res/sprites/player/sam/sam_selectcharacter.png"));
        charSelectImg.put("nimuel",   img("/res/sprites/player/nimuel/nimuel_selectcharacter.png"));
        charSelectImg.put("johnfiel", img("/res/sprites/player/johnfiel/johnfiel_selectcharacter.png"));

        // NPC stands
        String[] npcs = {"james","alieyandrew","kyle","johnru","adrian","darryll","gio","yohann","dirk","jake","vaughn"};
        for (String n : npcs) npcStand.put(n, img("/res/sprites/enemies/" + n + "/" + n + "_stand.png"));
    }

    private BufferedImage img(String path) {
        try { return ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path))); }
        catch (Exception e) { return null; }
    }

    private void btn(String key, String idle, String hover) {
        btnImgs.put(key, new BufferedImage[]{ img(idle), img(hover) });
    }

    public void loadMapImages(String mapName) {
        currentMapName = mapName;
        currentDialog  = "";
        dialogStage    = 0;
        lastNPCColor   = 0;
        if (!EMALL_MAP.equals(mapName)) pendingBattleEnemyColor = 0;
        settingsOpen   = false;
        narrating      = false;

        switch (mapName) {
            case GLE_MAP -> {
                mapImage    = img("/res/gui/pixelart/map/gle.png");
                hitboxImage = img("/res/gui/pixelart/map/gle_hitbox.png");
                mapTitleImg = img("/res/gui/pixelart/map/gle_title.png");
                playMusic("gle_soundtrack");
            }
            case FRONTGATE_MAP -> {
                mapImage    = img("/res/gui/pixelart/map/frontgate.png");
                hitboxImage = img("/res/gui/pixelart/map/frontgate_hitbox.png");
                mapTitleImg = img("/res/gui/pixelart/map/frontgate_title.png");
                playMusic("frontgate_soundtrack");
            }
            case EMALL_MAP -> {
                mapImage    = null;
                hitboxImage = null;
                mapTitleImg = null;
            }
        }
        if (player != null) setPlayerSpawn();
        autoSave();
        repaint();
    }

    // ─────────────────────────────────────────────────────────────
    //  MUSIC
    // ─────────────────────────────────────────────────────────────
    private void playMusic(String name) {
        if (name == null || name.isBlank()) return;
        if (name.equals(musicTrack) && musicThread != null && musicThread.isAlive()) return;
        stopMusic();
        musicTrack = name;
        if (isMuted) return;
        stopMusicRequested = false; // safe now — old thread is confirmed dead after stopMusic()
        musicThread = new Thread(() -> {
            while (!stopMusicRequested) {
                try (InputStream raw = openMusicStream(name)) {
                    if (raw == null) return;
                    javazoom.jl.player.Player player = new javazoom.jl.player.Player(new BufferedInputStream(raw));
                    activeMusicPlayer = player;
                    player.play();
                } catch (Exception ignored) {
                    return;
                } finally {
                    activeMusicPlayer = null;
                }
            }
        }, "music-" + name);
        musicThread.setDaemon(true);
        musicThread.start();
    }

    private void stopMusic() {
        stopMusicRequested = true;
        javazoom.jl.player.Player player = activeMusicPlayer;
        if (player != null) {
            try { player.close(); } catch (Exception ignored) {}
        }
        Thread t = musicThread;
        if (t != null && t.isAlive()) {
            try { t.join(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            if (t.isAlive()) t.interrupt();
        }
        musicThread = null;
        activeMusicPlayer = null;
        // Only reset stopMusicRequested AFTER the old thread is confirmed dead
        // so the new playMusic loop doesn't race with the old thread's while-check
    }

    private void applyVolume() {
        if (isMuted) {
            stopMusic();
        } else if (!musicTrack.isBlank()) {
            String track = musicTrack;
            musicTrack = "";
            playMusic(track);
        }
    }

    public void toggleMute() { isMuted = !isMuted; applyVolume(); }

    private InputStream openMusicStream(String name) {
        String[] paths = {
                "/res/soundtrack/" + name + ".mp3",
                "/res/soundTrack/" + name + ".mp3",
                "/res/soundtrack/" + name + ".wav",
                "/res/soundTrack/" + name + ".wav"
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
                new ArrayList<>(abilities.getAbilities()));
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
        player = new Player(this, keyH, saveData.characterName);
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
        hoveredBtn = null;
        hoveredCharColor = 0;
        if (settingsOpen && settingsMuteRect().contains(mouse)) {
            hoveredBtn = isMuted ? "wmuted" : "wmute";
            return;
        }
        if ((gameState == menuState || gameState == menuCharState) && fixedMenuSettingsRect().contains(mouse)) {
            hoveredBtn = "settings";
            return;
        }
        if (gameState == menuCharState) {
            String charHover = charButtonAt(mouse);
            if (charHover != null) {
                hoveredBtn = charHover;
                hoveredCharColor = charColor(charHover);
                return;
            }
        }
        if (gameState == playState || gameState == inventoryState || gameState == abilityState) {
            hoveredBtn = worldButtonAt(mouse);
            return;
        }
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
        if (settingsOpen) {
            settingsClick(p);
            return;
        }
        if ((gameState == menuState || gameState == menuCharState) && fixedMenuSettingsRect().contains(p)) {
            settingsOpen = true;
            return;
        }
        int c;
        switch (gameState) {
            case menuState      -> { c = colorAt(menuMainHitbox, p);  menuClick(c); }
            case menuStartState -> { c = colorAt(menuStartHitbox, p); menuStartClick(c); }
            case menuCharState  -> { c = colorAt(menuCharHitbox, p);  charClick(c, p); }
            case playState, inventoryState, abilityState -> {
                worldClick(p);
            }
            case fadeState -> { }
            case battleState -> { battleClick(colorAt(battleHitbox, p), p); }
            case outcomeState -> { if (colorAt(outcomeHitbox, p) == BC_CONTBAT) nextRound(); }
            case creditsState -> { if (keyH.escPressed) gameState = menuState; }
        }
    }

    private void menuClick(int c) {
        switch (c) {
            case BC_START  -> gameState = menuStartState;
            case BC_CREDIT -> gameState = creditsState;
            case BC_QUIT   -> System.exit(0);
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
        if (gameState == inventoryState) {
            handleItemClick(p);
            // If still in inventoryState after click (no row hit), check if they clicked the button to close
            if (gameState == inventoryState) {
                String key = worldButtonAt(p);
                if ("item_inv".equals(key)) { gameState = playState; return; }
                if ("abil_inv".equals(key)) { prevStateBeforePanel = playState; gameState = abilityState; return; }
            }
            return;
        }
        if (gameState == abilityState) {
            handleAbilityClick(p);
            // If still in abilityState after click (no row hit or locked), check button clicks
            if (gameState == abilityState) {
                String key = worldButtonAt(p);
                if ("abil_inv".equals(key)) { gameState = playState; return; }
                if ("item_inv".equals(key)) { prevStateBeforePanel = playState; gameState = inventoryState; return; }
            }
            return;
        }
        String key = worldButtonAt(p);
        if (key == null) return;
        switch (key) {
            case "item_inv" -> { prevStateBeforePanel = playState; toggleState(inventoryState); }
            case "abil_inv" -> { prevStateBeforePanel = playState; toggleState(abilityState); }
            case "backmenu" -> { gameState = menuState; stopMusic(); playMusic("menu_sountrack"); currentDialog = ""; autoSave(); }
            case "wsave"    -> { autoSave(); abilMsg = "Game saved!"; abilMsgTimer = 120; }
            case "wset" -> settingsOpen = true;
        }
    }

    private void toggleState(int s) {
        gameState = (gameState == s) ? playState : s;
    }

    private void battleClick(int c, Point p) {
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
        } else if (!panelR.contains(p)) {
            settingsOpen = false;
        }
    }

    private void handleItemClick(Point p) {
        int pw=520, px=getWidth()/2-260, py=getHeight()/2-210;
        List<ItemSystem.Item> list = items.getItems();
        int iy = py + 72;
        for (ItemSystem.Item item : list) {
            if (new Rectangle(px+20, iy-28, pw-40, 38).contains(p)) { useItem(item); return; }
            iy += 48;
        }
    }

    private void handleAbilityClick(Point p) {
        int pw=540, px=getWidth()/2-270, py=getHeight()/2-210;
        List<AbilitySystem.Ability> unique = abilities.getUnique();
        int ay = py + 72;
        for (AbilitySystem.Ability ab : unique) {
            if (new Rectangle(px+20, ay-28, pw-40, 38).contains(p)) { useAbility(ab); return; }
            ay += 48;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  CHARACTER SELECT
    // ─────────────────────────────────────────────────────────────
    public void selectChar(String name) {
        // If the user picks the same character as the current save, continue the existing progress.
        // If they pick a different character, reset everything back to a fresh run.
        if (saveData != null && saveData.isForCharacter(name)) {
            loadSave();
            return;
        }
        enemyStats = new EnemyStats();
        items      = new ItemSystem();
        abilities  = new AbilitySystem();
        player = new Player(this, keyH, name);
        loadMapImages(GLE_MAP);
        gameState = playState;
        autoSave();
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
        enemyMaxHP     = enemyStats.getEnemyHP(pendingBattleEnemyColor);
        enemyHP        = enemyMaxHP;
        battleRound    = 1;
        battleMsg      = "Round " + battleRound + " - Choose your move!";
        battleResolved = false;
        waitingOutcome = false;
        isFinalBoss    = (pendingBattleEnemyColor == COLOR_FINALBOSS);
        enemyName      = enemyName(pendingBattleEnemyColor);

        String mapKey = switch (currentMapName) {
            case GLE_MAP -> "gle"; case FRONTGATE_MAP -> "frontgate"; default -> "emall";
        };
        battleSceneImg  = img("/res/gui/pixelart/battle_scene/" + mapKey + "_battle.png");
        outcomeSceneImg = img("/res/gui/pixelart/battle_scene/" + mapKey + "_outcome.png");

        String folder = enemyFolder(pendingBattleEnemyColor);
        if (folder != null) {
            enemyBattleImg = img("/res/sprites/enemies/" + folder + "/" + folder + "_battle.png");
            enemyDialogImg = img("/res/sprites/enemies/" + folder + "/" + folder + "_dialog.png");
        } else if (isFinalBoss) {
            enemyBattleImg = img("/res/sprites/enemies/finalboss/final_boss.png");
            enemyDialogImg = enemyBattleImg;
        }
        if (player != null) {
            String pn = player.characterName;
            playerBattleImg = img("/res/sprites/player/" + pn + "/" + pn + "_battle.png");
            playerDialogImg = img("/res/sprites/player/" + pn + "/" + pn + "_dialog.png");
        }
        playMusic("battle_sountrack");

        // Final boss gets a pre-battle dialogue before the fight begins
        if (isFinalBoss) {
            String playerFirst = (player != null) ? capitalize(player.characterName) : "Player";
            preBattleLines = new String[]{
                playerFirst + ": \"Back off! I'm not giving you anything!\"",
                "Beggar: \"You think you can just walk away? Hand it over — NOW!\"",
                playerFirst + ": \"You want it? Come and get it then.\"",
                "Beggar: \"Fine. Don't say I didn't warn you...\"",
                "— Both of you get into stance. There's no turning back. —"
            };
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
            battleMsg = "You Cheater activated! Round voided — pick again.";
            return;
        }

        if (fxHealRounds > 0) {
            player.currentHP = Math.min(player.currentHP + fxHealAmt, player.maxHP);
            fxHealRounds--;
        }

        double dm = player.damageMultiplier;
        if (fxEnergyRounds > 0) { dm *= 20; fxEnergyRounds--; }

        switch (result) {
            case PLAYER_WIN -> {
                int dmg = (int) Math.max(1, (rand.nextInt(10)+1) * dm);
                if (fxFullCounter) { dmg *= 2; fxFullCounter = false; }
                fxUnoReverse = false;
                enemyHP = Math.max(0, enemyHP - dmg);
                if (enemyHP <= 0) {
                    battleMsg = "You Win! Dealt " + dmg + " dmg — " + enemyName + " defeated!";
                    battleResolved = true;
                    enemyStats.markDefeated(pendingBattleEnemyColor);
                    grantRewards();
                } else {
                    battleMsg = "You Win! Dealt " + dmg + " damage to " + enemyName + ".";
                }
            }
            case ENEMY_WIN -> {
                int dmg = (int) Math.max(1, (rand.nextInt(10)+1) / Math.max(0.1, player.damageMultiplier));
                if (isFinalBoss) dmg = (int)(dmg * 1.5);
                if (fxUnoReverse || fxFullCounter) {
                    int ref = fxFullCounter ? dmg*2 : dmg;
                    fxUnoReverse = false; fxFullCounter = false;
                    enemyHP = Math.max(0, enemyHP - ref);
                    battleMsg = "Reflected " + ref + " damage back!";
                    if (enemyHP <= 0) { battleMsg += " " + enemyName + " defeated!"; battleResolved = true; enemyStats.markDefeated(pendingBattleEnemyColor); grantRewards(); }
                } else {
                    player.currentHP = Math.max(0, player.currentHP - dmg);
                    if (player.currentHP <= 0) { battleMsg = "You Lose! Took " + dmg + " dmg — Defeated!"; battleResolved = true; }
                    else battleMsg = "You Lose! Took " + dmg + " damage from " + enemyName + ".";
                }
            }
            case DRAW -> battleMsg = "Draw! No damage dealt.";
        }

        String pp = pm.name().toLowerCase(), ep = em.name().toLowerCase();
        outcomeRPSImg = img("/res/gui/pixelart/battle_outcome/" + pp + "_" + ep + ".png");
        waitingOutcome = true;
        gameState = outcomeState;
    }

    private void grantRewards() {
        int ic = rand.nextInt(5) + 1;
        for (int i=0;i<ic;i++) items.addRandom(rand);
        int ac = rand.nextDouble() < 0.3 ? rand.nextInt(3)+1 : 1;
        for (int i=0;i<ac;i++) abilities.addRandom(rand);
        autoSave();
    }

    private void nextRound() {
        gameState = battleState;
        if (battleResolved) {
            if (player.currentHP <= 0) {
                if (isFinalBoss) {
                    // Lost to the final boss — show lose screen, then start over
                    stopMusic();
                    enemyStats = new EnemyStats();
                    items      = new ItemSystem();
                    abilities  = new AbilitySystem();
                    saveData   = null;
                    new java.io.File(System.getProperty("user.home") + java.io.File.separator + "fivesix_save.dat").delete();
                    gameState = loseState;
                    return;
                }
                player.respawnWithPenalty();
            }
            if (isFinalBoss && player.currentHP > 0) {
                // Win — wipe save immediately so there's nothing to continue
                new java.io.File(System.getProperty("user.home") + java.io.File.separator + "fivesix_save.dat").delete();
                saveData = null;
                gameState = winState;
                return;
            }
            if (pendingBattleEnemyColor == COLOR_VAUGHN && player.currentHP > 0) {
                startNarration(); return;
            }
            gameState = playState;
            currentDialog = ""; dialogStage = 0; lastNPCColor = 0;
            battleResolved = false; waitingOutcome = false;
            playMusic(GLE_MAP.equals(currentMapName) ? "gle_soundtrack" : "frontgate_soundtrack");
            autoSave();
        } else {
            battleRound++;
            battleMsg = "Round " + battleRound + " - Choose your move!";
            waitingOutcome = false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  NARRATION (post-Vaughn → final boss)
    // ─────────────────────────────────────────────────────────────
    private void startNarration() {
        currentMapName = EMALL_MAP;
        // Pre-load the emall battle scene so paintNarration and paintPreBattle both have it
        battleSceneImg  = img("/res/gui/pixelart/battle_scene/emall_battle.png");
        outcomeSceneImg = img("/res/gui/pixelart/battle_scene/emall_outcome.png");

        narrating = true; narIndex = 0; eWasNarHeld = false;
        narLines = new String[]{
            "After a long day of fighting, you've finally reclaimed every last peso...",
            "Player: \"Finally... I have all my money back. Jollibee, here I com—\"",
            "???:     \"Excuse me po, sir...\"",
            "Player: \"Huh?\"",
            "Beggar:  \"Sir, pwede po bang makahingi ng barya? Gutom na gutom na po ako.\"",
            "Player: \"Sorry, I can't. I only have just enough for what I need.\"",
            "Beggar:  \"...Just enough? No, no. I saw how much you have. Don't lie to me.\"",
            "Player: \"I'm serious, I don't have anything extra—\"",
            "Beggar:  \"THEN I'LL TAKE IT FROM YOU!!\"",
            "The beggar lunges forward. You have no choice but to fight!"
        };
        currentDialog = narLines[0];
        gameState = narrationState;
    }

    // ─────────────────────────────────────────────────────────────
    //  ITEMS / ABILITIES
    // ─────────────────────────────────────────────────────────────
    private void useItem(ItemSystem.Item item) {
        boolean inBattle = prevStateBeforePanel == battleState;

        // Healing items can only be used outside of battle
        boolean isHealItem = switch (item) {
            case WATER, BARNUTS, GREENCROSS, SLEEPING_MASK -> true;
            default -> false;
        };
        if (inBattle && isHealItem) {
            abilMsg = item.displayName + " can only be used outside battle!";
            abilMsgTimer = 140;
            return; // don't consume the item
        }

        items.remove(item);
        switch (item) {
            case WATER        -> player.currentHP = Math.min(player.currentHP + player.maxHP / 10, player.maxHP);
            case BARNUTS      -> player.currentHP = Math.min(player.currentHP + 50, player.maxHP);
            case GREENCROSS   -> { fxHealRounds = 10; fxHealAmt = 50; player.currentHP = Math.max(1, player.currentHP - 2); }
            case COFFEE       -> player.maxHP += 10;
            case ENERGY_DRINK -> fxEnergyRounds = 10;
            case SLEEPING_MASK-> player.currentHP = player.maxHP;
        }
        abilMsg = "Used " + item.displayName + "!"; abilMsgTimer = 120;
        gameState = prevStateBeforePanel;
        autoSave();
    }

    private void useAbility(AbilitySystem.Ability ability) {
        if (prevStateBeforePanel != battleState) return; // view-only outside battle

        abilities.remove(ability);
        switch (ability) {
            case CLAIRVOYANCE -> {
                clairMove = BattleSystem.getRandomEnemyMove();
                String moveName = switch (clairMove) {
                    case ROCK     -> "Rock";
                    case PAPER    -> "Paper";
                    case SCISSORS -> "Scissors";
                };
                clairText = "👁 " + enemyName + " will use " + moveName + " next turn!";
                clairVisible = true;
            }
            case UNO_REVERSE  -> fxUnoReverse  = true;
            case HYPNOTIZE    -> fxHypnotize   = true;
            case YOU_CHEATER  -> fxYouCheater  = true;
            case FULL_COUNTER -> fxFullCounter = true;
        }
        abilMsg = "Used " + ability.displayName + "!"; abilMsgTimer = 120;
        gameState = prevStateBeforePanel;
        autoSave();
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
        boolean escNow = keyH.escPressed;

        if (escNow && !escWasHeld) {
            if (settingsOpen) {
                settingsOpen = false;
            } else {
                switch (gameState) {
                    case playState -> {
                        currentDialog = "";
                        gameState = menuCharState;
                        stopMusic();
                        playMusic("menu_sountrack");
                    }
                    case menuCharState -> gameState = menuStartState;
                    case menuStartState -> gameState = menuState;
                    case creditsState -> gameState = menuState;
                    case winState -> {
                        stopMusic();
                        player = null;
                        enemyStats = new EnemyStats();
                        items      = new ItemSystem();
                        abilities  = new AbilitySystem();
                        saveData   = null;
                        new java.io.File(System.getProperty("user.home") + java.io.File.separator + "fivesix_save.dat").delete();
                        gameState  = menuState;
                        playMusic("menu_sountrack");
                    }
                    case loseState -> {
                        player = null;
                        gameState = menuState;
                        playMusic("menu_sountrack");
                    }
                    case inventoryState -> gameState = (prevStateBeforePanel == battleState ? battleState : playState);
                    case abilityState -> gameState = (prevStateBeforePanel == battleState ? battleState : playState);
                    default -> { }
                }
            }
        }
        escWasHeld = escNow;

        if (gameState == playState && player != null) {
            player.update();
            if (keyH.f1Pressed && !f1WasHeld) { showDebug = !showDebug; f1WasHeld = true; }
            if (!keyH.f1Pressed) f1WasHeld = false;
        }
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
        }
        g2.dispose();
    }

    // ─────────────────────────────────────────────────────────────
    //  DRAWING HELPERS
    // ─────────────────────────────────────────────────────────────
    private void fill(Graphics2D g2, BufferedImage im) {
        if (im != null) g2.drawImage(im, 0, 0, getWidth(), getHeight(), null);
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
        g2.setColor(new Color(0,0,0,210)); g2.fillRoundRect(bx,by,bw,bh,25,25);
        g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(3)); g2.drawRoundRect(bx,by,bw,bh,25,25);

        // Portrait — stretched to fit #6377FF region in worldGuiHitbox if available,
        // otherwise fall back to fixed position inside dialog box
        BufferedImage port = null;
        if (lastNPCColor != 0) {
            String f = enemyFolder(lastNPCColor);
            if (f != null) port = npcDialog.computeIfAbsent(f,
                k -> img("/res/sprites/enemies/"+k+"/"+k+"_dialog.png"));
        } else if (player != null) {
            port = playerDialogImg;
        }

        if (port != null) {
            // Try to draw in the BC_CHARDIAL hitbox region (world gui)
            Rectangle dialR = (worldGuiHitbox != null) ? bounds(worldGuiHitbox, BC_CHARDIAL) : null;
            if (dialR != null) {
                g2.drawImage(port, dialR.x, dialR.y, dialR.width, dialR.height, null);
            } else {
                // Fallback: draw inside dialog box with shake
                int shX = (talkShake%6<3)?2:-2;
                g2.drawImage(port, bx+12+shX, by+(bh-110)/2, 80, 110, null);
            }
        }

        g2.setFont(new Font("Arial",Font.BOLD,21)); g2.setColor(Color.WHITE);
        int tx = (port != null && bounds(worldGuiHitbox, BC_CHARDIAL) == null) ? bx+110 : bx+20;
        int ty=by+48, mw=bw - (tx - bx) - 20;
        for (String ln : wrap(g2,text,mw)) { g2.drawString(ln,tx,ty); ty+=30; }
        g2.setFont(new Font("Arial",Font.ITALIC,16)); g2.setColor(new Color(180,180,180));
        g2.drawString("Press 'E' to continue...", bx+bw-260, by+bh-18);
    }

    // Cached dialog portraits (loaded once per NPC folder)
    private final Map<String, BufferedImage> npcDialog = new HashMap<>();

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
        drawBtn(g2, menuStartHitbox, BC_CONTINUE, "continue");
        drawBtn(g2, menuStartHitbox, BC_SELCHAR,  "selchar");
        drawBtn(g2, menuStartHitbox, BC_SETTINGS, "settings");
        if (settingsOpen) paintSettingsPanel(g2);
    }

    private void paintCharSelect(Graphics2D g2) {
        fill(g2, menuScreenImg);

        for (String name : List.of("ivan", "sam", "nimuel", "johnfiel")) {
            BufferedImage[] arr = btnImgs.get(name);
            Rectangle r = charButtonRect(name);
            if (arr == null || r == null) continue;
            BufferedImage img = name.equals(hoveredBtn) && arr[1] != null ? arr[1] : arr[0];
            if (img != null) g2.drawImage(img, r.x, r.y, r.width, r.height, null);
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
        fill(g2, menuScreenImg);
        g2.setColor(new Color(0,0,0,200)); g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial",Font.BOLD,48));
        String t="CREDITS"; g2.drawString(t,getWidth()/2-g2.getFontMetrics().stringWidth(t)/2,100);
        g2.setFont(new Font("Arial",Font.PLAIN,28));
        int y=180;
        for (String n : new String[]{"Developer Team","","Ivan","Nimuel","Sam","Johnfiel","","Press ESC to go back"}) {
            g2.drawString(n,getWidth()/2-g2.getFontMetrics().stringWidth(n)/2,y); y+=44;
        }
    }

    private void paintSettingsPanel(Graphics2D g2) {
        Rectangle panel = settingsPanelRect();
        int px = panel.x, py = panel.y, pw = panel.width, ph = panel.height;
        g2.setColor(new Color(20,20,40,240)); g2.fillRoundRect(px,py,pw,ph,20,20);
        g2.setColor(new Color(100,150,255)); g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(px,py,pw,ph,20,20);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial",Font.BOLD,28));
        g2.drawString("SETTINGS",px+140,py+50);
        String mk = isMuted ? "wmuted" : "wmute";
        BufferedImage[] ma = btnImgs.get(mk);
        boolean hoverMute = settingsMuteRect().contains(mouse);
        BufferedImage muteImg = (ma != null) ? ((hoverMute && ma[1] != null) ? ma[1] : ma[0]) : null;
        if (muteImg != null) {
            Rectangle muteRect = settingsMuteRect();
            g2.drawImage(muteImg, muteRect.x, muteRect.y, muteRect.width, muteRect.height, null);
        }
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial",Font.PLAIN,22));
        g2.drawString(isMuted?"Unmute":"Mute",px+200,py+120);
        g2.setFont(new Font("Arial",Font.ITALIC,16)); g2.setColor(new Color(180,180,180));
        g2.drawString("Click outside the panel to dismiss",px+55,py+ph-18);
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
        return new Rectangle(getWidth()/2-200, getHeight()/2-125, 400, 250);
    }

    private Rectangle settingsMuteRect() {
        Rectangle panel = settingsPanelRect();
        return new Rectangle(panel.x + 60, panel.y + 80, 120, 60);
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
            BufferedImage emallBg = img("/res/gui/pixelart/battle_scene/emall_battle.png");
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
        return bounds(worldGuiHitbox, BC_MUTE);
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
            drawBtn(g2, battleHitbox, BC_ROCKBTN,  "rock");
            drawBtn(g2, battleHitbox, BC_PAPERBTN, "paper");
            drawBtn(g2, battleHitbox, BC_SCISSBTN, "scissors");
            drawBtn(g2, battleHitbox, BC_USEITEM,  "useitem");
            drawBtn(g2, battleHitbox, BC_USEABIL,  "useabil");
        }
        // Active effects
        int ey=90; g2.setFont(new Font("Arial",Font.BOLD,17));
        for (String fx : activeEffects()) {
            g2.setColor(new Color(80,255,180)); g2.drawString(fx,40,ey); ey+=24;
        }
        // Ability message
        if (abilMsgTimer>0) {
            g2.setFont(new Font("Arial",Font.BOLD,28));
            g2.setColor(new Color(100,255,150));
            int aw=g2.getFontMetrics().stringWidth(abilMsg);
            g2.drawString(abilMsg, getWidth()/2-aw/2, getHeight()/2-40);
        }
        // Clairvoyance
        if (clairVisible) {
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
    }

    private List<String> activeEffects() {
        List<String> out=new ArrayList<>();
        if(fxUnoReverse)   out.add("Uno Reverse: ON");
        if(fxHypnotize)    out.add("Hypnotize: ON");
        if(fxYouCheater)   out.add("You Cheater: ON");
        if(fxFullCounter)  out.add("Full Counter: ON");
        if(fxHealRounds>0) out.add("Ticking Heal: "+fxHealRounds+" rounds");
        if(fxEnergyRounds>0) out.add("Energy Drink: "+fxEnergyRounds+" rounds");
        return out;
    }

    // ─────────────────────────────────────────────────────────────
    //  OUTCOME STATE
    //  Layer: outcome_scene > RPS image (BC_OUTCZONE region) > continue btn
    // ─────────────────────────────────────────────────────────────
    private void paintOutcome(Graphics2D g2) {
        fill(g2, outcomeSceneImg);
        if (outcomeRPSImg != null && outcomeHitbox != null) {
            Rectangle r = bounds(outcomeHitbox, BC_OUTCZONE);
            if (r!=null) g2.drawImage(outcomeRPSImg, r.x, r.y, r.width, r.height, null);
        }
        drawBtn(g2, outcomeHitbox, BC_CONTBAT, "contbat");
        Rectangle continueRect = bounds(outcomeHitbox, BC_CONTBAT);
        g2.setFont(new Font("Arial",Font.BOLD,28));
        int my = continueRect != null ? continueRect.y + continueRect.height + 48 : getHeight()-120;
        for (String ln : wrap(g2, battleMsg, getWidth()-200)) {
            int lw=g2.getFontMetrics().stringWidth(ln);
            g2.setColor(new Color(255,230,100)); g2.drawString(ln,getWidth()/2-lw/2,my); my+=36;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  INVENTORY / ABILITY
    // ─────────────────────────────────────────────────────────────
    private void paintInventory(Graphics2D g2) {
        if (prevStateBeforePanel == battleState) {
            paintBattle(g2);
        } else {
            fill(g2, mapImage);
            g2.setColor(new Color(0,0,0,100));
            g2.fillRect(0,0,getWidth(),getHeight());
        }
        boolean inBattle = prevStateBeforePanel == battleState;
        int pw=520,ph=420,px=getWidth()/2-pw/2,py=getHeight()/2-ph/2;
        panel(g2,px,py,pw,ph,"ITEMS");
        List<ItemSystem.Item> list = items.getItems();
        int iy=py+72; g2.setFont(new Font("Arial",Font.PLAIN,20));
        for (ItemSystem.Item item : list) {
            int cnt = items.count(item);
            boolean isHeal = switch (item) {
                case WATER, BARNUTS, GREENCROSS, SLEEPING_MASK -> true;
                default -> false;
            };
            boolean locked = inBattle && isHeal;
            boolean h = !locked && new Rectangle(px+20,iy-28,pw-40,38).contains(mouse);
            g2.setColor(locked ? new Color(50,30,30) : (h ? new Color(90,90,180) : new Color(40,40,100)));
            g2.fillRoundRect(px+20,iy-28,pw-40,38,8,8);
            g2.setColor(locked ? new Color(110,80,80) : Color.WHITE);
            g2.drawString("x"+cnt+"  "+item.displayName+" — "+item.description+(locked?" [battle only: locked]":""), px+34, iy);
            iy+=48; if(iy>py+ph-44) break;
        }
        if (list.isEmpty()) { g2.setColor(new Color(180,180,180)); g2.setFont(new Font("Arial",Font.ITALIC,22)); g2.drawString("No items",px+pw/2-44,py+ph/2); }
        g2.setColor(new Color(160,160,160)); g2.setFont(new Font("Arial",Font.ITALIC,15));
        String hint = inBattle ? "Healing items locked in battle  |  ESC to close" : "Click item to use  |  ESC to close";
        g2.drawString(hint, px+32, py+ph-14);
    }

    private void paintAbility(Graphics2D g2) {
        if (prevStateBeforePanel == battleState) {
            paintBattle(g2);
        } else {
            g2.setColor(new Color(20, 10, 50));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        boolean inBattle = prevStateBeforePanel == battleState;
        int pw = 560, ph = 440, px = getWidth()/2 - pw/2, py = getHeight()/2 - ph/2;

        // Panel box — bright border so it's clearly visible
        g2.setColor(new Color(30, 25, 80));
        g2.fillRoundRect(px, py, pw, ph, 18, 18);
        g2.setColor(new Color(120, 180, 255));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, pw, ph, 18, 18);

        // Title
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        String title = "ABILITIES";
        g2.drawString(title, px + pw/2 - g2.getFontMetrics().stringWidth(title)/2, py + 42);

        // Divider
        g2.setColor(new Color(80, 100, 200));
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(px + 20, py + 54, px + pw - 20, py + 54);

        // Rows
        List<AbilitySystem.Ability> unique = abilities.getUnique();
        int ay = py + 88;
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        for (AbilitySystem.Ability ab : unique) {
            int cnt = abilities.count(ab);
            boolean hovered = inBattle && new Rectangle(px+16, ay-26, pw-32, 36).contains(mouse);
            g2.setColor(hovered ? new Color(80, 80, 200) : new Color(50, 45, 120));
            g2.fillRoundRect(px+16, ay-26, pw-32, 36, 8, 8);
            g2.setColor(inBattle ? Color.WHITE : new Color(190, 190, 190));
            g2.drawString("x" + cnt + "  " + ab.displayName + " — " + ab.description, px + 28, ay);
            ay += 46;
            if (ay > py + ph - 40) break;
        }
        if (unique.isEmpty()) {
            g2.setColor(new Color(160, 160, 160));
            g2.setFont(new Font("Arial", Font.ITALIC, 20));
            g2.drawString("No abilities", px + pw/2 - 55, py + ph/2);
        }

        // Footer
        g2.setColor(new Color(130, 130, 130));
        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        String hint = inBattle ? "Click an ability to use it  |  ESC to close" : "View only outside battle  |  ESC to close";
        g2.drawString(hint, px + 20, py + ph - 12);
    }

    private void panel(Graphics2D g2, int px, int py, int pw, int ph, String title) {
        g2.setColor(new Color(10,10,30,240)); g2.fillRoundRect(px,py,pw,ph,20,20);
        g2.setColor(new Color(100,150,255)); g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(px,py,pw,ph,20,20);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial",Font.BOLD,30));
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
        boolean isPlayerLine = line.startsWith("Player");
        boolean isNarLine    = !line.contains(":") || line.startsWith("The ");

        int bx = 50, by = getHeight() - 210, bw = getWidth() - 100, bh = 165;
        g2.setColor(new Color(0, 0, 0, 210));
        g2.fillRoundRect(bx, by, bw, bh, 22, 22);
        g2.setColor(isNarLine ? new Color(220, 200, 80) : Color.WHITE);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(bx, by, bw, bh, 22, 22);

        // Portrait on left if it's the player speaking
        int textX = bx + 20;
        if (isPlayerLine && playerDialogImg != null) {
            g2.drawImage(playerDialogImg, bx + 14, by + (bh - 115) / 2, 82, 115, null);
            textX = bx + 108;
        }

        // Text
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        int ty = by + 52;
        for (String ln : wrap(g2, line, bw - (textX - bx) - 24)) {
            g2.drawString(ln, textX, ty);
            ty += 34;
        }

        // Prompt
        g2.setFont(new Font("Arial", Font.ITALIC, 16));
        g2.setColor(new Color(170, 170, 170));
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
        boolean isPlayerLine = !isNarration && preBattleIndex % 2 == 0;

        int bx = 60, by = getHeight() - 200, bw = getWidth() - 120, bh = 150;
        g2.setColor(new Color(0, 0, 0, 220)); g2.fillRoundRect(bx, by, bw, bh, 25, 25);
        g2.setColor(isNarration ? new Color(180, 180, 80) : Color.WHITE);
        g2.setStroke(new BasicStroke(3)); g2.drawRoundRect(bx, by, bw, bh, 25, 25);

        // Portrait
        BufferedImage port = isPlayerLine ? playerDialogImg : (isNarration ? null : enemyDialogImg);
        int textX = bx + 20;
        if (port != null) {
            int shX = (talkShake % 6 < 3) ? 2 : -2;
            g2.drawImage(port, bx + 12 + shX, by + (bh - 110) / 2, 80, 110, null);
            textX = bx + 110;
        }

        // Dialogue text
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        int ty = by + 50;
        int maxW = bw - (textX - bx) - 20;
        for (String ln : wrap(g2, line, maxW)) {
            g2.drawString(ln, textX, ty); ty += 32;
        }

        // Prompt
        g2.setFont(new Font("Arial", Font.ITALIC, 16));
        g2.setColor(new Color(180, 180, 180));
        boolean isLast = preBattleIndex >= preBattleLines.length - 1;
        String prompt = isLast ? "Press 'E' to begin the battle!" : "Press 'E' to continue...";
        g2.drawString(prompt, bx + bw - 280, by + bh - 18);
    }

    private void paintWin(Graphics2D g2) {
        g2.setColor(Color.BLACK); g2.fillRect(0,0,getWidth(),getHeight());
        if (player != null) { BufferedImage wi = charSelectImg.get(player.characterName); if(wi!=null) fill(g2,wi); }
        g2.setColor(new Color(0,0,0,160)); g2.fillRect(0,getHeight()-200,getWidth(),200);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Arial",Font.BOLD,26));
        String txt="After that weird encounter you finally get to enjoy your long awaited meal at Jollibee!!";
        int ty=getHeight()-170;
        for (String ln : wrap(g2,txt,getWidth()-80)) {
            g2.drawString(ln,getWidth()/2-g2.getFontMetrics().stringWidth(ln)/2,ty); ty+=38;
        }
        g2.setFont(new Font("Arial",Font.ITALIC,20)); g2.setColor(new Color(200,200,200));
        String back="Press ESC to return to menu";
        g2.drawString(back,getWidth()/2-g2.getFontMetrics().stringWidth(back)/2,getHeight()-55);
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
        String back = "Press ESC to return to the main menu and start over";
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
}