package com.racer.item;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.racer.Car;

public class ItemBoxGroup {
	private static Model BOX_MODEL;
	private static BlendingAttribute BOX_ALPHA;
	private static Random RAND;
	private static final float BOX_SIZE = 1.2f;
	private static final float BOX_RAD2 = BOX_SIZE * BOX_SIZE * 0.2f;
	private static final float BOX_MARGIN = 1f;
	
	private ItemManager im;
	private ArrayList<Box> boxes;
	private Vector2 position;
	private float radius;
	
	private class Box extends ModelInstance {
		public Vector2 pos;
		private float rotX;
		private float rotY;
		private float rotZ;
		private float scale;
		private int direction;
		private int respawnTime;
		
		public Box(float x, float y) {
			super(BOX_MODEL, x, BOX_SIZE/1.2f, y);
			materials.get(0).set(BOX_ALPHA);
			randomizeRotation();
			pos = new Vector2(x, y);
			scale = 1f;
		}
		
		public void update() {
			if (--respawnTime == 0) {
				randomizeRotation();
			} else if (scale < 1 && respawnTime < 0) {
				scale(0.01f);
			} else if (respawnTime > 0 && scale > 0.001f) {
				scale(-0.05f);
			}
			transform.rotate(rotX, rotY, rotZ, 1f * direction);
		}
		
		private void scale(float scaleChange) {
			scale += scaleChange;
			if (scale < 0.001f) scale = 0.0008f;
			float scl = scale / transform.getScaleX();
			transform.scale(scl, scl, scl);
		}
		
		private void randomizeRotation() {
			rotX = RAND.nextFloat() * 0.8f + 0.1f;
			rotY = RAND.nextFloat() * 0.8f + 0.1f;
			rotZ = RAND.nextFloat() * 0.8f + 0.1f;
			direction = RAND.nextBoolean() ? -1 : 1;
		}
		
		public void kill() {
			respawnTime = 120;
		}
		
		public boolean pickable() {
			return respawnTime < -40;
		}
		
		/** Returns true if this box should be rendered. */
		public boolean render() {
			return scale > 0.001f;
		}
	}
	
	/**
	 * Creates a new group of item boxes. The boxes will be added in a line
	 * between pos and pos2. If they don't fit on one line, they will
	 * overflow to a new one.
	 */
	public ItemBoxGroup(int boxAmount, Vector2 pos, Vector2 pos2) {
		boxes = new ArrayList<Box>();
		float dx = pos2.x - pos.x;
		float dy = pos2.y - pos.y;
		float len = (float) Math.sqrt(dx*dx + dy*dy);
		float groupLen = boxAmount * BOX_SIZE + (boxAmount-1) * BOX_MARGIN;
		float linePos = (len - groupLen) / 2;
		
		for (int i=0; i < boxAmount; i++) {
			float lerp = linePos/len;
			boxes.add(new Box(pos.x + dx*lerp, pos.y + dy*lerp));
			linePos += BOX_SIZE + BOX_MARGIN;
		}
		
		radius = groupLen/2 + BOX_SIZE;
		position = new Vector2(pos).add(dx * 0.5f, dy * 0.5f);
	}
	
	public static void initiateBoxModel() {
		Texture text = new Texture(Gdx.files.internal("box.png"));
		ModelBuilder mb = new ModelBuilder();
        BOX_MODEL = mb.createBox(BOX_SIZE, BOX_SIZE, BOX_SIZE,
        		new Material(TextureAttribute.createDiffuse(text)),
        		Usage.Position | Usage.TextureCoordinates | Usage.Normal);
        BOX_ALPHA = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        RAND = new Random();
	}
	
	/**
	 * Renders all of the boxes in this group.
	 * @param batch The ModelBatch to render them in.
	 * @param env The lighting environment to use.
	 */
	public void render(ModelBatch batch, Environment env) {
		for (Box box : boxes) {
			if (!box.render()) continue;
			batch.render(box, env);
		}
	}
	
	public void update() {
		for (Box box : boxes) {
			box.update();
		}
	}
	
	/**
	 * Tests the boxes in this group against a car for collision. It will first
	 * see if the car is inside of the group and only then test every box individually.
	 */
	public boolean hitTestCar(Car car) {
		Vector2 carPos = car.getPosition2();
		float dist2 = carPos.dst2(position);
		float carRad2 = Car.RADIUS_SQRD;
		float radius2 = carRad2 + radius * radius;
		if (dist2 > radius2) return false;
		
		for (Box box : boxes) {
			if (!box.pickable()) continue;
			
			dist2 = carPos.dst2(box.pos);
			radius2 = carRad2 + BOX_RAD2;
			if (dist2 <= radius2) {
				box.kill();
				im.giveItemTo(car);
				return true;
			}
		}
		return false;
	}
	
	public void setManager(ItemManager im) {
		this.im = im;
	}
}
