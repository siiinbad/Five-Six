package panels;

import java.awt.*;
import javax.swing.ImageIcon;

import main.GamePanel;

public class SelectCharacter {

    GamePanel gp;

    Rectangle ivanBtn, nimuelBtn, samBtn, backBtn, selectBtn;
    public String selectedChar = ""; // store selection
    String hoveredChar = "";

    ImageIcon ivanImg, nimuelImg, samImg;

    public SelectCharacter(GamePanel gp) {
        this.gp = gp;
        initButtons();
        loadImages();
    }

    private void initButtons() {
        int btnW = 250, btnH = 60;
        int centerX = gp.screenWidth / 4 + 50;
        ivanBtn = new Rectangle(20, 250, btnW, btnH);
        nimuelBtn = new Rectangle(20, 350, btnW, btnH);
        samBtn = new Rectangle(20, 450, btnW, btnH);
        backBtn = new Rectangle(20, 550, btnW, btnH);
        selectBtn = new Rectangle(centerX, 650, btnW, btnH);
    }

    private void loadImages() {
        try {
            ivanImg = new ImageIcon(getClass().getResource("/res/sprites/ivan/ivan_initial_select.gif"));
            nimuelImg = new ImageIcon(getClass().getResource("/res/sprites/nimuel/nimuel_initial_select.gif"));
            samImg = new ImageIcon(getClass().getResource("/res/sprites/sam/sam_initial_select.gif"));
        } catch (Exception e) {
            System.out.println("Failed to load character images.");
        }
    }

    public void handleClick(Point p) {
        if (p == null) return;
        else if (ivanBtn.contains(p)) selectedChar = "ivan";
        else if (nimuelBtn.contains(p)) selectedChar = "nimuel";
        else if (samBtn.contains(p)) selectedChar = "sam";
        else if(selectBtn.contains(p) && !selectedChar.isEmpty() && selectedChar != null){
            gp.selectChar(selectedChar);
        }
        else if (backBtn.contains(p)) {
            gp.openTitle();
        }
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
        drawBtn(g2, "BACK", backBtn);
        drawBtn(g2, "SELECT", selectBtn);

        // Hover text
        if (!hoveredChar.isEmpty()) {
            g2.setFont(new Font("Arial", Font.PLAIN, 28));
            g2.setColor(Color.YELLOW);
            g2.drawString(hoveredChar, gp.getWidth()/2 - 150, 550);
        }

        // Hover images safely
        Point mouse = gp.getMousePosition();
        if (mouse != null) {
            if (ivanBtn.contains(mouse)) drawHoverImage(g2, ivanImg.getImage());
            else if (nimuelBtn.contains(mouse)) drawHoverImage(g2, nimuelImg.getImage());
            else if (samBtn.contains(mouse)) drawHoverImage(g2, samImg.getImage());
        }

        // Selected highlight
        g2.setColor(Color.GREEN);
        if (selectedChar.equals("ivan")) g2.draw(ivanBtn);
        else if (selectedChar.equals("nimuel")) g2.draw(nimuelBtn);
        else if (selectedChar.equals("sam")) g2.draw(samBtn);
    }

    private void drawHoverImage(Graphics2D g2, Image img) {
        if (img != null) {
            g2.drawImage(img, gp.getWidth()/2 - 150, 200, 450, 450, null);
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