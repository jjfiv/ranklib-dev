/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.metric;

import ciir.umass.edu.learning.RankList;

import java.util.Arrays;

/**
 * @author vdang
 */
public class PrecisionScorer extends MetricScorer {

	public PrecisionScorer() { this(10); }
	public PrecisionScorer(int k) {
		assert(k > 0);
		this.k = k;
	}
	public double score(RankList rl)
	{
		int count = 0;
		
		int size = k;
		if(k > rl.size() || k <= 0)
			size = rl.size();
		
		for(int i=0;i<size;i++) {
			if(rl.get(i).getIntLabel() > 0)//relevant
				count++;
		}
		return ((double)count)/size;
	}
	public MetricScorer copy() {
		return new PrecisionScorer(k);
	}
	public String name()
	{
		return "P@"+k;
	}
	public double[][] swapChange(RankList rl)
	{
		int size = (rl.size() > k) ? k : rl.size();

		double[][] changes = new double[rl.size()][];
		for(int i=0;i<rl.size();i++) {
			changes[i] = new double[rl.size()];
			Arrays.fill(changes[i], 0);
		}
		
		for(int i=0;i<size;i++) {
			for(int j=size;j<rl.size();j++) {
				int c = rl.get(j).getIntLabel() - rl.get(i).getIntLabel();
				changes[i][j] = changes[j][i] = ((float)c)/size;
			}
		}
		return changes;
	}
}
