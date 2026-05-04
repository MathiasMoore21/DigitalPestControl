import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;

public class CircuitCleanerGamePanel  extends JPanel implements ActionListener, KeyListener {

    private final GameWindow window;

    private final Image background;
    private final Image headImg;
    private final Image[] scrapImgs = new Image[3];
    private final Image virusImg;

    private final Timer timer;

    private JButton pauseBtn, restartBtn, backBtn;

    private final ArrayList<Point> cable = new ArrayList<>();

    private Point scrap;
    private Point virus;

    private int scrapType = 1;

    private int direction = 1;
    private boolean started = false;
    private boolean paused = false;
    private boolean gameOver = false;

    private final int step = 4;                
    private int targetLength = 6;

    private final Random rand = new Random();

    private int virusMoveTick = 0;
    private int virusSpawnTimer = 0;

    public CircuitCleanerGamePanel(GameWindow window, String bgFile) {

        this.window = window;
        this.background = ImageManager.loadImage(bgFile);

        setLayout(null);
        setFocusable(true);
        addKeyListener(this);

        setPreferredSize(new Dimension(900, 600));

        // ---------------- IMAGES ----------------
        headImg = new ImageIcon("cable.png").getImage();

        scrapImgs[0] = new ImageIcon("scrap1.png").getImage();
        scrapImgs[1] = new ImageIcon("scrap2.png").getImage();
        scrapImgs[2] = new ImageIcon("scrap3.png").getImage();

        virusImg = new ImageIcon("virus.png").getImage();

        // ---------------- START POSITION ----------------
        initCable();

        spawnScrap();

        // ---------------- BACKGROUND SOUND ----------------
        SoundManager.playLoop("circuit.wav");

        timer = new Timer(100, this);
        timer.start();

        createButtons();
    }

    private void initCable() {
        cable.clear();
        int x = 450, y = 300;
        for (int i = 0; i < 4; i++) {
            cable.add(new Point(x - (i * step), y));
        }
    }

    // ---------------- BUTTONS ----------------
    private void createButtons() {

        pauseBtn = new JButton("Pause");
        restartBtn = new JButton("Restart");
        backBtn = new JButton("Next");

        pauseBtn.setBounds(300, 520, 100, 30);
        restartBtn.setBounds(410, 520, 100, 30);
        backBtn.setBounds(520, 520, 100, 30);

        pauseBtn.setFocusable(false);
        restartBtn.setFocusable(false);
        backBtn.setFocusable(false);

        add(pauseBtn);
        add(restartBtn);
        add(backBtn);

        pauseBtn.addActionListener(e -> {
            if (!started || gameOver) return;
            paused = !paused;
        });

        restartBtn.addActionListener(e -> restartGame());

        backBtn.addActionListener(e -> {
            timer.stop();
            SoundManager.stop();
            window.returnToMenu();
        });
    }

    // ---------------- RESTART ----------------
    private void restartGame() {
        timer.stop();
        SoundManager.stop();                 
        initCable();
        direction = 1;
        targetLength = 6;
        started = false;
        paused = false;
        gameOver = false;
        virus = null;
        virusSpawnTimer = 0;
        virusMoveTick = 0;
        spawnScrap();
        SoundManager.playLoop("circuit.wav"); 
        timer.start();
    }

    // ---------------- SCRAP (RANDOM IMAGE EACH SPAWN) ----------------
    private void spawnScrap() {
        scrap = new Point(
                rand.nextInt(850) + 20,
                rand.nextInt(450) + 20
        );
        scrapType = rand.nextInt(3) + 1;
    }

    // ---------------- VIRUS SPAWN SYSTEM ----------------
    private void spawnVirus() {
        virus = new Point(
                rand.nextInt(850) + 20,
                rand.nextInt(450) + 20
        );
    }

    private void spawnVirusOverTime() {
        virusSpawnTimer++;
        if (virusSpawnTimer > 180) {
            virusSpawnTimer = 0;
            if (virus == null) {
                spawnVirus();
            } else {
                virus.x = rand.nextInt(850) + 20;
                virus.y = rand.nextInt(450) + 20;
            }
        }
    }

    private void moveVirus() {
        if (virus == null) return;
        virusMoveTick++;
        if (virusMoveTick % 20 == 0) {
            int d = rand.nextInt(4);
            if (d == 0) virus.y -= 10;
            if (d == 1) virus.x += 10;
            if (d == 2) virus.y += 10;
            if (d == 3) virus.x -= 10;
            virus.x = Math.max(0, Math.min(getWidth() - 60, virus.x));
            virus.y = Math.max(0, Math.min(getHeight() - 80, virus.y));
        }
    }

