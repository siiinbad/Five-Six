package entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.imageio.ImageIO;
import main.GamePanel;
import main.KeyHandler;

public class Player extends Entity {
    public GamePanel gp;
    KeyHandler keyH;
    public String characterName;
    boolean nearInteractable = false;
    boolean eWasPressed = false;

    public CharacterStats.CharacterType charType;
    public double damageMultiplier;
    private static final int BASE_SPEED = 4;

    public int spawnX, spawnY;

    public Player(GamePanel gp, KeyHandler keyH, String characterName) {
        this.gp = gp;
        this.keyH = keyH;
        this.characterName = characterName;

        charType = CharacterStats.CharacterType.fromName(characterName);
        damageMultiplier = charType.damageMultiplier;

        setHP(charType.maxHP);

        speed = BASE_SPEED;
        direction = "down";
        getPlayerImage();
    }

    public void saveSpawn(int x, int y) {
        this.spawnX = x;
        this.spawnY = y;
    }

    public void respawnWithPenalty() {
        x = spawnX;
        y = spawnY;
        setHPPercent(0.30);
    }

    public void getPlayerImage() {
        try {
            String path = "/res/sprites/player/" + characterName + "/" + characterName + "_";
            up1        = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "back_walk1.png")));
            up2        = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "back_walk2.png")));
            down1      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "front_walk1.png")));
            down2      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "front_walk2.png")));
            left1      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "left_walk1.png")));
            left2      = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "left_walk2.png")));
            right1     = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "right_walk1.png")));
            right2     = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "right_walk2.png")));
            standUp    = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "back_stand.png")));
            standDown  = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "front_stand.png")));
            standLeft  = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "left_stand.png")));
            standRight = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "right_stand.png")));
        } catch (Exception e) {
            System.out.println("Images failed to load.");
        }
    }

    public void update() {
        if (gp.gameState == gp.battleState || gp.gameState == gp.fadeState) return;

        int moveSpeed = getScaledSpeed();
        int nextX = x, nextY = y;
        boolean moving = false;

        if (keyH.upPressed)    { direction = "up";    nextY -= moveSpeed; moving = true; }
        else if (keyH.downPressed)  { direction = "down";  nextY += moveSpeed; moving = true; }
        else if (keyH.leftPressed)  { direction = "left";  nextX -= moveSpeed; moving = true; }
        else if (keyH.rightPressed) { direction = "right"; nextX += moveSpeed; moving = true; }

        if (moving && !isColliding(nextX, nextY)) {
            x = nextX; y = nextY;
            if (++spriteCounter > 12) { spriteNum = (spriteNum == 1) ? 2 : 1; spriteCounter = 0; }
        }
        checkInteractions();
    }

    private boolean isColliding(int nx, int ny) {
        // No collision on the placeholder walkway map
        if (GamePanel.WALKWAY_MAP.equals(gp.currentMapName)) {
            return false;
        }

        if (gp.hitboxImage == null) return false;

        Rectangle playerBox = getPlayerCollisionBox(nx, ny);

        if (areaContainsColor(playerBox, gp.COLOR_WALL)) {
            return true;
        }

        for (int enemyColor : getEnemyColors()) {
            if (!gp.enemyStats.isDefeated(enemyColor) && areaContainsColor(playerBox, enemyColor)) {
                return true;
            }
        }

        // Doors (orange) and next area (red) stay walkable on purpose.
        return false;
    }




    private void checkInteractions() {
        nearInteractable = false;
        if (gp.hitboxImage == null) return;

        Rectangle playerBox = getPlayerCollisionBox(x, y);
        Rectangle interactBox = getInteractionBox(playerBox);

        int color = findInteractableColor(interactBox);
        if (color == 0) {
            color = findInteractableColor(playerBox);
        }

        boolean isInteractable = (color == gp.COLOR_JAMES || color == gp.COLOR_ALIEYANDREW ||
                color == gp.COLOR_KYLE  || color == gp.COLOR_JOHNRU ||
                color == gp.COLOR_ADRIAN || color == gp.COLOR_DOOR ||
                color == gp.COLORNEXTAREA);

        if (color != gp.COLORNEXTAREA && gp.enemyStats.isDefeated(color)) {
            gp.currentDialog = "";
            return;
        }

        if (isInteractable) {
            nearInteractable = true;

            if (gp.lastNPCColor != color) {
                gp.dialogStage = 0;
                gp.lastNPCColor = color;
            }

            if (keyH.ePressed && !eWasPressed) {
                eWasPressed = true;

                if (color == gp.COLORNEXTAREA) {
                    if (gp.enemyStats.isDefeated(gp.COLOR_JOHNRU)) {
                        gp.loadMap(GamePanel.WALKWAY_MAP);
                    } else {
                        gp.currentDialog = "You must defeat Johnru first.";
                    }
                } else if (color == gp.COLOR_JOHNRU && !allOtherEnemiesDefeated()) {
                    gp.currentDialog = "Johnru: come back to me when you have beaten the others";
                } else {
                    gp.dialogStage++;
                    updateDialogue(color);

                    if (isFinalDialogStage(color)) {
                        gp.pendingBattleEnemyColor = color;
                        gp.startFadeToBlack();
                    }
                }
            }
        } else {
            if (!gp.currentDialog.isEmpty()) {
                gp.currentDialog = "";
                gp.dialogStage = 0;
                gp.lastNPCColor = 0;
            }
        }

        if (!keyH.ePressed) eWasPressed = false;
    }

    private Rectangle getPlayerCollisionBox(int px, int py) {
        int spriteSize = gp.getScaledTileSize();
        int offsetX = gp.scaleUniform(45);
        int offsetY = gp.scaleUniform(40);
        int width = gp.scaleUniform(70);
        int height = Math.max(1, spriteSize - gp.scaleUniform(60));
        return new Rectangle(px + offsetX, py + offsetY, width, height);
    }

    private Rectangle getInteractionBox(Rectangle playerBox) {
        Rectangle interactionBox = new Rectangle(playerBox);
        int interactDistance = gp.getScaledTileSize() / 3;

        switch (direction) {
            case "up" -> interactionBox.translate(0, -interactDistance);
            case "down" -> interactionBox.translate(0, interactDistance);
            case "left" -> interactionBox.translate(-interactDistance, 0);
            case "right" -> interactionBox.translate(interactDistance, 0);
        }

        return interactionBox;
    }

    private int getScaledSpeed() {
        return Math.max(1, gp.scaleUniform(BASE_SPEED));
    }

    private boolean areaContainsColor(Rectangle screenRect, int targetColor) {
        Rectangle imageRect = toImageRect(screenRect);
        if (imageRect == null) return false;

        for (int iy = imageRect.y; iy < imageRect.y + imageRect.height; iy++) {
            for (int ix = imageRect.x; ix < imageRect.x + imageRect.width; ix++) {
                int color = gp.hitboxImage.getRGB(ix, iy) & 0xFFFFFF;
                if (color == targetColor) {
                    return true;
                }
            }
        }

        return false;
    }

    private int findInteractableColor(Rectangle screenRect) {
        Rectangle imageRect = toImageRect(screenRect);
        if (imageRect == null) return 0;

        boolean foundDoor = false;
        boolean foundNextArea = false;

        for (int iy = imageRect.y; iy < imageRect.y + imageRect.height; iy++) {
            for (int ix = imageRect.x; ix < imageRect.x + imageRect.width; ix++) {
                int color = gp.hitboxImage.getRGB(ix, iy) & 0xFFFFFF;

                if (color == gp.COLOR_DOOR) {
                    foundDoor = true;
                    continue;
                }

                if (color == gp.COLORNEXTAREA) {
                    foundNextArea = true;
                    continue;
                }

                for (int enemyColor : getEnemyColors()) {
                    if (color == enemyColor && !gp.enemyStats.isDefeated(enemyColor)) {
                        return enemyColor;
                    }
                }
            }
        }

        if (foundDoor) return gp.COLOR_DOOR;
        if (foundNextArea) return gp.COLORNEXTAREA;
        return 0;
    }

    private Rectangle toImageRect(Rectangle screenRect) {
        if (gp.hitboxImage == null || gp.getWidth() <= 0 || gp.getHeight() <= 0) {
            return null;
        }

        int imageWidth = gp.hitboxImage.getWidth();
        int imageHeight = gp.hitboxImage.getHeight();

        int left = Math.max(0, screenRect.x * imageWidth / gp.getWidth());
        int top = Math.max(0, screenRect.y * imageHeight / gp.getHeight());
        int right = Math.min(imageWidth - 1, ((screenRect.x + screenRect.width - 1) * imageWidth) / gp.getWidth());
        int bottom = Math.min(imageHeight - 1, ((screenRect.y + screenRect.height - 1) * imageHeight) / gp.getHeight());

        if (left > right || top > bottom) {
            return null;
        }

        return new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    private int[] getEnemyColors() {
        return new int[] {
                gp.COLOR_JAMES,
                gp.COLOR_ALIEYANDREW,
                gp.COLOR_KYLE,
                gp.COLOR_JOHNRU,
                gp.COLOR_ADRIAN
        };
    }


    private boolean isFinalDialogStage(int color) {
        if (color == gp.COLOR_JAMES)        return gp.dialogStage >= 2;
        if (color == gp.COLOR_ALIEYANDREW)  return gp.dialogStage >= 3;
        if (color == gp.COLOR_KYLE)         return gp.dialogStage >= 2;
        if (color == gp.COLOR_JOHNRU)       return allOtherEnemiesDefeated() && gp.dialogStage >= 2;
        if (color == gp.COLOR_ADRIAN)       return gp.dialogStage >= 3;
        return false;
    }

    private boolean allOtherEnemiesDefeated() {
        return gp.enemyStats.isDefeated(gp.COLOR_JAMES) &&
                gp.enemyStats.isDefeated(gp.COLOR_ALIEYANDREW) &&
                gp.enemyStats.isDefeated(gp.COLOR_KYLE) &&
                gp.enemyStats.isDefeated(gp.COLOR_ADRIAN);
    }

    private void updateDialogue(int color) {
        if (color == gp.COLOR_JAMES) {
            if (gp.dialogStage == 1)      gp.currentDialog = "James: Yo " + characterName + " do you want your money back?";
            else if (gp.dialogStage == 2) gp.currentDialog = "James: Okay but you got to win first lets battle";
            else                          gp.currentDialog = "";
        } else if (color == gp.COLOR_ALIEYANDREW) {
            if (gp.dialogStage == 1)      gp.currentDialog = "Alieyandrew: Oh hi there " + characterName;
            else if (gp.dialogStage == 2) gp.currentDialog = "Alieyandrew: So you want your money?";
            else if (gp.dialogStage == 3) gp.currentDialog = "Alieyandrew: Okay beat me then ill give it to you";
            else                          gp.currentDialog = "";
        } else if (color == gp.COLOR_KYLE) {
            if (gp.dialogStage == 1)      gp.currentDialog = "Kyle: Oi!! " + characterName + " your here to get your money?";
            else if (gp.dialogStage == 2) gp.currentDialog = "Kyle: HAHAHAAHAHAHAHA, COME BEAT ME FIRST";
            else                          gp.currentDialog = "";
        } else if (color == gp.COLOR_JOHNRU) {
            if (!allOtherEnemiesDefeated()) {
                gp.currentDialog = "Johnru: come back to me when you have beaten the others";
                gp.dialogStage = 0;
            } else {
                if (gp.dialogStage == 1)      gp.currentDialog = "Johnru: Oh, you think your strong now huh, " + characterName;
                else if (gp.dialogStage == 2) gp.currentDialog = "Johnru: Well lets go fight now, I'll let you get out of the room";
                else                          gp.currentDialog = "";
            }
        } else if (color == gp.COLOR_ADRIAN) {
            if (gp.dialogStage == 1)      gp.currentDialog = "Adrian: Hi There " + characterName + ", class is over";
            else if (gp.dialogStage == 2) gp.currentDialog = "Adrian: Oh you want your money that I owe you?";
            else if (gp.dialogStage == 3) gp.currentDialog = "Adrian: Okay no problem but lets play a game first";
            else                          gp.currentDialog = "";
        } else if (color == gp.COLOR_DOOR) {
            gp.currentDialog = "This door is broken and cant be opened.";
        } else if (color == gp.COLORNEXTAREA) {
            if (gp.enemyStats.isDefeated(gp.COLOR_JOHNRU)) {
                gp.currentDialog = "Press E to go to the walkway.";
            } else {
                gp.currentDialog = "Defeat Johnru first.";
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage img = null;
        boolean isMoving = keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed;
        switch (direction) {
            case "up":    img = isMoving ? (spriteNum == 1 ? up1    : up2)    : standUp;    break;
            case "down":  img = isMoving ? (spriteNum == 1 ? down1  : down2)  : standDown;  break;
            case "left":  img = isMoving ? (spriteNum == 1 ? left1  : left2)  : standLeft;  break;
            case "right": img = isMoving ? (spriteNum == 1 ? right1 : right2) : standRight; break;
        }

        g2.setColor(new Color(0, 0, 0, 100));
        int spriteSize = gp.getScaledTileSize();
        int shadowWidth = (int)(spriteSize * 0.6);
        int shadowHeight = (int)(spriteSize * 0.15);
        int shadowX = x + (spriteSize - shadowWidth) / 2;
        int shadowY = y + spriteSize - shadowHeight - gp.scaleUniform(15);
        g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);

        g2.drawImage(img, x, y, spriteSize, spriteSize, null);

        if (gp.showDebug) {
            g2.setColor(Color.RED);
            Rectangle collisionBox = getPlayerCollisionBox(x, y);
            g2.drawRect(collisionBox.x, collisionBox.y, collisionBox.width, collisionBox.height);
        }

        if (nearInteractable && gp.currentDialog.isEmpty()) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, gp.scaleUniform(20)));
            g2.drawString("Press E to talk", x + gp.scaleUniform(10), y - gp.scaleUniform(10));
        }
    }
}