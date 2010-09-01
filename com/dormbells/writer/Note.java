package com.dormbells.writer;

import java.util.HashMap;
import java.util.regex.*;

import com.dormbells.writer.Writer.Error;


/**
 * Class for a note in a song.  Maintains a hash map of all allowed tones.
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 *
 */
public class Note {

	private String noteName = null;
	private float noteValue;
	
	/** the tones available for playback in the song */
	static HashMap<String, Integer> availableTones = new HashMap<String, Integer>();

	/**
	 * Add new tone to the set of available tones a song can use.
	 * If the tone is already in the set, the program will not redefine it.
	 * @param name the name of the tone to add, e.g. "F#"
	 * @param freq the frequency of the tone to add
	 */
	public static void addTone(String name, float freq) {
		if (availableTones.containsKey(name)) {
			System.err.println("A definition for the tone " + name + 
			" already exists. Not going to redefine it");
		}
		else {
			int ticks = (freq != 0) ? Math.round(Writer.CLOCK_FREQ / (freq * 2)) : 0;
			availableTones.put(name, ticks);
		}
	}
	
	/**
	 * Constructor for a new note for adding to the song
	 * Program will fail and exit if the note is not one of the available tones.
	 * @param noteName the name of the note to add, e.g. "F#"
	 * @param noteValue the value of the note, e.g. 8 for eighth note
	 */
	public Note(String noteName, String noteValue) {
		if (!availableTones.containsKey(noteName)) {
			System.err.println("Invalid note " + noteName + 
			" has been given.  Exiting");
			System.exit(Error.INVALID_INPUT.ordinal());
		}
		this.setNoteName(noteName);
		this.setNoteValue(noteValue);
	}

	/** 
	 * @param noteValue the note value to set, e.g. 8 for eighth note
	 * Note values can also be dotted, e.g. "8." to increase length by 50%
	 * Accepted note values are powers of 2.  Program will fail and exit if value is not.
	 */
	public void setNoteValue(String noteValue) {	
		Pattern p = Pattern.compile("([0-9]+)(\\.?)");
		Matcher m = p.matcher(noteValue);
		if (m.matches()) {
			int noteValueInt = Integer.valueOf(m.group(1));
			if ((noteValueInt & (noteValueInt - 1)) == 0) {	// cool power of 2 check; thanks Wikipedia!
				float noteValueFloat;
				if (m.group(2).equals("."))
					noteValueFloat = (float)noteValueInt - (float)noteValueInt * 0.25f;	// equiv. of 1.5x length
				else
					noteValueFloat = (float)noteValueInt;
				this.noteValue = noteValueFloat;
				return;
			}
		}
		System.err.println("The note value " + noteValue + " is invalid." + 
				" Parse error, exiting");
		System.exit(Error.INVALID_INPUT.ordinal());
	}
	
	/** @return the note value, e.g. 8 for eighth note */
	public float getNoteValue() {	return noteValue;	}
	/** @param noteName the note name to set */
	public void setNoteName(String noteName) {	this.noteName = noteName;	}
	/** @return the note name */
	public String getNoteName() {	return noteName;	}
}
