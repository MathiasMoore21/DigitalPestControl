import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GameWindow extends JFrame implements ActionListener {

    private static final String SOUND_SUMMER = "summer.wav";
    private static final String SOUND_SPRING = "spring.wav";
    private static final String SOUND_AUTUMN = "autumn.wav";
    private static final String SOUND_WINTER = "winter.wav";

    private JButton summerBtn, springBtn, autumnBtn, winterBtn;
    private final Image menuBg;
    private GamePanel gamePanel;

    public GameWindow() {
        setTitle("Whack-A-Mole");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        menuBg = ImageManager.loadImage("background.png");
        showMenu();
        setVisible(true);
        SoundManager.playLoop("mainscreen.wav");
    }

    private void showMenu() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (menuBg != null)
                    g.drawImage(menuBg, 0, 0, getWidth(), getHeight(), this);
                g.setFont(new Font("Arial", Font.BOLD, 48));
                g.setColor(Color.YELLOW);
                String title = "WHACK-A-MOLE";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, 100);
            }
        };
        panel.setLayout(new GridLayout(4, 1, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(150, 300, 150, 300));
        summerBtn = new JButton("Summer - Easy");
        springBtn = new JButton("Spring - Medium");
        autumnBtn = new JButton("Autumn - Hard");
        winterBtn = new JButton("Winter - Very Hard");
        summerBtn.addActionListener(this);
        springBtn.addActionListener(this);
        autumnBtn.addActionListener(this);
        winterBtn.addActionListener(this);
        panel.add(summerBtn);
        panel.add(springBtn);
        panel.add(autumnBtn);
        panel.add(winterBtn);
        setContentPane(panel);
        revalidate();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SoundManager.stop();
        if (e.getSource() == summerBtn)
            startGame("summerbg.jpg", false, false, SOUND_SUMMER, 0);
        else if (e.getSource() == springBtn)
            startGame("springbg.jpeg", false, false, SOUND_SPRING, 1);
        else if (e.getSource() == autumnBtn)
            startGame("autumnbg.jpeg", false, true, SOUND_AUTUMN, 2);
        else if (e.getSource() == winterBtn)
            startGame("winterbg.jpeg", true, false, SOUND_WINTER, 3);
    }

    private void startGame(String bg, boolean snow, boolean leaves, String sound, int difficulty) {
        WhackAMoleMiniGame whack = new WhackAMoleMiniGame(this, bg, snow, leaves, sound, difficulty);
        gamePanel = new GamePanel(this, whack);
        setContentPane(gamePanel);
        gamePanel.requestFocusInWindow();
        revalidate();
        repaint();
    }

    // Every MiniGame grabs this to call switchGame() on transitions
    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public void returnToMenu() {
        SoundManager.stop();
        showMenu();
        SoundManager.playLoop("mainscreen.wav");
    }

    public void launchCircuitCleanerGame() {
        CircuitCleanerMiniGame game = new CircuitCleanerMiniGame(this, "CircuitCleanerbg.jpg");
        gamePanel.switchGame(game);
    }
}

    