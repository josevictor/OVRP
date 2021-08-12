package model;

import static ilog.cplex.IloCplex.DoubleParam.TiLim;
import static ilog.cplex.IloCplex.IntParam.MIPDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import data.Data;
import data.Link;
import data.Node;
import data.Route;
import data.Solution;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class Model {

	private Data data;
	private IloCplex cplex;
	private IloIntVar[] x;
	private IloNumVar[] u;
	private double startTime;
	private double modelCreationTime;
	private double modelSolvingTime;
	private int capacityVehicle;
	public Solution solution;
	List<Link> links;

	public Model(Data data, Solution solution, int[][] solMatrix, double timeLimit) {

		this.startTime = System.currentTimeMillis();

		this.data = data;
		this.solution = solution;
		this.capacityVehicle = data.capacityVehicle;

		try {

			// cria links
			links = new ArrayList<Link>();

			createLinks(links, solMatrix);

			int sizeCustomersWithDepot = data.nodes.length;
			int sizeCustomers = sizeCustomersWithDepot - 1;
			int linksSize = links.size();

			// data
			double[] demand = new double[sizeCustomers];

			//populate demand
			
			for (int i = 1; i < data.nodes.length; i++) {
				demand[i - 1] = data.nodes[i].demand;
			}

			double M = maxValueMatrix(data.distances) + 1.0;

			// r
			double[] costMinNodeToNode = new double[sizeCustomers];

			calculateCostMinNodeToNode(costMinNodeToNode, data.distances, M);

			// qi_barra
			double[] __demand__ = new double[sizeCustomers];

			calculateDemandMaxNodeToNode(__demand__, demand);

			// environment
			cplex = new IloCplex();
			cplex.setParam(MIPDisplay, 0);
			cplex.setParam(TiLim, timeLimit);

			// variables
			x = new IloIntVar[linksSize];
			u = new IloNumVar[sizeCustomersWithDepot]; // incluindo o deposito

			for (int i = 0; i < linksSize; i++) {
				String varName = "(" + links.get(i).nodeSource.nodeID + "," + links.get(i).nodeTarget.nodeID + ")";
				x[i] = cplex.boolVar("x" + varName);
			}

			for (int i = 0; i < sizeCustomersWithDepot; i++) {
				String varName = "(" + data.nodes[i].nodeID + ")";
				u[i] = cplex.numVar(0, Double.MAX_VALUE, "u" + varName);
			}

			// função objetivo
			IloLinearNumExpr expression = cplex.linearNumExpr();
			String exprName = "function_objective";

			for (int i = 0; i < linksSize; i++) {
				expression.addTerm(links.get(i).distance, x[i]);
			}

			cplex.addMinimize(expression, exprName);

			// (2) OK
			for (int j = 1; j < sizeCustomersWithDepot; j++) {
				expression = cplex.linearNumExpr();
				exprName = "expression02(" + j + ")";
				for (int l = 0; l < linksSize; l++) {
					Link link = links.get(l);
					if (link.nodeTarget.nodeID == j) {
						expression.addTerm(1, x[l]);
					}
				}
				cplex.addEq(expression, 1, exprName);
			}

			// (18) OK
			for (int j = 1; j < sizeCustomersWithDepot; j++) {
				expression = cplex.linearNumExpr();
				exprName = "expression18(" + j + ")";
				for (int l = 0; l < linksSize; l++) {
					Link link = links.get(l);
					if (link.nodeTarget.nodeID == j) {
						expression.addTerm(1, x[l]);
					}
					if (link.nodeSource.nodeID == j && link.nodeTarget.nodeID != 0) {
						expression.addTerm(-1, x[l]);
					}
				}
				cplex.addGe(expression, 0, exprName);
			}

			// (19) OK
			for (int l = 0; l < linksSize; l++) {
				Link link = links.get(l);
				for (int l2 = l + 1; l2 < linksSize; l2++) {
					Link link2 = links.get(l2);
					if ((link.nodeSource.nodeID == link2.nodeTarget.nodeID)
							&& (link.nodeTarget.nodeID == link2.nodeSource.nodeID)) {
						expression = cplex.linearNumExpr();
						exprName = "expression19(" + link + "," + link2 + ")";
						expression.addTerm(1, x[l]);
						expression.addTerm(1, x[l2]);
						cplex.addLe(expression, 1, exprName);
					}
				}
			}

			// (20) OK
			expression = cplex.linearNumExpr();
			exprName = "expression20";
			for (int l = 0; l < linksSize; l++) {
				Link link = links.get(l);
				if (link.nodeTarget.nodeID == 0) {
					expression.addTerm(1, x[l]);
				}
			}
			cplex.addEq(expression, 0, exprName);

			// (4)
			for (int l = 0; l < linksSize; l++) {
				expression = cplex.linearNumExpr();
				Link link = links.get(l);
				exprName = "expression04(" + link + ")";

				if (link.nodeSource.nodeID == 0 || link.nodeTarget.nodeID == 0)
					continue; // evita percorrer link com deposito[0]

				expression.addTerm(1, u[link.nodeSource.nodeID]);
				expression.addTerm(-1, u[link.nodeTarget.nodeID]);
				expression.addTerm(capacityVehicle, x[l]);

				for (int l2 = 0; l2 < linksSize; l2++) {
					Link link2 = links.get(l2);
					if ((link.nodeSource.nodeID == link2.nodeTarget.nodeID)
							&& (link.nodeTarget.nodeID == link2.nodeSource.nodeID)) {
						expression.addTerm(capacityVehicle - demand[link.nodeSource.nodeID - 1]
								- demand[link.nodeTarget.nodeID - 1], x[l2]);
						break;
					}
				}

				cplex.addLe(expression, capacityVehicle - demand[link.nodeTarget.nodeID - 1], exprName);

			}

			// (5)
			for (int i = 1; i < sizeCustomersWithDepot; i++) {
				expression = cplex.linearNumExpr();
				exprName = "expression05(" + i + ")";
				expression.addTerm(1, u[i]);
				for (int l = 0; l < linksSize; l++) {
					Link link = links.get(l);
					if (link.nodeSource.nodeID == 0 || link.nodeTarget.nodeID == 0)
						continue; // evita percorrer link com deposito[0]
					if (link.nodeTarget.nodeID == i) {
						// expression.addTerm(-demand[link.nodeTarget.nodeID - 1], x[l]);
						expression.addTerm(-demand[link.nodeSource.nodeID - 1], x[l]);
					}
				}
				cplex.addGe(expression, demand[i - 1], exprName);
			}

			// (6)
			for (int i = 1; i < sizeCustomersWithDepot; i++) {
				expression = cplex.linearNumExpr();
				exprName = "expression06(" + i + ")";
				expression.addTerm(1, u[i]);
				for (int l = 0; l < linksSize; l++) {
					Link link = links.get(l);
					if (link.nodeSource.nodeID == 0 && link.nodeTarget.nodeID == i) {
						expression.addTerm(capacityVehicle - demand[i - 1] - __demand__[i - 1], x[l]);
					}
					if (link.nodeSource.nodeID == i && link.nodeTarget.nodeID > 0) {
						expression.addTerm(demand[link.nodeTarget.nodeID - 1], x[l]);
					}
				}
				cplex.addLe(expression, capacityVehicle, exprName);
			}

			// (7)
			expression = cplex.linearNumExpr();
			exprName = "expression07(" + u[0] + ")";
			expression.addTerm(1, u[0]);
			cplex.addEq(expression, 0, exprName);

			// (8) e (9)
			for (int i = 1; i < sizeCustomersWithDepot; i++) {
				for (int l = 0; l < linksSize; l++) {
					Link link = links.get(l);
					if (link.nodeSource.nodeID == 0 && link.nodeTarget.nodeID == i) {
						expression = cplex.linearNumExpr();
						exprName = "expression08(" + i + ")";
						expression.addTerm(M, x[l]);
						cplex.addGe(expression, costMinNodeToNode[i - 1] - link.distance, exprName);
						break;
					}
				}
			}

			// (10)
			double demandaTotal = Arrays.stream(demand).sum();

			expression = cplex.linearNumExpr();
			exprName = "expression10";
			for (int l = 0; l < linksSize; l++) {
				Link link = links.get(l);
				if (link.nodeSource.nodeID == 0) {
					expression.addTerm(1, x[l]);
				}
			}

			cplex.addGe(expression, Math.ceil((demandaTotal / capacityVehicle)), exprName);

			// MIP start
			if (solution != null) {
				IloIntVar[] startVar = new IloIntVar[links.size()];
				double[] startVal = new double[links.size()];
				for (int l = 0; l < links.size(); l++) {
					startVar[l] = x[l];
				}
				for (int r = 0; r < solution.routes.size(); r++) {
					Route route = solution.routes.get(r);
					for (int n = 0; n < route.nodes.size() - 1; n++) {
						Node node = route.nodes.get(n);
						Node next = route.nodes.get(n + 1);
						for (int l = 0; l < links.size(); l++) {
							Link link = links.get(l);
							if (link.nodeSource.nodeID == node.nodeID && link.nodeTarget.nodeID == next.nodeID) {
								startVal[l] = 1.0;
							}
						}
					}
				}
				cplex.addMIPStart(startVar, startVal);
				startVar = null;
				startVal = null;
			}
			modelCreationTime = (System.currentTimeMillis() - startTime) / 1000;

			// solve
			/*
			 * if (cplex.solve()) { System.out.println("Solução: " + cplex.getObjValue());
			 * for (int i = 0; i < x.length; i++) { if (cplex.getValue(x[i]) == 1) {
			 * System.out.println(links.get(i)); } } }
			 * 
			 * // cplex.exportModel("./model/model.lp");
			 * 
			 * // imprimir solução
			 * 
			 * if (cplex.solve()) { for (int i = 0; i < sizeCustomers; i++) {
			 * System.out.print(__demand__[i] + " "); }
			 * 
			 * System.out.println();
			 * 
			 * for (int i = 0; i < sizeCustomers; i++) {
			 * System.out.print(costMinNodeToNode[i] + " "); }
			 
			if (cplex.solve()) {
				System.out.println("\nValor objetivo: " + cplex.getObjValue() + "\n");
				  
				 for (int i = 0; i < links.size(); i++) { if (cplex.getValue(x[i]) > 0.9999) {
					 System.out.println(links.get(i).nodeSource.nodeID + " -> " +
							 links.get(i).nodeTarget.nodeID); 
					} } //}
			}*/
			 
			 

		} catch (IloException e) {
			System.out.println("Message error: " + e.getMessage());
		}

	}

	private void createLinks(List<Link> links, int[][] solutionMatrix) {

		for (int i = 0; i < data.nodes.length; i++) {
			for (int j = 0; j < data.nodes.length; j++) {
				if (i != j && solutionMatrix[i][j] >= 1) {
					Link link = new Link(data.nodes[i], data.nodes[j], data.distances[i][j]);
					links.add(link);
				}
			}
		}

	}

	private void calculateDemandMaxNodeToNode(double[] _d, double[] demand) {

		int length = demand.length;
		double max = -1;
		for (int i = 0; i < length; i++) {
			for (int j = 0; j < length; j++) {
				if (i != j && demand[j] > max) {
					max = demand[j];
				}
			}
			_d[i] = max;
			max = -1;
		}

	}

	private void calculateCostMinNodeToNode(double[] costMinNodeToNode, double[][] matrixDistance, double M) {

		double min = M;
		int length = costMinNodeToNode.length;

		for (int i = 0; i < length; i++) {

			for (int j = 0; j < length; j++) {

				if (i != j && matrixDistance[j + 1][i + 1] < min) {
					min = matrixDistance[j + 1][i + 1];
				}

			}
			costMinNodeToNode[i] = min;
			min = M;
		}
	}

	private double maxValueMatrix(double[][] matrixDistance) {

		double max = Arrays.stream(matrixDistance[0]).max().getAsDouble();

		for (int i = 1; i < matrixDistance.length; i++) {
			double maxCurrent = Arrays.stream(matrixDistance[i]).max().getAsDouble();

			if (maxCurrent > max) {
				max = maxCurrent;
			}
		}
		return max;
	}

	public boolean solveModel() throws IloException {
		startTime = System.currentTimeMillis();
		if (cplex.solve()) {
			modelSolvingTime = (System.currentTimeMillis() - startTime) / 1000;
			buildSolution();
			return true;
		}
		modelSolvingTime = (System.currentTimeMillis() - startTime) / 1000;
		return false;
	}

	public void buildSolution() throws IloException {
		int[][] solutionMatrix = new int[data.nodes.length][data.nodes.length];
		List<Route> routes = new ArrayList<Route>();
		for (int l = 0; l < links.size(); l++) {
			Link link = links.get(l);
			if (link.nodeSource.nodeID == 0 && cplex.getValue(x[l]) > 0.9999) {
				solutionMatrix[link.nodeSource.nodeID][link.nodeTarget.nodeID] = 1;
				Route currentRoute = new Route(data.distances);
				currentRoute.nodes.add(link.nodeSource);
				Node destination = link.nodeTarget;

				boolean endRoute = false;
				
				while(!endRoute) {
					endRoute = true;
					for (int m = 0; m < links.size(); m++) {
						Link next = links.get(m);
						if (next.nodeSource.nodeID == destination.nodeID && cplex.getValue(x[m]) > 0.9999) {
							solutionMatrix[next.nodeSource.nodeID][next.nodeTarget.nodeID] = 1;
							currentRoute.nodes.add(next.nodeSource);
							endRoute = false;
							destination = next.nodeTarget;
							break;
						}
					}
				}
				currentRoute.nodes.add(destination);
				routes.add(currentRoute);
			}
		}
		solution = new Solution(solutionMatrix, routes, cplex.getObjValue(), cplex.getBestObjValue(),
				Math.abs(cplex.getObjValue() - cplex.getBestObjValue()) / ((1E-10) + Math.abs(cplex.getObjValue())),
				cplex.getStatus().toString(), modelCreationTime, modelSolvingTime);
	}

	public void exportModel() throws IloException {
		cplex.exportModel("./model/" + data.filename + ".lp");
	}

	public void finalizeModel() throws IloException {
		cplex.end();
	}

}
