package com.dormbells.writer;

import java.util.regex.*;

import com.dormbells.writer.Writer.Error;

/**
 * Class for a note in a song.  Translates scientific pitch
 * notation (SPN) note names into MSP430 clock ticks
 * and note values into appropriate duration values.
 * 
 * Copyright (C) 2010  DormBells
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 *
 */

/*
 * This file is part of Writer.
 *
 * Writer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Writer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Writer.  If not, see <http://www.gnu.org/licenses/>.
 */

public class Note {

	// required fields
	private String noteName;
	private float noteValue;
	private int noteTicks;
	
	// useful fields
	private String note;
	private boolean isSharp;
	private boolean isFlat;
	private int index;
	
	/**
	 * Constructor for a new note
	 * 
	 * @param noteName the name of the note to add in SPN, e.g. "F#4"
	 * @param noteValue the value of the note, e.g. 8 for eighth note
	 */
	public Note(String noteName, String noteValue) {
		this.setNoteName(noteName);
		this.setNoteValue(noteValue);
		this.setNoteTicks();
	}
	
	/**
	 * sets the note name for the note
	 * Program will fail and exit if note name is not in SPN format
	 * Use '#' for Sharp, 'b' for Flat, and 'R' for rest
	 * 
	 * @param noteName the note name in SPN, e.g. "Gb3"
	 */
	public void setNoteName(String noteName) {
		Pattern p = Pattern.compile("\\A([A-G])(#|b)?([0-8])\\z");
		Matcher m = p.matcher(noteName);
		if (m.matches()) {
			this.noteName = noteName;
			note = m.group(1);
			isSharp = "#".equals(m.group(2));
			isFlat = "b".equals(m.group(2));
			index = Integer.valueOf(m.group(3));	
		}
		else if (noteName.equals("R"))
			this.noteName = note = noteName;
		else {
			System.err.println("Invalid note " + noteName + 
			" has been given.  Exiting");
			System.exit(Error.INVALID_INPUT.ordinal());
		}
	}
	
	/** @return the note name in scientific pitch notation */
	public String getNoteName() {	return noteName;	}

	/** 
	 * @param noteValue the note value to set, e.g. 8 for eighth note
	 * Note values can also be dotted, e.g. "8." to increase length by 50%
	 * Accepted note values are powers of 2.  Program will fail and exit if value is not.
	 */
	public void setNoteValue(String noteValue) {	
		Pattern p = Pattern.compile("\\A([0-9]+)(\\.?)\\z");
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

	/**
	 * Calculates and sets the number of timer ticks the
	 * MSP430 needs to wait before changing square wave voltage.
	 * 
	 * Program will fail and exit if the note name is not set first.
	 */
	public void setNoteTicks() {
		int key = noteNametoKey();
		if (key != -1)
			this.noteTicks = keyToTicks(key);
		else {
			System.err.println("The note sub-name " + note + 
					" is invalid.  Error, exiting");
			System.exit(Error.SYSTEM_ERROR.ordinal());
		}
	}

	/**
	 * @return the number of timer ticks the MSP430 needs 
	 * to wait before changing square wave voltage. Used
	 * by the PWM system to playback a tone.
	 */
	public int getNoteTicks() {
		return noteTicks;
	}
	
	/**
	 * helper method for setNoteTicks() that takes data
	 * from an SPN note to figure out the associated piano key.
	 * That key can be used to calculate frequency and thus ticks.
	 * 
	 * @return the piano key associated with the SPN note
	 */
	private int noteNametoKey() {
		int key = 12 * index;
		if 		(note.equals("A")) key += 1;
		else if (note.equals("B")) key += 3;
		else if (note.equals("C")) key -= 8;
		else if (note.equals("D")) key -= 6;
		else if (note.equals("E")) key -= 4;
		else if (note.equals("F")) key -= 3;
		else if (note.equals("G")) key -= 1;
		else if (note.equals("R")) key = 0;
		else return -1;
		
		if (isSharp) key++;
		if (isFlat) key--;
		return key;
	}
	
	/**
	 * Helper method for setNoteTicks() that uses the piano key
	 * to calculate frequency and PWM timer ticks.
	 * @param key the piano key for the SPN note
	 * @return the number of PWM timer ticks needed for note playback
	 */
	private int keyToTicks(int key) {
		// if freq is 0 then note is a rest
		if (key == 0) return 0;
		// formula from http://en.wikipedia.org/wiki/Piano_key_frequencies
		double freq = 440 * Math.pow(2, (float)(key - 49) / 12);
		return (int) (Math.round(Writer.CLOCK_FREQ / (freq * 2)));
	}
	
}
