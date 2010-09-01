package com.dormbells.writer;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;
/**
 * XML parser of song data.
 * Code heavily borrowed from Robert Hustead's JavaWorld Article:
 * http://www.javaworld.com/javaworld/jw-08-2000/jw-0804-sax.html
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
 */
public class XMLParser extends DefaultHandler {
	// Local objects to collect data as it comes
	private String toneName;
	private float toneFreq;
	private String noteName;
	private String noteValue;

	// Buffer for collecting data from the "characters" SAX event.
	private CharArrayWriter contents = new CharArrayWriter();
	private String previousOpenTag;
	private XMLReader xr;
	
	private Writer w;
	
	// Override methods of the DefaultHandler class
	// to gain notification of SAX Events.
	//
	// See org.xml.sax.ContentHandler for all available events.
	//
	public void startElement( String namespaceURI,
			String localName,
			String qName,
			Attributes attr ) throws SAXException {
		contents.reset();
		if (localName.equals("tone"))  {
			toneName = null;
			previousOpenTag = localName;
		}
		else if (localName.equals("note")) {
			noteName = null;
			previousOpenTag = localName;
		}
	}
	public void endElement( String namespaceURI,
			String localName,
			String qName ) throws SAXException {
		if (localName.equals("tempo"))
			w.setTempo(Integer.valueOf(contents.toString().trim()));
		else if (localName.equals("pause"))
			w.setPause(Integer.valueOf(contents.toString().trim()));
		else if (localName.equals("name") && previousOpenTag.equals("tone"))
			toneName = contents.toString();
		else if (localName.equals("freq"))
			toneFreq = Float.valueOf(contents.toString().trim());
		else if (localName.equals("tone"))
			Note.addTone(toneName, toneFreq);
		else if (localName.equals("name") && previousOpenTag.equals("note"))
			noteName = contents.toString();
		else if (localName.equals("value"))
			noteValue = contents.toString().trim();
		else if (localName.equals("note"))
			w.addNote(new Note(noteName, noteValue));
		else if (localName.equals("time"))
			w.setTime(Integer.valueOf(contents.toString().trim()));
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		contents.write( ch, start, length );
	}
	
	void parse(String filename) throws FileNotFoundException, IOException, SAXException {
		xr.parse(new InputSource(new FileReader(filename)));
	}

	XMLParser(Writer w) throws SAXException {
		this.w = w;
		xr = XMLReaderFactory.createXMLReader();	// create the SAX 2 parser
		xr.setContentHandler(this);
	}
}
