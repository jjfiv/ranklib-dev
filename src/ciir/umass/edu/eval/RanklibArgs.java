package ciir.umass.edu.eval;

import ciir.umass.edu.features.*;
import ciir.umass.edu.learning.*;
import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.learning.neuralnet.Neuron;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.metric.ERRScorer;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.MyThreadPool;
import ciir.umass.edu.utilities.RankLibError;
import ciir.umass.edu.utilities.SimpleMath;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jfoley.
 */
public class RanklibArgs {

  String trainFile = "";
  String featureDescriptionFile = "";
  float ttSplit = 0;//train-test split
  float tvSplit = 0;//train-validation split
  int foldCV = -1;
  String validationFile = "";
  String testFile = "";
  List<String> testFiles = new ArrayList<>();
  int rankerType = 4;
  String trainMetric = "ERR@10";
  String testMetric = "";
  String savedModelFile = "";
  List<String> savedModelFiles = new ArrayList<>();
  String kcvModelDir = "";
  String kcvModelFile = "";
  String rankFile = "";
  String prpFile = "";
  boolean mustHaveRelDoc = false;
  boolean useSparseRepresentation = false;
  Normalizer nml = new NoopNormalizer();
  String modelFile = "";

  int nThread = -1; // nThread = #cpu-cores
  //for my personal use
  String indriRankingFile = "";
  String scoreFile = "";
  RankerFactory factory = new RankerFactory();
  boolean verbose = true;
  protected MetricScorerFactory mFact = new MetricScorerFactory();

  /**
   * measure such as NDCG and MAP requires "complete" judgment.
   * The relevance labels attached to our samples might be only a subset of th* e entire relevance judgment set.
   * If we're working on datasets like Letor/Web10K or Yahoo! LTR, we can to* tally ignore this parameter.
   * However, if we sample top-K documents from baseline run (e.g. query-likelihood) to create training data for TREC collections,
   * there's a high chance some relevant document (the in qrel file TREC provides) does not appear in our top-K list -- thus the calculation of
   * MAP and NDCG is no longer precise. If so, specify that "external" relevance judgment here (via the -qrel cmd parameter)
   */
  public String qrelFile = null;

  public String newFeatureFile = "";
  public boolean keepOrigFeatures = false;
  public int topNew = 2000;

  PrintStream out;

