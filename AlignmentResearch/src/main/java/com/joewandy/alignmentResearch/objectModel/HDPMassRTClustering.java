package com.joewandy.alignmentResearch.objectModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mzmatch.ipeak.util.Common;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;

import com.joewandy.alignmentResearch.alignmentMethod.AlignmentMethodParam;
import com.joewandy.alignmentResearch.alignmentMethod.custom.HdpResult;

public class HDPMassRTClustering implements HDPClustering {

	private HDPClusteringParam hdpParam;
	private List<HDPFile> hdpFiles;
	private List<HDPMetabolite> hdpMetabolites;
	private int hdpMetaboliteId;
	
	/** random noise generator **/
	private final RandomData randomData;	
	
	private int J;
	private int I;
	private List<Integer> fi;
	private List<Double> ti;
	private List<Double> si;
		
	private int fa;
	private double sa;
	
	private Map<HdpResult, HdpResult> resultMap;
	private int samplesTaken;
	
	public HDPMassRTClustering(List<AlignmentFile> dataList, AlignmentMethodParam methodParam) {
		
		this.hdpParam = new HDPClusteringParam();
		
		hdpParam.setUsePpm(methodParam.isUsePpm());
		
		hdpParam.setMu_0(getRTMean(dataList));
		hdpParam.setPsi_0(getMassMean(dataList));
		hdpParam.setSigma_0_prec(1.0/5E6);
		hdpParam.setRho_0_prec(1.0/5E6);
				
		hdpParam.setNsamps(methodParam.getGroupingNSamples());
		hdpParam.setBurnIn(methodParam.getGroupingBurnIn());
		hdpParam.setAlpha_rt(methodParam.getHdpAlphaRt());
		hdpParam.setAlpha_mass(methodParam.getHdpAlphaMass());
		hdpParam.setTop_alpha(methodParam.getHdpTopAlpha());
		
		double globalRtClusterStdev = methodParam.getHdpGlobalRtClusterStdev();
		double globalRtClusterPrec = 1.0 / (globalRtClusterStdev*globalRtClusterStdev);
		hdpParam.setDelta_prec(globalRtClusterPrec);
		
		double localRtClusterStdev = methodParam.getHdpLocalRtClusterStdev();
		double localRtClusterPrec = 1.0 / (localRtClusterStdev*localRtClusterStdev);
		hdpParam.setGamma_prec(localRtClusterPrec);
				
		double massTol = methodParam.getHdpMassTol();
		double massPrec = getMassPrec(massTol);
		hdpParam.setRho_prec(massPrec);
						
		this.randomData = new RandomDataImpl();
		this.resultMap = new HashMap<HdpResult, HdpResult>();
		
		// sample initial metabolite RT
		setI(1);
		this.hdpMetabolites = new ArrayList<HDPMetabolite>();
		this.fi = new ArrayList<Integer>();
		this.ti = new ArrayList<Double>();
		this.si = new ArrayList<Double>();
		for (int i = 0; i < this.I; i++) {
			double rt = randomData.nextGaussian(hdpParam.getMu_0(), Math.sqrt(1/hdpParam.getSigma_0_prec()));
			appendFi(0);
			appendTi(rt);
			appendSi(0.0);
			hdpMetaboliteId = i;
			this.hdpMetabolites.add(new HDPMetabolite(hdpMetaboliteId));
		}
		
	    // assign peaks across files into 1 RT cluster per file, 1 top-level metabolite
		fa = 0;
		sa = 0;
		J = dataList.size();
		hdpFiles = new ArrayList<HDPFile>();
		List<Feature> dataAll = new ArrayList<Feature>();
		int i = 0;
		for (int j=0; j < dataList.size(); j++) {
						
			HDPFile hdpFile = new HDPFile(j);
			AlignmentFile alignmentFile = dataList.get(j);
			hdpFile.addFeatures(alignmentFile.getFeatures());
			hdpFile.setK(1);
			
			fa += hdpFile.N();
			sa += hdpFile.getMassSum(this.hdpParam.isUsePpm());
			
			dataAll.addAll(hdpFile.getFeatures());

			// 1 x N, peaks to clusters assignment
			for (int n = 0; n < hdpFile.N(); n++) {
				hdpFile.appendZ(0); // assign all features under 1 RT cluster
			}

			// 1 x K, clusters to metabolites assignment
			for (int k = 0; k < hdpFile.K(); k++) {
				hdpFile.appendTopZ(0); // assign all RT clusters under 1 metabolite 
				// 1 x K, sample initial the clusters' RT
				int parentI = hdpFile.topZ(k);
				double ti = this.ti.get(parentI);
				double tij = randomData.nextGaussian(ti, Math.sqrt(1/hdpParam.getDelta_prec()));
				hdpFile.appendTij(tij);
				addSi(parentI, tij);
				hdpFile.appendCountZ(hdpFile.N());
				hdpFile.appendSumZ(hdpFile.getRtSum());
			}
			
			addFi(i, hdpFile.K());
			hdpFiles.add(hdpFile);
			
		}
		
		// assign all peaks under 1 metabolite
		for (i = 0; i < this.I; i++) {
			
			HDPMetabolite met = hdpMetabolites.get(hdpMetaboliteId);
			hdpMetaboliteId++;			
			met.setA(1);
			double theta = randomData.nextGaussian(hdpParam.getPsi_0(), Math.sqrt(1/hdpParam.getRho_prec())); 
			met.appendTheta(theta);
			met.appendFa(fa);
			met.appendSa(sa);
			for (int n = 0; n < dataAll.size(); n++) {
				met.appendV(0);
			}
			met.addPeakData(dataAll);
			
		}
		
	}
	
