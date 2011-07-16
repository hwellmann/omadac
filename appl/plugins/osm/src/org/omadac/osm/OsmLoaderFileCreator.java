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

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.omadac.config.ConfigManager;
import org.omadac.config.OmadacException;
import org.omadac.config.jaxb.OmadacSettings;
import org.omadac.config.jaxb.OsmSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsmLoaderFileCreator implements Runnable
{
    private static Logger log = LoggerFactory.getLogger(OsmLoaderFileCreator.class);
    
    private OmadacSettings config;

    @Override
    public void run()
    {
        OsmSettings osm = config.getImport().getOsm();
        String osmFile = osm.getXmlInput();
        String dumpDir = osm.getDumpDir();

        LoaderFileOsmElementHandler handler = new LoaderFileOsmElementHandler(dumpDir);
        OsmStreamingParser parser = new OsmStreamingParser(handler);
        try
        {
            log.info("parsing {}", osmFile);
            log.info("dumping to {}", dumpDir);
            parser.parseXml(osmFile);
            log.info("finished parsing");
        }
        catch (JAXBException exc)
        {
            throw new OmadacException(exc);
        }
        catch (IOException exc)
        {
            throw new OmadacException(exc);
        }
        catch (XMLStreamException exc)
        {
            throw new OmadacException(exc);
        }
        handler.close();
    }
    
    public void setConfigManager(ConfigManager configManager)
    {
        config = configManager.getConfiguration();
    }
}
