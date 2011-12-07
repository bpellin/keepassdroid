package net.temerity.davsync;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PropFindHandler extends DefaultHandler {
	private int level = 0;
	private Boolean ele_getlastmodified = false, gotResponse = false;
	public Date lastModified;

	@Override public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		level++;
		
		/*
		 * We only support a single response element - this indicates a regular file
		 * If multiple response elements are returned, we have a directory listing
		 */
		if( qName.equals("D:response") ) {
			if( gotResponse == false )
				gotResponse = true;
			else
				throw new SAXException("Found multiple response elements");
		}
		
		if( qName.equals("lp1:getlastmodified") ) {
			ele_getlastmodified = true;
		}
	}

	@Override public void endElement(String uri, String localName, String qName) throws SAXException {
		level--;
		if( qName.equals("lp1:getlastmodified") ) {
			ele_getlastmodified = false;
		}
	}

	@Override public void characters(char[] ch, int start, int length) throws SAXException {
		if( ele_getlastmodified == true ) {
			String dateString = new String(ch, start, length);
			DateFormat fmt = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
			try {
	    		lastModified = (Date)fmt.parse(dateString);
	    	} catch( ParseException pe ) {
	    		throw new SAXException("Unable to parse remote timestamp");
	    	}
		}
	}
}
