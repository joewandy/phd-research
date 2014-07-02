package com.joewandy.alignmentResearch.alignmentMethod;

import com.joewandy.alignmentResearch.main.MultiAlignConstants;

public class AlignmentMethodParam {

	// show setup dialog or not during MZMine alignment ?
	public static final boolean SHOW_PARAM_SETUP_DIALOG = false;
	
	/** 
	 * This value sets the range, in terms of retention time, to create the model 
	 * using RANSAC and non-linear regression algorithm. 
	 * Maximum allowed retention time difference
	 */
	public static final int PARAM_RT_TOLERANCE_BEFORE_CORRECTION = 300;

	/**
	 * Maximum number of iterations allowed in the algorithm to find the right model 
	 * consistent in all the pairs of aligned peaks. When its value is 0, the number 
	 * of iterations (k) will be estimate automatically.
	 */
	public static final int PARAM_RANSAC_ITERATION = 50000;
//	public static final int PARAM_RANSAC_ITERATION = 0;

	/**
	 * % of points required to consider the model valid (d).
	 */
	public static final double PARAM_MINIMUM_NO_OF_POINTS = 0.10;

	/**
	 * Threshold value (seconds) for determining when a data 
	 * point fits a model (t)
	 */
	public static final double PARAM_THRESHOLD_VALUE = 15;
	
	/**
	 * Switch between polynomial model or lineal model
	 */
	public static final boolean PARAM_LINEAR_MODEL = true;
	
	/**
	 * If checked, only rows having same charge state can be aligned
	 */
	public static final boolean PARAM_REQUIRE_SAME_CHARGE_STATE = false;	
	
	/**
	 * How much weight to allocate to the various weights
	 */
	public static final double PARAM_ALPHA = 0.5;
	
	/**
	 * Use grouping information during alignment ?
	 */
	public static final boolean USE_GROUP = false;	

	/**
	 * Exact or approximate matching
	 */
	public static final boolean EXACT_MATCH = false;	
	
	/**
	 * Use peak shape info during grouping ?
	 */
	public static final boolean USE_PEAK_SHAPE = false;	
	
	/**
	 * OpenMS parameter
	 */
	public static final double PARAM_MZ_PAIR_MAX_DISTANCE = 0.5;
	
	private double massTolerance;
	private double rtTolerance;
	private boolean usePpm;
	private boolean useGroup;
	private boolean exactMatch;
	private boolean usePeakShape;
	private String groupingMethod;
	private double groupingRtTolerance;
	private String scoringMethod;
	private double alpha;
	
	// for ransac alignment
	private double ransacRtToleranceBeforeMinute;
	private double ransacRtToleranceAfterMinute;
	private int ransacIteration;
	private double ransacNMinPoints;
	private double ransacThreshold;
	private boolean ransacLinearModel;
	private boolean ransacSameChargeRequired;
	
	// for openms
	private double openMsMzPairMaxDistance;
	
	// variant of Builder pattern, as described in Effective Java 2nd Ed.
	public static class Builder {

		// required parameters
		private double massTolerance;
		private double rtTolerance;
		private boolean usePpm;
		
		// optional parameters
		private boolean useGroup;
		private boolean exactMatch;
		private boolean usePeakShape;		
		private String groupingMethod;
		private double groupingRtTolerance;
		private String scoringMethod;
		private double alpha;

		// for ransac alignment
		private double ransacRtToleranceBeforeMinute;
		private double ransacRtToleranceAfterMinute;
		private int ransacIteration;
		private double ransacNMinPoints;
		private double ransacThreshold;
		private boolean ransacLinearModel;
		private boolean ransacSameChargeRequired;
		
		// for OpenMS alignment
		private double openMsMzPairMaxDistance;
		
		public Builder(double massTolerance, double rtTolerance) {

			this.massTolerance = massTolerance;
			this.rtTolerance = rtTolerance;
		
			// set whole loads of default value
			
			this.usePpm = MultiAlignConstants.ALIGN_BY_RELATIVE_MASS_TOLERANCE;

			// set whole loads of default value for ransac
			this.ransacRtToleranceBeforeMinute = AlignmentMethodParam.PARAM_RT_TOLERANCE_BEFORE_CORRECTION / 60.0;
			this.ransacRtToleranceAfterMinute = this.rtTolerance / 60.0;
			this.ransacIteration = AlignmentMethodParam.PARAM_RANSAC_ITERATION;
			this.ransacNMinPoints = AlignmentMethodParam.PARAM_MINIMUM_NO_OF_POINTS / 100.0;
			this.ransacThreshold = AlignmentMethodParam.PARAM_THRESHOLD_VALUE / 60.0;
			this.ransacLinearModel = AlignmentMethodParam.PARAM_LINEAR_MODEL;
			this.ransacSameChargeRequired = AlignmentMethodParam.PARAM_REQUIRE_SAME_CHARGE_STATE;
			
			// for openms
			this.openMsMzPairMaxDistance = AlignmentMethodParam.PARAM_MZ_PAIR_MAX_DISTANCE;
			
			// set parameter for other alignment methods
			this.useGroup = AlignmentMethodParam.USE_GROUP;
			this.exactMatch = AlignmentMethodParam.EXACT_MATCH;
			this.usePeakShape = AlignmentMethodParam.USE_PEAK_SHAPE;
			this.groupingMethod = MultiAlignConstants.GROUPING_METHOD_GREEDY;
			this.scoringMethod = MultiAlignConstants.SCORING_METHOD_DIST;
			this.alpha = AlignmentMethodParam.PARAM_ALPHA;
			
		}

