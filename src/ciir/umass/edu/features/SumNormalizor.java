/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.features;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Dataset;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.utilities.RankLibError;

import java.util.Arrays;

/**
 * @author vdang
 */
public class SumNormalizor extends Normalizer {
	@Override
	public void normalize(Dataset ds, RankList rl, int[] fids) {
		if(rl.size() == 0) {
			throw new RankLibError("Error in SumNormalizor::normalize(): The input ranked list is empty");
		}
		
		//remove duplicate features from the input @fids ==> avoid normalizing the same features multiple times
		fids = removeDuplicateFeatures(fids);
				
		double[] norm = new double[fids.length];
		Arrays.fill(norm, 0);
		for (DataPoint dp : rl) {
			for (int j = 0; j < fids.length; j++)
				norm[j] += Math.abs(dp.getFeatureValue(fids[j]));
		}
		for (DataPoint dp : rl) {
			for (int j = 0; j < fids.length; j++)
				if (norm[j] > 0)
					dp.setFeatureValue(fids[j], (float) (dp.getFeatureValue(fids[j]) / norm[j]));
		}
	}
	public String name()
	{
		return "sum";
	}
}
