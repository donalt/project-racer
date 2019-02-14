package com.racer.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.racer.Car;
import com.racer.Config;
import com.racer.Config.Key;
import com.racer.RacerGame;
import com.racer.track.TrackLoader;

public class MenuScreen implements Screen {
	private Input input;
	private AssignInput key;
	private SpriteBatch batch;
	private ShapeRenderer sr;
	private BitmapFont font;
	private Color tempColor;
	private Texture carTexture;
	
	private int screenW;
	private int screenH;
	
	private RacerGame game;
	private Config config;
	
	private enum Menu {
		MAIN, PLAYER
	}
	private Menu currentMenu;
	private int timeInMenu;
	
	// Main menu specific
	private enum MainButton {
		START, QUIT
	}
	private MainButton mainButton;
	private Color mainTextC;
	private Color mainTextSelectC;
	private Texture title;
	
	// Player menu specific
	private static final int BOTTOM_HEIGHT = 80;
	private String[] keyActions;
	private int playerChanging;
	private int currKey;
	protected int pressedKey;
	
	// The dimensions of the track choosing window.
	private static final int TW_WIDTH = 300;
	private static final int TW_HEIGHT = 200;
	private boolean choosingTrack;
	private int curTrack;
	
	// An InputProcessor used for controls assignment.
	private class AssignInput implements InputProcessor {
		private int pressedKey;
		private boolean keyWasPressed;
		public Key[] keys;
		
		public AssignInput() {
			keys = Config.Key.values();
		}
		
		@Override
		public boolean keyDown(int keyCode) {
			keyWasPressed = true;
			pressedKey = keyCode;
			return false;
		}
		public boolean wasPressed() {
			return keyWasPressed;
		}
		public int getKeyCode() {
			keyWasPressed = false;
			return pressedKey;
		}
		public void reset() {
			keyWasPressed = false;
		}

		// We are forced to "implement" all the methods of InputProcessor.
		public boolean keyUp(int keycode) {return false;}
		public boolean keyTyped(char character) {return false;}
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {return false;}
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {return false;}
		public boolean touchDragged(int screenX, int screenY, int pointer) {return false;}
		public boolean mouseMoved(int screenX, int screenY) {return false;}
		public boolean scrolled(int amount) {return false;}
	}
	
	public MenuScreen(RacerGame game, Config config) {
		this.game = game;
		this.config = config;
		
		title = new Texture(Gdx.files.internal("title.png"));
		input = Gdx.input;
		key = new AssignInput();
		
		batch = new SpriteBatch();
		sr = new ShapeRenderer();
		font = new BitmapFont();
		tempColor = new Color();
		carTexture = Car.TEXTURE;
		
		screenW = Gdx.graphics.getWidth();
		screenH = Gdx.graphics.getHeight();
		
		//mainTextC = new Color(0, 0.91f, 0.5f, 1);
		//mainTextSelectC = new Color(0, 0.2f, 0, 1);
		
		mainTextC = new Color(0, 0.5f, 0.9f, 1);
		mainTextSelectC = new Color(0, 0.15f, 0.38f, 1);
		
		keyActions = new String[]{
			"Accelerate:   ",
			"Brake:          ",
			"Steer Left:    ",
			"Steer Right: ",
			"Look Back: ",
			"Use Item:    "
		};
		
		currentMenu = Menu.MAIN;
	}
	
	/**
	 * Switches menu to another one while still keeping the game on this screen.
	 * @param menu The menu to switch to.
	 */
	private void switchMenu(Menu menu) {
		switch (menu) {
		case MAIN:
			mainButton = MainButton.START;
			break;
		case PLAYER:
			playerChanging = -1;
			choosingTrack = false;
			curTrack = 1;
			break;
		}
		
		currentMenu = menu;
		timeInMenu = 0;
	}
	
	/**
	 * Go out of the menu into the actual game.
	 */
	private void switchToGame() {
		game.switchScreen();
	}
	
	@Override
	public void render(float delta) {
		timeInMenu++;
		batch.begin();
		
		switch (currentMenu) {
		case MAIN:
			renderMain();
			break;
		case PLAYER:
			renderPlayerMenu();
			break;
		}
		
		batch.end();
	}
	
