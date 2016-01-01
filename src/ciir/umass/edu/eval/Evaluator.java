/*===============================================================================
 * Copyright (c) 2010-2015 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.eval;

import ciir.umass.edu.features.FeatureManager;
import ciir.umass.edu.learning.Dataset;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerTrainer;
import ciir.umass.edu.metric.ERRScorer;
import ciir.umass.edu.utilities.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author vdang
 * 
 * This class is meant to provide the interface to run and compare different ranking algorithms. It lets users specify general parameters (e.g. what algorithm to run, 
 * training/testing/validating data, etc.) as well as algorithm-specific parameters. Type "java -jar bin/RankLib.jar" at the command-line to see all the options. 
 */
public class Evaluator {

	private final RanklibArgs cfg;
	private final PrintStream out;
	private Ranker ranker;

	/**
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		RanklibArgs cfg = new RanklibArgs();
		if(!cfg.parse(args, System.out)) {
			return;
		}
		Evaluator eval = cfg.createEvaluator();
		eval.run();
	}

	public Evaluator(RanklibArgs args) {
		this.cfg = args;
		this.out = cfg.out;
	}

	public void train() {
		out.println("Training data:\t" + cfg.trainFile);

		//print out parameter settings
		if(cfg.foldCV != -1) {
			out.println("Cross validation: " + cfg.foldCV + " folds.");
			if(cfg.tvSplit > 0)
				out.println("Train-Validation split: " + cfg.tvSplit);
		} else {
			if(cfg.testFile.compareTo("") != 0)
				out.println("Test data:\t" + cfg.testFile);
			else if(cfg.ttSplit > 0)//choose to split training data into train and test
				out.println("Train-Test split: " + cfg.ttSplit);

			if(cfg.validationFile.compareTo("")!=0)//the user has specified the validation set
				out.println("Validation data:\t" + cfg.validationFile);
			else if(cfg.ttSplit <= 0 && cfg.tvSplit > 0)
				out.println("Train-Validation split: " + cfg.tvSplit);
		}
		out.println("Feature vector representation: " + ((cfg.useSparseRepresentation)?"Sparse":"Dense") + ".");
		out.println("Ranking method:\t" + ranker.name());
		if(cfg.featureDescriptionFile.compareTo("")!=0)
			out.println("Feature description file:\t" + cfg.featureDescriptionFile);
		else
			out.println("Feature description file:\tUnspecified. All features will be used.");
		out.println("Train metric:\t" + cfg.trainMetric);
		out.println("Test metric:\t" + cfg.testMetric);


		if(cfg.kcvModelDir.compareTo("")!=0)
			out.println("Models directory: " + cfg.kcvModelDir);

		if(cfg.kcvModelFile.compareTo("")!=0)
			out.println("Models' name: " + cfg.kcvModelFile);

		if(cfg.modelFile.compareTo("")!=0)
			out.println("Model file: " + cfg.modelFile);
		//out.println("#threads:\t" + nThread);

		out.println("");
		out.println("[+] " + ranker.name() + "'s Parameters:");
		ranker.printParameters();
		out.println("");

		//starting to do some work
		if(cfg.foldCV != -1) {
			//if(kcvModelDir.compareTo("") != 0 && kcvModelFile.compareTo("") == 0)
			//	kcvModelFile = "default";
			//
			//- Behavioral changes: Write kcv models if kcvmd OR kcvmn defined.  Use
			//  default names for missing arguments: "kcvmodels" default directory
			//  and "kcv" default model name.
			if (cfg.kcvModelDir.compareTo("") != 0 && cfg.kcvModelFile.compareTo("") == 0) {
				cfg.kcvModelFile = "kcv";
			} else if(cfg.kcvModelDir.compareTo("") == 0 && cfg.kcvModelFile.compareTo("") != 0) {
				cfg.kcvModelDir = "kcvmodels";
			}

			//- models won't be saved if kcvModelDir=""   [OBSOLETE]
			//- Models saved if EITHER kcvmd OR kcvmn defined.  Use default names for missing values.
			evaluate(cfg.trainFile, cfg.featureDescriptionFile, cfg.foldCV, cfg.tvSplit, cfg.kcvModelDir, cfg.kcvModelFile);
		} else {
			if(cfg.ttSplit > 0.0)//we should use a held-out portion of the training data for testing?
				evaluate(cfg.trainFile, cfg.validationFile, cfg.featureDescriptionFile, cfg.ttSplit);//no validation will be done if validationFile=""
			else if(cfg.tvSplit > 0.0)//should we use a portion of the training data for validation?
				evaluate(cfg.trainFile, cfg.tvSplit, cfg.testFile, cfg.featureDescriptionFile);
			else
				evaluate(cfg.trainFile, cfg.validationFile, cfg.testFile, cfg.featureDescriptionFile);//All files except for trainFile can be empty. This will be handled appropriately
		}
	}

	public void test() {
		String rankFile = cfg.rankFile;
		String scoreFile = cfg.scoreFile;
		List<String> savedModelFiles = cfg.savedModelFiles;
		String savedModelFile = cfg.savedModelFile;
		String indriRankingFile = cfg.indriRankingFile;
		String testMetric = cfg.testMetric;
		List<String> testFiles = cfg.testFiles;
		String prpFile = cfg.prpFile;
		String testFile = cfg.testFile;

		out.println("Model file:\t" + cfg.savedModelFile);
		if(rankFile.compareTo("") != 0) {
			if(scoreFile.compareTo("") != 0) {
				if(savedModelFiles.size() > 1)//models trained via cross-validation
					score(savedModelFiles, rankFile, scoreFile);
				else //a single model
					score(savedModelFile, rankFile, scoreFile);
			}
			else if(indriRankingFile.compareTo("") != 0) {
				if(savedModelFiles.size() > 1)//models trained via cross-validation
					rank(savedModelFiles, rankFile, indriRankingFile);
				else if(savedModelFiles.size() == 1)
					rank(savedModelFile, rankFile, indriRankingFile);
				else {
					//This is *ONLY* for debugging purposes. It is *NOT* exposed via cmd-line
					//It will evaluate the input ranking (without being re-ranked by any model) using any measure specified via metric2T
					rank(rankFile, indriRankingFile);
				}
			} else {
				throw RankLibError.create("This function has been removed.\n" +
						"Consider using -score in addition to your current parameters, " +
						"and do the ranking yourself based on these scores.");
				//e.rank(savedModelFile, rankFile);
			}
		} else {
			out.println("Test metric:\t" + testMetric);
			if(testMetric.startsWith("ERR"))
				out.println("Highest relevance label (to compute ERR): " + (int)SimpleMath.logBase2(ERRScorer.MAX));

			if(savedModelFile.compareTo("") != 0) {
				if(savedModelFiles.size() > 1)//models trained via cross-validation
				{
					if(testFiles.size() > 1)
						test(savedModelFiles, testFiles, prpFile);
					else
						test(savedModelFiles, testFile, prpFile);
				}
				else if(savedModelFiles.size() == 1) // a single model
					test(savedModelFile, testFile, prpFile);
			}
			else if(scoreFile.compareTo("") != 0)
				testWithScoreFile(testFile, scoreFile);
				//It will evaluate the input ranking (without being re-ranked by any model) using any measure specified via metric2T
			else
				test(testFile, prpFile);
		}
	}

	public void run() {
		if(!this.cfg.trainFile.isEmpty()) {
			ranker = cfg.factory.createRanker(cfg.rankerType);
			train();
		} else { //scenario: test a saved model
			test();
		}
		MyThreadPool.getInstance().shutdown();
	}

	public Dataset readInput(String inputFile) {
		return FeatureManager.readInput(inputFile, cfg.mustHaveRelDoc, cfg.useSparseRepresentation);
	}
	public int[] readFeature(String featureDefFile) {
		if(featureDefFile.compareTo("") == 0)
			return null;
		return FeatureManager.readFeature(featureDefFile);
	}
	public double evaluate(Ranker ranker, List<RankList> rl) {
		List<RankList> l = rl;
		if(ranker != null)
			l = ranker.rank(rl);
		return cfg.testScorer.score(l);
	}
	
	/**
	 * Evaluate the currently selected ranking algorithm using <training data, validation data, testing data and the defined features>.
	 * @param trainFile
	 * @param validationFile
	 * @param testFile
	 * @param featureDefFile
	 */
	public void evaluate(String trainFile, String validationFile, String testFile, String featureDefFile)
	{
		Dataset train = readInput(trainFile);//read input
		
		Dataset validation = null;
		if(validationFile.compareTo("")!=0)
			validation = readInput(validationFile);
		
		Dataset test = null;
		if(testFile.compareTo("")!=0)
			test = readInput(testFile);
		
		int[] features = readFeature(featureDefFile);//read features
		if(features == null)//no features specified ==> use all features in the training file
			features = FeatureManager.getFeatureFromSampleVector(train);

		cfg.nml.normalize(train, features);
		cfg.nml.normalize(validation, features);
		cfg.nml.normalize(test, features);

		RankerTrainer trainer = new RankerTrainer();

		trainer.train(this.ranker, train.samples, validation == null ? null : validation.samples, features, cfg.trainScorer);
		
		if(test != null)
		{
			double rankScore = evaluate(ranker, test.samples);
			out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		}
		String modelFile = cfg.modelFile;
		if(modelFile.compareTo("")!=0) {
			out.println("");
			ranker.save(modelFile);
			out.println("Model saved to: " + modelFile);
		}
	}
	/**
	 * Evaluate the currently selected ranking algorithm using percenTrain% of the samples for training the rest for testing.
	 * @param sampleFile
	 * @param validationFile Empty string for "no validation data"
	 * @param featureDefFile
	 * @param percentTrain
	 */
	public void evaluate(String sampleFile, String validationFile, String featureDefFile, double percentTrain)
	{
		List<RankList> trainingData = new ArrayList<>();
		List<RankList> testData = new ArrayList<>();
		int[] features = prepareSplit(sampleFile, featureDefFile, percentTrain, !cfg.nml.isNoop(), trainingData, testData);
		Dataset validation = null;
		if(validationFile.compareTo("") != 0)
		{
			validation = readInput(validationFile);
			cfg.nml.normalize(validation, features);
		}

		RankerTrainer trainer = new RankerTrainer();
		trainer.train(ranker, trainingData, validation == null ? null : validation.samples, features, cfg.trainScorer);
		
		double rankScore = evaluate(ranker, testData);
		
		out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		if(cfg.modelFile.compareTo("")!=0)
		{
			out.println("");
			ranker.save(cfg.modelFile);
			out.println("Model saved to: " + cfg.modelFile);
		}
	}
	/**
	 * Evaluate the currently selected ranking algorithm using percenTrain% of the training samples for training the rest as validation data.
	 * Test data is specified separately.
	 * @param trainFile
	 * @param percentTrain
	 * @param testFile Empty string for "no test data"
	 * @param featureDefFile
	 */
	public void evaluate(String trainFile, double percentTrain, String testFile, String featureDefFile)
	{
		boolean normalize = !cfg.nml.isNoop();
		List<RankList> train = new ArrayList<>();
		List<RankList> validation = new ArrayList<>();
		int[] features = prepareSplit(trainFile, featureDefFile, percentTrain, normalize, train, validation);
		Dataset test = null;
		if(testFile.compareTo("") != 0)
		{
			test = readInput(testFile);
			cfg.nml.normalize(test, features);
		}
		
		RankerTrainer trainer = new RankerTrainer();
		trainer.train(ranker, train, validation, features, cfg.trainScorer);
		
		if(test != null) {
			double rankScore = evaluate(ranker, test.samples);
			out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		}
		if(cfg.modelFile.compareTo("")!=0) {
			out.println("");
			ranker.save(cfg.modelFile);
			out.println("Model saved to: " + cfg.modelFile);
		}
	}
	/**
	 * Evaluate the currently selected ranking algorithm using <data, defined features> with k-fold cross validation.
	 * @param sampleFile
	 * @param featureDefFile
	 * @param nFold
	 * @param modelDir
	 * @param modelFile
	 */
	public void evaluate(String sampleFile, String featureDefFile, int nFold, String modelDir, String modelFile)
	{
		evaluate(sampleFile, featureDefFile, nFold, -1, modelDir, modelFile);
	}
	/**
	 * Evaluate the currently selected ranking algorithm using <data, defined features> with k-fold cross validation.
	 * @param sampleFile
	 * @param featureDefFile
	 * @param nFold
	 * @param tvs Train-validation split ratio.
	 * @param modelDir
	 * @param modelFile
	 */
	public void evaluate(String sampleFile, String featureDefFile, int nFold, float tvs, String modelDir, String modelFile)
	{
		List<List<RankList>> trainingData = new ArrayList<>();
		List<List<RankList>> validationData = new ArrayList<>();
		List<List<RankList>> testData = new ArrayList<>();
		//read all samples
		Dataset dataset = FeatureManager.readInput(sampleFile);
		//get features
		int[] features = readFeature(featureDefFile);//read features
		if(features == null)//no features specified ==> use all features in the training file
			features = FeatureManager.getFeatureFromSampleVector(dataset);
		FeatureManager.prepareCV(dataset.samples, nFold, tvs, trainingData, validationData, testData);
		//normalization
		for(int i=0;i<nFold;i++) {
			cfg.nml.normalizeSplits(dataset, trainingData, features);
			cfg.nml.normalizeSplits(dataset, validationData, features);
			cfg.nml.normalizeSplits(dataset, testData, features);
		}

		double scoreOnTrain = 0.0;
		double scoreOnTest = 0.0;
		double totalScoreOnTest = 0.0;
		int totalTestSampleSize = 0;
		
		double[][] scores = new double[nFold][];
		for(int i=0;i<nFold;i++)
			scores[i] = new double[]{0.0, 0.0};
		for(int i=0;i<nFold;i++)
		{
			List<RankList> train = trainingData.get(i);
			List<RankList> vali = null;
			if(tvs > 0)
				vali = validationData.get(i);
			List<RankList> test = testData.get(i);
			
			RankerTrainer trainer = new RankerTrainer();
			trainer.train(ranker, train, vali, features, cfg.trainScorer);
			
			double s2 = evaluate(ranker, test);
			scoreOnTrain += ranker.getScoreOnTrainingData();
			scoreOnTest += s2;
			totalScoreOnTest += s2 * test.size();
			totalTestSampleSize += test.size();

			//save performance in each fold
			scores[i][0] = ranker.getScoreOnTrainingData();
			scores[i][1] = s2;
			
			if(modelDir.compareTo("") != 0)
			{
				ranker.save(FileUtils.makePathStandard(modelDir) + "f" + (i+1) + "." + modelFile);
				out.println("Fold-" + (i+1) + " model saved to: " + modelFile);
			}
		}
		out.println("Summary:");
		out.println(cfg.testScorer.name() + "\t|   Train\t| Test");
		out.println("----------------------------------");
		for(int i=0;i<nFold;i++)
			out.println("Fold " + (i+1) + "\t|   " + SimpleMath.round(scores[i][0], 4) + "\t|  " + SimpleMath.round(scores[i][1], 4) + "\t");
		out.println("----------------------------------");
		out.println("Avg.\t|   " + SimpleMath.round(scoreOnTrain/nFold, 4) + "\t|  " + SimpleMath.round(scoreOnTest/nFold, 4) + "\t");
		out.println("----------------------------------");
		out.println("Total\t|   " + "\t" + "\t|  " + SimpleMath.round(totalScoreOnTest/totalTestSampleSize, 4) + "\t");
	}
	