	private double getMassPrec(double massTol) {
		double prec = 0;
		if (this.hdpParam.isUsePpm()) {
			double logOnePpm = Math.log(1000001) - Math.log(1000000);
			double logDiff = logOnePpm * massTol; 
			double stdev = logDiff/2; // assume 2 stdev = logDiff
			prec = 1.0 / (stdev*stdev);
		} else {
			// in case of absolute mass tolerance, not using log scale 
			// to make it easier to specify the value
			prec = 1.0 / (massTol*massTol);
		}
		System.out.println("usePpm = " + this.hdpParam.isUsePpm() + ", massPrec = " + prec);
		return prec;			
	}
	
	private double getFeatureMass(Feature f) {
		if (this.hdpParam.isUsePpm()) {
			return f.getMassLog();
		} else {
			return f.getMass();
		}
	}

	private double getRTMean(List<AlignmentFile> dataList) {
		double sum = 0;
		int count = 0;
		for (AlignmentFile file : dataList) {
			for (Feature f : file.getFeatures()) {
				sum += f.getRt();
				count++;
			}
		}
		double mean = sum / count;
		return mean;
	}

	private double getMassMean(List<AlignmentFile> dataList) {
		double sum = 0;
		int count = 0;
		for (AlignmentFile file : dataList) {
			for (Feature f : file.getFeatures()) {
				sum += Math.exp(getFeatureMass(f));
				count++;
			}
		}
		double mean = sum / count;
		return Math.log(mean);
	}
	
	public void run() {

		for (int s = 0; s < hdpParam.getNsamps(); s++) {

//			boolean printMsg = false;
//			if ((s+1) % 10 == 0) {
//				printMsg = true;
//			}			
			boolean printMsg = true;
			
			if ((s+1) > hdpParam.getBurnIn()) {
				if (printMsg) {
					System.out.print(String.format("Sample S#%05d", (s+1)));					
				}
			} else {
				if (printMsg) {
					System.out.print(String.format("Sample B#%05d", (s+1)));					
				}
			}
			
			long startTime = System.currentTimeMillis();
			assignPeakMassRt();
			updateParametersMassRt();
			long endTime = System.currentTimeMillis();
			double timeTaken = (endTime - startTime) / 1000.0;

			StringBuilder sb = new StringBuilder();
			if ((s+1) > hdpParam.getBurnIn()) {
				// store the actual samples
				sb.append(String.format("\ttime=%5.2fs I=%d ", timeTaken, this.I));
				updateResultMap();
				samplesTaken++;
			} else {
				// discard the burn-in samples
				sb.append(String.format("\ttime=%5.2fs I=%d ", timeTaken, this.I));			
			}
			
			sb.append("all_A = [");
			for (int i = 0; i < this.I; i++) {
				HDPMetabolite met = hdpMetabolites.get(i);
				int A = met.A();
				String formatted = String.format(" %3d", A);
				sb.append(formatted);
			}
			sb.append(" ]");

			if (printMsg) {
				System.out.println(sb.toString());				
			}
			
		}
		
	}
	
