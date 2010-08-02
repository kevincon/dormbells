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


public class Writer {
	
	private static final String PORT_NAME = "/dev/ttyUSB0";
	private static final int BAUD_RATE = 9600;
	private static final int SIGNAL = 53;
	
	private BufferedInputStream in;
	private BufferedOutputStream out;

	private int pause = 33;
	private int tempo = 11633;
	private Tone[] tones = { Tone.G, Tone.G, Tone.A, Tone.G, Tone.C, 
			Tone.B, Tone.R, Tone.G, Tone.G, Tone.A, Tone.G, Tone.D, Tone.C, Tone.R };	// Java, your verbosity is painful sometimes
	private int[] beats = { 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 1 };
	
	public Writer(String portName) throws NoSuchPortException, PortInUseException, 
	UnsupportedCommOperationException, IOException {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);
		SerialPort sp = (SerialPort) commPort;
		sp.setSerialPortParams(BAUD_RATE,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		
		in = new BufferedInputStream(sp.getInputStream());
		out = new BufferedOutputStream(sp.getOutputStream(), 128);
	}
	
	private void writeByte(int num) throws IOException {
		out.write(num);
	}
	
	private void writeInt(int num) throws IOException {
		int LSB = num & 0xFF;
		int MSB = (num >> 8) & 0xFF;
		out.write(MSB);
		out.write(LSB);
	}
	
	private void writeArray(int[] arr) throws IOException {
		byte[] bytes = new byte[arr.length];
		for (int i = 0; i < arr.length; i++)
			bytes[i] = (byte) arr[i];
		out.write(bytes);
	}
	
	private void writeArray(Tone[] tones) throws IOException {
		byte[] bytes = new byte[tones.length];
		for (int i = 0; i < tones.length; i++)
			bytes[i] = (byte) tones[i].freq;
		out.write(bytes);
	}
	
	void send() {
		try {
		writeByte(tones.length);
		writeByte(pause);
		writeArray(tones);
		out.flush();
		while (in.read() != SIGNAL);
		writeInt(tempo);
		writeArray(beats);
		out.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
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
