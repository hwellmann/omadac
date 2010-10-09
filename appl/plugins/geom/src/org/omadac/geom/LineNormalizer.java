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

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Converts a line string into a list of normalized line strings. The union of the normalized
 * line is equal to the original line. Each normalized part is simple and not closed. Normalized
 * parts do not contain duplicate points. The coordinates of the original line are assumed to
 * be integral. The coordinates of the normalized parts are also integral, which may result in 
 * rounding errors when the original line has a self-intersection with non-integral coordinates. 
 * 
 * @author hwellmann
 *
 */
public class LineNormalizer
{
    private RoundingCoordinateFilter rounder;
    
    public LineNormalizer()
    {
        this.rounder = new RoundingCoordinateFilter();
    }
    
    
    public List<LineString> normalize(LineString lineString)
    {
        LineString line = lineString;
        if (hasCollapsedSegments(line))
        {
            line = removeCollapsedSegments(line);
        }
        ArrayList<LineString> parts = new ArrayList<LineString>();
        if (line.isSimple())
        {
            checkForLoop(line, parts);
        }
        else
        {
            Geometry multiline = line.intersection(line.getEnvelope());
            for (int i = 0; i < multiline.getNumGeometries(); i++)
            {
                LineString part = (LineString) multiline.getGeometryN(i);
                LineString rounded = (LineString) part.getFactory().createGeometry(part);
                rounded.apply(rounder);
                part = removeCollapsedSegments(rounded);
                if (part == null)
                    continue;
                
                assert part.isSimple();                
                checkForLoop(part, parts);
            }
        }
        return parts;
    }

    private boolean hasCollapsedSegments(LineString line)
    {
        Coordinate last = null;
        for (int i = 0; i < line.getNumPoints(); i++)
        {
            Coordinate c = line.getCoordinateN(i);
            if (c.equals(last))
                return true;
            last = c;
        }
        return false;
    }

    private LineString removeCollapsedSegments(LineString line)
    {
        Coordinate last = null;
        int numPoints = line.getNumPoints();
        List<Coordinate> coords = new ArrayList<Coordinate>(numPoints);
        for (int i = 0; i < numPoints; i++)
        {
            Coordinate c = line.getCoordinateN(i);
            if (! c.equals(last))
            {
                coords.add(c);
            }
            last = c;
        }
        if (coords.size() >= 2)
        {
            CoordinateListSequence sequence = new CoordinateListSequence(coords);
            LineString newLine = line.getFactory().createLineString(sequence);
            return newLine;
        }        
        return null;
    }
    
    private void checkForLoop(LineString line, List<LineString> parts)
    {
        if (line.isClosed())
        {
            breakLoop(line, parts);
        }
        else
        {
            parts.add(line);
        }
    }

    private void breakLoop(LineString line, List<LineString> parts)
    {
        Coordinate[] coordinates = line.getCoordinates();
        int numPoints = coordinates.length;
        assert numPoints >= 3;
        
        int mid = coordinates.length/2;
        
        Coordinate[] left = new Coordinate[mid+1];
        Coordinate[] right = new Coordinate[numPoints-mid];
        
        for (int i = 0; i < numPoints; i++)
        {
            if (i <= mid)
            {
                left[i] = new Coordinate(coordinates[i]);
            }
            if (i >= mid)
            {
                right[i-mid] = new Coordinate(coordinates[i]);                
            }
        }
        
        GeometryFactory factory = line.getFactory();
        parts.add(factory.createLineString(left));
        parts.add(factory.createLineString(right));
    }
}
