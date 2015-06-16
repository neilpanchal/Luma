package com.luma;

public enum MunsellHue {
	
	R(0), YR(36), Y(72), GY(108), G(144), BG(180), B(216), PB(252), P(288), RP(324);
	
	private int hue;
	
	private MunsellHue(int hue) {
		this.hue = hue;
	}
	
	public int getHue() {
		return this.hue;
	}
}
