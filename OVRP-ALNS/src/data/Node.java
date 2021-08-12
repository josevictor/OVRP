package data;

public class Node {

	public int nodeID;
	public double demand;
	public double coordX;
	public double coordY;
	
	public Node(int nodeID, double demand, double coordX, double coordY) {
		this.nodeID = nodeID;
		this.demand = demand;
		this.coordX = coordX;
		this.coordY = coordY;
	}
	
}
