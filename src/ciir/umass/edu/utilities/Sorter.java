/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.utilities;
import java.util.List;
import java.util.ArrayList;
/**
 * This class contains the implementation of some simple sorting algorithms.
 * @author Van Dang
 * @version 1.3 (July 29, 2008)
 */
public class Sorter {
	/**
	 * Sort a double array using Interchange sort.
	 * @param sortVal The double array to be sorted. 
	 * @param asc TRUE to sort ascendingly, FALSE to sort descendingly.
	 * @return The sorted indexes.
	 */
	public static int[] sort(double[] sortVal, boolean asc)
	{
		int[] freqIdx = new int[sortVal.length];
		for(int i=0;i<sortVal.length;i++)
			freqIdx[i] = i;
		for(int i=0;i<sortVal.length-1;i++)
		{
			int max = i;
			for(int j=i+1;j<sortVal.length;j++)
			{
				if(asc)
				{
					if(sortVal[freqIdx[max]] > sortVal[freqIdx[j]])
						max = j;
				}
				else
				{
					if(sortVal[freqIdx[max]] <  sortVal[freqIdx[j]])
						max = j;
				}
			}
			//swap
			int tmp = freqIdx[i];
			freqIdx[i] = freqIdx[max];
			freqIdx[max] = tmp;
		}
		return freqIdx;
	}
	/**
	 * Sort an integer array using Quick Sort.
	 * @param sortVal The integer array to be sorted.
	 * @param asc TRUE to sort ascendingly, FALSE to sort descendingly.
	 * @return The sorted indexes.
	 */
	public static int[] sort(int[] sortVal, boolean asc)
	{
		return qSort(sortVal, asc);
	}
	/**
	 * Sort an integer array using Quick Sort.
	 * @param l The integer array to be sorted.
	 * @param asc TRUE to sort ascendingly, FALSE to sort descendingly.
	 * @return The sorted indexes.
	 */
	private static int[] qSort(int[] l, boolean asc)
	{
		int[] idx = new int[l.length];
		List<Integer> idxList = new ArrayList<Integer>();
		for(int i=0;i<l.length;i++)
			idxList.add(i);
		//System.out.print("Sorting...");
		idxList = qSort(l, idxList, asc);
		for(int i=0;i<l.length;i++)
			idx[i] = idxList.get(i);
		//System.out.println("[Done.]");
		return idx;
	}

	/**
	 * Quick sort internal.
	 * @param l
	 * @param idxList
	 * @param asc
	 * @return The sorted indexes.
	 */
	private static List<Integer> qSort(int[] l, List<Integer> idxList, boolean asc)
	{
		int mid = idxList.size()/2;
		List<Integer> left = new ArrayList<>();
		List<Integer> right = new ArrayList<>();
		List<Integer> pivot = new ArrayList<>();
		for (int item : idxList) {
			if (l[item] > l[idxList.get(mid)]) {
				if (asc)
					right.add(item);
				else
					left.add(item);
			} else if (l[item] < l[idxList.get(mid)]) {
				if (asc)
					left.add(item);
				else
					right.add(item);
			} else
				pivot.add(item);
		}
		if(left.size() > 1)
			left = qSort(l, left, asc);
		if(right.size() > 1)
			right = qSort(l, right, asc);
		List<Integer> newIdx = new ArrayList<>();
		newIdx.addAll(left);
		newIdx.addAll(pivot);
		newIdx.addAll(right);
		return newIdx;
	}
}
