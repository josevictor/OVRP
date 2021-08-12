package data;

import java.util.ArrayList;
import java.util.List;

public class Route {
	
	public List<Node> nodes;
	private double[][] distances;
	public int totalDemand;
	
	public Route(double[][] distances) {
		this.nodes = new ArrayList<Node>();
		totalDemand = 0;
		this.distances = distances;
	}
	
	public double getCost() {
		double cost = 0;
		for (int i = 0; i < nodes.size() - 1; i++) {
			cost += distances[nodes.get(i).nodeID][nodes.get(i+1).nodeID];
		}
		
		return cost; 
	}

}
