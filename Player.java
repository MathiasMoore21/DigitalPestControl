import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;

public class Player {

    private static final int DX = 4;
    private static final int DY = 32;
    private static final int TILE_SIZE = 64;
    private static final int PLAYER_WIDTH = 48;
    private static final int PLAYER_HEIGHT = 48;

    private final JPanel panel;
    private final TileMap tileMap;
    private final BackgroundManager bgManager;

    private int x;
    private int y;

    private Animation rightAnim;
    private Animation currentAnim;
    private Image fallbackRightImage;

    private boolean jumping;
    private int timeElapsed;
    private int startY;

    private boolean goingUp;
    private boolean goingDown;

    private boolean inAir;
    private int initialVelocity;

    public Player(JPanel panel, TileMap t, BackgroundManager b) {
        this.panel = panel;
        this.tileMap = t;
        this.bgManager = b;

        goingUp = false;
        goingDown = false;
        inAir = false;

        fallbackRightImage = ImageManager.loadImage("images/playerRight.gif");

        rightAnim = Animation.loadLooping("images/playerRight", 3, 220);
        if (rightAnim == null) {
            rightAnim = new Animation(false);
            if (fallbackRightImage != null) {
                rightAnim.addFrame(fallbackRightImage, 1000);
            }
            rightAnim.start();
        }

        currentAnim = rightAnim;
    }

    public Point collidesWithTile(int newX, int newY) {
        int offsetY = tileMap.getOffsetY();
        int xTile = tileMap.pixelsToTiles(newX);
        int yTile = tileMap.pixelsToTiles(newY - offsetY);

        if (tileMap.getTile(xTile, yTile) != null) {
            return new Point(xTile, yTile);
        }
        return null;
    }

    public Point collidesWithTileDown(int newX, int newY) {
        int playerWidth = getWidth();
        int playerHeight = getHeight();
        int offsetY = tileMap.getOffsetY();
        int xTile = tileMap.pixelsToTiles(newX);
        int yTileFrom = tileMap.pixelsToTiles(y - offsetY);
        int yTileTo = tileMap.pixelsToTiles(newY - offsetY + playerHeight);

        for (int yTile = yTileFrom; yTile <= yTileTo; yTile++) {
            if (tileMap.getTile(xTile, yTile) != null) {
                return new Point(xTile, yTile);
            } else {
                if (tileMap.getTile(xTile + 1, yTile) != null) {
                    int leftSide = (xTile + 1) * TILE_SIZE;
                    if (newX + playerWidth > leftSide) {
                        return new Point(xTile + 1, yTile);
                    }
                }
            }
        }
        return null;
    }

    public Point collidesWithTileUp(int newX, int newY) {
        int playerWidth = getWidth();
        int offsetY = tileMap.getOffsetY();
        int xTile = tileMap.pixelsToTiles(newX);

        int yTileFrom = tileMap.pixelsToTiles(y - offsetY);
        int yTileTo = tileMap.pixelsToTiles(newY - offsetY);

        for (int yTile = yTileFrom; yTile >= yTileTo; yTile--) {
            if (tileMap.getTile(xTile, yTile) != null) {
                return new Point(xTile, yTile);
            } else {
                if (tileMap.getTile(xTile + 1, yTile) != null) {
                    int leftSide = (xTile + 1) * TILE_SIZE;
                    if (newX + playerWidth > leftSide) {
                        return new Point(xTile + 1, yTile);
                    }
                }
            }
        }
        return null;
    }

