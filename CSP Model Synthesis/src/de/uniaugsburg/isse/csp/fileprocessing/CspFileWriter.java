/**
 * 
 */
package de.uniaugsburg.isse.csp.fileprocessing;

import java.io.PrintStream;
import java.util.List;

import de.uniaugsburg.isse.csp.model.CspInputModel;
import de.uniaugsburg.isse.csp.model.CspOutputModel;
import de.uniaugsburg.isse.csp.util.StringLists;

/**
 * @author Jan-Philipp Stegh�fer
 * 
 */
public class CspFileWriter {

	private PrintStream outputStream;

	public CspFileWriter() {
		outputStream = System.out;
	}

	public void writeFile(List<CspInputModel> inputModels,
			CspOutputModel outputModel) {
		outputStream.println("// Predefined Constraints");
		outputStream.println(StringLists.toString(outputModel.getConstants()));

		outputStream.println("// Model-specific constants");
		for (CspInputModel model : inputModels) {
			outputStream.print("// Constants for model ");
			outputStream.println(model.getFileName());
			outputStream.println(StringLists.toString(model
					.getModelSpecificConstants()));
		}

		outputStream.println("// Types ");
		outputStream.println(StringLists.toString(outputModel.getTypes()));

		outputStream.println("// Model List");
		outputStream.println(StringLists.toString(outputModel.getModelList()));

		outputStream.println("// Feasible regions ");
		outputStream.println(outputModel.getFeasibleRegionString());
		outputStream.println("// Mandatory agent constants");
		outputStream.println(StringLists.toString(outputModel
				.getMandatoryConstants()));

		outputStream.print("{string} softConstraints = {");
		boolean first = true;
		for (String s : outputModel.getSoftConstraintsList()) {
			if (first)
				first = false;
			else
				outputStream.print(", ");
			outputStream.print(s);
		}
		outputStream.println("};");
		outputStream.println("// Predefined Decision Variables");
		outputStream.println(StringLists.toString(outputModel
				.getDecisionVariables()));

		outputStream.println("// Predefined Decision Expressions");
		outputStream.println(StringLists.toString(outputModel
				.getDecisionExpressions()));
		// Suppress model-specific decision variables
		/*
		 * outputStream.println("// Model-specific decision variables"); for
		 * (CspInputModel model : inputModels) {
		 * outputStream.print("// Decision variables for model ");
		 * outputStream.println(model.getFileName());
		 * outputStream.println(StringLists
		 * .toString(model.getDecisionVariables())); }
		 */

		outputStream.println("// Predefined Constraints");
		outputStream.println("subject to {");
		outputStream
				.println(StringLists.toString(outputModel.getConstraints()));

		outputStream.println("// Model-specific constraints");
		for (CspInputModel model : inputModels) {
			outputStream.print("// Constraints for model ");
			outputStream.println(model.getFileName());
			outputStream.println(StringLists.toString(model.getConstraints()));
		}

		outputStream.println("// Initial state constraints");
		for (CspInputModel model : inputModels) {
			for (String decVarName : outputModel.getDecisionVariableNames()) {
				if (decVarName.contains("penalties"))
					continue;
				outputStream.println(decVarName + "[\"" + model.getModelName()
						+ "\"][0] == " + decVarName + "Init_"
						+ model.getModelName() + ";");
			}
		}
		outputStream.println(" }");
	}

	public PrintStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(PrintStream outputStream) {
		this.outputStream = outputStream;
	}
}
