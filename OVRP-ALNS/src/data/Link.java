package data;

public class Link {

	public Node nodeSource;
	public Node nodeTarget;
	public double distance;

	public Link(Node nodeSource, Node nodeTarget, double distance) {
		this.nodeSource = nodeSource;
		this.nodeTarget = nodeTarget;
		this.distance = distance;
	}
	
	@Override
	public String toString() {
		return "[ " + nodeSource.nodeID + "," + nodeTarget.nodeID + " ]";
	}
}