    public synchronized void move(int direction) {
        int newX = x;
        Point tilePos = null;

        if (!panel.isVisible()) return;

        if (direction == 1) {
            if (!currentAnim.isStillActive()) currentAnim.start();
            newX = x - DX;
            if (newX < 0 && !tileMap.isEndless()) {
                x = 0;
                return;
            }
            tilePos = collidesWithTile(newX, y);
        } else if (direction == 2) {
            currentAnim = rightAnim;
            if (!currentAnim.isStillActive()) currentAnim.start();
            int playerWidth = getWidth();
            newX = x + DX;
            int tileMapWidth = tileMap.getWidthPixels();
            if (!tileMap.isEndless() && newX + playerWidth >= tileMapWidth) {
                x = tileMapWidth - playerWidth;
                return;
            }
            tilePos = collidesWithTile(newX + playerWidth, y);
        } else if (direction == 3 && !jumping) {
            jump();
            return;
        }

        if (tilePos != null) {
            if (direction == 1) {
                System.out.println(": Collision going left");
                x = ((int) tilePos.getX() + 1) * TILE_SIZE;
            } else if (direction == 2) {
                System.out.println(": Collision going right");
                int playerWidth = getWidth();
                x = ((int) tilePos.getX()) * TILE_SIZE - playerWidth;
            }
        } else {
            if (direction == 1) {
                x = newX;
                bgManager.moveLeft();
            } else if (direction == 2) {
                x = newX;
                bgManager.moveRight();
            }
            if (isInAir()) {
                System.out.println("In the air. Starting to fall.");
                if (direction == 1) {
                    int playerWidth = getWidth();
                    x = x - playerWidth + DX;
                }
                fall();
            }
        }
    }

    public boolean isInAir() {
        if (!jumping && !inAir) {
            int playerHeight = getHeight();
            Point tilePos = collidesWithTile(x, y + playerHeight + 1);
            return tilePos == null;
        }
        return false;
    }

    private void fall() {
        jumping = false;
        inAir = true;
        timeElapsed = 0;
        goingUp = false;
        goingDown = true;
        startY = y;
        initialVelocity = 0;
    }

    public void jump() {
        if (!panel.isVisible()) return;
        jumping = true;
        timeElapsed = 0;
        goingUp = true;
        goingDown = false;
        startY = y;
        initialVelocity = 70;
    }

    public void update() {
        if (currentAnim != null) currentAnim.update();

        if (jumping || inAir) {
            int distance = (int) (initialVelocity * timeElapsed - 4.9 * timeElapsed * timeElapsed);
            int newY = startY - distance;
            timeElapsed++;

            if (newY > y && goingUp) {
                goingUp = false;
                goingDown = true;
            }

            if (goingUp) {
                Point tilePos = collidesWithTileUp(x, newY);
                if (tilePos != null) {
                    System.out.println("Jumping: Collision Going Up!");
                    int offsetY = tileMap.getOffsetY();
                    int topTileY = ((int) tilePos.getY()) * TILE_SIZE + offsetY;
                    int bottomTileY = topTileY + TILE_SIZE;
                    y = bottomTileY;
                    fall();
                } else {
                    y = newY;
                    System.out.println("Jumping: No collision.");
                }
            } else if (goingDown) {
                Point tilePos = collidesWithTileDown(x, newY);
                if (tilePos != null) {
                    System.out.println("Jumping: Collision Going Down!");
                    int playerHeight = getHeight();
                    goingDown = false;
                    int offsetY = tileMap.getOffsetY();
                    int topTileY = ((int) tilePos.getY()) * TILE_SIZE + offsetY;
                    y = topTileY - playerHeight;
                    jumping = false;
                    inAir = false;
                } else {
                    y = newY;
                    System.out.println("Jumping: No collision.");
                }
            }
        }
    }

    public void moveUp() {
        if (!panel.isVisible()) return;
        y -= DY;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Image getImage() {
        if (currentAnim != null) {
            Image img = currentAnim.getImage();
            if (img != null) return img;
        }
        return fallbackRightImage;
    }

    public int getWidth() {
        return PLAYER_WIDTH;
    }

    public int getHeight() {
        return PLAYER_HEIGHT;
    }

    public Rectangle2D.Double getBoundingRectangle() {
        return new Rectangle2D.Double(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);
    }
}
