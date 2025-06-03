package crimescene.classification;

import java.awt.Point;
import java.util.List;

/**
 * Enhanced SuperPixel class with texture features
 * Represents a segment of the image with similar pixel characteristics
 */
class SuperPixel {
    // Center coordinates
    int centerX, centerY;
    
    // Average RGB color
    int r, g, b;
    
    // Pixels belonging to this superpixel
    List<Point> pixels;
    
    // For calculation
    int sumX, sumY, sumR, sumG, sumB, count;
    
    // Bounding box
    int minX, minY, maxX, maxY;
    
    // Texture features (variances in RGB channels)
    double[] textureFeatures = new double[3];
    
    /**
     * Gets the aspect ratio of this superpixel
     * @return Width divided by height of the bounding box
     */
    public double getAspectRatio() {
        return (maxX - minX) / Math.max(1.0, maxY - minY);
    }
    
    /**
     * Gets the size (number of pixels) of this superpixel
     * @return Number of pixels in this superpixel
     */
    public int getSize() {
        return pixels == null ? 0 : pixels.size();
    }
    
    /**
     * Gets the area of the bounding box
     * @return Area of the bounding box in pixels
     */
    public int getBoundingBoxArea() {
        return (maxX - minX) * (maxY - minY);
    }
    
    /**
     * Calculates the fill ratio (how much of the bounding box is filled)
     * @return Ratio of pixels to bounding box area
     */
    public double getFillRatio() {
        int area = getBoundingBoxArea();
        return area > 0 ? getSize() / (double)area : 0;
    }
    
    /**
     * Calculates average texture variance across all channels
     * @return Average texture variance
     */
    public double getAverageTextureVariance() {
        if (textureFeatures == null) {
            return 0;
        }
        double sum = 0;
        for (double variance : textureFeatures) {
            sum += variance;
        }
        return sum / textureFeatures.length;
    }
}