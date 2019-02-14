package com.racer;

import java.util.ArrayList;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.racer.gui.HUD;
import com.racer.item.ItemBoxGroup;
import com.racer.item.ItemManager;
import com.racer.track.Track;
import com.racer.track.TrackLoader;

public class GameScreen implements Screen{
	
	private RacerGame game;
	private Config config;
	private Ground ground;
	private Background bg;
	private HUD hud;
	private Race race;
	private ItemManager items;
	private ArrayList<Car> cars;
	private ArrayList<Player> players;
	private ArrayList<ItemBoxGroup> boxes;
	
	//private boolean gameSetUp;
	
	public GameScreen(RacerGame game, Config config) {
		this.game = game;
		this.config = config;
		ground = new Ground();
		bg = new Background();
		items = new ItemManager(cars);
		
		cars = new ArrayList<Car>();
		for (int i=0; i < 8; i++) {
			Car c = new Car(new Color(config.getColor(i)), i);
			cars.add(c);
		}
	}
	
	public void setUpGame() {
		Track track = TrackLoader.loadTrack(config.track());
		
		players = new ArrayList<Player>();
		
		ground.generateTrack(track);
		boxes = ground.getBoxes();
		items.setUp(boxes);
		
		// Set up the normal players.
		for (int i=0; i < config.players(); i++) {
			Player p = new Player(false, ground, bg, cars, config.getColorId(i));
			players.add(p);
			
			p.setUpForSplitScreen(config.players(), i);
			p.setControls(config.getControls(i));
			p.getCar().setWalls(ground.getLeft(), ground.getRight(), ground.segments);
		}
		
		// Set up the AI players.
		for (int i=0; i < config.aiPlayers(); i++) {
			Player p = new Player(true, ground, bg, cars, config.aiColorId());
			players.add(p);
			p.getCar().setWalls(ground.getLeft(), ground.getRight(), ground.segments);
		}
		
		hud = new HUD(players);
		race = new Race(game, hud);
		hud.setRace(race, config.players());
		hud.createMap(ground.minimap, ground.minimapV);
		
		race.beginNewRace(players, track.laps, ground.getGoalLineWidth());
	}
	
	@Override
	public void render(float delta) {
		for (Player p : players) {
			p.updatePlayer();
			p.renderPlayerView();
		}
		
		// Collision test the cars with each other. Really sucks having to put this
		// here in this otherwise clean function but don't know where else to.
		int size = cars.size();
		for (int i=0; i < size; ++i) {
			Car iCar = cars.get(i);
			
			for (ItemBoxGroup box : boxes)
				box.hitTestCar(iCar);
			
			for (int j=i+1; j < size; ++j)
				iCar.collideWith(cars.get(j));
		}
		items.update();
		race.update();
		
		hud.render();
	}
	
	/** Called when this becomes the current screen. */
	@Override
	public void show() {
		setUpGame();
	}
	
	/** Called when this stops being the current screen. */
	@Override
	public void hide() {
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
	}

}
