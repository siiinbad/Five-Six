package entity;

import java.awt.image.BufferedImage;
import java.awt.*;

public class Entity {
    public int worldX, worldY;
    public int speed;

    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;

    public BufferedImage standUp, standDown, standLeft, standRight;

    public String direction;
    public int spriteCounter = 0;
    public int spriteNum = 1;
    public Rectangle solidArea = new Rectangle(0, 0, 48, 48);
    public boolean collisionOn = false;
}