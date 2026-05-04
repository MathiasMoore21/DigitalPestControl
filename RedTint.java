import java.awt.*;
import java.awt.image.BufferedImage;

public class RedTint implements ImageFX {

    @Override
    public BufferedImage applyEffect(BufferedImage img) {

        BufferedImage copy = ImageManager.copyImage(img);

        for (int y = 0; y < copy.getHeight(); y++) {
            for (int x = 0; x < copy.getWidth(); x++) {

                Color c = new Color(copy.getRGB(x, y));

                int red = Math.min(255, c.getRed() + 80);

                Color newColor = new Color(red, c.getGreen(), c.getBlue());

                copy.setRGB(x, y, newColor.getRGB());
            }
        }

        return copy;
    }
}