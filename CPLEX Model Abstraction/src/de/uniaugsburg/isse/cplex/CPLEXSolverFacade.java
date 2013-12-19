package de.uniaugsburg.isse.cplex;

import ilog.concert.IloException;
import ilog.concert.IloIntMap;
import ilog.concert.IloIntRange;
import ilog.concert.IloIntVarMap;
import ilog.concert.IloMapIndexArray;
import ilog.concert.IloNumDExprMap;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumMap;
import ilog.concert.IloNumVarMap;
import ilog.concert.IloSymbolSet;
import ilog.cp.IloCP;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;
import ilog.cplex.IloCplex.IntParam;
import ilog.opl.IloOplCplexVectors;
import ilog.opl.IloOplDataSource;
import ilog.opl.IloOplErrorHandler;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import ilog.opl.IloOplModelSource;
import ilog.opl.IloOplSettings;
import de.uniaugsburg.isse.solver.SolverFacade;

public class CPLEXSolverFacade implements SolverFacade {
	private IloOplFactory factory;
	private IloCP cp;
	private IloCplex cplex;
	private IloOplModel model;
	private IloSymbolSet controllablePlants;
	private IloIntRange timeRange;
	private IloNumVarMap energyProductionMap;
	private IloIntMap runningMap;
	private IloNumDExprMap totalProductionMap;
	private String settingsFile;
	private boolean solved;
	private boolean isSimplified = false;
	private int timeLimit;
	private boolean usePresolve = false;
	private boolean useInitialSolution = false;

	public boolean isSimplified() {
		return this.isSimplified;
	}

	@Override
	public void setSimplified(boolean isSimplified) {
		this.isSimplified = isSimplified;
	}

	public CPLEXSolverFacade() {
		this.setUpCplex();
	}

