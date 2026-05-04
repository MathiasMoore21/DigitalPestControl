import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

public class CircuitCleanerMiniGame implements MiniGame {
    private final GameWindow window;
    private final Image background;
    private final Image headImg;
    private final Image[] scrapImgs = new Image[3];
    private final Image virusImg;
    private final ArrayList<Point> cable = new ArrayList<>();
    private Point scrap;

    // Two viruses now
    private Point virus1;
    private Point virus2;

    private int scrapType = 1;
    private int score = 0;
    private int direction = 1;
    private boolean started = false;
    private boolean paused = false;
    private boolean gameOver = false;
    private final int step = 4;
    private int targetLength = 12; // bigger starting snake
    private final Random rand = new Random();

    private int virusMoveTick = 0;
    private int virusSpawnTimer = 0;
    private int lastWidth = 900;
    private int lastHeight = 600;

    // Chase speed — pixels per chase tick; keep low so it's fair
    private static final int VIRUS_CHASE_SPEED = 3;
    // How often (in update ticks) the virus moves
    private static final int VIRUS_MOVE_INTERVAL = 12;

    private final JPanel buttonPanel;

    public CircuitCleanerMiniGame(GameWindow window, String bg) {
        this.window = window;
        this.background = ImageManager.loadImage(bg);
        this.headImg    = new ImageIcon("cable.png").getImage();
        this.scrapImgs[0] = new ImageIcon("scrap1.png").getImage();
        this.scrapImgs[1] = new ImageIcon("scrap2.png").getImage();
        this.scrapImgs[2] = new ImageIcon("scrap3.png").getImage();
        this.virusImg     = new ImageIcon("virus.png").getImage();

        initCable();
        spawnScrap();
        SoundManager.playLoop("circuit.wav");

        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton pauseBtn   = new JButton("Pause");
        JButton restartBtn = new JButton("Restart");
        JButton exitBtn    = new JButton("Exit");
        pauseBtn.addActionListener(e -> { if (started && !gameOver) paused = !paused; });
        restartBtn.addActionListener(e -> restartGame());
        exitBtn.addActionListener(e -> window.getGamePanel().exitToMenu());
        buttonPanel.add(pauseBtn);
        buttonPanel.add(restartBtn);
        buttonPanel.add(exitBtn);
    }

    // ── Init / restart ────────────────────────────────────────────────

    private void initCable() {
        cable.clear();
        int cx = 450, cy = 300;
        // Larger starting length of 12 segments
        for (int i = 0; i < 12; i++) {
            cable.add(new Point(cx - i * 4, cy));
        }
    }

    private void restartGame() {
        SoundManager.stop();
        initCable();
        direction       = 1;
        targetLength    = 12;
        score           = 0;
        started         = false;
        paused          = false;
        gameOver        = false;
        virus1          = null;
        virus2          = null;
        virusSpawnTimer = 0;
        virusMoveTick   = 0;
        spawnScrap();
        SoundManager.playLoop("circuit.wav");
    }

    // ── Spawning ──────────────────────────────────────────────────────

    private void spawnScrap() {
        scrap     = new Point(rand.nextInt(850) + 20, rand.nextInt(450) + 20);
        scrapType = rand.nextInt(3) + 1;
    }

    /** Spawns a virus well away from the snake head so it's not instant death. */
    private Point safeVirusSpawn() {
        Point head = cable.get(0);
        Point p;
        int attempts = 0;
        do {
            p = new Point(rand.nextInt(850) + 20, rand.nextInt(450) + 20);
            attempts++;
        } while (p.distance(head) < 200 && attempts < 20);
        return p;
    }

    private void spawnVirusOverTime() {
        virusSpawnTimer++;
        if (virusSpawnTimer > 180) {
            virusSpawnTimer = 0;
            // Respawn / reposition both viruses
            if (virus1 == null) virus1 = safeVirusSpawn();
            else { virus1.x = safeVirusSpawn().x; virus1.y = safeVirusSpawn().y; }

            if (virus2 == null) virus2 = safeVirusSpawn();
            else { virus2.x = safeVirusSpawn().x; virus2.y = safeVirusSpawn().y; }
        }
    }

    // ── Chase movement ────────────────────────────────────────────────

    /**
     * Moves {@code virus} one step toward the snake head using a simple
     * axis-aligned chase — same pattern as the bat example but capped at
     * VIRUS_CHASE_SPEED pixels so it stays beatable.
     */
    private void chaseHead(Point virus) {
        if (virus == null) return;
        Point head = cable.get(0);

        if (virus.x > head.x) virus.x -= VIRUS_CHASE_SPEED;
        else if (virus.x < head.x) virus.x += VIRUS_CHASE_SPEED;

        if (virus.y > head.y) virus.y -= VIRUS_CHASE_SPEED;
        else if (virus.y < head.y) virus.y += VIRUS_CHASE_SPEED;

        // Keep in bounds
        virus.x = Math.max(0, Math.min(lastWidth  - 70, virus.x));
        virus.y = Math.max(0, Math.min(lastHeight - 80, virus.y));
    }

