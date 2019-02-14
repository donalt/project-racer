package com.racer;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.racer.item.Item;

/*
 * Draws a car that is movable in all directions, with input from the arrow keys. 
 */
public class Car {
	// Information about the frames in the sprite sheet.
	public static Texture TEXTURE;
	public static final int FRAMES = 16;
	public static final int FRAME_WIDTH = 50;
	public static final int FRAME_HEIGHT = 31;
	
	private static final float MAX_VEL = 0.35f;
	private static final float MAX_SPEED = -0.04f;
	private static final float MAX_BACKING_SPEED = 0.01f;
	private static final float TURN_SPEED = 0.03f;
	
	private static final float ACCELERATION = 0.00015f;
	private static final float BACKING_ACCELERATION = 0.0001f;
	
	private static final float FRICTION = 0.992f;
	private static final float BREAK_FRICTION = 0.986f;
	private static final float BACK_BREAK_FRICTION = 0.8f;
	
	// The amount of frames it takes to reach max turning speed.
	private static final int FRAMES_FOR_TURNING = 10;
	// The distance between the front and back wheels.
	private static final float WHEEL_DIST = 0.2f;
	public static final float RADIUS = 1.3f;
	public static final float RADIUS_SQRD = RADIUS * RADIUS;
	
	// Vectors that are used for temporary calculations instead of
	// creating new vectors in the functions every frame.
	private Vector2 temp;
	private Vector2 temp2;
	
	private Vector2 oldPos;
	private Vector2 pos;
	private Vector2 vel;
	private float speed;
	private float angle;
	private int turningFrame;
	private int framesAtMaxTurning;
	private float posAboveGround;
	
	private Decal car;
	private ArrayList<TextureRegion> frames;
	public Color color;
	
	private ArrayList<Vector2> walls1;
	private ArrayList<Vector2> walls2;
	private ArrayList<Vector2> segments;
	
	private Player player;
	public int rank;
	private int laps;
	private boolean cheatedLap;
	private boolean locked;
	private int framesAccelerated;
	
	private int oldSeg;
	private int curSeg;
	private float distToSeg;
	private float longestDist;
	
	private Item item;
	
	public Car(Color color, int spriteIndex){
		this.color = color;
		frames = new ArrayList<TextureRegion>();
		for (int i=0; i < FRAMES; i++) {
			frames.add(new TextureRegion(TEXTURE, FRAME_WIDTH*i, FRAME_HEIGHT*spriteIndex,
					FRAME_WIDTH, FRAME_HEIGHT));
		}
		
        car = Decal.newDecal(1, (float)FRAME_HEIGHT/FRAME_WIDTH, frames.get(0), true);
        car.setScale(1.8f);
        posAboveGround = 1.8f* car.getHeight() / 2;
        
        pos = new Vector2(5, 5);
        oldPos = new Vector2(pos);
        vel = new Vector2();
        temp = new Vector2();
        temp2 = new Vector2();
	}
	
	public static void loadTexture() {
		TEXTURE = new Texture(Gdx.files.internal("placeholderCar.png"));
	}
	
	public Vector3 getPosition() {
		return car.getPosition();
	}
	public Vector2 getPosition2() {
		return pos;
	}
	public float getAngle() {
		return angle;
	}
	public int getLaps() {
		return (cheatedLap) ? laps - 1 : laps;
	}
	public int getTrueLaps() {
		return laps;
	}
	
