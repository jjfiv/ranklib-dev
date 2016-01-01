/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning;

import ciir.umass.edu.utilities.Sorter;

import java.util.AbstractList;
import java.util.List;

/**
 * @author vdang
 * 
 * This class implement the list of objects (each of which is a DataPoint) to be ranked. 
 */
public class RankList extends AbstractList<DataPoint> {

	protected DataPoint[] rl = null;
	
	public RankList(List<DataPoint> rl) {
		this.rl = new DataPoint[rl.size()];
		for(int i=0;i<rl.size();i++)
			this.rl[i] = rl.get(i);
	}
	public RankList(RankList rl) {
		this.rl = new DataPoint[rl.size()];
		for(int i=0;i<rl.size();i++)
			this.rl[i] = rl.get(i);
	}
	public RankList(RankList rl, int[] idx) {
		this.rl = new DataPoint[rl.size()];
		for(int i=0;i<idx.length;i++)
			this.rl[i] = rl.get(idx[i]);
	}
	public RankList(RankList rl, int[] idx, int offset) {
		this.rl = new DataPoint[rl.size()];
		for(int i=0;i<idx.length;i++)
			this.rl[i] = rl.get(idx[i]-offset);
	}	
	public String getID()
	{
		return get(0).getID();
	}
	public int size()
	{
		return rl.length;
	}
	public DataPoint get(int k)
	{
		return rl[k];
	}
	public DataPoint set(int k, DataPoint p) {
		DataPoint prev = rl[k];
		rl[k] = p;
		return prev;
	}
	public RankList getCorrectRanking() {
		double[] score = new double[rl.length];
		for(int i=0;i<rl.length;i++)
			score[i] = rl[i].getLabel();
		int[] idx = Sorter.sort(score, false); 
		return new RankList(this, idx);
	}
	
	public RankList getRanking(short fid) {
		double[] score = new double[rl.length];
		for(int i=0;i<rl.length;i++)
			score[i] = rl[i].getFeatureValue(fid);
		int[] idx = Sorter.sort(score, false);
		return new RankList(this, idx);
	}

	public int getFeatureCount() {
		int fc = 0;
		for (DataPoint dataPoint : rl) {
			fc = Math.max(fc, dataPoint.getKnownFeatures());
		}
		return fc;
	}
}
