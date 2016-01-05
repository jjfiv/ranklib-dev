/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.features;

import ciir.umass.edu.learning.Dataset;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.utilities.RankLibError;

import java.util.HashSet;
import java.util.List;

/**
 * @author vdang
 *
 * Abstract class for feature normalization
 */
public abstract class Normalizer {
	public void normalize(Dataset ds, RankList rl) {
		if(rl.size() == 0) {
			throw RankLibError.create("Error in Normalizor::normalize(): The input ranked list is empty");
		}
		int nFeature = ds.getMaxFeaturePosition();
		int[] fids = new int[nFeature];
		for(int i=1;i<=nFeature;i++)
			fids[i-1] = i;
		normalize(ds, rl, fids);
	}
	public abstract void normalize(Dataset ds, RankList rl, int[] fids);

	public void normalize(Dataset ds) {
		if(ds == null) return;
		for (RankList sample : ds.samples) {
			normalize(ds, sample);
		}
	}
	public void normalize(Dataset ds, int[] fids) {
		if(ds == null) return;
		for (RankList sample : ds.samples) {
			normalize(ds, sample, fids);
		}
	}
	public void normalizeLists(Dataset ds, List<RankList> list, int[] fids) {
		if(ds == null) return;
		for (RankList sample : list) {
			normalize(ds, sample, fids);
		}
	}
	public void normalizeSplits(Dataset ds, List<List<RankList>> lists, int[] fids) {
		if(ds == null) return;
		for (List<RankList> list : lists) {
			for (RankList sample : list) {
				normalize(ds, sample, fids);
			}
		}
	}

	public boolean isNoop() {
		return false;
	}

	public int[] removeDuplicateFeatures(int[] fids) {
		HashSet<Integer> uniqueSet = new HashSet<>();
		for (int fid : fids)
			if (!uniqueSet.contains(fid))
				uniqueSet.add(fid);
		fids = new int[uniqueSet.size()];
		int fi=0;
		for(int i : uniqueSet)
			fids[fi++] = i;
		return fids;
	}
	public abstract String name();
}
