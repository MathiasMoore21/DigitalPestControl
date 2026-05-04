import java.awt.*;

/**
 * A hole on the play field.
 * Always drawn AFTER moles so the rim naturally frames the emergence point.
 */
public class Hole {

    public static final int W = 70;  // draw width  (px)
    public static final int H = 45;  // draw height (px)

    private final int   cx;          // centre x in panel coords
    private final int   cy;          // centre y in panel coords
    private final Image img;
    private final Animation animation;
    private boolean     occupied;    // true while a mole/bomb is active here

    public Hole(int cx, int cy) {
        this.cx  = cx;
        this.cy  = cy;
        Animation anim = Animation.loadLooping("images/hole", 2, 420);
        if (anim != null) {
            this.animation = anim;
            this.img = null;
        } else {
            this.animation = null;
            this.img = ImageManager.loadImage("hole.png");
        }
    }

    public void update() {
        if (animation != null) animation.update();
    }

    public void draw(Graphics g) {
        Image image = animation != null ? animation.getImage() : img;
        if (image != null) {
            g.drawImage(image, cx - W / 2, cy - H / 2, W, H, null);
        }
    }

    public int     getCx()                { return cx; }
    public int     getCy()                { return cy; }
    public boolean isOccupied()           { return occupied; }
    public void    setOccupied(boolean v) { this.occupied = v; }
}