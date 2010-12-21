package com.dormbells.writer;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;

/**
 * XML parser of song data.
 * Code heavily borrowed from Robert Hustead's JavaWorld Article:
 * http://www.javaworld.com/javaworld/jw-08-2000/jw-0804-sax.html
 * 
 * Copyright (C) 2010  DormBells
 * 
 * @author Varun Sampath <vsampath@seas.upenn.edu>
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

public class XMLParser extends DefaultHandler {
	// Local objects to collect data as it comes
	private String noteName;
	private String noteValue;

	// Buffer for collecting data from the "characters" SAX event.
	private CharArrayWriter contents = new CharArrayWriter();
	private XMLReader xr;
	
	private Song song;
	
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
		if (localName.equals("note")) {
			noteName = null; noteValue = null;
		}
	}
	public void endElement( String namespaceURI,
			String localName,
			String qName ) throws SAXException {
		
		if (localName.equals("title"))
			song.setTitle(contents.toString().trim());
		if (localName.equals("tempo"))
			song.setTempo(Integer.valueOf(contents.toString().trim()));
		else if (localName.equals("pause"))
			song.setPause(Integer.valueOf(contents.toString().trim()));
		else if (localName.equals("name"))
			noteName = contents.toString();
		else if (localName.equals("value"))
			noteValue = contents.toString().trim();
		else if (localName.equals("note"))
			song.addNote(new Note(noteName, noteValue));
		else if (localName.equals("time"))
			song.setTime(Integer.valueOf(contents.toString().trim()));
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		contents.write( ch, start, length );
	}
	
	void parse(String filename) throws FileNotFoundException, IOException, SAXException {
		xr.parse(new InputSource(new FileReader(filename)));
	}

	XMLParser(Song song) throws SAXException {
		this.song = song;
		xr = XMLReaderFactory.createXMLReader();	// create the SAX 2 parser
		xr.setContentHandler(this);
	}
}
