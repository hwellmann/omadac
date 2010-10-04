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

import java.util.ArrayList;
import java.util.List;

public class NomLink extends Feature
{
    private RoadAttributes attr;
    private List<NomJunction> junctions;
    private List<NamedRoadLink> namedRoadLinks;
    private int length;
    
    public NomLink()
    {
        this.junctions = new ArrayList<NomJunction>(2);
    }

    public NomLink(long id)
    {
        super(id);
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public RoadAttributes getAttr()
    {
        return attr;
    }

    public void setAttr(RoadAttributes attr)
    {
        this.attr = attr;
    }
    
    public List<NomJunction> getJunctions()
    {
        return junctions;
    }

    public void setJunctions(List<NomJunction> junctions)
    {
        this.junctions = junctions;
    }

    public NomJunction getNonReferenceNode()
    {
        return junctions.get(1);
    }

    public void setNonReferenceNode(NomJunction nonReferenceNode)
    {
        junctions.set(1, nonReferenceNode);
    }

    public NomJunction getReferenceNode()
    {
        return junctions.get(0);
    }

    public void setReferenceNode(NomJunction referenceNode)
    {
        junctions.set(0, referenceNode);
    }
    
    public List<NamedRoadLink> getNamedRoadLinks()
    {
        return namedRoadLinks;
    }

    public void setNamedRoadLinks(List<NamedRoadLink> namedRoadLinks)
    {
        this.namedRoadLinks = namedRoadLinks;
    }

    @Override
    public String toString()
    {
        return String.format("[id=%d, junctions=%s, geom=%s]", getId(), junctions, getGeometry());
    }
}
