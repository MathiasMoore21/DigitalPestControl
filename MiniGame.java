import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

public interface MiniGame {
    void update();
    void draw(Graphics2D g2, int width, int height);
    void keyPressed(KeyEvent e);
    void keyReleased(KeyEvent e);
    void mousePressed(MouseEvent e);
    JPanel getButtonPanel(); // null if no buttons needed
}