	public Map<HdpResult, HdpResult> getSimilarityResult() {
		
		System.out.println("Samples taken = " + samplesTaken);
		for (Entry<HdpResult, HdpResult> e : resultMap.entrySet()) {
			HdpResult value = e.getValue();
			value.setSimilarity(value.getSimilarity() / samplesTaken);
		}
		
		return this.resultMap;
	}

	private void assignPeakMassRt() {
		
		// TODO: loop across all files randomly
		for (HDPFile hdpFile : hdpFiles) {

//			System.out.print("\t- File" + hdpFile.getId() + " ");
			
			int j = hdpFile.getId();
			
			// TODO: loop across peaks randomly
			assert(hdpFile.N() == hdpFile.Zsize());
			for (int n = 0; n < hdpFile.N(); n++) {
							
//				if (n%100==0) {
//					System.out.print('.');
//				}
				
				Feature thisPeak = hdpFile.getFeatures().get(n);
				
				// find the RT cluster
				int k = hdpFile.Z(n);
				int i = hdpFile.topZ(k);
				
				// find the mass cluster
				HDPMetabolite met = hdpMetabolites.get(i);
				int peakPos = met.findPeakPos(thisPeak);
				assert (peakPos != -1) : "file is " + j;
				assert (met.vSize() == met.peakDataSize());
				
				// %%%%%%%%%% 1. remove peak from model %%%%%%%%%%
				
				hdpFile.setZ(n, -1);
				hdpFile.decreaseCountZ(k);
				hdpFile.subsSumZ(k, thisPeak.getRt());
				int a = met.V(peakPos);
				met.removeV(peakPos);
				met.removePeakData(peakPos);
				met.decreaseFa(a);
				met.subsSa(a, getFeatureMass(thisPeak));
				
				// does this result in an empty mass cluster ?
				if (met.fa(a) == 0) {
					
					// delete this mass cluster indexed by a
					met.decreaseA();
					met.removeTheta(a);
					met.removeFa(a);
					met.removeSa(a);
					
					// decrease all indices in V that is > a by 1 since a is deleted
					met.reindexV(a);
					
					assert(met.thetasSize() == met.A());					
					
				}
				
				// does this result in an empty RT cluster ?
				if (hdpFile.countZ(k) == 0) {
					
					// delete the RT cluster
					hdpFile.decreaseK();
					hdpFile.reindexZ(k);
					
					// update for bookkeeping
					hdpFile.removeCountZ(k);
					hdpFile.removeSumZ(k);
					double tij = hdpFile.tij(k);
					hdpFile.removeTij(k);
					
					// remove assignment of cluster to parent metabolite
					hdpFile.removeTopZ(k);
					// decrease count of clusters under parent metabolite
					decreaseFi(i);
					subsSi(i, tij);
					
					// does this result in an empty metabolite ?
					if (fi(i) == 0) {
						
						// delete the metabolite across all replicates
						for (int rep = 0; rep < J; rep++) {
							
							HDPFile repFile = hdpFiles.get(rep);
							repFile.reindexTopZ(i);
							
						}
						
						// delete top-level info too
						removeFi(i);
						removeSi(i);
						removeTi(i);
						decreaseI();
						
						// delete mass clusters info related to this metabolite too
						hdpMetabolites.remove(i);
						
					}
					
				}
				
				// %%%%%%%%%% 2. perform peak assignments %%%%%%%%%%
				
				// for current RT cluster, first compute the RT term
				double[] tijArr = hdpFile.tijArray();
				double prec = hdpParam.getGamma_prec();
				double[] rtTermLogLike = new double[tijArr.length];
				assert(hdpFile.K()==rtTermLogLike.length);
				for (int idx = 0; idx < tijArr.length; idx++) {
					double tij = tijArr[idx];
					double logLikelihood = computeLogLikelihood(thisPeak.getRt(), tij, prec);
					rtTermLogLike[idx] = logLikelihood;
 				}
				
				// then for every RT cluster, compute the likelihood of this peak to be in the mass clusters linked to it
				double[] metaboliteMassLike = new double[I];
				for (int metIndex = 0; metIndex < I; metIndex++) {

					HDPMetabolite thisMetabolite = hdpMetabolites.get(metIndex);
					
					// first consider existing mass clusters
					int countFa = thisMetabolite.faSize();
					double[] massTermPrior = new double[countFa+1]; // extra 1 for new cluster
					for (int idx = 0; idx < countFa; idx++) {
						massTermPrior[idx] = thisMetabolite.fa(idx);
					}
					int countTheta = thisMetabolite.thetasSize();
					double[] mu = new double[countTheta+1]; // extra 1 for new cluster
					double[] prec2 = new double[countTheta+1];
					for (int idx = 0; idx < countTheta; idx++) {
						mu[idx] = thisMetabolite.theta(idx);
						prec2[idx] = hdpParam.getRho_prec();
					}
					
					// then add the term for the new mass cluster
					assert(massTermPrior.length == mu.length);
					assert(massTermPrior.length == prec2.length);
					int end = massTermPrior.length-1;
					massTermPrior[end] = hdpParam.getAlpha_mass();
					massTermPrior = normalise(massTermPrior, sum(massTermPrior));
					mu[end] = hdpParam.getPsi_0();
					double temp = 1/(1/hdpParam.getRho_prec() + 1/hdpParam.getRho_0_prec());
					prec2[end] = temp;

					// compute posterior probability
					double[] massTermLikelihood = new double[prec2.length];
					double[] massTermPost = new double[prec2.length];					
					for (int idx = 0; idx < prec2.length; idx++) {
						double likelihood = computeLikelihood(getFeatureMass(thisPeak), mu[idx], prec2[idx]);
						massTermLikelihood[idx] = likelihood;
						massTermPost[idx] = massTermPrior[idx] * massTermLikelihood[idx];
					}
					assert(massTermPost.length==thisMetabolite.A()+1);

					// marginalise over all the mass clusters by summing over them, then take the log for use later
					metaboliteMassLike[metIndex] = sum(massTermPost);					
				
				}								
				double[] massTermLogLike = new double[hdpFile.K()]; // % 1 by K, stores the mass log likelihood linked to each RT cluster
				for (int thisCluster = 0; thisCluster < hdpFile.K(); thisCluster++) {
					
					// the RT cluster's parent metabolite
					int metIndex = hdpFile.topZ(thisCluster);
					massTermLogLike[thisCluster] = Math.log(metaboliteMassLike[metIndex]);
					
				}
				
				// finally, the likelihood of peak going into a current cluster eq #15 is the RT * mass terms
				double[] currentClusterLogLike = addArray(rtTermLogLike, massTermLogLike);
				
				// now compute the likelihood for peak going into new cluster, eq #20      
	            // first, compute p( x_nj | existing metabolite ), eq #21
				double denum = sum(fiArray()) + hdpParam.getTop_alpha();
				double[] currentMetabolitePost = new double[I];
				for (int idx = 0; idx < I; idx++) {
					double prior = fi(idx) / denum;
					// first compute the RT term
					double mu = ti(idx);
					prec = 1/(1/hdpParam.getGamma_prec() + 1/hdpParam.getDelta_prec());
					double rtLikelihood = computeLikelihood(thisPeak.getRt(), mu, prec);
					// then compute the mass term
					double massLikelihood = metaboliteMassLike[idx];
					// multiply likelihood with prior to get the posterior p( % d_jn | existing metabolite )
					double likelihood = rtLikelihood * massLikelihood;
					double posterior = prior * likelihood;
					currentMetabolitePost[idx] = posterior;
				}
				
				// then compute p( d_jn | new metabolite ), eq #22
				double prior = hdpParam.getTop_alpha()/denum;
				double mu = hdpParam.getMu_0();
				prec = 1/(1/hdpParam.getGamma_prec() + 1/hdpParam.getDelta_prec() + 1/hdpParam.getSigma_0_prec()); 
				double rtLikelihood = computeLikelihood(thisPeak.getRt(), mu, prec);
				mu = hdpParam.getPsi_0();
				prec = 1/(1/hdpParam.getRho_prec() + 1/hdpParam.getRho_0_prec()); 
				double massLikelihood = computeLikelihood(thisPeak.getMass(), mu, prec); // TODO: use log-likelihood!
				double likelihood = rtLikelihood * massLikelihood;
				double newMetabolitePost = prior * likelihood;
				
				// sum over for eq #17
				double[] metabolitePost = append(currentMetabolitePost, newMetabolitePost);
				double newClusterLogLike = Math.log(sum(metabolitePost));
							
				// pick either existing or new RT cluster
				// set the prior
				int[] countZArr = hdpFile.countZArray();
				double[] clusterPrior = append(toDouble(countZArr), hdpParam.getAlpha_rt());
				clusterPrior = normalise(clusterPrior, sum(clusterPrior));
				double[] clusterLogLike = append(currentClusterLogLike, newClusterLogLike);
				double[] clusterLogPost = addArray(logArray(clusterPrior), clusterLogLike);
				// compute and sample from posterior
				double[] clusterPost = expArray(subsArray(clusterLogPost, max(clusterLogPost)));
				clusterPost = normalise(clusterPost, sum(clusterPost));
				k = sample(clusterPost);
				
				if ((k+1) > hdpFile.K()) {
					
					// new cluster
					hdpFile.increaseK();
					// resize peak-cluster assignment to include the new cluster
					hdpFile.appendCountZ(0);
					hdpFile.appendSumZ(0.0);
					// resize cluster-metabolite assignment to include the new cluster
					hdpFile.appendTopZ(0);
					// decide which metabolite to assign the new cluster to
					metabolitePost = normalise(metabolitePost, sum(metabolitePost));
					i = sample(metabolitePost);
					
					if ((i+1) <= this.I) {
						
						// current metabolite
						hdpFile.setTopZ(k, i);
						increaseFi(i);
						
					} else {
						
						// new metabolite
						increaseI();
						
						// assign the cluster under this new metabolite
						hdpFile.setTopZ(k, i);
						appendFi(1);
						appendSi(0.0);
						
						// generate ti given data RT
						double temp = 1 / (1/hdpParam.getGamma_prec() + 1/hdpParam.getDelta_prec());
						prec = temp + hdpParam.getSigma_0_prec();
						mu = 1/prec * (temp*thisPeak.getRt() + hdpParam.getSigma_0_prec()*hdpParam.getMu_0());
						double newTi = randomData.nextGaussian(mu, Math.sqrt(1/prec));
						appendTi(newTi);
						
						// create empty mass cluster data structures for use later
						hdpMetabolites.add(new HDPMetabolite(hdpMetaboliteId));
						hdpMetaboliteId++;
						
					}
					
					// generate tij given ti and data RT
					prec = hdpParam.getGamma_prec() + hdpParam.getDelta_prec();
					mu = 1/prec * (hdpParam.getGamma_prec()*thisPeak.getRt() + hdpParam.getDelta_prec()*ti(i));
					double newTij = randomData.nextGaussian(mu, Math.sqrt(1/prec));
					hdpFile.appendTij(newTij);
					addSi(i, newTij);
					
				} // end new RT cluster
				
				// now given RT cluster k, we can assign peak to the mass clusters linked to k
				// find the parent metabolite first  
				i = hdpFile.topZ(k);
				
				// for existing and new mass cluster
				HDPMetabolite metabolite_i = hdpMetabolites.get(i);
				double[] logPrior = append(logArray(metabolite_i.faArray()), Math.log(hdpParam.getAlpha_mass()));
				double[] mu2 = append(metabolite_i.thetasArray(), hdpParam.getPsi_0());
				double[] temp = new double[metabolite_i.thetasSize()];
				fill(temp, hdpParam.getRho_prec());
				double temp2 = 1/( 1/hdpParam.getRho_prec() + 1/hdpParam.getRho_0_prec() );
				double[] prec2 = append(temp, temp2);
				
				// compute likelihood and posterior 
				double[] logLikelihood = new double[mu2.length];
				for (int idx = 0; idx < logLikelihood.length; idx++) {
					double entry = computeLogLikelihood(getFeatureMass(thisPeak), mu2[idx], prec2[idx]);
					logLikelihood[idx] = entry;
				}
				double[] logPost = addArray(logPrior, logLikelihood);
				
				// pick the mass cluster
				double[] post = expArray(subsArray(logPost, max(logPost)));
				post = normalise(post, sum(post));
				a = sample(post);
				
				if ((a+1) > metabolite_i.A()) {
					
					// make a new mass cluster
					metabolite_i.increaseA();
					metabolite_i.appendFa(0);
					metabolite_i.appendSa(0.0);
					
					// sample new mass cluster value
					double sigma_a = hdpParam.getRho_prec() + hdpParam.getRho_0_prec();
					double mu_a = 1/sigma_a * (hdpParam.getRho_prec()*getFeatureMass(thisPeak) + hdpParam.getRho_0_prec()*hdpParam.getPsi_0());
					double theta_ia = randomData.nextGaussian(mu_a, Math.sqrt(1/sigma_a));
					metabolite_i.appendTheta(theta_ia);
					assert(metabolite_i.thetasSize()==metabolite_i.A());
					
				}
				
				// %%%%%%%%%% 3. add peak back into model %%%%%%%%%%
				
				// assign the peak under mass cluster a
				metabolite_i.increaseFa(a);
				metabolite_i.addSa(a, getFeatureMass(thisPeak));
				metabolite_i.appendV(a);
				metabolite_i.addPeakData(thisPeak);
				
				// assign the peak under RT cluster k
				hdpFile.setZ(n, k);
				hdpFile.increaseCountZ(k);
				hdpFile.addSumZ(k, thisPeak.getRt());
				
				// %%%%%%%%%% 4. maintain model state consistency %%%%%%%%%%

				assert(this.hdpMetabolites.size() == this.I);
				assert(metabolite_i.thetasSize()==metabolite_i.A());
				assert(metabolite_i.faSize()==metabolite_i.A());
				assert(metabolite_i.saSize()==metabolite_i.A());
				
			} // end loop across peaks randomly
		
//			System.out.println();
			
		} // end loop across files randomly
		
	}
	
