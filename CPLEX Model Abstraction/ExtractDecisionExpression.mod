/*********************************************
 * OPL 12.4 Model
 * Author: alexander
 * Creation Date: Sep 4, 2013 at 12:25:05 AM
 *********************************************/

 {string} ControllablePlants = {"a", "b"};
 int t = 10;
 range TIMERANGE = 1..t; 
 dvar float energyProduction[ControllablePlants][TIMERANGE] in 0.0..500.0;
 dexpr float totalProduction[t in TIMERANGE] = sum(p in ControllablePlants) energyProduction[p][t];
 dvar int running[ControllablePlants][t in TIMERANGE];
 
 maximize sum (t in TIMERANGE) totalProduction[t];