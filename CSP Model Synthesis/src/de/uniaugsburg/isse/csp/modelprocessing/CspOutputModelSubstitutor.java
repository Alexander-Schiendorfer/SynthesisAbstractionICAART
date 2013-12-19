/**
 * 
 */
package de.uniaugsburg.isse.csp.modelprocessing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import ua.cr2csop.constraints.Constraint;
import ua.cr2csop.graph.DirectedConstraintGraph;
import ua.cr2csop.parser.ConstraintRelationParser;
import ua.cr2csop.util.OPLUtil;
import ua.cr2csop.weights.BfsWeightAssigner;
import ua.cr2csop.weights.concrete.TransitivePredecessorDominanceWeightingFunction;
import de.uniaugsburg.isse.csp.model.CspInputModel;
import de.uniaugsburg.isse.csp.model.CspOutputModel;
import de.uniaugsburg.isse.csp.util.StringLists;

/**
 * 
 * @author steghoja
 * 
 */
public class CspOutputModelSubstitutor {

	/**
	 * Replaces the list of power plants in the output model with the list of
	 * actual sub-models loaded.
	 * 
	 * @param outputModel
	 *            the model containing the list
	 * @param inputModels
	 *            the sub-models that model the individual power plants
	 */
	public void replaceModelList(CspOutputModel outputModel,
			List<CspInputModel> inputModels) {
		String modelList = StringLists.toString(outputModel.getModelList());
		StringBuilder modelNameBuilder = new StringBuilder();
		modelNameBuilder.append("{ ");
		for (CspInputModel model : inputModels) {
			modelNameBuilder.append("\"");
			modelNameBuilder.append(model.getFileName().substring(0,
					model.getFileName().indexOf(".")));
			modelNameBuilder.append("\",");
		}
		int lastCommaIndex = modelNameBuilder.lastIndexOf(",");
		modelNameBuilder.delete(lastCommaIndex, lastCommaIndex + 1);
		modelNameBuilder.append(" }");
		modelList = modelList.replaceAll("\\.\\.\\.",
				modelNameBuilder.toString());
		outputModel.setModelList(StringLists.toList(modelList));
	}

	public DirectedConstraintGraph processRelationships(
			CspOutputModel outputModel, StringBuilder relationshipBuilder) {
		String charset = "UTF-8";
		InputStream in = new ByteArrayInputStream(relationshipBuilder
				.toString().getBytes(Charset.forName(charset)));
		ConstraintRelationParser crp = new ConstraintRelationParser();
		DirectedConstraintGraph dcg = crp.getDirectedConstraintGraph(in);
		// add more important constraint edges to all not having indegree
		Collection<Constraint> roots = new ArrayList<Constraint>(dcg
				.getUnderlyingGraph().getVertexCount());

		for (Constraint c : dcg.getUnderlyingGraph().getVertices()) {
			if (dcg.getUnderlyingGraph().getInEdges(c).isEmpty()) {
				roots.add(c);
			}
		}

		Collection<String> constraintNames = OPLUtil
				.getConstraintNames(outputModel.getConstraints());

		for (String orgConstraintIdent : constraintNames) {
			Constraint orgConstraint = dcg.lookupOrAdd(orgConstraintIdent);
			for (Constraint root : roots) {
				dcg.addEdge(orgConstraint, root);
			}
		}

		BfsWeightAssigner weightAssigner = new BfsWeightAssigner(
				new TransitivePredecessorDominanceWeightingFunction());
		weightAssigner.assignWeights(dcg);

		// check weights
		for (Constraint c : dcg.getUnderlyingGraph().getVertices()) {
			System.out.println(c);
		}
		return dcg;
	}

	public void addSoftConstraints(DirectedConstraintGraph dcg,
			CspOutputModel model) {
		List<String> modelList = model.getSoftConstraintsList();
		TreeSet<String> sortedModelList = new TreeSet<String>();
		for (Constraint c : dcg.getUnderlyingGraph().getVertices()) {
			sortedModelList.add("\"" + c.getName() + "\"");
		}
		modelList.addAll(sortedModelList);
	}

	/**
	 * Replaces constraint contents with their penalty formulation
	 * 
	 * @param dcg
	 * @param outputModel
	 */
	public void replaceSoftConstraints(DirectedConstraintGraph dcg,
			CspOutputModel outputModel) {
		List<String> newConstraints = OPLUtil.replaceConstraintsPenalties(
				outputModel.getConstraints(), dcg);

		outputModel.setConstraints(newConstraints);
	}

	public void replaceMandatoryConstants(CspOutputModel outputModel,
			List<CspInputModel> inputModels) {
		List<String> newMandConstants = new ArrayList<String>(outputModel
				.getMandatoryConstants().size());
		for (String mandatoryConstantLine : outputModel.getMandatoryConstants()) {
			StringTokenizer tok = new StringTokenizer(mandatoryConstantLine,
					" ");
			tok.nextToken(); // type
			String mandatoryConstantName = tok.nextToken();
			mandatoryConstantName = mandatoryConstantName.substring(0,
					mandatoryConstantName.indexOf("[")).trim();

			StringBuilder mandConstantBuilder = new StringBuilder();
			mandConstantBuilder.append("[ ");
			boolean first = true;
			for (CspInputModel model : inputModels) {
				if (first)
					first = false;
				else {
					mandConstantBuilder.append(", ");
				}
				mandConstantBuilder.append(mandatoryConstantName + "_"
						+ model.getModelName());
			}
			mandConstantBuilder.append(" ]");

			mandatoryConstantLine = mandatoryConstantLine.replaceAll(
					"\\.\\.\\.", mandConstantBuilder.toString());
			newMandConstants.add(mandatoryConstantLine);
		}
		outputModel.setMandatoryConstants(newMandConstants);
	}

	public void addFeasibleRegions(CspOutputModel outputModel,
			List<CspInputModel> inputModels) {
		StringBuilder feasibleRegionBuilder = new StringBuilder(
				"{IntervalType} feasibleRegions[plants] = [");
		boolean first = true;
		for (CspInputModel model : inputModels) {
			if (first)
				first = false;
			else
				feasibleRegionBuilder.append(", ");
			feasibleRegionBuilder.append("{ <0,0>, " + "<" + model.getpMin()
					+ ", " + model.getpMax() + "> }");
		}
		feasibleRegionBuilder.append("];\n");
		outputModel.setFeasibleRegionString(feasibleRegionBuilder.toString());
	}
}
