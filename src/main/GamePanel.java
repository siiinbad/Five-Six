package main;

import entity.Player;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable{
  final int originalTileSize = 16; // 16x16 tile
  final int scale = 3;
  
  public final int tileSize = originalTileSize * scale; // 48x48 tile
  final int maxScreenCol = 16;
  final int maxScreenRow = 12;
  final int screenWidth = tileSize * maxScreenCol; // 768 pixels
  final int screenHeight = tileSize * maxScreenRow; // 576 pixels

  int fps = 60;

  KeyHandler keyH = new KeyHandler();
  Thread gameThread;
  Player player = new Player(this, keyH);

  public GamePanel() {
    this.setPreferredSize(new Dimension(screenWidth, screenHeight));
    this.setBackground(Color.black);
    this.setDoubleBuffered(true);
    this.addKeyListener(keyH);
    this.setFocusable(true);
  }

  public void startGameThread(){
    gameThread = new Thread(this);
    gameThread.start();
  }
  
  @Override
  public void run() {
    double drawInterval = 1000000000/fps; // 0.01666 seconds
    double delta = 0;
    long lastTime = System.nanoTime();
    long currentTime;
    
    while(gameThread != null){
        currentTime = System.nanoTime();
        delta += (currentTime - lastTime) / drawInterval;
        lastTime = currentTime;

        if(delta >= 1){
            update();
            delta = 0;
        }

        repaint();
    }   
  }

  public void update(){
        player.update();
  }

  @Override
  public void paintComponent(Graphics g){
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D)g;
    
    player.draw(g2);

    g2.dispose();
  }
}