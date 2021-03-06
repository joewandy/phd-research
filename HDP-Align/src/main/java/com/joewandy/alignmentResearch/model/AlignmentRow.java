package com.joewandy.alignmentResearch.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import peakml.chemistry.PeriodicTable;

public class AlignmentRow {

	private AlignmentList parent;
	private int pos;
	private int rowId;
	private Set<Feature> features;
	private boolean aligned;
	private boolean delete;
	private double score;
	private double avgMz;
	private double avgRt;
	private double avgIntensity;
	private double absoluteRtDiff;
	private double minRt;
	
	public AlignmentRow(AlignmentList parent, int rowId) {
		this.parent = parent;
		this.rowId = rowId;
		this.features = new HashSet<Feature>();
		this.aligned = false;
		this.delete = false;
		recomputeStats();
	}
	
	public AlignmentList getParent() {
		return parent;
	}

	public void setParent(AlignmentList parent) {
		this.parent = parent;
	}

	public int getRowId() {
		return rowId;
	}
	
	public void setRowId(int rowId) {
		this.rowId = rowId;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public void addAlignedFeature(Feature feature) {
		feature.setAligned(true);
		this.features.add(feature);
		recomputeStats();
	}

	public void addAlignedFeatures(Set<Feature> features) {
		for (Feature f : features) {
			f.setAligned(true);
		}
		this.features.addAll(features);
		recomputeStats();
	}
	
	public void addFeature(Feature feature) {
		this.features.add(feature);
		recomputeStats();
	}

	public void addFeatures(Set<Feature> features) {
		this.features.addAll(features);
		recomputeStats();
	}

	public Set<Feature> getFeatures() {
		return features;
	}
	
	public Feature asFeature() {
		int peakID = this.getRowId();
		double mz = this.getAverageMz();
		double rt = this.getAverageRt();
		double intensity = this.getAverageIntensity();
		Feature representative = new Feature(peakID, mz, rt, intensity);
		return representative;
	}
	
	public boolean contains(Feature f) {
		if (features.contains(f)) {
			return true;
		}
		return false;
	}
	
	public Set<Feature> intersect(Set<Feature> another) {
		Set<Feature> intersection = new HashSet<Feature>(this.features);
		intersection.retainAll(another);
		return intersection;
	}
	
	public Feature getFirstFeature() {
		Iterator<Feature> iter = features.iterator();
		return iter.next();
	}
	
	public int getFeaturesCount() {
		return features.size();
	}
	
	public double[] getFeatureRts() {
		double[] rts = new double[features.size()];
		int counter = 0;
		for (Feature f : features) {
			rts[counter] = f.getRt();
			counter++;
		}
		return rts;
	}
			
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public boolean isAligned() {
		return aligned;
	}

	public void setAligned(boolean aligned) {
		this.aligned = aligned;
		for (Feature f : this.features) {
			f.setAligned(true);
		}
	}

	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
		for (Feature f : this.features) {
			f.setDelete(true);
		}
	}

	public double getAverageMz() {
		return avgMz;
	}

	public double getAverageRt() {
		return avgRt;
	}
	
	public double getAverageIntensity() {
		return avgIntensity;
	}

	public double getAbsoluteRtDiff() {
		return absoluteRtDiff;
	}

	public double getMinRt() {
		return minRt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + rowId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlignmentRow other = (AlignmentRow) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (rowId != other.rowId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		String output = "SimpleAlignmentRow [rowId=" + rowId + 
				", SCORE=" + this.getScore() + ", featureIDs=[";
		for (Feature f : this.features) {
			output += " data " + f.getFileID() + " peakID " + f.getPeakID() +
					" mz " + f.getMass() + " rt " + f.getRt()
					+ " -- ";
		}
		output += "]";
		output = output.substring(0, output.length()-1);
		output += "]";
		return output;
	}
	
	public boolean rowInRange(AlignmentRow another, double massTol, double rtTol, 
			boolean usePpm) {

		double delta = 0;
		if (usePpm) {
			delta = PeriodicTable.PPM(this.getAverageMz(), massTol);			
		} else {
			delta = massTol;			
		}

		double massLower = this.getAverageMz() - delta;
		double massUpper = this.getAverageMz() + delta;
		double rtLower = this.getAverageRt() - rtTol;
		double rtUpper = this.getAverageRt() + rtTol;	
		
		double massToCheck = another.getAverageMz();
		double rtToCheck = another.getAverageRt();
		if (inRange(massToCheck, massLower, massUpper)) {

			// in the mass range
			if (rtTol != -1) {
				
				// and in retention time range
				if (inRange(rtToCheck, rtLower, rtUpper)) {
					return true;
				}
				 
			} else {

				// not using retention time check
				return true;
			
			}
			
		}
		
		return false;

	}
	
	private boolean inRange(double toCheck, double lowerRange, double upperRange) {
		// TODO: double comparison ?
		if (toCheck > lowerRange && toCheck < upperRange) {
			return true;
		} else {
			return false;
		}
	}
	
	private void recomputeStats() {
		this.avgMz = this.computeAverageMz();
		this.avgRt = this.computeAverageRt();
		this.avgIntensity = this.computeAverageIntensity();
		this.absoluteRtDiff = this.computeAbsoluteRtDiff();
		this.minRt = this.computeMinRt();		
	}

	private double computeAverageMz() {
		double sum = 0;
		for (Feature f : features) {
			sum += f.getMass();
		}
		return sum / getFeaturesCount();
	}
	
	private double computeAverageRt() {
		double sum = 0;
		for (Feature f : features) {
			sum += f.getRt();
		}
		return sum / getFeaturesCount();
	}

	private double computeAverageIntensity() {
		double sum = 0;
		for (Feature f : features) {
			sum += f.getIntensity();
		}
		return sum / getFeaturesCount();
	}
	
	private double computeMinRt() {
		double min = 0;
		for (Feature f : features) {
			if (f.getRt() > min) {
				min = f.getRt();
			}
		}
		return min;
	}
	
	private double computeAbsoluteRtDiff() {
		if (features.size() < 2) {
			return 0;
		} else {
			double diff = 0;
			double mean = computeAverageRt();
			for (Feature f : features) {
				diff += Math.abs(f.getRt()-mean);
			}
			return diff;
		}
	}
		
}
