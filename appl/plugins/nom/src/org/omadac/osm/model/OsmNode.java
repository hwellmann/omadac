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
package org.omadac.osm.model;

import java.util.HashMap;
import java.util.Map;


public class OsmNode
{
    private long id;
    private int longitude;
    private int latitude;
    private Map<String, String> tags = new HashMap<String, String>();
    
    public OsmNode()
    {
    }
    
    public OsmNode(long id, int longitude, int latitude)
    {
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int getLongitude()
    {
        return longitude;
    }

    public void setLongitude(int longitude)
    {
        this.longitude = longitude;
    }

    public int getLatitude()
    {
        return latitude;
    }

    public void setLatitude(int latitude)
    {
        this.latitude = latitude;
    }

    public Map<String, String> getTags()
    {
        return tags;
    }

    public void setTags(Map<String, String> tags)
    {
        this.tags = tags;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
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
        OsmNode other = (OsmNode) obj;
        if (id != other.id)
            return false;
        return true;
    }
}
