package com.joewandy.alignmentResearch.alignmentMethod;

import java.util.List;

import com.joewandy.alignmentResearch.alignmentExperiment.AlignmentData;
import com.joewandy.alignmentResearch.alignmentMethod.custom.BaselineAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.custom.ConsistencyAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.custom.MyJoinAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.custom.MyMaximumMatchingAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.custom.MyStableMarriageAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.external.MzMineJoinAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.external.MzMineRansacAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.external.OpenMSAlignment;
import com.joewandy.alignmentResearch.alignmentMethod.external.SimaAlignment;
import com.joewandy.alignmentResearch.objectModel.AlignmentFile;

public class AlignmentMethodFactory {

	// MZMatch current greedy alignment method
	public static final String ALIGNMENT_METHOD_GREEDY = "greedy";
	
	// a simple greedy alignment algorithm as baseline
	public static final String ALIGNMENT_METHOD_BASELINE = "baseline";

	// my own aligners
	public static final String ALIGNMENT_METHOD_MY_JOIN = "myJoin";
	public static final String ALIGNMENT_METHOD_MY_STABLE_MARRIAGE = "myStableMarriage";
	public static final String ALIGNMENT_METHOD_MY_MAXIMUM_WEIGHT_MATCHING = "myMaxWeight";
	
	// calls mzMine Join aligner
	public static final String ALIGNMENT_METHOD_MZMINE_JOIN = "join";
	
	// calls mzMine RANSAC aligner
	public static final String ALIGNMENT_METHOD_MZMINE_RANSAC = "ransac";
	
	// calls SIMA alignment
	public static final String ALIGNMENT_METHOD_SIMA = "sima";

	// calls OpenMS alignment
	public static final String ALIGNMENT_METHOD_OPENMS = "openMS";
	
	// calls consistency-based alignment
	public static final String ALIGNMENT_METHOD_CONSISTENCY = "consistency";
	
	public static AlignmentMethod getAlignmentMethod(final String method, AlignmentMethodParam.Builder paramBuilder, 
			AlignmentData data) {

		List<AlignmentFile> alignmentDataList = data.getAlignmentDataList();
		AlignmentMethodParam param = paramBuilder.build();
		
		AlignmentMethod aligner = null; 
		if (AlignmentMethodFactory.ALIGNMENT_METHOD_BASELINE.equals(method)) {				
			aligner = new BaselineAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_MY_JOIN.equals(method)) {
			aligner = new MyJoinAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_MY_STABLE_MARRIAGE.equals(method)) {
			aligner = new MyStableMarriageAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_MY_MAXIMUM_WEIGHT_MATCHING.equals(method)) {
			aligner = new MyMaximumMatchingAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_MZMINE_RANSAC.equals(method)) {
			aligner = new MzMineRansacAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_MZMINE_JOIN.equals(method)) {
			aligner = new MzMineJoinAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_SIMA.equals(method)) {
			aligner = new SimaAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_OPENMS.equals(method)) {
			aligner = new OpenMSAlignment(alignmentDataList, param);
		} else if (AlignmentMethodFactory.ALIGNMENT_METHOD_CONSISTENCY.equals(method)) {
			aligner = new ConsistencyAlignment(alignmentDataList, param);
		}
		
		return aligner;

	}
	
}