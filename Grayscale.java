import java.awt.*;
import java.awt.image.BufferedImage;

public class Grayscale implements ImageFX {

    @Override
    public BufferedImage applyEffect(BufferedImage img) {

        BufferedImage copy = ImageManager.copyImage(img);

        for (int y = 0; y < copy.getHeight(); y++) {
            for (int x = 0; x < copy.getWidth(); x++) {

                Color c = new Color(copy.getRGB(x, y));

                int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;

                Color newColor = new Color(gray, gray, gray);

                copy.setRGB(x, y, newColor.getRGB());
            }
        }

        return copy;
    }
}