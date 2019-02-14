package com.racer;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.racer.gui.HUD;

/**
 * The Race class keeps track of and handles everything that has to do with the
 * current race. Things like time, rankings and laps.
 */
public class Race {
	public static final int START_TIME = 200;
	
	private RacerGame game;
	private HUD hud;
	private ArrayList<Player> players;
	private ArrayList<Car> cars;
	private ArrayList<Car> carsInGoal;
	private int[] finishTimes;
	private int laps;
	private int time;
	private boolean started;
	private boolean finished;
	
	private int humanPlayers;
	private int playersFinished;
	private int lastFinishTime;
	private float goalLineWidth;
	
	public Race(RacerGame game, HUD hud) {
		this.game = game;
		this.hud = hud;
	}
	
	/**
	 * Starts a new race by resetting positions and ranks.
	 */
	public void beginNewRace(ArrayList<Player> players, int laps, float goalLineWidth) {
		this.players = players;
		this.laps = laps;
		
		carsInGoal = new ArrayList<Car>();
		cars = new ArrayList<Car>();
		finishTimes = new int[players.size()]; 
		started = false;
		finished = false;
		humanPlayers = playersFinished = 0;
		this.goalLineWidth = goalLineWidth;
		
		int carsPerRow = 4;
		float carPadX = 2.5f;
		float carPadY = 0.7f;
		float leftX = (goalLineWidth + carPadX*(carsPerRow-0.5f))/2;
		
		float carX = leftX;
		float carY = 8f;
		for (int i=players.size()-1; i >= 0; i--) {
			Player p = players.get(i);
			if (p.realPlayer()) {
				p.removePossession();
				humanPlayers++;
			}
			
			Car c = p.getCar();
			
			p.reset();
			cars.add(c);
			c.resetPos(carX, carY);
			c.lock();
			carX -= carPadX;
			carY -= carPadY;
			if (i == carsPerRow) {
				carX = leftX - carPadX/2;
			}
		}
		hud.readyHUD();
		time = 0;
	}
	
	private void startRace() {
		for (int i=0; i < cars.size(); i++) {
			cars.get(i).unleash();
		}
		started = true;
		time = 0;
	}
	
	public boolean finished() {
		return finished;
	}
	
	/**
	 * Returns the amount of frames since the race started.
	 */
	public int getElapsedTime() {
		if (!started) return 0;
		return time;
	}
	
	/**
	 * Returns the amount of frames before the race starts.
	 * Returns a negative number if race has started.
	 */
	public int timeBeforeStart() {
		if (started) return -time;
		return START_TIME - time;
	}
	
	public int timeSinceFinish() {
		return time - lastFinishTime;
	}
	
	public ArrayList<Car> getCarsInGoal() {
		return carsInGoal;
	}
	public int[] getFinishTimes() {
		return finishTimes;
	}
	
	/**
	 * Update the current ongoing race by checking
	 * for lap, segment and rank changes.
	 */
	public void update() {
		for (int i=0; i < cars.size(); i++) {
			Car c = cars.get(i);
			Player p = c.getPlayer();
			int oldLaps = c.getTrueLaps();
			c.updateSegment();
			c.updateLaps();
			
			// Cars that have finished don't need the rest of the loop.
			if (p.finished()) continue;
			
			// If car has finished race.
			int curLaps = c.getTrueLaps();
			if (curLaps == laps) {
				finishTimes[carsInGoal.size()] = time;
				carsInGoal.add(c);
				p.setRank(carsInGoal.size());
				if (!p.aiControlled()) {
					lastFinishTime = time;
					p.possess();
					playersFinished++;
				}
				continue;
			}
			
			if (curLaps > oldLaps && !c.getPlayer().aiControlled()) {
				hud.showLapsLeft(c.getPlayer().getId(), laps - curLaps);
			}
			improveRank(i);
		}
		// sadly another loop is needed to update the cars rankings
		for (int i=0; i < cars.size(); i++) {
			cars.get(i).rank = i + 1;
		}
		
		time++;
		if (!started) {
			if (time > START_TIME) {
				startRace();
			} 
		}
		
		// When all human players have finished the race should end.
		if (playersFinished == humanPlayers) {
			if (!finished) {
				// Finish the race 3 seconds after all players have entered goal.
				if (time - 180 > lastFinishTime) {
					finished = true;
					lastFinishTime = time;
				}
			} else if (HUD.TIME_PER_PLAYER * (carsInGoal.size()-1) < timeSinceFinish()) {
				if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
					game.switchScreen();
				} else if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
					beginNewRace(players, laps, goalLineWidth);
				}
			}
		}
	}
	
	/**
	 * Check if a car has caught up to the car in front of it. If it has it
	 * will switch their position in the car list.
	 * @param car The index of the car to check.
	 */
	private void improveRank(int car) {
		if (car == 0) return;
		Car back = cars.get(car);
		Car front = cars.get(car - 1);
		
		// compare laps
		if (back.getLaps() > front.getLaps()) {
			switchCars(car, car - 1);
			return;
		} else if (back.getLaps() < front.getLaps()) {
			return;
		}
		
		// compare segments
		if (back.getSeg() > front.getSeg()) {
			switchCars(car, car - 1);
			return;
		} else if (back.getSeg() < front.getSeg()) {
			return;
		}
		
		// compare distance to end of segment, here a low number is better!
		if (back.getDistToSeg() < front.getDistToSeg()) {
			switchCars(car, car - 1);
		}
	}
	
	/**
	 * Switch two cars in the list. This will end up switching their ranks.
	 * @param c1 Index of the first car.
	 * @param c2 Index of the second car.
	 */
	private void switchCars(int c1, int c2) {		
		Car temp = cars.get(c1);
		cars.set(c1, cars.get(c2));
		cars.set(c2, temp);
	}
}
