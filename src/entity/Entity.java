package entity;

import java.awt.image.BufferedImage;

public class Entity {
    public int x, y;
    public int speed;

    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
    public BufferedImage standUp, standDown, standLeft, standRight;

    public String direction;
    public int spriteCounter = 0;
    public int spriteNum = 1;

    // HP fields
    public int maxHP;
    public int currentHP;

    public void setHP(int maxHP) {
        this.maxHP = maxHP;
        this.currentHP = maxHP;
    }

    public void healPercent(double percent) {
        int amount = (int)(maxHP * percent);
        currentHP = Math.min(currentHP + amount, maxHP);
    }

    public void setHPPercent(double percent) {
        currentHP = (int)(maxHP * percent);
        if (currentHP < 1) currentHP = 1;
    }

    public boolean isAlive() {
        return currentHP > 0;
    }
}