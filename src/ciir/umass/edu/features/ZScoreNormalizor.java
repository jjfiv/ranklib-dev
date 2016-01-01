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

import java.util.Arrays;

/**
 * @author vdang
 */
public class ZScoreNormalizor extends Normalizer {
	@Override
	public void normalize(Dataset ds, RankList rl, int[] fids) {
		if(rl.size() == 0)
		{
			System.out.println("Error in ZScoreNormalizor::normalize(): The input ranked list is empty");
			System.exit(1);
		}
		
		//remove duplicate features from the input @fids ==> avoid normalizing the same features multiple times
		fids = removeDuplicateFeatures(fids);
		
		double[] means = new double[fids.length];
		Arrays.fill(means, 0);
		for (DataPoint dp : rl) {
			for (int j = 0; j < fids.length; j++)
				means[j] += dp.getFeatureValue(fids[j]);
		}
		
		for(int j=0;j<fids.length;j++) {
			means[j] = means[j] / rl.size();
			double std = 0;
			for (DataPoint p : rl) {
				double x = p.getFeatureValue(fids[j]) - means[j];
				std += x*x;
			}
			std = Math.sqrt(std / (rl.size()-1));
			//normalize
			if(std > 0.0) {
				for (DataPoint p : rl) {
					double x = (p.getFeatureValue(fids[j]) - means[j])/std;//x ~ standard normal (0, 1)
					p.setFeatureValue(fids[j], (float)x);
				}
			}
		}
	}
	public String name()
	{
		return "zscore";
	}
}
