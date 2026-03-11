package panels;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import main.GamePanel;

public class SelectCharacter {

    GamePanel gp;

    Rectangle ivanBtn, nimuelBtn, samBtn;
    public String selectedChar = ""; // store selection
    String hoveredChar = "";

    BufferedImage ivanImg, nimuelImg, samImg;

    public SelectCharacter(GamePanel gp) {
        this.gp = gp;
        initButtons();
        loadImages();
    }

    private void initButtons() {
        int btnW = 250, btnH = 60;
        int centerX = gp.screenWidth / 2 - btnW / 2;
        ivanBtn = new Rectangle(centerX, 250, btnW, btnH);
        nimuelBtn = new Rectangle(centerX, 350, btnW, btnH);
        samBtn = new Rectangle(centerX, 450, btnW, btnH);
    }

    private void loadImages() {
        try {
            ivanImg = ImageIO.read(getClass().getResourceAsStream("/res/sprites/ivan/ivan_front_stand.png"));
            nimuelImg = ImageIO.read(getClass().getResourceAsStream("/res/sprites/nimuel/nimuel_front_stand.png"));
            samImg = ImageIO.read(getClass().getResourceAsStream("/res/sprites/sam/sam_front_stand.png"));
        } catch (Exception e) {
            System.out.println("Failed to load character images.");
        }
    }

    public void handleClick(Point p) {
        if (p == null) return;
        if (ivanBtn.contains(p)) selectedChar = "ivan";
        else if (nimuelBtn.contains(p)) selectedChar = "nimuel";
        else if (samBtn.contains(p)) selectedChar = "sam";
    }

    public void handleHover(Point p) {
        if (p == null) {
            hoveredChar = "";
            return;
        }
        if (ivanBtn.contains(p)) hoveredChar = "Ivan: Attack +10, HP +100";
        else if (nimuelBtn.contains(p)) hoveredChar = "Nimuel: Magic +15, MP +120";
        else if (samBtn.contains(p)) hoveredChar = "Sam: Defense +20, HP +150";
        else hoveredChar = "";
    }

    public void draw(Graphics2D g2) {
        g2.setColor(new Color(50,50,80));
        g2.fillRect(0,0,gp.getWidth(), gp.getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("SELECT CHARACTER", gp.getWidth()/2 - 220, 150);

        drawBtn(g2, "IVAN", ivanBtn);
        drawBtn(g2, "NIMUEL", nimuelBtn);
        drawBtn(g2, "SAM", samBtn);

        // Hover text
        if (!hoveredChar.isEmpty()) {
            g2.setFont(new Font("Arial", Font.PLAIN, 28));
            g2.setColor(Color.YELLOW);
            g2.drawString(hoveredChar, gp.getWidth()/2 - 150, 550);
        }

        // Hover images safely
        Point mouse = gp.getMousePosition();
        if (mouse != null) {
            if (ivanBtn.contains(mouse)) drawHoverImage(g2, ivanImg);
            else if (nimuelBtn.contains(mouse)) drawHoverImage(g2, nimuelImg);
            else if (samBtn.contains(mouse)) drawHoverImage(g2, samImg);
        }

        // Selected highlight
        g2.setColor(Color.GREEN);
        if (selectedChar.equals("ivan")) g2.draw(ivanBtn);
        else if (selectedChar.equals("nimuel")) g2.draw(nimuelBtn);
        else if (selectedChar.equals("sam")) g2.draw(samBtn);
    }

    private void drawHoverImage(Graphics2D g2, BufferedImage img) {
        if (img != null) {
            int w = 150, h = 150;
            g2.drawImage(img, gp.getWidth()/2 - w/2, 380, w, h, null);
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