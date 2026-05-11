package main;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;

public class ImageDisplay {

    private BufferedImage mapImage;
    private BufferedImage hitboxImage;

    private BufferedImage menuMainHitbox;
    private BufferedImage menuStartHitbox;
    private BufferedImage menuCharHitbox;
    private BufferedImage battleHitbox;
    private BufferedImage battleSpriteHitbox;
    private BufferedImage worldGuiHitbox;
    private BufferedImage outcomeHitbox;

    private BufferedImage menuScreenImg;
    private BufferedImage logoImg;
    private BufferedImage mapTitleImg;
    private BufferedImage battleSceneImg;
    private BufferedImage outcomeSceneImg;
    private BufferedImage outcomeRPSImg;
    private BufferedImage creditsImg;
    private BufferedImage winImg;

    private final Map<String, BufferedImage[]> btnImgs = new HashMap<>();
    private final Map<String, BufferedImage> npcStand = new HashMap<>();
    private final Map<String, BufferedImage> charSelectImg = new HashMap<>();
    private final Map<String, BufferedImage> npcDialog = new HashMap<>();

    private BufferedImage playerBattleImg;
    private BufferedImage playerDialogImg;
    private BufferedImage enemyBattleImg;
    private BufferedImage enemyDialogImg;

    public void loadAll() {
        menuScreenImg = img("/res/gui/pixelart/menu/menu_screen.png");
        logoImg = img("/res/gui/pixelart/menu/fixsix_log.png");
        creditsImg = firstImg("/res/gui/pixelart/menu/credits_placeholder.png",
                "/res/sprites/menu/gui/credits_placeholder.png");
        winImg = firstImg("/res/gui/pixelart/menu/win_placeholder.png",
                "/res/sprites/menu/gui/win_placeholder.png");

        menuMainHitbox = img("/res/gui/button_hitbox/menu_main_hitbox.png");
        menuStartHitbox = img("/res/gui/button_hitbox/menu_start_hitbox.png");
        menuCharHitbox = img("/res/gui/button_hitbox/menu_characterselect_hitbox.png");
        battleHitbox = img("/res/gui/button_hitbox/battle_hitbox.png");
        battleSpriteHitbox = img("/res/gui/button_hitbox/battle_sprite_hitbox.png");
        worldGuiHitbox = img("/res/gui/button_hitbox/world_gui_hitbox.png");
        outcomeHitbox = img("/res/gui/button_hitbox/outcome_hitbox.png");

        btn("start", "/res/gui/buttons/menu/start_idle.png", "/res/gui/buttons/menu/start_hover.png");
        btn("credits", "/res/gui/buttons/menu/credits_idle.png", "/res/gui/buttons/menu/credits_hover.png");
        btn("quit", "/res/gui/buttons/menu/quit_idle.png", "/res/gui/buttons/menu/quit_hover.png");
        btn("settings", "/res/gui/buttons/menu/settings_idle.png", "/res/gui/buttons/menu/settings_hover.png");
        btn("mute", "/res/gui/buttons/menu/mute_idle.png", "/res/gui/buttons/menu/mute_hover.png");
        btn("muted", "/res/gui/buttons/menu/muted_idle.png", "/res/gui/buttons/menu/muted_hover.png");
        btn("save", "/res/gui/buttons/menu/save_idle.png", "/res/gui/buttons/menu/save_hover.png");
        btn("continue", "/res/gui/buttons/menu/continue_idle.png", "/res/gui/buttons/menu/continue_hover.png");
        btn("selchar", "/res/gui/buttons/menu/selectcharacter_idle.png", "/res/gui/buttons/menu/selectcharacter_hover.png");

        btn("ivan", "/res/gui/buttons/player/ivan_idle.png", "/res/gui/buttons/player/ivan_hover.png");
        btn("sam", "/res/gui/buttons/player/sam_idle.png", "/res/gui/buttons/player/sam_hover.png");
        btn("nimuel", "/res/gui/buttons/player/nimuel_idle.png", "/res/gui/buttons/player/nimuel_hover.png");
        btn("johnfiel", "/res/gui/buttons/player/johnfiel_idle.png", "/res/gui/buttons/player/johnfiel_hover.png");

        btn("item_inv", "/res/gui/buttons/world/item_idle.png", "/res/gui/buttons/world/item_hover.png");
        btn("abil_inv", "/res/gui/buttons/world/ability_idle.png", "/res/gui/buttons/world/ability_hover.png");
        btn("backmenu", "/res/gui/buttons/world/backtomenu_idle.png", "/res/gui/buttons/world/backtomenu_hover.png");
        btn("wsave", "/res/gui/buttons/world/save_idle.png", "/res/gui/buttons/world/save_hover.png");
        btn("wset", "/res/gui/buttons/world/settings_idle.png", "/res/gui/buttons/world/settings_hover.png");
        btn("wmute", "/res/gui/buttons/world/mute_idle.png", "/res/gui/buttons/world/mute_hover.png");
        btn("wmuted", "/res/gui/buttons/world/muted_idle.png", "/res/gui/buttons/world/muted_hover.png");

        btn("rock", "/res/gui/buttons/battle/rock_idle.png", "/res/gui/buttons/battle/rock_hover.png");
        btn("paper", "/res/gui/buttons/battle/paper_idle.png", "/res/gui/buttons/battle/paper_hover.png");
        btn("scissors", "/res/gui/buttons/battle/scissors_idle.png", "/res/gui/buttons/battle/scissors_hover.png");
        btn("contbat", "/res/gui/buttons/battle/continue_idle.png", "/res/gui/buttons/battle/continue_hover.png");
        btn("useitem", "/res/gui/buttons/battle/items_idle.png", "/res/gui/buttons/battle/items_hover.png");
        btn("useabil", "/res/gui/buttons/battle/abilities_idle.png", "/res/gui/buttons/battle/abilities_hover.png");

        charSelectImg.put("ivan", img("/res/sprites/player/ivan/ivan_selectcharacter.png"));
        charSelectImg.put("sam", img("/res/sprites/player/sam/sam_selectcharacter.png"));
        charSelectImg.put("nimuel", img("/res/sprites/player/nimuel/nimuel_selectcharacter.png"));
        charSelectImg.put("johnfiel", img("/res/sprites/player/johnfiel/johnfiel_selectcharacter.png"));

        String[] npcs = {"james", "alieyandrew", "kyle", "johnru", "adrian", "darryll", "gio", "yohann", "dirk", "jake", "vaughn"};
        for (String n : npcs) {
            npcStand.put(n, img("/res/sprites/enemies/" + n + "/" + n + "_stand.png"));
        }
    }

