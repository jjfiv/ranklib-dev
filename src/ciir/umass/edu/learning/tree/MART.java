/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning.tree;

import ciir.umass.edu.learning.RankerType;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.metric.MetricScorer;

import java.util.List;

/**
 * @author vdang
 *
 *  This class implements MART for (point-wise) ranking:
 *  J.H. Friedman. Greedy function approximation: A gradient boosting machine. 
 *  Technical Report, IMS Reitz Lecture, Stanford, 1999; see also Annals of Statistics, 2001.
 */
public class MART extends LambdaMART {
	//Parameters
	//Inherits *ALL* parameters from LambdaMART
	
	public MART() {	}

	public MART(MART prototype) {
		super(prototype);
	}
	public MART(List<RankList> samples, int[] features, MetricScorer scorer)
	{
		super(samples, features, scorer);
	}
	
	public Ranker createNew() {
		return new MART(this);
	}
	public String name()
	{
		return "MART";
	}
	protected void computePseudoResponses()
	{
		for(int i=0;i<martSamples.length;i++)
			pseudoResponses[i] = martSamples[i].getLabel() - modelScores[i];
	}
	protected void updateTreeOutput(RegressionTree rt) {
		List<Split> leaves = rt.leaves();
		for (Split s : leaves) {
			float s1 = 0.0F;
			int[] idx = s.getSamples();
			for (int k : idx) {
				s1 += pseudoResponses[k];
			}
			s.setOutput(s1 / idx.length);
		}
	}

	@Override
	public RankerType rankerType() {
		return RankerType.MART;
	}
}
