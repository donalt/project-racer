package com.racer;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.racer.item.ItemBoxGroup;
import com.racer.track.Node;
import com.racer.track.Track;

/**
 * Ground consists of a big flat texture that covers the 80% bottom part of
 * the screen. The texture is generated using an array of nodes. The same list
 * also generates walls for collisions and a minimap.
 */
public class Ground {
	private static final int GROUND_SIZE = 1200;
	private static final int PIXMAP_SIZE = 4096;
	private static final float SCALE = ((float)PIXMAP_SIZE)/GROUND_SIZE;
	
	private static final float WALL_WIDTH = 0.8f;
	private static final float WALL_HEIGHT = 0.35f;
	
	private static final int C_ROAD_LIGHT = 0x939393FF;
	private static final int C_ROAD_DARK = 0x757575FF;
	private static final int C_BORDER_LIGTH = 0x363636FF;
	
	private Model trackModel;
	private ModelBatch modelBatch;
	private ModelInstance instance;
	private ModelInstance ground;
	private ModelInstance walls;
	private Environment environment;
	private ArrayList<ItemBoxGroup> boxes;
	
	private Pixmap pixmap;
	private Texture texture;
	private Texture groundTex;
	
	// These fields and the code within this class that creates the minimap
	// should probably be in the Minimap class, but this is a quickfix.
	public Pixmap minimap;
	public float[] minimapV;
	public ArrayList<Vector2> segments;
	
	private boolean useAltColor;
	public Decal goal;
	
	private ArrayList<Vector2> leftWall;
	private ArrayList<Vector2> rightWall;
	private float goalLineWidth;
	
	public Ground () {
		minimapV = new float[5];
		modelBatch = new ModelBatch();
		
		groundTex = new Texture(PIXMAP_SIZE, PIXMAP_SIZE, Format.RGB888);
		// Create the ground model
		ModelBuilder modelBuilder = new ModelBuilder();
        Model groundModel = modelBuilder.createBox(GROUND_SIZE, 0.01f, GROUND_SIZE,
        	   new Material(TextureAttribute.createDiffuse(groundTex)),
               Usage.Position | Usage.TextureCoordinates | Usage.Normal);
        ground = new ModelInstance(groundModel);
        ground.transform.translate(0, -0.2f, 0);
        createGround();
        
        // Lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1));
        DirectionalLight direct = new DirectionalLight();
        direct.set(0.8f, 0.8f, 1f, 0.6f, -0.4f, 0.3f);
        environment.add(direct);
        
        ItemBoxGroup.initiateBoxModel();
	}
	
	public void generateTrack(Track track) {
		if (trackModel != null) trackModel.dispose();
		boxes = new ArrayList<ItemBoxGroup>();
		// TEMPORARY FOR TESTING
		boxes.add(new ItemBoxGroup(6, new Vector2(0, 15), new Vector2(15, 15)));
		
		pixmap = new Pixmap(PIXMAP_SIZE, PIXMAP_SIZE, Format.RGBA8888);
		texture = new Texture(pixmap);
		
		ModelBuilder modelBuilder = new ModelBuilder();
		// Create a plane by choosing a small value (0.01f) as height of box.
        trackModel = modelBuilder.createBox(GROUND_SIZE, 0.01f, GROUND_SIZE,
        		new Material(TextureAttribute.createDiffuse(texture)),
        		Usage.Position | Usage.TextureCoordinates | Usage.Normal);
        instance = new ModelInstance(trackModel);
        instance.materials.get(0).set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        instance.transform.rotate(0, 1, 0, -90);
        
        generateTrackData(track);
	}
	
	public ArrayList<Vector2> getLeft() {
		return leftWall;
	}
	public ArrayList<Vector2> getRight() {
		return rightWall;
	}
	public float getGoalLineWidth() {
		return goalLineWidth;
	}
	public Environment getEnv() {
		return environment;
	}
	public ArrayList<ItemBoxGroup> getBoxes() {
		return boxes;
	}
	
