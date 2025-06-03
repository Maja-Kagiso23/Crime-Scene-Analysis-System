// ======================================================
// PACKAGE: crimescene.util
// ======================================================

package crimescene.util;

import java.util.*;

/**
 * Generic graph implementation supporting any node and edge types
 */
public class Graph<N extends GraphNode, E extends GraphEdge> {
    private Map<Integer, N> nodes;
    private Map<String, E> edges;
    
    public Graph() {
        nodes = new HashMap<>();
        edges = new HashMap<>();
    }
    
    public void addNode(N node) {
        nodes.put(node.getId(), node);
    }
    
    public N getNode(int id) {
        return nodes.get(id);
    }
    
    public void addEdge(N from, N to, E edge) {
        String edgeKey = generateEdgeKey(from, to);
        edges.put(edgeKey, edge);
    }
    
    public E getEdge(N from, N to) {
        String edgeKey = generateEdgeKey(from, to);
        return edges.get(edgeKey);
    }
    
    public Collection<N> getNodes() {
        return nodes.values();
    }
    
    public Collection<E> getEdges() {
        return edges.values();
    }
    
    public List<N> getAdjacentNodes(N node) {
        List<N> adjacent = new ArrayList<>();
        
        for (N other : nodes.values()) {
            if (other != node && getEdge(node, other) != null) {
                adjacent.add(other);
            }
        }
        
        return adjacent;
    }
    
    private String generateEdgeKey(N from, N to) {
        // Make edge key unique and symmetric (so A->B is same as B->A)
        int min = Math.min(from.getId(), to.getId());
        int max = Math.max(from.getId(), to.getId());
        return min + "-" + max;
    }
}