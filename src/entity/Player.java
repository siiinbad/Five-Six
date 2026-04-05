package entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import main.GamePanel;
import main.KeyHandler;

public class Player extends Entity {

    GamePanel gp;
    KeyHandler keyH;
    public String characterName;

    public Player(GamePanel gp, KeyHandler keyH, String characterName) {
        this.gp = gp;
        this.keyH = keyH;
        this.characterName = characterName;

        solidArea = new Rectangle(40, 85, 70, 35);

        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        worldX = 100;
        worldY = 100;
        speed = 4;
        direction = "down";
    }

    public void getPlayerImage() {
        try {
            String folder = characterName;
            String path = "/res/sprites/" + folder + "/" + characterName + "_";

            up1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "back_walk1.png")));
            up2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "back_walk2.png")));
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "front_walk1.png")));
            down2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "front_walk2.png")));
            left1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "left_walk1.png")));
            left2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "left_walk2.png")));
            right1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "right_walk1.png")));
            right2 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "right_walk2.png")));

            standUp = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "back_stand.png")));
            standDown = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "front_stand.png")));
            standLeft = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "left_stand.png")));
            standRight = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path + "right_stand.png")));

        } catch (IOException | NullPointerException e) {
            System.out.println("Error loading player images for: " + characterName);
            e.printStackTrace();
        }
    }

    public void update() {

        int nextX = worldX;
        int nextY = worldY;

        boolean moving = false;

        if (keyH.upPressed) {
            direction = "up";
            nextY -= speed;
            moving = true;
        } else if (keyH.downPressed) {
            direction = "down";
            nextY += speed;
            moving = true;
        } else if (keyH.leftPressed) {
            direction = "left";
            nextX -= speed;
            moving = true;
        } else if (keyH.rightPressed) {
            direction = "right";
            nextX += speed;
            moving = true;
        }

        // Predict future hitbox
        Rectangle futureBox = new Rectangle(
                nextX + solidArea.x,
                nextY + solidArea.y,
                solidArea.width,
                solidArea.height
        );

        collisionOn = false;

        // Get correct walls for current map
        java.util.List<Rectangle> currentWalls =
                (gp.currentMap == gp.MAP_1) ? gp.map1Walls : gp.map2Walls;

        for (Rectangle wall : currentWalls) {
            if (futureBox.intersects(wall)) {
                collisionOn = true;
                break;
            }
        }

        // Check enemies
        java.util.List<Enemy> currentEnemies =
                (gp.currentMap == gp.MAP_1) ? gp.map1Enemies : gp.map2Enemies;

        for (Enemy enemy : currentEnemies) {
            Rectangle enemyBox = new Rectangle(
                    enemy.worldX + enemy.solidArea.x,
                    enemy.worldY + enemy.solidArea.y,
                    enemy.solidArea.width,
                    enemy.solidArea.height
            );

            if (futureBox.intersects(enemyBox)) {
                collisionOn = true;
                break;
            }
        }

        // Move if no collision
        if (!collisionOn) {
            worldX = nextX;
            worldY = nextY;
        }

        // Animation
        if (moving) {
            spriteCounter++;
            if (spriteCounter > 12) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        } else {
            spriteNum = 1; // reset to standing
        }
    }

    public void draw(Graphics2D g2) {

        BufferedImage image = null;
        boolean isMoving = keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed;

        switch (direction) {
            case "up" -> image = isMoving ? (spriteNum == 1 ? up1 : up2) : standUp;
            case "down" -> image = isMoving ? (spriteNum == 1 ? down1 : down2) : standDown;
            case "left" -> image = isMoving ? (spriteNum == 1 ? left1 : left2) : standLeft;
            case "right" -> image = isMoving ? (spriteNum == 1 ? right1 : right2) : standRight;
        }

        g2.drawImage(image, worldX, worldY, gp.tileSize, gp.tileSize, null);

        // Debug hitbox
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0));
        g2.setColor(Color.RED);
        g2.fillRect(
                worldX + solidArea.x,
                worldY + solidArea.y,
                solidArea.width,
                solidArea.height
        );
        g2.setComposite(oldComposite);
    }
}