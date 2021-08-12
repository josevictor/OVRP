package data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

public class Data {
	
	private final String DIMENSION = "DIMENSION";
	private final String CAPACITY = "CAPACITY";
	private final String NODE_COORD_SECTION = "NODE_COORD_SECTION";
	private final String DEPOT_SECTION = "DEPOT_SECTION";
	private final String DEMAND_SECTION = "DEMAND_SECTION";
	
	public String filename;
	public Node[] nodes;
	public double[][] distances;
	public int capacityVehicle;

	public Data(String filename, int inputFormat) {
		
		this.filename = filename;

		try {
			//System.out.println(getClass().getResourceAsStream("/data.dat"));
			//Scanner scanner = new Scanner(new FileReader("./data/ovrp/" + filename + ".dat"));
			
			Scanner scanner = new Scanner(getClass().getResourceAsStream("/instances/"+ filename + ".vrp.txt"));
			//Scanner scanner = new Scanner(getClass().getResourceAsStream("/"+ filename + ".dat"));
			
			boolean eof = false;
			boolean readNodes = false;
			boolean readDemands = false;
			
			while(scanner.hasNextLine() && !eof) {
				switch(inputFormat) {
					case 0:
						String[] line = scanner.nextLine().split("=");
						String key = line[0].trim();
						int numberC = 0;
						
						if(key.equalsIgnoreCase("Q")) {
							this.capacityVehicle = Integer.valueOf(line[1].strip());
						} else {
							if(key.equalsIgnoreCase("c")) {
								String[] rows = line[1].split(";");
								int i = 0, j = 0;
								for (String s1 : rows) {
									String[] cols = s1.split(",");
									for (String s2 : cols) {
										distances[i][j++] = Integer.parseInt(s2.strip());
									}
									i++;
									j = 0;
								}
							} 
							else {
								if(key.equalsIgnoreCase("numberC")) {
									numberC = Integer.parseInt(line[1].strip());
									this.distances = new double[numberC+1][numberC+1];
									this.nodes = new Node[numberC+1];
								}
								else {							
									String[] demands = line[1].split(",");
									this.nodes[0] = new Node(0, 0, 0, 0);
									int id = 1;
									for (String s : demands) {
										int demand = Integer.parseInt(s.strip());
										this.nodes[id] = new Node(id++, demand, 0, 0);
									}
								}
							}
						}
						break;
					case 1:
						String readLine = scanner.nextLine();
						if(readLine.strip().equalsIgnoreCase(DEPOT_SECTION)) {
							eof = true;
							readDemands = false;
							break;
						} 
						if(readNodes) {
							//ler nós
							String[] s_line = readLine.split(" ");
							
							if(s_line.length == 3) {								
								int id = Integer.parseInt(s_line[0].strip()) - 1;
								double coordX = Double.parseDouble(s_line[1].strip());
								double coordY = Double.parseDouble(s_line[2].strip());
								
								this.nodes[id] = new Node(id, 0, coordX, coordY);
							}
							
							
						}
						if(readDemands) {
							//ler demandas
							String[] s_line = readLine.split(" ");
							
							if(s_line.length == 2) {
								int id = Integer.parseInt(s_line[0].strip()) - 1;
								int demand = Integer.parseInt(s_line[1].strip());
								
								this.nodes[id].demand = demand;
							}
							
						}
						if(readLine.strip().equalsIgnoreCase(NODE_COORD_SECTION)) {
							readNodes = true;
						}
						if(readLine.strip().equalsIgnoreCase(DEMAND_SECTION)) {
							readNodes = false;
							readDemands = true;
						}
						else {
							String[] s_line = readLine.split(":");
							
							if(s_line[0].strip().equalsIgnoreCase(CAPACITY)) {
								this.capacityVehicle = Integer.valueOf(s_line[1].strip());
							}
							if(s_line[0].strip().equalsIgnoreCase(DIMENSION)) {
								int len = Integer.valueOf(s_line[1].strip());
								this.nodes = new Node[len];
								this.distances = new double[len][len];
							}
						}
						break;
					}
				}
			
			scanner.close();
			
			if(inputFormat == 1) {
				
				for (int i = 0; i < this.nodes.length; i++) {
					Node source = this.nodes[i];
					for (int j = 0; j < this.nodes.length; j++) {
						if(i != j) {
							Node target = this.nodes[j];
							double dist = Math.sqrt(Math.pow(source.coordX - target.coordX, 2) + Math.pow(source.coordY - target.coordY, 2));
							BigDecimal bigDecimal = new BigDecimal(dist);
							bigDecimal = bigDecimal.setScale(6, RoundingMode.HALF_UP);
							this.distances[i][j] = bigDecimal.doubleValue();				
						}
					}
				}
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
