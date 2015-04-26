package com.luma;

import com.chroma.*;
import processing.core.*;

public class TestClient extends PApplet {
	public TestClient() {
	}

	Luma testLuma;
	Chroma[] lumaClusters;
	Chroma[] lumaDomain;

	int startTime;
	int endTime;
	int totalTime;

	int lumaNumber = 5;
	int lumaQuality = 50;

	int lumaMinL = 70;
	int lumaMaxL = 75;

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
	    lumaDomain = testLuma.getDomain();

	    endTime = millis();


	    println("lumaClusters Length: " + lumaClusters.length);
	    println("lumaDomain Length: " + lumaDomain.length);

	    println("Start Time(ms): " + startTime);
	    println("End Time(ms): " + endTime);
	    println("Total Time(ms): " + (endTime-startTime));
	    println();

	}

	@Override
	public void draw() {
	    background(255);
	    plotLuma();
	    plotLumaCentroids();
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
	    }
	}
}


