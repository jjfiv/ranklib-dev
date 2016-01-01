/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning;

import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.LambdaRank;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.MART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.utilities.FileUtils;
import ciir.umass.edu.utilities.RankLibError;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

/**
 * @author vdang
 * 
 * This class implements the Ranker factory. All ranking algorithms implemented have to be recognized in this class. 
 */
public class RankerFactory {

	// Prototypes: set config values on these to influence models that are generated to be trained/tested.
	public MART mart = new MART();
	public RankBoost rankBoost = new RankBoost();
	public RankNet rankNet = new RankNet();
	public AdaRank adaRank = new AdaRank();
	public CoorAscent coorAscent = new CoorAscent();
	public LambdaRank lambdaRank = new LambdaRank();
	public LambdaMART lambdaMart = new LambdaMART();
	public ListNet listNet = new ListNet();
	public RFRanker rfRanker = new RFRanker();
	public LinearRegRank linearRegRank = new LinearRegRank();

	protected List<Ranker> rFactory = Arrays.asList(
			mart,
			rankBoost,
			rankNet,
			adaRank,
			coorAscent,
			lambdaRank,
			lambdaMart,
			listNet,
			rfRanker,
			linearRegRank);

	protected TreeMap<String, RankerType> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	public RankerFactory() {
		for (Ranker ranker : rFactory) {
			ranker.factory = this;
			String name = ranker.name();
			map.put(name, ranker.rankerType());
		}
	}
	public Ranker createRanker(RankerType type) {
		return rFactory.get(type.ordinal() - RankerType.MART.ordinal()).createNew().setFactory(this);
	}
	public Ranker createRanker(RankerType type, List<RankList> samples, int[] features, MetricScorer scorer) {
		Ranker r = createRanker(type);
		r.setTrainingSet(samples);
		r.setFeatures(features);
		r.setMetricScorer(scorer);
		return r;
	}
	public Ranker createRanker(String rankerName) {
		RankerType rankerType = map.get(rankerName);
		if(rankerType == null) throw new IllegalArgumentException("No such RANKER="+rankerName);
		return createRanker(rankerType);
	}
	public Ranker loadRankerFromFile(String modelFile) {
    return loadRankerFromString(FileUtils.read(modelFile, "ASCII"));
	}
  public Ranker loadRankerFromString(String fullText) {
    try (BufferedReader in = new BufferedReader(new StringReader(fullText))) {
			Ranker r;
      String content = in.readLine();//read the first line to get the name of the ranking algorithm
      content = content.replace("## ", "").trim();
      System.out.println("Model:\t\t" + content);
      r = createRanker(content);
      r.loadFromString(fullText);
			return r;
    } catch(Exception ex) {
			throw RankLibError.create(ex);
    }
  }

	public List<String> getRankerNames() {
		List<String> names = new ArrayList<>();
		for (Ranker ranker : rFactory) {
			names.add(ranker.name());
		}
		return names;
	}

	public List<Ranker> getAllRankers() {
		return Collections.unmodifiableList(rFactory);
	}

	public RankerType getBaggedRankerType(int rt) {
		if(rt == 0 || rt == 6) {
			for (Ranker ranker : rFactory) {
				RankerType x = ranker.rankerType();
				if(x.getRankerId() == rt) {
					return x;
				}
			}
		}
		throw RankLibError.create("Ranker="+rt + " cannot be bagged. Random Forests only supports MART/LambdaMART.");

	}

	public Ranker createRanker(int rt) {
		for (Ranker ranker : rFactory) {
			RankerType x = ranker.rankerType();
			if(x.getRankerId() == rt) {
				return ranker.createNew().setFactory(this);
			}
		}
		throw RankLibError.create("Ranker="+rt + " cannot be created.");
	}
}
