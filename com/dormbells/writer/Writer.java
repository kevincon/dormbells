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
	
	/** communication speed (baud) */
	private static final int BAUD_RATE = 2400;
	/** size of usable MSP430 info memory */
	private static final int MAX_BYTES = 192;
    /** MSP430 Timer clock in Hz */
	public static final int CLOCK_FREQ = 32768;
	public static final boolean DEBUG = true;

	/** Data stream from serial communication */
	private OutputStream out;
	/** Data to transmit */
	private static List<Song> songs;		


	/**
	 * Initializes serial communication and data streams.
	 * @param portName the name of port used for communication, 
	 * e.g. "COM1" for Windows or "/dev/ttyUSB0" for Linux.
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
		songs = new ArrayList<Song>();
	}
	
	/**
	 * Send an 8-bit integer over the serial line.  
	 * Only the 8 lowest bits are sent; the remaining bits are discarded.
	 * @param num the integer to send.
	 * @throws IOException
	 */
	private void writeByte(int num) throws IOException {
		out.write(num);
		out.flush();
	}

	/**
	 * Sends data over the serial line in the appropriate order.
	 * Also enforces MSP430 memory limits
	 */
	void send() {
		int totalBytes = 0;
		ArrayList<Integer> data = new ArrayList<Integer>();
		Song prevSong = null;

		// collect the data to send based off memory limits
		for (Song song : songs) {
			// 2 per note for ticks and value, 1 for pause, 1 for length, 2 for tempo
			totalBytes += 2*song.getLength() + 1 + 1 + 2;
			if (totalBytes > MAX_BYTES) {
				if (prevSong == null)
					System.err.println("No songs to write!");
				else
					System.err.println("Memory limit exceeded: only writing up to song \"" + prevSong.getTitle() + "\"");
				break;
			}
			
			int[] tones = song.getNotesTones();
			int[] beats = song.getNotesBeats(); // has to be called in advance since tempo will change

			data.add(song.getLength());
			data.add(song.getPause());
			data.add(song.getTempo() & 0xFF);	// MSP430 is little endian
			data.add((song.getTempo() >> 8) & 0xFF);
			for (int i = 0; i < tones.length; i++) { data.add(tones[i]); data.add(beats[i]); }
			
			prevSong = song;
		}
		
		// send said data
		try {
			if (DEBUG) for (int i : data) System.out.printf("0x%x, ", i);
			int counter = 0;
			writeByte(totalBytes); 	// send total bytes
			Thread.sleep(10);
			for (int byteVal : data) {
				if (counter == MAX_BYTES/2) {
					Thread.sleep(40);	// wait for MSP430 to write to flash
				}
				writeByte(byteVal);
				counter++;
			}
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
	 * first argument must be comm port
	 * rest of arguments are input XML song files
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
		if (args.length < 2) {
			System.err.println("comm port and input file are required arguments");
			System.exit(Error.INVALID_INPUT.ordinal());
		}
		
		// check if comm port asked for is valid and open it up
		String commPort = args[0];
		if (!commPorts.contains(commPort)) {
			System.err.println("No valid communications port given. Choose from these next time:");
			for (String s : commPorts) System.err.print(s + " ");
			System.err.println();
			System.exit(Error.INVALID_INPUT.ordinal());
		}
		try {
			w = new Writer(commPort);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(Error.SYSTEM_ERROR.ordinal());
		}
		
		// parse all of the input song files
		int i = 1;
		try {
			for (i = 1; i < args.length; i++) {
				Song song = new Song();
				XMLParser xp = new XMLParser(song);
				songs.add(song);
				xp.parse(args[i]);
			}
		} catch (FileNotFoundException e) {
			System.err.println("Input XML File " + args[i] + " not found.  Exiting.");
			System.exit(Error.INVALID_FILE.ordinal());
		} catch (SAXException e) {
			e.printStackTrace();
			System.exit(Error.SYSTEM_ERROR.ordinal());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(Error.SYSTEM_ERROR.ordinal());
		}
		
		// send all of them across to the MSP430
		w.send();
		w.exit();
	}
}
