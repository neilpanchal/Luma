package com.luma;

import java.util.ArrayList;
import java.util.Random;

// Requires Chroma library (https://github.com/neilpanchal/Chroma)
import com.chroma.*;

public class Luma {

	// FIELDS----
	// ///////////////////////////////////////////////////////////////////////
	private int clusterCount;
	private int maxIter;
	private int quality;
	private double convergenceQuality;

	// Range variables
	private double minL;
	private double maxL;
	private double minC;
	private double maxC;
	private double minH;
	private double maxH;

	// Array variables
	private ArrayList<Node> nodeList;
	private ArrayList<Centroid> centroidList;

	// CONSTRUCTORS ----
	// ////////////////////////////////////////////////////////////////
	public Luma(int clusterCount_, int quality_, double minL_, double maxL_,
			double minC_, double maxC_, double minH_, double maxH_) {

		// Validate arguments
		Args.checkForPositive(clusterCount_);
		Args.checkForPositive(quality_);
		Args.checkForRange(quality_, 0, 100);

		Args.checkForRange(minL_, 0, 100);
		Args.checkForRange(maxL_, 0, 100);
		Args.checkForLowHigh(minL_, maxL_);

		Args.checkForRange(minC_, 0, 128);
		Args.checkForRange(maxC_, 0, 128);
		Args.checkForLowHigh(minC_, maxC_);

		clusterCount = clusterCount_;
		quality = quality_;
		maxIter = quality_;
		convergenceQuality = mapRange(quality_, 0.0, 100.0, 0.1, 0.0001);
		minL = minL_;
		maxL = maxL_;
		minC = minC_;
		maxC = maxC_;
		
		// Handling cyclic domains. 
		if (minH_ < maxH_) {
			
			if (cycleHue(maxH_) > minH_) {
				maxH_ = 360+minH_;
			}

			minH = minH_;
			maxH = maxH_;
			
		} else if (minH_ > maxH_) {
			
			if (minH_> (360+maxH_)) {
				minH_ = cycleHue(minH_);
			}
			minH = minH_;
			maxH = 360+maxH_;
		} else {
			minH = minH_;
			maxH = maxH_;
		}

		// Create random data points in the restricted domain. This method does
		// not allow for constant domain size. The total number of data points
		// is proportional to the input ranges of luma, chroma and hue. As a
		// consequence, the processing time is not constant & difficult to
		// predict. Therefore, it has been deprecated and replaced by the
		// 'createDomain' method.

		/*
		 * nodeList = new ArrayList<Node>(); nodeList =
		 * createDomain((int)mapRange(quality, 0, 100, 1000, 7500), minL_,
		 * maxL_, minC_, maxC_, minH_, maxH_);
		 */

		nodeList = new ArrayList<Node>();
		nodeList = createDomain(
				(int) mapRange(quality, 0.0F, 100.0, 1000.0, 7500.0), minL_,
				maxL_, minC_, maxC_, minH_, maxH_);

		centroidList = new ArrayList<Centroid>();
		centroidList = createClusters(clusterCount);

		kMeansClustering();
	}

	public Luma(int clusterCount_) {

		// Default quality = 60, ranges for LCH = full domain.
		this(clusterCount_, 60, 0, 100, 0, 128, 0, 360);
	}

	public Luma(int minL_, int maxL_, int minC_, int maxC_, int minH_, int maxH_) {

		// Default number of colors = 8 and quality = 60
		this(8, 60, minL_, maxL_, minC_, maxC_, minH_, maxH_);
	}

	public Luma(MunsellHue low_, MunsellHue high_) {

	}

	// CENTROID CLASS ----
	// //////////////////////////////////////////////////////////////
	// Centroid class instantiates datapoints containing Chroma objects.
	private class Centroid {

		private Chroma lumaChroma;

		public Centroid(double l_, double c_, double h_) {
			this.lumaChroma = new Chroma(ColorSpace.LCH, l_, c_, h_);
		}

