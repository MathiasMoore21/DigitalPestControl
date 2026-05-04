import java.awt.*;

/**
 * Mole/bomb entity for the whack-a-mole game.
 */
public class Mole {

    public enum Kind  { MOLE, BOMB }
    public enum State { RISING, SHOWING, FALLING, DONE }

    public static final int W = 72;   // sprite draw width  (px)
    public static final int H = 72;   // sprite draw height (px)
    private static final int SPEED = 3;  // pixels per game-tick

    // ── identity ───────────────────────────────────────────────────────────
    private final int   holeIndex;   // which Hole in GamePanel's list
    private final int   cx;          // horizontal centre (mirrors hole cx)
    private final int   holeY;       // hole centre y  – used for clip boundary
    private final Kind  kind;
    private final Image img;
    private final Animation animation;

    // ── animation ──────────────────────────────────────────────────────────
    private float       y;           // current top-left y of the sprite
    private final float topY;        // y when fully risen    (holeY - H)
    private final float hiddenY;     // y when fully hidden   (holeY)

    private State   state = State.RISING;
    private int     showTicks;       // countdown while SHOWING
    private boolean hit   = false;   // set true when player clicks this entity

    // ── wiggle / shake offsets ─────────────────────────────────────────────
    private int animCounter = 0;      // increases every tick while SHOWING
    private float offsetX = 0;        // current horizontal shift for drawing

    // ── constructor ────────────────────────────────────────────────────────
    public Mole(int holeIndex, int cx, int holeY, Kind kind, int showTicks) {
        this.holeIndex = holeIndex;
        this.cx        = cx;
        this.holeY     = holeY;
        this.kind      = kind;
        this.showTicks = showTicks;

        this.hiddenY = holeY;        // start hidden below hole centre
        this.topY    = holeY - H;    // rise until sprite top reaches here
        this.y       = hiddenY;

        String base = kind == Kind.BOMB ? "images/bomb" : "images/mole";
        Animation anim = Animation.loadLooping(base, 3, 420);
        if (anim != null) {
            this.animation = anim;
            this.img = null;
        } else {
            this.animation = null;
            this.img = ImageManager.loadImage(kind == Kind.BOMB ? "bomb.png" : "mole.png");
        }
    }

    // ── per-tick update ────────────────────────────────────────────────────
    public void update() {
        switch (state) {
            case RISING:
                y -= SPEED;
                if (y <= topY) { y = topY; state = State.SHOWING; }
                break;

            case SHOWING:
                if (--showTicks <= 0) state = State.FALLING;
                if (animation != null) animation.update();
                animCounter++;
                float amplitude = (kind == Kind.BOMB) ? 5.0f : 2.0f;
                offsetX = (float) Math.sin(animCounter * 0.25) * amplitude;
                break;

            case FALLING:
                y += SPEED;
                if (y >= hiddenY) { y = hiddenY; state = State.DONE; }
                break;

            default:
                break;
        }
    }

    // ── rendering ──────────────────────────────────────────────────────────
    /**
     * Draws the sprite with a horizontal offset (wiggle/shake) applied.
     * The clip is still relative to the original holeY to keep the illusion.
     */
    public void draw(Graphics g) {
        if (state == State.DONE) return;

        int dx = cx - W / 2 + (int) offsetX;
        int dy = (int) y;

        Image frame = animation != null ? animation.getImage() : img;
        Shape old = g.getClip();
        g.setClip(new Rectangle(0, 0, 10_000, holeY));
        if (frame != null) {
            g.drawImage(frame, dx, dy, W, H, null);
        }
        g.setClip(old);
    }

    // ── hit-test (accounts for animation offset) ───────────────────────────
    /** True when (mx, my) overlaps the visible sprite (only during SHOWING). */
    public boolean contains(int mx, int my) {
        if (state == State.DONE || state == State.RISING) return false;
        int dx = cx - W / 2 + (int) offsetX;
        int dy = (int) y;
        return mx >= dx && mx < dx + W
            && my >= dy && my < dy + H;
    }

    // ── actions ────────────────────────────────────────────────────────────
    public void hit() {
        hit   = true;
        state = State.FALLING;
    }

    // ── accessors ──────────────────────────────────────────────────────────
    public boolean isHit()        { return hit; }
    public boolean isDone()       { return state == State.DONE; }
    public Kind    getKind()      { return kind; }
    public int     getHoleIndex() { return holeIndex; }
}