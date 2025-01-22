import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class q1 {

    // Parameters
    public static int t;
    public static int n;
    public static int width=4096;
    public static int height=4096;
    public static int radiusReductionFactor = 2;

    public static int red = 0xffff0000;
    public static int green = 0xff00ff00;
    public static int blue = 0xff0000ff;

    enum Orientation {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    public static void main(String[] args) {


        try {

            // once we know what size we want we can creat an empty image
            BufferedImage outputimage = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);

            // ------------------------------------
            // Your code would go here
            
            // The easiest mechanisms for getting and setting pixels are the
            // BufferedImage.setRGB(x,y,value) and getRGB(x,y) functions.
            // Consult the javadocs for other methods.

            // The getRGB/setRGB functions return/expect the pixel value in ARGB format, one byte per channel.  For example,
            //  int p = img.getRGB(x,y);
            // With the 32-bit pixel value you can extract individual colour channels by shifting and masking:
            //  int red = ((p>>16)&0xff);
            //  int green = ((p>>8)&0xff);
            //  int blue = (p&0xff);
            // If you want the alpha channel value it's stored in the uppermost 8 bits of the 32-bit pixel value
            //  int alpha = ((p>>24)&0xff);
            // Note that an alpha of 0 is transparent, and an alpha of 0xff is fully opaque.

            drawSnowman(outputimage, Orientation.DOWN, 1000, 1000, 300, red);
            
            // ------------------------------------
            
            // Write out the image
            File outputfile = new File("Assignment 1/outputimage.png");
            ImageIO.write(outputimage, "png", outputfile);

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

    /**
     * Implementation of the mid-point circle algorithm
     * @param bufferedImage
     * @param centerX
     * @param centerY
     * @param radius
     * @param rgb
     */
    public static void drawCircle(BufferedImage bufferedImage, int centerX, int centerY, int radius, int rgb) {

        // Choosing top of circle for simplicity
        int x = 0;
        int y = -radius;

        // Stop when end of octant is reached
        while (x < -y) {
            double midpointY = y + 0.5;

            if (x*x + midpointY*midpointY > radius*radius) {
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

    public static void drawSnowman(BufferedImage bufferedImage, Orientation orientation, int centerX, int centerY, int radius, int rgb) {
        drawCircle(bufferedImage, centerX, centerY, radius, rgb);

        int midRadius = radius / radiusReductionFactor;
        int topRadius = midRadius / radiusReductionFactor;
        switch(orientation) {
            case UP: {
                // Draw mid-section
                int midCenterY = centerY - radius - midRadius;
                drawCircle(bufferedImage, centerX, midCenterY, midRadius, rgb);

                // Draw top-section
                int topCenterY = midCenterY - midRadius - topRadius;
                drawCircle(bufferedImage, centerX, topCenterY, topRadius, rgb);

                break;
            }
            case DOWN: {
                // Draw mid-section
                int midCenterY = centerY + radius + midRadius;
                drawCircle(bufferedImage, centerX, midCenterY, midRadius, rgb);

                // Draw top-section
                int topCenterY = midCenterY + midRadius + topRadius;
                drawCircle(bufferedImage, centerX, topCenterY, topRadius, rgb);

                break;
            }
            case LEFT: {
                // Draw mid-section
                int midCenterX = centerX - radius - midRadius;
                drawCircle(bufferedImage, midCenterX, centerY, midRadius, rgb);

                // Draw top-section
                int topCenterX = midCenterX - midRadius - topRadius;
                drawCircle(bufferedImage, topCenterX, centerY, topRadius, rgb);

                break;
            }
            case RIGHT: {
                // Draw mid-section
                int midCenterX = centerX + radius + midRadius;
                drawCircle(bufferedImage, midCenterX, centerY, midRadius, rgb);

                // Draw top-section
                int topCenterX = midCenterX + midRadius + topRadius;
                drawCircle(bufferedImage, topCenterX, centerY, topRadius, rgb);

                break;
            }
        }
    }
}
