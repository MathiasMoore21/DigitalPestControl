import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

public class TileMapMiniGame implements MiniGame {

    private static final int TILE_SIZE       = 64;
    private static final int SCREEN_W        = 900;
    private static final int PLAYER_SCREEN_X = SCREEN_W / 2 - TILE_SIZE;
    private static final int SPAWN_AHEAD     = SCREEN_W + 20; // pixels ahead of player screen X
    private static final int REQUIRED_COINS  = 5;

    private static final int ZONE_SPLIT_RATIO = 2;
    private static final int COIN_SPAWN_TICKS = 40;
    private static final int BOMB_SPAWN_TICKS = 60;

    @SuppressWarnings("unused")
    private final GameWindow window;
    private TileMap map;
    private int currentMapNum = 1;

    // Each entry: { pixelsAheadOfPlayerScreenX, screenY }
    private final ArrayList<int[]> coins = new ArrayList<>();
    private final ArrayList<int[]> bombs = new ArrayList<>();

    private Animation coinAnim;
    private Animation bombAnim;

    private final Random rand = new Random();

    private int     score        = 0;
    private int     lives        = 5;
    private boolean gameOver     = false;
    private boolean won          = false;
    private boolean paused       = false;
    private int     redTintTicks = 0;
    private int     coinSpawnTicks = 0;
    private int     bombSpawnTicks = 0;

    private final JPanel buttonPanel;

    // ── Constructor ───────────────────────────────────────────────────

    public TileMapMiniGame(GameWindow window) {
        this.window = window;
        GamePanel panel = window.getGamePanel();

        TileMapManager manager = new TileMapManager(panel);
        try {
            map = manager.loadMap("maps/map1.txt");
            map.setEndless(true);
        } catch (IOException ex) {
            System.err.println("Could not load map: " + ex.getMessage());
            JOptionPane.showMessageDialog(panel, "Could not load map: " + ex.getMessage());
        }

        SoundManager.playLoop("tilemap_level" + currentMapNum + ".wav");

        // ── Build animations ─────────────────────────────────────────
        coinAnim = Animation.loadLooping("images/coin", 3, 220);
        if (coinAnim == null) {
            coinAnim = new Animation(true);
            coinAnim.addFrame(new ImageIcon("images/coin1.png").getImage(), 150);
            coinAnim.addFrame(new ImageIcon("images/coin2.png").getImage(), 150);
            coinAnim.addFrame(new ImageIcon("images/coin3.png").getImage(), 150);
            coinAnim.start();
        }

        bombAnim = Animation.loadLooping("images/bomb", 3, 260);
        if (bombAnim == null) {
            bombAnim = new Animation(true);
            bombAnim.addFrame(new ImageIcon("images/bomb1.png").getImage(), 200);
            bombAnim.addFrame(new ImageIcon("images/bomb2.png").getImage(), 200);
            bombAnim.addFrame(new ImageIcon("images/bomb3.png").getImage(), 200);
            bombAnim.start();
        }

        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        JButton pauseBtn = new JButton("Pause");
        JButton backBtn  = new JButton("Next");
        pauseBtn.setFocusable(false);
        backBtn.setFocusable(false);
        pauseBtn.addActionListener(e -> { if (!gameOver && !won) paused = !paused; });
        backBtn.addActionListener(e -> {
            if (won && currentMapNum < 3) {
                won = false;
                score = 0;
                loadNextMap();
            } else {
                panel.exitToMenu();
            }
        });
        buttonPanel.add(pauseBtn);
        buttonPanel.add(backBtn);
    }

    // ── Screen-space solid check ──────────────────────────────────────

