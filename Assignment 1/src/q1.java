import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.util.ArrayList;
import java.util.List;
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
    public static int width;
    public static int height;
    private static final int RADIUS_REDUCTION_FACTOR = 2;
    private static final int MIN_RADIUS = 8;
    private static final int MAX_RADIUS = 500;
    public static final int red = 0xffff0000;
    public static final int green = 0xff00ff00;
    public static final int blue = 0xff0000ff;

    public static void main(String[] args) {

        if (args.length != 4) {
            System.out.println("Usage: q1.java width height numThreads numSnowman");
            return;
        }

        try {

            width = Integer.parseInt(args[0]);
            height = Integer.parseInt(args[1]);
            t = Integer.parseInt(args[2]);
            n = Integer.parseInt(args[3]);

            // once we know what size we want we can creat an empty image
            BufferedImage outputimage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);

            // ------------------------------------
            int numSnowmanToDraw = n / t;

            // Create t number of threads with n / t snowman to draw
            long startTime = System.currentTimeMillis();
            SnowmanDrawer[] snowmanDrawers = new SnowmanDrawer[t];
            for (int i = 0; i < t; i++) {
                snowmanDrawers[i] = new SnowmanDrawer(outputimage, green, numSnowmanToDraw);
                snowmanDrawers[i].start();
            }

            for (SnowmanDrawer snowmanDrawer : snowmanDrawers) {
                snowmanDrawer.join();
            }

            // Once all threads have finished, measure program runtime
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("Total time taken to draw " + n + " snowmen with " + t + " threads: " + duration + " ms");
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
        private static final List<SnowmanBox> drawnSnowman = new ArrayList<>();
        private Thread thread;
        private final BufferedImage bufferedImage;
        private final int color;
        private final int numSnowman;

        public SnowmanDrawer(BufferedImage bufferedImage, int color, int numSnowman) {
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

                    if (isSnowmanDrawable(orientation, centerX, centerY, radius, color)) {
                        remainingSnowman--;
                    }
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

        public void join() throws InterruptedException {
            thread.join();
        }

        private boolean lockSnowman(SnowmanBox snowmanBox ) {
            // Check if snowman is inbounds
            if (!snowmanBox.isSnowmanBoxInbounds()) {
                return false;
            }

            synchronized (drawnSnowman) {
                for (SnowmanBox drawnSnowman : drawnSnowman) {
                    if (SnowmanBox.areBoxesTouching(snowmanBox, drawnSnowman)) {
                        return false;
                    }
                }
                drawnSnowman.add(snowmanBox);
            }
            return true;
        }

        private boolean isSnowmanDrawable(Orientation orientation, int centerX, int centerY, int radius, int rgb) {
            int midRadius = radius / RADIUS_REDUCTION_FACTOR;
            int topRadius = midRadius / RADIUS_REDUCTION_FACTOR;

            int midCenterX, midCenterY, topCenterX, topCenterY;

            switch (orientation) {
                case UP: {
                    // Unchanged x values
                    midCenterX = centerX;
                    topCenterX = centerX;
                    // New y values
                    midCenterY = centerY - radius - midRadius;
                    topCenterY = midCenterY - midRadius - topRadius;

                    break;
                }
                case DOWN: {
                    // Unchanged x values
                    midCenterX = centerX;
                    topCenterX = centerX;
                    // New y values
                    midCenterY = centerY + radius + midRadius;
                    topCenterY = midCenterY + midRadius + topRadius;

                    break;
                }
                case LEFT: {
                    // Unchanged y values
                    midCenterY = centerY;
                    topCenterY = centerY;
                    // New x values
                    midCenterX = centerX - radius - midRadius;
                    topCenterX = midCenterX - midRadius - topRadius;

                    break;
                }
                case RIGHT: {
                    // Unchanged y values
                    midCenterY = centerY;
                    topCenterY = centerY;
                    // New x values
                    midCenterX = centerX + radius + midRadius;
                    topCenterX = midCenterX + midRadius + topRadius;

                    break;
                }
                default: {
                    return false;
                }
            }

            SnowmanBox snowmanBox = new SnowmanBox(centerX, centerY, orientation, radius);

            // Attempt locking of circles
            if (!lockSnowman(snowmanBox)) {
                return false;
            }

            Circle bottom = new Circle(centerX, centerY, radius);
            Circle middle = new Circle(midCenterX, midCenterY, midRadius);
            Circle top = new Circle(topCenterX, topCenterY, topRadius);

            bottom.drawCircle(rgb);
            middle.drawCircle(rgb);
            top.drawCircle(rgb);

            return true;
        }

        private class Circle {
            public int centerX;
            public int centerY;
            public int radius;

            public Circle(int centerX, int centerY, int radius) {
                this.centerX = centerX;
                this.centerY = centerY;
                this.radius = radius;
            }

            /**
             * Implementation of the mid-point circle algorithm
             * @param rgb
             */
            private void drawCircle(int rgb) {

                // Choosing top of circle for simplicity
                int x = 0;
                int y = -this.radius;

                // Stop when end of octant is reached
                while (x < -y) {
                    double midpointY = y + 0.5;

                    if (x * x + midpointY * midpointY > this.radius * this.radius) {
                        y += 1;
                    }

                    bufferedImage.setRGB(this.centerX + x, this.centerY + y, rgb);
                    bufferedImage.setRGB(this.centerX + x, this.centerY - y, rgb);
                    bufferedImage.setRGB(this.centerX - x, this.centerY + y, rgb);
                    bufferedImage.setRGB(this.centerX - x, this.centerY - y, rgb);
                    bufferedImage.setRGB(this.centerX + y, this.centerY + x, rgb);
                    bufferedImage.setRGB(this.centerX + y, this.centerY - x, rgb);
                    bufferedImage.setRGB(this.centerX - y, this.centerY + x, rgb);
                    bufferedImage.setRGB(this.centerX - y, this.centerY - x, rgb);

                    x++;
                }
            }
        }

        private static class SnowmanBox {
            private int minX;
            private int maxX;
            private int minY;
            private int maxY;

            public SnowmanBox(int minX, int maxX, int minY, int maxY) {
                this.minX = minX;
                this.maxX = maxX;
                this.minY = minY;
                this.maxY = maxY;
            }

            public SnowmanBox(int centerX, int centerY, Orientation orientation, int radius) {
                switch (orientation) {
                    case UP:
                        this.minX = centerX - radius;
                        this.maxX = centerX + radius;

                        this.maxY = centerY + radius;
                        this.minY = centerY - radius - 2 * (radius/RADIUS_REDUCTION_FACTOR) - 2 * (radius/(2*RADIUS_REDUCTION_FACTOR));
                        break;
                    case DOWN:
                        this.minX = centerX - radius;
                        this.maxX = centerX + radius;

                        this.minY = centerY - radius;
                        this.maxY = centerY + radius + 2 * (radius/RADIUS_REDUCTION_FACTOR) + 2 * (radius/(2*RADIUS_REDUCTION_FACTOR));
                        break;
                    case LEFT:
                        this.minY = centerY - radius;
                        this.maxY = centerY + radius;

                        this.maxX = centerX + radius;
                        this.minX = centerX - radius - 2 * (radius/RADIUS_REDUCTION_FACTOR) - 2 * (radius/(2*RADIUS_REDUCTION_FACTOR));
                        break;
                    case RIGHT:
                        this.minY = centerY - radius;
                        this.maxY = centerY + radius;

                        this.minX = centerX - radius;
                        this.maxX = centerX + radius + 2 * (radius/RADIUS_REDUCTION_FACTOR) + 2 * (radius/(2*RADIUS_REDUCTION_FACTOR));
                        break;
                    default:
                        break;
                }
            }

            public static boolean areBoxesTouching(SnowmanBox snowmanBox1, SnowmanBox snowmanBox2) {
                if (snowmanBox1.maxX < snowmanBox2.minX || snowmanBox2.maxX < snowmanBox1.minX || snowmanBox1.maxY < snowmanBox2.minY || snowmanBox2.maxY < snowmanBox1.minY) {
                    return false;
                }
                return true;
            }

            public boolean isSnowmanBoxInbounds() {
                if (minX < 10 || maxX > width - 10 || minY < 10 || maxY > height - 10) {
                    return false;
                }
                return true;
            }
        }
    }
}
