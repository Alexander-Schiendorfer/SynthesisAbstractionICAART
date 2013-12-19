/**
 * 
 */
package de.uniaugsburg.isse.csp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

import de.uniaugsburg.isse.csp.fileprocessing.CspFileWriter;
import de.uniaugsburg.isse.csp.fileprocessing.CspInputFileSplitter;
import de.uniaugsburg.isse.csp.fileprocessing.CspOutputFileSplitter;
import de.uniaugsburg.isse.csp.model.CspInputModel;
import de.uniaugsburg.isse.csp.model.CspOutputModel;
import de.uniaugsburg.isse.csp.modelprocessing.CspInputModelSubstitutor;
import de.uniaugsburg.isse.csp.modelprocessing.CspOutputModelSubstitutor;
import de.uniaugsburg.isse.csp.util.OplModelFileFilter;

/**
 * 
 * @author Jan-Philipp Stegh�fer
 */
public class CspCombiner {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws FileNotFoundException {
		File[] inputFiles = null;
		File outputFile = null;
		final JFileChooser fc = new JFileChooser();
		fc.setMultiSelectionEnabled(true);
		fc.setFileFilter(new OplModelFileFilter());
		fc.setDialogTitle("Choose OPL files for combination");

		// Select the input files

		int fileChooserReturnValue = fc.showOpenDialog(null);
		if (fileChooserReturnValue == JFileChooser.APPROVE_OPTION) {
			inputFiles = fc.getSelectedFiles();
		} else {
			System.out.println("No input file selected. Exiting.");
			System.exit(1);
		}

		List<CspInputModel> inputModels = new ArrayList<CspInputModel>();
		for (File file : inputFiles) {
			CspInputFileSplitter splitter = new CspInputFileSplitter(
					file.getName(), new FileInputStream(file));
			splitter.split();
			inputModels.add(splitter.getModel());
		}

		// Select output file

		fc.setMultiSelectionEnabled(false);
		fc.setDialogTitle("Choose OPL file for integration");
		fileChooserReturnValue = fc.showOpenDialog(null);

		CspOutputModel outputModel = null;
		if (fileChooserReturnValue == JFileChooser.APPROVE_OPTION) {
			outputFile = fc.getSelectedFile();
			CspOutputFileSplitter splitter = new CspOutputFileSplitter(
					outputFile.getName(), new FileInputStream(outputFile));
			splitter.split();
			outputModel = splitter.getModel();
		} else {
			System.out.println("No output file selected. Exiting.");
			System.exit(1);
		}

		// Modify input files
		List<String> decVars = outputModel.getDecisionVariableNames();
		for (CspInputModel model : inputModels) {
			CspInputModelSubstitutor substitutor = new CspInputModelSubstitutor();
			substitutor.replaceConstantNames(model);
			substitutor.replaceConstraintNames(model);
			substitutor.replaceDecisionVariables(model, decVars);
		}

		// Modify output file

		new CspOutputModelSubstitutor().replaceModelList(outputModel,
				inputModels);

		// Write output file

		CspFileWriter fileWriter = new CspFileWriter();
		fileWriter.writeFile(inputModels, outputModel);

	}
}