    public void loadMapImages(String mapName) {
        switch (mapName) {
            case GamePanel.GLE_MAP -> {
                mapImage = img("/res/gui/pixelart/map/gle.png");
                hitboxImage = img("/res/gui/pixelart/map/gle_hitbox.png");
                mapTitleImg = img("/res/gui/pixelart/map/gle_title.png");
            }
            case GamePanel.FRONTGATE_MAP -> {
                mapImage = img("/res/gui/pixelart/map/frontgate.png");
                hitboxImage = img("/res/gui/pixelart/map/frontgate_hitbox.png");
                mapTitleImg = img("/res/gui/pixelart/map/frontgate_title.png");
            }
            case GamePanel.EMALL_MAP -> {
                mapImage = null;
                hitboxImage = null;
                mapTitleImg = null;
            }
            default -> {
                mapImage = null;
                hitboxImage = null;
                mapTitleImg = null;
            }
        }
    }

    public void loadBattleImages(String mapName, int enemyColor, boolean isFinalBoss, String playerName) {
        String mapKey = switch (mapName) {
            case GamePanel.GLE_MAP -> "gle";
            case GamePanel.FRONTGATE_MAP -> "frontgate";
            default -> "emall";
        };

        battleSceneImg = img("/res/gui/pixelart/battle_scene/" + mapKey + "_battle.png");
        outcomeSceneImg = img("/res/gui/pixelart/battle_scene/" + mapKey + "_outcome.png");

        String folder = HitboxColors.enemyFolder(enemyColor);
        if (folder != null) {
            enemyBattleImg = img("/res/sprites/enemies/" + folder + "/" + folder + "_battle.png");
            enemyDialogImg = img("/res/sprites/enemies/" + folder + "/" + folder + "_dialog.png");
        } else if (isFinalBoss) {
            enemyBattleImg = img("/res/sprites/enemies/finalboss/final_boss.png");
            enemyDialogImg = firstImg("/res/sprites/enemies/finalboss/finalboss_dialog.png",
                    "/res/sprites/enemies/finalboss/final_boss_dialog.png",
                    "/res/sprites/enemies/finalboss/final_boss.png");
        } else {
            enemyBattleImg = null;
            enemyDialogImg = null;
        }

        if (playerName != null && !playerName.isBlank()) {
            playerBattleImg = img("/res/sprites/player/" + playerName + "/" + playerName + "_battle.png");
            playerDialogImg = img("/res/sprites/player/" + playerName + "/" + playerName + "_dialog.png");
        } else {
            playerBattleImg = null;
            playerDialogImg = null;
        }
    }

