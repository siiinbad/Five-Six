package entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;
import main.GamePanel;

public class Enemy extends Entity {

    GamePanel gp;

    public String name;
    BufferedImage image;

    public Enemy(GamePanel gp, int worldX, int worldY, String name) {
        this.gp = gp;
        this.worldX = worldX;
        this.worldY = worldY;
        this.name = name;

        speed = 0;
        direction = "down";

        solidArea = new Rectangle(47, 100, 60, 16);

        getEnemyImage(); // 🔥 load image
    }

    public void getEnemyImage() {
        try {
            String path = "/res/sprites/" + name + "/" + name + "_stand.png";

            image = ImageIO.read(Objects.requireNonNull(
                getClass().getResourceAsStream(path)
            ));

        } catch (IOException | NullPointerException e) {
            System.out.println("Failed to load enemy: " + name);
        }
    }

    public void update() {
        // static enemy (can add AI later)
    }

    public void draw(Graphics2D g2) {

        if (image != null) {
            g2.drawImage(image, worldX, worldY, gp.tileSize, gp.tileSize, null);
        } else {
            g2.setColor(Color.RED);
            g2.fillRect(worldX, worldY, gp.tileSize, gp.tileSize);
        }

        // hitbox debug
        g2.setColor(Color.YELLOW);
        g2.drawRect(
            worldX + solidArea.x,
            worldY + solidArea.y,
            solidArea.width,
            solidArea.height
        );
    }
}