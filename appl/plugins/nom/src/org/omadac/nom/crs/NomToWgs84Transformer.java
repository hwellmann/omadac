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

import org.omadac.config.OmadacException;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

public class NomToWgs84Transformer
{
    private MathTransform transform;

    public NomToWgs84Transformer()
    {
        findCoordinateTransform();
    }

    public MathTransform findCoordinateTransform()
    {
        try
        {
            transform = CRS.findMathTransform(NomCrs.NOM,
                    DefaultGeographicCRS.WGS84);
            return transform;
        }
        catch (FactoryException exc)
        {
            throw new OmadacException(exc);
        }
    }

    public MathTransform getCoordinateTransform()
    {
        return transform;
    }

    public Geometry transformGeometry(Geometry geom)
    {
        try
        {
            Geometry transformed = JTS.transform(geom, transform);
            return transformed;
        }
        catch (TransformException exc)
        {
            throw new OmadacException(exc);
        }
    }

}
