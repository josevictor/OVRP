/*********************************************
 * OPL 12.9.0.0 Model
 * Author: jvsantos
 * Creation Date: 8 de nov de 2020 at 17:16:23
 *********************************************/

 //quantidade de clientes
int numberC = ...;

//capacidade do veiculo
int Q = ...;

//conjunto de clientes
{int} N = {i | i in 1..numberC};

//conjunto de clientes + deposito
{int} V = {i | i in 0..numberC};

//matriz de custo das arestas
int c[V][V] = ...;

//vetor de demanda de cada cliente
int d[N] = ...;

// valor maximo
int M = 1001;

//calcular o custo minimo de viagem de any j para i
int r[i in N] = min(j in N: i != j) c[j][i];

//(qi) -> calcular o qi barra( demanda do client j excluindo o cliente i)
int _d[i in N] = max(j in N : i != j) d[j];

//VARIAVEIS DE DECISAO

//variavel que define se o veiculo fez a rota ou não
dvar boolean x[V][V];

//1 se o custo minimo de viagem para o cliente i saiu do depósito
//dvar boolean y[N];

//variavel que define o limite superior da carga já existente no veiculo ao deixa o nó i
dvar float+ u[V];

//função objetivo
minimize
  sum(i in V, j in V : i != j) c[i][j] * x[i][j];

subject to {

    //(2)
    forall(j in N){
		sum (i in V : i != j) x[i][j] == 1;
	}

    //(18)
    forall(j in N){
		sum (i in V : i != j) x[i][j] - (sum (i in N : i != j) x[j][i]) >= 0;	
	}

    //(19)
    forall(i in V, j in V : i != j){
        x[i][j] + x[j][i] <= 1;
    }

    //(20)
    sum(j in N) x[j][0] == 0;

    //(4)
    forall(i in N, j in N : i != j) {
        u[i] - u[j] + Q*x[i][j] + (Q - d[i] - d[j]) * x[j][i] <= Q - d[j];
    }

    //(5)
    forall(i in N){
    	u[i] - sum(j in N : i != j) d[j] * x[j][i] >= d[i];
    }

    //(6)
    forall(i in N) {
        u[i] + (Q - d[i] - _d[i]) * x[0][i] + sum(j in N: i != j) d[j] * x[i][j] <= Q;
    }

    //(7)
    u[0] == 0;

    //nova
    forall(i in N) {
        M * x[0][i] >= r[i] - c[0][i];
    }
    
    /*(8)
    forall(i in N) {
        c[0][i] + M * y[i] >= r[i];
    }
	
    //(9)
    forall(i in N){
        x[0][i] >= y[i];
    }*/
	
    //(10)
    sum(i in N) x[0][i] >= ceil( ( sum(i in N) d[i] ) / Q);
}

execute solucao {
  		
  		for(var i in N) {
  			write(_d[i], " ");	  
  		}
  		writeln();
  		for(var i in N) {
  			write(r[i], " ");	  
  		}
  		writeln();
		for(var i in V) {
			for(var j in V) {
				if (i != j && x[i][j] == 1) {
					writeln(i, " -> ", j);
				}			
 			}
 		}
}