	private void updateParametersMassRt() {
		
		// %%%%% update RT clusters %%%%%        
		for (int j = 0; j < hdpFiles.size(); j++) {

			HDPFile hdpFile = hdpFiles.get(j);
			for (int k = 0; k < hdpFile.K(); k++) {
				
				// find parent metabolite of this cluster
				int i = hdpFile.topZ(k);
				double ti = ti(i);
				double sum_xn = hdpFile.sumZ(k);
				int count_peaks = hdpFile.countZ(k);
				
				// draw new tij
	            double prec = hdpParam.getDelta_prec() + count_peaks*hdpParam.getGamma_prec();
	            double mu = (1/prec) * ( ti*hdpParam.getDelta_prec() + hdpParam.getGamma_prec()*sum_xn );
				double new_tij = randomData.nextGaussian(mu, Math.sqrt(1/prec));
				hdpFile.setTij(k, new_tij);
				
			}
			
		}
		
		// %%%%% update metabolite RT %%%%%
		for (int i = 0; i < hdpMetabolites.size(); i++) {
		
			double sum_clusters = si(i);
			double count_clusters = fi(i);
			
			// draw new ti given the cluster RTs
			double prec = hdpParam.getSigma_0_prec() + count_clusters*hdpParam.getDelta_prec();
	        double mu = (1/prec) * ( hdpParam.getMu_0()*hdpParam.getSigma_0_prec() + hdpParam.getDelta_prec()*sum_clusters );
	        double new_ti = randomData.nextGaussian(mu, Math.sqrt(1/prec));
	        setTi(i, new_ti);
	        
	        // also update all the mass clusters linked to this metabolite
			HDPMetabolite met = hdpMetabolites.get(i);
			for (int a = 0; a < met.A(); a++) {
				prec = hdpParam.getRho_0_prec() + (hdpParam.getRho_prec() + met.fa(a));
				mu = (1/prec) * (hdpParam.getRho_0_prec() + hdpParam.getPsi_0()) + (hdpParam.getRho_prec() * met.sa(a));
				double newTheta = randomData.nextGaussian(mu, Math.sqrt(1/prec)); 
				met.setTheta(a, newTheta);
			}
	        
	        
		}
				
	}
	
