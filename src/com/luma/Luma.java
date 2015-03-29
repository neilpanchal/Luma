package com.luma;

import java.util.ArrayList;
import java.util.Random;
import com.chroma.*; // Requires Chroma library (https://github.com/neilpanchal/Chroma)

public class Luma {

    // FIELDS---- ///////////////////////////////////////////////////////////////////////
    private int lumaClusterCount;
    private int maxIter;
    private int quality;
    private float convergenceQuality;

    // Range variables
    private float minL;
    private float maxL;
    private float minC;
    private float maxC;
    private float minH;
    private float maxH;

    // Array variables
    private ArrayList<LumaData> lumaDomain;
    private ArrayList<LumaCentroid> lumaClusters;


    // CONSTRUCTORS ---- ////////////////////////////////////////////////////////////////
    public Luma(int lumaClusterCount_, int quality_,  float minL_, float maxL_,
                                        float minC_, float maxC_,
                                        float minH_, float maxH_) {

        lumaClusterCount = lumaClusterCount_;
        quality = quality_;
        maxIter = quality_;
        convergenceQuality = mapRange(quality_, 0.0F, 100.0F, 0.1F, 0.0001F);
        minL = minL_;
        maxL = maxL_;
        minC = minC_;
        maxC = maxC_;
        minH = minH_;
        maxH = maxH_;

        // Create random data points in the restricted domain. This method does not allow for constant domain size. The total number of data points is proportional to the input ranges of luma, chroma and hue. As a consequence, the processing time is not constant & difficult to predict. Therefore, it has been deprecated and replaced by the 'createConstantDomain' method.

        /*
        lumaDomain = new ArrayList<LumaData>();
        lumaDomain = createDomain((int)mapRange(quality, 0, 100, 1000, 7500), minL_, maxL_, minC_, maxC_, minH_, maxH_);
        */

        lumaDomain = new ArrayList<LumaData>();
        lumaDomain = createConstantDomain((int)mapRange(quality, 0.0F, 100.0F, 1000.0F, 7500.0F), minL_, maxL_, minC_, maxC_, minH_, maxH_);

        lumaClusters = new ArrayList<LumaCentroid>();
        lumaClusters = createClusters(lumaClusterCount);

        kMeansClustering();
    }

	// CENTROID CLASS ---- //////////////////////////////////////////////////////////////
    // LumaCentroid class instantiates datapoints containing Chroma objects.
    private class LumaCentroid {

        private Chroma lumaChroma;

        public LumaCentroid (float l_, float c_, float h_) {
            this.lumaChroma = new Chroma(l_, c_, h_, ColorSpace.LCH);
        }

        public LumaCentroid (LumaCentroid toClone) {
            float l_ = toClone.getChroma().getLum();
            float c_ = toClone.getChroma().getChr();
            float h_ = toClone.getChroma().getHue();
            this.lumaChroma = new Chroma(l_, c_, h_, ColorSpace.LCH);
        }

        // Getter & Setter methods
        public Chroma getChroma() {
            return this.lumaChroma;
        }

        public void setChroma(float l_, float c_, float h_) {
            this.lumaChroma.setLCH(l_, c_, h_);
        }

        public boolean getClippedStatus() {
            return this.lumaChroma.clipped();
        }

        // Print methods
        public String toString() {

            StringBuilder result = new StringBuilder();
            result.append(this.lumaChroma.toString(ColorSpace.LCH));
            return result.toString();

        }
    }


    // DATASET CLASS ---- ///////////////////////////////////////////////////////////////
    // This class holds all points in the Domain and the ID of the closest cluster centroid. The ID field is synced with the j-index of the lumaClusters ArrayList.
    private class LumaData extends LumaCentroid {

        private int clusterID;

        public LumaData(float l_, float c_, float h_, int clusterID_) {
            super(l_, c_, h_);
            this.clusterID = clusterID_;
        }

        public LumaData(float l_, float c_, float h_) {
            this(l_, c_, h_, -1);
        }

        public void setClusterID(int clusterNumber) {
            this.clusterID = clusterNumber;
        }

        public int getClusterID() {
            return this.clusterID;
        }

    }

    // EXPORT METHODS ---- //////////////////////////////////////////////////////////////
    public Chroma[] getDomain() {

        int domainSize = this.lumaDomain.size();
        Chroma[] tempDomainArray = new Chroma[domainSize];

        for (int i = 0; i < domainSize; i++) {
            tempDomainArray[i] = this.lumaDomain.get(i).getChroma();
        }
        return tempDomainArray;
    }


    public Chroma[] getClusters() {

        int clusterSize = this.lumaClusters.size();
        Chroma[] tempClusterArray = new Chroma[clusterSize];

        for (int i = 0; i < clusterSize; i++) {
            tempClusterArray[i] = this.lumaClusters.get(i).getChroma();
        }
        return tempClusterArray;
    }