    private void moveViruses() {
        virusMoveTick++;
        if (virusMoveTick % VIRUS_MOVE_INTERVAL == 0) {
            chaseHead(virus1);
            chaseHead(virus2);
        }
    }

    // ── Update ────────────────────────────────────────────────────────

    @Override
    public void update() {
        if (started && !paused && !gameOver) {
            move();
            checkCollisions();
            moveViruses();
            spawnVirusOverTime();
        }
    }

    private void move() {
        Point head = cable.get(0);
        Point next = new Point(head.x, head.y);
        if (direction == 0) next.y -= step;
        if (direction == 1) next.x += step;
        if (direction == 2) next.y += step;
        if (direction == 3) next.x -= step;

        cable.add(0, next);
        while (cable.size() > targetLength) cable.remove(cable.size() - 1);

        if (next.x < 0 || next.x > lastWidth || next.y < 0 || next.y > lastHeight) {
            triggerGameOver();
        }
    }

    private void checkCollisions() {
        Point head = cable.get(0);

        // Scrap pickup
        if (scrap != null && head.distance(scrap) < 50) {
            targetLength += 3;
            score++;
            SoundManager.playOnce("collected.wav");
            spawnScrap();
            if (score >= 6) {
                SoundManager.stop();
                window.getGamePanel().switchGame(new TileMapTransitionMiniGame(window));
                return;
            }
            // Small chance to immediately spawn/reposition viruses
            if (rand.nextInt(100) < 30) {
                virus1 = safeVirusSpawn();
                virus2 = safeVirusSpawn();
            }
        }

        // Virus 1 hit
        if (virus1 != null) {
            Point centre1 = new Point(virus1.x + 35, virus1.y + 35);
            if (head.distance(centre1) < 35) triggerGameOver();
        }

        // Virus 2 hit
        if (virus2 != null) {
            Point centre2 = new Point(virus2.x + 35, virus2.y + 35);
            if (head.distance(centre2) < 35) triggerGameOver();
        }

        // Self-collision (skip first 6 segments of grace)
        for (int i = 6; i < cable.size(); i++) {
            if (head.distance((Point2D) cable.get(i)) < 10) triggerGameOver();
        }
    }

    private void triggerGameOver() {
        gameOver = true;
        SoundManager.stop();
    }

    // ── Draw ──────────────────────────────────────────────────────────

    @Override
    public void draw(Graphics2D g2, int width, int height) {
        lastWidth  = width;
        lastHeight = height;

        if (background != null) g2.drawImage(background, 0, 0, width, height, null);

        drawObjects(g2);
        drawCable(g2);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("Scraps: " + score + "/10   Length: " + cable.size() + "ft", 10, 30);

        if (!started)  drawStart(g2, width, height);
        if (paused)    drawPause(g2, width, height);
        if (gameOver)  drawGameOver(g2, width, height);
    }

    private void drawObjects(Graphics2D g2) {
        if (scrap != null)
            g2.drawImage(scrapImgs[scrapType - 1], scrap.x, scrap.y, 55, 55, null);
        if (virus1 != null)
            g2.drawImage(virusImg, virus1.x, virus1.y, 80, 80, null);
        if (virus2 != null)
            g2.drawImage(virusImg, virus2.x, virus2.y, 80, 80, null);
    }

    private void drawCable(Graphics2D g2) {
        for (int i = 1; i < cable.size(); i++) {
            Point p = cable.get(i);
            g2.setColor(new Color(90, 90, 90));
            g2.fillOval(p.x - 4, p.y - 4, 8, 8);
        }
        Point head = cable.get(0);
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(head.x, head.y);
        double angle = switch (direction) {
            case 0 -> -Math.PI / 2;
            case 2 ->  Math.PI / 2;
            case 3 ->  Math.PI;
            default -> 0.0;
        };
        g.rotate(angle);
        g.drawImage(headImg, -40, -40, 80, 80, null);
        g.dispose();
    }

    private void drawStart(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, w, h);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        String msg = "Press any arrow key to start";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    private void drawPause(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, w, h);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        String msg = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    private void drawGameOver(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, w, h);
        g2.setColor(Color.RED);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        String over = "GAME OVER";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(over, (w - fm.stringWidth(over)) / 2, h / 2 - 40);
        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        g2.setColor(Color.WHITE);
        String sub = "Cable length reached: " + cable.size() + "ft";
        fm = g2.getFontMetrics();
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, h / 2 + 20);
    }

    // ── Input ─────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;
        started = true;
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_UP    && direction != 2) direction = 0;
        if (k == KeyEvent.VK_RIGHT && direction != 3) direction = 1;
        if (k == KeyEvent.VK_DOWN  && direction != 0) direction = 2;
        if (k == KeyEvent.VK_LEFT  && direction != 1) direction = 3;
    }

    @Override public void keyReleased(KeyEvent e)    {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public JPanel getButtonPanel()         { return buttonPanel; }
}