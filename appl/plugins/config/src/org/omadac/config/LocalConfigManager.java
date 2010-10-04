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
package org.omadac.config;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.File;
import java.net.URL;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.omadac.config.jaxb.OmadacSettings;
import org.xml.sax.SAXException;

public class LocalConfigManager implements ConfigManager
{
    private static final String CONFIG_SCHEMA = "/xsd/configuration.xsd";

    private static final String CONTEXT_PATH = "org.omadac.config.jaxb";

    private OmadacSettings config;

    @Override
    public OmadacSettings getConfiguration()
    {
        if (config == null)
        {
            String fileName = System.getProperty("omadac.config");

            File file = new File(fileName);
            if (!file.exists())
            {
                String msg = String.format("configuration file %s does not exist", fileName);
                throw new OmadacException(msg);
            }

            JAXBElement<OmadacSettings> elem = parseXml(fileName);
            config = elem.getValue();
        }
        return config;
    }

    @Override
    public OmadacSettings getConfiguration(UUID uuid)
    {
        throw new UnsupportedOperationException();
    }

    private JAXBElement<OmadacSettings> parseXml(String fileName)
    {
        try
        {
            URL resource = LocalConfigManager.class.getResource(CONFIG_SCHEMA);
            Schema schema = getSchema(resource);

            SAXParserFactory pf = SAXParserFactory.newInstance();
            pf.setSchema(schema);
            pf.setNamespaceAware(true);

            JAXBContext ctx = JAXBContext.newInstance(CONTEXT_PATH);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            unmarshaller.setSchema(schema);

            @SuppressWarnings("unchecked")
            JAXBElement<OmadacSettings> result = (JAXBElement<OmadacSettings>) 
                unmarshaller.unmarshal(new File(fileName));
            return result;
        }
        catch (SAXException exc)
        {
            throw new OmadacException(exc);
        }
        catch (JAXBException exc)
        {
            throw new OmadacException(exc);
        }
    }

    private Schema getSchema(URL url) throws SAXException
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(url);
        return schema;
    }
}