    // CLUSTERING METHODS ---- //////////////////////////////////////////////////////////
    public void kMeansClustering() {

        int index = 0;
        int convergenceIndex = 0;
        float LARGE_NUMBER = 100000000.0F;
        boolean converged = false;

        while(!converged) {
            // This boolean array will hold the convergence status of all centroids.
            boolean[] convergenceTest = new boolean[lumaClusters.size()];

            // Assign each data point to its nearest cluster centroid. The centroid ID assigned to each data point is the current j-index of the lumaClusters ArrayList.
            for (int i = 0; i < lumaDomain.size(); i++) {
                float minDistance = LARGE_NUMBER;

                for(int j = 0; j < lumaClusters.size(); j++) {
                    float distance = computeDistance(lumaDomain.get(i), lumaClusters.get(j));
                    if (distance < minDistance) {
                        lumaDomain.get(i).setClusterID(j);
                        minDistance = distance;
                        index++;
                    }
                }
            }

            // Now compute the centroid means and check if its within the Domain
            for (int j = 0; j < lumaClusters.size(); j++) {

                int count = 0;

                // kMean array will acculumate all Euclidean distances.
                float[] kMean = new float[]{0.0F,0.0F,0.0F};

                LumaCentroid currentCluster = lumaClusters.get(j);

                for (int i = 0; i < lumaDomain.size(); i++) {
                    // Collect all data points that have the current cluster ID
                    if (lumaDomain.get(i).getClusterID() == j) {
                        kMean[0] += lumaDomain.get(i).getChroma().getLum();
                        kMean[1] += lumaDomain.get(i).getChroma().getChr();
                        kMean[2] += lumaDomain.get(i).getChroma().getHue();
                        count++;
                    }
                }

                if (count!=0) {
                    // The current centroid has at least one assigned data point

                    kMean[0] /= count; // Compute the means
                    kMean[1] /= count;
                    kMean[2] /= count;

                    // Construct a new centroid based on the new location
                    LumaCentroid tempCentroid = new LumaCentroid(kMean[0], kMean[1], kMean[2]);

                    // Check if the new location of the centroid is within the bounds

                    if (validChroma(tempCentroid.getChroma())) {

                        convergenceTest[j] = checkConvergence(tempCentroid, lumaClusters.get(j));

                        lumaClusters.set(j,tempCentroid);

                        /*  ---- PRINT CODE-------------------------------------------
                        println("[Centroid " + (j+1) + "]\t" + tempCentroid + "\tConverged: " +convergenceTest[j]);
                        */

                    } else {

                        LumaCentroid randomCentroid = new LumaCentroid(getRandom(this.minL,this.maxL), getRandom(this.minC,this.maxC), getRandom(this.minH, this.maxH));

                        // Force to compute the kMeans again and see if the centroid converges.
                        convergenceTest[j] = false;

                        lumaClusters.set(j,randomCentroid);

                        /*  ---- PRINT CODE-------------------------------------------
                        println("[Centroid* " + (j+1) + "]\t" + tempCentroid + "\tConverged: " +convergenceTest[j]);
                        println("Out of bounds. Move the centroid to a random location: ");
                        println("[Centroid+ " + (j+1) + "]\t" + randomCentroid + "\tConverged: " + convergenceTest[j]);
                        */
                    }

                } else {

                    // Count is ZERO! No data points assigned to this cluster. Find the closest data point and assign its location.

                    float minDistance = LARGE_NUMBER;
                    int closest = 0;

                    for (int i = 0; i < lumaDomain.size(); i++) {

                        float distance = computeDistance(lumaDomain.get(i), currentCluster);
                        if (distance < minDistance) {
                            minDistance = distance;
                            closest = i;
                        }
                    }

                    // Clone the data point and create a centroid at the new location. Cloning is important otherwise the centroid will only get copy the reference to the LumaData object and move the data point itself in subsequent iterations. If the cluster has only one data point, it will not converge.

                    LumaCentroid closestCentroid = new LumaCentroid(lumaDomain.get(closest));

                    // Force to compute the kMeans again and see if the centroid converges.
                    convergenceTest[j] = false;


                    /*  ---- PRINT CODE-----------------------------------------------
                    println("Assigned closest data point: " + closest);
                    println("[Centroid " + (j+1) + "]\t" + closestCentroid + "\tConverged: " +convergenceTest[j]);
                    */

                    // Replace the current cluster with the newly found closestCentroid location.
                    lumaClusters.set(j,closestCentroid);
                }

            }
            /*  ---- PRINT CODE-------------------------------------------------------
            println("---------------------------------------------------------------------------------------------------------------");
            */

            convergenceIndex++;
            converged = checkTruth(convergenceTest);

            if(convergenceIndex >= this.maxIter) {
                break;
            }
        }


        /*  ---- PRINT CODE-----------------------------------------------------------
        println();
        if (convergenceIndex == maxIter) {
            println("Maximum Iterations reached: " + convergenceIndex);
        } else {
            println("Iterations to converge Centroids: " + convergenceIndex);
        }
        println("Iterations to find the closest Centroid: " + index +"\n");
        */

    }

