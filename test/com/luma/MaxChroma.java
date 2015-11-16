// These tests are obsolete and do not work in Processing 3

package com.luma;

import java.util.HashMap;
import java.util.Map;

import com.chroma.*;

import processing.core.*;

public class MaxChroma extends PApplet {

	Luma testLuma;
	Chroma[] lumaClusters;
	Chroma[] lumaMaxChroma;
	Chroma[] lumaDomain;

	int startTime;
	int endTime;
	int totalTime;

	int lumaNumber = 5;
	int lumaQuality = 50;

	int lumaMinL = 50;
	int lumaMaxL = 80;

	int lumaMinC = 0;
	int lumaMaxC = 128;

	int lumaMinH = 0;
	int lumaMaxH = 360;

	

	@Override
	public void setup() {

	    size(600, 600, "processing.core.PGraphicsRetina2D");
	    rectMode(CENTER);
	    smooth();
	    noStroke();
	    frameRate(30);


	    startTime = millis();

	    testLuma = new Luma(lumaNumber, lumaQuality, lumaMinL, lumaMaxL, lumaMinC, lumaMaxC, lumaMinH, lumaMaxH);

	    lumaClusters = testLuma.getClusters();
//	    lumaMaxChroma = getMaxChromaArray(lumaClusters);
//	    println(lumaClusters);
//	    println(lumaMaxChroma);
	    lumaDomain = testLuma.getDomain();

	    endTime = millis();


	    println("lumaClusters Length: " + lumaClusters.length);
	    println("lumaDomain Length: " + lumaDomain.length);

	    println("Start Time(ms): " + startTime);
	    println("End Time(ms): " + endTime);
	    println("Total Time(ms): " + (endTime-startTime));
	    println();
	    
	    noLoop();

	}

	@Override
	public void draw() {
	    background(255);
	    plotLuma();
	    plotLumaCentroids();
	    getMaxChroma(lumaClusters[0]);
	}

	void plotLuma() {
	    for (int i=0 ; i< lumaDomain.length; i++) {
	        // fill(lumaDomain[i].getColor());
	        stroke(lumaDomain[i].get());
	        strokeWeight(5);
	        point(map((float)lumaDomain[i].getLCH(Channel.H), 0, 360, 0, width), map((float)lumaDomain[i].getLCH(Channel.C), 0, 132, 0, height));
	    }
	}

	void plotLumaCentroids() {
	    for(int j = 0; j < lumaClusters.length; j++) {
	        fill(lumaClusters[j].get());
	        stroke(0);
	        strokeWeight(2);
	        rect(map((float)lumaClusters[j].getLCH(Channel.H), 0, 360, 0, width), map((float)lumaClusters[j].getLCH(Channel.C), 0, 132, 0, height), 20, 20);

	        fill(getMaxChroma(lumaClusters[j]).get());
	        stroke(0);
	        strokeWeight(2);
	        ellipse(map((float)getMaxChroma(lumaClusters[j]).getLCH(Channel.H), 0, 360, 0, width), map((float)getMaxChroma(lumaClusters[j]).getLCH(Channel.C), 0, 132, 0, height), 20, 20);
	    }
	}
	
//	public Chroma[] getMaxChromaArray (Chroma[] input) {
//		
//		Chroma[] output = new Chroma[input.length];
//		
//		for (int i = 0; i < input.length; i++) {
//			output[i] = getMaxChroma(input[i]);
//		}
//
//		return output;
//	}
	
	public Chroma getMaxChroma(Chroma input) {
		System.out.println("Test Candidate: Luma: " + input.getLCH(Channel.L) +
				" | Chroma: " + input.getLCH(Channel.C) +
				" | Hue: " + input.getLCH(Channel.H) +
				" | Clipped: " + input.clipped());
		
		System.out.println("Input Red: " + input.getRGB(Channel.R) + 
						" | Input Green: " + input.getRGB(Channel.G) + 
						" | Input Blue: " + input.getRGB(Channel.B));

		int red_ = (int)input.getRGB(Channel.R);
		int green_ = (int)input.getRGB(Channel.G);
		int blue_ = (int)input.getRGB(Channel.B);
		
		Chroma maxRed = new Chroma(255, green_, blue_);
		Chroma maxGreen = new Chroma(red_, 255, blue_);
		Chroma maxBlue = new Chroma(red_, green_, 255);

		Map<Double, Chroma> mapChroma = new HashMap<Double, Chroma>();
		mapChroma.put(maxRed.getLCH(Channel.C), maxRed);
		mapChroma.put(maxGreen.getLCH(Channel.C), maxGreen);
		mapChroma.put(maxBlue.getLCH(Channel.C), maxBlue);
		
		double currentMax = 0.0;
		double max = 0.0;
		
		for (Double key : mapChroma.keySet()) {
			
			currentMax = key;
			if (currentMax >= max) {
				max = currentMax;
			}
			
			System.out.println("Test Candidate: Luma: " + mapChroma.get(key).getLCH(Channel.L) +
					" | Chroma: " + mapChroma.get(key).getLCH(Channel.C) +
					" | Hue: " + mapChroma.get(key).getLCH(Channel.H) +
					" | Max Chroma: " + key +
					" | Clipped: " + mapChroma.get(key).clipped());
			
			System.out.println("Test Red: " + mapChroma.get(key).getRGB(Channel.R) + 
					" | Test Green: " + mapChroma.get(key).getRGB(Channel.G) + 
					" | Test Blue: " + mapChroma.get(key).getRGB(Channel.B));
			
			
			
			
			//	System.out.println(key + " " + mapChroma.get(key));
		}
	    System.out.println("Color: " + mapChroma.get(max) + " | Max Chroma: " + max);
	    
	    System.out.println("-------------------------------------------------------------------");
		return mapChroma.get(max);
	}

}


