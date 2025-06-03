package crimescene.pathfinding;

import crimescene.util.GraphNode;
import crimescene.util.Graph;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.awt.*;
import java.util.*;

/**
 * A pathfinding utility that finds optimal paths on a floor plan image using A* algorithm.
 * The class converts a floor plan image into a grid graph and finds paths between points.
 * 
 * @author Kagiso Maja
 */
public class PathFinder {
    private BufferedImage floorPlan;
    private Graph<GridNode, GridEdge> gridGraph;
    private List<GridNode> path = null;
    private boolean[][] walkable;
    private int gridWidth, gridHeight;
    private int nodeSize;
    
    /**
     * Constructs a PathFinder with default settings.
     */
    public PathFinder() {
        nodeSize = 10; // Size of each grid cell in pixels
    }
    
    /**
     * Finds the optimal path between two points on a floor plan image.
     * 
     * @param floorPlanImage The floor plan image to analyze
     * @param start The starting point (in pixel coordinates)
     * @param end The destination point (in pixel coordinates)
     * @return A new image with the path visualized on top of the floor plan
     */
    public BufferedImage findOptimalPath(BufferedImage floorPlanImage, Point start, Point end) {
        this.floorPlan = floorPlanImage;
        
        // Step 1: Convert floor plan to grid
        createGridFromImage();
        
        // Step 2: Build grid graph
        buildGridGraph();
        
        // Step 3: Find path using A* algorithm
        List<GridNode> path = findPathAStar(start, end);
        
        // Step 4: Generate output image with path overlay
        return generateOutputImage(path);
    }
    