	private void updateResultMap() {
		
		// for all metabolite
		for (int i = 0; i < hdpMetabolites.size(); i++) {

			HDPMetabolite met = hdpMetabolites.get(i);

			// for all mass clusters
			for (int a = 0; a < met.A(); a++) {
				List<Feature> peaksInside = met.getPeaksInMassCluster(a);
				for (Feature f1 : peaksInside) {
					for (Feature f2 : peaksInside) {
						HdpResult hdpRes = new HdpResult(f1, f2);
						if (resultMap.containsKey(hdpRes)) {
							HdpResult current = resultMap.get(hdpRes);
							current.setSimilarity(current.getSimilarity()+1);
						} else {
							hdpRes.setSimilarity(1);
							resultMap.put(hdpRes, hdpRes);
						}
					}
				}
			}
			
		}
		
	}
	
	private void setI(int i) {
		I = i;
	}

	private void increaseI() {
		I++;
	}
	
	private void decreaseI() {
		I--;
	}
	
	private int fi(int i) {
		return this.fi.get(i);
	}
	
	private void setFi(int i, int newFi) {
		this.fi.set(i, newFi);
	}
	
	private void addFi(int i, int amount) {
		int fi = this.fi.get(i);
		fi += amount;
		setFi(i, fi);
	}
	
