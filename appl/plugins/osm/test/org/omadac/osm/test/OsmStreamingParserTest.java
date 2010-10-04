/*
 *    Omadac - The Open Map Database Compiler
 *    http://omadac.org
 * 
 *    (C) 2010, Harald Wellmann and Contributors
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.omadac.osm.test;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.omadac.osm.OsmStreamingParser;

import org.junit.Test;
import org.xml.sax.SAXException;


public class OsmStreamingParserTest
{
    @Test
    public void parseOsm() throws SAXException, JAXBException, ParserConfigurationException, IOException, XMLStreamException
    {
        LoggingOsmElementHandler handler = new LoggingOsmElementHandler();
        OsmStreamingParser parser = new OsmStreamingParser(handler);
        parser.parseXml("sample.xml");
    }
}