    /**
     * Converts the floor plan image into a grid of walkable/non-walkable cells.
     * Light-colored areas are considered walkable.
     */
    private void createGridFromImage() {
        // Calculate grid dimensions
        gridWidth = floorPlan.getWidth() / nodeSize;
        gridHeight = floorPlan.getHeight() / nodeSize;
        
        // Initialize walkable array
        walkable = new boolean[gridWidth][gridHeight];
        
        // Analyze floor plan to determine walkable areas
        // For simplicity, we consider light-colored areas as walkable
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                // Sample pixel from the center of grid cell
                int centerX = x * nodeSize + nodeSize / 2;
                int centerY = y * nodeSize + nodeSize / 2;
                
                if (centerX < floorPlan.getWidth() && centerY < floorPlan.getHeight()) {
                    Color color = new Color(floorPlan.getRGB(centerX, centerY));
                    
                    // Light colors are considered walkable (simplified approach)
                    int brightness = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                    walkable[x][y] = brightness > 200; // Threshold can be adjusted
                } else {
                    walkable[x][y] = false;
                }
            }
        }
    }
    
    /**
     * Builds a grid graph from the walkable cells.
     * Each walkable cell becomes a node, and edges connect adjacent walkable cells.
     */
    private void buildGridGraph() {
        // Create grid graph
        gridGraph = new Graph<>();
        
        // Add nodes for each walkable grid cell
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (walkable[x][y]) {
                    int id = y * gridWidth + x;
                    GridNode node = new GridNode(id, x, y);
                    gridGraph.addNode(node);
                }
            }
        }
        
        // Add edges between adjacent walkable cells (4-directional)
        int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}}; // Right, Down, Left, Up
        
        for (GridNode node : gridGraph.getNodes()) {
            int x = node.getX();
            int y = node.getY();
            
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                
                // Check if neighbor is within bounds and walkable
                if (nx >= 0 && nx < gridWidth && ny >= 0 && ny < gridHeight && walkable[nx][ny]) {
                    int neighborId = ny * gridWidth + nx;
                    GridNode neighbor = gridGraph.getNode(neighborId);
                    
                    if (neighbor != null) {
                        // Edge weight is 1.0 for uniform grid
                        GridEdge edge = new GridEdge(1.0);
                        gridGraph.addEdge(node, neighbor, edge);
                    }
                }
            }
        }
    }
   
    
    /**
     * Finds a path between two points using the A* algorithm.
     * 
     * @param startPoint The starting point in pixel coordinates
     * @param endPoint The destination point in pixel coordinates
     * @return A list of nodes representing the path from start to end
     */
    private List<GridNode> findPathAStar(Point startPoint, Point endPoint) {
        // Convert pixel coordinates to grid coordinates
        int startGridX = startPoint.x / nodeSize;
        int startGridY = startPoint.y / nodeSize;
        int endGridX = endPoint.x / nodeSize;
        int endGridY = endPoint.y / nodeSize;
        
        // Find closest walkable grid cells if start/end are not walkable
        GridNode startNode = findClosestWalkableNode(startGridX, startGridY);
        GridNode endNode = findClosestWalkableNode(endGridX, endGridY);
        
        if (startNode == null || endNode == null) {
            return new ArrayList<>(); // No path possible
        }
        
        // A* algorithm implementation
        // Open set: nodes to be evaluated
        PriorityQueue<NodeWithCost> openSet = new PriorityQueue<>();
        // Closed set: nodes already evaluated
        Set<GridNode> closedSet = new HashSet<>();
        
        // Cost from start to node
        Map<GridNode, Double> gScore = new HashMap<>();
        // Parent pointers for path reconstruction
        Map<GridNode, GridNode> cameFrom = new HashMap<>();
        
        // Initialize start node
        gScore.put(startNode, 0.0);
        openSet.add(new NodeWithCost(startNode, 0.0 + heuristic(startNode, endNode)));
        
        while (!openSet.isEmpty()) {
            // Get node with lowest f-score
            GridNode current = openSet.poll().node;
            
            // Check if we reached the goal
            if (current.equals(endNode)) {
                return reconstructPath(cameFrom, current);
            }
            
            closedSet.add(current);
            
            // Check all neighbors
            for (GridNode neighbor : getNeighbors(current)) {
                if (closedSet.contains(neighbor)) {
                    continue; // Already evaluated
                }
                
                // Tentative g-score
                double tentativeGScore = gScore.get(current) + getEdgeWeight(current, neighbor);
                
                // If this is a better path to neighbor
                if (!gScore.containsKey(neighbor) || tentativeGScore < gScore.get(neighbor)) {
                    // Record this path
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    
                    // Add to open set with f-score
                    double fScore = tentativeGScore + heuristic(neighbor, endNode);
                    
                    // Check if already in open set
                    boolean found = false;
                    for (NodeWithCost nwc : openSet) {
                        if (nwc.node.equals(neighbor)) {
                            found = true;
                            nwc.cost = fScore; // Update cost
                            break;
                        }
                    }
                    
                    if (!found) {
                        openSet.add(new NodeWithCost(neighbor, fScore));
                    }
                }
            }
        }
        
        // No path found
        return new ArrayList<>();
    }
    
    /**
     * Finds the closest walkable node to the specified grid coordinates.
     * 
     * @param gridX The x-coordinate in grid units
     * @param gridY The y-coordinate in grid units
     * @return The closest walkable GridNode, or null if none found
     */
    private GridNode findClosestWalkableNode(int gridX, int gridY) {
        // If the specified grid cell is walkable, return it directly
        if (gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight && walkable[gridX][gridY]) {
            int id = gridY * gridWidth + gridX;
            return gridGraph.getNode(id);
        }
        
        // Otherwise, search for closest walkable cell
        int maxSearchRadius = Math.max(gridWidth, gridHeight);
        
        for (int radius = 1; radius < maxSearchRadius; radius++) {
            // Check in expanding square pattern
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    // Only check perimeter of square
                    if (Math.abs(dx) == radius || Math.abs(dy) == radius) {
                        int nx = gridX + dx;
                        int ny = gridY + dy;
                        
                        if (nx >= 0 && nx < gridWidth && ny >= 0 && ny < gridHeight && walkable[nx][ny]) {
                            int id = ny * gridWidth + nx;
                            return gridGraph.getNode(id);
                        }
                    }
                }
            }
        }
        
        return null; // No walkable cell found
    }
    
    /**
     * Gets all neighboring nodes of a given node.
     * 
     * @param node The node to get neighbors for
     * @return List of neighboring nodes
     */
    private List<GridNode> getNeighbors(GridNode node) {
        List<GridNode> neighbors = new ArrayList<>();
        
        for (GraphNode neighborNode : gridGraph.getAdjacentNodes(node)) {
            neighbors.add((GridNode) neighborNode);
        }
        
        return neighbors;
    }
    
    /**
     * Gets the weight of the edge between two nodes.
     * 
     * @param from The starting node
     * @param to The destination node
     * @return The edge weight, or Double.MAX_VALUE if no edge exists
     */
    private double getEdgeWeight(GridNode from, GridNode to) {
        GridEdge edge = gridGraph.getEdge(from, to);
        return edge != null ? edge.getWeight() : Double.MAX_VALUE;
    }
    
    /**
     * Calculates the heuristic (estimated distance) between two nodes.
     * Uses Euclidean distance.
     * 
     * @param from The starting node
     * @param to The destination node
     * @return The estimated distance between the nodes
     */
    private double heuristic(GridNode from, GridNode to) {
        // Euclidean distance heuristic
        int dx = from.getX() - to.getX();
        int dy = from.getY() - to.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Reconstructs the path from the cameFrom map.
     * 
     * @param cameFrom Map containing parent pointers
     * @param current The end node of the path
     * @return The reconstructed path from start to end
     */
    private List<GridNode> reconstructPath(Map<GridNode, GridNode> cameFrom, GridNode current) {
        List<GridNode> path = new ArrayList<>();
        path.add(current);
        
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current); // Add to front of list to get start-to-end order
        }
        
        return path;
    }
    
    /**
     * Generates an output image with the path visualized.
     * 
     * @param path The path to visualize
     * @return A new image with the path drawn on the floor plan
     */
    private BufferedImage generateOutputImage(List<GridNode> path) {
        // Create a copy of the floor plan
    	this.path = path;
        BufferedImage output = new BufferedImage(floorPlan.getWidth(), floorPlan.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(floorPlan, 0, 0, null);
        
        // Draw the path if found
        if (!this.path.isEmpty()) {
            g2d.setColor(new Color(0, 150, 255, 180)); // Semi-transparent blue
            g2d.setStroke(new BasicStroke(nodeSize / 2.0f));
            
            // Draw path lines
            for (int i = 0; i < this.path.size() - 1; i++) {
                GridNode current = this.path.get(i);
                GridNode next = this.path.get(i + 1);
                
                int x1 = current.getX() * nodeSize + nodeSize / 2;
                int y1 = current.getY() * nodeSize + nodeSize / 2;
                int x2 = next.getX() * nodeSize + nodeSize / 2;
                int y2 = next.getY() * nodeSize + nodeSize / 2;
                
                g2d.drawLine(x1, y1, x2, y2);
            }
            
            // Draw nodes along path
            g2d.setColor(new Color(0, 100, 200));
            for (GridNode node : this.path) {
                int x = node.getX() * nodeSize + nodeSize / 2;
                int y = node.getY() * nodeSize + nodeSize / 2;
                
                g2d.fillOval(x - 4, y - 4, 8, 8);
            }
            
            // Mark start and end points
            if (this.path.size() >= 2) {
                GridNode start = this.path.get(0);
                GridNode end = this.path.get(this.path.size() - 1);
                
                int startX = start.getX() * nodeSize + nodeSize / 2;
                int startY = start.getY() * nodeSize + nodeSize / 2;
                int endX = end.getX() * nodeSize + nodeSize / 2;
                int endY = end.getY() * nodeSize + nodeSize / 2;
                
                g2d.setColor(Color.GREEN);
                g2d.fillOval(startX - 6, startY - 6, 12, 12);
                g2d.setColor(Color.RED);
                g2d.fillOval(endX - 6, endY - 6, 12, 12);
            }
        } else {
            // No path found
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            g2d.drawString("No path found", 50, 50);
        }
        
        g2d.dispose();
        return output;
    }
    
    /**
     * Helper class for A* algorithm that associates a node with its cost.
     */
    private class NodeWithCost implements Comparable<NodeWithCost> {
        GridNode node;
        double cost;
        
        public NodeWithCost(GridNode node, double cost) {
            this.node = node;
            this.cost = cost;
        }
        
        @Override
        public int compareTo(NodeWithCost other) {
            return Double.compare(this.cost, other.cost);
        }
    }
}