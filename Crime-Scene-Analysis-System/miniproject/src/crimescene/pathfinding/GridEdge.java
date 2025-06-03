package crimescene.pathfinding;

import crimescene.util.GraphEdge;

//GridEdge class for pathfinding
class GridEdge extends GraphEdge {
 private double weight;
 
 public GridEdge(double weight) {
     this.weight = weight;
 }
 
 public double getWeight() {
     return weight;
 }

@Override
public GraphEdge copy() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public boolean isSimilar(GraphEdge other, double threshold) {
	// TODO Auto-generated method stub
	return false;
}
}
