/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.*;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.MergeSorter;
import ciir.umass.edu.utilities.MyThreadPool;
import ciir.umass.edu.utilities.RankLibError;
import ciir.umass.edu.utilities.SimpleMath;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author vdang
 *
 *  This class implements LambdaMART.
 *  Q. Wu, C.J.C. Burges, K. Svore and J. Gao. Adapting Boosting for Information Retrieval Measures. 
 *  Journal of Information Retrieval, 2007.
 */
public class LambdaMART extends Ranker {

	/** The number of trees to create in this ensemble. */
	public int nTrees = 1000;
	/** Learning rate, or shrinkage. */
	public float learningRate = 0.1F;
	/** Number of threshold candidates. */
	public int nThreshold = 256;
	/**
	 * If no performance gain on the *VALIDATION* data is observed in #rounds, stop the training process right away.
	 */
	public int nRoundToStopEarly = 100;
	public int nTreeLeaves = 10;
	public int minLeafSupport = 1;

	//Local variables
	protected float[][] thresholds = null;
	protected Ensemble ensemble = null;
	protected double[] modelScores = null;//on training data
	
	protected double[][] modelScoresOnValidation = null;
	protected int bestModelOnValidation = Integer.MAX_VALUE-2;
	
	//Training instances prepared for MART
	protected DataPoint[] martSamples = null;//Need initializing only once
	protected int[][] sortedIdx = null;//sorted list of samples in @martSamples by each feature -- Need initializing only once 
	protected FeatureHistogram hist = null;
	protected double[] pseudoResponses = null;//different for each iteration
	protected double[] weights = null;//different for each iteration
	
	public LambdaMART() {	}
	public LambdaMART(List<RankList> samples, int[] features, MetricScorer scorer) {
		super(samples, features, scorer);
	}

	/** Construct this from a "prototype" instance that has all features configured as you would like... */
	public LambdaMART(LambdaMART config) {
		this.nTrees = config.nTrees;
		this.learningRate = config.learningRate;
		this.nThreshold = config.nThreshold;
		this.nRoundToStopEarly = config.nRoundToStopEarly;
		this.nTreeLeaves = config.nTreeLeaves;
		this.minLeafSupport = config.minLeafSupport;
		this.factory = config.factory;
	}

