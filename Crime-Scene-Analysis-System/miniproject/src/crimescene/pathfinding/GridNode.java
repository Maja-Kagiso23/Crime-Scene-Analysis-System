package crimescene.pathfinding;

import java.util.Objects;

import crimescene.util.GraphNode;

//GridNode class for pathfinding
class GridNode extends GraphNode {
 private int x, y; // Grid coordinates
 
 public GridNode(int id, int x, int y) {
     super(id);
     this.x = x;
     this.y = y;
 }
 
 public int getX() {
     return x;
 }
 
 public int getY() {
     return y;
 }
 
 @Override
 public boolean equals(Object obj) {
     if (this == obj) return true;
     if (obj == null || getClass() != obj.getClass()) return false;
     
     GridNode other = (GridNode) obj;
     return this.x == other.x && this.y == other.y;
 }
 
 @Override
 public int hashCode() {
     return Objects.hash(x, y);
 }
}