	/**
	 * Evaluate the performance (in -metric2T) of the input rankings
	 * @param testFile Input rankings
	 */
	public void test(String testFile)
	{
		Dataset test = readInput(testFile);
		double rankScore = evaluate(null, test.samples);
		out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
	}
	public void test(String testFile, String prpFile)
	{
		Dataset test = readInput(testFile);
		double rankScore = 0.0;
		List<String> ids = new ArrayList<>();
		List<Double> scores = new ArrayList<>();
		for (RankList l : test.samples) {
			double score = cfg.testScorer.score(l);
			ids.add(l.getID());
			scores.add(score);
			rankScore += score;
		}
		rankScore /= test.size();
		ids.add("all");
		scores.add(rankScore);		
		out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		if(prpFile.compareTo("") != 0)
		{
			savePerRankListPerformanceFile(ids, scores, prpFile);
			out.println("Per-ranked list performance saved to: " + prpFile);
		}
	}
	/**
	 * Evaluate the performance (in -metric2T) of a pre-trained model. Save its performance on each of the ranked list if this is specified. 
	 * @param modelFile Pre-trained model
	 * @param testFile Test data
	 * @param prpFile Per-ranked list performance file: Model's performance on each of the ranked list. These won't be saved if prpFile="". 
	 */
	public void test(String modelFile, String testFile, String prpFile)
	{
		Ranker ranker = cfg.factory.loadRankerFromFile(modelFile);
		int[] features = ranker.getFeatures();
		Dataset test = readInput(testFile);
		cfg.nml.normalize(test, features);
		
		double rankScore = 0.0;
		List<String> ids = new ArrayList<>();
		List<Double> scores = new ArrayList<>();
		for (RankList aTest : test.samples) {
			RankList l = ranker.rank(aTest);
			double score = cfg.testScorer.score(l);
			ids.add(l.getID());
			scores.add(score);
			rankScore += score;
		}
		rankScore /= test.size();
		ids.add("all");
		scores.add(rankScore);		
		out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		if(prpFile.compareTo("") != 0)
		{
			savePerRankListPerformanceFile(ids, scores, prpFile);
			out.println("Per-ranked list performance saved to: " + prpFile);
		}
	}
	/**
	 * Evaluate the performance (in -metric2T) of k pre-trained models. Data in the test file will be splitted into k fold, where k=|models|.
	 * Each model will be evaluated on the data from the corresponding fold.
	 * @param modelFiles Pre-trained models
	 * @param testFile Test data
	 * @param prpFile Per-ranked list performance file: Model's performance on each of the ranked list. These won't be saved if prpFile="".
	 */
	public void test(List<String> modelFiles, String testFile, String prpFile)
	{
		List<List<RankList>> trainingData = new ArrayList<>();
		List<List<RankList>> testData = new ArrayList<>();
		//read all samples
		int nFold = modelFiles.size();
		Dataset dataset = FeatureManager.readInput(testFile);
		out.print("Preparing " + nFold + "-fold test data... ");
		FeatureManager.prepareCV(dataset.samples, nFold, trainingData, testData);
		out.println("[Done.]");
		double rankScore = 0.0;
		List<String> ids = new ArrayList<>();
		List<Double> scores = new ArrayList<>();
		for(int f=0;f<nFold;f++)
		{
			List<RankList> test = testData.get(f);
			Ranker ranker = cfg.factory.loadRankerFromFile(modelFiles.get(f));
			int[] features = ranker.getFeatures();
			cfg.nml.normalizeLists(dataset, test, features);

			for (RankList aTest : test) {
				RankList l = ranker.rank(aTest);
				double score = cfg.testScorer.score(l);
				ids.add(l.getID());
				scores.add(score);
				rankScore += score;
			}
		}
		rankScore = rankScore/ids.size();
		ids.add("all");
		scores.add(rankScore);
		out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		if(prpFile.compareTo("") != 0)
		{
			savePerRankListPerformanceFile(ids, scores, prpFile);
			out.println("Per-ranked list performance saved to: " + prpFile);
		}
	}
	/**
	 * Similar to the above, except data has already been splitted. The k-th model will be applied on the k-th test file.
	 * @param modelFiles
	 * @param testFiles
	 * @param prpFile
	 */
	public void test(List<String> modelFiles, List<String> testFiles, String prpFile)
	{
		int nFold = modelFiles.size();
		double rankScore = 0.0;
		List<String> ids = new ArrayList<>();
		List<Double> scores = new ArrayList<>();
		for(int f=0;f<nFold;f++)
		{
			Dataset test = FeatureManager.readInput(testFiles.get(f));
			ranker = cfg.factory.loadRankerFromFile(modelFiles.get(f));
			int[] features = ranker.getFeatures();
			cfg.nml.normalize(test, features);

			for (RankList aTest : test.samples) {
				RankList l = ranker.rank(aTest);
				double score = cfg.testScorer.score(l);
				ids.add(l.getID());
				scores.add(score);
				rankScore += score;
			}
		}
		rankScore = rankScore/ids.size();
		ids.add("all");
		scores.add(rankScore);
		out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		if(prpFile.compareTo("") != 0)
		{
			savePerRankListPerformanceFile(ids, scores, prpFile);
			out.println("Per-ranked list performance saved to: " + prpFile);
		}
	}
	/**
	 * Re-order the input rankings and measure their effectiveness (in -metric2T)
	 * @param testFile Input rankings
	 * @param scoreFile The model score file on each of the documents
	 */
	public void testWithScoreFile(String testFile, String scoreFile)
	{
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(scoreFile), "UTF-8"))) {
			Dataset test = readInput(testFile);
			String content = "";
			;
			List<Double> scores = new ArrayList<>();
			while((content = in.readLine()) != null)
			{
				content = content.trim();
				if(content.compareTo("") == 0)
					continue;
				scores.add(Double.parseDouble(content));
			}
			in.close();
			int k = 0;
			for(int i=0;i<test.size();i++)
			{
				RankList rl = test.get(i);
				double[] s = new double[rl.size()];
				for(int j=0;j<rl.size();j++)
					s[j] = scores.get(k++);
				rl = new RankList(rl, MergeSorter.sort(s, false));
				test.set(i, rl);
			}
			
			double rankScore = evaluate(null, test.samples);
			out.println(cfg.testScorer.name() + " on test data: " + SimpleMath.round(rankScore, 4));
		} catch (IOException e) {
			throw RankLibError.create(e);
		}
	}

	/**
	 * Write the model's score for each of the documents in a test rankings. 
	 * @param modelFile Pre-trained model
	 * @param testFile Test data
	 * @param outputFile Output file
	 */
	public void score(String modelFile, String testFile, String outputFile)
	{
		ranker = cfg.factory.loadRankerFromFile(modelFile);
		int[] features = ranker.getFeatures();
		Dataset test = readInput(testFile);
		cfg.nml.normalize(test, features);
		
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
			for (RankList l : test.samples) {
				for (int j = 0; j < l.size(); j++) {
					out.write(l.getID() + "\t" + j + "\t" + ranker.eval(l.get(j)) + "");
					out.newLine();
				}
			}
			out.close();
		}
		catch(IOException ex)
		{
			throw RankLibError.create("Error in Evaluator::rank(): ", ex);
		}
	}
	/**
	 * Write the models' score for each of the documents in a test rankings. These test rankings are splitted into k chunks where k=|models|.
	 * Each model is applied on the data from the corresponding fold.
	 * @param modelFiles
	 * @param testFile
	 * @param outputFile
	 */
	public void score(List<String> modelFiles, String testFile, String outputFile)
	{
		List<List<RankList>> trainingData = new ArrayList<>();
		List<List<RankList>> testData = new ArrayList<>();
		//read all samples
		int nFold = modelFiles.size();
		Dataset dataset = FeatureManager.readInput(testFile);
		out.print("Preparing " + nFold + "-fold test data... ");
		FeatureManager.prepareCV(dataset.samples, nFold, trainingData, testData);
		out.println("[Done.]");
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
			for(int f=0;f<nFold;f++)
			{
				List<RankList> test = testData.get(f);
				Ranker ranker = cfg.factory.loadRankerFromFile(modelFiles.get(f));
				int[] features = ranker.getFeatures();
				cfg.nml.normalizeLists(dataset, test, features);
				for (RankList l : test) {
					for (int j = 0; j < l.size(); j++) {
						out.write(l.getID() + "\t" + j + "\t" + ranker.eval(l.get(j)) + "");
						out.newLine();
					}
				}
			}
			out.close();
		}
		catch(IOException ex)
		{
			throw RankLibError.create("Error in Evaluator::score(): ", ex);
		}
	}
	/**
	 * Similar to the above, except data has already been splitted. The k-th model will be applied on the k-th test file.
	 * @param modelFiles
	 * @param testFiles
	 * @param outputFile
	 */
	public void score(List<String> modelFiles, List<String> testFiles, String outputFile)
	{
		int nFold = modelFiles.size();
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
			for(int f=0;f<nFold;f++) {
				Dataset test = FeatureManager.readInput(testFiles.get(f));
				Ranker ranker = cfg.factory.loadRankerFromFile(modelFiles.get(f));
				int[] features = ranker.getFeatures();
				cfg.nml.normalize(test, features);
				for (RankList l : test.samples) {
					for (int j = 0; j < l.size(); j++) {
						out.write(l.getID() + "\t" + j + "\t" + ranker.eval(l.get(j)) + "");
						out.newLine();
					}
				}
			}
		}
		catch(IOException ex)
		{
			throw RankLibError.create("Error in Evaluator::score(): ", ex);
		}
	}
	/**
	 * Use a pre-trained model to re-rank the test rankings. Save the output ranking in indri's run format
	 * @param modelFile
	 * @param testFile
	 * @param indriRanking
	 */
	public void rank(String modelFile, String testFile, String indriRanking)
	{
		Ranker ranker = cfg.factory.loadRankerFromFile(modelFile);
		int[] features = ranker.getFeatures();
		Dataset test = readInput(testFile);
		cfg.nml.normalize(test, features);
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indriRanking), "UTF-8"));
			for (RankList l : test.samples) {
				double[] scores = new double[l.size()];
				for (int j = 0; j < l.size(); j++)
					scores[j] = ranker.eval(l.get(j));
				int[] idx = MergeSorter.sort(scores, false);
				for (int j = 0; j < idx.length; j++) {
					int k = idx[j];
					String str = l.getID() + " Q0 " + l.get(k).getDescription().replace("#", "").trim() + " " + (j + 1) + " " + SimpleMath.round(scores[k], 5) + " ranklib";
					out.write(str);
					out.newLine();
				}
			}
			out.close();
		}
		catch(IOException ex)
		{
			throw RankLibError.create("Error in Evaluator::rank(): ", ex);
		}
	}
	/**
	 * Generate a ranking in Indri's format from the input ranking
	 * @param testFile
	 * @param indriRanking
	 */
	public void rank(String testFile, String indriRanking)
	{
		Dataset test = readInput(testFile);
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indriRanking), "UTF-8"));
			for (RankList l : test.samples) {
				for (int j = 0; j < l.size(); j++) {
					String str = l.getID() + " Q0 " + l.get(j).getDescription().replace("#", "").trim() + " " + (j + 1) + " " + SimpleMath.round(1.0 - 0.0001 * j, 5) + " ranklib";
					out.write(str);
					out.newLine();
				}
			}
			out.close();
		}
		catch(IOException ex)
		{
			throw RankLibError.create("Error in Evaluator::rank(): ", ex);
		}
	}
	/**
	 * Use k pre-trained models to re-rank the test rankings. Test rankings will be splitted into k fold, where k=|models|.
	 * Each model will be used to rank the data from the corresponding fold. Save the output ranking in indri's run format. 
	 * @param modelFiles
	 * @param testFile
	 * @param indriRanking
	 */
	public void rank(List<String> modelFiles, String testFile, String indriRanking)
	{
		List<List<RankList>> trainingData = new ArrayList<>();
		List<List<RankList>> testData = new ArrayList<>();
		//read all samples
		int nFold = modelFiles.size();
		Dataset dataset = FeatureManager.readInput(testFile);
		out.print("Preparing " + nFold + "-fold test data... ");
		FeatureManager.prepareCV(dataset.samples, nFold, trainingData, testData);
		out.println("[Done.]");
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indriRanking), "UTF-8"));
			for(int f=0;f<nFold;f++)
			{
				List<RankList> test = testData.get(f);
				Ranker ranker = cfg.factory.loadRankerFromFile(modelFiles.get(f));
				int[] features = ranker.getFeatures();
				cfg.nml.normalizeLists(dataset, test, features);

				for (RankList l : test) {
					double[] scores = new double[l.size()];
					for (int j = 0; j < l.size(); j++)
						scores[j] = ranker.eval(l.get(j));
					int[] idx = MergeSorter.sort(scores, false);
					for (int j = 0; j < idx.length; j++) {
						int k = idx[j];
						String str = l.getID() + " Q0 " + l.get(k).getDescription().replace("#", "").trim() + " " + (j + 1) + " " + SimpleMath.round(scores[k], 5) + " ranklib";
						out.write(str);
						out.newLine();
					}
				}				
			}
			out.close();
		}
		catch(Exception ex)
		{
			throw RankLibError.create("Error in Evaluator::rank(): ", ex);
		}
	}	
	/**
	 * Similar to the above, except data has already been splitted. The k-th model will be applied on the k-th test file.
	 * @param modelFiles
	 * @param testFiles
	 * @param indriRanking
	 */
	public void rank(List<String> modelFiles, List<String> testFiles, String indriRanking)
	{
		int nFold = modelFiles.size();
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indriRanking), "UTF-8"));
			for(int f=0;f<nFold;f++) {
				Dataset test = FeatureManager.readInput(testFiles.get(f));
				Ranker ranker = cfg.factory.loadRankerFromFile(modelFiles.get(f));
				int[] features = ranker.getFeatures();
				cfg.nml.normalize(test, features);

				for (RankList l : test.samples) {
					double[] scores = new double[l.size()];
					for (int j = 0; j < l.size(); j++)
						scores[j] = ranker.eval(l.get(j));
					int[] idx = MergeSorter.sort(scores, false);
					for (int j = 0; j < idx.length; j++) {
						int k = idx[j];
						String str = l.getID() + " Q0 " + l.get(k).getDescription().replace("#", "").trim() + " " + (j + 1) + " " + SimpleMath.round(scores[k], 5) + " ranklib";
						out.write(str);
						out.newLine();
					}
				}				
			}
			out.close();
		}
		catch(IOException ex)
		{
			throw RankLibError.create("Error in Evaluator::rank(): ", ex);
		}
	}

	/**
	 * Split the input file into two with respect to a specified split size.
	 * @param sampleFile Input data file
	 * @param featureDefFile Feature definition file (if it's an empty string, all features in the input file will be used)
	 * @param percentTrain How much of the input data will be used for training? (the remaining will be reserved for test/validation)
	 * @param normalize Whether to do normalization.
	 * @param trainingData [Output] Training data (after splitting) 
	 * @param testData [Output] Test (or validation) data (after splitting)
	 * @return A list of ids of the features to be used for learning.
	 */
	private int[] prepareSplit(String sampleFile, String featureDefFile, double percentTrain, boolean normalize, List<RankList> trainingData, List<RankList> testData)
	{
		Dataset data = readInput(sampleFile);//read input
		int[] features = readFeature(featureDefFile);//read features
		if(features == null)//no features specified ==> use all features in the training file
			features = FeatureManager.getFeatureFromSampleVector(data);

		cfg.nml.normalize(data, features);
		
		FeatureManager.prepareSplit(data.samples, percentTrain, trainingData, testData);
		return features;
	}
		
	/**
	 * Save systems' performance to file
	 * @param ids Ranked list IDs.
	 * @param scores Evaluation score (in whatever measure specified/calculated upstream such as NDCG@k, ERR@k, etc.)
	 * @param prpFile Output filename.
	 */
	public void savePerRankListPerformanceFile(List<String> ids, List<Double> scores, String prpFile)
	{
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prpFile)))) {
			for(int i=0;i<ids.size();i++)
			{
				//out.write(testScorer.name() + "   " + ids.get(i) + "   " + SimpleMath.round(scores.get(i), 4));
				out.write(cfg.testScorer.name() + "   " + ids.get(i) + "   " + scores.get(i));
				out.newLine();
			}
		}
		catch(Exception ex)
		{
			throw RankLibError.create("Error in Evaluator::savePerRankListPerformanceFile(): ", ex);
		}
	}
}