	private void generateTrackData(Track track) {
		ArrayList<Node> nodes = track.nodes;
		int SegLen = track.segmentLength;
		
		// These ArrayLists will store all of the node
		// positions to be used for collision testing.
		ArrayList<Vector2> leftN = new ArrayList<Vector2>();
		ArrayList<Vector2> rightN = new ArrayList<Vector2>();
		segments = new ArrayList<Vector2>();
		
		double pi2 = Math.PI/2;
		double lastRot = nodes.get(0).rot;
		// All vectors are instantiated here so that the don't
		// have to be recreated each loop
		Vector2 pos = new Vector2(PIXMAP_SIZE / 2, PIXMAP_SIZE / 2);
		Vector2 pos2 = new Vector2();
		Vector2 left = new Vector2();
		Vector2 left2 = new Vector2();
		Vector2 right = new Vector2();
		Vector2 right2 = new Vector2();
		
		Vector2 prevLeft = new Vector2();
		Vector2 prevRight = new Vector2();
		
		// Keep track of the bottom-left and upper-right corner of the map.
		Vector2 blCorner = new Vector2(pos);
		Vector2 urCorner = new Vector2(pos);
		
		//pixmap.setColor(C_GRASS_LIGHT);
		//pixmap.fillRectangle(0, 0, PIXMAP_SIZE, PIXMAP_SIZE);
		
		// Add a copy of first node to end to help with looping
		nodes.add(nodes.get(0));
		Vector2 startPos = new Vector2(pos);
		useAltColor = false;
		for (int i=0; i<nodes.size() - 1; i++) {
			Node node1 = nodes.get(i);
			Node node2 = nodes.get(i+1);
			
			// Check if the road turns or goes straight with this segment.
			boolean turnsLeft = lastRot > node1.rot;
			boolean turnsRight = lastRot < node1.rot;
			lastRot = node1.rot;
			
			if (turnsRight) {
				pos.set(prevRight).add((float) Math.cos(node1.rot-pi2)*node1.width/2,
						               (float) Math.sin(node1.rot-pi2)*node1.width/2);
			} else if (turnsLeft) {
				pos.set(prevLeft).add((float) Math.cos(node1.rot+pi2)*node1.width/2,
				                      (float) Math.sin(node1.rot+pi2)*node1.width/2);
			}
			
			
			left.set(pos).add((float) Math.cos(node1.rot-pi2)*node1.width/2,
			                  (float) Math.sin(node1.rot-pi2)*node1.width/2);
			right.set(pos).add((float) Math.cos(node1.rot+pi2)*node1.width/2,
	                           (float) Math.sin(node1.rot+pi2)*node1.width/2);
			
			if (i == 0) {
				// Move the beginning nodes so that right is at (0, 0)
				pos.set(left);
				left.set(pos).add((float) Math.cos(node1.rot-pi2)*node1.width/2,
		                  (float) Math.sin(node1.rot-pi2)*node1.width/2);
				right.set(pos).add((float) Math.cos(node1.rot+pi2)*node1.width/2,
                         (float) Math.sin(node1.rot+pi2)*node1.width/2);
				startPos.set(pos);
				
				// Add goal at the first segment
				Vector2 goalPos = new Vector2(pos).sub(left);
				goal = Decal.newDecal(new TextureRegion(
						new Texture(Gdx.files.internal("goal.png"))), true);
				goal.setWidth(node1.width/SCALE);
				goal.setHeight(5);
				goal.setPosition(goalPos.x/SCALE, 2.5f, goalPos.y/SCALE);
			}
			
			// If the road turns, draw a connecting triangle.
			if (turnsRight) {
				pixmap.setColor(C_ROAD_DARK);
				drawTriangle(prevLeft, prevRight, left);
				leftN.add(new Vector2(prevLeft));
			}
			else if (turnsLeft) {
				pixmap.setColor(C_ROAD_DARK);
				drawTriangle(prevLeft, prevRight, right);
				rightN.add(new Vector2(prevRight));
			}
			
			leftN.add(new Vector2(left));
			rightN.add(new Vector2(right));
			
			// Loop through each segment of the node and draw them
			// with two triangles each.
			for (int segment = 1; segment <= node1.length; segment++) {
				float interp = (float)segment / node1.length;
				float rWidth = node1.width/2 + (node2.width-node1.width)/2 * interp;
				
				pos2.set(pos.x + (float)Math.cos(node1.rot)*SegLen*node1.length *interp,
						 pos.y + (float)Math.sin(node1.rot)*SegLen*node1.length *interp);
				left2.set(pos2).add((float) Math.cos(node1.rot-pi2)*rWidth,
		                            (float) Math.sin(node1.rot-pi2)*rWidth);
				right2.set(pos2).add((float) Math.cos(node1.rot+pi2)*rWidth,
	                                 (float) Math.sin(node1.rot+pi2)*rWidth);
				
				pixmap.setColor(nextRoadColor());
				drawTriangle(left, right2, right);
				drawTriangle(left, right2, left2);
				
				left.set(left2);
				right.set(right2);
			}
			pos.set(pos2);
			prevLeft.set(left2);
			prevRight.set(right2);
			
			// Save the end line of the segment
			segments.add(new Vector2(left2));
			segments.add(new Vector2(right2));
		}
		
		// Draw the last connecting piece between the first and last segment.
		Vector2 firstLeft = new Vector2();
		Vector2 firstRight = new Vector2();
		Node n1 = nodes.get(0);
		firstLeft.set(startPos).add((float) Math.cos(n1.rot-pi2)*n1.width/2,
		                            (float) Math.sin(n1.rot-pi2)*n1.width/2);
		firstRight.set(startPos).add((float) Math.cos(n1.rot+pi2)*n1.width/2,
                                     (float) Math.sin(n1.rot+pi2)*n1.width/2);
		pixmap.setColor(C_ROAD_DARK);
		drawTriangle(firstLeft, prevRight, prevLeft);
		drawTriangle(firstLeft, prevRight, firstRight);
		
		// Add the two last nodes to the node collections.
		leftN.add(new Vector2(prevLeft));
		rightN.add(new Vector2(prevRight));
		// Loop the nodes
		leftN.add(new Vector2(leftN.get(0)));
		rightN.add(new Vector2(rightN.get(0)));
		// Also save the beginning nodes to finish of the segment list.
		segments.add(new Vector2(leftN.get(0)));
		segments.add(new Vector2(rightN.get(0)));
		
		// Update the corner vectors
		calcCorners(leftN, blCorner, urCorner);
		calcCorners(rightN, blCorner, urCorner);
		
		// Scale the wall nodes so that they are in the same scale as the pixmap.
		Vector2 offset = new Vector2(rightN.get(0).scl(-1/SCALE));
		scaleToGround(segments, offset);
		scaleToGround(leftN, offset);
		scaleToGround(rightN, offset);
		// fix weird bug where the first right node gets sent into oblivion
		rightN.get(0).set(rightN.get(rightN.size()-1));
		
		texture.draw(pixmap, 0, 0);
		
		generateWalls(leftN, rightN);
		leftWall = leftN;
		rightWall = rightN;
		goalLineWidth = leftN.get(0).x;
		
		// Create the minimap by copying the road that has been drawn by
		// scaling and moving it to be as big as possible on the minimap.
		int mapSize = 180;
		minimap = new Pixmap(mapSize, mapSize, Pixmap.Format.RGBA4444);
		float mapWidth = urCorner.x - blCorner.x;
		float mapHeight = urCorner.y - blCorner.y;
		float biggestLen = (mapWidth > mapHeight) ? mapWidth : mapHeight;
		int xOff = (int) ((mapSize/2)- mapSize/2 *mapWidth/biggestLen);
		int yOff = (int) ((mapSize/2)- mapSize/2 *mapHeight/biggestLen);
		
		minimap.drawPixmap(pixmap, (int)blCorner.x, (int)blCorner.y,
				(int)biggestLen, (int)biggestLen, xOff, yOff, mapSize, mapSize);
		minimapV[0] = SCALE*mapSize/biggestLen;
		minimapV[1] = xOff - (blCorner.x - PIXMAP_SIZE/2) *mapSize/biggestLen;
		minimapV[2] = yOff + (urCorner.y - PIXMAP_SIZE/2) *mapSize/biggestLen;
		minimapV[3] = goalLineWidth * minimapV[0];
		
	}
	
