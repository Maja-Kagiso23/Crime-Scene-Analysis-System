package crimescene.classification;

import crimescene.util.GraphEdge;

/**
 * Enhanced SimilarityEdge class for the Region Adjacency Graph (RAG)
 * Stores multiple similarity measures between superpixels
 * @author: MAJA KAGISO
 */
class SimilarityEdge extends GraphEdge {
    private double similarity; // Overall similarity score
    private double colorSimilarity; // Color-based similarity
    private double textureSimilarity; // Texture-based similarity
    private double shapeSimilarity; // Shape-based similarity
    
    private SuperPixelNode fromNode; // Source node of the edge
    private SuperPixelNode toNode; // Target node of the edge

    /**
     * Constructor with overall similarity score
     * @param similarity Overall similarity between connected nodes
     */
    public SimilarityEdge(double similarity) {
        this.similarity = similarity;
        this.colorSimilarity = 0.0;
        this.textureSimilarity = 0.0;
        this.shapeSimilarity = 0.0;
    }
    
    /**
     * Constructor with nodes and similarity score
     * @param fromNode Source node of the edge
     * @param toNode Target node of the edge
     * @param similarity Overall similarity between connected nodes
     */
    public SimilarityEdge(SuperPixelNode fromNode, SuperPixelNode toNode, double similarity) {
        this(similarity);
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    /**
     * Gets the overall similarity value
     * @return The similarity value (higher means more similar)
     */
    public double getSimilarity() {
        return similarity;
    }

    /**
     * Sets the color similarity component
     * @param colorSimilarity The color-based similarity measure
     */
    public void setColorSimilarity(double colorSimilarity) {
        this.colorSimilarity = colorSimilarity;
    }

    /**
     * Gets the color similarity component
     * @return The color-based similarity measure
     */
    public double getColorSimilarity() {
        return colorSimilarity;
    }

    /**
     * Sets the texture similarity component
     * @param textureSimilarity The texture-based similarity measure
     */
    public void setTextureSimilarity(double textureSimilarity) {
        this.textureSimilarity = textureSimilarity;
    }

    /**
     * Gets the texture similarity component
     * @return The texture-based similarity measure
     */
    public double getTextureSimilarity() {
        return textureSimilarity;
    }

    /**
     * Sets the shape similarity component
     * @param shapeSimilarity The shape-based similarity measure
     */
    public void setShapeSimilarity(double shapeSimilarity) {
        this.shapeSimilarity = shapeSimilarity;
    }

    /**
     * Gets the shape similarity component
     * @return The shape-based similarity measure
     */
    public double getShapeSimilarity() {
        return shapeSimilarity;
    }
    
    /**
     * Sets the source node of the edge
     * @param fromNode The source node
     */
    public void setFromNode(SuperPixelNode fromNode) {
        this.fromNode = fromNode;
    }
    
    /**
     * Sets the target node of the edge
     * @param toNode The target node
     */
    public void setToNode(SuperPixelNode toNode) {
        this.toNode = toNode;
    }
    
    /**
     * Gets the source node of the edge
     * @return The source SuperPixelNode
     */
    public SuperPixelNode getFrom() {
        return fromNode;
    }
    
    /**
     * Gets the target node of the edge
     * @return The target SuperPixelNode
     */
    public SuperPixelNode getTo() {
        return toNode;
    }

    /**
     * Recalculates the overall similarity from component similarities
     * @param colorWeight Weight for color similarity
     * @param textureWeight Weight for texture similarity
     * @param shapeWeight Weight for shape similarity
     */
    public void recalculateSimilarity(double colorWeight, double textureWeight, double shapeWeight) {
        double sum = colorWeight + textureWeight + shapeWeight;
        if (sum > 0) {
            // Normalize weights
            colorWeight /= sum;
            textureWeight /= sum;
            shapeWeight /= sum;

            // Calculate weighted similarity
            this.similarity = (colorWeight * colorSimilarity) + (textureWeight * textureSimilarity) + (shapeWeight * shapeSimilarity);
        }
    }

    @Override
    public GraphEdge copy() {
        SimilarityEdge copy = new SimilarityEdge(fromNode, toNode, similarity);
        copy.setColorSimilarity(colorSimilarity);
        copy.setTextureSimilarity(textureSimilarity);
        copy.setShapeSimilarity(shapeSimilarity);
        return copy;
    }

    @Override
    public boolean isSimilar(GraphEdge other, double threshold) {
        if (!(other instanceof SimilarityEdge)) {
            return false;
        }
        
        SimilarityEdge otherEdge = (SimilarityEdge) other;
        return Math.abs(this.similarity - otherEdge.getSimilarity()) <= threshold;
    }
}