	private void renderMain() {
		// Select the other button when the player presses up or down.
		if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W) ||
			input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
			if (mainButton == MainButton.START) {
				mainButton = MainButton.QUIT;
			} else {
				mainButton = MainButton.START;
			}
		}
		
		// Draw the title graphic
		int tWidth = title.getWidth()*2;
		int tHeight = title.getHeight()*2;
		batch.draw(title, (screenW - tWidth)/2, (screenH - tHeight)/2 + 130, tWidth, tHeight);
		
		Color sColor;
		Color qColor;
		float t = (float) Math.abs(Math.sin(timeInMenu/5.0f));
		tempColor.set(mainTextC).lerp(mainTextSelectC, t);
		// Set correct colors depending on which option is selected.
		if (mainButton == MainButton.START) {
			sColor = tempColor;
			qColor = mainTextC;
		} else {
			sColor = mainTextC;
			qColor = tempColor;
		}
		
		setFontSize(40);
		font.setColor(sColor);
		font.draw(batch, "Play Game", screenW/2 - 90, screenH/2);
		font.setColor(qColor);
		font.draw(batch, "Quit", screenW/2 - 90, screenH/2 - 70);
		
		if (input.isKeyJustPressed(Keys.ENTER)) {
			switch (mainButton) {
			case START:
				switchMenu(Menu.PLAYER);
				break;
			case QUIT:
				game.quitGame();
			}
		}
	}
	
	/**
	 * This is the menu where you select the amount of players, controls and colors.
	 */
	private void renderPlayerMenu() {
		// Go back to main menu with ESC. Alternatively stop choosing track.
		if (input.isKeyJustPressed(Keys.ESCAPE)) {
			if (choosingTrack) choosingTrack = false;
			else switchMenu(Menu.MAIN);
		}
		
		int moduleH = (screenH-BOTTOM_HEIGHT)/4;
		int topY = screenH;
		int drawW = Car.FRAME_WIDTH * 2;
		int drawH = Car.FRAME_HEIGHT * 2;
		int carX = screenW - drawW - 30;
		// Change frame of car every 18 frames.
		int carFrame = (timeInMenu/12) % Car.FRAMES;
		
		setFontSize(15);
		
		for (int i=0; i < config.players(); i++) {
			font.setColor(config.getColorInst(i));
			font.draw(batch, "Player " + (i+1), 20, topY - moduleH/2);
			// Draw the preview car with correct color.
			batch.draw(carTexture, carX,topY - (moduleH + drawH)/2, drawW,drawH,
					carFrame * Car.FRAME_WIDTH, config.getColorId(i) * Car.FRAME_HEIGHT,
					Car.FRAME_WIDTH, Car.FRAME_HEIGHT,
					false, false);
			
			// Let the player be able to switch color of their car.
			if (!changingKeys() && !choosingTrack) {
				if (input.isKeyJustPressed(config.getKey(i, Config.Key.LEFT))) {
					config.setToPrevColor(i);
				} else if (input.isKeyJustPressed(config.getKey(i, Config.Key.RIGHT))) {
					config.setToNextColor(i);
				}
			}
			
			// Print the controls for this player.
			int contX = (int) (screenW*0.2f);
			int contY = topY - 20;
			int row = 0;
			font.setColor(mainTextC);
			Key[] keys = key.keys;
			for (int k=0; k < keys.length; k++) {
				// Change to a flashing color if this is the key being changed.
				if (playerChanging == i && currKey == k){
					float t = (float) Math.abs(Math.sin(timeInMenu/5.0f));
					tempColor.set(mainTextC).lerp(mainTextSelectC, t);
					font.setColor(tempColor);
				} else
					font.setColor(mainTextC);
				
				font.draw(batch, keyActions[k] + Keys.toString(config.getKey(i, keys[k])), contX, contY -20*row);
				if (++row > 3) {
					row = 0;
					contX += 200;
				}
			}
			// Darken this player module if someone else is assigning controls.
			if (changingKeys() && playerChanging != i) {
				darkenArea(0, topY, screenW, -moduleH);
			}
			
			topY -= moduleH;
		} // END OF PLAYER LOOP
		
		// Some simple control information
		font.setColor(0.7f, 0.7f, 0.7f, 1);
		font.draw(batch, "1/2/3/4: Change controls", 20, 60);
		font.draw(batch, "LEFT / RIGHT: Change car color", 20, 40);
		font.draw(batch, "ESCAPE: Go back     BACKSPACE: Remove player", 20, 20);
		font.setColor(1f, 0.84f, 0, 1);
		font.draw(batch, "ENTER: Choose track", screenW - 170, 20);
		
		// Render and update the track menu.
		if (choosingTrack) {
			renderTrackMenu();
		}
		
		// We check first that we're not changing keys because SPACE,
		// BACKSPACE and ENTER should all be valid controls for assignment.
		if (!changingKeys() && !choosingTrack){
			// Make it possible to add more players if less than 4.
			if (config.players() < 4) {
				font.setColor(1, 1, 1, (float) Math.abs(Math.sin(timeInMenu/10.0f)));
				font.draw(batch, "Press SPACE to join", screenW/2 - 68, topY - (moduleH - 15)/2);
				
				if (input.isKeyJustPressed(Keys.SPACE)) {
					config.addPlayer();
				}
			}
			// Make it possible to remove players until there is one remaining.
			if (config.players() > 1) {
				if (input.isKeyJustPressed(Keys.BACKSPACE)) {
					config.removePlayer();
				}
			}
			if (input.isKeyJustPressed(Keys.ENTER)) {
				choosingTrack = true;
			}
		}
		
		// Customize controls.
		if (changingKeys()) {
			if (key.wasPressed()) {
				config.changeKey(playerChanging, key.keys[currKey], key.getKeyCode());
				if (++currKey >= key.keys.length) {
					playerChanging = -1;
				}
			}
		} 
		else if (!choosingTrack){
			int p = config.players();
			if (input.isKeyJustPressed(Keys.NUM_1)) {
				assignControls(0);
			} else if (p > 1 && input.isKeyJustPressed(Keys.NUM_2)) {
				assignControls(1);
			} else if (p > 2 && input.isKeyJustPressed(Keys.NUM_3)) {
				assignControls(2);
			} else if (p > 3 && input.isKeyJustPressed(Keys.NUM_4)) {
				assignControls(3);
			}
		}
	}
	
	private void renderTrackMenu() {
		darkenArea(0, 0, screenW, screenH);
		batch.end();
		
		int wx = (screenW - TW_WIDTH) / 2;
		int wy = (screenH - TW_HEIGHT) / 2;
		int hWidth = TW_WIDTH/2;
		int textX = wx + hWidth - 120;
		
		sr.begin(ShapeType.Filled);
		sr.setColor(1, 1, 1, 1);
		sr.rect(wx - 3, wy - 3, TW_WIDTH + 6, TW_HEIGHT + 6);
		sr.setColor(0, 0, 0, 1);
		sr.rect(wx, wy, TW_WIDTH, TW_HEIGHT);
		sr.end();
		
		String tName = TrackLoader.trackName(curTrack - 1);
		int tracks = TrackLoader.numTracks();
		String s = "";
		if (tracks != 1) s = "s";
		
		batch.begin();
		font.setColor(0.8f, 0.8f, 0, 1);
		font.draw(batch, tracks + " track" + s + " available", textX, wy + TW_HEIGHT - 40);
		font.draw(batch, curTrack+"/"+tracks+": " + tName, textX, wy + TW_HEIGHT/2 + 20);
		
		font.setColor(0.9f, 0.9f, 0.9f, 1);
		font.draw(batch, "LEFT / RIGHT: Choose track\n"
				+ "ESC: Cancel     ENTER: Start race", textX, wy + 60);
		
		// Change the selected track.
		if (input.isKeyJustPressed(Keys.LEFT)) {
			curTrack--;
		} else if (input.isKeyJustPressed(Keys.RIGHT)) {
			curTrack++;
		}
		if (curTrack > tracks) curTrack = 1;
		else if (curTrack < 1) curTrack = tracks;
		
		// Choose the selected track and go on to the game.
		if (input.isKeyJustPressed(Keys.ENTER)) {
			config.setTrack(curTrack - 1);
			switchToGame();
		}
	}
	
	private void setFontSize(int px) {
		// The default font has size 15px.
		font.getData().setScale(px/15.0f);
	}
	
	/**
	 * Returns true if a player is currently changing keys.
	 */
	private boolean changingKeys() {
		return playerChanging > -1;
	}
	
	/**
	 * Start assigning controls for a player.
	 */
	private void assignControls(int playerId) {
		playerChanging = playerId;
		currKey = 0;
		key.reset();
	}
	
	private void darkenArea(float x, float y, float width, float height) {
		batch.end();
		Gdx.gl20.glEnable(GL20.GL_BLEND);
		
		sr.begin(ShapeType.Filled);
		sr.setColor(0,0, 0, 0.7f);
		sr.rect(x, y, width, height);
		sr.end();
		
		Gdx.gl20.glDisable(GL20.GL_BLEND);
		batch.begin();
	}
	
	/** Called when this becomes the current screen. */
	@Override
	public void show() {
		switchMenu(currentMenu);
		input.setInputProcessor(key);
		config.clearAiColors();
	}
	
	/** Called when this stops being the current screen. */
	@Override
	public void hide() {
		input.setInputProcessor(null);
	}
	
	public void resize(int width, int height) {}
	public void pause() {}
	public void resume() {}
	public void dispose() {}
}