	private void increaseFi(int i) {
		int fi = this.fi.get(i);
		fi++;
		this.fi.set(i, fi);
	}
	
	private void decreaseFi(int i) {
		int fi = this.fi.get(i);
		fi--;
		this.fi.set(i, fi);
	}

	private void appendFi(int count) {
		this.fi.add(count);
	}
	
	private void removeFi(int i) {
		this.fi.remove(i);
	}
	
	public int[] fiArray() {
		Integer[] temp = fi.toArray(new Integer[fi.size()]);
		return ArrayUtils.toPrimitive(temp);
	}	
	
	private double ti(int i) {
		return this.ti.get(i);
	}
	
	private void setTi(int i, double ti) {
		this.ti.set(i, ti);
	}
		
	private void appendTi(double ti) {
		this.ti.add(ti);
	}
	
	private void removeTi(int i) {
		this.ti.remove(i);
	}
	
	private void setSi(int i, double si) {
		this.si.set(i, si);
	}
	
	private double si(int i) {
		return this.si.get(i);
	}
	
	private void addSi(int i, double amount) {
		double si = this.si.get(i);
		si += amount;
		setSi(i, si);
	}

	private void subsSi(int i, double amount) {
		double si = this.si.get(i);
		si -= amount;
		setSi(i, si);
	}

