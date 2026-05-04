import java.awt.*;
import javax.swing.JPanel;

public class Background {

    private final Image image;
    private final JPanel panel;
    private final int moveAmount;
    private int xOffset = 0;

    public Background(JPanel panel, String filename, int moveAmount) {
        this.panel = panel;
        this.image = ImageManager.loadImage(filename);
        this.moveAmount = moveAmount;
    }

    public void moveRight() {
        xOffset -= moveAmount;
    }

    public void moveLeft() {
        xOffset += moveAmount;
    }

    public void draw(Graphics2D g2) {
        if (image == null || panel == null) return;
        int width = panel.getWidth();
        int height = panel.getHeight();
        if (width <= 0 || height <= 0) return;

        int x = xOffset % width;
        if (x > 0) x -= width;

        for (; x < width; x += width) {
            g2.drawImage(image, x, 0, width, height, null);
        }
    }
}