/*********************************************
 * OPL 12.2 Model
 * Author: steghoja
 * Creation Date: 03.05.2012 at 14:59:51
 *********************************************/
 
 /* AVPP ident */
 // AVPP_1
 /* END ident */

 /* Types */
 tuple IntervalType {
  float lower;
  float upper;
};
 /* END Types */

 /* Model list */
 {string} plants = ...;
 /* END Model list */

{IntervalType} feasibleRegions[plants] = ...;
{string} softConstraints = ...;

/* Constants */
 int LAST_SIMULATION_STEP = 10;
 range TIMERANGE = 0..LAST_SIMULATION_STEP;
 float loadCurve[TIMERANGE] = [200.0, 250.0, 230.0, 247.0, 349.0, 551.0, 463.0, 410.0, 380.0, 270.0, 217.0];
 /* END Constants */
  
/* Decision variables */
 dvar float production[plants][TIMERANGE];
 dvar int consStopping[plants][TIMERANGE];
 dvar int consRunning[plants][TIMERANGE];
 dvar int+ penalties[softConstraints][TIMERANGE];
/* END Decision variables */
 
 /* Decision Expressions */
 dexpr float totalProduction[t in TIMERANGE] = sum ( p in plants ) production[p][t];
 dexpr int inRange[p in plants][t in TIMERANGE] = sum(f in feasibleRegions[p]) (production[p][t] >= f.lower && production[p][t] <= f.upper);
 dexpr float P_max[p in plants] = max(f in feasibleRegions[p]) f.upper;
 dexpr float loadFactor[p in plants][t in TIMERANGE] = production[p][t] / P_max[p];
 dexpr float minLoadFact[t in TIMERANGE] = min(p in plants) loadFactor[p][t];
 dexpr float maxLoadFact[t in TIMERANGE] = max(p in plants) loadFactor[p][t];
 dexpr int running[p in plants][t in TIMERANGE] = !(production[p][t] == 0);   // REPLACE
 dexpr float violation = sum(t in TIMERANGE) abs(totalProduction[t]-loadCurve[t]);
 dexpr float penaltySum = sum(t in TIMERANGE, c in softConstraints) penalties[c][t];
 dexpr float penaltyPerStep[t in TIMERANGE] = sum(c in softConstraints) penalties[c][t];
 /* END Decision Expressions */
 
 minimize violation;
 // minimize penaltySum;
 
 subject to {
   forall ( t in TIMERANGE ) {
     oc1: minLoadFact[t] >= 0.4; 
     oc2: maxLoadFact[t] <= 0.6; 
     forall(p in plants) {
 		inRange[p][t] == 1;
 	 }	
   }        
 }   
