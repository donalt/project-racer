package com.racer.gui;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.racer.Car;
import com.racer.Player;

public class Minimap {
	private ShapeRenderer sr;
	private Texture mapTexture;
	private ShaderProgram shader;
	private Color color;
	
	private ArrayList<Player> players;
	private int[] pos;
	private float[] offset;
	private float scale;
	
	private static int GOAL_BORDER = 2;
	private static int EXTRA_GOAL_W = 12;
	private float goalWidth;
	
	private int screenW;
	private int screenH;
	
	public Minimap(ArrayList<Player> p) {
		sr = new ShapeRenderer();
		color = new Color();
		
		players = p;
		pos = new int[2];
		offset = new float[2];
		
		screenW = Gdx.graphics.getWidth();
		screenH = Gdx.graphics.getHeight();
		
		setupShader();
	}
	
	public Texture getMap() {
		return mapTexture;
	}
	public int size() {
		return mapTexture.getWidth();
	}
	public int[] getPos() {
		return pos;
	}
	
	/**
	 * Creates the minimap of a track.
	 * @param w1 The source map to scale down and reposition.
	 */
	public void createMap(Pixmap srcMap, float[] scale) {
		if (mapTexture != null) mapTexture.dispose();
		
		mapTexture = new Texture(srcMap);
		this.scale = scale[0];
		offset[0] = scale[1];
		offset[1] = scale[2];
		goalWidth = scale[3];
	}
	
	/**
	 * Draws all of the cars in their current positions on the minimap.
	 */
	public void drawCars() {
		sr.begin(ShapeType.Filled);
		
		// draw the goal
		float goalX = offset[0] + pos[0] - goalWidth+1 - EXTRA_GOAL_W/2;
		float goalY = offset[1] + pos[1];
		sr.setColor(0.1f, 0.1f, 0.1f, 1);
		sr.rect(goalX - GOAL_BORDER, goalY - GOAL_BORDER,
				goalWidth + EXTRA_GOAL_W + GOAL_BORDER*2, 5 + GOAL_BORDER*2);
		sr.setColor(0.96f, 0.7f, 0, 1);
		sr.rect(goalX, goalY, goalWidth + EXTRA_GOAL_W, 5);
		
		for (Player p : players) {
			Car c = p.getCar();
			Vector3 carPos = c.getPosition();
			
			float xPos = carPos.x * -scale + offset[0] + pos[0];
			float yPos = carPos.z *  scale + offset[1] + pos[1];
			
			// draw the outer circle, 70% darker
			sr.setColor(color.set(c.color).sub(0.7f, 0.7f, 0.7f, 0));
			sr.circle(xPos, yPos, 6, 8);
			// draw the inner circle with original color but smaller
			sr.setColor(c.color);
			sr.circle(xPos, yPos, 3.7f, 8);
		}
		sr.end();
	}
	
	/**
	 * Draws the minimap on the screen.
	 * @param batch The SpriteBatch to draw on.
	 */
	public void drawMap(SpriteBatch batch) {
		batch.setShader(shader);
		batch.setColor(1, 1, 1, 0.8f);
		batch.draw(mapTexture, pos[0], pos[1]);
		batch.setColor(1, 1, 1, 1);
		batch.setShader(null);
	}
	
	/**
	 * Calculates the position of the map and stores it in pos.
	 */
	public void calcMapPos(int humanPlayers) {
		int s = mapTexture.getWidth();
		
		switch (humanPlayers) {
		case 1:
			// bottom right
			pos[0] = screenW - s - 10;
			pos[1] = 50;
			break;
		case 2:
			// middle right
			pos[0] = screenW - s - 10;
			pos[1] = (screenH - s) / 2;
			break;
		case 3:
			// in the empty bottom-right square
			pos[0] = screenW - screenW/4 - s/2;
			pos[1] = screenH/4 - s/2 + 15;
			break;
		case 4:
			// in the middle of screen
			pos[0] = (screenW - s) / 2;
			pos[1] = (screenH - s) / 2;
			break;
		}
	}
	
	/**
	 * Sets up the shader for drawing the minimap in a single color.
	 */
	private void setupShader() {
	// This shader is almost a copy of the default shader of spritebatch:
	// https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/g2d/SpriteBatch.java
	// The only thing that has been changed is the added '.a' at the end
	// of fragmentShader. This little change will make every pixel have
	// the color alphaOfPixel * spriteBatchColor instead of just tinting.
		String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
	            + "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
	            + "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
	            + "uniform mat4 u_projTrans;\n" //
	            + "varying vec4 v_color;\n" //
	            + "varying vec2 v_texCoords;\n" //
	            + "\n" //
	            + "void main()\n" //
	            + "{\n" //
	            + "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
	            + "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" //
	            + "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
	            + "}\n";
		String fragmentShader = "#ifdef GL_ES\n" //
	            + "#define LOWP lowp\n" //
	            + "precision mediump float;\n" //
	            + "#else\n" //
	            + "#define LOWP \n" //
	            + "#endif\n" //
	            + "varying LOWP vec4 v_color;\n" //
	            + "varying vec2 v_texCoords;\n" //
	            + "uniform sampler2D u_texture;\n" //
	            + "void main()\n"//
	            + "{\n" //
	            + "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords).a;\n" //
	            + "}";
		shader = new ShaderProgram(vertexShader, fragmentShader);
	}
}
