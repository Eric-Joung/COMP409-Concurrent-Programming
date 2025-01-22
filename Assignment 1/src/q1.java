import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.util.Random;

enum Orientation {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    private static final Random Random = new Random();

    public static Orientation getRandomOrientation() {
        Orientation[] orientations = Orientation.values();
        return orientations[Random.nextInt(orientations.length)];
    }
}

public class q1 {

    // Parameters
    public static int t;
    public static int n;
    public static int width=4096;
    public static int height=4096;
    private static final int RADIUS_REDUCTION_FACTOR = 2;
    private static final int MIN_RADIUS = 200;
    private static final int MAX_RADIUS = 500;



    public static final int red = 0xffff0000;
    public static final int green = 0xff00ff00;
    public static final int blue = 0xff0000ff;

    public static void main(String[] args) {


        try {

            // once we know what size we want we can creat an empty image
            BufferedImage outputimage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);

            // ------------------------------------
            SnowmanDrawer SD1 = new SnowmanDrawer(outputimage, red, 2);
            SD1.start();

            SnowmanDrawer SD2 = new SnowmanDrawer(outputimage, blue, 2);
            SD2.start();
            // ------------------------------------
            
            // Write out the image
            File outputfile = new File("Assignment 1/outputimage.png");
            ImageIO.write(outputimage, "png", outputfile);

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

    static class SnowmanDrawer implements Runnable {
        private Thread thread;
        private final BufferedImage bufferedImage;
        private final int color;
        private final int numSnowman;


        SnowmanDrawer(BufferedImage bufferedImage, int color, int numSnowman) {
            this.bufferedImage = bufferedImage;
            this.color = color;
            this.numSnowman = numSnowman;
        }

        public void run() {
            try {
                int remainingSnowman = numSnowman;
                while (remainingSnowman > 0) {
                    Orientation orientation = Orientation.getRandomOrientation();
                    int centerX = (int)(Math.random() * width);
                    int centerY = (int)(Math.random() * height);
                    int radius = (int)(Math.random() * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS);

                    drawSnowman(orientation, centerX, centerY, radius, color);
                    remainingSnowman--;
                }
            } catch (Exception e) {
                System.out.println("Exception in thread " + color + ": " + e);
            }
        }

        public void start() {
            if (thread == null) {
                thread = new Thread(this);
                thread.start();
            }
        }

        /**
         * Implementation of the mid-point circle algorithm
         *
         * @param centerX
         * @param centerY
         * @param radius
         * @param rgb
         */
        private void drawCircle(int centerX, int centerY, int radius, int rgb) {

            // Choosing top of circle for simplicity
            int x = 0;
            int y = -radius;

            // Stop when end of octant is reached
            while (x < -y) {
                double midpointY = y + 0.5;

                if (x * x + midpointY * midpointY > radius * radius) {
                    y += 1;
                }

                bufferedImage.setRGB(centerX + x, centerY + y, rgb);
                bufferedImage.setRGB(centerX + x, centerY - y, rgb);
                bufferedImage.setRGB(centerX - x, centerY + y, rgb);
                bufferedImage.setRGB(centerX - x, centerY - y, rgb);
                bufferedImage.setRGB(centerX + y, centerY + x, rgb);
                bufferedImage.setRGB(centerX + y, centerY - x, rgb);
                bufferedImage.setRGB(centerX - y, centerY + x, rgb);
                bufferedImage.setRGB(centerX - y, centerY - x, rgb);

                x++;
            }
        }

        private void drawSnowman(Orientation orientation, int centerX, int centerY, int radius, int rgb) {
            this.drawCircle(centerX, centerY, radius, rgb);

            int midRadius = radius / RADIUS_REDUCTION_FACTOR;
            int topRadius = midRadius / RADIUS_REDUCTION_FACTOR;
            switch (orientation) {
                case UP: {
                    // Draw mid-section
                    int midCenterY = centerY - radius - midRadius;
                    this.drawCircle(centerX, midCenterY, midRadius, rgb);

                    // Draw top-section
                    int topCenterY = midCenterY - midRadius - topRadius;
                    this.drawCircle(centerX, topCenterY, topRadius, rgb);

                    break;
                }
                case DOWN: {
                    // Draw mid-section
                    int midCenterY = centerY + radius + midRadius;
                    this.drawCircle(centerX, midCenterY, midRadius, rgb);

                    // Draw top-section
                    int topCenterY = midCenterY + midRadius + topRadius;
                    this.drawCircle(centerX, topCenterY, topRadius, rgb);

                    break;
                }
                case LEFT: {
                    // Draw mid-section
                    int midCenterX = centerX - radius - midRadius;
                    this.drawCircle(midCenterX, centerY, midRadius, rgb);

                    // Draw top-section
                    int topCenterX = midCenterX - midRadius - topRadius;
                    this.drawCircle(topCenterX, centerY, topRadius, rgb);

                    break;
                }
                case RIGHT: {
                    // Draw mid-section
                    int midCenterX = centerX + radius + midRadius;
                    this.drawCircle(midCenterX, centerY, midRadius, rgb);

                    // Draw top-section
                    int topCenterX = midCenterX + midRadius + topRadius;
                    this.drawCircle(topCenterX, centerY, topRadius, rgb);

                    break;
                }
            }
        }
    }
}
