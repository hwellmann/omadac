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

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.persistence.EntityManagerFactory;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;
import org.omadac.jpa.JpaUtil;
import org.omadac.make.Action;
import org.omadac.make.ExecutionContext;
import org.omadac.make.JobManager;
import org.omadac.make.MakeEngine;
import org.omadac.make.MakeException;
import org.omadac.make.NoOpTarget;
import org.omadac.make.Target;
import org.omadac.make.Target.Status;
import org.omadac.make.impl.dot.MakeGraphDotWriter;
import org.omadac.make.impl.jmx.MakeEngineMBeanImpl;
import org.omadac.make.impl.jmx.MakeEngineMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Make engine implementation keeping the dependency graph in memory. 
 * @author hwellmann
 *
 */
public class GraphMakeEngine implements MakeEngine
{
    private static Logger log = LoggerFactory.getLogger(GraphMakeEngine.class);

    /** Job manager for executing actions. */
    private JobManager manager;
    
    /** Execution context for all targets. */
    private ExecutionContext context;

    /** File name of DOT file for rendering dependency graph. */
    private String dotOutput;
    
    /* Queue of targets to be processed. */
    private BlockingQueue<Target> pendingTargets;

    /** Default target which depends on all goals. */
    private Target defaultTarget;
    
    /** Are all targets up to date? */
    private boolean allUpToDate;

    /** Dependency graph. */
    private MakeGraph graph;

    /** Constructs a make engine with an empty graph. */
    public GraphMakeEngine()
    {
        graph = new MakeGraph();
        pendingTargets = new LinkedBlockingQueue<Target>();
        defaultTarget = new DefaultTarget();
    }
    
    @Override
    public void setJobManager(JobManager jobManager)
    {
        this.manager = jobManager;
    }
        
    /**
     * Used by Service Component Runtime to inject execution context.
     * @param executionContext
     */
    public void setExecutionContext(ExecutionContext executionContext)
    {
        this.context = executionContext;
    }
   
    @Override
    public void setDotOutput(String fileName)
    {
        this.dotOutput = fileName;
    }
    
    @Override
    public void addDependency(Target target, Target prerequisite)
    {
        graph.addVertex(target);
        graph.addVertex(prerequisite);
        graph.addEdge(target, prerequisite);
    }

    @Override
    public Collection<Target> getPrerequisites(Target target)
    {
        List<Target> deps = Graphs.successorListOf(graph, target);
        return deps;
    }


    @Override
    public Collection<Target> getDependents(Target target)
    {
        List<Target> deps = Graphs.predecessorListOf(graph, target);
        return deps;
    }

    @Override
    public void addGoal(Target target)
    {
        graph.addVertex(target);
        addDependency(defaultTarget, target);
    }

    @Override
    public void addForcedTarget(Target target)
    {
        target.setStatus(Status.FORCED);
    }

    public Collection<Target> getGoals()
    {
        return getPrerequisites(defaultTarget);
    }

    @Override
    public Collection<Target> getTargets()
    {
        return Collections.unmodifiableSet(graph.vertexSet());
    }

    @Override
    public void make()
    {
        createExecutionContext();
        registerMBean();        
        drawGraph();
        
        /*
         * Retrieve last known target status from persistent storage and update status.
         * This is done within a single transaction for all targets. 
         */
        computeTargetStatus();
        EntityManagerFactory emf = context.getEngineEntityManagerFactory();
        JpaUtil.getCurrentEntityManager(emf).getTransaction().commit();
                
        updateTargets();
    }

    public void onCompleted(Action action)
    {
        Target target = action.getTarget();
        if (target.getExecutionContext() == null)
        {
            target.setExecutionContext(context);
        }
        target.setStatus(Status.COMPLETED);
        pendingTargets.add(target);
        for (Target dep : getDependents(target))
        {
            log.debug("adding {} to queue", dep.getName());
            pendingTargets.add(dep);
        }
    }

    public void onError(Action action)
    {
        Target target;
        if (action == null)
        {
            target = new NoOpTarget("<error>"); 
        }
        else
        {
            target = action.getTarget();
        }
        target.setStatus(Status.ERROR);
        pendingTargets.add(target);
    }

    /**
     * Propagate execution context to all targets.
     */
    private void createExecutionContext()
    {
        for (Target target : graph.vertexSet())
        {
            target.setExecutionContext(context);
        }
    }

