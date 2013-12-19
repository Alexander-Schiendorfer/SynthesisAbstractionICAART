// Predefined Constraints
 int LAST_SIMULATION_STEP = 10;
 range TIMERANGE = 1..LAST_SIMULATION_STEP;
 float loadCurve[TIMERANGE] = [10.0, 12.0, 14.0, 15.0, 13.0, 12.0, 10.0];

// Model-specific constants
// Constants for model CPP1_T.mod
float P_min_CPP1_T = 50.0;
float P_max_CPP1_T = 100.0;
float powerInit_CPP1_T = 0.0;
int consRunningInit_CPP1_T = 0;
int consStandingInit_CPP1_T = 1;
int minOffTime_CPP1_T = 2;
float rateOfChange_CPP1_T = 0.15;
float pricePerKWh_CPP1_T = 13.0;

// Constants for model CPP2_T.mod
float P_min_CPP2_T = 15.0;
float P_max_CPP2_T = 35.0;
float powerInit_CPP2_T = 18.0;
int consRunningInit_CPP2_T = 1;
int consStandingInit_CPP2_T = 0;
int minOffTime_CPP2_T = 2;
float rateOfChange_CPP2_T = 0.125;
float pricePerKWh_CPP2_T = 70.0;

// Constants for model CPP3_T.mod
float P_min_CPP3_T = 50.0;
float P_max_CPP3_T = 100.0;
float powerInit_CPP3_T = 250.0;
int consRunningInit_CPP3_T = 1;
int consStandingInit_CPP3_T = 0;
int minOffTime_CPP3_T = 2;
float rateOfChange_CPP3_T = 0.2;
float pricePerKWh_CPP3_T = 6.0;

// Model List
 {string} plants = { "CPP1_T","CPP2_T","CPP3_T" };

{string} softConstraints = {"oc2", "oc1", "rate_of_change_opt_CPP3_T", "economically_optimal_CPP3_T", "economically_good_CPP2_T", "rate_of_change_pref_CPP1_T", "rate_of_change_opt_CPP1_T", "economically_optimal_CPP2_T", "economically_acceptable_CPP2_T", "economically_good_CPP3_T"};
// Predefined Decision Variables
 dvar float production[plants][TIMERANGE];
 dvar float loadFactor[plants][TIMERANGE];
 dvar boolean off[plants][TIMERANGE];
 dvar plantPenalties[softConstraints][plants][TIMERANGE];
 dvar orgPenalties[softConstraints][TIMERANGE];

