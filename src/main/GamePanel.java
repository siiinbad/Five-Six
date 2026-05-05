package main;

import entity.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
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
    private static final double CHARACTER_SCALE_BOOST = 1.25;
    public int screenWidth  = tileSize * 16;
    public int screenHeight = tileSize * 12;

    // SYSTEM
    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;
    public Player player;
    public BufferedImage mapImage, hitboxImage, mapTitleImage;
    public BufferedImage menuBackgroundImage, menuLogoImage;
    public BufferedImage startIdleImage, startHoverImage;
    public BufferedImage creditsIdleImage, creditsHoverImage;
    public BufferedImage quitIdleImage, quitHoverImage;
    public BufferedImage continueIdleImage, continueHoverImage;
    public BufferedImage selectCharacterIdleImage, selectCharacterHoverImage;
    public BufferedImage muteIdleImage, muteHoverImage;
    public BufferedImage settingsIdleImage, settingsHoverImage;
    public BufferedImage[] characterButtonIdleImages, characterButtonHoverImages;
    public BufferedImage[] characterSelectImages;
    public BufferedImage menuStartHitboxImage, menuGuiHitboxImage, menuCharacterSelectHitboxImage;
    public String currentMapName = "gle";
    public static final String START_MAP = "gle";
    public static final String WALKWAY_MAP = "walkWay";

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
    public final int COLOR_MENU_LOGO   = 0xBA8D7D;
    public final int COLOR_CONTINUE    = 0xCE74FF;
    public final int COLOR_SELECT_CHAR = 0x47B2FF;
    public final int COLOR_CHAR_IVAN   = 0xFF0000;
    public final int COLOR_CHAR_SAM    = 0xFFE679;
    public final int COLOR_CHAR_NIMUEL = 0xFFADAD;
    public final int COLOR_CHAR_JOHNFIEL = 0x0EFC7B;
    private static final int COLOR_CHAR_PREVIEW = 0x078F00;

    /** Battle UI regions on {@code battle_hitbox.png} (see design doc). */
    private static final int COLOR_BATTLE_ITEMS      = 0xFEA800;
    private static final int COLOR_BATTLE_ABILITIES  = 0xA600FF;
    private static final int COLOR_BATTLE_PICK_ROCK  = 0xA4A29E;
    private static final int COLOR_BATTLE_PICK_PAPER = 0x77FF88;
    private static final int COLOR_BATTLE_PICK_SCISSORS = 0xC00000;
    /** In-battle continue / advance (distinct from main-menu continue). */
    private static final int COLOR_BATTLE_CONTINUE_PINK = 0xFF00C7;
    /** Sprite placement on {@code battle_sprite_hitbox.png}. */
    private static final int COLOR_BATTLE_PLAYER_SLOT = 0x444757;
    private static final int COLOR_BATTLE_ENEMY_SLOT  = 0x692323;
    /** Main art panel on {@code outcome_hitbox.png} (muted rectangle behind outcome PNG). */
    private static final int COLOR_OUTCOME_ART_PANEL = 0xBC9995;

    /** Fallback battle controls when {@code battleButton} PNGs are absent (wood panel style). */
    private static final Color BATTLE_BTN_WOOD_FILL   = new Color(210, 185, 145);
    private static final Color BATTLE_BTN_WOOD_BORDER = new Color(92, 58, 32);

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
    private int menuMuteColor = 0;
    private int menuSettingsColor = 0;
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
    private BufferedImage battleSceneImage;
    private BufferedImage battleHitboxImage;
    private BufferedImage battleSpriteHitboxImage;
    private BufferedImage battlePlayerImage;
    private BufferedImage battleEnemyImage;
    /** Set when battle sprites load — avoids scanning full PNG alpha every frame (was freezing the UI). */
    private Rectangle battlePlayerOpaqueBounds;
    private Rectangle battleEnemyOpaqueBounds;

    private BufferedImage battleItemIdle, battleItemHover;
    private BufferedImage battleAbilityIdle, battleAbilityHover;
    private BufferedImage battleRockIdle, battleRockHover;
    private BufferedImage battlePaperIdle, battlePaperHover;
    private BufferedImage battleScissorsIdle, battleScissorsHover;
    private BufferedImage battleContinueIdle, battleContinueHover;

    /** Full-screen outcome scene and layout hitbox ({@code gle_outcome.png}, {@code outcome_hitbox.png}). */
    private BufferedImage outcomeSceneImage;
    private BufferedImage outcomeHitboxImage;
    /** Win/lose illustration from {@code battleOutcomes/}; cleared when a new battle starts. */
    private BufferedImage battleOutcomeArtImage;

    private String battleHoverZone = "";

    // BATTLE BUTTONS (screen-space; from colored regions on battle_hitbox, or fallback layout)
    private Rectangle itemsBattleBtn = new Rectangle(0, 0, 0, 0);
    private Rectangle abilitiesBattleBtn = new Rectangle(0, 0, 0, 0);
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
                    String menuButton = getCharacterSelectButtonAt(p);
                    if ("mute".equals(menuButton) || "settings".equals(menuButton)) {
                        return;
                    }
                    String selectedCharacter = getCharacterNameAt(p);
                    if (selectedCharacter != null) {
                        selectChar(selectedCharacter);
                    }
                }

                if (gameState == battleState) {
                    handleBattleClick(p);
                }
            }
        });

        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (menuFadeActive) return;
                Point p = e.getPoint();
                if (gameState == battleState) {
                    String z = battleHoverZoneFromPoint(p);
                    if (!z.equals(battleHoverZone)) {
                        battleHoverZone = z;
                        repaint();
                    }
                    return;
                }
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
                hoveredMenuButton = getCharacterSelectButtonAt(p);
                hoveredCharIndex = getCharacterIndexAt(p);
                if (hoveredCharIndex != prevChar || !hoveredMenuButton.equals(prevButton)) repaint();
            }
        });
    }

    /**
     * Screen-space layout: top row Items | Abilities, bottom row Rock | Paper | Scissors,
     * centered cluster (~reference layout). Overwritten by {@code battle_hitbox.png} regions when present.
     */
    private void layoutFallbackBattleButtons() {
        int w = getWidth();
        int h = getHeight();
        int midX = w / 2;
        int bh = Math.max(44, Math.min(68, h / 13));
        int padBottom = Math.max(18, h / 26);
        int rowGap = Math.max(8, h / 80);
        int hGap = Math.max(8, w / 100);
        int btnY = h - padBottom - bh;
        int topY = btnY - bh - rowGap;

        int maxCluster = Math.min(640, w - 40);
        int bottomW = (maxCluster - 2 * hGap) / 3;
        int topW = Math.min((maxCluster - hGap) / 2, Math.max(48, (int) (bottomW * 0.90)));
        int bottomCluster = 3 * bottomW + 2 * hGap;
        int topCluster = 2 * topW + hGap;

        int bottomStart = midX - bottomCluster / 2;
        int topStart = midX - topCluster / 2;

        itemsBattleBtn = new Rectangle(topStart, topY, topW, bh);
        abilitiesBattleBtn = new Rectangle(topStart + topW + hGap, topY, topW, bh);
        rockBtn = new Rectangle(bottomStart, btnY, bottomW, bh);
        paperBtn = new Rectangle(bottomStart + bottomW + hGap, btnY, bottomW, bh);
        scissorsBtn = new Rectangle(bottomStart + 2 * (bottomW + hGap), btnY, bottomW, bh);
    }

    private void updateBattleButtons() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        layoutFallbackBattleButtons();

        int midX = getWidth() / 2;
        int bh = Math.max(44, Math.min(68, getHeight() / 13));
        int padBottom = Math.max(18, getHeight() / 26);
        int btnY = getHeight() - padBottom - bh;

        if (waitingForNext && outcomeHitboxImage != null) {
            Rectangle contR = getColorBounds(outcomeHitboxImage, COLOR_BATTLE_CONTINUE_PINK);
            continueBtn = contR != null ? contR : new Rectangle(midX - 100, btnY, 200, bh);
            return;
        }

        if (battleHitboxImage != null) {
            Rectangle itemsR = getColorBounds(battleHitboxImage, COLOR_BATTLE_ITEMS);
            Rectangle abilR = getColorBounds(battleHitboxImage, COLOR_BATTLE_ABILITIES);
            Rectangle rockR = getColorBounds(battleHitboxImage, COLOR_BATTLE_PICK_ROCK);
            Rectangle paperR = getColorBounds(battleHitboxImage, COLOR_BATTLE_PICK_PAPER);
            Rectangle sciR = getColorBounds(battleHitboxImage, COLOR_BATTLE_PICK_SCISSORS);
            Rectangle contR = getColorBounds(battleHitboxImage, COLOR_BATTLE_CONTINUE_PINK);

            // getColorBounds() already maps hitbox image → panel pixels; do not map twice.
            if (itemsR != null) {
                itemsBattleBtn = itemsR;
            }
            if (abilR != null) {
                abilitiesBattleBtn = abilR;
            }
            if (rockR != null) {
                rockBtn = rockR;
            }
            if (paperR != null) {
                paperBtn = paperR;
            }
            if (sciR != null) {
                scissorsBtn = sciR;
            }

            boolean haveRps = rockR != null && paperR != null && sciR != null;
            if (!haveRps) {
                Rectangle[] blob = detectBattleButtonBlobsImageSpace();
                if (blob != null && blob.length >= 5) {
                    java.util.Arrays.sort(blob, (a, b) -> {
                        int rowA = a.y / 20;
                        int rowB = b.y / 20;
                        if (rowA != rowB) return Integer.compare(rowA, rowB);
                        return Integer.compare(a.x, b.x);
                    });
                    itemsBattleBtn = mapHitboxImageRectToScreen(blob[0], battleHitboxImage);
                    abilitiesBattleBtn = mapHitboxImageRectToScreen(blob[1], battleHitboxImage);
                    rockBtn = mapHitboxImageRectToScreen(blob[2], battleHitboxImage);
                    paperBtn = mapHitboxImageRectToScreen(blob[3], battleHitboxImage);
                    scissorsBtn = mapHitboxImageRectToScreen(blob[4], battleHitboxImage);
                }
            }

            if (waitingForNext) {
                continueBtn = contR != null
                        ? contR
                        : new Rectangle(midX - 100, btnY, 200, bh);
            } else {
                continueBtn = new Rectangle(0, 0, 0, 0);
            }
            return;
        }

        if (waitingForNext) {
            continueBtn = new Rectangle(midX - 100, btnY, 200, bh);
        } else {
            continueBtn = new Rectangle(0, 0, 0, 0);
        }
    }

    private Rectangle mapHitboxImageRectToScreen(Rectangle imageRect, BufferedImage reference) {
        int x = imageRect.x * getWidth() / Math.max(1, reference.getWidth());
        int y = imageRect.y * getHeight() / Math.max(1, reference.getHeight());
        int w = Math.max(1, imageRect.width * getWidth() / Math.max(1, reference.getWidth()));
        int h = Math.max(1, imageRect.height * getHeight() / Math.max(1, reference.getHeight()));
        return new Rectangle(x, y, w, h);
    }

    private void handleBattleClick(Point p) {
        if (waitingForNext) {
            if (outcomeHitboxImage != null) {
                if (continueBtn != null && continueBtn.width > 0 && continueBtn.contains(p)) {
                    nextRound();
                }
                return;
            }
            if (battleHitboxImage != null) {
                int c = getMenuColorAt(p, battleHitboxImage);
                if (c == COLOR_BATTLE_CONTINUE_PINK
                        || (continueBtn != null && continueBtn.width > 0 && continueBtn.contains(p))) {
                    nextRound();
                }
            } else if (continueBtn != null && continueBtn.width > 0 && continueBtn.contains(p)) {
                nextRound();
            }
            return;
        }

        String zone = battlePickNearestButtonZone(p);
        if ("rock".equals(zone)) {
            resolveBattle(BattleSystem.Move.ROCK);
        } else if ("paper".equals(zone)) {
            resolveBattle(BattleSystem.Move.PAPER);
        } else if ("scissors".equals(zone)) {
            resolveBattle(BattleSystem.Move.SCISSORS);
        }
    }

    /**
     * Hit-test battle buttons using screen rectangles only. If several rects overlap (common when
     * {@code battle_hitbox.png} regions are loose), pick the button whose center is closest to the cursor.
     */
    private String battlePickNearestButtonZone(Point p) {
        String[] names = {"items", "abilities", "rock", "paper", "scissors"};
        Rectangle[] rects = {itemsBattleBtn, abilitiesBattleBtn, rockBtn, paperBtn, scissorsBtn};
        java.util.ArrayList<Integer> hit = new java.util.ArrayList<>(5);
        for (int i = 0; i < names.length; i++) {
            Rectangle r = rects[i];
            if (r != null && r.width > 0 && r.height > 0 && r.contains(p)) {
                hit.add(i);
            }
        }
        if (hit.isEmpty()) {
            return "";
        }
        if (hit.size() == 1) {
            return names[hit.get(0)];
        }
        int bestIdx = hit.get(0);
        long bestD = battleDistSqToRectCenter(p, rects[bestIdx]);
        for (int k = 1; k < hit.size(); k++) {
            int i = hit.get(k);
            long d = battleDistSqToRectCenter(p, rects[i]);
            if (d < bestD) {
                bestD = d;
                bestIdx = i;
            }
        }
        return names[bestIdx];
    }

    private static long battleDistSqToRectCenter(Point p, Rectangle r) {
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        long dx = p.x - cx;
        long dy = p.y - cy;
        return dx * dx + dy * dy;
    }

    private String battleHoverZoneFromPoint(Point p) {
        if (waitingForNext) {
            if (outcomeHitboxImage != null) {
                if (continueBtn != null && continueBtn.width > 0 && continueBtn.contains(p)) return "continue";
                return "";
            }
            if (battleHitboxImage != null) {
                int c = getMenuColorAt(p, battleHitboxImage);
                if (c == COLOR_BATTLE_CONTINUE_PINK) return "continue";
            }
            if (continueBtn != null && continueBtn.width > 0 && continueBtn.contains(p)) return "continue";
            return "";
        }
        return battlePickNearestButtonZone(p);
    }

    private void loadImages() {
        try {
            mapImage    = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/" + currentMapName + ".png")));
            hitboxImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/" + currentMapName + "Hitboxes.png")));
            mapTitleImage = readOptionalImage("/res/sprites/map/" + currentMapName + "_title.png");

            jamesStand       = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/enemies/james/james_stand.png")));
            alieyandrewStand = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/enemies/alieyandrew/alieyandrew_stand.png")));
            kyleStand        = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/enemies/kyle/kyle_stand.png")));
            johnruStand      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/enemies/johnru/johnru_stand.png")));
            adrianStand      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/enemies/adrian/adrian_stand.png")));

            ivanStands = new BufferedImage[4];
            ivanStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/ivan/ivan_front_stand.png")));
            ivanStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/ivan/ivan_left_stand.png")));
            ivanStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/ivan/ivan_back_stand.png")));
            ivanStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/ivan/ivan_right_stand.png")));

            nimuelStands = new BufferedImage[4];
            nimuelStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/nimuel/nimuel_front_stand.png")));
            nimuelStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/nimuel/nimuel_left_stand.png")));
            nimuelStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/nimuel/nimuel_back_stand.png")));
            nimuelStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/nimuel/nimuel_right_stand.png")));

            samStands = new BufferedImage[4];
            samStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/sam/sam_front_stand.png")));
            samStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/sam/sam_left_stand.png")));
            samStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/sam/sam_back_stand.png")));
            samStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/sam/sam_right_stand.png")));

            johnfielStands = new BufferedImage[4];
            johnfielStands[0] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/johnfiel/johnfiel_front_stand.png")));
            johnfielStands[1] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/johnfiel/johnfiel_left_stand.png")));
            johnfielStands[2] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/johnfiel/johnfiel_back_stand.png")));
            johnfielStands[3] = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/player/johnfiel/johnfiel_right_stand.png")));
            menuBackgroundImage = readImage("/res/sprites/menu/gui/menu_screen.png");
            menuLogoImage = readImage("/res/sprites/menu/gui/fixsix_log.png");

            startIdleImage = readImage("/res/sprites/menu/button/start_idle.png");
            startHoverImage = readImage("/res/sprites/menu/button/start_hover.png");
            creditsIdleImage = readImage("/res/sprites/menu/button/credits_idle.png");
            creditsHoverImage = readImage("/res/sprites/menu/button/credits_hover.png");
            quitIdleImage = readImage("/res/sprites/menu/button/quit_idle.png");
            quitHoverImage = readImage("/res/sprites/menu/button/quit_hover.png");
            continueIdleImage = readImage("/res/sprites/menu/button/continue_idle.png");
            continueHoverImage = readImage("/res/sprites/menu/button/continue_hover.png");
            selectCharacterIdleImage = readImage("/res/sprites/menu/button/selectcharacter_idle.png");
            selectCharacterHoverImage = readImage("/res/sprites/menu/button/selectcharacter_hover.png");
            muteIdleImage = readImage("/res/sprites/menu/button/mute_idle.png");
            muteHoverImage = readImage("/res/sprites/menu/button/mute_hover.png");
            settingsIdleImage = readImage("/res/sprites/menu/button/settings_idle.png");
            settingsHoverImage = readImage("/res/sprites/menu/button/settings_hover.png");

            characterButtonIdleImages = new BufferedImage[] {
                    readImage("/res/sprites/menu/buttonPlayer/ivan_idle.png"),
                    readImage("/res/sprites/menu/buttonPlayer/nimuel_idle.png"),
                    readImage("/res/sprites/menu/buttonPlayer/sam_idle.png"),
                    readImage("/res/sprites/menu/buttonPlayer/johnfiel_idle.png")
            };

            characterButtonHoverImages = new BufferedImage[] {
                    readImage("/res/sprites/menu/buttonPlayer/ivan_hover.png"),
                    readImage("/res/sprites/menu/buttonPlayer/nimuel_hover.png"),
                    readImage("/res/sprites/menu/buttonPlayer/sam_hover.png"),
                    readImage("/res/sprites/menu/buttonPlayer/johnfiel_hover.png")
            };

            characterSelectImages = new BufferedImage[] {
                    readImage("/res/sprites/menu/menuCharacterSelect/ivan_selectcharacter.png"),
                    readImage("/res/sprites/menu/menuCharacterSelect/nimuel_selectcharacter.png"),
                    readImage("/res/sprites/menu/menuCharacterSelect/sam_selectcharacter.png"),
                    readImage("/res/sprites/menu/menuCharacterSelect/johnfiel_selectcharacter.png")
            };

            menuStartHitboxImage = readImage("/res/sprites/menu/menuHitbox/menu_start_hitbox.png");
            menuGuiHitboxImage = readImage("/res/sprites/menu/menuHitbox/menu_gui_hitbox.png");
            menuCharacterSelectHitboxImage = readImage("/res/sprites/menu/menuHitbox/menu_characterselect_hitbox.png");
            battleHitboxImage = readOptionalImage("/res/sprites/menu/menuHitbox/battle_hitbox.png");
            battleSpriteHitboxImage = readOptionalImage("/res/sprites/menu/menuHitbox/battle_sprite_hitbox.png");
            loadBattleUiArt();
            refreshMenuIconColors();
        } catch (Exception e) {
            System.out.println("Image loading failed.");
            e.printStackTrace();
        }
    }

    private BufferedImage readImage(String path) throws Exception {
        return ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    private BufferedImage readOptionalImage(String path) {
        try {
            java.io.InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) return null;
            return ImageIO.read(stream);
        } catch (Exception e) {
            return null;
        }
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

    private int getHitboxColorAtBaseRectCenter(BufferedImage hitbox, Rectangle baseRect) {
        if (hitbox == null || baseRect == null) return 0;
        int centerBaseX = baseRect.x + baseRect.width / 2;
        int centerBaseY = baseRect.y + baseRect.height / 2;
        int imageX = Math.min(hitbox.getWidth() - 1, Math.max(0, centerBaseX * hitbox.getWidth() / 500));
        int imageY = Math.min(hitbox.getHeight() - 1, Math.max(0, centerBaseY * hitbox.getHeight() / 342));
        return hitbox.getRGB(imageX, imageY) & 0xFFFFFF;
    }

    private void refreshMenuIconColors() {
        menuMuteColor = getHitboxColorAtBaseRectCenter(menuCharacterSelectHitboxImage, muteBtn);
        menuSettingsColor = getHitboxColorAtBaseRectCenter(menuCharacterSelectHitboxImage, settingsBtn);

        if (menuMuteColor == 0) menuMuteColor = getHitboxColorAtBaseRectCenter(menuGuiHitboxImage, muteBtn);
        if (menuSettingsColor == 0) menuSettingsColor = getHitboxColorAtBaseRectCenter(menuGuiHitboxImage, settingsBtn);
        if (menuMuteColor == 0) menuMuteColor = getHitboxColorAtBaseRectCenter(menuStartHitboxImage, muteBtn);
        if (menuSettingsColor == 0) menuSettingsColor = getHitboxColorAtBaseRectCenter(menuStartHitboxImage, settingsBtn);
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
        int color = getMenuColorAt(p, menuStartHitboxImage);
        if (color == COLOR_MENU_START) {
            hoveredMenuColor = color;
            return "start";
        }
        if (color == COLOR_MENU_CREDIT) {
            hoveredMenuColor = color;
            return "credits";
        }
        if (color == COLOR_MENU_QUIT) {
            hoveredMenuColor = color;
            return "quit";
        }
        if (color == COLOR_MENU_LOGO) {
            hoveredMenuColor = color;
            return "logo";
        }
        if (menuMuteColor != 0 && color == menuMuteColor) {
            hoveredMenuColor = color;
            return "mute";
        }
        if (menuSettingsColor != 0 && color == menuSettingsColor) {
            hoveredMenuColor = color;
            return "settings";
        }

        if (containsBasePoint(startBtn, p)) {
            hoveredMenuColor = COLOR_MENU_START;
            return "start";
        }
        if (containsBasePoint(creditsBtn, p)) {
            hoveredMenuColor = COLOR_MENU_CREDIT;
            return "credits";
        }
        if (containsBasePoint(quitBtn, p)) {
            hoveredMenuColor = COLOR_MENU_QUIT;
            return "quit";
        }

        hoveredMenuColor = 0;
        if (containsBasePoint(muteBtn, p)) return "mute";
        if (containsBasePoint(settingsBtn, p)) return "settings";
        return "";
    }

    private String getStartMenuButtonAt(Point p) {
        int color = getMenuColorAt(p, menuGuiHitboxImage);
        if (color == COLOR_CONTINUE) {
            hoveredMenuColor = color;
            return "continue";
        }
        if (color == COLOR_SELECT_CHAR) {
            hoveredMenuColor = color;
            return "selectCharacter";
        }
        if (menuMuteColor != 0 && color == menuMuteColor) {
            hoveredMenuColor = color;
            return "mute";
        }
        if (menuSettingsColor != 0 && color == menuSettingsColor) {
            hoveredMenuColor = color;
            return "settings";
        }

        if (containsBasePoint(continueMenuBtn, p)) {
            hoveredMenuColor = COLOR_CONTINUE;
            return "continue";
        }
        if (containsBasePoint(selectCharacterBtn, p)) {
            hoveredMenuColor = COLOR_SELECT_CHAR;
            return "selectCharacter";
        }

        hoveredMenuColor = 0;
        if (containsBasePoint(muteBtn, p)) return "mute";
        if (containsBasePoint(settingsBtn, p)) return "settings";
        return "";
    }

    private int getCharacterIndexAt(Point p) {
        int color = getMenuColorAt(p, menuCharacterSelectHitboxImage);

        if (color == COLOR_CHAR_IVAN) return 0;
        if (color == COLOR_CHAR_NIMUEL) return 1;
        if (color == COLOR_CHAR_SAM) return 2;
        if (color == COLOR_CHAR_JOHNFIEL) return 3;

        if (containsBasePoint(ivanBtn, p)) return 0;
        if (containsBasePoint(nimuelBtn, p)) return 1;
        if (containsBasePoint(samBtn, p)) return 2;
        if (containsBasePoint(johnfielBtn, p)) return 3;
        return -1;
    }

    private String getCharacterSelectButtonAt(Point p) {
        int color = getMenuColorAt(p, menuCharacterSelectHitboxImage);
        if (menuMuteColor != 0 && color == menuMuteColor) {
            hoveredMenuColor = color;
            return "mute";
        }
        if (menuSettingsColor != 0 && color == menuSettingsColor) {
            hoveredMenuColor = color;
            return "settings";
        }

        hoveredMenuColor = 0;
        if (containsBasePoint(muteBtn, p)) return "mute";
        if (containsBasePoint(settingsBtn, p)) return "settings";
        return "";
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

    private Rectangle getImageColorBounds(BufferedImage image, int targetColor) {
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
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private Rectangle getNonTransparentBounds(BufferedImage image) {
        if (image == null) return null;

        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha == 0) continue;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) return null;
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
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
        battleOutcomeArtImage = null;
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

    /** Starts battle immediately (no fade / no intermediate screen). */
    public void startFadeToBlack() {
        currentDialog = "";
        // Prepare sprites and HP before battleState so EDT repaint cannot paint battle UI with the
        // previous enemy's stance (game loop is not the Swing EDT).
        startBattle();
        gameState = battleState;
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
        battleHoverZone = "";
        loadBattleUiArt();
        loadBattleAssets();
        enemyMaxHP        = enemyStats.getEnemyHP(pendingBattleEnemyColor);
        enemyCurrentHP    = enemyMaxHP;
        battleRound       = 1;
        playerMoveDisplay = "";
        enemyMoveDisplay  = "";
        battleResolved    = false;
        waitingForNext    = false;
        battleOutcomeArtImage = null;
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

    private BufferedImage readFirstExisting(String... paths) {
        for (String path : paths) {
            BufferedImage img = readOptionalImage(path);
            if (img != null) return img;
        }
        return null;
    }

    private void loadBattleUiArt() {
        String base = "/res/sprites/menu/battleButton/";
        battleItemIdle = readFirstExisting(base + "items_idle.png", base + "item_idle.png");
        battleItemHover = readFirstExisting(base + "items_hover.png", base + "item_hover.png");
        battleAbilityIdle = readFirstExisting(base + "abilities_idle.png", base + "ability_idle.png");
        battleAbilityHover = readFirstExisting(base + "abilities_hover.png", base + "ability_hovers.png", base + "ability_hover.png");
        battleRockIdle = readFirstExisting(base + "rock_idle.png");
        battleRockHover = readFirstExisting(base + "rock_hover.png");
        battlePaperIdle = readFirstExisting(base + "paper_idle.png");
        battlePaperHover = readFirstExisting(base + "paper_hover.png");
        battleScissorsIdle = readFirstExisting(base + "scissors_idle.png");
        battleScissorsHover = readFirstExisting(base + "scissors_hover.png");
        battleContinueIdle = readFirstExisting(base + "continue_idle.png");
        battleContinueHover = readFirstExisting(base + "continue_hover.png");
        if (battleContinueIdle == null) battleContinueIdle = continueIdleImage;
        if (battleContinueHover == null) battleContinueHover = continueHoverImage;

        outcomeHitboxImage = readOptionalImage("/res/sprites/menu/menuHitbox/outcome_hitbox.png");
        outcomeSceneImage = readOptionalImage("/res/sprites/menu/battleScene/gle_outcome.png");
    }

    private String getEnemySpriteKey(int color) {
        if (color == COLOR_JAMES) return "james";
        if (color == COLOR_ALIEYANDREW) return "alieyandrew";
        if (color == COLOR_KYLE) return "kyle";
        if (color == COLOR_JOHNRU) return "johnru";
        if (color == COLOR_ADRIAN) return "adrian";
        return "";
    }

    private void loadBattleAssets() {
        battlePlayerImage = null;
        battleEnemyImage = null;
        battlePlayerOpaqueBounds = null;
        battleEnemyOpaqueBounds = null;

        String mapBattlePath = "/res/sprites/menu/battleScene/" + currentMapName + "_battle.png";
        battleSceneImage = readOptionalImage(mapBattlePath);
        if (battleSceneImage == null) {
            battleSceneImage = readOptionalImage("/res/sprites/menu/battleScene/" + currentMapName + "_outcome.png");
        }

        String playerKey = CharacterStats.CharacterType.fromName(player.characterName).name().toLowerCase();
        battlePlayerImage = readFirstExisting(
                "/res/sprites/player/" + playerKey + "/" + playerKey + "_battle.png",
                "/res/sprites/player/" + player.characterName + "/" + player.characterName + "_battle.png");

        String enemyKey = getEnemySpriteKey(pendingBattleEnemyColor);
        if (!enemyKey.isEmpty()) {
            battleEnemyImage = readOptionalImage("/res/sprites/enemies/" + enemyKey + "/" + enemyKey + "_battle.png");
        } else {
            battleEnemyImage = null;
        }

        battlePlayerOpaqueBounds = battlePlayerImage != null ? getNonTransparentBounds(battlePlayerImage) : null;
        battleEnemyOpaqueBounds = battleEnemyImage != null ? getNonTransparentBounds(battleEnemyImage) : null;
    }

    private static String battleMoveFileTag(BattleSystem.Move move) {
        return switch (move) {
            case ROCK -> "rock";
            case PAPER -> "paper";
            case SCISSORS -> "scissors";
        };
    }

    /**
     * PNGs in {@code /res/sprites/menu/battleOutcomes/}: primary {@code <your_move>_<enemy_move>.png}
     * (e.g. {@code paper_paper.png}, {@code rock_scissors.png}). If missing and the battle just ended,
     * falls back to character win/lose filenames ({@code win_<enemy>.png}, {@code lose_<player>.png}, …).
     */
    private void loadBattleOutcomeArt(BattleSystem.Move playerMove, BattleSystem.Move enemyMove) {
        battleOutcomeArtImage = null;
        String base = "/res/sprites/menu/battleOutcomes/";
        battleOutcomeArtImage = readOptionalImage(
                base + battleMoveFileTag(playerMove) + "_" + battleMoveFileTag(enemyMove) + ".png");
        if (!battleResolved || battleOutcomeArtImage != null) {
            return;
        }

        String playerKey = CharacterStats.CharacterType.fromName(player.characterName).name().toLowerCase();
        String enemyKey = getEnemySpriteKey(pendingBattleEnemyColor);
        boolean playerWon = enemyCurrentHP <= 0;

        if (playerWon) {
            if (!enemyKey.isEmpty()) {
                battleOutcomeArtImage = readFirstExisting(
                        base + "win_" + enemyKey + ".png",
                        base + enemyKey + "_defeat.png",
                        base + enemyKey + "_lose.png");
            }
            if (battleOutcomeArtImage == null) {
                battleOutcomeArtImage = readFirstExisting(
                        base + playerKey + "_win.png",
                        base + "win_" + playerKey + ".png",
                        base + "player_win.png");
            }
        } else {
            battleOutcomeArtImage = readFirstExisting(
                    base + "lose_" + playerKey + ".png",
                    base + playerKey + "_lose.png",
                    base + playerKey + "_defeat.png");
            if (battleOutcomeArtImage == null && !enemyKey.isEmpty()) {
                battleOutcomeArtImage = readFirstExisting(
                        base + "win_" + enemyKey + ".png",
                        base + enemyKey + "_win.png");
            }
            if (battleOutcomeArtImage == null) {
                battleOutcomeArtImage = readOptionalImage(base + "player_lose.png");
            }
        }
    }

    /** Fallback when {@code battle_hitbox.png} uses one flat color per button (no distinct RGB ids). */
    private Rectangle[] detectBattleButtonBlobsImageSpace() {
        if (battleHitboxImage == null) return null;

        int w = battleHitboxImage.getWidth();
        int h = battleHitboxImage.getHeight();
        boolean[][] visited = new boolean[h][w];
        java.util.ArrayList<Rectangle> parts = new java.util.ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (visited[y][x] || !isBattleHitboxBlobPixel(x, y)) continue;

                int minX = x, maxX = x, minY = y, maxY = y;
                java.util.ArrayDeque<Point> queue = new java.util.ArrayDeque<>();
                queue.add(new Point(x, y));
                visited[y][x] = true;

                while (!queue.isEmpty()) {
                    Point p = queue.removeFirst();
                    int px = p.x;
                    int py = p.y;

                    if (px < minX) minX = px;
                    if (px > maxX) maxX = px;
                    if (py < minY) minY = py;
                    if (py > maxY) maxY = py;

                    int[][] dirs = {{1,0}, {-1,0}, {0,1}, {0,-1}};
                    for (int[] d : dirs) {
                        int nx = px + d[0];
                        int ny = py + d[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (visited[ny][nx] || !isBattleHitboxBlobPixel(nx, ny)) continue;
                        visited[ny][nx] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
                parts.add(new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1));
            }
        }

        if (parts.size() < 5) return null;

        parts.sort((a, b) -> {
            int rowA = a.y / 20;
            int rowB = b.y / 20;
            if (rowA != rowB) return Integer.compare(rowA, rowB);
            return Integer.compare(a.x, b.x);
        });

        if (parts.size() > 5) {
            parts = new java.util.ArrayList<>(parts.subList(0, 5));
        }
        return parts.toArray(new Rectangle[0]);
    }

    private boolean isBattleHitboxBlobPixel(int x, int y) {
        int argb = battleHitboxImage.getRGB(x, y);
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha == 0) return false;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return !(r > 245 && g > 245 && b > 245);
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
        loadBattleOutcomeArt(playerMove, enemyMove);
    }

    private void nextRound() {
        if (battleResolved) {
            if (player.currentHP <= 0) {
                player.respawnWithPenalty();
            }
            battleHoverZone = "";
            gameState         = playState;
            currentDialog     = "";
            dialogStage       = 0;
            lastNPCColor      = 0;
            battleResolved    = false;
            waitingForNext    = false;
            battleOutcomeArtImage = null;
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
        drawMenuHoverOutline(g2, menuStartHitboxImage);
    }

    private void drawStartMenuScreen(Graphics2D g2) {
        drawMenuBase(g2);
        drawMenuButton(g2, continueMenuBtn, continueIdleImage, continueHoverImage, "continue");
        drawMenuButton(g2, selectCharacterBtn, selectCharacterIdleImage, selectCharacterHoverImage, "selectCharacter");
        drawMenuIconButton(g2, muteBtn, muteIdleImage, muteHoverImage, "mute");
        drawMenuIconButton(g2, settingsBtn, settingsIdleImage, settingsHoverImage, "settings");
        drawMenuHoverOutline(g2, menuGuiHitboxImage);
    }

    private void drawCharacterSelectScreen(Graphics2D g2) {
        drawMenuBase(g2);
        drawCharacterPreview(g2);
        drawCharacterButton(g2, ivanBtn, 0);
        drawCharacterButton(g2, samBtn, 2);
        drawCharacterButton(g2, nimuelBtn, 1);
        drawCharacterButton(g2, johnfielBtn, 3);
        drawMenuIconButton(g2, muteBtn, muteIdleImage, muteHoverImage, "mute");
        drawMenuIconButton(g2, settingsBtn, settingsIdleImage, settingsHoverImage, "settings");
    }

    private void drawCharacterPreview(Graphics2D g2) {
        if (hoveredCharIndex < 0 || characterSelectImages == null) return;
        if (hoveredCharIndex >= characterSelectImages.length) return;

        BufferedImage image = characterSelectImages[hoveredCharIndex];
        Rectangle sourceBounds = getNonTransparentBounds(image);
        Rectangle screenBounds = getColorBounds(menuCharacterSelectHitboxImage, COLOR_CHAR_PREVIEW);

        if (image == null || sourceBounds == null || screenBounds == null) return;

        // Fit character art into the preview box while preserving aspect ratio.
        double boxScale = 0.90;
        int availableWidth = Math.max(1, (int) Math.round(screenBounds.width * boxScale));
        int availableHeight = Math.max(1, (int) Math.round(screenBounds.height * boxScale));
        double fitScale = Math.min((double) availableWidth / sourceBounds.width, (double) availableHeight / sourceBounds.height);
        int targetWidth = Math.max(1, (int) Math.round(sourceBounds.width * fitScale));
        int targetHeight = Math.max(1, (int) Math.round(sourceBounds.height * fitScale));
        int targetX = screenBounds.x + (screenBounds.width - targetWidth) / 2;
        int targetY = screenBounds.y + (screenBounds.height - targetHeight) / 2;

        g2.drawImage(
                image,
                targetX,
                targetY,
                targetX + targetWidth,
                targetY + targetHeight,
                sourceBounds.x,
                sourceBounds.y,
                sourceBounds.x + sourceBounds.width,
                sourceBounds.y + sourceBounds.height,
                null
        );
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
        int characterColor = getCharacterColorByIndex(index);
        Rectangle screenBounds = getColorBounds(menuCharacterSelectHitboxImage, characterColor);

        if (screenBounds != null) {
            g2.drawImage(image, screenBounds.x, screenBounds.y, screenBounds.width, screenBounds.height, null);
            return;
        }

        // Fallback to fixed base rectangles if the hitbox color is missing.
        drawBaseImage(g2, image, bounds);
    }

    private int getCharacterColorByIndex(int index) {
        return switch (index) {
            case 0 -> COLOR_CHAR_IVAN;
            case 1 -> COLOR_CHAR_NIMUEL;
            case 2 -> COLOR_CHAR_SAM;
            case 3 -> COLOR_CHAR_JOHNFIEL;
            default -> 0;
        };
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
        if (gameState == characterSelectState) return;
        if (hoveredMenuColor == COLOR_MENU_LOGO) return;
        if ((menuMuteColor != 0 && hoveredMenuColor == menuMuteColor) ||
                (menuSettingsColor != 0 && hoveredMenuColor == menuSettingsColor)) return;
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
        drawHPBar(g2, x, y, w, h, current, max, label, Color.WHITE);
    }

    private void drawHPBar(Graphics2D g2, int x, int y, int w, int h, int current, int max, String label, Color labelColor) {
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

        g2.setColor(labelColor);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString(label + "  " + current + " / " + max, x, y + 14);
    }

    public int getScaledTileSize() {
        if (getHeight() <= 0) {
            return tileSize;
        }

        int baseScaledTileSize = Math.max(1, tileSize * getHeight() / 1080);
        return Math.max(1, (int) Math.round(baseScaledTileSize * CHARACTER_SCALE_BOOST));
    }

    public int scaleUniform(int value) {
        return Math.max(1, value * getScaledTileSize() / tileSize);
    }

    private void drawMapNameGUI(Graphics2D g2) {
        if (mapTitleImage != null) {
            int maxWidth = Math.max(1, getWidth() / 4);
            int maxHeight = Math.max(1, getHeight() / 8);
            int imageWidth = mapTitleImage.getWidth();
            int imageHeight = mapTitleImage.getHeight();
            double scale = Math.min((double) maxWidth / imageWidth, (double) maxHeight / imageHeight);
            int drawWidth = Math.max(1, (int) Math.round(imageWidth * scale));
            int drawHeight = Math.max(1, (int) Math.round(imageHeight * scale));
            int bx = (getWidth() - drawWidth) / 2;
            int by = scaleUniform(8);
            g2.drawImage(mapTitleImage, bx, by, drawWidth, drawHeight, null);
            return;
        }

        String label = currentMapName.toUpperCase();
        int fontSize = scaleUniform(24);
        g2.setFont(new Font("Arial", Font.BOLD, fontSize));
        int textWidth = g2.getFontMetrics().stringWidth(label);
        int padding = scaleUniform(12);
        int boxWidth = textWidth + padding * 2;
        int boxHeight = fontSize + padding;
        int bx = (getWidth() - boxWidth) / 2;
        int by = padding;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(bx, by, boxWidth, boxHeight, scaleUniform(12), scaleUniform(12));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(Math.max(1, scaleUniform(2))));
        g2.drawRoundRect(bx, by, boxWidth, boxHeight, scaleUniform(12), scaleUniform(12));
        g2.drawString(label, bx + padding, by + fontSize);
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
    }

    /** Background {@code gle_outcome.png}, outcome art in {@code COLOR_OUTCOME_ART_PANEL} region, continue on magenta. */
    private void drawBattleOutcomeScreen(Graphics2D g2) {
        if (outcomeSceneImage != null) {
            g2.drawImage(outcomeSceneImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2.setColor(new Color(15, 15, 35));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        Rectangle panel = outcomeHitboxImage != null
                ? getColorBounds(outcomeHitboxImage, COLOR_OUTCOME_ART_PANEL)
                : null;

        if (battleOutcomeArtImage != null) {
            if (panel != null && panel.width > 0 && panel.height > 0) {
                drawImageFitInRect(g2, battleOutcomeArtImage, panel, false, null);
            } else {
                int margin = scaleUniform(24);
                int reserveBottom = Math.max(scaleUniform(120), continueBtn != null ? continueBtn.height + margin * 3 : scaleUniform(96));
                Rectangle fallback = new Rectangle(
                        margin,
                        margin,
                        Math.max(1, getWidth() - 2 * margin),
                        Math.max(1, getHeight() - reserveBottom));
                drawImageFitInRect(g2, battleOutcomeArtImage, fallback, false, null);
            }
        }

        boolean contHover = "continue".equals(battleHoverZone);
        BufferedImage ci = contHover && battleContinueHover != null ? battleContinueHover : battleContinueIdle;
        if (ci != null && continueBtn != null && continueBtn.width > 0) {
            drawImageFitInRect(g2, ci, continueBtn, false, null);
        } else if (continueBtn != null && continueBtn.width > 0) {
            String label = battleResolved ? "Back to Game" : "Continue";
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

        if (!battleMessage.isEmpty()) {
            int midX = getWidth() / 2;
            g2.setFont(new Font("Arial", Font.BOLD, Math.min(28, scaleUniform(24))));
            FontMetrics fm = g2.getFontMetrics();
            int lineGap = scaleUniform(8);
            int padBelowBtn = scaleUniform(16);
            String[] lines = battleMessage.split("\n");

            int msgStartY;
            if (continueBtn != null && continueBtn.width > 0 && continueBtn.height > 0) {
                msgStartY = continueBtn.y + continueBtn.height + padBelowBtn + fm.getAscent();
            } else {
                msgStartY = panel != null && panel.height > 0
                        ? panel.y + panel.height + scaleUniform(28)
                        : getHeight() / 2 + scaleUniform(40);
            }

            int msgY = msgStartY;
            for (String line : lines) {
                int lw = fm.stringWidth(line);
                g2.setColor(new Color(255, 230, 100));
                g2.drawString(line, midX - lw / 2, msgY);
                msgY += fm.getHeight() + lineGap;
            }
        }
    }

    private void drawBattleState(Graphics2D g2) {
        updateBattleButtons();

        if (waitingForNext && outcomeHitboxImage != null) {
            drawBattleOutcomeScreen(g2);
            return;
        }

        if (battleSceneImage != null) {
            g2.drawImage(battleSceneImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            g2.setColor(new Color(15, 15, 35));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        if (battleSpriteHitboxImage != null) {
            // getColorBounds() returns panel-space rects (same as battle_hitbox.png handling).
            Rectangle playerSlot = getColorBounds(battleSpriteHitboxImage, COLOR_BATTLE_PLAYER_SLOT);
            Rectangle enemySlot = getColorBounds(battleSpriteHitboxImage, COLOR_BATTLE_ENEMY_SLOT);
            if (playerSlot != null && playerSlot.width > 0) {
                drawBattleSpriteStretched(g2, battlePlayerImage, playerSlot, false, battlePlayerOpaqueBounds);
            }
            if (enemySlot != null && enemySlot.width > 0) {
                drawBattleSpriteStretched(g2, battleEnemyImage, enemySlot, false, battleEnemyOpaqueBounds);
            }
        } else {
            int battleSpriteY = getHeight() / 2 - 30;
            drawBattleCharacter(g2, battlePlayerImage, getWidth() / 4, battleSpriteY, false, battlePlayerOpaqueBounds);
            drawBattleCharacter(g2, battleEnemyImage, getWidth() * 3 / 4, battleSpriteY, false, battleEnemyOpaqueBounds);
        }

        int midX = getWidth() / 2;

        int hpY = Math.max(8, scaleUniform(8));
        int barW = Math.min(400, (getWidth() - 96) / 2);
        int leftX = 24;
        int rightX = getWidth() - 24 - barW;
        drawHPBar(g2, leftX, hpY, barW, 22, player.currentHP, player.maxHP, player.characterName, Color.WHITE);
        drawHPBar(g2, rightX, hpY, barW, 22, enemyCurrentHP, enemyMaxHP, enemyName, new Color(255, 120, 120));

        int titleY = hpY + 52;
        g2.setColor(new Color(220, 60, 60));
        g2.setFont(new Font("Arial", Font.BOLD, 34));
        String battleTitle = "BATTLE - Round " + battleRound;
        int tw = g2.getFontMetrics().stringWidth(battleTitle);
        g2.drawString(battleTitle, midX - tw / 2, titleY);

        g2.setColor(new Color(255, 200, 50));
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics vsFm = g2.getFontMetrics();
        g2.drawString("VS", midX - vsFm.stringWidth("VS") / 2, titleY + 44);

        int eHPx = rightX;

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
            drawBattleWoodButton(g2, itemsBattleBtn, battleItemIdle, battleItemHover, "Items", "items".equals(battleHoverZone));
            drawBattleWoodButton(g2, abilitiesBattleBtn, battleAbilityIdle, battleAbilityHover, "Abilities", "abilities".equals(battleHoverZone));
            drawStretchedPickArt(g2, rockBtn, battleRockIdle, battleRockHover, "Rock", "rock".equals(battleHoverZone));
            drawStretchedPickArt(g2, paperBtn, battlePaperIdle, battlePaperHover, "Paper", "paper".equals(battleHoverZone));
            drawStretchedPickArt(g2, scissorsBtn, battleScissorsIdle, battleScissorsHover, "Scissors", "scissors".equals(battleHoverZone));
        } else {
            boolean contHover = "continue".equals(battleHoverZone);
            BufferedImage ci = contHover && battleContinueHover != null ? battleContinueHover : battleContinueIdle;
            if (ci != null && continueBtn.width > 0) {
                drawImageFitInRect(g2, ci, continueBtn, false, null);
            } else if (continueBtn.width > 0) {
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
    }

    /**
     * Draws {@code img} inside {@code dest} preserving aspect ratio (letterboxed), centered.
     * Optional {@code srcCrop} limits the source to a sub-rectangle (e.g. non-transparent bounds).
     */
    private void drawImageFitInRect(Graphics2D g2, BufferedImage img, Rectangle dest, boolean flipHorizontal,
                                    Rectangle srcCrop) {
        if (img == null || dest == null || dest.width <= 0 || dest.height <= 0) return;

        int sx0 = 0;
        int sy0 = 0;
        int sw = img.getWidth();
        int sh = img.getHeight();
        if (srcCrop != null && srcCrop.width > 0 && srcCrop.height > 0) {
            sx0 = srcCrop.x;
            sy0 = srcCrop.y;
            sw = srcCrop.width;
            sh = srcCrop.height;
        }
        if (sw <= 0 || sh <= 0) return;

        double scale = Math.min((double) dest.width / sw, (double) dest.height / sh);
        int dw = Math.max(1, (int) Math.round(sw * scale));
        int dh = Math.max(1, (int) Math.round(sh * scale));
        int dx = dest.x + (dest.width - dw) / 2;
        int dy = dest.y + (dest.height - dh) / 2;

        Object prevInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        try {
            if (!flipHorizontal) {
                g2.drawImage(img, dx, dy, dx + dw, dy + dh, sx0, sy0, sx0 + sw, sy0 + sh, null);
            } else {
                AffineTransform old = g2.getTransform();
                g2.translate(dx + dw, dy);
                g2.scale(-1, 1);
                g2.drawImage(img, 0, 0, dw, dh, sx0, sy0, sx0 + sw, sy0 + sh, null);
                g2.setTransform(old);
            }
        } finally {
            // getRenderingHint can return null; setRenderingHint(..., null) throws IllegalArgumentException.
            if (prevInterp != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevInterp);
            } else {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
        }
    }

    /** Optional PNG from {@code battleButton/}; otherwise draws the wood-style label on the battle scene. */
    private void drawBattleWoodButton(Graphics2D g2, Rectangle rect, BufferedImage idle, BufferedImage hover,
                                      String label, boolean hovered) {
        if (rect == null || rect.width <= 0) return;
        BufferedImage img = hovered && hover != null ? hover : idle;
        if (img != null) {
            drawImageFitInRect(g2, img, rect, false, null);
        } else {
            drawMoveButton(g2, rect, label, BATTLE_BTN_WOOD_FILL, BATTLE_BTN_WOOD_BORDER);
        }
    }

    private void drawStretchedPickArt(Graphics2D g2, Rectangle rect, BufferedImage idle, BufferedImage hover,
                                     String fallbackLabel, boolean hovered) {
        if (rect == null || rect.width <= 0) return;
        BufferedImage img = hovered && hover != null ? hover : idle;
        if (img != null) {
            drawImageFitInRect(g2, img, rect, false, null);
        } else {
            drawMoveButton(g2, rect, fallbackLabel, BATTLE_BTN_WOOD_FILL, BATTLE_BTN_WOOD_BORDER);
        }
    }

    private void drawMoveButton(Graphics2D g2, Rectangle btn, String label, Color fill, Color border) {
        g2.setColor(fill);
        g2.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 14, 14);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(btn.x, btn.y, btn.width, btn.height, 14, 14);
        g2.setColor(new Color(60, 42, 22));

        int fontSize = Math.min(26, Math.max(14, btn.height / 2));
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        while (fontSize > 12 && fm.stringWidth(label) > btn.width - 10) {
            fontSize--;
            font = new Font("Arial", Font.BOLD, fontSize);
            g2.setFont(font);
            fm = g2.getFontMetrics();
        }
        int lw = fm.stringWidth(label);
        int textY = btn.y + (btn.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(label, btn.x + btn.width / 2 - lw / 2, textY);
    }

    private void drawBattleSpriteStretched(Graphics2D g2, BufferedImage sprite, Rectangle slot, boolean flipHorizontal,
                                          Rectangle cachedOpaqueBounds) {
        if (sprite == null || slot == null || slot.width <= 0 || slot.height <= 0) return;
        Rectangle crop = cachedOpaqueBounds != null ? cachedOpaqueBounds : getNonTransparentBounds(sprite);
        drawImageFitInRect(g2, sprite, slot, flipHorizontal, crop);
    }

    private void drawBattleCharacter(Graphics2D g2, BufferedImage sprite, int anchorX, int baselineY, boolean flip,
                                    Rectangle cachedOpaqueBounds) {
        if (sprite == null) return;

        Rectangle spriteBounds = cachedOpaqueBounds != null ? cachedOpaqueBounds : getNonTransparentBounds(sprite);
        if (spriteBounds == null) return;

        int targetH = Math.max(scaleUniform(190), getHeight() / 3);
        int targetW = Math.max(1, (int) (targetH * ((double) spriteBounds.width / Math.max(1, spriteBounds.height))));
        int drawX = anchorX - targetW / 2;
        int drawY = baselineY - targetH;

        if (!flip) {
            g2.drawImage(
                    sprite,
                    drawX, drawY, drawX + targetW, drawY + targetH,
                    spriteBounds.x, spriteBounds.y, spriteBounds.x + spriteBounds.width, spriteBounds.y + spriteBounds.height,
                    null
            );
        } else {
            g2.drawImage(
                    sprite,
                    drawX + targetW, drawY, drawX, drawY + targetH,
                    spriteBounds.x, spriteBounds.y, spriteBounds.x + spriteBounds.width, spriteBounds.y + spriteBounds.height,
                    null
            );
        }
    }
}