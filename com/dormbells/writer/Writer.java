package com.dormbells.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 */
public class Writer {

	private enum Mode {
		GUI,
		File,
		Internal;
	}

	private static final int BAUD_RATE = 2400;				// communication speed (baud)
	private static final boolean DEBUG = false;

	// Data stream from serial communication
	private OutputStream out;

	// Data to transmit
	private int pause = 33;
	private int tempo = 11633;	
	 
	// The Can-Can in D major, one of my favorites from middle school orchestra
	// btw, Java, your verbosity is painful sometimes
	private Tone[] tones = {	
			Tone.D, Tone.D, Tone.E, Tone.G, Tone.F, Tone.E, Tone.A, Tone.A,
			Tone.A, Tone.B, Tone.F, Tone.G, Tone.E, Tone.E, Tone.E, Tone.G,
			Tone.F, Tone.E, Tone.D, Tone.d, Tone.C, Tone.B, Tone.A, Tone.G,
			Tone.F, Tone.E, Tone.D, Tone.D, Tone.E, Tone.G, Tone.F, Tone.E,
			Tone.A, Tone.A, Tone.A, Tone.B, Tone.F, Tone.G, Tone.E, Tone.E,
			Tone.E, Tone.G, Tone.F, Tone.E, Tone.D, Tone.A, Tone.E, Tone.F,
			Tone.D, Tone.D,
			};
	private int[] beats = { 
			2, 2, 1, 1, 1, 1, 2, 2,
			1, 1, 1, 1, 2, 2, 1, 1,
			1, 1, 2, 1, 1, 1, 1, 1,
			1, 1, 2, 2, 1, 1, 1, 1,
			2, 2, 1, 1, 1, 1, 2, 2, 
			1, 1, 1, 1, 1, 1, 1, 1,
			2, 2,
	};

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
		if (DEBUG) System.err.printf("LSB: %x\tMSB: %x\n", LSB, MSB);
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
	 * Sends an array of tone frequencies over the serial line.  Frequencies are defined in the Tone Enum.
	 * @param tones array of tones to send
	 * @throws IOException
	 */
	private void writeArray(Tone[] tones) throws IOException {
		for (int i = 0; i < tones.length; i++) {
			out.write((byte) tones[i].freq);
		}
	}

	/**
	 * Sends data over the serial line in the appropriate order.  Waits for MSP430 acknowledgment
	 * so as to not overflow its buffer.
	 */
	void send() {
		try {
			if (DEBUG) System.err.println("Debug: Writing length");
			writeByte(tones.length);
			if (DEBUG) System.err.println("Debug: Writing pause");
			writeByte(pause);
			if (DEBUG) System.err.println("Debug: Writing tones");
			writeArray(tones);
			if (DEBUG) System.err.println("Debug: Flushing");
			out.flush();
			Thread.sleep(40);	// wait for MSP430 to write to flash
			if (DEBUG) System.err.println("Debug: Writing tempo");
			writeInt(tempo);
			if (DEBUG) System.err.println("Debug: Writing beats");
			writeArray(beats);
			if (DEBUG) System.err.println("Debug: Flushing");
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) { }
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
	 * Usage cases:
	 * no arguments will invoke GUI (not implemented)
	 * if arguments given, one must be comm port
	 * -f take file input for song data (pass filename as next argument) (not implemented)
	 * if no -f, internal mode is assumed
	 * 
	 * @param commPorts list of communication ports
	 * @param args command-line arguments
	 * @return options data for main() to interpret.  [0] is Mode, [1] is comm port, 
	 * and [2] is additional arg if any
	 */
	private static String[] optParse(List<String> commPorts, String args[]) {
		String commPort = null;
		boolean validComm = true;
		String[] returnVal = new String[3];
		if (args.length == 0) {
			// since this is GUI invocation, will have drop down box option
			returnVal[0] = "GUI";
		} 
		else if (args.length == 1) {
			// only argument must be comm port, assume internal mode
			commPort = args[0];
		} 
		else {
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
					if (args[i].equals("-f")) {
						if ((i+1) < args.length) {
							returnVal[0] = "File";
							returnVal[2] = args[i+1];
							break;
						}
						else {
							System.err.println("No input file given, exiting");
							System.exit(-2);
						}
					}
				}
			}
		}
		// check if comm port found is valid
		validComm &= commPorts.contains(commPort);
		if (!validComm) {
			System.err.println("No valid communications port given. Choose from these next time:");
			for (String s : commPorts) System.err.print(s + " ");
			System.err.println();
			System.exit(-1);
		}

		// Assume internal mode unless given otherwise
		if (returnVal[0] == null) returnVal[0] = "Internal";
		returnVal[1] = commPort;
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
			System.exit(-1);
		}

		// Options parsing
		String[] opts = optParse(commPorts, args);
		Mode mode = Mode.valueOf(opts[0]);
		String commPort = opts[1];
		//String input = opts[2];

		try {
			w = new Writer(commPort);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if (mode == Mode.Internal) {
			w.send();
		}
		w.exit();
	}
}
