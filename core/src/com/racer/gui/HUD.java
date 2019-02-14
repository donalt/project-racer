package com.racer.gui;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.racer.Car;
import com.racer.Player;
import com.racer.Race;

public class HUD {
	// How many frames it should take to show each player in the score board.
	public static final int TIME_PER_PLAYER = 30;
	
	private static final int ITEM_WIDTH = 20;
	private static final float RANK_X = 0.03f;
	private static final float RANK_Y = 0.92f;
	
	private SpriteBatch batch;
	public Race race;
	private BitmapFont font;
	private Texture items;
	private Minimap map;
	
	private Color rankColor1;
	private Color rankColor2;
	private Color temp;
	
	private ArrayList<Player> players;
	private int humanPlayers;
	private int[] lapsLeft;
	
	// pos saves results from calcPos()
	private float[] pos;
	
	private int screenW;
	private int screenH;
	
	
	public HUD(ArrayList<Player> players) {
		this.players = players;
		
		batch = new SpriteBatch();
		font = new BitmapFont();
		pos = new float[2];
		map = new Minimap(players);
		items = new Texture(Gdx.files.internal("items.png"));
		
		rankColor1 = new Color(1, 1, 0, 1);
		rankColor2 = new Color(0, 1, 1, 1);
		temp = new Color();
		
		screenW = Gdx.graphics.getWidth();
		screenH = Gdx.graphics.getHeight();
	}
	
	public void setRace(Race race, int humanPlayers) {
		this.race = race;
		this.humanPlayers = humanPlayers;
	}
	public void readyHUD() {
		map.calcMapPos(humanPlayers);
		lapsLeft = new int[players.size()*2];
	}
	
	public void createMap(Pixmap srcMap, float[] scale) {
		map.createMap(srcMap, scale);
	}
	public Minimap getMap() {
		return map;
	}
	
	/**
	 * Renders the HUD for all players.
	 */
	public void render() {
		// Draw on the whole screen.
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		if (race.finished()) {
			renderRaceResults();
			return;
		}
		
		batch.begin();
		
		String readyMsg = format321();
		
		for (int i=0; i < players.size(); i++) {
			Player p = players.get(i);
			
			Car car = p.getCar();
			
			if (p.finished() && p.realPlayer()) {
				showVictory(i, p.finalRank());
				continue;
			}
			
			// Don't draw any of the player specific HUD if it's an AI.
			if (p.aiControlled()) continue;
			
			// Show the players current rank.
			int rank = car.rank;
			if (rank < 4) font.setColor(rankColor1);
			else font.setColor(1, 1, 1, 1);
			calcPos(i, RANK_X, RANK_Y);
			setFontSize(45);
			font.draw(batch, formatRank(rank), pos[0], pos[1]);
			
			// Show laps left when player passes goal line.
			if (lapsLeft[i*2] > 0) {
				lapsLeft[i*2] -= 1;
				calcPos(i, 0.5f, 0.8f);
				font.setColor(1, 1, 0, 1);
				font.draw(batch, formatLaps(lapsLeft[i*2+1]), pos[0] - 130, pos[1]);
			}
			
			// Show "Wrong Way" when player is going opposite direction.
			if (car.goingWrongWay()) {
				calcPos(i, 0.5f, 0.8f);
				font.setColor(1, 0, 0, (int)Math.round(Math.sin(race.getElapsedTime()/4.0f)));
				font.draw(batch, "WRONG WAY", pos[0] - 140, pos[1]);
			}
			
			// Show 3, 2, 1 GO! before the race begins
			if (readyMsg != null) {
				int msgX = (race.timeBeforeStart() < 0) ? -65 : -22;
				setFontSize(80);
				calcPos(i, 0.5f, 0.85f);
				font.setColor(1, 1, 0, 1);
				font.draw(batch, readyMsg, pos[0] + msgX, pos[1]);
			}
			
			// Display the item that the player is holding.
			if (car.hasItem()) {
				calcPos(i, 0.05f, 0.05f);
				int w = ITEM_WIDTH;
				int s = 3; // Scale
				// The box that frames the item.
				batch.draw(items, pos[0], pos[1], w*s, w*s,
						0, 0, w, w, false, false);
				// The actual item.
				batch.draw(items, pos[0], pos[1], w*s, w*s,
						w * car.getItemId(), 0, w, w, false, false);
			}
		}
		
		map.drawMap(batch);
		
		// Draw the current race time.
		setFontSize(30);
		font.setColor(1, 1, 0.3f, 1);
		font.draw(batch, formatTime(race.getElapsedTime()),
				map.getPos()[0] + 50, map.getPos()[1] - 10);
		
		// SpriteBatch must be ended before using the ShapeRenderer in map.drawCars()
		batch.end();
		map.drawCars();
	}
	
