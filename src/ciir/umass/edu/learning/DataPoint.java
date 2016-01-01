/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.learning;

import ciir.umass.edu.features.LibSVMFormat;

/**
 * @author vdang
 * 
 * This class implements objects to be ranked. In the context of Information retrieval, each instance is a query-url pair represented by a n-dimentional feature vector.
 * It should be general enough for other ranking applications as well (not limited to just IR I hope). 
 */
public abstract class DataPoint {
	protected static float UNKNOWN = Float.NaN;
	protected static boolean isUnknown(float fVal) {
		return Float.isNaN(fVal);
	}

	//attributes
	protected float label = 0.0f;//[ground truth] the real label of the data point (e.g. its degree of relevance according to the relevance judgment)
	protected String id = "";//id of this data point (e.g. query-id)
	protected String description = "";
	protected float[] fVals = null; //fVals[0] is un-used. Feature id MUST start from 1
	
	//helper attributes
	protected int knownFeatures; // number of known feature values
	
	//internal to learning procedures
	protected double cached = -1.0;//the latest evaluation score of the learned model on this data point

	/**
	* Get the value of the feature with the given feature ID
	* @param fid
	* @return
	*/
	public abstract float getFeatureValue(int fid);
	
	/**
	* Set the value of the feature with the given feature ID
	* @param fid
	* @param fval
	*/
	public abstract void setFeatureValue(int fid, float fval);
	
	/**
	* Sets the value of all features with the provided dense array of feature values
	*/
	public abstract void setFeatureVector(float[] dfVals);
	
	/**
	* Gets the value of all features as a dense array of feature values.
	*/
	public abstract float[] getFeatureVector();
	
	/**
	* Default constructor. No-op.
	*/
	protected DataPoint() {};
	
	/**
	* The input must have the form: 
	* @param text
	*/
	protected DataPoint(String text)
	{
		PointBuilder pb = LibSVMFormat.parsePoint(text, new Dataset());
		pb.build(this);
	}
	
	public String getID()
	{
		return id;
	}
	public void setID(String id)
	{
		this.id = id;
	}
	public float getLabel()
	{
		return label;
	}
	public void setLabel(float label)
	{
		this.label = label;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public void setCached(double c)
	{
		cached = c;
	}
	public double getCached()
	{
		return cached;

	}
	public void resetCached()
	{
		cached = -100000000.0f;;
	}
	
	public String toString()
	{
		float[] fVals = getFeatureVector();
		String output = ((int)label) + " " + "qid:" + id + " ";
		for(int i=1;i<fVals.length;i++)
			if(!isUnknown(fVals[i]))
				output += i + ":" + fVals[i] + ((i==fVals.length-1)?"":" ");
		output += " " + description;
		return output;
	}

	public int getIntLabel() {
		return getLabel() > 0.0f ? 1 : 0;
	}

	public void setKnownFeatures(int knownFeatures) {
		this.knownFeatures = knownFeatures;
	}

	public int getKnownFeatures() {
		return knownFeatures;
	}
}