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

            long startTime = System.currentTimeMillis();

            // Create t number of threads with n / t snowman to draw
            SnowmanDrawer[] snowmanDrawers = new SnowmanDrawer[t];
            for (int i = 0; i < t; i++) {
                snowmanDrawers[i] = new SnowmanDrawer(outputimage, green, numSnowmanToDraw);
                snowmanDrawers[i].start();
            }

            for (SnowmanDrawer snowmanDrawer : snowmanDrawers) {
                snowmanDrawer.join();
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println("Total time taken: " + duration + " ms");
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
        private static List<Circle> drawnCircles = new ArrayList<>();
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

        private synchronized boolean lockSnowman(List<Circle> snowman) {
            synchronized (drawnCircles) {
                for (Circle circle : snowman) {
                    if (circle.isCircleOverlapping()) {
                        return false;
                    }
                }

                drawnCircles.addAll(snowman);
                return true;
            }
        }

        private boolean isSnowmanInBounds(List<Circle> snowman) {
            for (Circle circle : snowman) {
                if (!circle.isCircleInBounds()) {
                    return false;
                }
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

            List<Circle> snowman = new ArrayList<>();

            Circle bottom = new Circle(centerX, centerY, radius);
            Circle middle = new Circle(midCenterX, midCenterY, midRadius);
            Circle top = new Circle(topCenterX, topCenterY, topRadius);

            snowman.add(bottom);
            snowman.add(middle);
            snowman.add(top);

            // Check if snowman is within the boundaries
            if (!isSnowmanInBounds(snowman)) {
                return false;
            }

            // Attempt locking of circles
            if (!lockSnowman(snowman)) {
                return false;
            }

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
             * Returns true if circle is within the boundaries of the BufferedImage
             * @return true if circle is within boundaries of the BufferedImage
             */
            private boolean isCircleInBounds() {
                int leftX = this.centerX - this.radius - 10;
                int rightX = this.centerX + this.radius + 10;

                int topY = this.centerY - this.radius - 10;
                int bottomY = this.centerY + this.radius + 10;

                return leftX > 0 && rightX <= width && topY > 0 && bottomY <= height;
            }

            /**
             * Returns true if the circle is successfully checked out. Returns false if the circle is already checked out.
             * @return true if circle is overlapping with existing circle, false otherwise.
             */
            private boolean isCircleOverlapping() {
                for (Circle drawnCircle : drawnCircles) {
                    if (Circle.areCirclesTouching(this, drawnCircle)) {
                        return true;
                    }
                }

                return false;
            }

            public static boolean areCirclesTouching(Circle circle1, Circle circle2) {
                int x1Max = circle1.centerX + circle1.radius;
                int y1Max = circle1.centerY + circle1.radius;
                int x1Min = circle1.centerX - circle1.radius;
                int y1Min = circle1.centerY - circle1.radius;


                int x2Max = circle2.centerX + circle2.radius;
                int y2Max = circle2.centerY + circle2.radius;
                int x2Min = circle2.centerX - circle2.radius;
                int y2Min = circle2.centerY - circle2.radius;

                if (x1Max < x2Min || x2Max < x1Min || y1Max < y2Min || y2Max < y1Min) {
                    return false;
                }

                return true;
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
    }
}
