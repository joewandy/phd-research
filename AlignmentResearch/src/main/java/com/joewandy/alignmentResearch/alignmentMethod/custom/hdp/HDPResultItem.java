package com.joewandy.alignmentResearch.alignmentMethod.custom.hdp;

import java.util.Collections;
import java.util.Set;

import com.joewandy.alignmentResearch.objectModel.Feature;

public class HDPResultItem {

	private Set<Feature> features;
	
	public HDPResultItem(Set<Feature> features) {
		this.features = Collections.unmodifiableSet(features);
	}

	public Set<Feature> getFeatures() {
		return features;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((features == null) ? 0 : features.hashCode());
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
		HDPResultItem other = (HDPResultItem) obj;
		if (features == null) {
			if (other.features != null)
				return false;
		} else if (!features.equals(other.features))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "HDPResultItem [features=" + features + "]";
	}
	
}
