/*********************************************
 * OPL 12.4 Model
 * Simplistic concrete powerplant temporally relevant
 * information
 * Author: Alexander Schiendorfer
 *********************************************/
 
using CPLEX;

int LAST_SIMULATION_STEP = 120;
range TIMERANGE = 0..LAST_SIMULATION_STEP;

 /* Interface parameters */
float P_min = 200.0;
float P_max = 400.0;
/* End mode Interface parameters */

// state input variables
/* Model-specific constants */
float productionInit = 250.0;
float loadFactorInit = productionInit/P_max;
int runningInit = 1;
int consRunningInit = 1;
int consStoppingInit = 0;
/* End Model-specific constants */

// general parameters (non state-specific)
/* Model-specific constants */
int minOffTime = 2;
float rateOfChange = 0.2;
float pricePerKWh = 6.0;
/* End Model-specific constants */

dvar float production[TIMERANGE];  // Production of the plant in kW.
dvar float loadFactor[TIMERANGE];  
dvar int consStopping[TIMERANGE];
dvar int consRunning[TIMERANGE];
dvar boolean running[TIMERANGE];   // plant not running?
 
subject to {
	forall (t in TIMERANGE) {
		c1_economically_optimal: production[t] >= 300.0 && production[t] <= 350.0;
	    c2_economically_good: production[t] >= 280.0 && production[t] <= 370.0;
	}   	
	
	forall (t in 0..LAST_SIMULATION_STEP-1) {	
		rate_of_change: (running[t] == 1) => abs(production[t] - production[t+1]) <= production[t]  * rateOfChange;
		c3_rate_of_change_opt: (running[t] == 1) => abs(production[t] - production[t+1]) <= production[t]  * 0.1;
		switch_off: (running[t] == true && running[t+1] == false) => production[t] == P_min;
		switch_on: (running[t] == false && running[t+1] == true) => (consStopping[t] - minOffTime) >= 0;
		switch_on_min: (running[t] == false && running[t+1] == true) => production[t+1] == P_min;
		consrun_const:(running[t+1] == 1 && consRunning[t+1] == (1 + consRunning[t])) || (running[t+1] == 0 && consRunning[t+1] == 0);
        consstop_const:(running[t+1] == 0 && consStopping[t+1] == (1 + consStopping[t])) || (running[t+1] == 1 && consStopping[t+1] == 0);
	}
}

/* RELATIONSHIPS 
 c1_economically_optimal >> c2_economically_good
 c1_economically_optimal >> c3_rate_of_change_opt
 * End RELATIONSHIPS  */
