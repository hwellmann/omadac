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

import com.vividsolutions.jts.geom.Geometry;

public class Poi extends Feature
{
    private NamedRoadLink link;
    private Character side;
    private Integer relativePosition;
    private String houseNumber;
    private String streetName;
    private String language;
    private String postalCode;
    private String phoneNumber;
    private String url;
    
    public Poi()
    {      
    }
    
    public Poi(long id, int featureType, long sourceId, Geometry geometry)
    {
        super(id, featureType, sourceId, geometry);
    }

    public NamedRoadLink getLink()
    {
        return link;
    }

    public void setLink(NamedRoadLink link)
    {
        this.link = link;
    }

    public Character getSide()
    {
        return side;
    }

    public void setSide(Character side)
    {
        this.side = side;
    }

    public Integer getRelativePosition()
    {
        return relativePosition;
    }

    public void setRelativePosition(Integer relativePosition)
    {
        this.relativePosition = relativePosition;
    }

    public String getHouseNumber()
    {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber)
    {
        this.houseNumber = houseNumber;
    }

    public String getStreetName()
    {
        return streetName;
    }

    public void setStreetName(String streetName)
    {
        this.streetName = streetName;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getPostalCode()
    {
        return postalCode;
    }

    public void setPostalCode(String postalCode)
    {
        this.postalCode = postalCode;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}
