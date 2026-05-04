import java.awt.*;

public class Bomb {

    private int x;
    private int y;
    private Image image;

    public Bomb(int x, int y) {
        this.x = x;
        this.y = y;
        image = ImageManager.loadImage("bomb.png");
    }

    public void draw(Graphics g) {
        g.drawImage(image, x, y, 60, 60, null);
    }
}