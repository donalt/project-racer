package com.racer.track;

/**
 * A Node tells the generation algorithm how the track
 * looks like. It stores a length measured in segments,
 * a pixel width and a rotation measured in degrees.
 */
public class Node {
	public int length;
	public int width;
	public double rot;
	
	public Node (int l, int w, int r) {
		length = l;
		width = w;
		// the '-' is there to make the degrees go counter-clockwise, which
		// is what you should be used to when it comes to trigonometry.
		rot = Math.toRadians(-r);
	}
}