		public Centroid(Centroid toClone) {

			double[] lchComp = toClone.get().getLCH();

			this.lumaChroma = new Chroma(ColorSpace.LCH, lchComp[0],
					lchComp[1], lchComp[2]);
		}

		// Getter & Setter methods
		public Chroma get() {
			return this.lumaChroma;
		}

		public void setChroma(double l_, double c_, double h_) {
			this.lumaChroma.setLCH(l_, c_, h_);
		}

		public boolean getClippedStatus() {
			return this.lumaChroma.clipped();
		}

		// Print methods
		@Override
		public String toString() {

			StringBuilder result = new StringBuilder();
			result.append(this.lumaChroma.toString());
			return result.toString();

		}
	}

	// DATASET CLASS ----
	// ///////////////////////////////////////////////////////////////
	// This class holds all points in the Domain and the ID of the closest
	// cluster centroid. The ID field is synced with the j-index of the
	// centroidList ArrayList.
	private class Node extends Centroid {

		private int clusterID;

		public Node(double l_, double c_, double h_, int clusterID_) {

			super(l_, c_, h_);
			this.clusterID = clusterID_;
		}

		public Node(double l_, double c_, double h_) {

			this(l_, c_, h_, -1);
		}

		public void setClusterID(int clusterNumber) {

			this.clusterID = clusterNumber;
		}

		public int getClusterID() {

			return this.clusterID;
		}

	}

	// INITIALIZATION METHODS
	// ///////////////////////////////////////////////////////////////////////////////////
	private ArrayList<Node> createDomain(int size_, double minL_, double maxL_,
			double minC_, double maxC_, double minH_, double maxH_) {

		ArrayList<Node> nodeList_ = new ArrayList<Node>();

		for (int i = 0; i < size_; i++) {

			boolean foundValidChroma = false;

			// Try until the centroid is not clipped
			while (!foundValidChroma) {
				Node tempData = new Node(getRandom(this.minL, this.maxL),
						getRandom(this.minC, this.maxC), getRandom(this.minH,
								this.maxH));

				if (validChroma(tempData.get())) {
					nodeList_.add(tempData);
					foundValidChroma = true;
				}
			}
		}
		return nodeList_;
	}

	private ArrayList<Centroid> createClusters(int clusterCount_) {

		ArrayList<Centroid> centroidList_ = new ArrayList<Centroid>();

		for (int i = 0; i < clusterCount_; i++) {
			boolean foundValidCentroid = false;
			// Try until the centroid is a valid RGB color and within the range.
			// See validChroma method for the checking routine.

			while (!foundValidCentroid) {
				Centroid tempCentroid = new Centroid(getRandom(this.minL,
						this.maxL), getRandom(this.minC, this.maxC), getRandom(
						this.minH, this.maxH));

				if (validChroma(tempCentroid.get())) {
					centroidList_.add(tempCentroid);
					foundValidCentroid = true;
				}
			}
		}
		return centroidList_;
	}

	/*
	 * ---- DEPRECATED
	 * ----------------------------------------------------------------- private
	 * ArrayList<Node> createDomain(double minL_, double maxL_, double minC_,
	 * double maxC_, double minH_, double maxH_) {
	 * 
	 * ArrayList<Node> nodeList_ = new ArrayList<Node>();
	 * 
	 * int l_step = (int)map(this.quality, 0, 100, 8, 2); int c_step =
	 * (int)map(this.quality, 0, 100, 12, 2); int h_step =
	 * (int)map(this.quality, 0, 100, 24, 4);
	 * 
	 * println("Quality: L: " + l_step + "\tC: " + c_step + "\tH: " + h_step);
	 * for(int l=(int)minL_; l<=(int)maxL_; l+=l_step){ for(int c=(int)minC_;
	 * c<=(int)maxC_; c+=c_step){ for(int h=(int)minH_; h<=(int)maxH_;
	 * h+=h_step){
	 * 
	 * Node tempData = new Node(l, c, h);
	 * 
	 * //Only add valid chroma to the array if (!tempData.getClippedStatus()) {
	 * nodeList_.add(tempData); } } } }
	 * 
	 * return nodeList_; }
	 */

