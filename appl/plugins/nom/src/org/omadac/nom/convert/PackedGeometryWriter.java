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

import java.io.DataOutput;
import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKBConstants;

public class PackedGeometryWriter
{
    private DataOutput os;
    
    public PackedGeometryWriter()
    {        
    }
    
    public void setOutput(DataOutput output)
    {
        this.os = output;
    }
    
    public void write(Geometry geom)  throws IOException
    {
        if (os == null)
        {
            throw new IllegalStateException("output stream is not set");
        }
        if (geom instanceof Point)
            writePoint((Point) geom);
        else if (geom instanceof LineString)
            writeLineString((LineString) geom);
        else if (geom instanceof Polygon)
            writePolygon((Polygon) geom);
        else if (geom instanceof MultiPoint)
            writeMultiPoint((MultiPoint) geom);
        else if (geom instanceof MultiLineString)
            writeMultiLineString((MultiLineString) geom);
        else if (geom instanceof MultiPolygon)
            writeMultiPolygon((MultiPolygon) geom);
        else
            throw new IllegalArgumentException(geom.getClass().getName());
    }
    
    private void writePoint(Point point) throws IOException
    {
        os.writeByte(WKBConstants.wkbPoint);
        Coordinate c = point.getCoordinate();
        assert isRounded(c);
        os.writeInt((int)c.x);
        os.writeInt((int)c.y);
        os.writeInt((int)c.z);
    }

    private void writeMultiLineString(MultiLineString multiline) throws IOException
    {
        os.writeByte(WKBConstants.wkbMultiLineString);
        int numLines = multiline.getNumGeometries();
        writeVarInt(numLines);
        for (int i = 0; i < numLines; i++)
        {
            writeLineStringCoordinates(multiline.getGeometryN(i).getCoordinates());
        }
    }
    
    private void writeLineString(LineString line) throws IOException
    {
        os.writeByte(WKBConstants.wkbLineString);
        writeLineStringCoordinates(line.getCoordinates());
    }
    
    private void writePolygon(Polygon face) throws IOException
    {
        os.writeByte(WKBConstants.wkbPolygon);
        writePolygonCoordinates(face);
    }
    
    private void writePolygonCoordinates(Polygon face) throws IOException
    {
        writeVarInt(face.getNumInteriorRing()+1);
        writeLinearRingCoordinates(face.getExteriorRing().getCoordinates());
        
        for (int i = 0; i < face.getNumInteriorRing(); i++ )
        {
            LineString ring = face.getInteriorRingN(i);
            writeLinearRingCoordinates(ring.getCoordinates());
        }
    }
    
    private void writeMultiPoint(MultiPoint multipoint) throws IOException
    {
        os.writeByte(WKBConstants.wkbMultiPoint);
        int numPoints = multipoint.getNumGeometries();
        writeVarInt(numPoints);
        Coordinate[] points = multipoint.getCoordinates();
        for (int i = 0; i < numPoints; i++)
        {
            Coordinate c = points[i];
            assert isRounded(c);
            os.writeInt((int) c.x);
            os.writeInt((int) c.y);
            os.writeInt((int) c.z);
        }
    }
    
    private void writeMultiPolygon(MultiPolygon mp) throws IOException
    {
        os.writeByte(WKBConstants.wkbMultiPolygon);
        int numPolygons = mp.getNumGeometries();
        writeVarInt(numPolygons);
        for (int i = 0; i < numPolygons; i++)
        {
            writePolygonCoordinates((Polygon)mp.getGeometryN(i));
        }
    }
    
    private void writeLineStringCoordinates(Coordinate[] coords) throws IOException
    {
        int length = coords.length;
        writeCoordList(coords, length);        
    }
    
    private void writeLinearRingCoordinates(Coordinate[] coords) throws IOException
    {
        int length = coords.length-1;
        writeCoordList(coords, length);        
    }
    
    private void writeCoordList(Coordinate[] coords, int length) throws IOException
    {
        writeVarInt(length);
        boolean first = true;
        int lastX = 0;
        int lastY = 0;
        int lastZ = 0;
        for (int i = 0; i < length; i++)
        {
            Coordinate c = coords[i];
            assert isRounded(c);
            if (first)
            {
                first = false;
                lastX = (int) c.x;
                lastY = (int) c.y;
                lastZ = (int) c.z;
                os.writeInt((int)c.x);
                os.writeInt((int)c.y);
                os.writeInt((int)c.z);
            }
            else
            {
                int dx = (int)(c.x-lastX);
                int dy = (int)(c.y-lastY);
                int dz = (int)(c.z-lastZ);
                assert dx != 0 || dy != 0;
                writeVarInt(dx);
                writeVarInt(dy);
                writeVarInt(dz);
                lastX = (int)c.x;
                lastY = (int)c.y;
                lastZ = (int)c.z;
            }
        }        
    }
    
    private void writeVarInt(int value) throws IOException
    {
        if (Short.MIN_VALUE < value && value < Short.MAX_VALUE)
        {
            os.writeShort(value);
        }
        else
        {
            os.writeShort(Short.MAX_VALUE);
            os.writeInt(value);
        }
    }
    
    private boolean isRounded(Coordinate c)
    {
        assert (long) c.x == Math.round(c.x);
        assert (long) c.y == Math.round(c.y);
        assert Double.isNaN(c.z) || ((long) c.z == Math.round(c.z));
        return true;
    }
}
