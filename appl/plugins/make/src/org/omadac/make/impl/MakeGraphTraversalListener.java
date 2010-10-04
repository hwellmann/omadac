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
package org.omadac.make.impl;

import static org.omadac.make.Target.Status.CREATING;
import static org.omadac.make.Target.Status.FORCED;
import static org.omadac.make.Target.Status.INCOMPLETE;
import static org.omadac.make.Target.Status.MISSING;
import static org.omadac.make.Target.Status.OUTDATED;
import static org.omadac.make.Target.Status.UPDATING;
import static org.omadac.make.Target.Status.UPTODATE;

import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultEdge;
import org.omadac.make.Target;
import org.omadac.make.Target.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MakeGraphTraversalListener extends TraversalListenerAdapter<Target, DefaultEdge>
{
    private static Logger log = LoggerFactory.getLogger(MakeGraphTraversalListener.class);

    private GraphMakeEngine makeEngine;

    public MakeGraphTraversalListener(GraphMakeEngine makeEngine)
    {
        this.makeEngine = makeEngine;
    }

    @Override
    public void vertexFinished(VertexTraversalEvent<Target> e)
    {
        Target target = e.getVertex();
        Status oldStatus = target.getStatus();
        target.refreshTargetStatus();

        Status newStatus = target.getStatus();

        if (newStatus == UPDATING || newStatus == CREATING)
        {
            newStatus = INCOMPLETE;
        }

        if (oldStatus == FORCED && (newStatus == UPDATING || newStatus == UPTODATE))
        {
            newStatus = OUTDATED;
        }

        int numOutdatedPrerequisites = 0;
        for (Target prereq : makeEngine.getPrerequisites(target))
        {
            if (prereq.getStatus() != UPTODATE)
            {
                numOutdatedPrerequisites++;
            }
        }
        if (numOutdatedPrerequisites != 0 && (newStatus == UPTODATE || newStatus == INCOMPLETE))
        {
            newStatus = OUTDATED;
        }

        if (newStatus != UPTODATE)
        {
            makeEngine.addOutdatedTarget(target);
        }

        assert newStatus == MISSING || newStatus == UPTODATE || newStatus == OUTDATED || 
            newStatus == INCOMPLETE; 

        target.setStatus(newStatus);
        log.info("{} is {}", target, newStatus);
    }
}
