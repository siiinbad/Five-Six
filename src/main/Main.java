package main;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("Five-Six");
        window.setUndecorated(true);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        window.pack();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        gd.setFullScreenWindow(window);

        window.setVisible(true);
        gamePanel.startGameThread();

        
    }
}