	public int getSeg() {
		return curSeg;
	}
	public float getDistToSeg() {
		return distToSeg;
	}
	public float getLongestDist() {
		return longestDist;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	/** Sets this car to its max speed. */
	public void setMaxSpeed() {
		speed = MAX_SPEED;
	}
	public float speedRatio() {
		return speed / MAX_SPEED;
	}
	
	public void setWalls(ArrayList<Vector2> w1, ArrayList<Vector2> w2, ArrayList<Vector2> segs) {
		walls1 = w1;
		walls2 = w2;
		segments = segs;
	}
	public void resetPos(float x, float y) {
		pos.set(x, y);
		oldPos.set(pos);
		
		vel.set(0, 0);
		speed = 0;
		angle = (float) -Math.PI/2;
		laps = 0;
		cheatedLap = false;
		oldSeg = 0;
		curSeg = 0;
		distToSeg = -y;
	}
	
	/**
	 * Locks the car in place so that it cannot move.
	 */
	public void lock() {
		locked = true;
	}
	/**
	 * "Unleash" the car. This is only used at the start of a race
	 * after the countdown. The car gets different starting speeds
	 * depending on how long it has accelerated for.
	 */
	public void unleash() {
		locked = false;
		float percent = framesAccelerated/60.0f;
		if (percent > 1) percent = 2 - percent;
		if (percent < 0) percent = 0;
		
		if (player.aiControlled()) {
			percent = AIcontroller.randomStartBoost();
		}
		
		speed = MAX_SPEED * percent;
	}
	
	/**
	 * Turns the car Decal towards the camera to act as a billboard. It also 
	 * calculates the angle between the two and chooses an appropriate animation frame.
	 * @param cam The camera to look at.
	 */
	public void lookAtCamera(Camera cam, Player p, boolean lookingBack) {
		car.lookAt(cam.position, cam.up);
		
		int frame;
		if (player == p && !p.finished()) {
			// Make the players car turn when turning even though the camera
			// technically is straight behind it.
			if (framesAtMaxTurning > 14) {
				frame = (turningFrame > 0) ? 3 : -3;
			}
			else if (framesAtMaxTurning >= 1) {
				frame = (turningFrame > 0) ? 2 : -2;
			}
			else if (turningFrame != 0) {
				frame = (turningFrame > 0) ? 1 : -1;
			}
			else frame = 0;
			if (vel.len2() < 0.003f) frame = 0;
			
			if (lookingBack) frame += FRAMES/2;
			
		}
		// Calculate correct frame if this car does not belong to this player.
		else {
			double pi = Math.PI;
			double frameAngleSize = 2*pi / FRAMES;
			double halfAngleSize = frameAngleSize / 2;
			// calculate the angle between car and camera.
			double a = Math.atan2(pos.y - cam.position.z, pos.x - cam.position.x) + pi;
			// factor in the current rotation of the car.
			a -= angle;
			a %= 2*pi;
			if (a < 0) a += 2*pi;
			
			// map the calculated angle into a frame number. HalfAngleSize is used
			// to make sure that angle 0 is in the "middle" of animation frame 0.
			if (a < halfAngleSize || a > 2*pi - halfAngleSize){
				frame = 0;
			}
			else {
				frame = (int)Math.floor((a + halfAngleSize) / frameAngleSize);
				// If the angle is exactly 2*PI frame will be equal to FRAMES.
				if (frame == FRAMES) frame = FRAMES - 1;
			}
		}
		
		if (frame < 0) frame += FRAMES;
		car.setTextureRegion(frames.get(frame));
	}
	
	/**
	 * Adds the car Decal into a DecalBatch.
	 */
	public void addToBatch(DecalBatch batch) {
		batch.add(car);
	}
	
	/*
	 * Updates the position of the car according to which buttons are held down.
	 */
	public void move(boolean up, boolean down, boolean left, boolean right){	
		if (up) {
			framesAccelerated++;
			speed -= ACCELERATION;
			if (speed < MAX_SPEED)
				speed = MAX_SPEED;
		} else {
			framesAccelerated = 0;
		}
		if (down) {
			speed += BACKING_ACCELERATION;
			if (speed > MAX_BACKING_SPEED)
				speed = MAX_BACKING_SPEED;
		}
		// Simulate turning the driving wheel by adding "acceleration" to
		// turning speed. We use ints for this since integer math is exact.
		if ((left && speed < 0) || (speed > 0 && right)) {
			turningFrame ++;
			if (turningFrame > FRAMES_FOR_TURNING){
				turningFrame = FRAMES_FOR_TURNING;
			}
		}
		if ((right && speed < 0) || (speed > 0 && left)) {
			turningFrame --;
			if (turningFrame < -FRAMES_FOR_TURNING) {
				turningFrame = -FRAMES_FOR_TURNING;
			}
		}
		
		if (Math.abs(turningFrame) == FRAMES_FOR_TURNING) {
			framesAtMaxTurning++;
		} else framesAtMaxTurning = 0;
		
		// Turn the wheels back if not turning.
		if (!left && !right) {
			if (turningFrame > 0) turningFrame --;
			else if (turningFrame < 0) turningFrame++;
		}
		
		// Slow down if not accelerating or reversing.
		if (!up && !down) {
			speed *= FRICTION;
		}
		// Break
		if (speed > 0 && up) {
			speed *= BACK_BREAK_FRICTION;
		} else if (speed < 0 && down) {
			speed *= BREAK_FRICTION;
		}
		
		// Lower max turning speed if velocity is low.
		double maxTurnSpeed = vel.len2() / 0.01 * TURN_SPEED;
		if (maxTurnSpeed > TURN_SPEED)
			maxTurnSpeed = TURN_SPEED;
		double angleChange = ((float)turningFrame / FRAMES_FOR_TURNING) * maxTurnSpeed;
		
		
		// Calculate front and back wheel positions.
		Vector2 fWheel = temp;
		Vector2 bWheel = temp2;
		fWheel.set((float)Math.cos(angle), (float)Math.sin(angle)).scl(WHEEL_DIST/2).add(pos);
		bWheel.set((float)Math.cos(angle), (float)Math.sin(angle)).scl(-WHEEL_DIST/2).add(pos);
		
		// Move the wheels forward in the car's direction. angleChange is added
		// to the front wheels since they are doing the turning.
		bWheel.add((float)Math.cos(angle)*speed, (float)Math.sin(angle)*speed);
		fWheel.add((float)Math.cos(angle+angleChange)*speed, (float)Math.sin(angle+angleChange)*speed);
		
		angle -= angleChange;
		
		// Scale down the velocity from previous frame.
		vel.scl(0.95f);
		if (vel.len2() > MAX_VEL) {
			vel.setLength2(MAX_VEL);
		}
		
		// Add this frames movement to the velocity.
		if (!locked) {
			vel.add(fWheel.add(bWheel).scl(0.5f).sub(pos));
		}
		
		// Add velocity to car and update position
		oldPos.set(pos);
		pos.add(vel);
		car.setPosition(pos.x, posAboveGround, pos.y);
		
		testForWallCollision();
	}
	
	/**
	 * Test this car for collision against every wall segment.
	 * It stops after it finds one collision.
	 */
	private void testForWallCollision() {
		Vector2 n1 = temp;
		Vector2 hitPoint = temp2;
		
		ArrayList<Vector2> list;
		double angleOffset;
		
		for (int w=0; w < 2; w++) {
			if (w == 0) {
				list = walls1;
				angleOffset = Math.PI/2;
			} else {
				list = walls2;
				angleOffset = -Math.PI/2;
			}
			n1.set(list.get(0));
			
			for (int i=1; i < list.size(); i++) {
				Vector2 n2 = list.get(i);
				if (Intersector.intersectSegments(n1, n2, oldPos, pos, hitPoint)) {
					// bounceA is technically the normal. angleOffset is used
					// here to get the correct side of the track segment.
					float bounceA = (float) (Math.atan2(n2.y-n1.y, n2.x-n1.x) + angleOffset);
					Vector2 wall = new Vector2(1, 0);
					wall.setAngleRad(bounceA).nor();
					
					// move the car just in front of the wall.
					pos.set(hitPoint.add((float)Math.cos(bounceA)*0.05f, (float)Math.sin(bounceA)*0.05f));
					
					// Lower the speed depending on which angle you hit the wall in.
					temp2.setAngleRad(angle).nor();
					float wallHitDot = wall.dot(temp2);
					speed *= (1 - Math.abs(wallHitDot)) * 0.85;
					
					// calculate the bounce using the reflection formula.
					float dot = vel.dot(wall);
					vel.sub(wall.scl(dot*2));
					break;
				}
				n1.set(n2);
			}
		}
	}
	
	/**
	 * Check if this car is colliding with another car.
	 * If there is, apply force to both.
	 * @param car The car to collide with.
	 */
	public void collideWith(Car car) {
		Vector2 carPos = car.getPosition2();
		float dx = carPos.x - pos.x;
		float dy = carPos.y - pos.y;
		float len2 = dx*dx + dy*dy;
		if (len2 <= RADIUS_SQRD) {
			// Get the total velocity of both cars and then push both
			// in opposite directions with half of that velocity.
			float a = temp.set(carPos).sub(pos).angleRad();
			float totalVel = vel.len() + car.vel.len();
			car.push((float) Math.cos(a)*totalVel/2, (float) Math.sin(a)*totalVel/2);
			push((float) Math.cos(a+Math.PI)*totalVel/2, (float) Math.sin(a+Math.PI)*totalVel/2);
		}
	}
	
	/**
	 * Push this car by a given force. It will also lose
	 * speed depending on the angle it's being pushed in.
	 */
	public void push(float x, float y) {
		temp.set(vel).nor();
		temp2.set(x, y).nor();
		float dot = temp.dot(temp2) + 1;
		speed *= (dot/2) * 0.8;
		
		vel.add(x, y);
	}
	
	/**
	 * Gives this car an item. If it already has one, nothing happens.
	 */
	public void giveItem(Item item) {
		if (hasItem()) return;
		this.item = item;
	}
	/**
	 * Use the item, if the car has one.
	 */
	public void useItem() {
		if (!hasItem()) return;
		item.activate();
		item = null;
	}
	public boolean hasItem() {
		return item != null;
	}
	public int getItemId() {
		return item.getId();
	}
	public Item.Type getItem() {
		return item.type();
	}
	
	/**
	 * Checks if the car is turned and going the wrong way.
	 */
	public boolean goingWrongWay() {
		// The dot product of the car and the segments normal is positive
		temp.set(segments.get(curSeg*2+1)).sub(segments.get(curSeg*2));
		temp.rotate90(1);
		temp2.setAngleRad(angle);
		return temp.dot(temp2) < 0;
	}
	
	/**
	 * Updates how many laps a car has done by checking
	 * its old and new segment index.
	 */
	public void updateLaps() {
		int totSegs = (segments.size()-1)/2;
		if (oldSeg == totSegs && curSeg == 0) {
			if (!cheatedLap) {
				laps++;
			} else cheatedLap = false;
		} else if (oldSeg == 0 && curSeg == totSegs){
			cheatedLap = true;
		}
	}
	
	/**
	 * Checks and updates which segment the car currently is in.
	 */
	public void updateSegment() {
		int i = curSeg * 2;
		int iChange = 2;
		int seg = i;
		oldSeg = curSeg;
		
		// This variable is needed to keep looking even though a segment change
		// has been found. This is needed since the car could possibly pass
		// through multiple segments in one frame.
		boolean intersectionFound = false;
		// The algorithm needs to look at the start segment and the one before it.
		boolean startDone = false;
		
		while (true) {
			if (i < 0) i = segments.size() + i;
			else if (i >= segments.size()) i = 0;
			
			if (Intersector.intersectSegments(oldPos, pos,
					           segments.get(i), segments.get(i+1), null)) {
				intersectionFound = true;
				seg = i;
			} else {
				// Even though this segment didn't intersect, the previous one did.
				if (intersectionFound) break;
				// Change direction and check the segment before this one next iteration.
				else if (!startDone){
					startDone = true;
					iChange = -2;
				} 
				// No new segment was found.
				else break;
			}
			i += iChange;
		}
		
		if (intersectionFound && !startDone) {
			seg +=2;
			if (seg >= segments.size()) seg = 0;
		}
		curSeg = seg/2;
		
		// Update the distance to segment.
		distToSeg = Intersector.distanceSegmentPoint(segments.get(seg), segments.get(seg+1), pos);
		
		if (oldSeg != curSeg || longestDist == 0) {
			longestDist = distToSeg;
		}
	}
}
