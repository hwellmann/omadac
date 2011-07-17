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

import static org.omadac.engine.Status.UPTODATE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.omadac.engine.Status;
import org.omadac.engine.TargetInfo;
import org.omadac.make.Action;
import org.omadac.make.ActionListener;
import org.omadac.make.ComplexStep;
import org.omadac.make.ComplexTarget;
import org.omadac.make.ExecutionContext;
import org.omadac.make.JobManager;
import org.omadac.make.Step;
import org.omadac.make.Target;
import org.omadac.make.TargetDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local job manager, using a thread pool on the local node.
 * 
 * @author hwellmann
 *
 */
public class ThreadPoolJobManager implements JobManager
{
    private static Logger log = LoggerFactory.getLogger(ThreadPoolJobManager.class);
    
    private TargetDao targetDao;
   
    /**
     * Maps complex targets to the number of pending subtargets. The map gets initialized with
     * the number of subtargets of each complex target. The number gets decremented on completion
     * of each subtarget. When the number is zero, the complex target is completed.
     */
    private Map<String, Integer> subtargetMap;
    
    /**
     * Action listeners to be notified.
     */
    private Vector<ActionListener> listeners;

    /**
     * Executor for running actions.
     */
    private ThreadPoolExecutor executor;
    
    /**
     * Target execution context.
     */
    private ExecutionContext context;

    /** Number of worker threads. */
    private int numThreads;

    public ThreadPoolJobManager()
    {
        this.subtargetMap = new HashMap<String, Integer>();
        this.listeners = new Vector<ActionListener>(1);
    }
    
    /**
     * Used by Service Component Runtime to inject execution context.
     * @param executionContext
     */
    public void setExecutionContext(ExecutionContext executionContext)
    {
        this.context = executionContext;
    }
    
    public void setNumThreads(int numThreads)
    {
        this.numThreads = numThreads;
    }
    
    

