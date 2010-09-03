package com.dormbells.writer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import org.xml.sax.SAXException;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * The main class for the Serial MSP430 Note Writer.
 * 
 * This class initializes UART communication and transmits song data to a receiving MSP430.
 * The MSP430 must be running code to receive the data, write it to Flash memory, and acknowledge transfer.
 * 
 * Copyright (C) 2010  DormBells
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 */

/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class Writer {

	enum Error {
		INVALID_FILE,
		INVALID_INPUT,
		SYSTEM_ERROR;
	}
	
	private static final int BAUD_RATE = 2400;				// communication speed (baud)
    /** MSP430 Timer clock in Hz */
	public static final int CLOCK_FREQ = 32768;
	private static final int MAX_NUM_NOTES = 94;
	private static final boolean DEBUG = true;

	// Data stream from serial communication
	private OutputStream out;
	
	private int time;	// lower numeral of time signature
	// Data to transmit
	private List<Note> song;		
	private int pause;
	private int tempo;	

	/**
	 * Initializes serial communication and data streams.
	 * @param portName the name of port used for communication, e.g. "COM1" for Windows or "/dev/ttyUSB0" for Linux.
	 * @throws NoSuchPortException
	 * @throws PortInUseException
	 * @throws UnsupportedCommOperationException
	 * @throws IOException
	 */
	public Writer(String portName) throws NoSuchPortException, PortInUseException, 
	UnsupportedCommOperationException, IOException {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);
		SerialPort sp = (SerialPort) commPort;
		sp.setSerialPortParams(BAUD_RATE,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);

		out = sp.getOutputStream();
		song = new ArrayList<Note>();
	}
	
	/**
	 * Adds a new note to the song.  Will not add the note if max number of notes
	 * are present in the song.
	 * @param note the note to add
	 */
	public void addNote(Note note) {
		if (song.size() == MAX_NUM_NOTES)
			System.err.println("Sorry, already " + MAX_NUM_NOTES + " notes" +
			" have been given. No more will be accepted.");
		else
			song.add(note);
	}

	/**
	 * Sets the pause in between the notes in clock ticks
	 * @param pause the pause in milliseconds
	 */
	public void setPause(int pause) {
		this.pause = Math.round((float)CLOCK_FREQ * pause / 1000);
		if (DEBUG) System.out.println("Pause: " + this.pause);
	}
	
	/**
	 * Sets the tempo in clock ticks
	 * @param tempo the tempo in beats per minute
	 */
	public void setTempo(int tempo) {
		this.tempo = Math.round((float)CLOCK_FREQ * 60 / tempo);
		if (DEBUG) System.out.println("Tempo: " + this.tempo);
	}
	
	/**
	 * Sets what note value constitutes one beat (the lower numeral of the time signature)
	 * @param time
	 */
	public void setTime(int time) { this.time = time; }
	
	/**
	 * Get the tick values into an integer array for sending
	 * @return the tick values
	 */
	private int[] getNotesTones() {
		int[] tones = new int[song.size()];
		if (DEBUG) System.out.print("Tones: [");
		for (int i = 0; i < tones.length; i++) {
			tones[i] = song.get(i).getNoteTicks();
			if (DEBUG) {
				System.out.print(song.get(i).getNoteName());
				if (i != tones.length-1) System.out.print(", ");
			}
		}
		if (DEBUG) System.out.println("]");
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
	private int[] getNotesBeats() {
		int[] beats = new int[song.size()];
		float[] noteValues = new float[song.size()];
		float shortestNoteValue = Float.MIN_VALUE;
		
		// find shortest note (means largest number)
		for (int i = 0; i < noteValues.length; i++) {
			noteValues[i] = song.get(i).getNoteValue();
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
					song.get(i).getNoteName() + ", " + song.get(i).getNoteValue() + ")" +
					" is too long to playback on MSP430. Please either increase tempo" +
					"or shorten the note. Error, exiting"  );
				System.exit(Error.SYSTEM_ERROR.ordinal());
			}
		}
		
		if (DEBUG) System.out.println("Beats: " + Arrays.toString(beats));
		if (DEBUG) System.out.println("New Tempo: " + tempo);
		return beats;
	}
	
	/**
	 * Send an 8-bit integer over the serial line.  Only the 8 lowest bits are sent; the remaining bits are discarded.
	 * @param num the integer to send.
	 * @throws IOException
	 */
	private void writeByte(int num) throws IOException {
		out.write(num);
	}

	/**
	 * Send a 16-bit integer over the serial line.  Only the 16 lowest bits are sent; the remaining bits are discarded.
	 * @param num the integer to send.
	 * @throws IOException
	 */
	private void writeInt(int num) throws IOException {
		// since all Java variables are signed and 
		// locals take 32-bits in JVM, might as well use ints
		int LSB = num & 0xFF;
		int MSB = (num >> 8) & 0xFF;
		// MSP430 is little endian
		out.write(LSB);
		out.write(MSB);
	}

	/**
	 * Send the array of beats over the serial line.  As usual, all 24 higher bits are discarded.
	 * @param arr array of beats to send
	 * @throws IOException
	 */
	private void writeArray(int[] arr) throws IOException {
		for (int i = 0; i < arr.length; i++) {
			out.write((byte) arr[i]);
		}
	}

	/**
	 * Sends data over the serial line in the appropriate order.
	 */
	void send() {
		try {
			int[] tones = getNotesTones();
			int[] beats = getNotesBeats();	// has to be called in advance since tempo will change
			writeByte(song.size());
			writeByte(pause);
			writeArray(tones);
			out.flush();
			Thread.sleep(40);	// wait for MSP430 to write to flash
			writeInt(tempo);
			writeArray(beats);
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(Error.SYSTEM_ERROR.ordinal());
		} catch (InterruptedException e) { }
		if (DEBUG) System.out.println("Done sending.");
	}
	
	/**
	 * Closes all I/O Streams and terminates JVM.
	 */
	void exit() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	/**
	 * one argument must be comm port
	 * must give -f: take file input for song data (pass filename as next argument)
	 * 
	 * @param commPorts list of communication ports
	 * @param args command-line arguments
	 * @return options data for main() to interpret.  [0] is comm port, 
	 * and [1] is input file
	 */
	private static String[] optParse(List<String> commPorts, String args[]) {
		String commPort = null;
		boolean validComm = true;
		String[] returnVal = new String[2];
		if (args.length != 3) {
			System.err.println("comm port and input file are required arguments");
			System.exit(Error.INVALID_INPUT.ordinal());
		} 
		// search for comm port argument.  It can't have a '-' prefix nor be the argument to a '-f'
		int i = 0;
		while (i < args.length && commPort == null) {
			if ((args[i].charAt(0) != '-') && ((i > 0 && !args[i-1].equals("-f")) || i == 0))
				commPort = args[i];
			i++;
		}
		validComm = !(i == args.length && commPort == null);
		if (validComm) {
			// check for '-f' option (file input)
			for (i = 0; i < args.length; i++) {
				if (args[i].equals("-f") && (i+1) < args.length) {
					returnVal[1] = args[i+1];
					break;
				}
			}
			if (returnVal[1] == null) {
				System.err.println("No valid input file given. Exiting.");
				System.exit(Error.INVALID_FILE.ordinal());
			}
		}
		// check if comm port found is valid
		validComm &= commPorts.contains(commPort);
		if (!validComm) {
			System.err.println("No valid communications port given. Choose from these next time:");
			for (String s : commPorts) System.err.print(s + " ");
			System.err.println();
			System.exit(Error.INVALID_INPUT.ordinal());
		}

		returnVal[0] = commPort;
		return returnVal;
	}
	
	/**
	 * See optParse Javadoc for usage cases.
	 * @param args
	 */
	public static void main(String args[]) {
		Writer w = null;
		// find all communications ports
		List<String> commPorts = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> en = CommPortIdentifier.getPortIdentifiers();
		while (en.hasMoreElements())
			commPorts.add(en.nextElement().getName());
		if (commPorts.isEmpty()) {
			System.err.println("No communication ports present, exiting");
			System.exit(Error.SYSTEM_ERROR.ordinal());
		}

		// Options parsing
		String[] opts = optParse(commPorts, args);
		String commPort = opts[0];
		String input = opts[1];

		try {
			w = new Writer(commPort);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(Error.SYSTEM_ERROR.ordinal());
		}
		
		try {
			XMLParser xp = new XMLParser(w);
			xp.parse(input);
		} catch (FileNotFoundException e) {
			System.err.println("Input XML File not found.  Exiting.");
			System.exit(Error.INVALID_FILE.ordinal());
		} catch (SAXException e) {
			e.printStackTrace();
			System.exit(Error.SYSTEM_ERROR.ordinal());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(Error.SYSTEM_ERROR.ordinal());
		}
		w.send();
		w.exit();
	}
}
