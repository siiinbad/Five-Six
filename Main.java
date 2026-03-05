import javax.swing.JFrame;

public class Main {
  public static void main(String[] args) {
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    frame.setResizable(false);
    frame.setTitle("Five-Six");
    frame.setLocationRelativeTo(null);

    FiveSixGamePanel gamePanel = new FiveSixGamePanel();
    frame.add(gamePanel);

    frame.pack();
  }
}
