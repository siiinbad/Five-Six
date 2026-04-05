package main;

import entity.Enemy;
import entity.Player;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import panels.*;
import collision.*;

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

    // MAP SYSTEM
    public int currentMap = 1;
    public final int MAP_1 = 1;
    public final int MAP_2 = 2;

    BufferedImage map1Image;
    BufferedImage map2Image;

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

    // MAP DATA
    public List<Enemy> map1Enemies = new ArrayList<>();
    public List<Enemy> map2Enemies = new ArrayList<>();

    public List<Rectangle> map1Walls = new ArrayList<>();
    public List<Rectangle> map2Walls = new ArrayList<>();

    public CollisionChecker cChecker = new CollisionChecker(this);

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

        gameState = loadingState;

        loadImages();
        setupMaps();

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

        // Fake loading
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            gameState = titleState;
        }).start();
    }

    private void loadImages() {
        try {
            map1Image = ImageIO.read(Objects.requireNonNull(
                    getClass().getResourceAsStream("/res/sprites/map/gle.png")));

            map2Image = ImageIO.read(Objects.requireNonNull(
                    getClass().getResourceAsStream("/res/sprites/map/map2.png")));

        } catch (Exception e) {
            System.out.println("Map image failed to load.");
        }
    }

    private void setupMaps() {

        // -------- MAP 1 --------
        map1Walls.add(new Rectangle(264, 333, 433, 58)); // x, y, width, height
        map1Walls.add(new Rectangle(846, 333, 433, 58));

        map1Walls.add(new Rectangle(153, 495, 539, 58));
        map1Walls.add(new Rectangle(846, 495, 539, 58));

        map1Walls.add(new Rectangle(144, 675, 545, 58));
        map1Walls.add(new Rectangle(846, 675, 545, 58));


        map1Enemies.add(new Enemy(this, 300, 338, "alieyandrew"));
        map1Enemies.add(new Enemy(this, 1160, 190, "dirk"));
        map1Enemies.add(new Enemy(this, 700, 452, "james"));

        // -------- MAP 2 --------
        map2Walls.add(new Rectangle(200, 300, 400, 40));
        map2Walls.add(new Rectangle(800, 500, 400, 40));

        map2Enemies.add(new Enemy(this, 400, 250, "kyle"));
        map2Enemies.add(new Enemy(this, 900, 450, "vaughn"));
    }

    public void switchMap(int map) {
        currentMap = map;

        if (map == MAP_1) {
            player.worldX = 100;
            player.worldY = 100;
        } else if (map == MAP_2) {
            player.worldX = 200;
            player.worldY = 200;
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

            List<Enemy> currentEnemies =
                    (currentMap == MAP_1) ? map1Enemies : map2Enemies;

            for (Enemy e : currentEnemies) {
                e.update();
            }

            // SIMPLE MAP SWITCH (right edge)
            if (player.worldX > screenWidth - 50) {
                switchMap(MAP_2);
            }

            if (player.worldX < 10) {
                switchMap(MAP_1);
            }

            if (keyH.escPressed) gameState = titleState;
        }

        if ((gameState == charSelectState || gameState == libraryState)
                && keyH.escPressed) {
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

        BufferedImage currentMapImage =
                (currentMap == MAP_1) ? map1Image : map2Image;

        if (currentMapImage != null)
            g2.drawImage(currentMapImage, 0, 0, getWidth(), getHeight(), null);
        else {
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0,0,getWidth(),getHeight());
        }

        if (player != null) player.draw(g2);

        // DRAW ENEMIES
        List<Enemy> currentEnemies =
                (currentMap == MAP_1) ? map1Enemies : map2Enemies;

        for (Enemy e : currentEnemies) {
            e.draw(g2);
        }

        // DRAW WALLS
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0)); //make wall transparent
        g2.setColor(Color.GRAY);
        List<Rectangle> currentWalls = (currentMap == MAP_1) ? map1Walls : map2Walls;

        for (Rectangle wall : currentWalls) {
            g2.fillRect(wall.x, wall.y, wall.width, wall.height);
        }
        g2.setComposite(oldComposite);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("MAP: " + currentMap, 20, 30);
        g2.drawString("ESC to Menu", 20, 60);
    }

    private void drawLibrary(Graphics2D g2) {
        g2.setColor(new Color(30,30,50));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 50));
        g2.drawString("LIBRARY PLACEHOLDER",
                getWidth()/2 - 250, getHeight()/2);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.drawString("Press ESC to return",
                getWidth()/2 - 100, getHeight()/2 + 50);
    }

    public void selectChar(String name) {
        player = new Player(this, keyH, name);
        gameState = playState;
    }

    public void openTitle(){
        gameState = titleState;
    }

    public void openLibrary() {
        gameState = libraryState;
    }
}