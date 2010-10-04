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
package org.omadac.nom.convert;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.omadac.geom.CoordinateListSequence;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKBConstants;

public class PackedGeometryReader
{
    private DataInput is;
    
    private GeometryFactory geometryFactory;
    
    
    public PackedGeometryReader()
    {
        geometryFactory = new GeometryFactory();
    }
    
    public void setInput(DataInput input)
    {
        this.is = input;
    }
    
    public Geometry readGeometry()  throws IOException
    {
        Geometry geom = null;
        int type = is.readByte();
        switch (type)
        {
            case WKBConstants.wkbPoint:
                geom = readPoint();
                break;
            case WKBConstants.wkbLineString:
                geom = readLineString();
                break;
            case WKBConstants.wkbPolygon:
                geom = readPolygon();
                break;
            case WKBConstants.wkbMultiPoint:
                geom = readMultiPoint();
                break;
            case WKBConstants.wkbMultiLineString:
                geom = readMultiLineString();
                break;
            case WKBConstants.wkbMultiPolygon:
                geom = readMultiPolygon();
                break;
            default:
                throw new IOException("unexpected WKB type in packed geometry");
        }
        return geom;
    }
    
    private Point readPoint() throws IOException
    {
        Coordinate c = new Coordinate();
        c.x = is.readInt();
        c.y = is.readInt();
        c.z = is.readInt();
        return geometryFactory.createPoint(c);
    }

    private LineString readLineString() throws IOException
    {
        CoordinateSequence points = readLineStringCoordinates();
        return geometryFactory.createLineString(points);
    }
    
    private Polygon readPolygon() throws IOException
    {
        Polygon face = null;
        int numRings = readVarInt();
        if (numRings > 0)
        {
            CoordinateSequence ring = readLinearRingCoordinates();
            LinearRing shell = geometryFactory.createLinearRing(ring);
            LinearRing[] holes = new LinearRing[numRings-1];
            for (int i = 0; i < numRings-1; i++)
            {
                ring = readLinearRingCoordinates();
                holes[i] = geometryFactory.createLinearRing(ring);
            }
            face = geometryFactory.createPolygon(shell, holes);
        }
        return face;
    }
    
    private MultiPoint readMultiPoint() throws IOException
    {
        int numPoints = readVarInt();
        Point[] points = new Point[numPoints];
        for (int i = 0; i < numPoints; i++)
        {
            points[i] = readPoint();
        }
        MultiPoint multipoint = geometryFactory.createMultiPoint(points);
        return multipoint;
    }
    
    private MultiLineString readMultiLineString() throws IOException
    {
        int numLines = readVarInt();
        LineString[] lines = new LineString[numLines];
        for (int i = 0; i < numLines; i++)
        {
            lines[i] = readLineString();
        }
        MultiLineString multiline = geometryFactory.createMultiLineString(lines);
        return multiline;
    }
    
    private MultiPolygon readMultiPolygon() throws IOException
    {
        int numFaces = readVarInt();
        Polygon[] faces = new Polygon[numFaces];
        for (int i = 0; i < numFaces; i++)
        {
            faces[i] = readPolygon();
        }
        MultiPolygon area = geometryFactory.createMultiPolygon(faces);
        return area;
    }
    
    private CoordinateSequence readLineStringCoordinates() throws IOException
    {
        List<Coordinate> coords = readCoordList();
        return new CoordinateListSequence(coords);
    }
    
    private CoordinateSequence readLinearRingCoordinates() throws IOException
    {
        List<Coordinate> coords = readCoordList();
        Coordinate last = new Coordinate(coords.get(0));
        coords.add(last);
        return new CoordinateListSequence(coords);
    }
    
    private List<Coordinate> readCoordList() throws IOException
    {
        int numPoints = readVarInt();
        List<Coordinate> coords = new ArrayList<Coordinate>(numPoints);
        Coordinate c = new Coordinate();
        c.x = is.readInt();
        c.y = is.readInt();
        c.z = is.readInt();
        coords.add(c);
        double lastX = c.x;
        double lastY = c.y;
        double lastZ = c.z;
        for (int i = 1; i < numPoints; i++)
        {
            c = new Coordinate();
            c.x = lastX + readVarInt();
            c.y = lastY + readVarInt();
            c.z = lastZ + readVarInt();
            lastX = c.x;
            lastY = c.y;
            lastZ = c.z;
            coords.add(c);
        }
        return coords;
    }
    
    private int readVarInt() throws IOException
    {
        int value = is.readShort();
        if (value == Short.MAX_VALUE)
        {
            value = is.readInt();
        }
        return value;
    }
}