// Predefined Constraints
subject to {
   forall ( t in TIMERANGE ) {
oc1:((minLoadFact[t] >= 0.4) && plantPenalties["oc1"][p][t] == 0) || (!(minLoadFact[t] >= 0.4) &&  plantPenalties["oc1"][p][t]== 16);
oc2:((maxLoadFact[t] <= 0.6) && plantPenalties["oc2"][p][t] == 0) || (!(maxLoadFact[t] <= 0.6) &&  plantPenalties["oc2"][p][t]== 16);
   }      
   
   

// Model-specific constraints
// Constraints for model CPP1_T.mod
    // Generator limitations - hard constraints
	forall (t in TIMERANGE) {
		max_production_CPP1_T: production["CPP1_T"][t] <= P_max_CPP1_T ; 
		min_production_CPP1_T: production["CPP1_T"][t] >= P_min_CPP1_T || (running[t] == true && production["CPP1_T"][t] == 0);
	}   	
	
	forall (t in 0..LAST_SIMULATION_STEP-1) {	
		rate_of_change_CPP1_T: abs(production["CPP1_T"][t] - production["CPP1_T"][t+1]) <= production["CPP1_T"][t]  * rateOfChange_CPP1_T;
rate_of_change_opt_CPP1_T:((abs(production["CPP1_T"][t] - production["CPP1_T"][t+1]) <= production["CPP1_T"][t]  * 0.07) && plantPenalties["rate_of_change_opt_CPP1_T"][p][t] == 0) || (!(abs(production["CPP1_T"][t] - production["CPP1_T"][t+1]) <= production["CPP1_T"][t]  * 0.07) &&  plantPenalties["rate_of_change_opt_CPP1_T"][p][t]== 2);
rate_of_change_pref_CPP1_T:((abs(production["CPP1_T"][t] - production["CPP1_T"][t+1]) <= production["CPP1_T"][t]  * 0.10) && plantPenalties["rate_of_change_pref_CPP1_T"][p][t] == 0) || (!(abs(production["CPP1_T"][t] - production["CPP1_T"][t+1]) <= production["CPP1_T"][t]  * 0.10) &&  plantPenalties["rate_of_change_pref_CPP1_T"][p][t]== 1);
		
		switch_off_CPP1_T: (running[t] == true && running[t+1] == false) => production["CPP1_T"][t] == P_min_CPP1_T;
		switch_on_CPP1_T: (running[t] == false && running[t+1] == true) => (consStanding[t] - minOffTime_CPP1_T) >= 0;
		cons_stand1_CPP1_T: (running[t] == false && running[t+1] == false) => consStanding[t+1] == consStanding[t] + 1;
		cons_stand2_CPP1_T: (running[t] == false && running[t+1] == true) => consStanding[t+1] == 0;
		cons_run1_CPP1_T: (running[t] == true && running[t+1] == true) => consRunning[t+1] == consRunning[t] + 1;
		cons_run2_CPP1_T: (running[t] == true && running[t+1] == false) => consRunning[t+1] == 0; 
	}

// Constraints for model CPP2_T.mod
    // Generator limitations - hard constraints
	forall (t in TIMERANGE) {
		max_production_CPP2_T: production["CPP2_T"][t] <= P_max_CPP2_T ; 
		min_production_CPP2_T: production["CPP2_T"][t] >= P_min_CPP2_T || (running[t] == true && production["CPP2_T"][t] == 0);
economically_optimal_CPP2_T:((production["CPP2_T"][t] >= 22 && production["CPP2_T"][t] <= 25) && plantPenalties["economically_optimal_CPP2_T"][p][t] == 0) || (!(production["CPP2_T"][t] >= 22 && production["CPP2_T"][t] <= 25) &&  plantPenalties["economically_optimal_CPP2_T"][p][t]== 4);
economically_good_CPP2_T:((production["CPP2_T"][t] >= 20 && production["CPP2_T"][t] <= 30) && plantPenalties["economically_good_CPP2_T"][p][t] == 0) || (!(production["CPP2_T"][t] >= 20 && production["CPP2_T"][t] <= 30) &&  plantPenalties["economically_good_CPP2_T"][p][t]== 2);
economically_acceptable_CPP2_T:((production["CPP2_T"][t] >= 18 && production["CPP2_T"][t] <= 33) && plantPenalties["economically_acceptable_CPP2_T"][p][t] == 0) || (!(production["CPP2_T"][t] >= 18 && production["CPP2_T"][t] <= 33) &&  plantPenalties["economically_acceptable_CPP2_T"][p][t]== 1);
	}   	
	
	forall (t in 0..LAST_SIMULATION_STEP-1) {	
		rate_of_change_CPP2_T: abs(production["CPP2_T"][t] - production["CPP2_T"][t+1]) <= production["CPP2_T"][t]  * rateOfChange_CPP2_T;
		switch_off_CPP2_T: (running[t] == true && running[t+1] == false) => production["CPP2_T"][t] == P_min_CPP2_T;
		switch_on_CPP2_T: (running[t] == false && running[t+1] == true) => (consStanding[t] - minOffTime_CPP2_T) >= 0;
		cons_stand1_CPP2_T: (running[t] == false && running[t+1] == false) => consStanding[t+1] == consStanding[t] + 1;
		cons_stand2_CPP2_T: (running[t] == false && running[t+1] == true) => consStanding[t+1] == 0;
		cons_run1_CPP2_T: (running[t] == true && running[t+1] == true) => consRunning[t+1] == consRunning[t] + 1;
		cons_run2_CPP2_T: (running[t] == true && running[t+1] == false) => consRunning[t+1] == 0; 
	}

// Constraints for model CPP3_T.mod
    // Generator limitations - hard constraints
	forall (t in TIMERANGE) {
		max_production_CPP3_T: production["CPP3_T"][t] <= P_max_CPP3_T ; 
		min_production_CPP3_T: production["CPP3_T"][t] >= P_min_CPP3_T || (running[t] == true && production["CPP3_T"][t] == 0);
economically_optimal_CPP3_T:((production["CPP3_T"][t] >= 300.0 && production["CPP3_T"][t] <= 350.0) && plantPenalties["economically_optimal_CPP3_T"][p][t] == 0) || (!(production["CPP3_T"][t] >= 300.0 && production["CPP3_T"][t] <= 350.0) &&  plantPenalties["economically_optimal_CPP3_T"][p][t]== 3);
economically_good_CPP3_T:((production["CPP3_T"][t] >= 280.0 && production["CPP3_T"][t] <= 370.0) && plantPenalties["economically_good_CPP3_T"][p][t] == 0) || (!(production["CPP3_T"][t] >= 280.0 && production["CPP3_T"][t] <= 370.0) &&  plantPenalties["economically_good_CPP3_T"][p][t]== 1);
	}   	
	
	forall (t in 0..LAST_SIMULATION_STEP-1) {	
		rate_of_change_CPP3_T: abs(production["CPP3_T"][t] - production["CPP3_T"][t+1]) <= production["CPP3_T"][t]  * rateOfChange_CPP3_T;
rate_of_change_opt_CPP3_T:((abs(production["CPP3_T"][t] - production["CPP3_T"][t+1]) <= production["CPP3_T"][t]  * 0.1) && plantPenalties["rate_of_change_opt_CPP3_T"][p][t] == 0) || (!(abs(production["CPP3_T"][t] - production["CPP3_T"][t+1]) <= production["CPP3_T"][t]  * 0.1) &&  plantPenalties["rate_of_change_opt_CPP3_T"][p][t]== 1);
		switch_off_CPP3_T: (running[t] == true && running[t+1] == false) => production["CPP3_T"][t] == P_min_CPP3_T;
		switch_on_CPP3_T: (running[t] == false && running[t+1] == true) => (consStanding[t] - minOffTime_CPP3_T) >= 0;
		cons_stand1_CPP3_T: (running[t] == false && running[t+1] == false) => consStanding[t+1] == consStanding[t] + 1;
		cons_stand2_CPP3_T: (running[t] == false && running[t+1] == true) => consStanding[t+1] == 0;
		cons_run1_CPP3_T: (running[t] == true && running[t+1] == true) => consRunning[t+1] == consRunning[t] + 1;
		cons_run2_CPP3_T: (running[t] == true && running[t+1] == false) => consRunning[t+1] == 0; 
	}

 }
