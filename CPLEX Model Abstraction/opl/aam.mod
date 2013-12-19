/* Generated CPLEX model */

// Prediction horizon
int timeHorizon = 3;
range TIMERANGE = 1 .. timeHorizon;
range COMPLETE_TIMERANGE = 0 .. timeHorizon;
range DEF_TIMERANGE = 0 .. timeHorizon+1;

tuple PowerPlantState {	float pInit; int runningInit; };/* type for intervals */
tuple IntervalType {
float lower;
float upper;
};

float jitter = 0.001;
{string} ControllablePlants = { "AVPP1"};
dvar float energyProduction[ControllablePlants][DEF_TIMERANGE];
dvar boolean running[ControllablePlants][DEF_TIMERANGE];
dexpr float totalProduction[t in DEF_TIMERANGE] = sum(p in ControllablePlants) energyProduction[p][t] ;
PowerPlantState initialData[ControllablePlants] = ...;
float energyConsumption[TIMERANGE] = ...;
range CONSTRAINED_TIMERANGE = 1 .. timeHorizon;
{IntervalType} generalBounds[ControllablePlants] = ...;
{IntervalType} generalHoles[ControllablePlants] = ...;
{IntervalType} temporalBounds[ControllablePlants][CONSTRAINED_TIMERANGE] = ...;
{IntervalType} temporalHoles[ControllablePlants][CONSTRAINED_TIMERANGE] = ...;
int maxBps = ...;
{string} Avpps = { "AVPP1"};
tuple AvppData {
int numBPsPos; int numBPsNeg; float firstInPos; float firstInNeg; float dPlAtFirst; float dNegAtFirst;
};
float slopesPlus[Avpps][1..maxBps+1] = ...;
float breakpointsPlus[Avpps][1..maxBps] = ...;
float slopesNeg[Avpps][1..maxBps+1] = ...;
float breakpointsNeg[Avpps][1..maxBps] = ...;
AvppData avppData[Avpps] = ...;
pwlFunction deltaPlus[p in Avpps] = piecewise(i in 1..avppData[p].numBPsPos) {
 slopesPlus[p][i]->breakpointsPlus[p][i]; slopesPlus[p][avppData[p].numBPsPos+1] 
} (avppData[p].firstInPos, avppData[p].dPlAtFirst);
pwlFunction deltaNeg[p in Avpps] = piecewise(i in 1..avppData[p].numBPsNeg) {
 slopesNeg[p][i]->breakpointsNeg[p][i]; slopesNeg[p][avppData[p].numBPsNeg+1]  
} (avppData[p].firstInNeg, avppData[p].dNegAtFirst);
minimize sum(t in TIMERANGE) abs(totalProduction[t] - energyConsumption[t]);

subject to {
forall (p in ControllablePlants, t in COMPLETE_TIMERANGE) {
  productionHoles : forall ( h in generalHoles[p]) {
  !(energyProduction[p][t] >= h.lower + jitter && energyProduction[p][t] <= h.upper - jitter);
  }
  productionRange : forall ( h in generalBounds[p]) {
    (energyProduction[p][t] >= h.lower && energyProduction[p][t] <= h.upper);
}
}
forall (p in ControllablePlants, t in CONSTRAINED_TIMERANGE) {
   PowerBoundsTemporalConstraint : forall(b in temporalBounds[p][t]) {
     energyProduction[p][t] >= b.lower;
     energyProduction[p][t] <= b.upper;
}
   PowerHolesTemporalConstraint : forall ( h in temporalHoles[p][t]) {
     !(energyProduction[p][t] >= h.lower + jitter && energyProduction[p][t] <= h.upper - jitter);
  }
}
forall(p in Avpps, t in CONSTRAINED_TIMERANGE) {
  energyProduction[p][t+1] >= energyProduction[p][t] => (energyProduction[p][t+1] <= deltaPlus[p](energyProduction[p][t]));
  energyProduction[p][t+1] <= energyProduction[p][t] => (energyProduction[p][t+1] >= deltaNeg[p](energyProduction[p][t]));
}
forall(p in ControllablePlants) {
  energyProduction[p][0] == initialData[p].pInit;
  running[p][0] == (initialData[p].runningInit == 1);
}

}
