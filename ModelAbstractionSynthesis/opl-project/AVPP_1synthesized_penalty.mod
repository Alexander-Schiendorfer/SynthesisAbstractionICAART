// Predefined Constraints
 int LAST_SIMULATION_STEP = 10;
 range TIMERANGE = 0..LAST_SIMULATION_STEP;
 float loadCurve[TIMERANGE] = [200.0, 250.0, 230.0, 247.0, 349.0, 551.0, 463.0, 410.0, 380.0, 270.0, 217.0];

// Model-specific constants
// Constants for model a.mod
float P_min_a = 50.0;
float P_max_a = 100.0;
float productionInit_a = 0.0;
float loadFactorInit_a = 0;
int consRunningInit_a = 0;
int runningInit_a = 0;
int consStoppingInit_a = 1;
int minOffTime_a = 2;
float rateOfChange_a = 0.15;
float pricePerKWh_a = 13.0;

// Constants for model b.mod
float P_min_b = 15.0;
float P_max_b = 35.0;
float productionInit_b = 18.0;
float loadFactorInit_b = productionInit_b/P_max_b;
int consRunningInit_b = 1;
int runningInit_b = 1;
int consStoppingInit_b = 0;
int minOffTime_b = 2;
float rateOfChange_b = 0.125;
float pricePerKWh_b = 70.0;

// Constants for model c.mod
float P_min_c = 200.0;
float P_max_c = 400.0;
float productionInit_c = 250.0;
float loadFactorInit_c = productionInit_c/P_max_c;
int runningInit_c = 1;
int consRunningInit_c = 1;
int consStoppingInit_c = 0;
int minOffTime_c = 2;
float rateOfChange_c = 0.2;
float pricePerKWh_c = 6.0;

// Model List
 {string} plants = { "a","b","c" };

{string} softConstraints = {"c1_economically_optimal_b", "c1_economically_optimal_c", "c1_rateOfChangeOpt_a", "c2_economically_good_b", "c2_economically_good_c", "c2_rateOfChangePref_a", "c3_economically_acc_b", "c3_rate_of_change_opt_c", "oc1", "oc2"};
// Predefined Decision Variables
 dvar float production[plants][TIMERANGE];
 dvar boolean running[plants][TIMERANGE];   // plant not running?
 dvar float loadFactor[plants][TIMERANGE];
 dvar int consStopping[plants][TIMERANGE];
 dvar int consRunning[plants][TIMERANGE];
 dvar int+ penalties[softConstraints][TIMERANGE];

// Predefined Decision Expressions
 /* Decision Expressions */
 dexpr float totalProduction[t in TIMERANGE] = sum ( p in plants ) production[p][t];
 dexpr float minLoadFact[t in TIMERANGE] = min(p in plants) loadFactor[p][t];
 dexpr float maxLoadFact[t in TIMERANGE] = max(p in plants) loadFactor[p][t];
 dexpr float violation = sum(t in TIMERANGE) abs(totalProduction[t]-loadCurve[t]);
 dexpr float penaltySum = sum(t in TIMERANGE, c in softConstraints) penalties[c][t];
 dexpr float penaltyPerStep[t in TIMERANGE] = sum(c in softConstraints) penalties[c][t];
 dexpr float penaltyCount[c in softConstraints][t in TIMERANGE] = penalties[c][t] != 0;
 dexpr float sumConstraintViolations = sum(t in TIMERANGE, c in softConstraints) penaltyCount[c][t];
 /* END Decision Expressions */
 
 //minimize violation;
 minimize penaltySum;
 

