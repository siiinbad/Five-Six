package panels;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import main.GamePanel;

public class MenuPage {
    GamePanel gp;

    // Buttons
    public Rectangle startBtn, ivanBtn, nimuelBtn, samBtn, libraryBtn, exitBtn;

    public MenuPage(GamePanel gp) {
        this.gp = gp;
        initButtons();
    }

    private void initButtons() {
        int btnW = 300;
        int btnH = 60;
        int centerX = gp.screenWidth / 2 - btnW / 2;

        startBtn = new Rectangle(centerX, 250, btnW, btnH);
        ivanBtn = new Rectangle(centerX, 350, btnW, btnH);
        nimuelBtn = new Rectangle(centerX, 450, btnW, btnH);
        samBtn = new Rectangle(centerX, 550, btnW, btnH);
        libraryBtn = new Rectangle(centerX, 650, btnW, btnH);
        exitBtn = new Rectangle(centerX, 750, btnW, btnH);
    }

    public void draw(Graphics2D g2) {
        g2.setColor(new Color(50, 50, 80));
        g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("MAIN MENU", gp.getWidth() / 2 - 150, 150);

        drawBtn(g2, "START GAME", startBtn);
        drawBtn(g2, "IVAN", ivanBtn);
        drawBtn(g2, "NIMUEL", nimuelBtn);
        drawBtn(g2, "SAM", samBtn);
        drawBtn(g2, "LIBRARY", libraryBtn);
        drawBtn(g2, "EXIT", exitBtn);
    }

    private void drawBtn(Graphics2D g2, String text, Rectangle r) {
        g2.setColor(Color.WHITE);
        g2.draw(r);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        int textX = r.x + (r.width - fm.stringWidth(text)) / 2;
        int textY = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, textX, textY);
    }

    public void handleClick(Point p) {
        if (startBtn.contains(p)) {
            gp.gameState = gp.playState;
            gp.player = new gp.playerClass(gp, gp.keyH, "ivan"); // default
        } else if (ivanBtn.contains(p)) {
            gp.selectChar("ivan");
        } else if (nimuelBtn.contains(p)) {
            gp.selectChar("nimuel");
        } else if (samBtn.contains(p)) {
            gp.selectChar("sam");
        } else if (libraryBtn.contains(p)) {
            new gp.LibraryUI(); // open library window
        } else if (exitBtn.contains(p)) {
            System.exit(0);
        }
    }
}