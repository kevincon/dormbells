package com.dormbells.writer;

/**
 * Class for a note in a song
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 *
 */
public class Note {

	private Tone tone = null;
	private String toneName = null;
	private int beats;
	/**
	 * @param name the name to set
	 */
	public void setTone(Tone tone) {	this.tone = tone;	}
	/**
	 * @return the name
	 */
	public Tone getTone() {		return tone;	}
	/**
	 * @param beats the duration to set
	 */
	public void setBeats(int beats) {	this.beats = beats;	}
	/**
	 * @return the duration
	 */
	public int getBeats() {	return beats;	}
	/**
	 * @param toneName the toneName to set
	 */
	public void setToneName(String toneName) {	this.toneName = toneName;	}
	/**
	 * @return the toneName
	 */
	public String getToneName() {	return toneName;	}
}
