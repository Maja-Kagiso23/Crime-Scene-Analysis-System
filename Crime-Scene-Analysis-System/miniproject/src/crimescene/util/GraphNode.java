package crimescene.util;

public abstract class GraphNode {
    private int id;
    
    public GraphNode(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
}
