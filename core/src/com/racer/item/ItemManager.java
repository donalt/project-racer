package com.racer.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.racer.Car;

/**
 * ItemManager manages all of the currently activated items. It provides functions
 * for randomizing items and it also handles the item boxes on the track.
 */
public class ItemManager {
	private ArrayList<Car> cars;
	private ArrayList<ItemBoxGroup> boxes;
	private LinkedList<Item> items;
	
	public ItemManager(ArrayList<Car> cars) {
		this.cars = cars;
		items = new LinkedList<Item>();
	}
	
	public void update() {
		// Update the items that have been activated.
		Iterator<Item> it = items.iterator();
		while (it.hasNext()) {
			Item item = it.next();
			if (item.activated() && item.update()) {
				item.expire();
				it.remove();
			}
		}
		
		// Update the item boxes on the track.
		for (ItemBoxGroup group : boxes) {
			group.update();
		}
	}
	
	public void setUp(ArrayList<ItemBoxGroup> boxes) {
		this.boxes = boxes;
		for (ItemBoxGroup box : boxes) 
			box.setManager(this);
		items = new LinkedList<Item>();
	}
	
	/** Gives a random item to the specified car. */
	public void giveItemTo(Car car) {
		Item item = new Booster(car);
		items.add(item);
		car.giveItem(item);
	}
}
