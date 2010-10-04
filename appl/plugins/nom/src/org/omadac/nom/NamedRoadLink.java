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
package org.omadac.nom;


public class NamedRoadLink
{
    private int id;

    private NamedRoad namedRoad;

    private NomLink link;

    private long leftRangeId;

    private long rightRangeId;
    
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public NamedRoad getNamedRoad()
    {
        return namedRoad;
    }

    public void setNamedRoad(NamedRoad namedRoad)
    {
        this.namedRoad = namedRoad;
    }

    public NomLink getLink()
    {
        return link;
    }

    public void setLink(NomLink link)
    {
        this.link = link;
    }

    public long getLeftRangeId()
    {
        return leftRangeId;
    }

    public void setLeftRangeId(long leftRangeId)
    {
        this.leftRangeId = leftRangeId;
    }

    public long getRightRangeId()
    {
        return rightRangeId;
    }

    public void setRightRangeId(long rightRangeId)
    {
        this.rightRangeId = rightRangeId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
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
        final NamedRoadLink other = (NamedRoadLink) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
