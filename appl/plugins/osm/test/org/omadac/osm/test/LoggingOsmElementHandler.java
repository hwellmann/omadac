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
package org.omadac.osm.test;

import org.omadac.osm.AbstractOsmElementHandler;
import org.omadac.osm.jaxb.Bounds;
import org.omadac.osm.jaxb.Node;
import org.omadac.osm.jaxb.NodeRef;
import org.omadac.osm.jaxb.Relation;
import org.omadac.osm.jaxb.RelationMember;
import org.omadac.osm.jaxb.Way;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingOsmElementHandler extends AbstractOsmElementHandler
{
    private static Logger log = LoggerFactory.getLogger(LoggingOsmElementHandler.class);
    
    protected void handleNode(Node node)
    {
        log.info(String.format("Node[id=%d, lat=%f, lon=%f]", 
                    node.getId(), node.getLat(), node.getLon()));
    }

    protected void handleWay(Way way)
    {
        log.info("Way[id={}]", way.getId());
        for (NodeRef node : way.getNd())
        {
            log.info("  node={}", node.getRef());
        }
    }

    @Override
    protected void handleBounds(Bounds bounds)
    {
        log.info(bounds.toString());
    }

    @Override
    protected void handleRelation(Relation relation)
    {
        log.info("Relation[id={}]", relation.getId());
        for (RelationMember member : relation.getMember())
        {
            log.info(String.format("  member=[ref=%d, type=%s, role=%s]" , member.getRef(), member.getType(), member.getRole()));
        }
    }

}