    /**
     * Checks if any point in the given screen-space rectangle overlaps a solid tile.
     * This works regardless of where the map is scrolled because we convert directly
     * from screen coords to tile coords using the camera offsets.
     */
    private boolean rectOverlapsSolid(int screenX, int screenY, int width, int height) {
        int offsetX = computeOffsetX();
        int offsetY = map.getOffsetY();
        int stepsX  = Math.max(1, width  / 4);
        int stepsY  = Math.max(1, height / 4);
        for (int dx = 0; dx <= width; dx += stepsX) {
            for (int dy = 0; dy <= height; dy += stepsY) {
                int tileX = (screenX + dx - offsetX) / TILE_SIZE;
                int tileY = (screenY + dy - offsetY) / TILE_SIZE;
                if (map.getTile(tileX, tileY) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── Camera helper ─────────────────────────────────────────────────

    private int computeOffsetX() {
        int playerX    = Math.round(map.getPlayer().getX());
        int mapWidthPx = map.getWidthPixels();
        int offsetX    = SCREEN_W / 2 - playerX - TILE_SIZE;
        if (!map.isEndless()) {
            offsetX = Math.min(offsetX, 0);
            offsetX = Math.max(offsetX, SCREEN_W - mapWidthPx);
        }
        return offsetX;
    }

    // ── Spawn helpers ─────────────────────────────────────────────────

    /**
     * Finds a free screen Y in the upper half of the map for coin spawning.
     * Uses screen coordinates directly — no map-coordinate conversion needed.
     */
    private int findFreeCoinScreenY() {
        int mapHeight = map.getHeight();
        int offsetY   = map.getOffsetY();
        int upperRows = mapHeight / ZONE_SPLIT_RATIO;
        int spawnSX   = PLAYER_SCREEN_X + SPAWN_AHEAD;

        for (int attempt = 0; attempt < 15; attempt++) {
            int row     = rand.nextInt(upperRows);
            int screenY = row * TILE_SIZE + offsetY + 20;
            if (!rectOverlapsSolid(spawnSX, screenY, 32, 32)) {
                return screenY;
            }
        }
        return -1;
    }

    // ── update ────────────────────────────────────────────────────────

    @Override
    public void update() {
        if (gameOver || won || paused || map == null) return;

        coinAnim.update();
        bombAnim.update();

        map.moveRight();
        map.updatePlayerOnly();

        int offsetY = map.getOffsetY();
        int mapH    = map.getHeight();

        // ── Spawn coin ────────────────────────────────────────────────
        coinSpawnTicks++;
        if (coinSpawnTicks >= COIN_SPAWN_TICKS) {
            coinSpawnTicks = 0;
            int screenY = findFreeCoinScreenY();
            if (screenY >= 0) {
                // Store as distance ahead of player screen X, plus absolute screen Y
                coins.add(new int[]{ SPAWN_AHEAD, screenY });
            }
        }

        // ── Spawn bomb ────────────────────────────────────────────────
        bombSpawnTicks++;
        if (bombSpawnTicks >= BOMB_SPAWN_TICKS) {
            bombSpawnTicks = 0;
            int groundSY = (mapH - 1) * TILE_SIZE + offsetY;
            int bombSY   = groundSY - 38;
            bombs.add(new int[]{ SPAWN_AHEAD, bombSY });
        }

        // ── Move / check coins ────────────────────────────────────────
        // c[0] = pixels ahead of PLAYER_SCREEN_X; decrement each frame to scroll left
        Iterator<int[]> coinIt = coins.iterator();
        while (coinIt.hasNext()) {
            int[] c       = coinIt.next();
            c[0]--;                               // approach the player at 1px/frame
            int   screenX = PLAYER_SCREEN_X + c[0];
            int   screenY = c[1];

            if (screenX < -40) {
                coinIt.remove();
                continue;
            }
            if (rectOverlapsSolid(screenX, screenY, 32, 32)) {
                coinIt.remove();
                continue;
            }
            if (hitsPlayer(screenX, screenY, 32, 32)) {
                score++;
                SoundManager.playOnce("collected.wav");
                coinIt.remove();
                if (score >= REQUIRED_COINS) {
                    won = true;
                    SoundManager.stop();
                    return;
                }
            }
        }

        // ── Move / check bombs ────────────────────────────────────────
        Iterator<int[]> bombIt = bombs.iterator();
        while (bombIt.hasNext()) {
            int[] b       = bombIt.next();
            b[0]--;
            int   screenX = PLAYER_SCREEN_X + b[0];
            int   screenY = b[1];

            if (screenX < -60) {
                bombIt.remove();
                continue;
            }
            if (rectOverlapsSolid(screenX, screenY, 40, 36)) {
                bombIt.remove();
                continue;
            }
            if (hitsPlayer(screenX, screenY, 40, 36)) {
                bombIt.remove();
                loseLife();
                if (gameOver) return;
            }
        }

        if (redTintTicks > 0) redTintTicks--;
    }

    // ── Collision ─────────────────────────────────────────────────────

    private boolean hitsPlayer(int objSX, int objSY, int objW, int objH) {
        Player p  = map.getPlayer();
        int    px = PLAYER_SCREEN_X;
        int    py = p.getY();
        int    pw = p.getWidth();
        int    ph = p.getHeight();
        return objSX < px + pw && objSX + objW > px
            && objSY < py + ph && objSY + objH > py;
    }

    private void loseLife() {
        lives--;
        redTintTicks = 45;
        SoundManager.playOnce("bomb.wav");
        if (lives <= 0) { lives = 0; gameOver = true; SoundManager.stop(); }
    }

    // ── draw ──────────────────────────────────────────────────────────

    @Override
    public void draw(Graphics2D g2, int width, int height) {
        if (map == null) return;

        map.draw(g2);

        Image coinFrame = coinAnim.getImage();
        Image bombFrame = bombAnim.getImage();

        // ── Draw coins ────────────────────────────────────────────────
        for (int[] c : coins) {
            int sx = PLAYER_SCREEN_X + c[0];
            int sy = c[1];
            if (coinFrame != null)
                g2.drawImage(coinFrame, sx, sy, 32, 32, null);
        }

        // ── Draw bombs ────────────────────────────────────────────────
        for (int[] b : bombs) {
            int sx = PLAYER_SCREEN_X + b[0];
            int sy = b[1];
            if (bombFrame != null)
                g2.drawImage(bombFrame, sx, sy, 40, 36, null);
        }

        drawHUD(g2, width);

        if (redTintTicks > 0) {
            int alpha = (int)(80 * Math.min(1f, redTintTicks / 25f));
            g2.setColor(new Color(200, 0, 0, alpha));
            g2.fillRect(0, 0, width, height);
        }

        if (paused)   drawPause(g2, width, height);
        if (won)      drawWin(g2, width, height);
        if (gameOver) drawGameOver(g2, width, height);
    }

    // ── Draw helpers ──────────────────────────────────────────────────

    private void drawHUD(Graphics2D g2, int width) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, width, 50);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRect(0, 48, width, 2);
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        int baseline = 33;
        g2.setColor(new Color(255, 220, 50));
        g2.drawString("Coins: " + score + " / " + REQUIRED_COINS, 18, baseline);
        StringBuilder sb = new StringBuilder("Lives: ");
        for (int i = 0; i < lives; i++) sb.append("♥ ");
        String livesStr = sb.toString().trim();
        g2.setColor(new Color(255, 80, 80));
        g2.drawString(livesStr, width - fm.stringWidth(livesStr) - 18, baseline);
    }

    private void drawPause(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, w, h);
        g2.setFont(new Font("Arial", Font.BOLD, 70));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        String msg = "PAUSED";
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    private void drawWin(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, w, h);
        g2.setFont(new Font("Arial", Font.BOLD, 72));
        g2.setColor(new Color(50, 220, 50));
        FontMetrics fm = g2.getFontMetrics();
        String msg = "LEVEL COMPLETE!";
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 - 40);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(new Color(255, 220, 50));
        fm = g2.getFontMetrics();
        String sub = REQUIRED_COINS + " coins collected!";
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, h / 2 + 30);
    }

    private void drawGameOver(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, w, h);
        g2.setFont(new Font("Arial", Font.BOLD, 72));
        g2.setColor(new Color(220, 40, 40));
        FontMetrics fm = g2.getFontMetrics();
        String over = "GAME OVER";
        g2.drawString(over, (w - fm.stringWidth(over)) / 2, h / 2 - 50);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.setColor(new Color(255, 220, 50));
        fm = g2.getFontMetrics();
        String sc = "Coins collected: " + score + " / " + REQUIRED_COINS;
        g2.drawString(sc, (w - fm.stringWidth(sc)) / 2, h / 2 + 20);
        g2.setFont(new Font("Arial", Font.PLAIN, 20));
        g2.setColor(new Color(180, 180, 180));
        fm = g2.getFontMetrics();
        String hint = "Press 'Back' to return to menu";
        g2.drawString(hint, (w - fm.stringWidth(hint)) / 2, h / 2 + 75);
    }

    // ── Map loading ───────────────────────────────────────────────────

    private void loadNextMap() {
        currentMapNum++;
        GamePanel panel = window.getGamePanel();
        TileMapManager manager = new TileMapManager(panel);
        try {
            String mapFile = "maps/map" + currentMapNum + ".txt";
            map = manager.loadMap(mapFile);
            map.setEndless(true);
            coins.clear();
            bombs.clear();
            SoundManager.playLoop("tilemap_level" + currentMapNum + ".wav");
        } catch (IOException ex) {
            System.err.println("Could not load map " + currentMapNum + ": " + ex.getMessage());
            won = true;
            SoundManager.stop();
        }
    }

    // ── Input ─────────────────────────────────────────────────────────

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || won || paused || map == null) return;
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_SPACE) map.jump();
    }

    @Override public void keyReleased(KeyEvent e)    {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public JPanel getButtonPanel()         { return buttonPanel; }
}