package solver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import alns.Greedy;
import data.Data;
import data.Node;
import data.Route;
import data.Solution;
import ilog.concert.IloException;
import model.Model;

public class Solver {

	public Solver() {

		Locale.setDefault(Locale.US);

		String[] filenames = {"CMT1", "CMT2", "CMT3", "CMT4", "CMT5", "CMT6", "CMT7", "CMT8", "CMT9", "CMT10", "CMT11", "CMT12", "CMT13", "CMT14"};
		int inputFormat = 1;

		int[] subprobTimeLimits = { 5, 10, 30};
		int numberOfExecutions = 3;
		Random randomGenerator = new Random();
		int timeLimit = 1800;

		for (String filename : filenames) {
			for (int exec = 1; exec <= numberOfExecutions; exec++) {
				for (int solverTimeLimit : subprobTimeLimits) {
					try {
						String fileDirectory = "./solutionGreedy60/" + filename + String.format("/limit-%02d", solverTimeLimit)
								+ String.format("/exec-%02d", exec);
						new File(fileDirectory).mkdirs();
						
						System.out.println(fileDirectory);
						
						Data data = new Data(filename, inputFormat);
						double startTime = System.currentTimeMillis();
						double processTime;
						double timeToBest = 0;
						int iterToBest = 0;
						int s = 0;
						Solution bestSolution = new Greedy(data, 60).solution;
						processTime = (System.currentTimeMillis() - startTime) / 1000;
						bestSolution.exportSolutionFile(
								fileDirectory + "/" + filename + String.format("-%06d", s) + ".sol", processTime, -1,
								0.00);

						double[] insertionRate = { 0.05, 0.05 };

						int[] operationCounter = new int[2];
						int[] improveCounter = new int[2];
						double[] improveRate = new double[2];
						double[] operationProb = new double[2];
						int operation;

						do {

							int[][] nextMatrix = Arrays.stream(bestSolution.matrix).map(int[]::clone)
									.toArray(int[][]::new);

							double sumOfRates = 0;
							for (int i = 0; i < operationCounter.length; i++) {
								improveRate[i] = (improveCounter[i] + 1.0) / (operationCounter[i] + 1.0);
								sumOfRates += improveRate[i];
							}
							for (int i = 0; i < operationCounter.length; i++) {
								operationProb[i] = improveRate[i] / sumOfRates;
							}
							for (int i = 1; i < operationCounter.length; i++) {
								operationProb[i] = operationProb[i] + operationProb[i - 1];
							}
							if (bestSolution.routes.size() == 1) {
								operation = 0;
							} else {
								double roulete = randomGenerator.nextDouble();
								if (roulete < operationProb[0]) {
									operation = 0;
								} else {
									operation = 1;
								}
							}

							switch (operation) {
							case 0:
								// ROUTE IMPROVEMENT
								int r = randomGenerator.nextInt(bestSolution.routes.size());
								Route route = bestSolution.routes.get(r);
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
							}

							Solution nextSolution = null;
							Model nextModel = new Model(data, bestSolution, nextMatrix, solverTimeLimit);
							if (nextModel.solveModel()) {
								nextSolution = nextModel.solution;
							}
							nextModel.finalizeModel();

							processTime = (System.currentTimeMillis() - startTime) / 1000;

							operationCounter[operation]++;

							if (nextSolution != null) {
								nextSolution.exportSolutionFile(
										fileDirectory + "/" + filename + String.format("-%06d", ++s) + ".sol",
										processTime, operation, insertionRate[operation]);
								if (nextSolution.totalCost < bestSolution.totalCost - 0.01) {
									bestSolution = nextSolution;
									improveCounter[operation]++;
									timeToBest = (System.currentTimeMillis() - startTime) / 1000;
									iterToBest = s;
								}
								if (nextSolution.gap < 0.01) {
									if (insertionRate[operation] <= 0.01) {
										insertionRate[operation] = 0.05;
									} else {
										insertionRate[operation] = Math.min(insertionRate[operation] + 0.05, 1.00);
									}
								} else {
									if (nextSolution.gap > 0.05) {
										insertionRate[operation] = Math.max(insertionRate[operation] - 0.05, 0.01);
									}
								}
							} else {
								bestSolution.exportSolutionFile(
										fileDirectory + "/" + filename + String.format("-%06d", ++s) + ".sol",
										processTime, operation, insertionRate[operation]);
								System.err.println("no solution at iteration " + s);
								insertionRate[operation] = Math.max(insertionRate[operation] - 0.05, 0.01);
							}

						} while (processTime <= timeLimit && s - iterToBest < 100);

						PrintStream printer = new PrintStream(fileDirectory + "/" + filename + ".cnt");
						printer.printf("%-30s%8d%8d%8.2f\n", "ROUTE IMPROVEMENT", operationCounter[0],
								improveCounter[0], insertionRate[0]);
						//printer.printf("%-30s%8d%8d%8.2f\n", "ROUTES COMMUTATION", operationCounter[1],
						//		improveCounter[1], insertionRate[1]);
						//printer.printf("%-30s%8d%8d%8.2f\n", "ROUTES AGGLUTINATION", operationCounter[2],
						//		improveCounter[2], insertionRate[2]);
						printer.printf("%-30s%8d%8d%8.2f\n", "CUSTOMERS CLIQUE", operationCounter[1], improveCounter[1],
								insertionRate[1]);
						printer.printf("%-30s%8.2f\n", "BEST COST", bestSolution.totalCost);
						printer.printf("%-30s%8.2f\n", "PROCESS TIME", processTime);
						printer.printf("%-30s%8.2f\n", "TIME TO BEST", timeToBest);
						printer.printf("%-30s%8d\n", "ITERATIONS", s);
						printer.printf("%-30s%8d\n", "ITERATIONS TO BEST", iterToBest);
						printer.close();
					} catch (IOException exc1) {
						System.err.println(exc1);
					} catch (IloException exc2) {
						System.err.println(exc2);
					} catch (OutOfMemoryError exc3) {
						System.err.println(exc3);
					}
				}

			}
		}
	}
}
