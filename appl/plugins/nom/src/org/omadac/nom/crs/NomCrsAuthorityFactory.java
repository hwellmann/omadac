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
package org.omadac.nom.crs;

import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.geotools.referencing.factory.PropertyAuthorityFactory;
import org.geotools.factory.Hints;
import org.geotools.metadata.iso.citation.Citations;

import java.io.IOException;

public class NomCrsAuthorityFactory extends PropertyAuthorityFactory
{
    private static final String WKT_FILE = "/nom.wkt";


    public NomCrsAuthorityFactory()
            throws IOException
    {

        super(ReferencingFactoryContainer.instance(new Hints(Hints.CRS_AUTHORITY_FACTORY, PropertyAuthorityFactory.class)),
                Citations.fromName("NOM"),
                NomCrsAuthorityFactory.class.getResource(WKT_FILE));
    }


}