	/**
	 * Generate 3D walls on the track.
	 * @param left  An array of vectors that define the left wall.
	 * @param right An array of vectors that define the right wall.
	 */
	private void generateWalls(ArrayList<Vector2> left, ArrayList<Vector2> right) {
		ModelBuilder modelB = new ModelBuilder();
		modelB.begin();
		Material mat = new Material(ColorAttribute.createDiffuse(new Color(C_BORDER_LIGTH)));
		MeshPartBuilder mb = modelB.part("walls", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, mat);
		
		float len, h = WALL_HEIGHT;
		
		// Once for each wall
		for (int w=0; w < 2; w++) {
			ArrayList<Vector2> nodes;
			if (w==0) {
				nodes = left;
				len = -WALL_WIDTH;
			}
			else {
				nodes = right;
				len = WALL_WIDTH;
			}
			
			Vector2 prev = nodes.get(0);
			for (int i=1; i < nodes.size(); i++) {
				Vector2 next = nodes.get(i), n1, n2;
				// The left and right walls need to draw the box in different orders.
				if (w==0) {
					n1 = next;
					n2 = prev;
				} else {
					n1 = prev;
					n2 = next;
				}
				double a = Math.atan2(prev.y-next.y, prev.x-next.x) - Math.PI/2;
				
				float dx = (float)Math.cos(a)*len;
				float dy = (float)Math.sin(a)*len;
				mb.box(new Vector3(n1.x, 0, n1.y), new Vector3(n1.x, h, n1.y),
					   new Vector3(n2.x, 0, n2.y), new Vector3(n2.x, h, n2.y),
					   new Vector3(n1.x+dx, 0, n1.y+dy), new Vector3(n1.x+dx, h, n1.y+dy),
					   new Vector3(n2.x+dx, 0, n2.y+dy), new Vector3(n2.x+dx, h, n2.y+dy));
				prev.set(next);
			}
		}
		Model walls = modelB.end();
		this.walls = new ModelInstance(walls);
	}
	
