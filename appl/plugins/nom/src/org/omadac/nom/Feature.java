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


import org.apache.openjpa.persistence.jdbc.Strategy;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author HWellmann
 *
 */
public class Feature implements Cloneable
{
    private long id;
    
    private int featureType;
    
    private long sourceId;
    
    @Strategy("org.omadac.nom.convert.GeometryValueHandler")
    private Geometry geometry;
    
    public Feature()
    {
        
    }
    
    public Feature(long id)
    {
        this.id = id;
    }
    
    public Feature(long id, Geometry geometry)
    {
        this.id = id;
        this.geometry = geometry;
    }
    
    public Feature(long id, int featureType, Geometry geometry)
    {
        this.id = id;
        this.featureType = featureType;
        this.sourceId = id;
        this.geometry = geometry;
    }
    
    public Feature(long id, int featureType, long sourceId, Geometry geometry)
    {
        this.id = id;
        this.featureType = featureType;
        this.sourceId = sourceId;
        this.geometry = geometry;
    }
    
    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }        

    public int getFeatureType()
    {
        return featureType;
    }

    public void setFeatureType(int featureType)
    {
        this.featureType = featureType;
    }

    public long getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(long sourceId)
    {
        this.sourceId = sourceId;
    }

    public Geometry getGeometry()
    {
        return geometry;
    }
    
    public void setGeometry(Geometry geometry)
    {
        this.geometry = geometry;
    }        
    
    /**
     * Returns a deep copy of this object.
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        Feature copy = (Feature) super.clone();
        copy.geometry = (Geometry) geometry.clone();
        return copy;
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
        Feature other = (Feature) obj;
        if (id != other.id)
            return false;
        return true;
    }
    
    
}