	public void init()
	{
		PRINT("Initializing... ");		
		//initialize samples for MART
		int dpCount = 0;
		for (RankList rl : samples) {
			dpCount += rl.size();
		}
		int current = 0;
		martSamples = new DataPoint[dpCount];
		modelScores = new double[dpCount];
		pseudoResponses = new double[dpCount];
		weights = new double[dpCount];
		for (RankList rl : samples) {
			for (int j = 0; j < rl.size(); j++) {
				martSamples[current + j] = rl.get(j);
				modelScores[current + j] = 0.0F;
				pseudoResponses[current + j] = 0.0F;
				weights[current + j] = 0;
			}
			current += rl.size();
		}			
		
		//sort (MART) samples by each feature so that we can quickly retrieve a sorted list of samples by any feature later on.
		sortedIdx = new int[features.length][];
		MyThreadPool p = MyThreadPool.getInstance();
		if(p.size() == 1)//single-thread
			sortSamplesByFeature(0, features.length-1);
		else//multi-thread
		{
			int[] partition = p.partition(features.length);
			for(int i=0;i<partition.length-1;i++)
				p.execute(new SortWorker(this, partition[i], partition[i+1]-1));
			p.await();
		}
		
		//Create a table of candidate thresholds (for each feature). Later on, we will select the best tree split from these candidates 
		thresholds = new float[features.length][];
		for(int f=0;f<features.length;f++) {
			//For this feature, keep track of the list of unique values and the max/min 
			List<Float> values = new ArrayList<>();
			float fmax = Float.NEGATIVE_INFINITY;
			float fmin = Float.MAX_VALUE;
			for(int i=0;i<martSamples.length;i++)
			{
				int k = sortedIdx[f][i];//get samples sorted with respect to this feature
				float fv = martSamples[k].getFeatureValue(features[f]);
				values.add(fv);
				if(fmax < fv)
					fmax = fv;
				if(fmin > fv)
					fmin = fv;
				//skip all samples with the same feature value
				int j=i+1;
				while(j < martSamples.length)
				{
					if(martSamples[sortedIdx[f][j]].getFeatureValue(features[f]) > fv)
						break;
					j++;
				}
				i = j-1;//[i, j] gives the range of samples with the same feature value
			}
			
			if(values.size() <= nThreshold || nThreshold == -1) {
				thresholds[f] = new float[values.size()+1];
				for(int i=0;i<values.size();i++)
					thresholds[f][i] = values.get(i);
				thresholds[f][values.size()] = Float.MAX_VALUE;
			} else {
				float step = (Math.abs(fmax - fmin))/nThreshold;
				thresholds[f] = new float[nThreshold+1];
				thresholds[f][0] = fmin;
				for(int j=1;j<nThreshold;j++)
					thresholds[f][j] = thresholds[f][j-1] + step;
				thresholds[f][nThreshold] = Float.MAX_VALUE;
			}
		}
		
		if(validationSamples != null)
		{
			modelScoresOnValidation = new double[validationSamples.size()][];
			for(int i=0;i<validationSamples.size();i++)
			{
				modelScoresOnValidation[i] = new double[validationSamples.get(i).size()];
				Arrays.fill(modelScoresOnValidation[i], 0);
			}
		}
		
		//compute the feature histogram (this is used to speed up the procedure of finding the best tree split later on)
		hist = new FeatureHistogram();
		hist.construct(martSamples, pseudoResponses, sortedIdx, features, thresholds);
		//we no longer need the sorted indexes of samples
		sortedIdx = null;
		
		System.gc();
		PRINTLN("[Done]");
	}
	public void learn() {
		ensemble = new Ensemble();
		
		PRINTLN("---------------------------------");
		PRINTLN("Training starts...");
		PRINTLN("---------------------------------");
		PRINTLN(new int[]{7, 9, 9}, new String[]{"#tree", scorer.name()+"-T", scorer.name()+"-V"});
		PRINTLN("---------------------------------");		
		
		//Start the gradient boosting process
		for(int m=0; m<nTrees; m++)
		{
			PRINT(new int[]{7}, new String[]{(m+1)+""});
			
			//Compute lambdas (which act as the "pseudo responses")
			//Create training instances for MART:
			//  - Each document is a training sample
			//	- The lambda for this document serves as its training label
			computePseudoResponses();
			
			//update the histogram with these training labels (the feature histogram will be used to find the best tree split)
			hist.update(pseudoResponses);
		
			//Fit a regression tree			
			RegressionTree rt = new RegressionTree(nTreeLeaves, martSamples, pseudoResponses, hist, minLeafSupport);
			rt.fit();
			
			//Add this tree to the ensemble (our model)
			ensemble.add(rt, learningRate);

			//update the outputs of the tree (with gamma computed using the Newton-Raphson method) 
			updateTreeOutput(rt);
			
			//Update the model's outputs on all training samples
			List<Split> leaves = rt.leaves();
			for (Split s : leaves) {
				int[] idx = s.getSamples();
				for (int i : idx)
					modelScores[i] += learningRate * s.getOutput();
			}

			//clear references to data that is no longer used
			rt.clearSamples();

			//Evaluate the current model
			scoreOnTrainingData = computeModelScoreOnTraining();
			//**** NOTE ****
			//The above function to evaluate the current model on the training data is equivalent to a single call:
			//
			//		scoreOnTrainingData = scorer.score(rank(samples);
			//
			//However, this function is more efficient since it uses the cached outputs of the model (as opposed to re-evaluating the model 
			//on the entire training set).
			
			PRINT(new int[]{9}, new String[]{SimpleMath.round(scoreOnTrainingData, 4) + ""});			
			
			//Evaluate the current model on the validation data (if available)
			if(validationSamples != null)
			{
				//Update the model's scores on all validation samples
				for(int i=0;i<modelScoresOnValidation.length;i++)
					for(int j=0;j<modelScoresOnValidation[i].length;j++)
						modelScoresOnValidation[i][j] += learningRate * rt.eval(validationSamples.get(i).get(j));
				
				//again, equivalent to scoreOnValidation=scorer.score(rank(validationSamples)), but more efficient since we use the cached models' outputs
				double score = computeModelScoreOnValidation();
				
				PRINT(new int[]{9}, new String[]{SimpleMath.round(score, 4) + ""});
				if(score > bestScoreOnValidationData)
				{
					bestScoreOnValidationData = score;
					bestModelOnValidation = ensemble.treeCount()-1;
				}
			}
			
			PRINTLN("");
			
			//Should we stop early?
			if(m - bestModelOnValidation > nRoundToStopEarly)
				break;
		}
		
		//Rollback to the best model observed on the validation data
		while(ensemble.treeCount() > bestModelOnValidation+1)
			ensemble.remove(ensemble.treeCount()-1);
		
		//Finishing up
		scoreOnTrainingData = scorer.score(rank(samples));
		PRINTLN("---------------------------------");
		PRINTLN("Finished sucessfully.");
		PRINTLN(scorer.name() + " on training data: " + SimpleMath.round(scoreOnTrainingData, 4));
		if(validationSamples != null)
		{
			bestScoreOnValidationData = scorer.score(rank(validationSamples));
			PRINTLN(scorer.name() + " on validation data: " + SimpleMath.round(bestScoreOnValidationData, 4));
		}
		PRINTLN("---------------------------------");
	}
	public double eval(DataPoint dp) { return ensemble.eval(dp); }
	public Ranker createNew() { return new LambdaMART(this); }
	public String toString() { return ensemble.toString(); }
	public String model() {
		String output = "## " + name() + "\n";
		output += "## No. of trees = " + nTrees + "\n";
		output += "## No. of leaves = " + nTreeLeaves + "\n";
		output += "## No. of threshold candidates = " + nThreshold + "\n";
		output += "## Learning rate = " + learningRate + "\n";
		output += "## Stop early = " + nRoundToStopEarly + "\n";
		output += "\n";
		output += toString();
		return output;
	}
  @Override
	public void loadFromString(String fullText) {
		try {
			String content;
			StringBuilder model = new StringBuilder();
			try (BufferedReader in = new BufferedReader(new StringReader(fullText))) {
				while ((content = in.readLine()) != null) {
					content = content.trim();
					if (content.length() == 0)
						continue;
					if (content.indexOf("##") == 0)
						continue;
					//actual model component
					model.append(content);
				}
			}
			//load the ensemble
			ensemble = new Ensemble(model.toString());
			features = ensemble.getFeatures();
		}
		catch(Exception ex)
		{
			throw RankLibError.create("Error in LambdaMART::load(): ", ex);
		}
	}
	public void printParameters()
	{
		PRINTLN("No. of trees: " + nTrees);
		PRINTLN("No. of leaves: " + nTreeLeaves);
		PRINTLN("No. of threshold candidates: " + nThreshold);
		PRINTLN("Min leaf support: " + minLeafSupport);
		PRINTLN("Learning rate: " + learningRate);
		PRINTLN("Stop early: " + nRoundToStopEarly + " rounds without performance gain on validation data");		
	}