	private void setUpCplex() {
		this.factory = new IloOplFactory();
		IloOplFactory.setDebugMode(false);
		try {
			this.cplex = this.factory.createCplex();
			// set cplex parameter
			this.cplex.setParam(IntParam.Symmetry, 0);
			// cplex.setParam(DoubleParam.WorkMem, 8000);

			if (this.timeLimit > 0)
				this.cplex.setParam(DoubleParam.TiLim, this.timeLimit);

			this.cplex.setOut(null);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void solve(String modelFile, String dataFile) {
		if (this.factory == null)
			this.setUpCplex();

		IloOplErrorHandler handler = this.factory.createOplErrorHandler();
		IloOplModelSource modelSource = this.factory.createOplModelSource(modelFile);
		IloOplSettings settings = this.factory.createOplSettings(handler);

		IloOplModelDefinition modelDefinition = this.factory.createOplModelDefinition(modelSource, settings);

		try {
			if (this.timeLimit > 0)
				this.cplex.setParam(DoubleParam.TiLim, this.timeLimit);

			if (this.isSimplified) {
				this.cplex.setParam(DoubleParam.TiLim, 10.0);
				this.cplex.setParam(DoubleParam.TuningTiLim, 5);
				this.cplex.setParam(IntParam.IntSolLim, 5);
				this.cplex.setParam(IntParam.RelaxPreInd, 0);
				this.cplex.setParam(IntParam.PrePass, -1);
				this.cplex.setParam(IntParam.RelaxPreInd, -1);
			} else {
				this.cplex.setParam(DoubleParam.TuningTiLim, 10000);
				this.cplex.setParam(IntParam.IntSolLim, 10000);
				this.cplex.setParam(IntParam.RelaxPreInd, 0);
				this.cplex.setParam(IntParam.RepeatPresolve, 0);
				this.cplex.setParam(IntParam.PrePass, 0);
			}

			if (this.usePresolve) {
				this.cplex.setParam(IntParam.RelaxPreInd, -1);
				this.cplex.setParam(IntParam.PrePass, -1);
				this.cplex.setParam(IntParam.RelaxPreInd, -1);
			}
			// cplex.setOut(null);

		} catch (IloException e) {
			e.printStackTrace();
		}

		this.model = this.factory.createOplModel(modelDefinition, this.cplex);
		if (dataFile != null) {
			IloOplDataSource dataSource = this.factory.createOplDataSource(dataFile);
			this.model.addDataSource(dataSource);
		}

		this.model.generate();

		if (this.useInitialSolution) {
			IloOplCplexVectors vecs = this.factory.createOplCplexVectors();
			IloNumVarMap energyProductionVars = this.model.getElement("energyProduction").asNumVarMap();
			IloNumMap initProductionValues = this.model.getElement("initProduction").asNumMap();

			IloIntVarMap runningVars = this.model.getElement("running").asIntVarMap();
			IloIntMap initRunningValues = this.model.getElement("initRunning").asIntMap();

			vecs.attach(energyProductionVars, initProductionValues);
			vecs.attach(runningVars, initRunningValues);
			vecs.setVectors(this.cplex);
		}
		try {
			long milliSeconds = System.currentTimeMillis();
			this.solved = this.cplex.solve();
			milliSeconds = System.currentTimeMillis() - milliSeconds;
			milliSeconds /= 1000;
			if (milliSeconds > 30) { // offer this as breakpoint and for debugging
				System.out.println("Model:");
				System.out.println(modelFile);
				System.out.println("Data:");
				System.out.println(dataFile);
			}
		} catch (IloException e) {
			e.printStackTrace();
			if (this.useInitialSolution) { // might be because of 3010 error
				this.useInitialSolution = false;
				this.modelCleanup();
				this.solve(modelFile, dataFile);
				this.useInitialSolution = true;
			}
		}

		if (this.solved) {
			this.model.postProcess();

			this.controllablePlants = this.model.getElement("ControllablePlants").asSymbolSet();
			this.timeRange = this.model.getElement("TIMERANGE").asIntRange();
			this.energyProductionMap = this.model.getElement("energyProduction").asNumVarMap();
			this.runningMap = this.model.getElement("running").asIntMap();
			this.totalProductionMap = this.model.getElement("totalProduction").asNumExprMap();
		}
	}

	private void modelCleanup() {
		try {
			this.model.end();
			this.cplex.clearModel();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void cleanup() {
		this.modelCleanup();
		this.factory.end();
		this.factory = null;
	}

	@Override
	public boolean isSolved() {
		return this.solved;
	}

	public void setSolved(boolean solved) {
		this.solved = solved;
	}

	@Override
	public double getProduction(String key, int step) {
		IloMapIndexArray id = null;
		try {
			id = this.factory.mapIndexArray(0);
			id.add(key);
			id.add(step);

			return this.model.getCplex().getValue(this.energyProductionMap.getAt(id));
		} catch (IloException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public boolean getRunning(String key, int step) {
		IloMapIndexArray id = null;
		try {
			id = this.factory.mapIndexArray(0);
			id.add(key);
			id.add(step);

			int running = this.runningMap.getAt(id);
			return running != 0;
		} catch (IloException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public double getObjective() {
		try {
			return this.model.getCplex().getObjValue();
		} catch (IloException e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public double getTotalProduction(int t) {
		try {
			IloNumExpr expr = this.totalProductionMap.get(t);
			return this.model.getCplex().getValue(expr);
		} catch (IloException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public double getDecExpr(String decExpr, int i) {
		try {
			IloNumDExprMap nvmap = this.model.getElement(decExpr).asNumExprMap();
			return this.model.getCplex().getValue(nvmap.get(i));
		} catch (IloException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public double getDecVar(String decVar, String p, int i) {
		IloMapIndexArray id = null;
		try {
			id = this.factory.mapIndexArray(0);
			id.add(p);
			id.add(i);

			IloNumVarMap nvmap = this.model.getElement(decVar).asNumVarMap();
			return this.model.getCplex().getValue(nvmap.getAt(id));
		} catch (IloException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public double getDecExpr(String decExpr) {
		try {
			return this.model.getCplex().getValue(this.model.getElement(decExpr).asNumExpr());
		} catch (IloException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public void setSettingsFile(String settingsFile) {
		this.settingsFile = settingsFile;

	}

	@Override
	public void setTimeLimit(int seconds) {
		this.timeLimit = seconds;
	}

	@Override
	public void setPresolve(boolean b) {
		this.usePresolve = b;
	}

	@Override
	public boolean isUseInitialSolution() {
		return this.useInitialSolution;
	}

	@Override
	public void setUseInitialSolution(boolean useInitialSolution) {
		this.useInitialSolution = useInitialSolution;
	}
}
