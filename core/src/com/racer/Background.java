package com.racer;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Background handles the rendering of the background.
 */
public class Background {
	private SpriteBatch sb;
	private ArrayList<Texture> layers;
	
	int width;
	int y;
	
	public Background() {
		sb = new SpriteBatch();
		
		layers = new ArrayList<Texture>();
		layers.add(new Texture(Gdx.files.internal("bg0_01.png")));
		layers.add(new Texture(Gdx.files.internal("bg0_02.png")));
		
		width = Gdx.graphics.getWidth();
		y = Gdx.graphics.getHeight() - 80;
	}
	
	/**
	 * Render the background layers with parallax scrolling.
	 * @param angle The angle to use for the parallax effect.
	 */
	public void render(float angle) {
		sb.begin();
		
		for (int i=0; i < layers.size(); i++) {
			Texture bg = layers.get(i);
			
			int w = bg.getWidth()*4;
			double scale = w/(Math.PI*2);
			int x = (int)(-angle * scale) % w;
			if (x > 0) x -= w;
			
			while (x < width) {
				sb.draw(bg, x, y, w, 80);
				x += w;
			}
		}
		sb.end();
	}
}