    public void loadPlayerImages(String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            playerBattleImg = img("/res/sprites/player/" + playerName + "/" + playerName + "_battle.png");
            playerDialogImg = img("/res/sprites/player/" + playerName + "/" + playerName + "_dialog.png");
        } else {
            playerBattleImg = null;
            playerDialogImg = null;
        }
    }

    public void loadOutcomeImage(String playerMove, String enemyMove) {
        outcomeRPSImg = img("/res/gui/pixelart/battle_outcome/" + playerMove + "_" + enemyMove + ".png");
    }

    public void loadEmallScenes() {
        battleSceneImg = img("/res/gui/pixelart/battle_scene/emall_battle.png");
        outcomeSceneImg = img("/res/gui/pixelart/battle_scene/emall_outcome.png");
    }

    public BufferedImage getEmallBattleBackground() {
        if (battleSceneImg == null) {
            battleSceneImg = img("/res/gui/pixelart/battle_scene/emall_battle.png");
        }
        return battleSceneImg;
    }

    public BufferedImage getNpcDialogImage(String folder) {
        if (folder == null) return null;
        return npcDialog.computeIfAbsent(folder,
                key -> img("/res/sprites/enemies/" + key + "/" + key + "_dialog.png"));
    }

    public BufferedImage getMapImage() { return mapImage; }
    public BufferedImage getHitboxImage() { return hitboxImage; }
    public BufferedImage getMenuMainHitbox() { return menuMainHitbox; }
    public BufferedImage getMenuStartHitbox() { return menuStartHitbox; }
    public BufferedImage getMenuCharHitbox() { return menuCharHitbox; }
    public BufferedImage getBattleHitbox() { return battleHitbox; }
    public BufferedImage getBattleSpriteHitbox() { return battleSpriteHitbox; }
    public BufferedImage getWorldGuiHitbox() { return worldGuiHitbox; }
    public BufferedImage getOutcomeHitbox() { return outcomeHitbox; }
    public BufferedImage getMenuScreenImg() { return menuScreenImg; }
    public BufferedImage getLogoImg() { return logoImg; }
    public BufferedImage getMapTitleImg() { return mapTitleImg; }
    public BufferedImage getBattleSceneImg() { return battleSceneImg; }
    public BufferedImage getOutcomeSceneImg() { return outcomeSceneImg; }
    public BufferedImage getOutcomeRPSImg() { return outcomeRPSImg; }
    public BufferedImage getCreditsImg() { return creditsImg; }
    public BufferedImage getWinImg() { return winImg; }
    public Map<String, BufferedImage[]> getButtonImages() { return btnImgs; }
    public Map<String, BufferedImage> getNpcStandImages() { return npcStand; }
    public Map<String, BufferedImage> getCharacterSelectImages() { return charSelectImg; }
    public BufferedImage getPlayerBattleImg() { return playerBattleImg; }
    public BufferedImage getPlayerDialogImg() { return playerDialogImg; }
    public BufferedImage getEnemyBattleImg() { return enemyBattleImg; }
    public BufferedImage getEnemyDialogImg() { return enemyDialogImg; }

    private BufferedImage img(String path) {
        try {
            return ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path)));
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage firstImg(String... paths) {
        for (String path : paths) {
            BufferedImage im = img(path);
            if (im != null) return im;
        }
        return null;
    }

    private void btn(String key, String idle, String hover) {
        btnImgs.put(key, new BufferedImage[]{img(idle), img(hover)});
    }
}
