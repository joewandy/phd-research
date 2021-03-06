package com.joewandy.alignmentResearch.main.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.joewandy.alignmentResearch.alignmentExperiment.AlignmentData;
import com.joewandy.alignmentResearch.alignmentExperiment.dataGenerator.AlignmentDataGenerator;
import com.joewandy.alignmentResearch.alignmentExperiment.dataGenerator.AlignmentDataGeneratorFactory;
import com.joewandy.alignmentResearch.main.MultiAlign;
import com.joewandy.alignmentResearch.main.MultiAlignCmdOptions;
import com.joewandy.alignmentResearch.model.AlignmentFile;
import com.joewandy.alignmentResearch.model.EvaluationResult;

public abstract class GenerativeExperiment extends MultiAlignBaseExp implements MultiAlignExperiment {

	public List<MultiAlignExpResult> performExperiment(
			MultiAlignCmdOptions options) throws FileNotFoundException {
		
		List<MultiAlignExpResult> results = new ArrayList<MultiAlignExpResult>();

		// all the experimental-wide settings go here
		setCommonExperimentalSettings(options);
		
		for (int currentIter = 0; currentIter < options.experimentIter; currentIter++) {

			AlignmentData data = getData(options, currentIter);	
			
			// for every method to be tested
			List<String> allMethods = getAllMethods();
			MultiAlignExpResult tempResult = new MultiAlignExpResult("test_"+currentIter);	
			for (String method : allMethods) {
				
				// do alignment using it
				options.method = method;
				MultiAlign multiAlign = new MultiAlign(options, data);
				options.output = null;
				EvaluationResult evalRes = multiAlign.runExperiment();	
				if (evalRes != null) {
					tempResult.addResult(evalRes);	
				}
				
				// and try the grouping approach too last
//				if (method.equals(AlignmentMethodFactory.ALIGNMENT_METHOD_MY_MAXIMUM_WEIGHT_MATCHING_HIERARCHICAL)) {
//					setGroupingSettings(options);
//					multiAlign = new MultiAlign(options, data);
//					evalRes = multiAlign.runExperiment();	
//					if (evalRes != null) {
//						evalRes.setTh(options.alpha);
//						String note = options.alpha + ", " + options.groupingRtWindow;
//						evalRes.setNote(note);
//						tempResult.addResult(evalRes);	
//					}					
//				}
				
			}
			results.add(tempResult);
			
			// save generated input files and ground truth
			saveOutput(data, options, currentIter);
																			
		} 
		
		
		return results;
		
	}
	
	protected AlignmentData getData(MultiAlignCmdOptions options, int currentIter)
			throws FileNotFoundException {

		AlignmentDataGenerator dataGenerator = AlignmentDataGeneratorFactory
				.getAlignmentDataGenerator(options);
//		dataGenerator.addNoise(new ContaminantPeaksNoise(0));
//		dataGenerator.addNoise(new RandomPeakNoise(1.0));
		AlignmentData data = dataGenerator.generateByIteration(currentIter);
		for (AlignmentFile file : data.getAlignmentDataList()) {
			System.out.println("test on " + file.getFilename());
		}
		return data;

	}

	private void saveOutput(AlignmentData data, MultiAlignCmdOptions options,
			int i) {

		if (options.output == null) {
			return;
		}
		
		System.out.println("Saving generated data to " + options.output);
		try {
		
			// save features in sima format
			String inputPath = options.output + "/" + options.experimentType + "/rep_" + i + "/input";
			createDir(inputPath);
			for (AlignmentFile file : data.getAlignmentDataList()) {
				String destPath = inputPath + "/" + file.getFilenameWithoutExtension() + ".txt";
				file.saveSimaFeatures(destPath);
				System.out.println("\t" + destPath + " saved");
			}

			// save features in csv format
			inputPath = options.output + "/" + options.experimentType + "/rep_" + i + "/input/csv";
			createDir(inputPath);
			for (AlignmentFile file : data.getAlignmentDataList()) {
				String destPath = inputPath + "/" + file.getFilenameWithoutExtension() + ".csv";
				file.saveCsvFeatures(destPath);
				System.out.println("\t" + destPath + " saved");
			}
			
			// save ground truth
			final String gtPath = options.output + "/" + options.experimentType + "/rep_" + i + "/ground_truth.txt";
			data.saveGroundTruth(gtPath);
			System.out.println("\t" + gtPath + " saved");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
								
	}

	private void createDir(final String inputPath) throws IOException {
		final File tempDir = new File(inputPath);
		if (!tempDir.exists() && !tempDir.mkdirs()) {
		    throw new IOException("Unable to create " + tempDir.getAbsolutePath());
		}
		FileUtils.cleanDirectory(tempDir);
	}

	@Override
	public void printTestingResult(List<MultiAlignExpResult> results) {

		StringBuilder f1Builder = new StringBuilder();
//		f1Builder.append("join,openms,sima,mw,mwg\n");
		for (MultiAlignExpResult result : results) {

			System.out.println();
			result.printResult();
			System.out.println();

			List<EvaluationResult> evalRes = result.getEvaluationResults();
			String[] toolF1 = new String[evalRes.size()];
			for (int i = 0; i < evalRes.size(); i++) {
				EvaluationResult eval = evalRes.get(i);
				String f1 = String.format("%.3f", eval.getF1());
				toolF1[i] = f1;
			}
			String joined = StringUtils.join(toolF1, ",");
			f1Builder.append(joined + "\n");

		}		 
		System.out.println("********* OVERALL *********");

		System.out.println(f1Builder);
		
	}

	protected abstract void setCommonExperimentalSettings(MultiAlignCmdOptions options);
	
	protected abstract void setGroupingSettings(MultiAlignCmdOptions options);

	protected abstract List<String> getAllMethods();

}
