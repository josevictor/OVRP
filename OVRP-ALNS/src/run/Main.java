package run;

import java.io.IOException;
import alns.Greedy;
import data.Data;
import data.Solution;
import ilog.concert.IloException;
import model.Model;
import solver.Solver;

public class Main {

	public static void main(String[] args) throws IloException, IOException {

		run();
		
		//runTest();
		
	}
	
	private static void run() {
		new Solver();
	}

	private static void runTest()  throws IloException, IOException {
		Data data = new Data("data", 0);

		int[][] solMatrix = new int[6][6];

		// Imprimir informações

		System.out.println("capacidade veic: " + data.capacityVehicle);

		System.out.print("\nMatriz: \n");
		for (int i = 0; i < data.distances.length; i++) {
			System.out.print("[");
			for (int j = 0; j < data.distances.length; j++) {
				System.out.printf("%-10f", data.distances[i][j]);
				solMatrix[i][j] = (int) data.distances[i][j];
			}
			System.out.print("]\n");
		}

		System.out.println();
		for (int i = 0; i < data.nodes.length; i++) {
			System.out.println(data.nodes[i].nodeID + " | " + data.nodes[i].demand);
		}
		System.out.println("\n\n");

		// Solver Greedy
		//Solution greedy = new Greedy(data, 1).solution;
		//greedy.exportSolutionFile("./solutions/greedyCMT1", 0, 0, 0);

		// Solver Model
		// Model model = new Model(data, greedy, greedy.matrix, 10);
		Model model = new Model(data, null, solMatrix, 10);
		if (model.solveModel()) {
			model.solution.exportSolutionFile("./solucoes/modelCMTXXX.sol", 0, 0, 0);
		}
		model.finalizeModel();
	}

}
