package com.joewandy.alignmentResearch.main.experiment;

import java.util.List;

import com.joewandy.alignmentResearch.main.MultiAlignCmdOptions;

public interface MultiAlignExperiment {

	public static final double[] ALL_ALPHA = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0  };
	public static final double[] ALL_GROUPING_RT = { 
		0.25, 0.5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
		11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
		21, 22, 23, 24, 25, 26, 27, 28, 29, 30
	};
	public static final double[] ALL_ALIGNMENT_RT = { 20, 40, 60, 80, 100 };
	public static final String EXPERIMENT_TYPE_M1 = "M1";		
	public static final String EXPERIMENT_TYPE_P1P2 = "P1P2";	
	public static final String EXPERIMENT_TYPE_GENERATIVE_TECHNICAL_REPLICATES = "GT";		
	public static final String EXPERIMENT_TYPE_GENERATIVE_BIOLOGICAL_REPLICATES = "GB";		
	
	public List<MultiAlignExpResult> performExperiment(MultiAlignCmdOptions options) throws Exception;

	public void printResult(List<MultiAlignExpResult> results);
	
}
