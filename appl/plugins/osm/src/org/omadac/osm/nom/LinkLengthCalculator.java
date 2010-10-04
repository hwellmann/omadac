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
package org.omadac.osm.nom;

import org.omadac.config.OmadacException;
import org.omadac.nom.crs.NomCrs;

import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


public class LinkLengthCalculator
{
    public static int computeLinkLength(Geometry geom)
    {
        Coordinate[] coords = geom.getCoordinates();
        double length = 0;
        for (int i = 1; i < coords.length; i++)
        {
            length += distance(coords[i-1], coords[i]);
        }
        return (int)length;
    }


    private static double distance(Coordinate p, Coordinate q)
    {
        double dist;
        try
        {
            dist = JTS.orthodromicDistance(p, q, NomCrs.NOM);
            double result = dist * 100.0;
            return result;
        }
        catch (TransformException exc)
        {
            throw new OmadacException(exc);
        }
    }
}
