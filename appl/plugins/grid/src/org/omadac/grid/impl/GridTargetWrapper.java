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
package org.omadac.grid.impl;

import static org.omadac.make.Target.Status.OUTDATED;
import static org.omadac.make.Target.Status.UPDATING;
import static org.omadac.make.Target.Status.UPTODATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gridgain.grid.GridException;
import org.gridgain.grid.GridJob;
import org.gridgain.grid.GridJobResult;
import org.gridgain.grid.GridTaskSplitAdapter;
import org.omadac.make.ComplexTarget;
import org.omadac.make.NoOpTarget;
import org.omadac.make.Target;
import org.omadac.make.Target.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridTargetWrapper extends GridTaskSplitAdapter<Target, String>
{
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(GridTargetWrapper.class);

    @Override
    public String reduce(List<GridJobResult> results) throws GridException
    {
        return results.get(0).getData();
    }

    @Override
    protected Collection<? extends GridJob> split(int gridSize, Target target) throws GridException
    {
        if (target.getParent() == null)
        {
            GridJob job = new TargetGridJobAdapter(target);
            return Collections.singleton(job);
        }
        else
        {
            ComplexTarget complexTarget = (ComplexTarget) target;
            List<Target> subtargets = complexTarget.split();
            if (subtargets.isEmpty())
            {
                log.warn("target " + target.getName() + " has no subtargets");
                // GridGain expects split() to return a non-empty list.
                subtargets.add(new NoOpTarget());
            }
            List<GridJob> gridJobs = new ArrayList<GridJob>();
            Status parentStatus = complexTarget.getStatus();
            if (parentStatus == UPDATING)
            {
                complexTarget.getAction().run();
            }
            for (Target subtarget : subtargets)
            {
                subtarget.setParent(complexTarget);
                subtarget.setExecutionContext(GridJobManager.getExecutionContext());
                subtarget.refreshTargetStatus();
                if (parentStatus == UPDATING)
                {
                    subtarget.setStatus(OUTDATED);
                    subtarget.saveStatus();
                   
                }
                if (subtarget.getStatus() == UPTODATE)
                {
                    continue;
                }
                
                GridJob gridJob = new TargetGridJobAdapter(subtarget);
                gridJobs.add(gridJob);
            }
            complexTarget.getEngineEntityManager().getTransaction().commit();
            return gridJobs;          
        }
    }
}
