
package com.luma;

import com.chroma.*;

import processing.core.*;

public class Saturate extends PApplet {

	public static void main(String[] args) {
		String[] a = { "Main" };
		PApplet.runSketch(a, new Saturate());
	}
	
	Luma testLuma;
	Chroma[] lumaClusters;
	Chroma[] lumaMaxChroma;
	Chroma[] lumaDomain;

	int startTime;
	int endTime;
	int totalTime;

	int lumaNumber = 10;
	int lumaQuality = 80;

	int lumaMinL = 70;
	int lumaMaxL = 71;

	int lumaMinC = 0;
	int lumaMaxC = 128;

	int lumaMinH = 0;
	int lumaMaxH = 360;

	@Override
	public void settings() {
		size(1000, 1000, FX2D);
		pixelDensity(2);
	}
	
	
	@Override
	public void setup() {

		rectMode(CENTER);
		smooth();
		noStroke();
		frameRate(30);
		noLoop();

		startTime = millis();

		testLuma = new Luma(lumaNumber, lumaQuality, lumaMinL, lumaMaxL,
				lumaMinC, lumaMaxC, lumaMinH, lumaMaxH);

		lumaClusters = testLuma.getClusters();

		lumaMaxChroma = new Chroma[lumaClusters.length];
		for (int i = 0; i < lumaClusters.length; i++) {
			lumaMaxChroma[i] = lumaClusters[i].saturate();

		}

		lumaDomain = testLuma.getDomain();

		endTime = millis();

		println(testLuma);
		println("lumaClusters Length: " + lumaClusters.length);
		println("lumaDomain Length: " + lumaDomain.length);

		println("Start Time(ms): " + startTime);
		println("End Time(ms): " + endTime);
		println("Total Time(ms): " + (endTime - startTime));
		println();

	}

	@Override
	public void draw() {
		background(255);
		plotLuma();
		plotLumaCentroids();

	}

	void plotLuma() {
		for (int i = 0; i < lumaDomain.length; i++) {
			// fill(lumaDomain[i].getColor());
			stroke(lumaDomain[i].get());
			strokeWeight(5);
			point(map((float) lumaDomain[i].getLCH(Channel.H), 0, 360, 0, width),
					map((float) lumaDomain[i].getLCH(Channel.C), 0, 132, 0,
							height));
		}
	}

	void plotLumaCentroids() {

		float cX;
		float cY;
		float mX;
		float mY;

		for (int j = 0; j < lumaClusters.length; j++) {

			cX = map((float) lumaClusters[j].getLCH(Channel.H), 0, 360, 0, width);
			cY = map((float) lumaClusters[j].getLCH(Channel.C), 0, 132, 0, height);
			mX = map((float) lumaMaxChroma[j].getLCH(Channel.H), 0, 360, 0, width);
			mY = map((float) lumaMaxChroma[j].getLCH(Channel.C), 0, 132, 0, height);

			fill(lumaClusters[j].get());
			stroke(100);
			strokeWeight(2);
			rect(cX, cY, 24, 24);

			fill(getMaxChroma(lumaClusters[j]).get());
			stroke(100);
			strokeWeight(2);
			ellipse(mX, mY, 24, 24);
			
			stroke(100);
			strokeWeight(2);
			line(cX,cY,mX,mY);
		}
	}

	public Chroma getMaxChroma(Chroma input) {
		return input.saturate();
	}

	@Override
	public void keyReleased() {
		// Save a screenshot in PNG format
		if (key == 's' || key == 'S') {
			saveFrame("####.png");
		}
	}
}
