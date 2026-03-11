package main;

import entity.Player;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import panels.SelectCharacter;

public class GamePanel extends JPanel implements Runnable {

    // Tiles
    final int originalTileSize = 16;
    final int scale = 10;
    public final int tileSize = originalTileSize * scale;
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    public int screenWidth = tileSize * maxScreenCol;
    int screenHeight = tileSize * maxScreenRow;

    int fps = 60;
    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;
    public Player player;

<<<<<<< HEAD
    BufferedImage mapImage;
=======
  Rectangle ivanBtn, nimuelBtn, samBtn, johnBtn;
>>>>>>> 32b2be8b2ca8978cbfb7a5ce2c6bcd96933f7c8e

    // Game states
    public int gameState;
    public final int loadingState = 0;
    public final int titleState = 1;
    public final int charSelectState = 2;
    public final int playState = 3;
    public final int libraryState = 4;

    // Menu buttons
    Rectangle startBtn, selectCharBtn, libraryBtn, exitBtn;

<<<<<<< HEAD
    // Character select screen
    SelectCharacter charSelect;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        gameState = loadingState;

        loadImages();
        initMenuButtons();

        // Character select screen
        charSelect = new SelectCharacter(this);

        // Menu buttons mouse listener
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameState == titleState) handleMenuClick(e.getPoint());
            }
        });

        // Simulate loading screen delay
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            gameState = titleState;
        }).start();
    }

    private void loadImages() {
        try {
            mapImage = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/res/sprites/map/gle.png")));
        } catch (Exception e) {
            System.out.println("Map image failed to load, using placeholder.");
        }
=======
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
>>>>>>> 32b2be8b2ca8978cbfb7a5ce2c6bcd96933f7c8e
    }

    private void initMenuButtons() {
        int btnW = 300, btnH = 60;
        int centerX = screenWidth / 2 - btnW / 2;

<<<<<<< HEAD
        startBtn = new Rectangle(centerX, 250, btnW, btnH);
        selectCharBtn = new Rectangle(centerX, 350, btnW, btnH);
        libraryBtn = new Rectangle(centerX, 450, btnW, btnH);
        exitBtn = new Rectangle(centerX, 550, btnW, btnH);
    }
=======
    drawBtn(g2, "IVAN", ivanBtn);
    drawBtn(g2, "NIMUEL", nimuelBtn);
    drawBtn(g2, "SAM", samBtn);
    drawBtn(g2,  "JOHN", johnBtn);
  }
>>>>>>> 32b2be8b2ca8978cbfb7a5ce2c6bcd96933f7c8e

    private void handleMenuClick(Point p) {
        if (startBtn.contains(p)) {
            player = new Player(this, keyH, "ivan"); // default
            gameState = playState;
        } else if (selectCharBtn.contains(p)) {
            gameState = charSelectState;
        } else if (libraryBtn.contains(p)) {
            gameState = libraryState;
        } else if (exitBtn.contains(p)) {
            System.exit(0);
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / fps;
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
        if (gameState == playState && player != null) {
            player.update();
            if (keyH.escPressed) gameState = titleState;
        }
        if (gameState == charSelectState) {
            if (keyH.escPressed) gameState = titleState;
        }
        if (gameState == libraryState) {
            if (keyH.escPressed) gameState = titleState;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        switch (gameState) {
            case loadingState -> drawLoading(g2);
            case titleState -> drawMenu(g2);
            case charSelectState -> charSelect.draw(g2);
            case playState -> drawGame(g2);
            case libraryState -> drawLibrary(g2);
        }

        g2.dispose();
    }

    private void drawLoading(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("LOADING...", getWidth()/2 - 150, getHeight()/2);
    }

    private void drawMenu(Graphics2D g2) {
        g2.setColor(new Color(50,50,80));
        g2.fillRect(0,0,getWidth(),getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("MAIN MENU", getWidth()/2 - 150, 150);

        drawBtn(g2, "START GAME", startBtn);
        drawBtn(g2, "SELECT CHARACTER", selectCharBtn);
        drawBtn(g2, "LIBRARY", libraryBtn);
        drawBtn(g2, "EXIT", exitBtn);
    }

    private void drawGame(Graphics2D g2) {
        if (mapImage != null) g2.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);
        else {
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0,0,getWidth(),getHeight());
        }

        if (player != null) player.draw(g2);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("ESC to Menu", 20,30);
    }

    private void drawLibrary(Graphics2D g2) {
        g2.setColor(new Color(30,30,30));
        g2.fillRect(0,0,getWidth(),getHeight());

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("LIBRARY", getWidth()/2 - 120, 120);

        g2.setFont(new Font("Arial", Font.PLAIN, 28));
        g2.drawString("Items:", 200, 250);
        g2.drawString("- Sword : +10 Attack", 200, 300);
        g2.drawString("- Shield : +15 Defense", 200, 340);

        g2.drawString("Skills:", 200, 420);
        g2.drawString("- Fireball : 20 Mana", 200, 470);
        g2.drawString("- Heal : Restore HP", 200, 510);

        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Press ESC to return to menu", 20, 40);
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