	// EXPORT METHODS ----
	// //////////////////////////////////////////////////////////////
	public Chroma[] getDomain() {

		int domainSize = this.nodeList.size();
		Chroma[] tempDomainArray = new Chroma[domainSize];

		for (int i = 0; i < domainSize; i++) {
			tempDomainArray[i] = this.nodeList.get(i).get();
		}
		return tempDomainArray;
	}

	public Chroma[] getClusters() {

		int clusterSize = this.centroidList.size();
		Chroma[] tempClusterArray = new Chroma[clusterSize];

		for (int i = 0; i < clusterSize; i++) {
			tempClusterArray[i] = this.centroidList.get(i).get();
		}
		return tempClusterArray;
	}

	// CLUSTERING METHODS ----
	// //////////////////////////////////////////////////////////
	public void kMeansClustering() {

		int index = 0;
		int convergenceIndex = 0;
		double LARGE_NUMBER = 100000000.0;
		boolean converged = false;

		while (!converged) {
			// This boolean array will hold the convergence status of all
			// centroids.
			boolean[] convergenceTest = new boolean[centroidList.size()];

			// Assign each data point to its nearest cluster centroid. The
			// centroid ID assigned to each data point is the current j-index of
			// the centroidList ArrayList.
			for (int i = 0; i < nodeList.size(); i++) {
				double minDistance = LARGE_NUMBER;

				for (int j = 0; j < centroidList.size(); j++) {
					double distance = computeDistance(nodeList.get(i),
							centroidList.get(j));
					if (distance < minDistance) {
						nodeList.get(i).setClusterID(j);
						minDistance = distance;
						index++;
					}
				}
			}

			// Now compute the centroid means and check if its within the Domain
			for (int j = 0; j < centroidList.size(); j++) {

				int count = 0;

				// kMean array will acculumate all Euclidean distances.
				double[] kMean = new double[] { 0.0, 0.0, 0.0 };

				Centroid currentCluster = centroidList.get(j);

				for (int i = 0; i < nodeList.size(); i++) {
					// Collect all data points that have the current cluster ID
					if (nodeList.get(i).getClusterID() == j) {
						kMean[0] += nodeList.get(i).get().getLCH(Channel.L);
						kMean[1] += nodeList.get(i).get().getLCH(Channel.C);
						kMean[2] += nodeList.get(i).get().getLCH(Channel.H);
						count++;
					}
				}

				if (count != 0) {
					// The current centroid has at least one assigned node

					kMean[0] /= count; // Compute the means
					kMean[1] /= count;
					kMean[2] /= count;

					// Construct a new centroid based on the new location
					Centroid tempCentroid = new Centroid(kMean[0], kMean[1],
							kMean[2]);

					// Check if the new location of the centroid is within the
					// bounds

					if (validChroma(tempCentroid.get())) {

						convergenceTest[j] = checkConvergence(tempCentroid,
								centroidList.get(j));

						centroidList.set(j, tempCentroid);

						/*
						 * ---- PRINT
						 * CODE-------------------------------------------
						 * println("[Centroid " + (j+1) + "]\t" + tempCentroid +
						 * "\tConverged: " +convergenceTest[j]);
						 */

					} else {

						Centroid randomCentroid = new Centroid(getRandom(
								this.minL, this.maxL), getRandom(this.minC,
								this.maxC), getRandom(this.minH, this.maxH));

						// Force to compute the kMeans again and see if the
						// centroid converges.
						convergenceTest[j] = false;

						centroidList.set(j, randomCentroid);

						/*
						 * ---- PRINT
						 * CODE-------------------------------------------
						 * println("[Centroid* " + (j+1) + "]\t" + tempCentroid
						 * + "\tConverged: " +convergenceTest[j]); println(
						 * "Out of bounds. Move the centroid to a random location: "
						 * ); println("[Centroid+ " + (j+1) + "]\t" +
						 * randomCentroid + "\tConverged: " +
						 * convergenceTest[j]);
						 */
					}

				} else {

					// Count is ZERO! No data points assigned to this cluster.
					// Find the closest data point and assign its location.

					double minDistance = LARGE_NUMBER;
					int closest = 0;

					for (int i = 0; i < nodeList.size(); i++) {

						double distance = computeDistance(nodeList.get(i),
								currentCluster);
						if (distance < minDistance) {
							minDistance = distance;
							closest = i;
						}
					}

					// Clone the data point and create a centroid at the new
					// location. Cloning is important otherwise the centroid
					// will only get copy the reference to the Node object and
					// move the data point itself in subsequent iterations. If
					// the cluster has only one data point, it will not
					// converge.

					Centroid closestCentroid = new Centroid(
							nodeList.get(closest));

					// Force to compute the kMeans again and see if the centroid
					// converges.
					convergenceTest[j] = false;

					/*
					 * ---- PRINT
					 * CODE-----------------------------------------------
					 * println("Assigned closest data point: " + closest);
					 * println("[Centroid " + (j+1) + "]\t" + closestCentroid +
					 * "\tConverged: " +convergenceTest[j]);
					 */

					// Replace the current cluster with the newly found
					// closestCentroid location.
					centroidList.set(j, closestCentroid);
				}

			}
			/*
			 * ---- PRINT
			 * CODE-------------------------------------------------------
			 * println(
			 * "---------------------------------------------------------------------------------------------------------------"
			 * );
			 */

			convergenceIndex++;
			converged = checkTruth(convergenceTest);

			if (convergenceIndex >= this.maxIter) {
				break;
			}
		}

		/*
		 * ---- PRINT
		 * CODE-----------------------------------------------------------
		 * println(); if (convergenceIndex == maxIter) {
		 * println("Maximum Iterations reached: " + convergenceIndex); } else {
		 * println("Iterations to converge Centroids: " + convergenceIndex); }
		 * println("Iterations to find the closest Centroid: " + index +"\n");
		 */

	}

