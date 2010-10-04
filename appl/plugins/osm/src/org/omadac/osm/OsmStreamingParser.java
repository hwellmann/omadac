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
package org.omadac.osm;

import java.io.FileReader;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.omadac.config.OmadacException;

public class OsmStreamingParser
{
    private static final String CONTEXT_PATH = "org.omadac.osm.jaxb";
    private JAXBContext ctx;    
    private OsmElementHandler handler;
    
    public OsmStreamingParser(OsmElementHandler handler)
    {
        this.handler = handler;
    }

    public void parseXml(String fileName) throws JAXBException, IOException, XMLStreamException
    {
        ctx = JAXBContext.newInstance(CONTEXT_PATH);

        XMLInputFactory factory = XMLInputFactory.newInstance();
        FileReader fr = new FileReader(fileName);
        XMLEventReader eventReader = factory.createXMLEventReader(fr);
        EventFilter filter = new EventFilter()
        {
            public boolean accept(XMLEvent event)
            {
                return event.isStartElement();
            }
        };
        XMLEventReader filteredReader = factory.createFilteredReader(eventReader, filter);

        StartElement e = (StartElement) filteredReader.nextEvent();
        if (!"osm".equals(e.getName().getLocalPart()))
        {
            throw new OmadacException("expected <osm> root element");
        }

        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        while (filteredReader.peek() != null)
        {
            JAXBElement<?> elem = (JAXBElement<?>) unmarshaller.unmarshal(eventReader);
            handler.handleElement(elem);
        }
        fr.close();
    }
}
