package crimescene.classification;

import crimescene.util.Graph;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * A classifier that identifies objects in crime scene images using superpixels and region adjacency graphs.
 * The classifier can identify weapons, tools, and blood stains in images.
 * 
 * @author Kagiso Maja
 */
public class ObjectClassifier {
    private BufferedImage image;
    private Graph<SuperPixelNode, SimilarityEdge> rag;
    private Map<Integer, String> objectClasses;
    
    /**
     * Constructs an ObjectClassifier with predefined object classes.
     */
    public ObjectClassifier() {
        objectClasses = new HashMap<>();
        objectClasses.put(1, "Weapon");
        objectClasses.put(2, "Tool");
        objectClasses.put(3, "Blood");
    }
    
    /**
     * Classifies objects in the input image and returns an annotated version.
     * 
     * @param inputImage The image to analyze
     * @return A new image with bounding boxes around detected objects
     */
    public BufferedImage classifyObjects(BufferedImage inputImage) {
        this.image = inputImage;
        // Step 1: Create superpixels from image
        List<SuperPixel> superpixels = createSuperPixels(image);
        
        // Step 2: Build Region Adjacency Graph
        buildRAG(superpixels);
        
        // Step 3: Classify using k-NN on RAG
        classifySuperpixels();
        
        // Step 4: Generate output image with bounding boxes
        return generateOutputImage();
    }
    
    /**
     * Creates superpixels from the input image using a simplified SLIC algorithm.
     * 
     * @param image The input image to segment
     * @return A list of SuperPixel objects representing the image regions
     */
    private List<SuperPixel> createSuperPixels(BufferedImage image) {
        // SLIC Superpixel algorithm implementation (simplified)
        List<SuperPixel> superpixels = new ArrayList<>();
        
        int numSuperpixels = 100; // Number can be adjusted based on image complexity
        int gridSize = (int) Math.sqrt(image.getWidth() * image.getHeight() / numSuperpixels);
        
        // Create initial cluster centers on a grid
        for (int y = gridSize/2; y < image.getHeight(); y += gridSize) {
            for (int x = gridSize/2; x < image.getWidth(); x += gridSize) {
                if (x < image.getWidth() && y < image.getHeight()) {
                    int rgb = image.getRGB(x, y);
                    Color color = new Color(rgb);
                    
                    SuperPixel sp = new SuperPixel();
                    sp.centerX = x;
                    sp.centerY = y;
                    sp.r = color.getRed();
                    sp.g = color.getGreen();
                    sp.b = color.getBlue();
                    sp.pixels = new ArrayList<>();
                    
                    superpixels.add(sp);
                }
            }
        }
        
        // Associate pixels with nearest superpixel center (simplified SLIC algorithm)
        int[][] labels = new int[image.getWidth()][image.getHeight()];
        
        // Perform assignment iterations
        for (int iter = 0; iter < 10; iter++) { // 10 iterations is typically sufficient
            // Assign each pixel to nearest cluster
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    double minDist = Double.MAX_VALUE;
                    int bestLabel = -1;
                    
                    Color pixelColor = new Color(image.getRGB(x, y));
                    
                    // Find nearest cluster
                    for (int i = 0; i < superpixels.size(); i++) {
                        SuperPixel sp = superpixels.get(i);
                        
                        // Calculate distance (combination of color and spatial distance)
                        double colorDist = Math.sqrt(
                            Math.pow(sp.r - pixelColor.getRed(), 2) +
                            Math.pow(sp.g - pixelColor.getGreen(), 2) +
                            Math.pow(sp.b - pixelColor.getBlue(), 2)
                        );
                        
                        double spatialDist = Math.sqrt(
                            Math.pow(sp.centerX - x, 2) +
                            Math.pow(sp.centerY - y, 2)
                        );
                        
                        // Normalized distance measure
                        double m = 10; // Compactness factor
                        double distance = colorDist + m/gridSize * spatialDist;
                        
                        if (distance < minDist) {
                            minDist = distance;
                            bestLabel = i;
                        }
                    }
                    
                    labels[x][y] = bestLabel;
                }
            }
            