	/**
	 * Convenience method meant for the generateRoad method. It returns the
	 * next road color in order to get alternate coloring.
	 */
	private int nextRoadColor() {
		useAltColor = !useAltColor;
		if (useAltColor) {
			return C_ROAD_DARK;
		}
		return C_ROAD_LIGHT;
	}
	
	/**
	 * Convenience method for generateRoad. Draws a triangle
	 * on the pixmap using three vectors.
	 */
	private void drawTriangle(Vector2 v1, Vector2 v2, Vector2 v3) {
		pixmap.fillTriangle((int)v1.x, (int)v1.y,
                            (int)v2.x, (int)v2.y,
                            (int)v3.x, (int)v3.y);
	}
	
	/**
	 * Scales an array of vector from pixmap coordinates to ground coordinates.
	 * Mostly meant for the node lists in generateGround.
	 * @param list
	 */
	private void scaleToGround(ArrayList<Vector2> list, Vector2 offset) {
		float scale = -1/SCALE;
		for (Vector2 v : list) {
			v.scl(scale);
			v.sub(offset);
		}
	}
	
	private void calcCorners(ArrayList<Vector2> nodes, Vector2 blCorner, Vector2 urCorner) {
		for (Vector2 n : nodes) {
			if (n.x < blCorner.x) blCorner.x = n.x;
			else if (n.x > urCorner.x) urCorner.x = n.x;
			if (n.y < blCorner.y) blCorner.y = n.y;
			else if (n.y > urCorner.y) urCorner.y = n.y;
		}
	}
	
	private void createGround() {
	    Pixmap pix = new Pixmap(Gdx.files.internal("grass.png"));
	    int pSize = pix.getWidth();
	    
	    for (int x=0; x < PIXMAP_SIZE; x++) {
	    	for (int y=0; y < PIXMAP_SIZE; y++) {
	    		groundTex.draw(pix, x, y);
	    		// For some reason, using pSize as a step results in gaps 
	    		// between segments, therefore add -1.
	    		y += pSize-1;
	    	}
	    	// -1 here as well.
	    	x += pSize-1;
	    }
	}
	
	/**
	 * Renders the ground using the specified camera.
	 */
	public void render(Camera cam) {
		modelBatch.begin(cam);
		modelBatch.render(ground, environment);
        modelBatch.render(instance, environment);
        modelBatch.render(walls, environment);
        modelBatch.end();
        
        // The item boxes need to be in a separate batch, otherwise they
        // blend weirdly with the track for some reason.
        modelBatch.begin(cam);
        for (ItemBoxGroup group : boxes) {
        	group.render(modelBatch, environment);
        }
        modelBatch.end();
	}
}