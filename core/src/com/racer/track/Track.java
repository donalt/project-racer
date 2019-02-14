package com.racer.track;

import java.util.ArrayList;

public class Track {
	public int laps;
	public int segmentLength;
	public ArrayList<Node> nodes;
	
	public Track(int laps, int segLen) {
		this.laps = laps;
		segmentLength = segLen;
		nodes = new ArrayList<Node>();
	}
	
	public void addSegment(int length, int width, int rotation) {
		nodes.add(new Node(length, width, rotation));
	}
}
