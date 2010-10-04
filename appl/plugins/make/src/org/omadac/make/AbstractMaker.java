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
package org.omadac.make;

import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractMaker implements Runnable
{
    private static final long THREE_SECONDS = 3000;

    protected MakeEngine engine;

    private ComponentContext context;

    protected void activate(ComponentContext componentContext)
    {
        this.context = componentContext;
    }

    protected void setMakeEngine(MakeEngine makeEngine)
    {
        this.engine = makeEngine;
    }

    protected JobManager lookupJobManager(String type)
    {
        JobManager jobManager = findService(JobManager.class, "type", type);
        engine.setJobManager(jobManager);
        return jobManager;
    }

    @Override
    public void run()
    {
        initialize();
        defineGoals();
        defineDependencies();
        defineForcedTargets();
        engine.make();
    }

    protected abstract void initialize();

    protected abstract void defineForcedTargets();

    protected abstract void defineGoals();

    protected abstract void defineDependencies();

    public Collection<Target> getDependencies(String targetName)
    {
        Target target = lookupTarget(targetName);
        return engine.getDependents(target);
    }

    protected Target addGoal(String targetName)
    {
        Target target = lookupTarget(targetName);
        engine.addGoal(target);
        return target;
    }

    protected void addDependency(String targetName, String... dependsOnNameList)
    {
        Target target = lookupTarget(targetName);
        for (String dependsOnName : dependsOnNameList)
        {
            Target dependsOn = lookupTarget(dependsOnName);
            engine.addDependency(target, dependsOn);
        }
    }

    protected void addDependency(Target target, String... dependsOnNameList)
    {
        for (String dependsOnName : dependsOnNameList)
        {
            Target dependsOn = lookupTarget(dependsOnName);
            engine.addDependency(target, dependsOn);
        }
    }

    protected void addDependency(String targetName, Target dependsOn)
    {
        Target target = lookupTarget(targetName);
        engine.addDependency(target, dependsOn);
    }

    protected void addDependency(Target target, Target dependsOn)
    {
        engine.addDependency(target, dependsOn);
    }

    protected void addGoal(Target goal)
    {
        engine.addGoal(goal);
    }

    protected Target lookupTarget(String targetName)
    {
        return findService(Target.class, "name", targetName);
    }

    @SuppressWarnings("unchecked")
    protected <T> T findService(Class<T> clazz, String propName, String propValue)
    {
        BundleContext bc = context.getBundleContext();

        try
        {
            Filter filter = bc.createFilter(String.format(
                "(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, clazz.getName(), propName, propValue));
            ServiceTracker tracker = new ServiceTracker(bc, filter, null);
            tracker.open();
            T service = (T) tracker.waitForService(THREE_SECONDS);
            tracker.close();
            if (service == null)
            {
                throw new IllegalArgumentException("service not found: " + filter);
            }
            return service;
        }
        catch (InvalidSyntaxException exc)
        {
            throw new MakeException(exc);
        }
        catch (InterruptedException exc)
        {
            throw new MakeException(exc);
        }
    }

    protected ComponentContext getContext()
    {
        return context;
    }

    protected void setDotOutput(String fileName)
    {
        engine.setDotOutput(fileName);
    }
}
