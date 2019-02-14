package com.racer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.racer.gui.MenuScreen;
import com.racer.track.TrackLoader;

public class RacerGame extends Game {
	private GameScreen game;
	private MenuScreen menu;
	private Config config;
	
	@Override
	public void create () {
		Car.loadTexture();
		config = new Config();
		menu = new MenuScreen(this, config);
		game = new GameScreen(this, config);
		
		TrackLoader.loadTracks();
		setScreen(menu);
	}
	
	/**
	 * Switches between the game and menu.
	 */
	public void switchScreen() {
		if (getScreen() == game) {
			setScreen(menu);
		} else {
			setScreen(game);
		}
	}
	
	public void quitGame() {
		Gdx.app.exit();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
		// Render the current set screen
		super.render();
	}
}
