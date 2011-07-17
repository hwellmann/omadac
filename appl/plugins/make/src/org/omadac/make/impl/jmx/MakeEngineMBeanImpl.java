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
package org.omadac.make.impl.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.omadac.engine.Status;
import org.omadac.make.ComplexTarget;
import org.omadac.make.Target;
import org.omadac.make.impl.GraphMakeEngine;

/**
 * Make engine MBean implementation for managing the make engine via JMX.
 * @author hwellmann
 *
 */
public class MakeEngineMBeanImpl implements MakeEngineMXBean
{
    private GraphMakeEngine engine;

    public MakeEngineMBeanImpl(GraphMakeEngine engine)
    {
        this.engine = engine;
    }

    @Override
    public int getNumTargets()
    {
        return engine.getTargets().size();
    }

    @Override
    public List<String> getGoals()
    {
        List<String> goals = new ArrayList<String>();
        for (Target target : engine.getGoals())
        {
            goals.add(target.getName());
        }
        return goals;
    }

    @Override
    public List<String> getForcedTargets()
    {
        return Collections.singletonList("dummy");
    }

    @Override
    public List<TargetStatus> getStatus()
    {
        Collection<Target> targets = engine.getTargets();
        List<TargetStatus> statusList = new ArrayList<TargetStatus>(targets.size());
        for (Target target : targets)
        {
            statusList.add(getTargetStatus(target));
        }
        return statusList;
    }

    private TargetStatus getTargetStatus(Target target)
    {
        String name = target.getName();
        boolean complex = target instanceof ComplexTarget;
        int numSubtargets = 0;
        int numCompletedSubtargets = 0;
        Status status = target.getStatus();
        if (complex)
        {
            // TODO dummy values
            numSubtargets = 49;
            numCompletedSubtargets = 17;            
        }
        TargetStatus targetStatus = new TargetStatus(name, status, complex, numSubtargets, numCompletedSubtargets);
        return targetStatus;
    }
}
