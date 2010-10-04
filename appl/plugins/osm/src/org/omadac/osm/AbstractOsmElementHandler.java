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
package org.omadac.osm;

import javax.xml.bind.JAXBElement;

import org.omadac.osm.jaxb.Bounds;
import org.omadac.osm.jaxb.Node;
import org.omadac.osm.jaxb.Relation;
import org.omadac.osm.jaxb.Way;

abstract public class AbstractOsmElementHandler implements OsmElementHandler
{
    @Override
    public void handleElement(JAXBElement<?> elem)
    {
        Class<?> type = elem.getDeclaredType();
        if (type == Node.class)
            handleNode((Node) elem.getValue());
        else if (type == Way.class)
            handleWay((Way) elem.getValue());
        else if (type == Relation.class)
            handleRelation((Relation) elem.getValue());
        else if (type == Bounds.class)
            handleBounds((Bounds) elem.getValue());
    }

    abstract protected void handleNode(Node node);
    abstract protected void handleWay(Way way);
    abstract protected void handleRelation(Relation relation);
    abstract protected void handleBounds(Bounds bounds);
}
