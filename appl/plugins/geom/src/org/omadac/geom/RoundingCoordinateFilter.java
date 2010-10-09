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
package org.omadac.geom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;

/**
 * Rounds the x, y and z values of a coordinate.
 * @author hwellmann
 *
 */
public class RoundingCoordinateFilter implements CoordinateFilter
{
    @Override
    public void filter(Coordinate coord)
    {
        coord.x = Math.round(coord.x);
        coord.y = Math.round(coord.y);
        coord.z = Math.round(coord.z);
    }
}
