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

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOM Coordinate Reference System for Geotools.
 *
 * @author hwellmann
 *
 */
public class NomCrs
{
    // CHECKSTYLE:OFF   can't make this final
    public static CoordinateReferenceSystem NOM;
    // CHECKSTYLE:ON

    private static Logger log = LoggerFactory.getLogger(NomCrs.class);

    static
    {
        log.info("Init NOM CRS");
        try
        {
            // NOTE: user-defined EPSG codes must be larger than 32767.
            NOM = CRS.decode("NOM:35001");
        }
        catch (FactoryException exc)
        {
            log.error("cannot create NOM CRS {}", exc);
        }
    }
}

