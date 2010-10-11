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
/**
 * 
 */
package org.omadac.nom;

import java.util.HashSet;
import java.util.Set;

public class NomJunction extends Feature
{
    private int x;

    private int y;

    private Integer z;

    private int zLevel;

    private Set<NomLink> links = new HashSet<NomLink>();

    public NomJunction()
    {
    }

    public NomJunction(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public NomJunction(long id, int x, int y)
    {
        super(id);
        this.x = x;
        this.y = y;
    }

    public int getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getY()
    {
        return y;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public Integer getZ()
    {
        return z;
    }

    public void setZ(Integer z)
    {
        this.z = z;
    }

    public int getZlevel()
    {
        return zLevel;
    }

    public void setZlevel(int level)
    {
        zLevel = level;
    }

    public Set<NomLink> getLinks()
    {
        return links;
    }

    public void setLinks(Set<NomLink> links)
    {
        this.links = links;
    }

    @Override
    public String toString()
    {
        return String.format("[id=%d, x=%d, y=%d]", getId(), x, y);
    }
}
