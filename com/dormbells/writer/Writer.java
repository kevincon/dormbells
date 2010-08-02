package com.dormbells.writer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

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

	private static final String PORT_NAME = "/dev/ttyUSB0";	// communication port
	private static final int BAUD_RATE = 9600;				// communication speed (baud)
	private static final int ACK = 53;						// Acknowledgment byte sent by MSP430

	// Data streams from serial communication
	private BufferedInputStream in;
	private BufferedOutputStream out;

	// Data to transmit
	private int pause = 33;
	private int tempo = 11633;
	private Tone[] tones = { Tone.G, Tone.G, Tone.A, Tone.G, Tone.C, 
			Tone.B, Tone.R, Tone.G, Tone.G, Tone.A, Tone.G, Tone.D, Tone.C, Tone.R };	// Java, your verbosity is painful sometimes
	private int[] beats = { 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 1 };

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

		in = new BufferedInputStream(sp.getInputStream());
		out = new BufferedOutputStream(sp.getOutputStream(), 128);
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
		out.write(MSB);
		out.write(LSB);
	}

	/**
	 * Send the array of beats over the serial line.  As usual, all 24 higher bits are discarded.
	 * @param arr array of beats to send
	 * @throws IOException
	 */
	private void writeArray(int[] arr) throws IOException {
		byte[] bytes = new byte[arr.length];
		for (int i = 0; i < arr.length; i++)
			bytes[i] = (byte) arr[i];
		out.write(bytes);
	}

	/**
	 * Sends an array of tone frequencies over the serial line.  Frequencies are defined in the Tone Enum.
	 * @param tones array of tones to send
	 * @throws IOException
	 */
	private void writeArray(Tone[] tones) throws IOException {
		byte[] bytes = new byte[tones.length];
		for (int i = 0; i < tones.length; i++)
			bytes[i] = (byte) tones[i].freq;
		out.write(bytes);
	}

	/**
	 * Sends data over the serial line in the appropriate order.  Waits for MSP430 acknowledgment
	 * so as to not overflow its buffer.
	 */
	void send() {
		try {
			writeByte(tones.length);
			writeByte(pause);
			writeArray(tones);
			out.flush();
			while (in.read() != ACK);
			writeInt(tempo);
			writeArray(beats);
			out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Usage cases (implementation not complete):
	 * -i send internal song data (Happy Birthday)
	 * -f take file input for song data (pass filename as second argument)
	 * no arguments will invoke GUI
	 * @param args
	 */
	public static void main(String args[]) {
		Writer w = null;
		try {
			w = new Writer(PORT_NAME);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if (args.length == 1 && args[0].equals("-i")) {
			w.send();
		}
	}
}