    // INITIALIZATION METHODS ///////////////////////////////////////////////////////////
    private ArrayList<LumaData> createConstantDomain(int size_, float minL_, float maxL_, float minC_, float maxC_, float minH_, float maxH_) {

        ArrayList<LumaData> lumaDomain_ = new ArrayList<LumaData>();

        for (int i = 0; i < size_; i++) {

            boolean foundValidChroma = false;

            // Try until the centroid is not clipped
            while(!foundValidChroma) {
                LumaData tempData = new LumaData(getRandom(this.minL,this.maxL), getRandom(this.minC,this.maxC), getRandom(this.minH, this.maxH));

                if (validChroma(tempData.getChroma())) {
                    lumaDomain_.add(tempData);
                    foundValidChroma = true;
                }
            }
        }
        return lumaDomain_;
    }

    private ArrayList<LumaCentroid> createClusters(int lumaClusterCount_) {

        ArrayList<LumaCentroid> lumaClusters_ = new ArrayList<LumaCentroid>();

        for (int i = 0; i < lumaClusterCount_; i++) {
            boolean foundValidCentroid = false;
            // Try until the centroid is a valid RGB color and within the range. See validChroma method for the checking routine.

            while(!foundValidCentroid) {

                LumaCentroid tempCentroid = new LumaCentroid(getRandom(this.minL,this.maxL), getRandom(this.minC,this.maxC), getRandom(this.minH, this.maxH));

                if (validChroma(tempCentroid.getChroma())) {

                    lumaClusters_.add(tempCentroid);
                    foundValidCentroid = true;
                }
            }
        }
        return lumaClusters_;
    }

    /*  ---- DEPRECATED -----------------------------------------------------------------
    private ArrayList<LumaData> createDomain(float minL_, float maxL_, float minC_, float maxC_, float minH_, float maxH_) {

        ArrayList<LumaData> lumaDomain_ = new ArrayList<LumaData>();

        int l_step = (int)map(this.quality, 0, 100, 8, 2);
        int c_step = (int)map(this.quality, 0, 100, 12, 2);
        int h_step = (int)map(this.quality, 0, 100, 24, 4);

        println("Quality: L: " + l_step + "\tC: " + c_step + "\tH: " + h_step);
        for(int l=(int)minL_; l<=(int)maxL_; l+=l_step){
            for(int c=(int)minC_; c<=(int)maxC_; c+=c_step){
                for(int h=(int)minH_; h<=(int)maxH_; h+=h_step){

                    LumaData tempData = new LumaData(l, c, h);

                    //Only add valid chroma to the array
                    if (!tempData.getClippedStatus()) {
                        lumaDomain_.add(tempData);
                    }
                }
            }
        }

        return lumaDomain_;
    }
    */

    // MATH & UTILITY METHODS ---- //////////////////////////////////////////////////////
    // Check if all array items are true
    private static boolean checkTruth(boolean[] array_) {
        for (boolean i: array_) if(!i) return false;
            return true;
    }

    // Check if the data point is a valid RGB color
    private boolean validChroma(Chroma chromaColor_) {
        return !chromaColor_.clipped() && validRange(chromaColor_);

    }

    // Check if the data point has moved out of the input range.
    private boolean validRange(Chroma chromaColor_) {
        return (chromaColor_.getLum() > this.minL)
            && (chromaColor_.getLum() < this.maxL)
            && (chromaColor_.getChr() > this.minC)
            && (chromaColor_.getChr() < this.maxC)
            && (chromaColor_.getHue() > this.minH)
            && (chromaColor_.getHue() < this.maxH);
    }


    private boolean checkConvergence(LumaCentroid currentCentroid_, LumaCentroid prevCentroid_) {
        return computeDistance(currentCentroid_, prevCentroid_) < this.convergenceQuality;
    }

    // Euclidean distance in 3D
    private static float computeDistance(LumaCentroid point1_, LumaCentroid point2_) {
        return (float)Math.sqrt(
            Math.pow(point1_.getChroma().getLum() - point2_.getChroma().getLum(), 2) +
            Math.pow(point1_.getChroma().getChr() - point2_.getChroma().getChr(), 2) +
            Math.pow(point1_.getChroma().getHue() - point2_.getChroma().getHue(), 2));

    }

    private static float getRandom(float min_, float max_) {
        return min_ + new Random().nextFloat() * (max_-min_);
    }

    private static float mapRange(float value_, float inputMin_, float inputMax_, float outputMin_, float outputMax_) {
        return (value_ - inputMin_) * (outputMax_ - outputMin_) / (inputMax_ - inputMin_) + outputMin_;
    }
}
