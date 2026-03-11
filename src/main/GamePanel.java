package main;

import entity.Player;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
  final int originalTileSize = 16;
  final int scale = 10;
  public final int tileSize = originalTileSize * scale;
  final int maxScreenCol = 16;
  final int maxScreenRow = 12;
  int screenWidth = tileSize * maxScreenCol;
  int screenHeight = tileSize * maxScreenRow;

  int fps = 60;
  KeyHandler keyH = new KeyHandler(this);
  Thread gameThread;
  public Player player;
  BufferedImage mapImage;
  BufferedImage menuImage; // Placeholder for menu image

  public int gameState;
  public final int titleState = 0;
  public final int playState = 1;

  Rectangle ivanBtn, nimuelBtn, samBtn, johnBtn;

  public GamePanel() {
    this.setPreferredSize(new Dimension(screenWidth, screenHeight));
    this.setBackground(Color.black);
    this.setDoubleBuffered(true);
    this.addKeyListener(keyH);
    this.setFocusable(true);

    gameState = titleState;
    loadImages();
    initButtons();

    this.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (gameState == titleState) {
          if (ivanBtn.contains(e.getPoint())) selectChar("ivan");
          else if (nimuelBtn.contains(e.getPoint())) selectChar("nimuel");
          else if (samBtn.contains(e.getPoint())) selectChar("sam");
          else if (johnBtn.contains(e.getPoint())) selectChar("john");
        }
      }
    });
  }

  private void initButtons() {
    int btnW = 300;
    int btnH = 60;
    int centerX = screenWidth / 2 - btnW / 2;

    ivanBtn = new Rectangle(centerX, 300, btnW, btnH);
    nimuelBtn = new Rectangle(centerX, 400, btnW, btnH);
    samBtn = new Rectangle(centerX, 500, btnW, btnH);
    johnBtn = new Rectangle(centerX, 600, btnW, btnH);
  }

  private void loadImages() {
    try {
      mapImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/gle.png")));
    } catch (Exception e) {
      System.out.println("Image loading failed, using color placeholders.");
    }
  }

  private void selectChar(String name) {
    player = new Player(this, keyH, name);
    gameState = playState;
  }

  public void startGameThread() {
    gameThread = new Thread(this);
    gameThread.start();
  }

  @Override
  public void run() {
    double drawInterval = 1000000000 / fps;
    double delta = 0;
    long lastTime = System.nanoTime();
    while (gameThread != null) {
      long currentTime = System.nanoTime();
      delta += (currentTime - lastTime) / drawInterval;
      lastTime = currentTime;
      if (delta >= 1) {
        update();
        repaint();
        delta--;
      }
    }
  }

  public void update() {
    if (gameState == playState) {
      player.update();
      // Return to menu if ESC is pressed
      if (keyH.escPressed) {
        gameState = titleState;
      }
    }
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    if (gameState == titleState) {
      drawMenu(g2);
    } else if (gameState == playState) {
      if (mapImage != null) {
        g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);
      } else {
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, getWidth(), getHeight());
      }
      player.draw(g2);

      // Visual indicator for returning to menu
      g2.setColor(Color.WHITE);
      g2.setFont(new Font("Arial", Font.PLAIN, 20));
      g2.drawString("ESC to Menu", 20, 30);
    }
    g2.dispose();
  }

  private void drawMenu(Graphics2D g2) {
    if (menuImage != null) {
      g2.drawImage(menuImage, 0, 0, getWidth(), getHeight(), null);
    } else {
      g2.setColor(new Color(50, 50, 80));
      g2.fillRect(0, 0, getWidth(), getHeight());
    }

    g2.setColor(Color.WHITE);
    g2.setFont(new Font("Arial", Font.BOLD, 50));
    g2.drawString("CHARACTER SELECT", getWidth()/2 - 250, 150);

    drawBtn(g2, "IVAN", ivanBtn);
    drawBtn(g2, "NIMUEL", nimuelBtn);
    drawBtn(g2, "SAM", samBtn);
    drawBtn(g2,  "JOHN", johnBtn);
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
}