import java.awt.*;
import java.util.*;

public class Animation2 {

    private final boolean snowEnabled;
    private final boolean leavesEnabled;

    private final ArrayList<Point> snowflakes = new ArrayList<>();
    private final ArrayList<Point> leaves = new ArrayList<>();

    private final Random rand = new Random();

    private final Image leafImage;

    public Animation2(boolean snow, boolean leaves) {

        this.snowEnabled = snow;
        this.leavesEnabled = leaves;

        leafImage = ImageManager.loadImage("leaf.png");

        // Create snow particles
        if (snowEnabled) {
            for (int i = 0; i < 60; i++) {
                snowflakes.add(new Point(
                        rand.nextInt(900),
                        rand.nextInt(600)));
            }
        }

        // Create leaf particles
        if (leavesEnabled) {
            for (int i = 0; i < 20; i++) {
                this.leaves.add(new Point(
                        rand.nextInt(900),
                        rand.nextInt(600)));
            }
        }
    }

    public void update(int width, int height) {

        if (snowEnabled) {
            for (Point p : snowflakes) {
                p.y += 3;

                if (p.y > height) {
                    p.y = 0;
                    p.x = rand.nextInt(width);
                }
            }
        }

        if (leavesEnabled) {
            for (Point p : leaves) {
                p.y += 2;
                p.x += rand.nextInt(5) - 2;

                if (p.y > height) {
                    p.y = 0;
                    p.x = rand.nextInt(width);
                }
            }
        }
    }

    public void draw(Graphics2D g2) {

        // Draw snow
        if (snowEnabled) {
            g2.setColor(Color.WHITE);
            for (Point p : snowflakes) {
                g2.fillOval(p.x, p.y, 6, 6);
            }
        }

        // Draw leaves
        if (leavesEnabled && leafImage != null) {
            for (Point p : leaves) {
                g2.drawImage(leafImage, p.x, p.y, 25, 25, null);
            }
        }
    }
}