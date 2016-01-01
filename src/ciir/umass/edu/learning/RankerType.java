/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning;

public enum RankerType {
	MART(0),
	RANKBOOST(1),
	RANKNET(2),
	ADARANK(3),
	COOR_ASCENT(4),
	LAMBDARANK(5),
	LAMBDAMART(6),
	LISTNET(7),
	RANDOM_FOREST(8),
	LINEAR_REGRESSION(9);

	/** RankLib ranker number, needs to be steady don't want to keep using ordinal because that could change if we insert in the middle. */
	int cliNumber;
	RankerType(int cliNumber) {
		this.cliNumber = cliNumber;
	}

	public int getRankerId() {
		return cliNumber;
	}
}