    public void setTargetDao(TargetDao targetDao)
    {
        this.targetDao = targetDao;
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
    public void submitTarget(Target target)
    {
        Action action = getAction(target);
        submitAction(action);
    }

    @Override
    public void submitAction(Action action)
    {
        Target target = action.getTarget();
        target.setExecutionContext(context);
        
        Step step = target.getStep();
        if (step != null && target instanceof ComplexTarget) {
            ComplexStep complexStep = (ComplexStep) step;
            processComplexStep(action, complexStep);                
            return;
        }
        
        if (target instanceof ComplexTarget)
        {
            ComplexTarget complexTarget = (ComplexTarget) target;
            Action complexAction = complexTarget.getAction();
            log.info("submitting job for {}", complexTarget);

            /*
             * Create subtargets and check status for each subtarget. There may be a large
             * number of subtargets, so we update all subtargets statuses within a single
             * transaction.
             */
            List<Target> subtargets = complexTarget.split();
            List<Action> subactions = new ArrayList<Action>(subtargets.size());                      
            for (Target subtarget : subtargets)
            {
                subtarget.setParent(complexTarget);
                subtarget.setExecutionContext(context);
                targetDao.refreshTargetStatus(subtarget);
                
                if (complexTarget.getStatus() == Status.UPDATING)
                {
                    subtarget.setStatus(Status.OUTDATED);
                    targetDao.saveStatus(subtarget);
                }
                
                /*
                 * If the subtarget is up to date then the complex target must be incomplete, i.e.
                 * a previous run of the make engine was interrupted. In this case, we leave the
                 * subtarget unchanged.
                 */
                if (subtarget.getStatus() == Status.UPTODATE)
                {
                    assert complexTarget.getStatus() == Status.INCOMPLETE;
                    continue;
                }

                Action subaction = subtarget.getAction();
                subaction.setTarget(subtarget);
                subactions.add(subaction);
                
                /*
                 * 
                 */
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
            
            /*
             * An incomplete complex target is now in the process of updating.
             */
            if (complexTarget.getStatus() == Status.INCOMPLETE)
            {
                complexTarget.setStatus(Status.UPDATING);
            }
            
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
            // simple target: directly submit the action
            log.info("submitting job for ", target);
            executor.submit(action, action);
        }
    }

    private void processStep(Action action, Step step)
    {
        // simple target: directly submit the action
        Target target = action.getTarget();
        log.info("submitting job for ", target);
        executor.submit(action, action);
    }

    private void processComplexStep(Action action, ComplexStep step)
    {
            ComplexTarget complexTarget = (ComplexTarget) action.getTarget();
            Action complexAction = complexTarget.getAction();
            log.info("submitting job for step {}", complexTarget);

            /*
             * Create subtargets and check status for each subtarget. There may be a large
             * number of subtargets, so we update all subtargets statuses within a single
             * transaction.
             */
            List<Target> subtargets = step.split(complexTarget);
            List<Action> subactions = new ArrayList<Action>(subtargets.size());                      
            for (Target subtarget : subtargets)
            {
                subtarget.setParent(complexTarget);
                subtarget.setExecutionContext(context);
                targetDao.refreshTargetStatus(subtarget);
                
                if (complexTarget.getStatus() == Status.UPDATING)
                {
                    subtarget.setStatus(Status.OUTDATED);
                    targetDao.saveStatus(subtarget);
                }
                
                /*
                 * If the subtarget is up to date then the complex target must be incomplete, i.e.
                 * a previous run of the make engine was interrupted. In this case, we leave the
                 * subtarget unchanged.
                 */
                if (subtarget.getStatus() == Status.UPTODATE)
                {
                    assert complexTarget.getStatus() == Status.INCOMPLETE;
                    continue;
                }

                Action subaction = getAction(subtarget);
                subaction.setTarget(subtarget);
                subactions.add(subaction);
                
                /*
                 * 
                 */
                if (subtarget.getStatus() == Status.MISSING)
                {
                    subtarget.setStatus(Status.CREATING);
                }
                else
                {
                    subtarget.setStatus(Status.UPDATING);
                }
                targetDao.saveStatus(subtarget);
                log.info("submitting job for {}", subtarget);
            }
            
            /*
             * An incomplete complex target is now in the process of updating.
             */
            if (complexTarget.getStatus() == Status.INCOMPLETE)
            {
                //complexTarget.setStatus(Status.UPDATING);
            }
            
            if (subtargets.isEmpty())
            {
                onCompleted(complexAction);
            }
            else
            {
                submitComplexTargetAction(complexAction, subactions);
            }
    }

    private void submitComplexTargetAction(Action complexAction, List<Action> subactions)
    {
        ComplexTarget complexTarget = (ComplexTarget) complexAction.getTarget();
        
        subtargetMap.put(complexTarget.getName(), subactions.size());
    
        /*
         * For an updating complex target, we need to run the clean method before updating
         * the subtargets.
         */
        if (complexTarget.getStatus() == Status.UPDATING)
        {
            runComplexTargetAction(complexTarget);
        }
    
        // Submit the subtarget actions
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

    /**
     * Callback on action completion. For the last subtarget of a complex target, this will
     * trigger a completion event for the complex target.
     * @param r
     * @param t
     */
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
        Target target = action.getTarget();
        if (target instanceof ComplexTarget)
        {
            ComplexTarget complexTarget = (ComplexTarget) target;
            complexTarget.merge();            
        }
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
    
    /**
     * Returns the action for updating this target, based on its current status.
     * @return action
     */
    public synchronized Action getAction(Target target)
    {
        Action action = target.getAction();
        if (action == null)
        {
            Runnable runnable;
            TargetInfo info = target.getInfo();
            switch (info.getStatus())
            {
                case MISSING:
                case CREATING:
                    runnable = create(target);
                    break;

                case INCOMPLETE:
                case OUTDATED:
                case UPDATING:
                    runnable = update(target);
                    break;

                default:
                    String msg = String.format("target %s is %s", info.getName(), info.getStatus());
                    throw new IllegalStateException(msg);
            }
            action = new Action(target, runnable);
            target.setAction(action);
        }
        return action;
    }

    /**
     * Returns a runnable for creating this target, when it does not exist.
     * @return  creating action
     */
    protected Runnable create(final Target target)
    {
        Runnable runnable = new SerializableRunnable() 
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void run()
            {
                Step step = target.getStep();
                if (step == null) {
                    target.compile();
                }
                else {
                    step.compile(target);
                }
                target.setStatus(UPTODATE);
                targetDao.saveStatus(target);
            }

        };
        return runnable;
    }
    
    /**
     * Returns a runnable for updating this target when it exists already.
     * @return updating action
     */
    protected Runnable update(final Target target)
    {
        Runnable runnable = new SerializableRunnable() 
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void run()
            {
                Step step = target.getStep();
                if (step == null) {
                    target.clean();
                    target.compile();
                }
                else {
                    if (target instanceof ComplexTarget) {
                        ComplexStep complexStep = (ComplexStep) step;
                        complexStep.cleanAll(target);
                    }
                    else {
                        step.clean(target);
                        step.compile(target);
                        target.setStatus(UPTODATE);
                        targetDao.saveStatus(target);
                    }
                }
            }
        };
        return runnable;
    }
}
