package com.dormbells.writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dormbells.writer.Writer.Error;

/**
 * Class for a song. A song consists of a series of notes,
 * a time signature, a tempo, and a pause duration between notes.
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

public class Song {
	
	private List<Note> notes;
	private String title;
	private int pause;	// single byte
	private int tempo;	// two bytes
	private int time;	// lower numeral of time signature
	
	public Song() {
		notes = new ArrayList<Note>();
	}
	
	/**
	 * Set the title of the song
	 * @param title
	 */
	public void setTitle(String title) { this.title = title; }
	
	/**
	 * Returns the title of the song
	 * @return the title of the song
	 */
	public String getTitle() { return this.title; }
	/**
	 * Adds a new note to the song.
	 * @param note the note to add
	 */
	public void addNote(Note note) {
		notes.add(note);
	}

	/**
	 * Sets the pause in between the notes in clock ticks
	 * @param pause the pause in milliseconds
	 */
	public void setPause(int pause) {
		this.pause = Math.round((float)Writer.CLOCK_FREQ * pause / 1000);
		if (Writer.DEBUG) System.out.println("Pause: " + this.pause);
	}
	
	/**
	 * Returns the number of MSP430 timer ticks for a pause
	 * @return pause ticks
	 */
	public int getPause() { return pause; }
	
	/**
	 * Sets the tempo in clock ticks
	 * @param tempo the tempo in beats per minute
	 */
	public void setTempo(int tempo) {
		this.tempo = Math.round((float)Writer.CLOCK_FREQ * 60 / tempo);
		if (Writer.DEBUG) System.out.println("Tempo: " + this.tempo);
	}
	
	/**
	 * Returns the number of MSP430 timer ticks per beat
	 * @return tempo ticks
	 */
	public int getTempo() { return tempo; }
	
	/**
	 * Sets what note value constitutes one beat (the lower numeral of the time signature)
	 * @param time
	 */
	public void setTime(int time) { this.time = time; }
	
	/**
	 * Returns the number of notes in the song
	 * @return the number of notes in the song
	 */
	public int getLength() { return notes.size(); }
	
	/**
	 * Get the tick values into an integer array for sending
	 * @return the tick values
	 */
	public int[] getNotesTones() {
		int[] tones = new int[notes.size()];
		if (Writer.DEBUG) System.out.print("Tones: [");
		for (int i = 0; i < tones.length; i++) {
			tones[i] = notes.get(i).getNoteTicks();
			if (Writer.DEBUG) {
				System.out.print(notes.get(i).getNoteName());
				if (i != tones.length-1) System.out.print(", ");
			}
		}
		if (Writer.DEBUG) System.out.println("]");
		return tones;
	}
	
	/**
	 * Converts and returns beat values derived from the note 
	 * values in the song. Changes the tempo in accordance 
	 * with the note values and the time signature. Program 
	 * fails and exits if a note is too lengthy to be playable.
	 * 
	 * @return the beat values for transmission
	 */
	public int[] getNotesBeats() {
		int[] beats = new int[notes.size()];
		float[] noteValues = new float[notes.size()];
		float shortestNoteValue = Float.MIN_VALUE;
		
		// find shortest note (means largest number)
		for (int i = 0; i < noteValues.length; i++) {
			noteValues[i] = notes.get(i).getNoteValue();
			if (noteValues[i] > shortestNoteValue) shortestNoteValue = noteValues[i];
		}
		
		// convert to ints for use as beats
		// since all beat values have 3 in numerator,
		// multiplying by 3 will eliminate all fractions (without needing to calculate GCF)
		// working with shortest note will make beat values as small as possible
		// these changes must be accounted for in the tempo
		tempo /= shortestNoteValue * 3 / time;
		for (int i = 0; i < beats.length; i++) {
			beats[i] = (int)Math.rint((shortestNoteValue * 3 / noteValues[i]));
			// check if note is too lengthy to be playable
			if (beats[i] * tempo > 65535) {
				System.err.println("Uh oh, note " + (i+1) + " (" + 
					notes.get(i).getNoteName() + ", " + notes.get(i).getNoteValue() + ")" +
					" is too long to playback on MSP430. Please either increase tempo" +
					"or shorten the note. Error, exiting"  );
				System.exit(Error.SYSTEM_ERROR.ordinal());
			}
		}
		
		if (Writer.DEBUG) System.out.println("Beats: " + Arrays.toString(beats));
		if (Writer.DEBUG) System.out.println("New Tempo: " + tempo);
		return beats;
	}
}
