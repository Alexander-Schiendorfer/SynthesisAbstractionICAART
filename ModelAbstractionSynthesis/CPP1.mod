/*********************************************
 * OPL 12.4 Model
 * Simplistic concrete powerplant
 * Author: Alexander Schiendorfer
 *********************************************/
 
using CPLEX;

int LAST_SIMULATION_STEP = 120;
range TIMERANGE = 1..LAST_SIMULATION_STEP;

 /* Interface parameters */
float P_min = 1.0;
float P_max = 4.0;
float rateOfChange = 1.0;
float pricePerKWh = 1.0;
/* End mode Interface parameters */
 
// state input variables
float P_init = 18.0;

dvar float production[TIMERANGE];  // Production of the plant in kW.
dvar boolean off[TIMERANGE];       // plant not running?
 
int energyConsumption[PowerConsumers][TIMERANGE] = ...;

subject to {
    // Generator limitations - hard constraints
	forall (t in TIMERANGE) {
		max_production: production[t] <= P_max ; 
		min_production: production[t] >= P_min || (off[t] == true && production[t] == 0);
	}   	
	forall (t in 1..LAST_SIMULATION_STEP-1) {	
		rate_of_change: abs(production[t] - production[t+1]) <= production[t]  * rateOfChange;
	}
}
