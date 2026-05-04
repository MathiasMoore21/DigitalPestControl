import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Iterator;
import java.util.LinkedList;

public class TileMap {

    private static final int TILE_SIZE = 64;

    private Image[][] tiles;
    private int screenWidth, screenHeight;
    private int mapWidth, mapHeight;
    private int offsetY;
    private boolean endless;

    private LinkedList<Object> sprites;
    private Player player;

    BackgroundManager bgManager;

    private GamePanel panel;
    private Dimension dimension;

    public TileMap(GamePanel panel, int width, int height) {
        this.panel = panel;
        dimension = panel.getSize();

        screenWidth = dimension.width;
        screenHeight = dimension.height;

        System.out.println("Width: " + screenWidth);
        System.out.println("Height: " + screenHeight);

        mapWidth = width;
        mapHeight = height;

        offsetY = screenHeight - tilesToPixels(mapHeight);
        System.out.println("offsetY: " + offsetY);

        bgManager = new BackgroundManager(panel, 12);

        tiles = new Image[mapWidth][mapHeight];
        player = new Player(panel, this, bgManager);

        sprites = new LinkedList<>();

        int playerHeight = player.getHeight();

        int x, y;
        x = (dimension.width / 2) + TILE_SIZE;
        y = dimension.height - (TILE_SIZE + playerHeight);

        player.setX(x);
        player.setY(y);
        ensureSafePlayerStart();

        System.out.println("Player coordinates: " + player.getX() + "," + player.getY());
    }

    private void ensureSafePlayerStart() {
        if (!playerOverlapsSolid(player.getX(), player.getY())) {
            return;
        }

        int step = TILE_SIZE;
        int startX = player.getX();
        int startY = player.getY();
        int mapWidthPx = getWidthPixels();

        for (int dx = 0; dx <= mapWidthPx; dx += step) {
            for (int dir = -1; dir <= 1; dir += 2) {
                int testX = startX + dx * dir;
                if (testX < 0 || testX + player.getWidth() > mapWidthPx) continue;
                if (!playerOverlapsSolid(testX, startY)) {
                    player.setX(testX);
                    return;
                }
            }
        }

        for (int dy = 0; dy <= TILE_SIZE * 2; dy += step) {
            int testY = startY - dy;
            if (testY < 0) break;
            if (!playerOverlapsSolid(startX, testY)) {
                player.setY(testY);
                return;
            }
        }
    }

    private boolean playerOverlapsSolid(int testX, int testY) {
        int playerWidth = player.getWidth();
        int playerHeight = player.getHeight();
        for (int px = testX; px < testX + playerWidth; px += 16) {
            for (int py = testY; py < testY + playerHeight; py += 16) {
                int tileX = pixelsToTiles(px);
                int tileY = pixelsToTiles(py - offsetY);
                if (getTile(tileX, tileY) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getWidthPixels() {
        return tilesToPixels(mapWidth);
    }

    public int getWidth() {
        return mapWidth;
    }

    public int getHeight() {
        return mapHeight;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setEndless(boolean endless) {
        this.endless = endless;
    }

    public boolean isEndless() {
        return endless;
    }

    public Image getTile(int x, int y) {
        if (y < 0 || y >= mapHeight) {
            return null;
        }
        if (endless && mapWidth > 0) {
            x = ((x % mapWidth) + mapWidth) % mapWidth;
        }
        if (x < 0 || x >= mapWidth) {
            return null;
        }
        return tiles[x][y];
    }

    public void setTile(int x, int y, Image tile) {
        tiles[x][y] = tile;
    }

    public Iterator<Object> getSprites() {
        return sprites.iterator();
    }

    public static int pixelsToTiles(float pixels) {
        return pixelsToTiles(Math.round(pixels));
    }

    public static int pixelsToTiles(int pixels) {
        return (int) Math.floor((float) pixels / TILE_SIZE);
    }

    public static int tilesToPixels(int numTiles) {
        return numTiles * TILE_SIZE;
    }

    public void draw(Graphics2D g2) {
        int mapWidthPixels = tilesToPixels(mapWidth);

        int offsetX = screenWidth / 2 -
                Math.round(player.getX()) - TILE_SIZE;
        if (!endless) {
            offsetX = Math.min(offsetX, 0);
            offsetX = Math.max(offsetX, screenWidth - mapWidthPixels);
        }

        bgManager.draw(g2);

        int firstTileX = pixelsToTiles(-offsetX);
        int lastTileX = firstTileX + pixelsToTiles(screenWidth) + 1;
        for (int y = 0; y < mapHeight; y++) {
            for (int x = firstTileX; x <= lastTileX; x++) {
                Image image = getTile(x, y);
                if (image != null) {
                    int drawX = tilesToPixels(x) + offsetX;
                    int drawY = tilesToPixels(y) + offsetY;
                    g2.drawImage(image, drawX, drawY, TILE_SIZE, TILE_SIZE, null);
                }
            }
        }

        g2.drawImage(player.getImage(),
                Math.round(player.getX()) + offsetX,
                Math.round(player.getY()),
                player.getWidth(), player.getHeight(),
                null);
    }

    public void moveLeft() {
        System.out.println("Going left. x = " + player.getX() + " y = " + player.getY());
        player.move(1);
    }

    public void moveRight() {
        System.out.println("Going right. x = " + player.getX() + " y = " + player.getY());
        player.move(2);
    }

    public void jump() {
        System.out.println("Jumping. x = " + player.getX() + " y = " + player.getY());
        player.move(3);
    }

    public void update() {
        player.update();
    }

    // --- Added for runner mode ---

    /** Exposes the player so the runner can check screen-space Y for collision. */
    public Player getPlayer() {
        return player;
    }

    /**
     * Updates only the player physics.
     * Skips any additional level-ending logic during the endless runner.
     */
    public void updatePlayerOnly() {
        player.update();
    }
}
