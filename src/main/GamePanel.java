package main;

import entity.Player;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import panels.MenuPage;
import panels.SelectCharacter;

public class GamePanel extends JPanel implements Runnable {

    // Tiles
    final int originalTileSize = 16;
    final int scale = 10;
    public final int tileSize = originalTileSize * scale;
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    public int screenWidth = tileSize * maxScreenCol;
    public int screenHeight = tileSize * maxScreenRow;

    int fps = 60;
    public KeyHandler keyH = new KeyHandler(this);
    Thread gameThread;
    public Player player;

    BufferedImage mapImage;

    // Game states
    public int gameState;
    public final int loadingState = 0;
    public final int titleState = 1;
    public final int charSelectState = 2;
    public final int playState = 3;
    public final int libraryState = 4;

    // Menu and character select
    public MenuPage menuPage;
    public SelectCharacter charSelect;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        gameState = loadingState;

        // Load images
        loadImages();

        // Initialize menu and character select
        menuPage = new MenuPage(this);
        charSelect = new SelectCharacter(this);

        // Mouse handling
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                if (p != null) {
                    if (gameState == titleState) menuPage.handleClick(p);
                    else if (gameState == charSelectState) charSelect.handleClick(p);
                }
            }
        });

        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                if (p != null) {
                    if (gameState == titleState) menuPage.handleHover(p);
                    else if (gameState == charSelectState) charSelect.handleHover(p);
                }
            }
        });

        // Simulate loading screen
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
        if ((gameState == charSelectState || gameState == libraryState) && keyH.escPressed) {
            gameState = titleState;
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        switch (gameState) {
            case loadingState -> drawLoading(g2);
            case titleState -> menuPage.draw(g2);
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
        g2.setColor(new Color(30,30,50));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("LIBRARY PLACEHOLDER", getWidth()/2 - 250, getHeight()/2);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Press ESC to return", getWidth()/2 - 100, getHeight()/2 + 50);
    }

    public void selectChar(String name) {
        player = new Player(this, keyH, name);
        gameState = playState;
    }

    public void openLibrary() {
        gameState = libraryState;
    }
}