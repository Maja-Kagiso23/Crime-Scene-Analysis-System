package crimescene.classification;

import crimescene.util.GraphNode;
/*
* @author: MAJA KAGISO
*/
//SuperPixelNode class for the RAG
class SuperPixelNode extends GraphNode {
 private SuperPixel superpixel;
 private String className;
 
 public SuperPixelNode(int id, SuperPixel superpixel) {
     super(id);
     this.superpixel = superpixel;
     this.className = "Unknown";
 }
 
 public SuperPixel getSuperpixel() {
     return superpixel;
 }
 
 public String getClassName() {
     return className;
 }
 
 public void setClassName(String className) {
     this.className = className;
 }
}