  public void printHelp() {
    out.println("Usage: java -jar RankLib.jar <Params>");
    out.println("Params:");
    out.println("  [+] Training (+ tuning and evaluation)");
    out.println("\t-train <file>\t\tTraining data");
    out.println("\t-ranker <type>\t\tSpecify which ranking algorithm to use");
    out.println("\t\t\t\t0: MART (gradient boosted regression tree)");
    out.println("\t\t\t\t1: RankNet");
    out.println("\t\t\t\t2: RankBoost");
    out.println("\t\t\t\t3: AdaRank");
    out.println("\t\t\t\t4: Coordinate Ascent");
    out.println("\t\t\t\t6: LambdaMART");
    out.println("\t\t\t\t7: ListNet");
    out.println("\t\t\t\t8: Random Forests");
    out.println("\t\t\t\t9: Linear regression (L2 regularization)");
    out.println("\t[ -feature <file> ]\tFeature description file: list features to be considered by the learner, each on a separate line");
    out.println("\t\t\t\tIf not specified, all features will be used.");
    out.println("\t[ -metric2t <metric> ]\tMetric to optimize on the training data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, BEST@k, ERR@k (default=" + trainMetric + ")");
    out.println("\t[ -metric2t <metric> ]\tMetric to optimize on the training data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, ERR@k (default=" + trainMetric + ")");
    out.println("\t[ -gmax <label> ]\tHighest judged relevance label. It affects the calculation of ERR (default=" + (int) SimpleMath.logBase2(ERRScorer.MAX) + ", i.e. 5-point scale {0,1,2,3,4})");
    out.println("\t[ -qrel <file> ]\tTREC-style relevance judgment file. It only affects MAP and NDCG (default=unspecified)");
    out.println("\t[ -silent ]\t\tDo not print progress messages (which are printed by default)");

    out.println("");
    //out.println("        Use the entire specified training data");
    out.println("\t[ -validate <file> ]\tSpecify if you want to tune your system on the validation data (default=unspecified)");
    out.println("\t\t\t\tIf specified, the final model will be the one that performs best on the validation data");
    out.println("\t[ -tvs <x \\in [0..1]> ]\tIf you don't have separate validation data, use this to set train-validation split to be (x)(1.0-x)");

    out.println("\t[ -save <model> ]\tSave the model learned (default=not-save)");

    out.println("");
    out.println("\t[ -test <file> ]\tSpecify if you want to evaluate the trained model on this data (default=unspecified)");
    out.println("\t[ -tts <x \\in [0..1]> ]\tSet train-test split to be (x)(1.0-x). -tts will override -tvs");
    out.println("\t[ -metric2T <metric> ]\tMetric to evaluate on the test data (default to the same as specified for -metric2t)");

    out.println("");
    out.println("\t[ -norm <method>]\tNormalize all feature vectors (default=no-normalization). Method can be:");
    out.println("\t\t\t\tsum: normalize each feature by the sum of all its values");
    out.println("\t\t\t\tzscore: normalize each feature by its mean/standard deviation");
    out.println("\t\t\t\tlinear: normalize each feature by its min/max values");

    //out.println("");
    //out.println("\t[ -sparse ]\t\tUse sparse representation for all feature vectors (default=dense)");

    out.println("");
    out.println("\t[ -kcv <k> ]\t\tSpecify if you want to perform k-fold cross validation using the specified training data (default=NoCV)");
    out.println("\t\t\t\t-tvs can be used to further reserve a portion of the training data in each fold for validation");
    //out.println("\t\t\t\tData for each fold is created from sequential partitions of the training data.");
    //out.println("\t\t\t\tRandomized partitioning can be done by shuffling the training data in advance.");
    //out.println("\t\t\t\tType \"java -cp bin/RankLib.jar ciir.umass.edu.feature.FeatureManager\" for help with shuffling.");

    out.println("\t[ -kcvmd <dir> ]\tDirectory for models trained via cross-validation (default=not-save)");
    out.println("\t[ -kcvmn <model> ]\tName for model learned in each fold. It will be prefix-ed with the fold-number (default=empty)");

    out.println("");
    out.println("    [-] RankNet-specific parameters");
    out.println("\t[ -epoch <T> ]\t\tThe number of epochs to train (default=" + RankNet.nIteration + ")");
    out.println("\t[ -layer <layer> ]\tThe number of hidden layers (default=" + RankNet.nHiddenLayer + ")");
    out.println("\t[ -node <node> ]\tThe number of hidden nodes per layer (default=" + RankNet.nHiddenNodePerLayer + ")");
    out.println("\t[ -lr <rate> ]\t\tLearning rate (default=" + (new DecimalFormat("###.########")).format(RankNet.learningRate) + ")");

    out.println("");
    out.println("    [-] RankBoost-specific parameters");
    out.println("\t[ -round <T> ]\t\tThe number of rounds to train (default=" + RankBoost.nIteration + ")");
    out.println("\t[ -tc <k> ]\t\tNumber of threshold candidates to search. -1 to use all feature values (default=" + RankBoost.nThreshold + ")");

    out.println("");
    out.println("    [-] AdaRank-specific parameters");
    out.println("\t[ -round <T> ]\t\tThe number of rounds to train (default=" + AdaRank.nIteration + ")");
    out.println("\t[ -noeq ]\t\tTrain without enqueuing too-strong features (default=unspecified)");
    out.println("\t[ -tolerance <t> ]\tTolerance between two consecutive rounds of learning (default=" + AdaRank.tolerance + ")");
    out.println("\t[ -max <times> ]\tThe maximum number of times can a feature be consecutively selected without changing performance (default=" + AdaRank.maxSelCount + ")");

    out.println("");
    out.println("    [-] Coordinate Ascent-specific parameters");
    out.println("\t[ -r <k> ]\t\tThe number of random restarts (default=" + CoorAscent.nRestart + ")");
    out.println("\t[ -i <iteration> ]\tThe number of iterations to search in each dimension (default=" + CoorAscent.nMaxIteration + ")");
    out.println("\t[ -tolerance <t> ]\tPerformance tolerance between two solutions (default=" + CoorAscent.tolerance + ")");
    out.println("\t[ -reg <slack> ]\tRegularization parameter (default=no-regularization)");

    out.println("");
    out.println("    [-] {MART, LambdaMART}-specific parameters");
    out.println("\t[ -tree <t> ]\t\tNumber of trees (default=" + LambdaMART.nTrees + ")");
    out.println("\t[ -leaf <l> ]\t\tNumber of leaves for each tree (default=" + LambdaMART.nTreeLeaves + ")");
    out.println("\t[ -shrinkage <factor> ]\tShrinkage, or learning rate (default=" + LambdaMART.learningRate + ")");
    out.println("\t[ -tc <k> ]\t\tNumber of threshold candidates for tree spliting. -1 to use all feature values (default=" + LambdaMART.nThreshold + ")");
    out.println("\t[ -mls <n> ]\t\tMin leaf support -- minimum % of docs each leaf has to contain (default=" + LambdaMART.minLeafSupport + ")");
    out.println("\t[ -estop <e> ]\t\tStop early when no improvement is observed on validaton data in e consecutive rounds (default=" + LambdaMART.nRoundToStopEarly + ")");

    out.println("");
    out.println("    [-] ListNet-specific parameters");
    out.println("\t[ -epoch <T> ]\t\tThe number of epochs to train (default=" + ListNet.nIteration + ")");
    out.println("\t[ -lr <rate> ]\t\tLearning rate (default=" + (new DecimalFormat("###.########")).format(ListNet.learningRate) + ")");

    out.println("");
    out.println("    [-] Random Forests-specific parameters");
    out.println("\t[ -bag <r> ]\t\tNumber of bags (default=" + RFRanker.nBag + ")");
    out.println("\t[ -srate <r> ]\t\tSub-sampling rate (default=" + RFRanker.subSamplingRate + ")");
    out.println("\t[ -frate <r> ]\t\tFeature sampling rate (default=" + RFRanker.featureSamplingRate + ")");
    int type = (RFRanker.rType.ordinal()- RankerType.MART.ordinal());
    out.println("\t[ -rtype <type> ]\tRanker to bag (default=" + type + ", i.e. " + factory.getRankerNames().get(type) + ")");
    out.println("\t[ -tree <t> ]\t\tNumber of trees in each bag (default=" + RFRanker.nTrees + ")");
    out.println("\t[ -leaf <l> ]\t\tNumber of leaves for each tree (default=" + RFRanker.nTreeLeaves + ")");
    out.println("\t[ -shrinkage <factor> ]\tShrinkage, or learning rate (default=" + RFRanker.learningRate + ")");
    out.println("\t[ -tc <k> ]\t\tNumber of threshold candidates for tree spliting. -1 to use all feature values (default=" + RFRanker.nThreshold + ")");
    out.println("\t[ -mls <n> ]\t\tMin leaf support -- minimum % of docs each leaf has to contain (default=" + RFRanker.minLeafSupport + ")");

    out.println("");
    out.println("    [-] Linear Regression-specific parameters");
    out.println("\t[ -L2 <reg> ]\t\tL2 regularization parameter (default=" + LinearRegRank.lambda + ")");

    out.println("");
    out.println("  [+] Testing previously saved models");
    out.println("\t-load <model>\t\tThe model to load");
    out.println("\t\t\t\tMultiple -load can be used to specify models from multiple folds (in increasing order),");
    out.println("\t\t\t\t  in which case the test/rank data will be partitioned accordingly.");
    out.println("\t-test <file>\t\tTest data to evaluate the model(s) (specify either this or -rank but not both)");
    out.println("\t-rank <file>\t\tRank the samples in the specified file (specify either this or -test but not both)");
    out.println("\t[ -metric2T <metric> ]\tMetric to evaluate on the test data (default=" + trainMetric + ")");
    out.println("\t[ -gmax <label> ]\tHighest judged relevance label. It affects the calculation of ERR (default=" + (int)SimpleMath.logBase2(ERRScorer.MAX) + ", i.e. 5-point scale {0,1,2,3,4})");
    out.println("\t[ -score <file>]\tStore ranker's score for each object being ranked (has to be used with -rank)");
    out.println("\t[ -qrel <file> ]\tTREC-style relevance judgment file. It only affects MAP and NDCG (default=unspecified)");
    out.println("\t[ -idv <file> ]\t\tSave model performance (in test metric) on individual ranked lists (has to be used with -test)");
    out.println("\t[ -norm ]\t\tNormalize feature vectors (similar to -norm for training/tuning)");
    //out.println("\t[ -sparse ]\t\tUse sparse representation for all feature vectors (default=dense)");

    out.println("");
  }

