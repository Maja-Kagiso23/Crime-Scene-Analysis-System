package crimescene.util;

/**
 * Enhanced GraphEdge class with additional functionality for the RAG
 */
public abstract class GraphEdge {
    private double weight;
    private boolean visited;
    private String label;

    public GraphEdge() {
        this.weight = 1.0; // Default weight
        this.visited = false;
        this.label = "";
    }

    public GraphEdge(double weight) {
        this.weight = weight;
        this.visited = false;
        this.label = "";
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Resets the edge's visited status
     */
    public void reset() {
        this.visited = false;
    }

    /**
     * Creates a deep copy of this edge
     * @return A new GraphEdge with the same properties
     */
    public abstract GraphEdge copy();

    /**
     * Checks if this edge is similar to another edge
     * @param other The edge to compare with
     * @param threshold The similarity threshold
     * @return true if edges are similar within the threshold
     */
    public abstract boolean isSimilar(GraphEdge other, double threshold);
}