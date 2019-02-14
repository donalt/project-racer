package com.racer.item;

import com.racer.Car;

/**
 * Booster gives the car a speed boost for a short while.
 */
public class Booster extends Item{
	private static final int EFFECT_DURATION = 180;
	
	public Booster(Car car) {
		super(car, Item.Type.BOOSTER);
	}
	
	@Override
	public boolean update() {
		super.update();	
		car.setMaxSpeed();
		return timeElapsed > EFFECT_DURATION;
	}
	
	@Override
	public void expire() {}
}
