package com.dormbells.writer;

/**
 * The mapping between note names and "frequencies"
 * Currently set for a D major mapping.
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 */
public enum Tone {
    D (1), // 294 Hz
    E (2), // 330 Hz
    F (3), // 370 Hz (F#)
    G (4), // 392 Hz
    A (5), // 440 Hz
    B (6), // 494 Hz
    C (7), // 554 Hz (C#)
    d (8), // 587 Hz
    R (0); // rest
    	    
    public final int freq;
    Tone (int freq) { this.freq = freq; }
}
