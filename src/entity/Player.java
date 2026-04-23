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

    public int spawnX, spawnY;

    public Player(GamePanel gp, KeyHandler keyH, String characterName) {
        this.gp = gp;
        this.keyH = keyH;
        this.characterName = characterName;

        charType = CharacterStats.CharacterType.fromName(characterName);
        damageMultiplier = charType.damageMultiplier;

        setHP(charType.maxHP);

        speed = 4;
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
            String path = "/res/sprites/" + characterName + "/" + characterName + "_";
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

        int nextX = x, nextY = y;
        boolean moving = false;

        if (keyH.upPressed)    { direction = "up";    nextY -= speed; moving = true; }
        else if (keyH.downPressed)  { direction = "down";  nextY += speed; moving = true; }
        else if (keyH.leftPressed)  { direction = "left";  nextX -= speed; moving = true; }
        else if (keyH.rightPressed) { direction = "right"; nextX += speed; moving = true; }

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

        int bw = 70;
        int bh = gp.tileSize - 60;
        int cx = nx + gp.tileSize / 2;
        int sy = ny + 40;

        int[][] pts = {
                {cx - bw/2, sy}, {cx - bw/2, sy + bh/3}, {cx - bw/2, sy + (2*bh)/3}, {cx - bw/2, sy + bh},
                {cx + bw/2, sy}, {cx + bw/2, sy + bh/3}, {cx + bw/2, sy + (2*bh)/3}, {cx + bw/2, sy + bh},
                {cx, sy}, {cx, sy + bh}
        };

        for (int[] p : pts) {
            int ix = p[0] * gp.hitboxImage.getWidth() / gp.getWidth();
            int iy = p[1] * gp.hitboxImage.getHeight() / gp.getHeight();

            if (ix < 0 || ix >= gp.hitboxImage.getWidth() || iy < 0 || iy >= gp.hitboxImage.getHeight()) {
                continue;
            }

            int color = gp.hitboxImage.getRGB(ix, iy) & 0xFFFFFF;

            if (color == gp.COLOR_WALL) {
                return true;
            }

            boolean isEnemy =
                    color == gp.COLOR_JAMES ||
                            color == gp.COLOR_ALIEYANDREW ||
                            color == gp.COLOR_KYLE ||
                            color == gp.COLOR_JOHNRU ||
                            color == gp.COLOR_ADRIAN;

            if (isEnemy && gp.enemyStats.isDefeated(color)) {
                continue;
            }

            if (isEnemy) {
                int enemyScreenX = (ix * gp.getWidth()) / gp.hitboxImage.getWidth();
                int enemyScreenY = (iy * gp.getHeight()) / gp.hitboxImage.getHeight();

                int enemyBoxW = 70;
                int enemyBoxH = 70;
                int enemyOffsetY = 45;

                // Make Alieyandrew collision narrower
                if (color == gp.COLOR_ALIEYANDREW) {
                    enemyBoxW = 42;
                    enemyBoxH = 62;
                    enemyOffsetY = 48;
                }

                // Make Kyle collision shorter/lower on Y
                if (color == gp.COLOR_KYLE) {
                    enemyBoxW = 60;
                    enemyBoxH = 48;
                    enemyOffsetY = 62;
                }

                Rectangle playerBox = new Rectangle(nx + 45, ny + 40, 70, gp.tileSize - 60);
                Rectangle enemyBox = new Rectangle(
                        enemyScreenX - enemyBoxW / 2,
                        enemyScreenY - gp.tileSize / 2 + enemyOffsetY,
                        enemyBoxW,
                        enemyBoxH
                );

                if (playerBox.intersects(enemyBox)) {
                    return true;
                }
            }
        }

        return false;
    }




    private void checkInteractions() {
        nearInteractable = false;
        if (gp.hitboxImage == null) return;

        int interactX = x + gp.tileSize / 2;
        int interactY = y + gp.tileSize / 2;

        int interactDistance = gp.tileSize / 2;

        switch (direction) {
            case "up" -> interactY -= interactDistance;
            case "down" -> interactY += interactDistance;
            case "left" -> interactX -= interactDistance;
            case "right" -> interactX += interactDistance;
        }

        int ix = interactX * gp.hitboxImage.getWidth() / gp.getWidth();
        int iy = interactY * gp.hitboxImage.getHeight() / gp.getHeight();

        if (ix < 0 || ix >= gp.hitboxImage.getWidth() || iy < 0 || iy >= gp.hitboxImage.getHeight()) return;

        int color = gp.hitboxImage.getRGB(ix, iy) & 0xFFFFFF;

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
        int shadowWidth = (int)(gp.tileSize * 0.6);
        int shadowHeight = (int)(gp.tileSize * 0.15);
        int shadowX = x + (gp.tileSize - shadowWidth) / 2;
        int shadowY = y + gp.tileSize - shadowHeight - 15;
        g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);

        g2.drawImage(img, x, y, gp.tileSize, gp.tileSize, null);

        if (gp.showDebug) {
            g2.setColor(Color.RED);
            int bw = 70, bh = gp.tileSize - 60;
            g2.drawRect(x + (gp.tileSize/2) - (bw/2), y + 40, bw, bh);
        }

        if (nearInteractable && gp.currentDialog.isEmpty()) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.drawString("Press E to talk", x + 10, y - 10);
        }
    }
}