  public boolean parse(String[] args, PrintStream out) {
    this.out = out;
    if(args.length < 2) {
      printHelp();
      return false;
    }

    for(int i=0;i<args.length;i++) {
      if(args[i].compareTo("-train")==0)
        trainFile = args[++i];
      else if(args[i].compareTo("-ranker")==0)
        rankerType = Integer.parseInt(args[++i]);
      else if(args[i].compareTo("-feature")==0)
        featureDescriptionFile = args[++i];
      else if(args[i].compareTo("-metric2t")==0)
        trainMetric = args[++i];
      else if(args[i].compareTo("-metric2T")==0)
        testMetric = args[++i];
      else if(args[i].compareTo("-gmax")==0)
        ERRScorer.MAX = Math.pow(2, Double.parseDouble(args[++i]));
      else if(args[i].compareTo("-qrel")==0)
        qrelFile = args[++i];
      else if(args[i].compareTo("-tts")==0)
        ttSplit = Float.parseFloat(args[++i]);
      else if(args[i].compareTo("-tvs")==0)
        tvSplit = Float.parseFloat(args[++i]);
      else if(args[i].compareTo("-kcv")==0)
        foldCV = Integer.parseInt(args[++i]);
      else if(args[i].compareTo("-validate")==0)
        validationFile = args[++i];
      else if(args[i].compareTo("-test")==0) {
        testFile = args[++i];
        testFiles.add(testFile);
      } else if(args[i].compareTo("-norm")==0) {
        String n = args[++i];
        if(n.compareTo("sum") == 0)
          nml = new SumNormalizor();
        else if(n.compareTo("zscore") == 0)
          nml = new ZScoreNormalizor();
        else if(n.compareTo("linear") == 0)
          nml = new LinearNormalizer();
        else
        {
          throw RankLibError.create("Unknown normalizor: " + n);
        }
      }
      else if(args[i].compareTo("-sparse")==0)
        useSparseRepresentation = true;
      else if(args[i].compareTo("-save")==0)
        modelFile = args[++i];
      else if(args[i].compareTo("-kcvmd")==0)
        kcvModelDir = args[++i];
      else if(args[i].compareTo("-kcvmn")==0)
        kcvModelFile = args[++i];
      else if(args[i].compareTo("-silent")==0)
        verbose = false;

      else if(args[i].compareTo("-load")==0) {
        savedModelFile = args[++i];
        savedModelFiles.add(args[i]);
      }
      else if(args[i].compareTo("-idv")==0)
        prpFile = args[++i];
      else if(args[i].compareTo("-rank")==0)
        rankFile = args[++i];
      else if(args[i].compareTo("-score")==0)
        scoreFile = args[++i];

        //Ranker-specific parameters
        //RankNet
      else if(args[i].compareTo("-epoch")==0)
      {
        RankNet.nIteration = Integer.parseInt(args[++i]);
        ListNet.nIteration = Integer.parseInt(args[i]);
      }
      else if(args[i].compareTo("-layer")==0)
        RankNet.nHiddenLayer = Integer.parseInt(args[++i]);
      else if(args[i].compareTo("-node")==0)
        RankNet.nHiddenNodePerLayer = Integer.parseInt(args[++i]);
      else if(args[i].compareTo("-lr")==0)
      {
        RankNet.learningRate = Double.parseDouble(args[++i]);
        ListNet.learningRate = Neuron.learningRate;
      }

      //RankBoost
      else if(args[i].compareTo("-tc")==0)
      {
        RankBoost.nThreshold = Integer.parseInt(args[++i]);
        LambdaMART.nThreshold = Integer.parseInt(args[i]);
      }

      //AdaRank
      else if(args[i].compareTo("-noeq")==0)
        AdaRank.trainWithEnqueue = false;
      else if(args[i].compareTo("-max")==0)
        AdaRank.maxSelCount = Integer.parseInt(args[++i]);

        //COORDINATE ASCENT
      else if(args[i].compareTo("-r")==0)
        CoorAscent.nRestart = Integer.parseInt(args[++i]);
      else if(args[i].compareTo("-i")==0)
        CoorAscent.nMaxIteration = Integer.parseInt(args[++i]);

        //ranker-shared parameters
      else if(args[i].compareTo("-round")==0)
      {
        RankBoost.nIteration = Integer.parseInt(args[++i]);
        AdaRank.nIteration = Integer.parseInt(args[i]);
      }
      else if(args[i].compareTo("-reg")==0)
      {
        CoorAscent.slack = Double.parseDouble(args[++i]);
        CoorAscent.regularized = true;
      }
      else if(args[i].compareTo("-tolerance")==0)
      {
        AdaRank.tolerance = Double.parseDouble(args[++i]);
        CoorAscent.tolerance = Double.parseDouble(args[i]);
      }

      //MART / LambdaMART / Random forest
      else if(args[i].compareTo("-tree")==0)
      {
        LambdaMART.nTrees = Integer.parseInt(args[++i]);
        RFRanker.nTrees = Integer.parseInt(args[i]);
      }
      else if(args[i].compareTo("-leaf")==0)
      {
        LambdaMART.nTreeLeaves = Integer.parseInt(args[++i]);
        RFRanker.nTreeLeaves = Integer.parseInt(args[i]);
      }
      else if(args[i].compareTo("-shrinkage")==0)
      {
        LambdaMART.learningRate = Float.parseFloat(args[++i]);
        RFRanker.learningRate = Float.parseFloat(args[i]);
      }
      else if(args[i].compareTo("-mls")==0)
      {
        LambdaMART.minLeafSupport = Integer.parseInt(args[++i]);
        RFRanker.minLeafSupport = LambdaMART.minLeafSupport;
      }
      else if(args[i].compareTo("-estop")==0)
        LambdaMART.nRoundToStopEarly = Integer.parseInt(args[++i]);
        //Random forest
      else if(args[i].compareTo("-bag")==0)
        RFRanker.nBag = Integer.parseInt(args[++i]);
      else if(args[i].compareTo("-srate")==0)
        RFRanker.subSamplingRate = Float.parseFloat(args[++i]);
      else if(args[i].compareTo("-frate")==0)
        RFRanker.featureSamplingRate = Float.parseFloat(args[++i]);
      else if(args[i].compareTo("-rtype")==0)
      {
        int rt = Integer.parseInt(args[++i]);
        RFRanker.rType = factory.getBaggedRankerType(rt);
      }

      else if(args[i].compareTo("-L2")==0)
        LinearRegRank.lambda = Double.parseDouble(args[++i]);

      else if(args[i].compareTo("-thread")==0)
        nThread = Integer.parseInt(args[++i]);

        /////////////////////////////////////////////////////
        // These parameters are *ONLY* for my personal use
        /////////////////////////////////////////////////////
      else if(args[i].compareTo("-nf")==0)
        newFeatureFile = args[++i];
      else if(args[i].compareTo("-keep")==0)
        keepOrigFeatures = true;
      else if(args[i].compareTo("-t")==0)
        topNew = Integer.parseInt(args[++i]);
      else if(args[i].compareTo("-indri")==0)
        indriRankingFile = args[++i];
      else if(args[i].compareTo("-hr")==0)
        mustHaveRelDoc = true;
      else
      {
        throw RankLibError.create("Unknown command-line parameter: " + args[i]);
      }
    }

    return true;
  }

