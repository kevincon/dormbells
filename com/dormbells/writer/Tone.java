package com.dormbells.writer;

public enum Tone {
    C (63), // 261 Hz
    D (56), // 294 Hz
    E (50), // 329 Hz
    F (47), // 349 Hz
    G (42), // 392 Hz
    A (37), // 440 Hz
    B (33), // 493 Hz
    R (0);	// rest
    	    
    public final int freq;
    Tone (int freq) { this.freq = freq; }
}