// Predefined Constraints
subject to {
  violation <= 337.29;
   forall ( t in TIMERANGE ) {
oc1:((minLoadFact[t] >= 0.4) && penalties["oc1"][t] == 0) || (!(minLoadFact[t] >= 0.4) &&  penalties["oc1"][t]== 16);
oc2:((maxLoadFact[t] <= 0.6) && penalties["oc2"][t] == 0) || (!(maxLoadFact[t] <= 0.6) &&  penalties["oc2"][t]== 16);
   }        

// Model-specific constraints
// Constraints for model a.mod
	forall (t in TIMERANGE) {
		max_production_a: production["a"][t] <= P_max_a ; 
		min_production_a: (running["a"][t] == true && production["a"][t] >= P_min_a) || (running["a"][t] == false && production["a"][t] == 0);
		fixLoadFactor_a: loadFactor["a"][t] == production["a"][t] /P_max_a;
	}   	
	
	forall (t in 0..LAST_SIMULATION_STEP-1) {	
		rate_of_change_a:  (running["a"][t] == 1) => abs(production["a"][t] - production["a"][t+1]) <= production["a"][t]  * rateOfChange_a;
c1_rateOfChangeOpt_a:(((running["a"][t] == 1) => abs(production["a"][t] - production["a"][t+1]) <= production["a"][t]  * 0.07) && penalties["c1_rateOfChangeOpt_a"][t] == 0) || (!((running["a"][t] == 1) => abs(production["a"][t] - production["a"][t+1]) <= production["a"][t]  * 0.07) &&  penalties["c1_rateOfChangeOpt_a"][t]== 2);
c2_rateOfChangePref_a:(((running["a"][t] == 1) => abs(production["a"][t] - production["a"][t+1]) <= production["a"][t]  * 0.10) && penalties["c2_rateOfChangePref_a"][t] == 0) || (!((running["a"][t] == 1) => abs(production["a"][t] - production["a"][t+1]) <= production["a"][t]  * 0.10) &&  penalties["c2_rateOfChangePref_a"][t]== 1);
		
		switch_off_a: (running["a"][t] == true && running["a"][t+1] == false) => production["a"][t] == P_min_a;
		switch_on_a: (running["a"][t] == false && running["a"][t+1] == true) => (consStopping["a"][t] - minOffTime_a) >= 0;
		switch_on_min_a: (running["a"][t] == false && running["a"][t+1] == true) => production["a"][t+1] == P_min_a;
		
		consrun_const_a:(running["a"][t+1] == 1 && consRunning["a"][t+1] == (1 + consRunning["a"][t])) || (running["a"][t+1] == 0 && consRunning["a"][t+1] == 0);
        consstop_const_a:(running["a"][t+1] == 0 && consStopping["a"][t+1] == (1 + consStopping["a"][t])) || (running["a"][t+1] == 1 && consStopping["a"][t+1] == 0);
	}

// Constraints for model b.mod
	forall (t in TIMERANGE) {
		max_production_b: production["b"][t] <= P_max_b ; 
		min_production_b: (running["b"][t] == true && production["b"][t] >= P_min_b) || (running["b"][t] == false && production["b"][t] == 0);
c1_economically_optimal_b:((production["b"][t] >= 22 && production["b"][t] <= 25) && penalties["c1_economically_optimal_b"][t] == 0) || (!(production["b"][t] >= 22 && production["b"][t] <= 25) &&  penalties["c1_economically_optimal_b"][t]== 4);
c2_economically_good_b:((production["b"][t] >= 20 && production["b"][t] <= 30) && penalties["c2_economically_good_b"][t] == 0) || (!(production["b"][t] >= 20 && production["b"][t] <= 30) &&  penalties["c2_economically_good_b"][t]== 2);
c3_economically_acc_b:((production["b"][t] >= 18 && production["b"][t] <= 33) && penalties["c3_economically_acc_b"][t] == 0) || (!(production["b"][t] >= 18 && production["b"][t] <= 33) &&  penalties["c3_economically_acc_b"][t]== 1);
	    fixLoadFactor_b:loadFactor["b"][t] == production["b"][t] /P_max_b;
	}   	
	
	forall (t in 0..LAST_SIMULATION_STEP-1) {	
		rate_of_change_b: (running["b"][t] == 1) => abs(production["b"][t] - production["b"][t+1]) <= production["b"][t]  * rateOfChange_b;
		switch_off_b: (running["b"][t] == true && running["b"][t+1] == false) => production["b"][t] == P_min_b;
		switch_on_b: (running["b"][t] == false && running["b"][t+1] == true) => (consStopping["b"][t] - minOffTime_b) >= 0;
		switch_on_min_b: (running["b"][t] == false && running["b"][t+1] == true) => production["b"][t+1] == P_min_b;
		consrun_const_b:(running["b"][t+1] == 1 && consRunning["b"][t+1] == (1 + consRunning["b"][t])) || (running["b"][t+1] == 0 && consRunning["b"][t+1] == 0);
        consstop_const_b:(running["b"][t+1] == 0 && consStopping["b"][t+1] == (1 + consStopping["b"][t])) || (running["b"][t+1] == 1 && consStopping["b"][t+1] == 0);
	}

// Constraints for model c.mod
	forall (t in TIMERANGE) {
		max_production_c: production["c"][t] <= P_max_c ; 
		min_production_c: (running["c"][t] == true && production["c"][t] >= P_min_c) || (running["c"][t] == false && production["c"][t] == 0);
c1_economically_optimal_c:((production["c"][t] >= 300.0 && production["c"][t] <= 350.0) && penalties["c1_economically_optimal_c"][t] == 0) || (!(production["c"][t] >= 300.0 && production["c"][t] <= 350.0) &&  penalties["c1_economically_optimal_c"][t]== 3);
c2_economically_good_c:((production["c"][t] >= 280.0 && production["c"][t] <= 370.0) && penalties["c2_economically_good_c"][t] == 0) || (!(production["c"][t] >= 280.0 && production["c"][t] <= 370.0) &&  penalties["c2_economically_good_c"][t]== 1);
	    fixLoadFactor_c: loadFactor["c"][t] == production["c"][t] /P_max_c;
	}   	
	
	forall (t in 0..LAST_SIMULATION_STEP-1) {	
		rate_of_change_c: (running["c"][t] == 1) => abs(production["c"][t] - production["c"][t+1]) <= production["c"][t]  * rateOfChange_c;
c3_rate_of_change_opt_c:(((running["c"][t] == 1) => abs(production["c"][t] - production["c"][t+1]) <= production["c"][t]  * 0.1) && penalties["c3_rate_of_change_opt_c"][t] == 0) || (!((running["c"][t] == 1) => abs(production["c"][t] - production["c"][t+1]) <= production["c"][t]  * 0.1) &&  penalties["c3_rate_of_change_opt_c"][t]== 1);
		switch_off_c: (running["c"][t] == true && running["c"][t+1] == false) => production["c"][t] == P_min_c;
		switch_on_c: (running["c"][t] == false && running["c"][t+1] == true) => (consStopping["c"][t] - minOffTime_c) >= 0;
		switch_on_min_c: (running["c"][t] == false && running["c"][t+1] == true) => production["c"][t+1] == P_min_c;
		consrun_const_c:(running["c"][t+1] == 1 && consRunning["c"][t+1] == (1 + consRunning["c"][t])) || (running["c"][t+1] == 0 && consRunning["c"][t+1] == 0);
        consstop_const_c:(running["c"][t+1] == 0 && consStopping["c"][t+1] == (1 + consStopping["c"][t])) || (running["c"][t+1] == 1 && consStopping["c"][t+1] == 0);
	}

// Initial state constraints
production["a"][0] == productionInit_a;
running["a"][0] == runningInit_a;
loadFactor["a"][0] == loadFactorInit_a;
consStopping["a"][0] == consStoppingInit_a;
consRunning["a"][0] == consRunningInit_a;
production["b"][0] == productionInit_b;
running["b"][0] == runningInit_b;
loadFactor["b"][0] == loadFactorInit_b;
consStopping["b"][0] == consStoppingInit_b;
consRunning["b"][0] == consRunningInit_b;
production["c"][0] == productionInit_c;
running["c"][0] == runningInit_c;
loadFactor["c"][0] == loadFactorInit_c;
consStopping["c"][0] == consStoppingInit_c;
consRunning["c"][0] == consRunningInit_c;
 }

 
 execute {
   function cut(r) {
	  return Math.round(r*100)/100;
   }

 	writeln('Violation: ', violation);
 	writeln('Penaties: ', penaltySum);  
 	writeln('Constraints violated: ', sumConstraintViolations);
 	for(var c in softConstraints) {
 	  	write(c, " & ");
	 	for(var t in TIMERANGE) {
	 	    if(t > 0)
	 	    	write(" & ");
	    	write(penalties[c][t]);
	    } 
	    writeln("\\\\");
  	}    	  
  	
  	write("total & ");
    for(t in TIMERANGE) {
    	if(t > 0) 
		   write(" & ");
	    write(penaltyPerStep[t]);
    }      
    writeln("\\\\");
    
 	writeln('------------------');
 	writeln('Production:');
 	for(var p in plants) {
 	  write(p, " & ");  
 	  for(t in TIMERANGE) {
 	    if(t > 0)
 	    	write(" & ");
    	write(cut(production[p][t]));
	  } 
	  writeln("\\\\");
    } 	  
      	
  	write("total & ");
    for(t in TIMERANGE) {
    	if(t > 0) 
		   write(" & ");
	    write(totalProduction[t]);
    }      
    writeln("\\\\");
    
    write("demand & ");
    for(t in TIMERANGE) {
    	if(t > 0) 
		   write(" & ");
	    write(cut(loadCurve[t]));
    }      
    writeln("\\\\");
    
    write("violation & ");
    for(t in TIMERANGE) {
    	if(t > 0) 
		   write(" & ");
	    write(cut(Math.abs(totalProduction[t]-loadCurve[t])));
    }      
    writeln("\\\\");
    writeln('------------------');
 	writeln('Running:');
 	for(p in plants) {
 	  write(p, " & ");  
 	  for( t in TIMERANGE) {
 	    if(t > 0)
 	    	write(" & ");
	 	write(running[p][t]);
	  } 
	  writeln("\\\\");
    } 	  
   
    writeln('Production CSV')

    for(t in TIMERANGE) {
	    writeln(t, ";", totalProduction[t], ";", loadCurve[t]);
    }      
 }   
