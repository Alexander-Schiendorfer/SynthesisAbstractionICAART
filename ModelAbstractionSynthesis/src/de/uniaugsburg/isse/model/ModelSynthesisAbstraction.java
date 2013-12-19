package de.uniaugsburg.isse.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ua.cr2csop.graph.DirectedConstraintGraph;
import de.uniaugsburg.isse.constraints.BoundsConstraint;
import de.uniaugsburg.isse.constraints.GraduallyOffConstraint;
import de.uniaugsburg.isse.constraints.RateOfChangeConstraint;
import de.uniaugsburg.isse.constraints.StopTimeConstraint;
import de.uniaugsburg.isse.csp.fileprocessing.CspFileWriter;
import de.uniaugsburg.isse.csp.fileprocessing.CspInputFileSplitter;
import de.uniaugsburg.isse.csp.fileprocessing.CspOutputFileSplitter;
import de.uniaugsburg.isse.csp.model.CspInputModel;
import de.uniaugsburg.isse.csp.model.CspOutputModel;
import de.uniaugsburg.isse.csp.modelprocessing.CspInputModelSubstitutor;
import de.uniaugsburg.isse.csp.modelprocessing.CspOutputModelSubstitutor;
import de.uniaugsburg.isse.data.CspPowerPlantDataReader;
import de.uniaugsburg.isse.powerplants.PowerPlantData;

/**
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class ModelSynthesisAbstraction {

	/**
	 * Just a simple test case containing of the abstraction of one AVPP
	 * containing two concrete plants
	 * 
	 * @param args
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String firstInput = "a.mod";
		String secondInput = "b.mod";
		String thirdInput = "c.mod";
		String outputPrototype = "Prototype Power Plant Model Synthesis.mod";

		/* =============================================== */
		File inputFile1 = new File(firstInput);
		File inputFile2 = new File(secondInput);
		File inputFile3 = new File(thirdInput);
		File outputFile = new File(outputPrototype);

		// read input models
		List<CspInputModel> inputModels = new ArrayList<CspInputModel>();

		CspInputFileSplitter splitter = new CspInputFileSplitter(
				inputFile1.getName(), new FileInputStream(inputFile1));
		splitter.split();
		inputModels.add(splitter.getModel());

		splitter = new CspInputFileSplitter(inputFile2.getName(),
				new FileInputStream(inputFile2));
		splitter.split();
		inputModels.add(splitter.getModel());

		splitter = new CspInputFileSplitter(inputFile3.getName(),
				new FileInputStream(inputFile3));
		splitter.split();
		inputModels.add(splitter.getModel());

		// read output model
		CspOutputModel outputModel = null;
		CspOutputFileSplitter osplitter = new CspOutputFileSplitter(
				outputFile.getName(), new FileInputStream(outputFile));
		osplitter.split();
		outputModel = osplitter.getModel();

		// first get the correct power plant data
		Map<String, PowerPlantData> plantData = getPlantData(inputModels);
		// combine and show
		// Modify input files

		// get mandatory constants
		StringBuilder relationshipBuilder = new StringBuilder();

		List<String> orgDecVars = outputModel.getDecisionVariableNames();
		for (CspInputModel model : inputModels) {
			CspInputModelSubstitutor substitutor = new CspInputModelSubstitutor();
			substitutor.replaceConstantNames(model);
			substitutor.replaceConstraintNames(model);
			substitutor.replaceDecisionVariables(model, orgDecVars);
			relationshipBuilder.append(substitutor
					.replaceConstraintRelationships(model));
		}

		System.out.println(relationshipBuilder.toString());
		// Modify output file
		CspOutputModelSubstitutor outputModelSubstitutor = new CspOutputModelSubstitutor();
		// Write output file
		DirectedConstraintGraph dcg = outputModelSubstitutor
				.processRelationships(outputModel, relationshipBuilder);

		// rewrite penalties for soft constraints
		for (CspInputModel model : inputModels) {
			CspInputModelSubstitutor substitutor = new CspInputModelSubstitutor();
			substitutor.replaceConstraintsPenalties(model, dcg);

		}

		outputModelSubstitutor.addSoftConstraints(dcg, outputModel);
		outputModelSubstitutor.replaceSoftConstraints(dcg, outputModel);
		outputModelSubstitutor.replaceModelList(outputModel, inputModels);
		outputModelSubstitutor.addFeasibleRegions(outputModel, inputModels);
		outputModelSubstitutor.replaceMandatoryConstants(outputModel,
				inputModels);

		CspFileWriter fileWriter = new CspFileWriter();
		FileOutputStream testFile = new FileOutputStream("opl-project/"
				+ outputModel.getIdent() + "synthesized.mod");
		fileWriter.setOutputStream(new PrintStream(testFile));
		fileWriter.writeFile(inputModels, outputModel);

		// NOW do some abstraction
		/*
		 * GeneralAbstraction ga = new GeneralAbstraction();
		 * ga.setPowerPlants(plantData); ga.perform(); ga.getFeasibleRegions();
		 * ga.print();
		 * 
		 * TemporalAbstraction ta = new TemporalAbstraction();
		 * ta.setPowerPlants(plantData); ta.perform(); // also do general
		 * abstraction ta.perform(10);
		 * 
		 * // NOW write the abstracted CSP model CspAbstractedModelExporter
		 * modelExporter = new CspAbstractedModelExporter( outputModel, ga, ta);
		 * modelExporter.print();
		 */
	}

	private static Map<String, PowerPlantData> getPlantData(
			List<CspInputModel> inputModels) {
		Map<String, PowerPlantData> plantData = new HashMap<String, PowerPlantData>(
				inputModels.size() * 2);

		CspPowerPlantDataReader reader = new CspPowerPlantDataReader();

		// -- first sample plant
		for (CspInputModel in : inputModels) {
			PowerPlantData p = reader.read(in);
			plantData.put(in.getModelName(), p);
			in.setP(p.getPowerBoundaries().min, p.getPowerBoundaries().max);
		}

		// add constraints - assumed homogeneous for the moment
		for (PowerPlantData pd : plantData.values()) {
			RateOfChangeConstraint roc = new RateOfChangeConstraint(pd);
			pd.addConstraint(roc);

			BoundsConstraint bc = new BoundsConstraint(pd);
			pd.addConstraint(bc);

			GraduallyOffConstraint goc = new GraduallyOffConstraint(pd);
			pd.addConstraint(goc);

			StopTimeConstraint stc = new StopTimeConstraint(pd);
			pd.addConstraint(stc);
		}
		return plantData;
	}

}
