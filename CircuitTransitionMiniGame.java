import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class CircuitTransitionMiniGame implements MiniGame {

    @SuppressWarnings("unused")
    private final GameWindow window;
    private final JPanel buttonPanel;

    public CircuitTransitionMiniGame(GameWindow window) {
        this.window = window;

        SoundManager.playLoop("circuit.wav");

        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton continueBtn = new JButton("Next");
        continueBtn.setFont(new Font("Arial", Font.BOLD, 28));
        continueBtn.addActionListener(e ->
            window.getGamePanel().switchGame(
                new CircuitCleanerMiniGame(window, "CircuitCleanerbg.jpg")));
        buttonPanel.add(continueBtn);
    }

    @Override public void update() {}

    @Override
    public void draw(Graphics2D g2, int width, int height) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 60),
                                              width, height, new Color(10, 10, 20));
        g2.setPaint(gp);
        g2.fillRect(0, 0, width, height);

        g2.setFont(new Font("Arial", Font.BOLD, 36));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String title = "STORY";
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 - 130);

        g2.setFont(new Font("Arial", Font.PLAIN, 22));
        fm = g2.getFontMetrics();
        String[] lines = {
            "The moles you whacked were actually digital pests",
            "hacking into the mainframe. Now you must enter the",
            "circuit board to clean up the magnetic debris before",
            "the system crashes! Beware of the virus.",
            "",
            "Prepare for: Circuit Cleaner"
        };
        int y = height / 2 - 80;
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