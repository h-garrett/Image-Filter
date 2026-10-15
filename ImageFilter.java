import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;

public class ImageFilter {

    private static final BufferedImage[] icons = new BufferedImage[12];
    private static final int ICON_SIZE = 16;

    // USER: These values determine the colors of positive and negative space
    private static final int POSITIVE = 0xFFFFFF;
    private static final int NEGATIVE = 0x0;


    private static BufferedImage image;
    private static BufferedImage dp;
    private static final String SAVE_FOLDER = "Results/";
    private static final String IMAGE_FOLDER = "Images/";

    /*
    USER: Modify this String to match the name of your file
    Make sure the file is placed in the Images folder.
    The result will appear in the Results folder.
     */
    private static final String FILENAME = "digiGarrett";

    public static void main(String[] args) throws IOException {
        image = ImageIO.read(new File(IMAGE_FOLDER + FILENAME + ".png"));
        dp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        iconsInit();
        for (int y = 0; y < image.getHeight() - 15; y += ICON_SIZE) {
            for (int x = 0; x < image.getWidth() - 15; x += ICON_SIZE) {
                int sat = aggregate(x, y);
                BufferedImage icon = getIcon(sat);
                drawIcon(x, y, icon, POSITIVE, NEGATIVE);
            }
        }
        saveImage(dp, SAVE_FOLDER + FILENAME + "Filtered.png");
    }

    /**
     * Base directory for the application (directory containing the JAR or exe).
     * Enables Icons2 to be found when run from a packaged exe or JAR.
     */
    private static String getAppBaseDir() {
        try {
            File jarOrDir = new File(ImageFilter.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File dir = jarOrDir.isDirectory() ? jarOrDir : jarOrDir.getParentFile();
            if (dir != null) return dir.getAbsolutePath();
        } catch (URISyntaxException | SecurityException ignored) { }
        return null;
    }

    /**
     * initialize the array of icons.
     * Looks for Icons2 next to the JAR/exe first, then in current directory (for IDE).
     */
    private static void iconsInit() {
        String base = getAppBaseDir();
        try {
            for (int i = 0; i < 12; i++) {
                File iconFile = base != null ? new File(base, "Icons2/icon" + i + ".png") : new File("Icons2/icon" + i + ".png");
                if (!iconFile.exists() && base != null)
                    iconFile = new File("Icons2/icon" + i + ".png");
                icons[i] = ImageIO.read(iconFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param value the aggregate RGB of a square region of pixels
     * @return the associated icon, which will replace the above mentioned region
     */
    private static BufferedImage getIcon(int value) {
        int index = (int) Math.round((double)(value / 255.0) * (icons.length - 1));
        return icons[index];
    }

    /** 
     * @param gridX start x
     * @param gridY start y
     * @return an int which represents the total saturation of color
     * in a square region of pixels
     */
    private static int aggregate(int gridX, int gridY) {
        int sum = 0;
        int[] rgb = new int[ICON_SIZE * ICON_SIZE];
        image.getRGB(gridX, gridY, ICON_SIZE, ICON_SIZE, rgb, 0, ICON_SIZE);
        for (int i = 0; i < rgb.length; i++) {
            int r = (rgb[i] >> 16) & 0xFF;
            int g = (rgb[i] >> 8) & 0xFF;
            int b = (rgb[i]) & 0xFF;
            int grayScale = (int) (0.21 * r + 0.72 * g + 0.07 * b);
            sum += grayScale;
        }
        return sum / (ICON_SIZE * ICON_SIZE);
    }

    /**
     * get the color of some pixel in the original image
     * @param rgb
     * @return
     */
    private static Color getColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb) & 0xFF;
        return new Color(r, g, b);
    }

    /**
     * draw an icon into the drawingPanel
     * @param startX
     * @param startY
     * @param icon
     * @param positiveColor RGB for non-transparent icon pixels
     * @param negativeColor RGB for transparent icon pixels
     */
    private static void drawIcon(int startX, int startY, BufferedImage icon, int positiveColor, int negativeColor) {
        for (int y = startY; y < startY + ICON_SIZE; y++) {
            for (int x = startX; x < startX + ICON_SIZE; x++) {
                int rgb = icon.getRGB(x % ICON_SIZE, y % ICON_SIZE);
                int alpha = rgb >> 24;
                if (alpha != 0) {
                    dp.setRGB(x, y, positiveColor);
                }
                else {
                    dp.setRGB(x, y, negativeColor);
                }
            }
        }
    }

    /**
     * Process an image with the given positive and negative colors.
     * Does not save; the caller is responsible for saving the returned image.
     * @param imageFile source image file
     * @param positiveColor RGB for positive space (e.g. 0xF8FF00)
     * @param negativeColor RGB for negative space (e.g. 0x1A7EE8)
     * @return the filtered BufferedImage, or null on error
     */
    public static BufferedImage processImage(File imageFile, int positiveColor, int negativeColor) {
        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img == null) return null;
            image = img;
            dp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            iconsInit();
            for (int y = 0; y < image.getHeight() - 15; y += ICON_SIZE) {
                for (int x = 0; x < image.getWidth() - 15; x += ICON_SIZE) {
                    int sat = aggregate(x, y);
                    BufferedImage icon = getIcon(sat);
                    drawIcon(x, y, icon, positiveColor, negativeColor);
                }
            }
            return dp;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveImage(BufferedImage dp, String fileName) {
        try {
            ImageIO.write(dp, "png", new File(fileName));
        } catch (IOException e) {
            System.out.println("Unable to save filtered image");
        }
    }

}
