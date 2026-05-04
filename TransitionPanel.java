import java.awt.*;
import javax.swing.*;

public class TransitionPanel extends JPanel {

    public TransitionPanel(GameWindow window, int difficulty) {
        setLayout(new BorderLayout());

        JLabel storyLabel = new JLabel("<html><div style='text-align: center;'>"
                + "<b style='font-size:28px'>STORY</b><br><br>"
                + "<span style='font-size:22px'>The moles you whacked were actually digital pests<br>"
                + "hacking into the mainframe. Now you must enter the<br>"
                + "circuit board to clean up the magnetic debris before<br>"
                + "the system crashes!Beware of the virus<br><br>"
                + "Prepare for: <b>Circuit Cleaner</b></span></div></html>");
        storyLabel.setForeground(Color.WHITE);
        storyLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton continueBtn = new JButton("Next");
        continueBtn.setFont(new Font("Arial", Font.BOLD, 28));
        continueBtn.addActionListener(e -> window.launchCircuitCleanerGame());

        add(storyLabel, BorderLayout.CENTER);
        add(continueBtn, BorderLayout.SOUTH);

        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint gradient = new GradientPaint(0, 0, new Color(30,30,60), getWidth(), getHeight(), new Color(10,10,20));
        g2.setPaint(gradient);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}