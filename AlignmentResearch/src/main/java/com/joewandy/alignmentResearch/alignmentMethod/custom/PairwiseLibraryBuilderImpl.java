package com.joewandy.alignmentResearch.alignmentMethod.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.joewandy.alignmentResearch.alignmentMethod.AlignmentMethod;
import com.joewandy.alignmentResearch.alignmentMethod.AlignmentMethodParam;
import com.joewandy.alignmentResearch.main.MultiAlignConstants;
import com.joewandy.alignmentResearch.objectModel.AlignmentFile;
import com.joewandy.alignmentResearch.objectModel.AlignmentLibrary;
import com.joewandy.alignmentResearch.objectModel.AlignmentList;
import com.joewandy.alignmentResearch.objectModel.AlignmentRow;
import com.joewandy.alignmentResearch.objectModel.DistanceCalculator;
import com.joewandy.alignmentResearch.objectModel.Feature;
import com.joewandy.alignmentResearch.objectModel.MahalanobisDistanceCalculator;
import com.joewandy.alignmentResearch.util.GraphEdgeConstructor;

public abstract class PairwiseLibraryBuilderImpl implements Runnable, PairwiseLibraryBuilder {

	protected BlockingQueue<AlignmentLibrary> queue;

	protected GraphEdgeConstructor edgeConstructor;
	protected int libraryID;
	protected double massTolerance;
	protected double rtTolerance;
	protected AlignmentFile data1;
	protected AlignmentFile data2;

	public PairwiseLibraryBuilderImpl(BlockingQueue<AlignmentLibrary> queue,
			int libraryID, double massTolerance, double rtTolerance, AlignmentFile data1, AlignmentFile data2) {
		this.queue = queue;
		this.edgeConstructor = new GraphEdgeConstructor();
		this.libraryID = libraryID;
		this.massTolerance = massTolerance;
		this.rtTolerance = rtTolerance;
		this.data1 = data1;
		this.data2 = data2;
	}

	
	public void run() {
		try {
			queue.put(producePairwiseLibrary());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public AlignmentLibrary producePairwiseLibrary() {
		
		System.out.println("PairwiseLibraryBuilder #" + libraryID + " running");
		
		List<AlignmentFile> files = new ArrayList<AlignmentFile>();
		files.add(data1);
		files.add(data2);

		AlignmentMethodParam.Builder paramBuilder = new AlignmentMethodParam.Builder(massTolerance, rtTolerance);
		paramBuilder.usePpm(MultiAlignConstants.ALIGN_BY_RELATIVE_MASS_TOLERANCE);
		AlignmentMethodParam param = paramBuilder.build();
		AlignmentMethod pairwiseAligner = getAlignmentMethod(files, param);

		AlignmentList result = pairwiseAligner.align();
		List<AlignmentRow> rows = result.getRows();

		AlignmentLibrary library = new AlignmentLibrary(libraryID, data1, data2);

		// find max dist and set some flags
		double maxDist = 0;
		for (AlignmentRow row : rows) {

			// TODO: hack .. mark all features as unaligned, necessary when doing the final alignment later
			// since we're not processing any features that have been aligned
			for (Feature f : row.getFeatures()) {
				f.setAligned(false);
			}
			
			// consider only rows containing pairwise alignment
			if (row.getFeaturesCount() == 2) {
				Feature[] features = row.getFeatures().toArray(new Feature[0]);
				Feature f1 = features[0];
				Feature f2 = features[1];
				double dist = computeDist(f1, f2);
				if (dist > maxDist) {
					maxDist = dist;
				}
			}
			
		}
		
		// compute similarities and add to library
		final double weight = 1;
		for (AlignmentRow row : rows) {
			// consider only rows containing pairwise alignment
			if (row.getFeaturesCount() == 2) {
				Feature[] features = row.getFeatures().toArray(new Feature[0]);
				Feature f1 = features[0];
				Feature f2 = features[1];
				double dist = computeDist(f1, f2);
				double score = 1-(dist/maxDist);
				library.addAlignedPair(f1, f2, score, weight);					
			}
		}
		
		System.out.println("#" + String.format("%04d ", libraryID) + 
				"(" + data1.getFilenameWithoutExtension() + ", " + data2.getFilenameWithoutExtension() + ")" +  
				" pairwise alignments = " + library.getAlignedPairCount() + 
				" average library weight = " + String.format("%.3f", library.getAvgWeight()));
		return library;
		
	}

	protected abstract AlignmentMethod getAlignmentMethod(List<AlignmentFile> files, AlignmentMethodParam param);

	protected double computeDist(Feature f1, Feature f2) {

		DistanceCalculator calc = new MahalanobisDistanceCalculator(massTolerance, rtTolerance);
		double dist = calc.compute(f1.getMass(), f2.getMass(), f1.getRt(), f2.getRt());
		return dist;
		
	}

}
