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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.omadac.make.Action;
import org.omadac.make.ActionListener;
import org.omadac.make.ComplexTarget;
import org.omadac.make.ExecutionContext;
import org.omadac.make.JobManager;
import org.omadac.make.Target;
import org.omadac.make.Target.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ThreadPoolJobManager implements JobManager
{
    private static Logger log = LoggerFactory.getLogger(ThreadPoolJobManager.class);
    
    private Map<String, Integer> subtargetMap;
    
    private Vector<ActionListener> listeners;

    private ThreadPoolExecutor executor;
    
    private ExecutionContext context;

    private int numThreads;

    public ThreadPoolJobManager()
    {
        this.subtargetMap = new HashMap<String, Integer>();
        this.listeners = new Vector<ActionListener>(1);
    }
    
    protected void setExecutionContext(ExecutionContext executionContext)
    {
        this.context = executionContext;
    }
    
    public void setNumThreads(int numThreads)
    {
        this.numThreads = numThreads;
    }

    @Override
    public void addActionListener(ActionListener listener)
    {
        listeners.add(listener);
    }
    
    @Override
    public void removeActionListener(ActionListener listener)
    {
        listeners.remove(listener);
    }
    
    @Override
    public void start()
    {
        executor = new NotifyingThreadPoolExecutor(numThreads, this);
    }

    @Override
    public void stop()
    {
        executor.shutdown();
        try
        {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        }
        catch (InterruptedException exc)
        {
            log.error("interrupted on shutdown", exc);
        }
    }

    @Override
    public void submitAction(Action action)
    {
        Target target = action.getTarget();
        target.setExecutionContext(context);
        
        if (target instanceof ComplexTarget)
        {
            ComplexTarget complexTarget = (ComplexTarget) target;
            Action complexAction = complexTarget.getAction();
            log.info("submitting job for {}", complexTarget);

            List<Target> subtargets = complexTarget.split();
            List<Action> subactions = new ArrayList<Action>(subtargets.size());
            EntityManager em = complexTarget.getEngineEntityManager();
            for (Target subtarget : subtargets)
            {
                subtarget.setParent(complexTarget);
                subtarget.setExecutionContext(context);
                subtarget.refreshTargetStatus();
                
                if (complexTarget.getStatus() == Status.UPDATING)
                {
                    subtarget.setStatus(Status.OUTDATED);
                    subtarget.saveStatus(em);
                }
                
                if (subtarget.getStatus() == Status.UPTODATE)
                {
                    assert complexTarget.getStatus() == Status.INCOMPLETE;
                    continue;
                }

                Action subaction = subtarget.getAction();
                subaction.setTarget(subtarget);
                subactions.add(subaction);
                
                if (subtarget.getStatus() == Status.MISSING)
                {
                    subtarget.setStatus(Status.CREATING);
                }
                else
                {
                    subtarget.setStatus(Status.UPDATING);
                }
                log.info("submitting job for {}", subtarget);
            }
            if (complexTarget.getStatus() == Status.INCOMPLETE)
            {
                complexTarget.setStatus(Status.UPDATING);
            }
            em.getTransaction().commit();
            
            if (subtargets.isEmpty())
            {
                onCompleted(complexAction);
            }
            else
            {
                submitComplexTargetAction(complexAction, subactions);
            }
        }
        else
        {
            log.info("submitting job for ", target);
            executor.submit(action, action);
        }
    }

    private void submitComplexTargetAction(Action complexAction, List<Action> subactions)
    {
        ComplexTarget complexTarget = (ComplexTarget) complexAction.getTarget();
        
        subtargetMap.put(complexTarget.getName(), subactions.size());
    
        if (complexTarget.getStatus() == Status.UPDATING)
        {
            runComplexTargetAction(complexTarget);
        }
    
        for (Action subaction : subactions)
        {
            executor.submit(subaction, subaction);
        }
    }

    private void runComplexTargetAction(ComplexTarget complexTarget)
    {
        try
        {
            complexTarget.getAction().run();
        }
        // CHECKSTYLE:OFF
        catch (Throwable exc)
        // CHECKSTYLE:ON
        {
            log.error("exception after action execution, shutting down", exc);
            executor.shutdownNow();
            
            onError(null);
        }
        
    }

    @SuppressWarnings("unchecked")
    protected synchronized void afterExecute(Runnable r, Throwable t)
    {
        Future<Action> future = (Future<Action>) r;
        try
        {
            Target target = future.get().getTarget();
            log.info("completed {}", target);
            
            ComplexTarget parent = target.getParent();
            if (parent != null)
            {
                String parentName = parent.getName();
                int numPendingSubtargets = subtargetMap.get(parentName) - 1;
                subtargetMap.put(parentName, numPendingSubtargets);
                if (numPendingSubtargets == 0)
                {
                    parent.setStatus(Status.COMPLETED);
                    onCompleted(parent.getAction());
                }
            }
            else
            {
                onCompleted(target.getAction());
            }
        }
        catch (ExecutionException exc)
        {
            log.error("exception in worker thread, shutting down", exc.getCause());
            executor.shutdownNow();           
            onError(null);
        }
        catch (InterruptedException exc)
        {
            log.error("worker thread interrupted");
        }
        // CHECKSTYLE:OFF
        catch (Throwable exc)
        // CHECKSTYLE:ON
        {
            log.error("exception after action execution, shutting down", exc);
            executor.shutdownNow();            
            onError(null);
        }
    }

    private void onCompleted(Action action)
    {
        for (ActionListener listener : listeners)
        {
            listener.onCompleted(action);
        }
    }
    
    private void onError(Action action)
    {
        for (ActionListener listener : listeners)
        {
            listener.onError(action);
        }
    }
}
