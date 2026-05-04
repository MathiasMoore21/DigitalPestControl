import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class WhackAMoleMiniGame implements MiniGame {

    private static final int HUD_H = 60;
    private static final int[][] HOLE_POS = {
        {230, 340}, {670, 340},
        {450, 240}, {160, 450}, {740, 450},
        {130, 310}, {770, 310}, {450, 430},
        {360, 195}
    };
    private static final int[][] WAVES = {
        { 0, 2, 90, 15},
        { 5, 5, 75, 25},
        {15, 8, 60, 30},
        {30, 9, 45, 40},
    };
    private static final int BASE_SPAWN_MS  = 2000;
    private static final int FPS_MS         = 16;
    private static final int TICKS_PER_SEC  = 1000 / FPS_MS; // ~62

    private final GameWindow window;
    private final Image background;
    private int score = 0, lives = 3, missed = 0;
    private boolean gameOver = false, paused = false;
    private int countdown = 3;
    private boolean countingDown = true;
    private int redTintTicks = 0;
    private BufferedImage gameOverSnapshot = null;

    private final Animation2 animation2;
    private final List<Hole> holes = new ArrayList<>();
    private final List<Mole> moles = new ArrayList<>();
    private final Random rng = new Random();

    private int wave = 0;
    private final int difficulty;
    private final float[] spawnMultipliers = {1.0f, 0.85f, 0.7f, 0.55f};
    private static final String SND_HIT      = "hit.wav";
    private static final String SND_BOMB     = "bomb.wav";
    private static final String SND_COLLECT  = "collected.wav";
    private static final String SND_SUMMER   = "summer.wav";
    private static final String SND_SPRING   = "spring.wav";
    private static final String SND_AUTUMN   = "autumn.wav";
    private static final String SND_WINTER   = "winter.wav";
    // Tick counters replace the old separate Timers
    private int countdownTicks    = 0;
    private int spawnTicks        = 0;
    private int spawnIntervalTicks;

    // Stored from draw() so update() can use real dimensions
    private int lastWidth = 900, lastHeight = 600;

    private final JPanel buttonPanel;

    public WhackAMoleMiniGame(GameWindow window, String bgFile,
                               boolean snow, boolean leaves,
                               String soundFile, int difficulty) {
        this.window     = window;
        this.difficulty = difficulty;
        this.background = ImageManager.loadImage(bgFile);
        this.animation2  = new Animation2(snow, leaves);

        applyWave();
        computeSpawnInterval();
        SoundManager.playLoop(soundFile);

        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton pauseBtn = new JButton("Pause");
        JButton backBtn  = new JButton("Back");
        pauseBtn.addActionListener(e -> {
            if (!gameOver && !countingDown) paused = !paused;
        });
        backBtn.addActionListener(e -> window.getGamePanel().exitToMenu());
        buttonPanel.add(pauseBtn);
        buttonPanel.add(backBtn);
    }

    private void computeSpawnInterval() {
        int ms = Math.max(600, (int)(BASE_SPAWN_MS * spawnMultipliers[difficulty]) - wave * 300);
        spawnIntervalTicks = ms / FPS_MS;
    }

    @Override
    public void update() {
        if (gameOver || paused) return;

        // Countdown (replaces old countTimer that fired every 1000 ms)
        if (countingDown) {
            countdownTicks++;
            if (countdownTicks >= TICKS_PER_SEC) {
                countdownTicks = 0;
                countdown--;
                if (countdown < 0) {
                    countingDown = false;
                    spawnTicks = 0;
                }
            }
            return;
        }

        animation2.update(lastWidth, lastHeight);
        for (Hole h : holes) h.update();
        checkWaveUpgrade();

        List<Mole> dead = new ArrayList<>();
        for (Mole m : moles) {
            m.update();
            if (m.isDone()) {
                if (m.getHoleIndex() < holes.size())
                    holes.get(m.getHoleIndex()).setOccupied(false);
                if (!m.isHit() && m.getKind() == Mole.Kind.MOLE) {
                    missed++;
                    loseLife();
                }
                dead.add(m);
            }
        }
        moles.removeAll(dead);

        if (redTintTicks > 0) redTintTicks--;

        // Spawn timer (replaces old spawnTimer)
        spawnTicks++;
        if (spawnTicks >= spawnIntervalTicks) {
            spawnTicks = 0;
            spawnEntity();
        }
    }

    private void checkWaveUpgrade() {
        int newWave = 0;
        for (int i = WAVES.length - 1; i >= 0; i--) {
            if (score >= WAVES[i][0]) { newWave = i; break; }
        }
        if (newWave != wave) {
            wave = newWave;
            applyWave();
            computeSpawnInterval();
            spawnTicks = 0;
        }
    }

    private void applyWave() {
        int target = WAVES[wave][1];
        while (holes.size() < target && holes.size() < HOLE_POS.length) {
            int i = holes.size();
            holes.add(new Hole(HOLE_POS[i][0], HOLE_POS[i][1]));
        }
    }

    private void spawnEntity() {
        if (gameOver || paused) return;
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < holes.size(); i++) {
            if (!holes.get(i).isOccupied()) free.add(i);
        }
        if (free.isEmpty()) return;
        int idx = free.get(rng.nextInt(free.size()));
        Hole h = holes.get(idx);
        Mole.Kind kind = (rng.nextInt(100) < WAVES[wave][3]) ? Mole.Kind.BOMB : Mole.Kind.MOLE;
        moles.add(new Mole(idx, h.getCx(), h.getCy(), kind, WAVES[wave][2]));
        h.setOccupied(true);
    }

    private void handleClick(int mx, int my) {
        for (Mole m : moles) {
            if (m.contains(mx, my)) {
                if (m.getKind() == Mole.Kind.MOLE) {
                    score++;
                    SoundManager.playOnce(SND_HIT);
                    if (score >= 7 && !gameOver) {
                        SoundManager.stop();
                        window.getGamePanel().switchGame(new CircuitTransitionMiniGame(window));
                        return;
                    }
                } else {
                    loseLife();
                    SoundManager.playOnce(SND_BOMB);
                }
                m.hit();
                return;
            }
        }
    }

    private void loseLife() {
        lives--;
        redTintTicks = 45;
        if (lives <= 0) {
            lives = 0;
            gameOver = true;
            SoundManager.stop();
        }
    }

    @Override
    public void draw(Graphics2D g2, int width, int height) {
        lastWidth  = width;
        lastHeight = height;

        if (gameOver) { paintGameOver(g2, width, height); return; }

        if (background != null)
            g2.drawImage(background, 0, 0, width, height, null);
        animation2.draw(g2);
        for (Mole m : moles) m.draw(g2);
        for (Hole h : holes)  h.draw(g2);
        drawHUD(g2, width);

        if (redTintTicks > 0) {
            int alpha = (int)(80 * Math.min(1f, redTintTicks / 25f));
            g2.setColor(new Color(200, 0, 0, alpha));
            g2.fillRect(0, 0, width, height);
        }
        if (countingDown) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, width, height);
            String txt = countdown > 0 ? String.valueOf(countdown) : "GO!";
            g2.setFont(new Font("Arial", Font.BOLD, 120));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(txt, (width - fm.stringWidth(txt)) / 2, (height + fm.getAscent()) / 2 - 10);
        }
        if (paused) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, width, height);
            g2.setFont(new Font("Arial", Font.BOLD, 70));
            g2.setColor(Color.WHITE);
            String p = "PAUSED";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(p, (width - fm.stringWidth(p)) / 2, (height + fm.getAscent()) / 2 - 10);
        }
    }

    private void drawHUD(Graphics2D g2, int width) {
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRect(0, 0, width, HUD_H);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRect(0, HUD_H - 2, width, 2);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        int baseline = (HUD_H + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score, 18, baseline);
        StringBuilder sb = new StringBuilder("Lives: ");
        for (int i = 0; i < 3; i++) sb.append(i < lives ? "❤️ " : "🖤 ");
        String livesStr = sb.toString().trim();
        g2.setColor(Color.RED);
        g2.drawString(livesStr, (width - fm.stringWidth(livesStr)) / 2, baseline);
        g2.setColor(Color.WHITE);
        g2.drawString("Missed: " + missed, width - fm.stringWidth("Missed: " + missed) - 18, baseline);
    }

    private void paintGameOver(Graphics2D g2, int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (gameOverSnapshot == null) {
            BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = (Graphics2D) buf.getGraphics();
            if (background != null) bg.drawImage(background, 0, 0, w, h, null);
            for (Mole m  : moles) m.draw(bg);
            for (Hole ho : holes) ho.draw(bg);
            drawHUD(bg, w);
            bg.dispose();
            gameOverSnapshot = new Grayscale().applyEffect(buf);
        }
        g2.drawImage(gameOverSnapshot, 0, 0, null);
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, w, h);
        g2.setFont(new Font("Arial", Font.BOLD, 82));
        g2.setColor(new Color(220, 40, 40));
        FontMetrics fm = g2.getFontMetrics();
        String over = "GAME OVER";
        g2.drawString(over, (w - fm.stringWidth(over)) / 2, h / 2 - 55);
        int bx = w/2-185, by = h/2-10, bw = 370, bh = 120;
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(bx, by, bw, bh, 18, 18);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.drawRoundRect(bx, by, bw, bh, 18, 18);
        g2.setFont(new Font("Arial", Font.BOLD, 26));
        g2.setColor(Color.WHITE);
        fm = g2.getFontMetrics();
        String[] lines = {"Final Score: " + score, "Moles Missed: " + missed};
        int lineH = fm.getHeight() + 4, startY = by + 38;
        for (String line : lines) {
            g2.drawString(line, bx + (bw - fm.stringWidth(line)) / 2, startY);
            startY += lineH;
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(new Color(200, 200, 200, 200));
        String hint = "Press 'Back' to return to main menu";
        fm = g2.getFontMetrics();
        g2.drawString(hint, (w - fm.stringWidth(hint)) / 2, h / 2 + 160);
    }

    @Override public void mousePressed(MouseEvent e) {
        if (!gameOver && !paused && !countingDown) handleClick(e.getX(), e.getY());
    }
    @Override public void keyPressed(KeyEvent e)  {}
    @Override public void keyReleased(KeyEvent e) {}
    @Override public JPanel getButtonPanel()      { return buttonPanel; }
}