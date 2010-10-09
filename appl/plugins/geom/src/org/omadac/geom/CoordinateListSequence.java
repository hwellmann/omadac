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
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Implements the CoordinateSequence interface for a wrapped list of coordinates to avoid
 * copying the list to an array. Used in combination with GeometryFactory methods.
 * @author hwellmann
 *
 */
public class CoordinateListSequence implements CoordinateSequence, Cloneable
{
    private List<Coordinate> coords;
    
    public CoordinateListSequence(List<Coordinate> coords)
    {
        this.coords = coords;
    }
    

    @Override
    public Object clone()
    {
        CoordinateListSequence copy;
        try
        {
            copy = (CoordinateListSequence) super.clone();
            ArrayList<Coordinate> dest = new ArrayList<Coordinate>(coords.size());
            dest.addAll(coords);
            copy.coords = dest;
            return copy;
        }
        catch (CloneNotSupportedException exc)
        {
            throw new RuntimeException(exc);
        }
    }

    @Override
    public Envelope expandEnvelope(Envelope env)
    {
        for (Coordinate c : coords)
        {
            env.expandToInclude(c);
        }
        return env;
    }

    @Override
    public Coordinate getCoordinate(int i)
    {
        return coords.get(i);
    }

    @Override
    public void getCoordinate(int i, Coordinate c)
    {
        c.setCoordinate(coords.get(i));       
    }

    @Override
    public Coordinate getCoordinateCopy(int i)
    {
        return new Coordinate(coords.get(i));
    }

    @Override
    public int getDimension()
    {
        return 2;
    }

    @Override
    public double getOrdinate(int i, int ordinateIndex)
    {
        Coordinate c = coords.get(i);
        switch (ordinateIndex)
        {
            case X:
                return c.x;
            case Y:
                return c.y;
            case Z:
                return c.z;
            default:
                throw new IllegalArgumentException("ordinateIndex = " + ordinateIndex);
        }
    }

    @Override
    public double getX(int i)
    {
        return coords.get(i).x;
    }

    @Override
    public double getY(int i)
    {
        return coords.get(i).y;
    }

    @Override
    public void setOrdinate(int index, int ordinateIndex, double value)
    {
        Coordinate c = coords.get(index);
        switch (ordinateIndex)
        {
            case X:
                c.x = value;
                break;
            case Y:
                c.y = value;
                break;
            case Z:
                c.z = value;
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public int size()
    {
        return coords.size();
    }

    @Override
    public Coordinate[] toCoordinateArray()
    {
        Coordinate[] c = new Coordinate[coords.size()];
        c = coords.toArray(c);
        return c;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coords == null) ? 0 : coords.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoordinateListSequence other = (CoordinateListSequence) obj;
        if (coords == null)
        {
            if (other.coords != null)
                return false;
        }
        else if (!coords.equals(other.coords))
            return false;
        return true;
    }    
}
