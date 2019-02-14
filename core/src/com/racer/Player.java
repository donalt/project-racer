package com.racer;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Vector3;

/**
 * The Player class keeps track of a player. It's primary purpose is to render
 * everything from the players camera. This allows for easy split-screen game play.
 */
public class Player {
	private Car car;
	private DecalBatch decalBatch;
	private PerspectiveCamera cam;
	private float camHeight = 2;
	private float camDist = 3;
	private double camAngle;
	
	private Ground ground;
	private Background bg;
	private ArrayList<Car> cars;
	private int rank;
	
	// Each player has their separate keys for movement.
	private int upKey;
	private int downKey;
	private int leftKey;
	private int rightKey;
	private int mirrorKey;
	private int itemKey;
	
	// Booleans to keep track if player is holding down a key.
	private boolean up;
	private boolean down;
	private boolean left;
	private boolean right;
	private boolean mirror;
	private boolean item;
	
	// Fields used for split screen
	private int players;
	private int id;
	
	// Vector that can be used as a temporary vector
	private Vector3 vec;
	
	private AIcontroller ai;
	private boolean possessed;
	
	/**
	 * Create a new player.
	 * @param ground A reference to the ground.
	 * @param sky A reference to the sky.
	 * @param cars A reference to all of the cars.
	 * @param playerCar The index of the car that the player should control.
	 */
	public Player(boolean isAI, Ground ground, Background bg, ArrayList<Car> cars, int playerCar) {
		this.ground = ground;
		this.cars = cars;
		this.bg = bg;
		car = cars.get(playerCar);
		car.setPlayer(this);
		
		vec = new Vector3();
		
		if (isAI) {
			ai = new AIcontroller(car, ground.segments);
			return; // The AI won't need an actual graphical view using a camera.
		}
		
		cam = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 4, -20);
		cam.near = 0.3f;
		cam.far = 200f;
		cam.update();
		
		decalBatch = new DecalBatch(new CameraGroupStrategy(cam));
	}
	
	public boolean aiControlled() {
		return ai != null;
	}
	/** 
	 * Returns true if this is or was a real player (before it got possessed).
	 */
	public boolean realPlayer() {
		return (!aiControlled() || possessed);
	}
	
	public Car getCar() {
		return car;
	}
	public int getId() {
		return id;
	}
	public String getName() {
		if (!realPlayer()) return "CPU";
		return "Player " + id;
	}
	
	public boolean finished() {
		return rank > 0;
	}
	public int finalRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	public void reset() {
		rank = 0;
		camAngle = 0;
	}
	
	/**
	 * Possess this player by making it controlled by the AI.
	 */
	public void possess() {
		ai = new AIcontroller(car, ground.segments);
		possessed = true;
	}
	public void removePossession() {
		if (!possessed) return;
		ai = null;
		possessed = false;
	}
	
	/**
	 * Returns the angle of the camera. It is different depending on
	 * if the players has finished the race or not.
	 */
	private float cameraAngle() {
		float angle = (float)camAngle;
		if (!finished()) {
			angle += car.getAngle();
		}
		return angle;
	}
	
	/**
	 * Centers the player camera right behind the car.
	 */
	private void centerCamera() {
		// If the mirror key is down, add pi to the angle so that you look backwards instead.
		if (mirror) camAngle = Math.PI;
		else if (!finished()) camAngle = 0;
		else camAngle += 0.03f;
		
		float angle = cameraAngle();
		cam.position.set((float)Math.cos(angle)*camDist, camHeight,
		                 (float)Math.sin(angle)*camDist);
		cam.position.add(car.getPosition());
		// There's a high chance that the camera rotates in other directions during
		// the calculations, therefore we need to reset the UP vector.
		cam.up.set(0, 1, 0);
		cam.lookAt(vec.set(car.getPosition()).add(0, 0.5f, 0));
		cam.update();
	}
	
	/**
	 * Sets up the player camera for split screen.
	 * @param players The amount players to display. Maximum of 4 players.
	 * @param playerId This players id.
	 */
	public void setUpForSplitScreen(int players, int playerId) {
		this.players = players;
		id = playerId;
	}
	
	/**
	 * Set the region of the screen that the camera should render on.
	 */
	private void setScreenRegion() {
		int height = Gdx.graphics.getHeight();
		int width = Gdx.graphics.getWidth();
		int posId = (players - 1) - id;
		
		switch (players) {
		case 1:
			Gdx.gl.glViewport(0, 0, width, height);
			cam.viewportWidth = width;
			cam.viewportHeight = height;
			break;
		case 2:
			Gdx.gl.glViewport(0, height/2 * posId, width, height/2);
			cam.viewportWidth = width;
			cam.viewportHeight = height / 2;
			break;
		case 3:
		case 4:
			Gdx.gl.glViewport(width/2 * (id % 2), height/2 * (1 - (id/2)), width/2, height/2);
			cam.viewportWidth = width / 2;
			cam.viewportHeight = height / 2;
		}
	}
	
	/**
	 * Sets the controls for this player.
	 */
	public void setControls(int[] controls) {
		upKey = controls[0];
		downKey = controls[1];
		leftKey = controls[2];
		rightKey = controls[3];
		mirrorKey = controls[4];
		itemKey = controls[5];
	}
	/*
	 * Update the key states for this player.
	 */
	private void handleInput(){
		Input input = Gdx.input;
		up = input.isKeyPressed(upKey);
		down = input.isKeyPressed(downKey);
		left = input.isKeyPressed(leftKey);
		right = input.isKeyPressed(rightKey);
		mirror = input.isKeyPressed(mirrorKey);
		item = input.isKeyPressed(itemKey);
	}
	
	public void updatePlayer() {
		if (aiControlled()) {
			boolean[] k = ai.useBrain();
			car.move(k[0], k[1], k[2], k[3]);
		} else {
			handleInput();
			car.move(up, down, left, right);
			if (item) car.useItem();
		}
	}
	
	/**
	 * Renders this specific players camera. All of the Decals will
	 * turn towards the camera before being rendered.
	 */
	public void renderPlayerView() {
		if (!realPlayer()) return;
		
		setScreenRegion();
		centerCamera();
		
		bg.render(cameraAngle());
		ground.render(cam);
		
		decalBatch.add(ground.goal);
		for (Car c : cars) {
			c.lookAtCamera(cam, this, mirror);
			c.addToBatch(decalBatch);
		}
        decalBatch.flush();
	}
}
