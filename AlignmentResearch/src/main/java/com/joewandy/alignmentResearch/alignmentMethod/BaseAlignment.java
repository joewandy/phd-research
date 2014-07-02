package com.joewandy.alignmentResearch.alignmentMethod;

import java.util.ArrayList;
import java.util.List;

import com.joewandy.alignmentResearch.filter.AlignmentResultFilter;
import com.joewandy.alignmentResearch.objectModel.AlignmentFile;
import com.joewandy.alignmentResearch.objectModel.AlignmentList;
import com.joewandy.alignmentResearch.objectModel.AlignmentRow;
import com.joewandy.alignmentResearch.objectModel.Feature;

public abstract class BaseAlignment implements AlignmentMethod {

	protected double massTolerance;
	protected boolean usePpm;
	protected double rtTolerance;

	protected List<AlignmentFile> dataList;	
	protected List<AlignmentResultFilter> filters;
	protected List<AlignmentRow> filteredResult;
		
	/**
	 * Initialise our aligner
	 * @param dataList List of feature data to align
	 * @param massTolerance Mass tolerance in ppm
	 * @param rtTolerance Retention time tolerance in seconds
	 * @param rtDrift 
	 */
	public BaseAlignment(List<AlignmentFile> dataList, AlignmentMethodParam param) {		

		this.dataList = dataList;		
		
		this.massTolerance = param.getMassTolerance();
		 // we use absolute mass tolerance for now to be the same as Lange, et al. (2008)
		this.usePpm = param.isUsePpm();
		this.rtTolerance = param.getRtTolerance();
		
		this.filters = new ArrayList<AlignmentResultFilter>();
		this.filteredResult = new ArrayList<AlignmentRow>();	
			
	}
			
	public AlignmentList align() {
				
		// match features provided by subclasses implementations
		AlignmentList alignmentResult = this.matchFeatures();
		if (alignmentResult == null) {
			return null;
		}
		
		// filter the alignment results sequentially, if necessary
		// System.out.println("Aligned rows " + alignmentResult.getRowsCount() + " with " + this.getClass().getName());
		if (filters.isEmpty()) {
			this.filteredResult = alignmentResult.getRows();
			return alignmentResult;
		}
		
		for (AlignmentResultFilter filter : filters) {

			System.out.println("Applying " + filter.getLabel() + ", initial size = " 
					+ alignmentResult.getRowsCount());
						
			filter.process(alignmentResult);	
			List<AlignmentRow> accepted = filter.getAccepted();
//			accepted.addAll(filter.getRejected());			

			// realign the rejected
//			Set<Feature> whiteList = new HashSet<Feature>();
//			for (AlignmentRow row : filter.getRejected()) {
//				whiteList.add(row.getFirstFeature());
//			}
//			List<AlignmentFile> newDataList = new ArrayList<AlignmentFile>();
//			for (AlignmentFile oldFile : this.dataList) {
//				AlignmentFile newFile = new AlignmentFile(oldFile.getId(), oldFile.getFilename(), oldFile.getDeletedFeatures());
//				for (Feature f : newFile.getFeatures()) {
//					f.setAligned(false);
//				}
//				newDataList.add(newFile);
//			}
//			Builder builder = new Builder(massTolerance, rtTolerance);
//			AlignmentMethodParam param = builder.build();
//			AlignmentMethod baseline = new BaselineAlignment(newDataList, param);
//			AlignmentList list = baseline.align();
//			List<AlignmentRow> deletedRows = list.getRows();
//			accepted.addAll(deletedRows);				
			
			List<AlignmentRow> combined = new ArrayList<AlignmentRow>();
			combined.addAll(accepted);			
			// find last row id
			int lastRowId = alignmentResult.getLastRowId() + 1;
			// rejected got broken down into individual features
			List<AlignmentRow> rejected = filter.getRejected();
			for (AlignmentRow rej : rejected) {
				for (Feature f : rej.getFeatures()) {
					AlignmentRow newRow = new AlignmentRow(rej.getParent(), lastRowId);
					newRow.addFeature(f);
					lastRowId++;
					combined.add(newRow);
				}
			}			
			alignmentResult.setRows(combined);
			this.filteredResult = combined;
					
		}		
		return alignmentResult;
		
	}
		
	public void addFilter(AlignmentResultFilter sizeFilter) {
		filters.add(sizeFilter);
	}
	
	public List<AlignmentRow> getAlignmentResult() {
		return filteredResult;
	}
		
	/**
	 * Implemented by subclasses to do the actual peak matching
	 * @return a list of aligned features in every row
	 */
	protected abstract AlignmentList matchFeatures();
		
}