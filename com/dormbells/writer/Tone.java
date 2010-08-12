package com.dormbells.writer;

/**
 * Object mapping note name and timer clock ticks
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 */

public class Tone {

	private String name;
    private int ticks;
    
    public void setName(String name) { this.name = name; }
    /**
     * Converts note frequency to MSP430 timer clock ticks
     * @param freq note frequency
     */
    public void setTicks(float freq) {
    	this.ticks = (freq != 0) ? Math.round(Writer.CLOCK_FREQ / (freq * 2)) : 0;
    }

    public String getName() { return this.name; }
    public int getTicks() { return this.ticks; }
}