	@Override
	public RankerType rankerType() {
		return RankerType.LAMBDAMART;
	}

	public String name()
	{
		return "LambdaMART";
	}
	public Ensemble getEnsemble()
	{
		return ensemble;
	}
	
	protected void computePseudoResponses()
	{
		Arrays.fill(pseudoResponses, 0F);
		Arrays.fill(weights, 0);
		MyThreadPool p = MyThreadPool.getInstance();
		if(p.size() == 1)//single-thread
			computePseudoResponses(0, samples.size()-1, 0);
		else //multi-threading
		{
			//divide the entire dataset into chunks of equal size for each worker thread
			int[] partition = p.partition(samples.size());
			int current = 0;
			for(int i=0;i<partition.length-1;i++)
			{
				//execute the worker
				LambdaComputationWorker wk = new LambdaComputationWorker(this, partition[i], partition[i+1]-1, current); 
				p.execute(wk);
				
				if(i < partition.length-2)
					for(int j=partition[i]; j<=partition[i+1]-1;j++)
						current += samples.get(j).size();
			}
			
			//wait for all workers to complete before we move on to the next stage
			p.await();
		}
	}
	protected void computePseudoResponses(int start, int end, int current) {
		int cutoff = scorer.getK();
		//compute the lambda for each document (a.k.a "pseudo response")
		for(int i=start;i<=end;i++)
		{
			RankList orig = samples.get(i);			
			int[] idx = MergeSorter.sort(modelScores, current, current+orig.size()-1, false);
			RankList rl = new RankList(orig, idx, current);
			double[][] changes = scorer.swapChange(rl);
			//NOTE: j, k are indices in the sorted (by modelScore) list, not the original
			// ==> need to map back with idx[j] and idx[k] 
			for(int j=0;j<rl.size();j++)
			{
				DataPoint p1 = rl.get(j);
				int mj = idx[j];
				for(int k=0;k<rl.size();k++)
				{
					if(j > cutoff && k > cutoff)//swaping these pair won't result in any change in target measures since they're below the cut-off point
						break;
					DataPoint p2 = rl.get(k);
					int mk = idx[k];
					if(p1.getLabel() > p2.getLabel())
					{
						double deltaNDCG = Math.abs(changes[j][k]);
						if(deltaNDCG > 0)
						{
							double rho = 1.0 / (1 + Math.exp(modelScores[mj] - modelScores[mk]));
							double lambda = rho * deltaNDCG;
							pseudoResponses[mj] += lambda;
							pseudoResponses[mk] -= lambda;
							double delta = rho * (1.0 - rho) * deltaNDCG;
							weights[mj] += delta;
							weights[mk] += delta;
						}
					}
				}
			}
			current += orig.size();
		}
	}
	protected void updateTreeOutput(RegressionTree rt) {
		List<Split> leaves = rt.leaves();
		for (Split split : leaves) {
			float s1 = 0F;
			float s2 = 0F;
			int[] idx = split.getSamples();
			for (int k : idx) {
				s1 += pseudoResponses[k];
				s2 += weights[k];
			}
			if (s2 == 0) {
				split.setOutput(0);
			} else {
				split.setOutput(s1 / s2);
			}
		}
	}
	protected int[] sortSamplesByFeature(DataPoint[] samples, int fid) {
		double[] score = new double[samples.length];
		for(int i=0;i<samples.length;i++)
			score[i] = samples[i].getFeatureValue(fid);
		return MergeSorter.sort(score, true);
	}
	/**
	 * This function is equivalent to the inherited function rank(...), but it uses the cached model's outputs instead of computing them from scratch.
	 * @param rankListIndex the index of the ranked list to rank
	 * @param current split offset for this ranked list
	 * @return RankList sorted by scored outputs
	 */
	protected RankList rank(int rankListIndex, int current) {
		RankList orig = samples.get(rankListIndex);	
		double[] scores = new double[orig.size()];
		for(int i=0;i<scores.length;i++)
			scores[i] = modelScores[current+i];
		int[] idx = MergeSorter.sort(scores, false);
		return new RankList(orig, idx);
	}
	protected float computeModelScoreOnTraining() {
		float s = computeModelScoreOnTraining(0, samples.size()-1, 0);
		s = s / samples.size();
		return s;
	}
	protected float computeModelScoreOnTraining(int start, int end, int current) {
		float s = 0;
		int c = current;
		for(int i=start;i<=end;i++)
		{
			s += scorer.score(rank(i, c));
			c += samples.get(i).size();
		}
		return s;
	}
	protected float computeModelScoreOnValidation() {
		float score = computeModelScoreOnValidation(0, validationSamples.size()-1);
		return score/validationSamples.size();
	}
	protected float computeModelScoreOnValidation(int start, int end) {
		float score = 0;
		for(int i=start;i<=end;i++)
		{
			int[] idx = MergeSorter.sort(modelScoresOnValidation[i], false);
			score += scorer.score(new RankList(validationSamples.get(i), idx));
		}
		return score;
	}
	
	protected void sortSamplesByFeature(int fStart, int fEnd) {
		for(int i=fStart;i<=fEnd; i++)
			sortedIdx[i] = sortSamplesByFeature(martSamples, features[i]);
	}

	//For multi-threading processing
	static class SortWorker implements Runnable {
		LambdaMART ranker = null;
		int start = -1;
		int end = -1;
		SortWorker(LambdaMART ranker, int start, int end) {
			this.ranker = ranker;
			this.start = start;
			this.end = end;
		}		
		public void run() {
			ranker.sortSamplesByFeature(start, end);
		}
	}
	static class LambdaComputationWorker implements Runnable {
		LambdaMART ranker = null;
		int rlStart = -1;
		int rlEnd = -1;
		int martStart = -1;
		LambdaComputationWorker(LambdaMART ranker, int rlStart, int rlEnd, int martStart) {
			this.ranker = ranker;
			this.rlStart = rlStart;
			this.rlEnd = rlEnd;
			this.martStart = martStart;
		}		
		public void run() {
			ranker.computePseudoResponses(rlStart, rlEnd, martStart);
		}
	}
}