  MetricScorer trainScorer = null;
  MetricScorer testScorer = null;

  public Evaluator createEvaluator() {
    if(nThread == -1)
      nThread = Runtime.getRuntime().availableProcessors();
    MyThreadPool.init(nThread);

    if(testMetric.compareTo("")==0)
      testMetric = trainMetric;

    out.println("");
    //out.println((keepOrigFeatures)?"Keep orig. features":"Discard orig. features");
    out.println("[+] General Parameters:");

    trainScorer = mFact.createScorer(trainMetric);
    testScorer = mFact.createScorer(testMetric);
    if(qrelFile != null) {
      trainScorer.loadExternalRelevanceJudgment(qrelFile);
      testScorer.loadExternalRelevanceJudgment(qrelFile);
    }

    if(trainMetric.toUpperCase().startsWith("ERR") || testMetric.toUpperCase().startsWith("ERR"))
      out.println("Highest relevance label (to compute ERR): " + (int)SimpleMath.logBase2(ERRScorer.MAX));

    if(qrelFile != null)
      out.println("TREC-format relevance judgment (only affects MAP and NDCG scores): " + qrelFile);

    out.println("Feature normalization: " + (nml instanceof NoopNormalizer ? "No" : nml.name()));

    return new Evaluator(this);
  }
}
