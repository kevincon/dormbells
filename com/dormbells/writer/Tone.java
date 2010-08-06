package com.dormbells.writer;

/**
 * The mapping between note names and "frequencies"
 * Currently set for a D major mapping.
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 */

// 
public enum Tone {
    D (56), // 294 Hz
    E (50), // 330 Hz
    F (44), // 370 Hz (F#)
    G (42), // 392 Hz
    A (37), // 440 Hz
    B (33), // 494 Hz
    C (30), // 554 Hz (C#)
    d (28), // 587 Hz
    R (0); // rest
    	    
    public final int freq;
    Tone (int freq) { this.freq = freq; }
}
