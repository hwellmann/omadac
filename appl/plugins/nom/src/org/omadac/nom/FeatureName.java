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

import java.io.Serializable;




public class FeatureName implements Serializable
{
    private static final long serialVersionUID = 1;

    private long id;
    
    private char featureClass;
    
    private long nameId;
    
    private char nameType;
    
    private AdminRegion region;
    
    public FeatureName()
    {
        
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public char getFeatureClass()
    {
        return featureClass;
    }

    public void setFeatureClass(char featureClass)
    {
        this.featureClass = featureClass;
    }

    public long getNameId()
    {
        return nameId;
    }

    public void setNameId(long nameId)
    {
        this.nameId = nameId;
    }

    public char getNameType()
    {
        return nameType;
    }

    public void setNameType(char nameType)
    {
        this.nameType = nameType;
    }

    public AdminRegion getRegion()
    {
        return region;
    }

    public void setRegion(AdminRegion region)
    {
        this.region = region;
    }

}
