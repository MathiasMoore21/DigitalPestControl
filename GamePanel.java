import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private static final int FPS_MS = 16;

    private MiniGame currentGame;
    private final Timer gameLoop;
    private final GameWindow window;
    private JPanel currentButtonPanel;

    public GamePanel(GameWindow window, MiniGame initialGame) {
        this.window = window;
        setLayout(new BorderLayout());
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        gameLoop = new Timer(FPS_MS, e -> {
            currentGame.update();
            repaint();
        });

        switchGame(initialGame);
        gameLoop.start();
    }

    public void switchGame(MiniGame game) {
        if (currentButtonPanel != null) {
            remove(currentButtonPanel);
            currentButtonPanel = null;
        }
        this.currentGame = game;
        JPanel btns = game.getButtonPanel();
        if (btns != null) {
            currentButtonPanel = btns;
            add(btns, BorderLayout.SOUTH);
        }
        revalidate();
        repaint();
        requestFocusInWindow();
    }

    public void endLevel() {
        gameLoop.stop();
        SoundManager.stop();
        window.returnToMenu();
    }

    public void exitToMenu() {
        gameLoop.stop();
        SoundManager.stop();
        window.returnToMenu();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentGame != null)
            currentGame.draw((Graphics2D) g, getWidth(), getHeight());
    }

    @Override public void keyPressed(KeyEvent e)    { if (currentGame != null) currentGame.keyPressed(e); }
    @Override public void keyReleased(KeyEvent e)   { if (currentGame != null) currentGame.keyReleased(e); }
    @Override public void keyTyped(KeyEvent e)      {}
    @Override public void mousePressed(MouseEvent e)  { if (currentGame != null) currentGame.mousePressed(e); }
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseClicked(MouseEvent e)  {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}