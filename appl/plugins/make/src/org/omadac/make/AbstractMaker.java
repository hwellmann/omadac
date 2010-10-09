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

/**
 * A maker defines a collection of targets and their dependencies, including goals and forced
 * targets. In the simplest case, a derived class only sets a goal and a number of dependencies.
 * <p>
 * In more complex cases, a derived class may compute the set of targets based on runtime 
 * configuration and some business logic. This is the motivation for designing a maker as a class
 * and not as a kind of makefile with some domain-specific syntax.
 * 
 * @author hwellmann
 *
 */
public abstract class AbstractMaker implements Runnable
{
    /** Timeout for looking up an OSGi service. */
    private static final long THREE_SECONDS = 3000;

    /** Make engine that will run this maker. */
    protected MakeEngine engine;

    /** OSGi component context. */
    private ComponentContext context;

    protected void activate(ComponentContext componentContext)
    {
        this.context = componentContext;
    }

    /** Used by Service Component Runtime to inject make engine. */
    protected void setMakeEngine(MakeEngine makeEngine)
    {
        this.engine = makeEngine;
    }

    /** 
     * Looks up job manager by type and injects it into make engine.
     * @param type job manager type 
     */
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

    /**
     * Initializes the maker (e.g. by evaluating runtime configuration).
     */
    protected abstract void initialize();

    /** Defines forced targets. Implementations shall call addForcedTarget(). */
    protected abstract void defineForcedTargets();

    /** Defines goals. Implementations shall call addGoal(). */
    protected abstract void defineGoals();

    /** Defines target dependencies. Implementations shall call addDependency(). */
    protected abstract void defineDependencies();

    /**
     * Returns the targets depending on the given one.
     * @param targetName  prerequisite target name
     * @return dependent targets
     */
    public Collection<Target> getDependencies(String targetName)
    {
        Target target = lookupTarget(targetName);
        return engine.getDependents(target);
    }

    /**
     * Adds a forced target
     * @param targetName   forced target name
     * @return target with given name
     */
    protected Target addForcedTarget(String targetName)
    {
        Target target = lookupTarget(targetName);
        engine.addForcedTarget(target);
        return target;
    }
    
    /**
     * Adds a goal.
     * @param targetName  name of goal target
     * @return target with given name
     */
    protected Target addGoal(String targetName)
    {
        Target target = lookupTarget(targetName);
        engine.addGoal(target);
        return target;
    }

    /**
     * Adds a list of dependencies for a given target and a number of prerequisites.
     * {@code addDependency(a, b, c, d)} is equivalent to
     * <pre>
     * addDependency(a, b); 
     * addDependency(a, c); 
     * addDependency(a, d);
     * </pre> 
     * @param targetName          name of target
     * @param dependsOnNameList   names of prerequisites
     */
    protected void addDependency(String targetName, String... dependsOnNameList)
    {
        Target target = lookupTarget(targetName);
        for (String dependsOnName : dependsOnNameList)
        {
            Target dependsOn = lookupTarget(dependsOnName);
            engine.addDependency(target, dependsOn);
        }
    }

    /**
     * Adds a list of dependencies for a given target and a number of prerequisites.
     * {@code addDependency(a, b, c, d)} is equivalent to
     * <pre>
     * addDependency(a, b); 
     * addDependency(a, c); 
     * addDependency(a, d);
     * </pre> 
     * @param target              a target with prerequisites
     * @param dependsOnNameList   names of prerequisites
     */
    protected void addDependency(Target target, String... dependsOnNameList)
    {
        for (String dependsOnName : dependsOnNameList)
        {
            Target dependsOn = lookupTarget(dependsOnName);
            engine.addDependency(target, dependsOn);
        }
    }

    /**
     * Adds a dependency for a target on a given prerequisite.
     * @param targetName   name of target
     * @param dependsOn    prerequisite target
     */
    protected void addDependency(String targetName, Target dependsOn)
    {
        Target target = lookupTarget(targetName);
        engine.addDependency(target, dependsOn);
    }

    /**
     * Adds a dependency for a target on a given prerequisite.
     * @param targetName   target
     * @param dependsOn    prerequisite target
     */
    protected void addDependency(Target target, Target dependsOn)
    {
        engine.addDependency(target, dependsOn);
    }

    protected void addGoal(Target goal)
    {
        engine.addGoal(goal);
    }

    /**
     * Looks up a target by name in the service registry.
     * @param targetName   target name
     * @return target service
     */
    protected Target lookupTarget(String targetName)
    {
        return findService(Target.class, "name", targetName);
    }

    /**
     * Finds a service of a given class such that a given property matches a given value. If no
     * such service is available the method wait a certain amount of time and then either returns
     * the matching service or throws an runtime exception.
     * @param <T>    service class
     * @param clazz  service class
     * @param propName  property name for selecting a service
     * @param propValue property value for selecting a service
     * @return  matching service
     */
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

    /**
     * Returns the OSGi component context.
     * @return component context
     */
    protected ComponentContext getContext()
    {
        return context;
    }

    /**
     * Sets the file name for rendering the dependency graph in DOT syntax.
     * @param fileName  dot file name
     */
    protected void setDotOutput(String fileName)
    {
        engine.setDotOutput(fileName);
    }
}
