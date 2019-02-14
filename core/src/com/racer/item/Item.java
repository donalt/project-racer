package com.racer.item;

import com.racer.Car;

/**
 * Defines an Item in the game. An Item could give both passive effects,
 * such as boosts, and also spawn objects such as traps and projectiles.
 */
public abstract class Item {
	private Type type;
	private int id;
	protected Car car;
	protected int timeElapsed;
	private boolean activated;
	
	public static enum Type {
		BOOSTER
	}
	
	/**
	 * Creates a new item that primarily will affect the car.
	 */
	public Item(Car car, Type type) {
		this.type = type;
		this.car = car;
		id = 1;
	}
	
	public int getId() {
		return id;
	}
	
	public Type type() {
		return type;
	}
	
	/** Activate triggers when the players uses this item. */
	public void activate() {
		activated = true;
	}
	/** Returns true if this item has been used. */
	public boolean activated() {
		return activated;
	}
	
	/**
	 * Update the effects of this item.
	 * @return True if this item should expire.
	 */
	public boolean update() {
		timeElapsed++;
		return false;
	}
	
	/**
	 * Expires the effects of this item.
	 */
	public abstract void expire();
}