	private void appendSi(double amount) {
		this.si.add(amount);
	}
	
	private void removeSi(int i) {
		this.si.remove(i);
	}
		
	private double sum(double[] arr) {
		double sum = 0;
		for (double elem : arr) {
			sum += elem;
		}
		return sum;
	}

	private double sum(int[] arr) {
		double sum = 0;
		for (double elem : arr) {
			sum += elem;
		}
		return sum;
	}
	
	private double max(double[] arr) {
		double max = Double.NEGATIVE_INFINITY;
		for (double elem : arr) {
			if (elem > max) {
				max = elem;
			}
		}
		return max;
	}
	
	private double[] normalise(double[] arr, double denum) {
		double[] result = arr.clone();
		for (int i = 0; i < result.length; i++) {
			double currVal = result[i];
			double newVal = currVal / denum;
			result[i] = newVal;
		}
		return result;
	}
	
	private double[] addArray(double[] arr1, double[] arr2) {
		assert(arr1.length == arr2.length);
		double[] result = arr1.clone();
		for (int i = 0; i < arr2.length; i++) {
			result[i] += arr2[i];
		}
		return result;
	}

	private double[] subsArray(double[] arr, double scalar) {
		double[] result = arr.clone();
		for (int i = 0; i < arr.length; i++) {
			result[i] -= scalar;
		}
		return result;
	}
	
