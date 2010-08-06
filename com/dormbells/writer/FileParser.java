package com.dormbells.writer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Parses a plain text file for song data.  A proper input text file should have:
 * Line 1) Pause in milliseconds (omit unit), e.g. "1"
 * Line 2) Tempo in beats per minute (omit unit), e.g. "169"
 * Line 3) Notes in comma-separated format, e.g. "A, B, C"
 * Line 4) Beats in comma-separated format, e.g. "1, 2, 1"
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 *
 */
public class FileParser {
	
	FileParser(Writer w, String filename) {
		// open file
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.err.println("File " + filename + " doesn't exist.");
			System.err.println("Exiting.");
			System.exit(-3);
		}
		
		// read lines
		String line = null;
		ArrayList<String> lines = new ArrayList<String>();
		try {
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			System.err.println("I/O Error occurred in reading line " + (lines.size()+1));
			System.exit(-3);
		}
		
		// check format
		if (lines.size() != 4) {
			System.err.println("Sorry, a valid file must have 4 lines.");
			System.err.println("These lines are pause, tempo, tones, and beats");
			System.err.println("Exiting.");
			System.exit(-3);
		}
		
		// write data to internal store
		w.setPause(lines.get(0));
		w.setTempo(lines.get(1));
		w.setTones(lines.get(2));
		w.setBeats(lines.get(3));
	}
}