		public Builder usePpm(boolean usePpm) {
			this.usePpm = usePpm;
			return this;
		}
		
		public Builder useGroup(boolean useGroup) {
			this.useGroup = useGroup;
			return this;
		}		

		public Builder exactMatch(boolean exactMatch) {
			this.exactMatch = exactMatch;
			return this;
		}		
		
		public Builder usePeakShape(boolean usePeakShape) {
			this.usePeakShape = usePeakShape;
			return this;
		}		
		
		public Builder groupingMethod(String groupingMethod) {
			this.groupingMethod = groupingMethod;
			return this;
		}				

		public Builder groupingRtTolerance(double groupingRtTolerance) {
			this.groupingRtTolerance = groupingRtTolerance;
			return this;
		}				

		public Builder scoringMethod(String scoringMethod) {
			this.scoringMethod = scoringMethod;
			return this;
		}				
		
		public Builder alpha(double alpha) {
			this.alpha = alpha;
			return this;
		}		

		public Builder ransacRtToleranceBefore(double rtToleranceBefore) {
			this.ransacRtToleranceBeforeMinute = rtToleranceBefore / 60.0;
			return this;
		}

		public Builder ransacRtToleranceAfter(double rtToleranceAfter) {
			this.ransacRtToleranceAfterMinute = rtToleranceAfter / 60.0;
			return this;
		}
		
		public Builder ransacIteration(int ransacIteration) {
			this.ransacIteration = ransacIteration;
			return this;
		}

		public Builder ransacNMinPoints(double nMinPoints) {
			this.ransacNMinPoints = nMinPoints / 100.0;
			return this;
		}

		public Builder ransacThreshold(double threshold) {
			this.ransacThreshold = threshold;
			return this;
		}

		public Builder ransacLinearModel(boolean linearModel) {
			this.ransacLinearModel = linearModel;
			return this;
		}

		public Builder ransacSameChargeRequired(boolean sameChargeRequired) {
			this.ransacSameChargeRequired = sameChargeRequired;
			return this;
		}
		
		public Builder openMsMzPairMaxDistance(double dist) {
			this.openMsMzPairMaxDistance = dist;
			return this;
		}

		public AlignmentMethodParam build() {
			return new AlignmentMethodParam(this);
		}

	}
	
	public AlignmentMethodParam(Builder builder) {
		this.massTolerance = builder.massTolerance;
		this.rtTolerance = builder.rtTolerance;
		this.usePpm = builder.usePpm;
		this.useGroup = builder.useGroup;
		this.exactMatch = builder.exactMatch;
		this.usePeakShape = builder.usePeakShape;
		this.groupingMethod = builder.groupingMethod;
		this.scoringMethod = builder.scoringMethod;
		this.groupingRtTolerance = builder.groupingRtTolerance;
		this.alpha = builder.alpha;
		this.ransacRtToleranceBeforeMinute = builder.ransacRtToleranceBeforeMinute;
		this.ransacRtToleranceAfterMinute = builder.ransacRtToleranceAfterMinute;
		this.ransacIteration = builder.ransacIteration;
		this.ransacNMinPoints = builder.ransacNMinPoints;
		this.ransacThreshold = builder.ransacThreshold;
		this.ransacLinearModel = builder.ransacLinearModel;
		this.ransacSameChargeRequired = builder.ransacSameChargeRequired;
		this.openMsMzPairMaxDistance = builder.openMsMzPairMaxDistance;
	}

	public double getMassTolerance() {
		return massTolerance;
	}

	public double getRtTolerance() {
		return rtTolerance;
	}

	public boolean isUsePpm() {
		return usePpm;
	}

	public boolean isUseGroup() {
		return useGroup;
	}

	public boolean isExactMatch() {
		return exactMatch;
	}
	
	public boolean isUsePeakShape() {
		return usePeakShape;
	}
	
	public String getGroupingMethod() {
		return groupingMethod;
	}
	
	public String getScoringMethod() {
		return scoringMethod;
	}

	public double getGroupingRtTolerance() {
		return groupingRtTolerance;
	}

	public double getAlpha() {
		return alpha;
	}

	public double getRansacRtToleranceBeforeMinute() {
		return ransacRtToleranceBeforeMinute;
	}

	public double getRansacRtToleranceAfterMinute() {
		return ransacRtToleranceAfterMinute;
	}

	public int getRansacIteration() {
		return ransacIteration;
	}

	public double getRansacNMinPoints() {
		return ransacNMinPoints;
	}

	public double getRansacThreshold() {
		return ransacThreshold;
	}

	public boolean isRansacLinearModel() {
		return ransacLinearModel;
	}

	public boolean isRansacSameChargeRequired() {
		return ransacSameChargeRequired;
	}
	
	public double getOpenMsMzPairMaxDistance() {
		return openMsMzPairMaxDistance;
	}
	
}