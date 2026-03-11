package entity;

import java.awt.Graphics2D;
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

        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        x = 100;
        y = 100;
        speed = 4;
        direction = "down";
    }

    public void getPlayerImage() {
        try {
            String folder = characterName.substring(0, 1).toUpperCase() + characterName.substring(1);
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
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {
            if (keyH.upPressed) { direction = "up"; y -= speed; }
            else if (keyH.downPressed) { direction = "down"; y += speed; }
            else if (keyH.leftPressed) { direction = "left"; x -= speed; }
            else if (keyH.rightPressed) { direction = "right"; x += speed; }

            spriteCounter++;
            if (spriteCounter > 12) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
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
        g2.drawImage(image, x, y, gp.tileSize, gp.tileSize, null);
    }
}