    // ---------------- GAME LOOP ----------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!started || paused || gameOver) {
            repaint();
            return;
        }
        move();
        checkCollisions();
        moveVirus();
        spawnVirusOverTime();
        repaint();
    }

    // ---------------- MOVE ----------------
    private void move() {
        Point head = cable.get(0);
        Point newHead = new Point(head.x, head.y);
        if (direction == 0) newHead.y -= step;
        if (direction == 1) newHead.x += step;
        if (direction == 2) newHead.y += step;
        if (direction == 3) newHead.x -= step;
        cable.add(0, newHead);
        while (cable.size() > targetLength) {
            cable.remove(cable.size() - 1);
        }
        if (newHead.x < 0 || newHead.x > getWidth()
                || newHead.y < 0 || newHead.y > getHeight()) {
            gameOver = true;
            timer.stop();
            SoundManager.stop();
        }
    }

    // ---------------- COLLISIONS ----------------
    private void checkCollisions() {
        Point head = cable.get(0);

        // SCRAP 
        if (scrap != null && head.distance(scrap) < 40) {
            targetLength += 3;
            SoundManager.playOnce("collected.wav");
            spawnScrap();
            if (rand.nextInt(100) < 30) {
                spawnVirus();
            }
        }

        // VIRUS
        if (virus != null) {
            int virusCenterX = virus.x + 35;   
            int virusCenterY = virus.y + 35;
            Point virusCenter = new Point(virusCenterX, virusCenterY);
            if (head.distance(virusCenter) < 35) {
                gameOver = true;
                timer.stop();
                SoundManager.stop();
            }
        }

        // Self collision
        for (int i = 6; i < cable.size(); i++) {
            if (head.distance(cable.get(i)) < 10) {
                gameOver = true;
                timer.stop();
                SoundManager.stop();
            }
        }
    }

    // ---------------- DRAW ----------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (background != null)
            g2.drawImage(background, 0, 0, getWidth(), getHeight(), this);

        drawObjects(g2);
        drawCable(g2);

        // Length counter (top‑left)
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("Length: " + cable.size() + "ft", 10, 30);

        if (!started) drawStart(g2);
        if (paused) drawPause(g2);
        if (gameOver) drawGameOver(g2);
    }

    private void drawObjects(Graphics2D g2) {
        if (scrap != null) {
            Image img = scrapImgs[scrapType - 1];
            g2.drawImage(img, scrap.x, scrap.y, 45, 45, this);
        }
        if (virus != null) {
            g2.drawImage(virusImg, virus.x, virus.y, 70, 70, this);
        }
    }

    private void drawCable(Graphics2D g2) {
        for (int i = 1; i < cable.size(); i++) {
            Point p = cable.get(i);
            g2.setColor(new Color(90,90,90));
            g2.fillOval(p.x - 3, p.y - 3, 6, 6);
        }
        Point head = cable.get(0);
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(head.x, head.y);
        double angle = 0;
        if (direction == 0) angle = -Math.PI / 2;
        if (direction == 1) angle = 0;
        if (direction == 2) angle = Math.PI / 2;
        if (direction == 3) angle = Math.PI;
        g.rotate(angle);
        g.drawImage(headImg, -30, -30, 60, 60, this);
        g.dispose();
    }

    private void drawStart(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        String msg = "Press any key to start";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth()-fm.stringWidth(msg))/2, getHeight()/2);
    }

    private void drawPause(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,150));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        g2.drawString("PAUSED", 320, 300);
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,getWidth(),getHeight());
        g2.setColor(Color.RED);
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        g2.drawString("GAME OVER", 250, 300);
        g2.setFont(new Font("Arial", Font.PLAIN, 30));
        g2.setColor(Color.WHITE);
        g2.drawString("Cable length reached: " + cable.size() + "ft", 280, 380);
    }

    // ---------------- INPUT ----------------
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;
        started = true;
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_UP && direction != 2) direction = 0;
        if (k == KeyEvent.VK_RIGHT && direction != 3) direction = 1;
        if (k == KeyEvent.VK_DOWN && direction != 0) direction = 2;
        if (k == KeyEvent.VK_LEFT && direction != 1) direction = 3;
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}