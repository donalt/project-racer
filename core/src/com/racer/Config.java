package com.racer;

import java.util.ArrayList;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;

/**
 * Config stores the configuration of controls, colors et cetera.
 */
public class Config {
	private int[][] controls;
	private final int[] colorInts;
	private ArrayList<Color> colors;
	private ArrayList<Integer> playerColors;
	private ArrayList<Integer> aiColors;
	private int trackId;
	
	public enum Key {
		ACCEL(0), BRAKE(1), LEFT(2), RIGHT(3), MIRROR(4), ITEM(5);
		int index;
		Key(int val) {
			index = val;
		}
	}
	
	public Config() {
		// Initialize the basic default controls.
		controls = new int[][]{
				{Keys.W, Keys.S, Keys.A, Keys.D, Keys.CONTROL_LEFT, Keys.SHIFT_LEFT},
				{Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT, Keys.CONTROL_RIGHT, Keys.SHIFT_RIGHT},
				{Keys.I, Keys.K, Keys.J, Keys.L, Keys.N, Keys.B},
				{Keys.NUMPAD_8, Keys.NUMPAD_5, Keys.NUMPAD_4, Keys.NUMPAD_6, Keys.NUMPAD_1, Keys.NUMPAD_0}
		};
		// Every color represents the color of each car in the sprite sheet.
		colorInts = new int[] {
			0x0000e8ff, 0xa600e8ff, 0xe7e800ff, 0xe80400ff,
			0x00b9e8ff, 0x00e804ff, 0xe85900ff, 0xff8ee8ff
		};
		
		colors = new ArrayList<Color>();
		for (int i=0; i < colorInts.length; i++) {
			colors.add(new Color(colorInts[i]));
		}
		
		playerColors = new ArrayList<Integer>();
		aiColors = new ArrayList<Integer>();
		// There has to be at least one player.
		addPlayer();
	}
	
	/**
	 * Returns the controls of a single player.
	 * @param playerId The player whose controls to return.
	 */
	public int[] getControls(int playerId) {
		return controls[playerId];
	}
	
	/**
	 * Returns the color id of a player.
	 */
	public int getColorId(int playerId) {
		return playerColors.get(playerId);
	}
	public int getColor(int colorId) {
		return colorInts[colorId];
	}
	public Color getColorInst(int playerId) {
		return colors.get(playerColors.get(playerId));
	}
	
	public int getKey(int playerId, Key key) {
		return controls[playerId][key.index];
	}
	
	/** Sets a car to the next available color. */
	public void setToNextColor(int playerId) {
		playerColors.set(playerId, getColor(playerColors.get(playerId), true));
	}
	/** Sets a car to the next available color going backwards. */
	public void setToPrevColor(int playerId) {
		playerColors.set(playerId, getColor(playerColors.get(playerId), false));
	}
	
	/** Returns the amount of players. */
	public int players() {
		return playerColors.size();
	}
	
	/** Returns the amount of players controlled by AI. */
	public int aiPlayers() {
		return colors.size() - playerColors.size();
	}
	
	/**
	 * Adds a new player. The players color will be chosen out of
	 * the ones that haven't been picked by other players yet.
	 */
	public void addPlayer() {
		playerColors.add(getColor(0, true));
	}
	
	/**
	 * Removes the player that was added last.
	 */
	public void removePlayer() {
		playerColors.remove(playerColors.size() - 1);
	}
	
	public void setTrack(int index) {
		trackId = index;
	}
	public int track() {
		return trackId;
	}
	
	/** 
	 * Searches for and returns an unoccupied color.
	 * @param startId The color id to start looking from.
	 * @param goRight Used to choose which direction to go when searching.
	 */
	private int getColor(int startId, boolean goRight) {
		int color = startId;
		while (true) {
			boolean inUse = false;
			for (int c : playerColors) {
				if (c == color) inUse = true;
			}
			if (inUse) {
				if (goRight) color++;
				else color--;
				
				if (color >= colorInts.length) color = 0;
				else if (color < 0) color = colorInts.length - 1;
			}
			else break;
		}
		return color;
	}
	
	/**
	 * Returns the next available colorId for an AI.
	 * @return
	 */
	public int aiColorId() {
		int id = 0;
		while (id < colors.size()) {
			if (!playerColors.contains(id) && !aiColors.contains(id)) {
				aiColors.add(id);
				break;
			}
			id++;
		}
		return id;
	}
	public void clearAiColors() {
		aiColors = new ArrayList<Integer>();
	}
	
	/**
	 * Changes one key for one player.
	 * @param playerId The player to change controls for.
	 * @param key The key to change.
	 * @param keyCode The key code to change key to.
	 */
	public void changeKey(int playerId, Key key, int keyCode) {
		controls[playerId][key.index] = keyCode;
	}
}