	// MATH & UTLITY METHODS
	// ///////////////////////////////////////////////////////////////////////////////////

	// Check if all array items are true
	private static boolean checkTruth(boolean[] array_) {
		for (boolean i : array_)
			if (!i)
				return false;
		return true;
	}

	// Check if the data point is a valid RGB color
	private boolean validChroma(Chroma chromaColor_) {
		return !chromaColor_.clipped() && validRange(chromaColor_);

	}

	// Check if the data point has moved out of the input range.
	private boolean validRange(Chroma chromaColor_) {
		return (chromaColor_.getLCH(Channel.L) > this.minL)
				&& (chromaColor_.getLCH(Channel.L) < this.maxL)
				&& (chromaColor_.getLCH(Channel.C) > this.minC)
				&& (chromaColor_.getLCH(Channel.C) < this.maxC);
	}

	private boolean checkConvergence(Centroid currentCentroid_,
			Centroid prevCentroid_) {
		return computeDistance(currentCentroid_, prevCentroid_) < this.convergenceQuality;
	}

	// Euclidean distance in 3D
	private static double computeDistance(Centroid point1_, Centroid point2_) {
		return Math.sqrt(Math.pow(point1_.get().getLCH(Channel.L)
				- point2_.get().getLCH(Channel.L), 2)
				+ Math.pow(point1_.get().getLCH(Channel.C)
						- point2_.get().getLCH(Channel.C), 2)
				+ Math.pow(point1_.get().getLCH(Channel.H)
						- point2_.get().getLCH(Channel.H), 2));

	}

	private static double getRandom(double min_, double max_) {
		return min_ + new Random().nextDouble() * (max_ - min_);
	}

	private static double mapRange(double value_, double inputMin_,
			double inputMax_, double outputMin_, double outputMax_) {
		return (value_ - inputMin_) * (outputMax_ - outputMin_)
				/ (inputMax_ - inputMin_) + outputMin_;
	}

	private static double cycleHue(double hue_) {

		if (hue_ >= 0 && hue_ <= 360) {
			return hue_;
		} else if (hue_ >= 0) {
			return hue_ % 360;
		} else {
			return cycleHue(360 - (-hue_));
		}
	}
}
