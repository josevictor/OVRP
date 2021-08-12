package alns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import data.Data;
import data.Node;
import data.Route;
import data.Solution;

public class Greedy {

	public Solution solution;

	public Greedy(Data data, int timeLimit) {
		
		double startTime = System.currentTimeMillis();
		double currentTime = 0.0;
		
		while (currentTime <= timeLimit) {
			
			int[][] solutionMatrix = new int[data.nodes.length][data.nodes.length];
			List<Route> routes = new ArrayList<Route>();
			List<Node> freeNodes = new ArrayList<Node>(Arrays.asList(data.nodes));
			
			freeNodes.remove(0); //tirando o deposito
			Collections.shuffle(freeNodes);
			
			double solutionCost = 0;
			//construir uma solu��o
			while (!freeNodes.isEmpty()) {
				
				//get primeiro da lista
				Node currentNode = freeNodes.remove(0);
				
				double dist = Double.MAX_VALUE;
				Route currentRoute = null;
				//verificar dist cliente para o deposito
				for (Route r : routes) {
					int indexLastNode = r.nodes.size() - 1;

					double distNodeToNode = data.distances[r.nodes.get(indexLastNode).nodeID][currentNode.nodeID];
					
					if( distNodeToNode < dist && (r.totalDemand + currentNode.demand) <= data.capacityVehicle) {
						dist = distNodeToNode;
						currentRoute = r;
					}
					
				}

				double dist_depot = data.distances[data.nodes[0].nodeID][currentNode.nodeID];

				if(dist_depot < dist || currentRoute == null) {
					Route newRoute = new Route(data.distances);
					newRoute.nodes.add(data.nodes[0]);
					newRoute.nodes.add(currentNode);
					newRoute.totalDemand += currentNode.demand;
					solutionCost += dist_depot;
					solutionMatrix[data.nodes[0].nodeID][currentNode.nodeID] = 1;
					routes.add(newRoute);
				} else if(currentRoute != null) {
					Node lastNode = currentRoute.nodes.get(currentRoute.nodes.size() - 1);
					currentRoute.nodes.add(currentNode);
					currentRoute.totalDemand += currentNode.demand;
					solutionCost += data.distances[lastNode.nodeID][currentNode.nodeID];
					solutionMatrix[lastNode.nodeID][currentNode.nodeID] = 1;
				} /*else {
					Route newRoute = new Route(data.distances);
					newRoute.nodes.add(data.nodes[0]);
					newRoute.nodes.add(currentNode);
					newRoute.totalDemand += currentNode.demand;
					solutionCost += data.distances[data.nodes[0].nodeID][currentNode.nodeID];
					solutionMatrix[data.nodes[0].nodeID][currentNode.nodeID] = 1;
					routes.add(newRoute);
				}*/
			}
			if ((solution == null) || solutionCost < solution.totalCost) {
				solution = new Solution(solutionMatrix, routes, solutionCost, solutionCost, 0.0, "Optimal", 0.0, 0.0);
			}
			currentTime = (System.currentTimeMillis() - startTime) / 1000;
		}
		solution.solvingTime = currentTime;
		
	}	
	
}
