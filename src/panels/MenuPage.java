package panels;

import java.awt.*;
import main.GamePanel;

public class MenuPage {

    GamePanel gp;

    public Rectangle startBtn, libraryBtn, exitBtn;
    String hoveredText = "";

    public MenuPage(GamePanel gp) {
        this.gp = gp;
        initButtons();
    }

    private void initButtons() {
        int btnW = 300, btnH = 60;
        int centerX = gp.screenWidth / 2 - btnW / 2;
        startBtn = new Rectangle(centerX, 250, btnW, btnH);
        libraryBtn = new Rectangle(centerX, 450, btnW, btnH);
        exitBtn = new Rectangle(centerX, 550, btnW, btnH);
    }

    public void handleClick(Point p) {
        if (startBtn.contains(p)) {
            gp.gameState = gp.charSelectState;
        } else if (libraryBtn.contains(p)) {
            gp.openLibrary();
        } else if (exitBtn.contains(p)) {
            System.exit(0);
        }
    }

    public void handleHover(Point p) {
        if (startBtn.contains(p)) hoveredText = "Start the adventure!";
        else if (libraryBtn.contains(p)) hoveredText = "View items and skills";
        else if (exitBtn.contains(p)) hoveredText = "Exit the game";
        else hoveredText = "";
    }

    public void draw(Graphics2D g2) {
        g2.setColor(new Color(50, 50, 80));
        g2.fillRect(0, 0, gp.getWidth(), gp.getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Ubuntu", Font.BOLD, 50));
        g2.drawString("FIVE SIX", gp.getWidth() / 2 - 150, 150);

        drawBtn(g2, "START GAME", startBtn);
        drawBtn(g2, "LIBRARY", libraryBtn);
        drawBtn(g2, "EXIT", exitBtn);

        if (!hoveredText.isEmpty()) {
            g2.setFont(new Font("Arial", Font.PLAIN, 25));
            g2.setColor(Color.YELLOW);
            g2.drawString(hoveredText, gp.getWidth()/2 - 100, 650);
        }
    }

    private void drawBtn(Graphics2D g2, String text, Rectangle r) {
        g2.setColor(Color.WHITE);
        g2.draw(r);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        int textX = r.x + (r.width - fm.stringWidth(text))/2;
        int textY = r.y + (r.height - fm.getHeight())/2 + fm.getAscent();
        g2.drawString(text, textX, textY);
    }
}