    /**
     * Registers an MBean for monitoring the make engine via JMX.
     */
    private void registerMBean()
    {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        MakeEngineMBeanImpl beanImpl = new MakeEngineMBeanImpl(this);
        try
        {
            ObjectName objectName = new ObjectName("org.omadac:type=MakeEngine");
            StandardMBean mBean = new StandardMBean(beanImpl, MakeEngineMXBean.class, true);
            server.registerMBean(mBean, objectName);
        }
        catch (JMException exc)
        {
            log.error("error registering MakeEngine MBean", exc);
        }
    }
    

    /**
     * Renders the make graph to a DOT file.
     */
    private void drawGraph()
    {
        if (dotOutput != null)
        {
            MakeGraphDotWriter makeGraphDotWriter = new MakeGraphDotWriter(graph, dotOutput);
            makeGraphDotWriter.writeDotFile();
        }
    }
    

    /**
     * Computes the current status of all targets based on the persisted value and current
     * dependencies.
     */
    private void computeTargetStatus()
    {
        DepthFirstIterator<Target, DefaultEdge> it = 
            new DepthFirstIterator<Target, DefaultEdge>(graph, defaultTarget);
            
        MakeGraphTraversalListener listener = new MakeGraphTraversalListener(this);
        it.addTraversalListener(listener);
        while (it.hasNext())
        {
            it.next();
        }
    }

    /**
     * Updates all targets, taking one target at a time from the pending target queue and
     * processing it, until the default target is up-to-date or the queue is empty.
     */
    private void updateTargets()
    {
        assert !pendingTargets.isEmpty();
        Target target = pendingTargets.peek();
        
        if (target instanceof DefaultTarget)
        {
            log.info("all targets up to date");
            return;
        }

        assert manager != null;
        manager.addActionListener(this);
        manager.start();

        allUpToDate = false;
        while (!allUpToDate)
        {
            try
            {
                target = pendingTargets.take();
            }
            catch (InterruptedException exc)
            {
                throw new MakeException(exc);
            }

            log.info("pending target {} is {} ", target.getName(), target.getStatus());            
            
            if (target.getStatus() == Status.ERROR)
            {
                log.error("error in target {}, terminating make engine", target.getName());
                break;
            }
            else if (target.getStatus() == Status.COMPLETED)
            {
                target.setStatus(Status.UPTODATE);
                target.saveStatus();
            }
            else
            {     
                updateTarget(target);
            }
        }
        if (allUpToDate)
        {
            log.info("completed <default>");
        }
        manager.stop();
        manager.removeActionListener(this);
    }

    /**
     * Updates the given target, provided its prerequisites are up to date.
     * @param target target to be updated
     */
    private void updateTarget(Target target)
    {
        Status newStatus = getNewStatus(target);
        if (newStatus != null)
        {
            if (canBuild(target))
            {
                if (target instanceof DefaultTarget)
                {
                    target.setStatus(Status.UPTODATE);
                    allUpToDate = true;
                }
                else
                {
                    submitTargetAction(target, newStatus);
                }
            }
        }
    }

    /**
     * Updates the target status and submits the updating action to the job manager.
     * @param target    target to be updated
     * @param newStatus new status of target
     */
    private void submitTargetAction(Target target, Status newStatus)
    {
        target.setStatus(newStatus);
        target.saveStatus();
        Action action = target.getAction();
        manager.submitAction(action);
    }

    /**
     * Computes the new status of the target.
     * @param target
     * @return
     */
    private Status getNewStatus(Target target)
    {
        assert target.getStatus() != null : target.getName();
        switch (target.getStatus())
        {
            case INCOMPLETE:
                return Status.INCOMPLETE;
                
            case MISSING:
                return Status.CREATING;
            
            case OUTDATED:
                return Status.UPDATING;
                
            default:
                return null;
        }
    }

    /**
     * Adds an outdated target to the pending queue, provided its prerequisites are up to date.
     * @param target  outdated target
     */
    void addOutdatedTarget(Target target)
    {
        if (canBuild(target))
        {
            pendingTargets.add(target);
        }
    }

    /**
     * Checks if a target is ready to be updated.
     * @param target  given target
     * @return true iff all prerequisites are up to date
     */
    private boolean canBuild(Target target)
    {
        for (Target prerequisite : getPrerequisites(target))
        {
            if (prerequisite.getStatus() != Status.UPTODATE)
            {
                log.info("cannot make {} since {} is {}",
                    new Object[] {target, prerequisite, prerequisite.getStatus()});
                return false;
            }
        }
        return true;
    }
}