            // Update cluster centers
            for (SuperPixel sp : superpixels) {
                sp.pixels.clear();
                sp.sumX = 0;
                sp.sumY = 0;
                sp.sumR = 0;
                sp.sumG = 0;
                sp.sumB = 0;
                sp.count = 0;
            }
            
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int label = labels[x][y];
                    if (label >= 0 && label < superpixels.size()) {
                        SuperPixel sp = superpixels.get(label);
                        Color pixelColor = new Color(image.getRGB(x, y));
                        
                        sp.pixels.add(new Point(x, y));
                        sp.sumX += x;
                        sp.sumY += y;
                        sp.sumR += pixelColor.getRed();
                        sp.sumG += pixelColor.getGreen();
                        sp.sumB += pixelColor.getBlue();
                        sp.count++;
                    }
                }
            }
            
            // Update cluster centers
            for (SuperPixel sp : superpixels) {
                if (sp.count > 0) {
                    sp.centerX = sp.sumX / sp.count;
                    sp.centerY = sp.sumY / sp.count;
                    sp.r = sp.sumR / sp.count;
                    sp.g = sp.sumG / sp.count;
                    sp.b = sp.sumB / sp.count;
                    
                    // Calculate bounding box
                    sp.minX = Integer.MAX_VALUE;
                    sp.minY = Integer.MAX_VALUE;
                    sp.maxX = Integer.MIN_VALUE;
                    sp.maxY = Integer.MIN_VALUE;
                    
                    for (Point p : sp.pixels) {
                        sp.minX = Math.min(sp.minX, p.x);
                        sp.minY = Math.min(sp.minY, p.y);
                        sp.maxX = Math.max(sp.maxX, p.x);
                        sp.maxY = Math.max(sp.maxY, p.y);
                    }
                }
            }
        }
        
        return superpixels;
    }
    
    /**
     * Builds a Region Adjacency Graph (RAG) from the superpixels.
     * 
     * @param superpixels The list of superpixels to build the graph from
     */
    private void buildRAG(List<SuperPixel> superpixels) {
        // Create Region Adjacency Graph
        rag = new Graph<>();
        
        // Add nodes (one for each superpixel)
        for (int i = 0; i < superpixels.size(); i++) {
            SuperPixel sp = superpixels.get(i);
            SuperPixelNode node = new SuperPixelNode(i, sp);
            rag.addNode(node);
        }
        
        // Add edges between adjacent superpixels
        for (int i = 0; i < superpixels.size(); i++) {
            SuperPixel sp1 = superpixels.get(i);
            SuperPixelNode node1 = (SuperPixelNode) rag.getNode(i);
            
            for (int j = i + 1; j < superpixels.size(); j++) {
                SuperPixel sp2 = superpixels.get(j);
                SuperPixelNode node2 = (SuperPixelNode) rag.getNode(j);
                
                // Check if superpixels are adjacent
                if (areAdjacent(sp1, sp2)) {
                    // Calculate similarity between superpixels
                    double similarity = calculateSimilarity(sp1, sp2);
                    SimilarityEdge edge = new SimilarityEdge(similarity);
                    rag.addEdge(node1, node2, edge);
                }
            }
        }
    }
    
    /**
     * Checks if two superpixels are adjacent.
     * 
     * @param sp1 The first superpixel
     * @param sp2 The second superpixel
     * @return true if the superpixels are adjacent, false otherwise
     */
    private boolean areAdjacent(SuperPixel sp1, SuperPixel sp2) {
        // Simple adjacency check based on bounding box proximity
        // More accurate implementation would check pixel neighborhood
        int expandedRadius = 5; // Tolerance for adjacency
        
        // Check if expanded bounding boxes intersect
        return !(sp1.maxX + expandedRadius < sp2.minX - expandedRadius || 
                 sp2.maxX + expandedRadius < sp1.minX - expandedRadius ||
                 sp1.maxY + expandedRadius < sp2.minY - expandedRadius || 
                 sp2.maxY + expandedRadius < sp1.minY - expandedRadius);
    }
    
    /**
     * Calculates the similarity between two superpixels based on color.
     * 
     * @param sp1 The first superpixel
     * @param sp2 The second superpixel
     * @return A similarity score between 0 and 1 (1 being most similar)
     */
    private double calculateSimilarity(SuperPixel sp1, SuperPixel sp2) {
        // Calculate color similarity between superpixels
        double colorDist = Math.sqrt(
            Math.pow(sp1.r - sp2.r, 2) +
            Math.pow(sp1.g - sp2.g, 2) +
            Math.pow(sp1.b - sp2.b, 2)
        );
        
        // Normalize to similarity (higher is more similar)
        return 1.0 / (1.0 + colorDist / 255.0);
    }
    
    /**
     * Classifies superpixels using a k-NN approach on the Region Adjacency Graph.
     */
    private void classifySuperpixels() {
        // Implement k-NN on RAG for superpixel classification
        for (SuperPixelNode node : rag.getNodes()) {
            // Get k-nearest neighbors based on graph connections
            List<SuperPixelNode> neighbors = getKNearestNeighbors(node, 5);
            
            // For this prototype, simulate classification
            // This would normally use features extracted from the superpixel
            classifyBasedOnFeatures(node, neighbors);
        }
    }
    
    /**
     * Gets the k nearest neighbors of a node in the Region Adjacency Graph.
     * 
     * @param node The node to find neighbors for
     * @param k The number of neighbors to find
     * @return A list of the k nearest neighbors
     */
    private List<SuperPixelNode> getKNearestNeighbors(SuperPixelNode node, int k) {
        // Get k nearest neighbors using graph edges
        List<SuperPixelNode> neighbors = new ArrayList<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>();
        
        // Add all nodes connected by edges
        for (SuperPixelNode otherNode : rag.getNodes()) {
            if (otherNode == node) continue;
            
            SimilarityEdge edge = rag.getEdge(node, otherNode);
            if (edge != null) {
                queue.add(new NodeDistance(otherNode, 1.0 - edge.getSimilarity())); // Convert to distance
            }
        }
        
        // Get top k
        for (int i = 0; i < k && !queue.isEmpty(); i++) {
            neighbors.add(queue.poll().node);
        }
        
        return neighbors;
    }
    
    /**
     * Classifies a superpixel based on its features and neighbors.
     * 
     * @param node The node to classify
     * @param neighbors The node's neighbors (used for k-NN classification)
     */
    private void classifyBasedOnFeatures(SuperPixelNode node, List<SuperPixelNode> neighbors) {
        // Modified classifier to correctly identify Weapon, Tool, Blood or Background
        SuperPixel sp = node.getSuperpixel();
        
        // Ensure we check the most distinctive features first
        // Blood identification (reddish color is the primary indicator)
        if (isBlood(sp)) {
            node.setClassName("Blood");
        }
        // Weapon identification (metallic color with specific shape characteristics)
        else if (isWeapon(sp)) {
            node.setClassName("Weapon");
        }
        // Tool identification (specific shape characteristics with certain textures)
        else if (isTool(sp)) {
            node.setClassName("Tool");
        }
        else {
            node.setClassName("Background");
        }
    }
    
    /**
     * Determines if a superpixel represents blood based on color characteristics.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel is likely blood, false otherwise
     */
    private boolean isBlood(SuperPixel sp) {
        // Blood is primarily red with low green and blue components
        boolean isReddish = sp.r > 120 && sp.g < 90 && sp.b < 90;
        
        // Blood also tends to have a certain size and can have irregular shapes
        int size = sp.pixels.size();
        
        // Check for blood splatter characteristics - high red to other color ratio
        double redToOtherRatio = sp.r / (double)(sp.g + sp.b + 1); // Avoid division by zero
        
        return isReddish && redToOtherRatio > 1.5 && size > 50;
    }
    
    /**
     * Determines if a superpixel represents a weapon based on color and shape.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel is likely a weapon, false otherwise
     */
    private boolean isWeapon(SuperPixel sp) {
        // Weapons typically have metallic appearances
        boolean hasMetallicColor = isMetallic(sp);
        
        // Weapons usually have distinct shapes - elongated or with sharp edges
        boolean hasWeaponShape = hasWeaponShape(sp);
        
        // Consider size as weapons tend to be significant objects
        int size = sp.pixels.size();
        
        return hasMetallicColor && hasWeaponShape && size > 200;
    }
    
    /**
     * Determines if a superpixel has metallic color characteristics.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel has metallic colors, false otherwise
     */
    private boolean isMetallic(SuperPixel sp) {
        // Metallic appearance often has similar RGB values with higher intensity
        int maxDiff = Math.max(Math.abs(sp.r - sp.g), Math.max(Math.abs(sp.g - sp.b), Math.abs(sp.r - sp.b)));
        
        // Metallic objects reflect light with similar intensity across channels
        boolean similarChannels = maxDiff < 40;
        
        // Metals are typically mid to high brightness but not extremely bright
        boolean appropriateBrightness = ((sp.r + sp.g + sp.b) / 3) > 80 && ((sp.r + sp.g + sp.b) / 3) < 220;
        
        return similarChannels && appropriateBrightness;
    }
    
    /**
     * Determines if a superpixel has weapon-like shape characteristics.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel has weapon-like shape, false otherwise
     */
    private boolean hasWeaponShape(SuperPixel sp) {
        // Weapons often have distinctive shapes - calculating aspect ratio helps identify them
        double aspectRatio = (double)(sp.maxX - sp.minX) / Math.max(1, sp.maxY - sp.minY); // Avoid division by zero
        
        // Many weapons have elongated shapes (knives, bats, etc.)
        boolean isElongated = aspectRatio > 3.0 || aspectRatio < 0.33;
        
        // Consider compactness - weapons tend to have certain compactness ratio
        double area = sp.pixels.size();
        double perimeter = 2 * ((sp.maxX - sp.minX) + (sp.maxY - sp.minY));
        double compactness = (4 * Math.PI * area) / (perimeter * perimeter);
        
        return isElongated || compactness < 0.6;
    }
    
    /**
     * Determines if a superpixel represents a tool based on color and shape.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel is likely a tool, false otherwise
     */
    private boolean isTool(SuperPixel sp) {
        // Tools often have distinct handle shapes and specific textures
        boolean hasToolShape = hasToolShape(sp);
        
        // Tools often have wood or metal components
        boolean hasToolColor = hasToolColor(sp);
        
        // Consider size as tools tend to be of medium size
        int size = sp.pixels.size();
        
        return hasToolShape && hasToolColor && size > 150;
    }
    
    /**
     * Determines if a superpixel has tool-like shape characteristics.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel has tool-like shape, false otherwise
     */
    private boolean hasToolShape(SuperPixel sp) {
        // Tools often have handle-like shapes - long and thin in one direction
        double aspectRatio = (double)(sp.maxX - sp.minX) / Math.max(1, sp.maxY - sp.minY);
        
        // Many tools have elongated handles
        boolean isElongated = aspectRatio > 2.5 || aspectRatio < 0.4;
        
        // Tools often have distinctive head-to-handle ratio
        double length = Math.max(sp.maxX - sp.minX, sp.maxY - sp.minY);
        double width = Math.min(sp.maxX - sp.minX, sp.maxY - sp.minY);
        double lengthToWidthRatio = length / Math.max(1, width);
        
        return isElongated || lengthToWidthRatio > 2.0;
    }
    
    /**
     * Determines if a superpixel has tool-like color characteristics.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel has tool-like colors, false otherwise
     */
    private boolean hasToolColor(SuperPixel sp) {
        // Tools often have wooden handles (brownish) or metal components
        boolean isWooden = isWoodenColor(sp);
        boolean isMetalPart = isMetallic(sp);
        
        return isWooden || isMetalPart;
    }
    
    /**
     * Determines if a superpixel has wooden color characteristics.
     * 
     * @param sp The superpixel to check
     * @return true if the superpixel has wooden colors, false otherwise
     */
    private boolean isWoodenColor(SuperPixel sp) {
        // Brown or wooden colors have higher red, medium green, and low blue
        boolean hasBrownCharacteristics = sp.r > sp.g && sp.g > sp.b && sp.b < 100 && sp.r > 100;
        
        // Wooden colors tend to have specific hue
        boolean hasWoodHue = (sp.r - sp.b) > 50;
        
        return hasBrownCharacteristics && hasWoodHue;
    }
    
    /**
     * Helper class for storing node-distance pairs used in k-NN search.
     */
    private static class NodeDistance implements Comparable<NodeDistance> {
        final SuperPixelNode node;
        final double distance;
        
        /**
         * Constructs a new NodeDistance pair.
         * 
         * @param node The node
         * @param distance The distance to the reference node
         */
        public NodeDistance(SuperPixelNode node, double distance) {
            this.node = node;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
    
    /**
     * Generates an output image with bounding boxes around classified objects.
     * 
     * @return The annotated image with object classifications
     */
    private BufferedImage generateOutputImage() {
        // Create a copy of the original image
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        
        // Draw bounding boxes and labels for classified objects
        for (SuperPixelNode node : rag.getNodes()) {
            SuperPixel sp = node.getSuperpixel();
            String className = node.getClassName();
            
            // Skip background superpixels
            if (className.equals("Background")) continue;
            
            // Draw bounding box with different colors based on class
            Color boxColor;
            switch (className) {
                case "Weapon": boxColor = Color.RED; break;
                case "Tool": boxColor = Color.BLUE; break;
                case "Blood": boxColor = Color.MAGENTA; break;
                default: boxColor = Color.GRAY; break;
            }
            
            g2d.setColor(boxColor);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(sp.minX, sp.minY, sp.maxX - sp.minX, sp.maxY - sp.minY);
            
            // Draw label
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(sp.minX, sp.minY - 15, g2d.getFontMetrics().stringWidth(className) + 4, 15);
            g2d.setColor(Color.BLACK);
            g2d.drawString(className, sp.minX + 2, sp.minY - 3);
        }
        
        g2d.dispose();
        return output;
    }
}