	private void renderRaceResults() {
		ArrayList<Car> cars = race.getCarsInGoal();
		int[] times = race.getFinishTimes();
		
		batch.begin();
		setFontSize(37);
		
		int textY = 430;
		int leftX = 110;
		int rightX = screenW - 100;
		
		for (int i=0; i < cars.size(); i++) {
			if (TIME_PER_PLAYER * i > race.timeSinceFinish())
				break;
				
			Car c = cars.get(i);
			font.setColor(c.color);
			font.draw(batch, c.getPlayer().getName(), leftX, textY);
			font.draw(batch, formatTime(times[i]), rightX - 100, textY);
			textY -= 38;
		}
		
		if (TIME_PER_PLAYER * (cars.size()-1) < race.timeSinceFinish()) {
			setFontSize(26);
			font.setColor(1, 1, 1, 1);
			font.draw(batch, "Enter: Race Again\nESC : Back to Customization", leftX + 60, 80);
		}
		
		batch.end();
	}
	
	/**
	 * Displays how many laps are left for a player.
	 * @param playerId The player whose screen to show it on.
	 * @param laps How many laps left to show.
	 */
	public void showLapsLeft(int playerId, int laps) {
		// Display the laps left for 3 seconds.
		lapsLeft[playerId*2] = 180;
		lapsLeft[playerId*2+1] = laps;
	}
	
	/**
	 * Shows the screen for when a player has finished the race.
	 * @param playerId The id of the player.
	 * @param rank The players rank.
	 */
	public void showVictory(int playerId, int rank) {
		calcPos(playerId, 0.5f, 0.8f);
		setFontSize(160);
		float t = (float) (Math.sin(race.getElapsedTime()/3.0f) + 1) / 2;
		if (rank > 3) t = 0;
		font.setColor(temp.set(rankColor1).lerp(rankColor2, t));
		font.draw(batch, formatRank(rank), pos[0] - 125, pos[1]);
	}
	
	/**
	 * Returns a string representing a cars current rank.
	 * @param rank The rank to format.
	 */
	private String formatRank(int rank) {
		switch (rank) {
		case 1:
			return rank + "st";
		case 2:
			return rank + "nd";
		case 3:
			return rank + "rd";
		default:
			return rank + "th";
		}
	}
	
	/**
	 * Returns a string saying how many laps are left for a player.
	 * @param lapsLeft The amount of laps left to format.
	 */
	private String formatLaps(int lapsLeft) {
		switch (lapsLeft) {
		case 1:
			return "FINAL LAP";
		default:
			return lapsLeft + " LAPS LEFT";
		}
	}
	
	/**
	 * Returns a string in the form m'ss''x (minutes' seconds'' centiseconds).
	 * @param frames The time in frames.
	 */
	private String formatTime(int frames) {
		int minutes = (frames/3600);
		int seconds = (frames/60);
		int hundredths = ((int)(frames/6.0f)) - seconds*10;
		seconds %= 60;
		
		String sPad = "";
		if (seconds < 10) sPad = "0";
		
		return minutes + "'" + sPad + seconds + "''" + hundredths;
	}
	
	private String format321() {
		int time = race.timeBeforeStart();
		if (time < -60) return null;
		if (time < 0) return "GO!";
		if (time < 60) return "1";
		if (time < 120) return "2";
		if (time < 180) return "3";
		return null;
	}
	
	/**
	 * Calculates the position of a HUD element during split-screen.
	 * The position is stored in the pos field.
	 * @param index The index of the player.
	 * @param x The positioning of the element in percent.
	 * @param y The positioning of the element in percent.
	 */
	private void calcPos(int index, float x, float y) {
		// revi is the reverse index. It is needed since text is drawn
		// with the y position going from bottom to top, while player 1
		// should be the one at the top.
		int revi = (humanPlayers - 1) - index;
		switch (humanPlayers) {
		case 1:
			pos[0] = x * screenW;
			pos[1] = y * screenH;
			break;
		case 2:
			pos[0] = x * screenW;
			pos[1] = (revi + y) * screenH/2;
			break;
		case 3:
		case 4:
			pos[0] = (index%2 + x) * screenW/2;
			pos[1] = (1 - (index/2) + y) * screenH/2;
		}
	}
	
	private void setFontSize(int px) {
		// The default font has size 15px.
		font.getData().setScale(px/15.0f);
	}
}
