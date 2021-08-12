package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import data.Data;
import data.Node;
import data.Route;
import data.Solution;

public class ALNS {

	public ALNS(Data data, int operation, Solution bestSolution, Route route, double[] insertionRate,
			int[][] nextMatrix) {

		Random randomGenerator = new Random();

		switch (operation) {
			case 0:
				// ROUTE IMPROVEMENT
				// for (r = 0; r < currentSolution.routes.size(); r++) {
				// route = currentSolution.routes.get(r);
				int r = randomGenerator.nextInt(bestSolution.routes.size());
				route = bestSolution.routes.get(r);
				// clique within route
				for (int n1 = 0; n1 < route.nodes.size(); n1++) {
					Node node1 = route.nodes.get(n1);
					for (int n2 = n1 + 1; n2 < route.nodes.size() - 1; n2++) {
						Node node2 = route.nodes.get(n2);
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node1.nodeID][node2.nodeID] = 1;
						}
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node2.nodeID][node1.nodeID] = 1;
						}
					}
				}
				// }
				break;
			case 1:
				// CUSTOMERS CLIQUE
				List<Node> clique = new ArrayList<Node>(Arrays.asList(data.nodes));
				clique.remove(0);
				while (clique.size() > data.nodes.length * insertionRate[operation]) {
					clique.remove(randomGenerator.nextInt(clique.size()));
				}
				// clique within customers
				for (int n1 = 0; n1 < clique.size(); n1++) {
					Node node1 = clique.get(n1);
					for (int n2 = n1 + 1; n2 < clique.size(); n2++) {
						Node node2 = clique.get(n2);
						nextMatrix[node1.nodeID][node2.nodeID] = 1;
						nextMatrix[node2.nodeID][node1.nodeID] = 1;
					}
				}
				break;
			/*
			case 1:
				// ROUTES COMMUTATION
				List<Node> commuters = new ArrayList<Node>();
				List<Node> previous = new ArrayList<Node>();
				List<Node> next = new ArrayList<Node>();
				// determination of the minimum size of the routes
				int minSize = bestSolution.routes.get(0).nodes.size();
				for (r = 1; r < bestSolution.routes.size(); r++) {
					route = bestSolution.routes.get(r);
					minSize = Math.min(minSize, route.nodes.size());
				}
				// index n is between 1 and the index value of the last customer of the smallest
				// route
				int n = randomGenerator.nextInt(minSize - 2) + 1;
				// determination of previous/commuters/next nodes
				for (r = 0; r < bestSolution.routes.size(); r++) {
					route = bestSolution.routes.get(r);
					commuters.add(route.nodes.get(n));
					if (n + 1 < route.nodes.size() - 1) { // valid customer
						commuters.add(route.nodes.get(n + 1));
					}
					previous.add(route.nodes.get(n - 1));
					if (n + 2 < route.nodes.size()) { // valid node
						next.add(route.nodes.get(n + 2));
					} else {
						next.add(route.nodes.get(n + 1)); // node 0
					}
				}
	
				// clique within commuters
				for (int n1 = 0; n1 < commuters.size(); n1++) {
					Node node1 = commuters.get(n1);
					for (int n2 = n1 + 1; n2 < commuters.size(); n2++) {
						Node node2 = commuters.get(n2);
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node1.nodeID][node2.nodeID] = 1;
						}
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node2.nodeID][node1.nodeID] = 1;
						}
					}
				}
				// links from previous to commuters
				for (int n1 = 0; n1 < previous.size(); n1++) {
					Node node1 = previous.get(n1);
					for (int n2 = 0; n2 < commuters.size(); n2++) {
						Node node2 = commuters.get(n2);
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node1.nodeID][node2.nodeID] = 1;
						}
					}
				}
				// links from commuters to next
				for (int n1 = 0; n1 < commuters.size(); n1++) {
					Node node1 = commuters.get(n1);
					for (int n2 = 0; n2 < next.size(); n2++) {
						Node node2 = next.get(n2);
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node1.nodeID][node2.nodeID] = 1;
						}
					}
				}
				break;
			case 2:
				// ROUTES AGGLUTINATION
				int r1 = randomGenerator.nextInt(bestSolution.routes.size());
				int r2 = randomGenerator.nextInt(bestSolution.routes.size());
				while (r1 == r2) {
					r2 = randomGenerator.nextInt(bestSolution.routes.size());
				}
				Route route1 = bestSolution.routes.get(r1);
				Route route2 = bestSolution.routes.get(r2);
				// clique within route1
				for (int n1 = 0; n1 < route1.nodes.size(); n1++) {
					Node node1 = route1.nodes.get(n1);
					for (int n2 = n1 + 1; n2 < route1.nodes.size() - 1; n2++) {
						Node node2 = route1.nodes.get(n2);
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node1.nodeID][node2.nodeID] = 1;
						}
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node2.nodeID][node1.nodeID] = 1;
						}
					}
				}
				// clique within route2
				for (int n1 = 0; n1 < route2.nodes.size(); n1++) {
					Node node1 = route2.nodes.get(n1);
					for (int n2 = n1 + 1; n2 < route2.nodes.size() - 1; n2++) {
						Node node2 = route2.nodes.get(n2);
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node1.nodeID][node2.nodeID] = 1;
						}
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node2.nodeID][node1.nodeID] = 1;
						}
					}
				}
				// cross links between route1 and route2
				for (int n1 = 1; n1 < route1.nodes.size() - 1; n1++) {
					Node node1 = route1.nodes.get(n1);
					for (int n2 = 1; n2 < route2.nodes.size() - 1; n2++) {
						Node node2 = route2.nodes.get(n2);
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node1.nodeID][node2.nodeID] = 1;
						}
						if (randomGenerator.nextDouble() < insertionRate[operation]) {
							nextMatrix[node2.nodeID][node1.nodeID] = 1;
						}
					}
				}
				break;*/
		}

	}

}
