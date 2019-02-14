package com.racer;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.math.Vector2;

/**
 * AIcontroller uses the current state of the track, items and cars to
 * very smartly control AI cars.
 *
 */
public class AIcontroller {
	private static Random rand;
	
	private Car car;
	private ArrayList<Vector2> segments;
	private boolean[] keys;
	
	private Vector2 temp;
	private Vector2 temp2;
	
	/**
	 * Creates a new AI controller.
	 * @param car The car to control.
	 * @param segments The list that defines the tracks layout.
	 */
	public AIcontroller(Car car, ArrayList<Vector2> segments) {
		if (rand == null) rand = new Random();
		
		this.car = car;
		this.segments = segments;
		keys = new boolean[4];
		
		temp = new Vector2(1, 0);
		temp2 = new Vector2(1, 0);
	}
	
	/**
	 * Calculates how the car should move.
	 * @return An array, [up, down, left, right], telling which keys to press.
	 */
	public boolean[] useBrain() {
		int i = car.getSeg()*2;
		
		float dot = 0;
		boolean goLeft = false;
		temp2.setAngleRad(car.getAngle()).nor();
		
		// Calculate an interpolated dot product twice. The first time using
		// the actual angle of the car, the other time with an offset of 5 degrees.
		// By comparing the two, we will know if the car should turn left or right.
		for (int k=0; k < 2; k++) {
			// Get the dot between the segment and the car.
			temp.set(segments.get(i+1)).sub(segments.get(i));
			temp.rotate90(1).nor();
			float dot1 = temp.dot(temp2);
			
			int oldI = i;
			i += 2;
			if (i >= segments.size()) i = 0;
			
			// Get the dot between the NEXT segment and the car.
			temp.set(segments.get(i+1)).sub(segments.get(i));
			temp.rotate90(1).nor();
			float dot2 = temp.dot(temp2);
			
			// Interpolate between the two dot products.
			float maxLen = car.getLongestDist();
			if (maxLen > 20) maxLen = 20;
			float t = 1 - (car.getDistToSeg() / maxLen);
			if (t < 0) t = 0;
			else if (car.speedRatio() < 0.15f) t = -0.2f;
			t *= 0.85f;
			
			float tempDot = dot1 - (dot1 - dot2) * t;

			if (k == 0) {
				dot = tempDot;
			} else {
				if (dot > tempDot) goLeft = true;
			}
			
			temp2.rotate(5);
			i = oldI;
		}
			
		if (goLeft) {
			turnLeft();
		} else {
			turnRight();
		}
		accel();
		
		handleItem();
		return keys;
	}
	
	/** Returns a random boost used for the start of every race. */
	public static float randomStartBoost() {
		float val = rand.nextFloat() * 1.8f;
		return (val > 1) ? 1 : val;
	}
	
	private void handleItem() {
		if (!car.hasItem()) return;
		switch (car.getItem()) {
		case BOOSTER:
			if (car.speedRatio() < 0.3f)
				car.useItem();
			break;
		}
	}
	
	/**
	 * Methods for easily setting how the car should move.
	 * @param b If the car should do the action or not.
	 */
	private void accel() {
		keys[0] = true;
		keys[1] = false;
	}
	private void brake() {
		keys[0] = false;
		keys[1] = true;
	}
	private void turnLeft() { 
		keys[2] = true;
		keys[3] = false;
	}
	private void turnRight() {
		keys[2] = false;
		keys[3] = true;
	}
	private void stopTurning() {
		keys[2] = false;
		keys[3] = false;
	}
	
	private boolean turning() {
		return (keys[2] || keys[3]);
	}
	
	/** Slow down without actually breaking. */
	private void slowDown() {
		keys[0] = false;
		keys[1] = false;
	}
}
