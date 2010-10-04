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

import java.util.Set;

public class NamedRoad
{
    private int id;

    private AdminRegion order8;
    private AdminRegion order9;
    
    private byte adminClass;
    
    private RoadName roadName;

    private boolean exit;

    private boolean junction;

    private Set<NamedRoadLink> links;

    public Set<NamedRoadLink> getLinks()
    {
        return links;
    }

    public void setLinks(Set<NamedRoadLink> links)
    {
        this.links = links;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }
    
    public byte getAdminClass()
    {
        return adminClass;
    }

    public void setAdminClass(byte adminClass)
    {
        this.adminClass = adminClass;
    }

    public RoadName getRoadName()
    {
        return roadName;
    }

    public void setRoadName(RoadName roadName)
    {
        this.roadName = roadName;
    }

    public boolean isExit()
    {
        return exit;
    }

    public void setExit(boolean exit)
    {
        this.exit = exit;
    }

    public boolean isJunction()
    {
        return junction;
    }

    public void setJunction(boolean junction)
    {
        this.junction = junction;
    }

    public AdminRegion getOrder8()
    {
        return order8;
    }

    public void setOrder8(AdminRegion order8)
    {
        this.order8 = order8;
    }

    public AdminRegion getOrder9()
    {
        return order9;
    }

    public void setOrder9(AdminRegion order9)
    {
        this.order9 = order9;
    }
}
