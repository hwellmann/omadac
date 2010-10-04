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

public class AdminRegion
{    
    private long id;

    private int adminOrder;

    private AdminRegion order0;

    private AdminRegion order1;

    private AdminRegion order2;

    private AdminRegion order8;

    private AdminRegion order9;
    
    private Set<RegionName> names;
    
    private Feature feature;
    
    public Set<RegionName> getNames()
    {
        return names;
    }

    public void setNames(Set<RegionName> names)
    {
        this.names = names;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public int getAdminOrder()
    {
        return adminOrder;
    }

    public void setAdminOrder(int adminOrder)
    {
        this.adminOrder = adminOrder;
    }

    public AdminRegion getOrder0()
    {
        return order0;
    }

    public void setOrder0(AdminRegion order0)
    {
        this.order0 = order0;
    }

    public AdminRegion getOrder1()
    {
        return order1;
    }

    public void setOrder1(AdminRegion order1)
    {
        this.order1 = order1;
    }

    public AdminRegion getOrder2()
    {
        return order2;
    }

    public void setOrder2(AdminRegion order2)
    {
        this.order2 = order2;
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

    public Feature getFeature()
    {
        return feature;
    }

    public void setFeature(Feature feature)
    {
        this.feature = feature;
    }
}