	private double[] append(double[] arr, double scalar) {
		double[] result = new double[arr.length+1];
		int end = result.length-1;
		for (int i = 0; i < result.length; i++) {
			if (i == end) {
				result[i] = scalar;
			} else {
				result[i] = arr[i];
			}
		}
		return result;	
	}
	
	private double[] logArray(double[] arr) {
		double[] result = arr.clone();
		for (int i = 0; i < arr.length; i++) {
			result[i] = Math.log(arr[i]);
		}
		return result;
	}

	private double[] expArray(double[] arr) {
		double[] result = arr.clone();
		for (int i = 0; i < arr.length; i++) {
			result[i] = Math.exp(arr[i]);
		}
		return result;
	}
	
	private double[] cumsumArray(double[] arr) {
		double[] result = new double[arr.length];
		double sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i];
			result[i] = sum;
		}
		return result;
	}
	
	private int sample(double[] distribution) {
		double randomNumber = randomData.nextUniform(0, 1);
		double[] cumsum = cumsumArray(distribution);
		int selectedIndex = 0;
		for (selectedIndex = 0; selectedIndex < cumsum.length; selectedIndex++) {
			double c = cumsum[selectedIndex];
			if (randomNumber <= c) {
				break;
			}
		}
		return selectedIndex;
	}
	
	private void fill(double[] arr, double value) {
		for (int i = 0; i < arr.length; i++) {
			arr[i] = value;
		}
	}
	
	private double computeLogLikelihood(double x, double mu, double prec) {
		/*
		 * f(x) 	= sqrt(prec/2pi)*e^((-prec(x-mu)^2)/2)
		 * log f(x) = log (sqrt(prec/2pi)*e^((-prec(x-mu)^2)/2))
		 * 			= log(sqrt(prec/2pi)) + log(e^((-prec(x-mu)^2)/2))
		 * 			= 0.5 log(prec) - 0.5 log(2pi) + ((-prec(x-mu)^2)/2)
		 * 			= 0.5 log(prec) - 0.5 log(2pi) - 0.5 * prec * (x-mu)^2
		 */
		double logLikelihood = -0.5 * Math.log(2*Math.PI);
		logLikelihood += 0.5 * Math.log(prec);
		logLikelihood -= 0.5 * prec * Math.pow(x - mu, 2);
		return logLikelihood;
	}
	
	private double computeLikelihood(double x, double mu, double prec) {
		double likelihood = Math.sqrt(prec/(2*Math.PI)) * Math.exp((-prec*Math.pow(x-mu, 2))/2);
		return likelihood;
	}
	
	private double[] toDouble(int[] arr) {
		double[] res = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			res[i] = arr[i];
		}
		return res;
	}
	
}
