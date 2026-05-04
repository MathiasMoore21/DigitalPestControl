import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TileMapTransitionMiniGame implements MiniGame {

    @SuppressWarnings("unused")
    private final GameWindow window;
    private final JPanel buttonPanel;

    public TileMapTransitionMiniGame(GameWindow window) {
        this.window = window;

        // Play transition sound
        SoundManager.playLoop("tilemap_transition.wav");

        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton continueBtn = new JButton("Next");
        continueBtn.setFont(new Font("Arial", Font.BOLD, 24));
        continueBtn.addActionListener(e -> {
            SoundManager.stop();
            window.getGamePanel().switchGame(new TileMapMiniGame(window));
        });
        buttonPanel.add(continueBtn);
    }

    @Override public void update() {}

    @Override
    public void draw(Graphics2D g2, int width, int height) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(0, 30, 30),
                                              width, height, new Color(0, 10, 50));
        g2.setPaint(gp);
        g2.fillRect(0, 0, width, height);

        g2.setFont(new Font("Arial", Font.BOLD, 36));
        g2.setColor(Color.CYAN);
        FontMetrics fm = g2.getFontMetrics();
        String title = "SYSTEM ALERT";
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 - 130);

        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        g2.setColor(Color.WHITE);
        fm = g2.getFontMetrics();
        String[] lines = {
            "The circuit board has been partially cleaned,",
            "but the core sectors are still corrupted!",
            "",
            "Navigate the Tile Grid and reach the power core",
            "before it's too late.",
            "",
            "Restore the system by reaching the core!"
        };
        int y = height / 2 - 70;
        for (String line : lines) {
            g2.drawString(line, (width - fm.stringWidth(line)) / 2, y);
            y += 32;
        }
    }

    @Override public void keyPressed(KeyEvent e)     {}
    @Override public void keyReleased(KeyEvent e)    {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public JPanel getButtonPanel()         { return buttonPanel; }
}