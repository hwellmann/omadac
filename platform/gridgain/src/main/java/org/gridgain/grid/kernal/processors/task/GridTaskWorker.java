/*
 * GRIDGAIN - OPEN CLOUD PLATFORM.
 * COPYRIGHT (C) 2005-2008 GRIDGAIN SYSTEMS. ALL RIGHTS RESERVED.
 *
 * THIS IS FREE SOFTWARE; YOU CAN REDISTRIBUTE IT AND/OR
 * MODIFY IT UNDER THE TERMS OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE AS PUBLISHED BY THE FREE SOFTWARE FOUNDATION; EITHER
 * VERSION 2.1 OF THE LICENSE, OR (AT YOUR OPTION) ANY LATER
 * VERSION.
 *
 * THIS LIBRARY IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
 * BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  SEE THE
 * GNU LESSER GENERAL PUBLIC LICENSE FOR MORE DETAILS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF THE GNU LESSER GENERAL PUBLIC
 * LICENSE ALONG WITH THIS LIBRARY; IF NOT, WRITE TO THE FREE
 * SOFTWARE FOUNDATION, INC., 51 FRANKLIN ST, FIFTH FLOOR, BOSTON, MA
 * 02110-1301 USA
 */

package org.gridgain.grid.kernal.processors.task;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.runnable.*;
import org.gridgain.grid.util.tostring.*;
import static org.gridgain.grid.kernal.GridTopic.*;
import static org.gridgain.grid.kernal.managers.communication.GridCommunicationThreadPolicy.*;

/**
 * Grid task worker. Handles full task life cycle.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <T> Task argument type.
 * @param <R> Task return value type.
 */
class GridTaskWorker<T, R> extends GridRunnable implements GridTimeoutObject {
    /**
     *
     */
    private enum State {
        /** */
        WAITING,

        /** */
        REDUCING,

        /** */
        REDUCED,

        /** */
        FINISHING
    }

    /** */
    private final GridManagerRegistry mgrReg;

    /** */
    private final GridProcessorRegistry procReg;

    /** Grid configuration. */
    private final GridConfiguration cfg;

    /** */
    private final GridTaskListener taskListener;

    /** */
    private final GridLogger log;

    /** */
    private final GridMarshaller marshaller;

    /** */
    private final GridTaskSessionImpl ses;

    /** */
    private final GridTaskFutureImpl<R> future;

    /** */                                                        
    private final T arg;

    /** */
    private final GridTaskEventListener evtListener;

    /** */
    private Map<UUID, GridJobResultImpl> jobResults = null;

    /** */
    private State state = State.WAITING;

    /** */
    private final GridDeploymentClass dep;

    /** Task class. */
    private final Class<? extends GridTask<T, R>> taskCls;

    /** */
    private GridTask<T, R> task = null;

    /** */
    private Queue<GridJobExecuteResponse> delayedResponses = new ConcurrentLinkedQueue<GridJobExecuteResponse>();

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param cfg Grid configuration.
     * @param arg Task argument.
     * @param ses Grid task session.
     * @param mgrReg Managers' registry.
     * @param procReg Resource context.
     * @param future Task future.
     * @param taskCls Task class.
     * @param task Task instance that might be null.
     * @param dep Deployed task.
     * @param taskListener Grid task listener.
     * @param evtListener Event listener.
     */
    GridTaskWorker(
        GridConfiguration cfg,
        T arg,
        GridTaskSessionImpl ses,
        GridManagerRegistry mgrReg,
        GridProcessorRegistry procReg,
        GridTaskFutureImpl<R> future,
        Class<? extends GridTask<T, R>> taskCls,
        GridTask<T, R> task,
        GridDeploymentClass dep,
        GridTaskListener taskListener,
        GridTaskEventListener evtListener) {
        super(cfg.getGridName(), "grid-task-worker", cfg.getGridLogger());

        assert ses != null : "ASSERTION [line=146, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";
        assert mgrReg != null : "ASSERTION [line=147, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";
        assert future != null : "ASSERTION [line=148, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";
        assert evtListener != null : "ASSERTION [line=149, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";
        assert dep != null : "ASSERTION [line=150, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

        this.arg = arg;
        this.cfg = cfg;
        this.mgrReg = mgrReg;
        this.procReg = procReg;
        this.future = future;
        this.ses = ses;
        this.taskCls = taskCls;
        this.task = task;
        this.dep = dep;
        this.taskListener = taskListener;
        this.evtListener = evtListener;

        log = cfg.getGridLogger().getLogger(getClass());
        marshaller = cfg.getMarshaller();
    }

    /**
     * @return FIXDOC
     */
    UUID getTaskSessionId() {
        return ses.getId();
    }

    /**
     * @return FIXDOC
     */
    GridTaskSessionImpl getSession() {
        return ses;
    }

    /**
     * @return Task future.
     */
    GridTaskFutureImpl<R> getTaskFuture() {
        return future;
    }

    /**
     * Gets property dep.
     *
     * @return Property dep.
     */
    GridDeploymentClass getDeployedClass() {
        return dep;
    }

    /**
     * @return Grid task.
     */
    public GridTask<T, R> getTask() {
        return task;
    }

    /**
     * @param task Deployed task.
     */
    public void setTask(GridTask<T, R> task) {
        this.task = task;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getTimeoutId() {
        return ses.getId();
    }

    /**
     * {@inheritDoc}
     */
    public void onTimeout() {
        synchronized (mux) {
            if (state != State.WAITING) {
                return;
            }
        }

        log.warning("Task has timed out: " + ses);

        recordEvent(GridEventType.TASK_TIMED_OUT, null);

        GridException e = (GridTaskTimeoutException)new GridTaskTimeoutException("Task timed out (check logs for error messages): " + ses).setData(233, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

        finishTask(null, e);
    }

    /**
     * {@inheritDoc}
     */
    public long getEndTime() {
        return ses.getEndTime();
    }

    /**
     *
     * @param taskCls FIXDOC
     * @return Task instance.
     * @throws GridException FIXDOC
     */
    @SuppressWarnings({"unchecked"})
    private GridTask<T, R> newTask(Class<? extends GridTask<T, R>> taskCls) throws GridException {
        try {
            return taskCls.newInstance();
        }
        catch (InstantiationException e) {
            throw (GridException)new GridException("Failed to instantiate task class: " + taskCls, e).setData(257, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");
        }
        catch (IllegalAccessException e) {
            throw (GridException)new GridException("Failed to instantiate task class: " + taskCls, e).setData(260, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");
        }
    }

    /**
     *
     */
    private void initializeSpis() {
        GridTaskSpis spis = GridUtils.getAnnotation(task.getClass(), GridTaskSpis.class);

        if (spis != null) {
            ses.setTopologySpi(spis.topologySpi());
            ses.setLoadBalancingSpi(spis.loadBalancingSpi());
            ses.setFailoverSpi(spis.failoverSpi());
            ses.setCheckpointSpi(spis.checkpointSpi());
        }
    }

    /**
     * Maps this task's jobs to nodes and sends them out.
     *
     * @see GridRunnable#body()
     * @throws InterruptedException FIXDOC
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void body() throws InterruptedException {
        assert dep != null : "ASSERTION [line=287, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

        evtListener.onTaskStarted(this);

        recordEvent(GridEventType.TASK_STARTED, null);

        //noinspection CatchGenericClass
        try {
            // Use either user task or deployed one.
            if (task == null) {
                setTask(newTask(taskCls));
            }

            initializeSpis();

            ses.setClassLoader(dep.getClassLoader());

            // Obtain topology from topology SPI.
            Collection<GridNode> nodes = mgrReg.getTopologyManager().getTopology(ses,
                mgrReg.getDiscoveryManager().getAllNodes());

            if (nodes == null || nodes.isEmpty() == true) {
                throw (GridException)new GridException("Task topology provided by topology SPI is empty or null.").setData(309, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");
            }

            List<GridNode> shuffledNodes = new ArrayList<GridNode>(nodes);

            // Shuffle nodes prior to giving them to user.
            Collections.shuffle(shuffledNodes);

            // Load balancer.
            GridLoadBalancer balancer = mgrReg.getLoadBalancingManager().getLoadBalancer(ses, shuffledNodes);

            // Inject resources.
            procReg.getResourceProcessor().inject(dep, getTask(), ses, balancer);

            Map<? extends GridJob, GridNode> mappedJobs = null;

            Thread curThread = Thread.currentThread();

            // Get original context class loader.
            ClassLoader ctxLoader = curThread.getContextClassLoader();

            try {
                curThread.setContextClassLoader(task.getClass().getClassLoader());

                mappedJobs = getTask().map(shuffledNodes, arg);
            }
            finally {
                // Set the original class loader back.
                curThread.setContextClassLoader(ctxLoader);
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Mapped task jobs to nodes [jobCnt=" + mappedJobs.size() + ", mappedJobs=" + mappedJobs +
                    ", ses=" + ses + ']');
            }

            if (mappedJobs == null || mappedJobs.isEmpty() == true) {
                throw (GridException)new GridException("Task map operation produced no mapped jobs: " + ses).setData(346, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");
            }

            List<GridJobResultImpl> jobResultList = new ArrayList<GridJobResultImpl>(mappedJobs.size());

            List<GridJobSibling> siblings = new ArrayList<GridJobSibling>(mappedJobs.size());

            // Map jobs to nodes for computation.
            for (Map.Entry<? extends GridJob, GridNode> mappedJob : mappedJobs.entrySet()) {
                GridJob job = mappedJob.getKey();
                GridNode node = mappedJob.getValue();

                if (job == null) {
                    throw (GridException)new GridException("GridTask.map(...) method returned null job [mappedJob=" + mappedJob +
                        ", ses=" + ses + ']').setData(359, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");
                }

                if (node == null) {
                    throw (GridException)new GridException("GridTask.map(...) method returned null node [mappedJob=" + mappedJob +
                        ", ses=" + ses + ']').setData(364, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");
                }

                UUID jobId = UUID.randomUUID();

                GridJobSiblingImpl sibling = new GridJobSiblingImpl(ses.getId(), jobId, node.getId(), mgrReg);

                jobResultList.add(new GridJobResultImpl(job, jobId, node, sibling));

                siblings.add(sibling);

                recordEvent(GridEventType.JOB_MAPPED, jobId, node.getId(), null);
            }

            ses.setJobSiblings(siblings);

            synchronized (mux) {
                jobResults = new HashMap<UUID, GridJobResultImpl>(mappedJobs.size());

                // Populate all remote mappedJobs into map, before mappedJobs are sent.
                // This is done to avoid race condition when we start
                // getting results while still sending out references.
                for (GridJobResultImpl res : jobResultList) {
                    if (jobResults.put(res.getJobId(), res) != null) {
                        throw (GridException)new GridException("Duplicate job ID for remote job found: " + res.getJobId()).setData(389, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");
                    }

                    res.setOccupied(true);
                }
            }

            // Send out all remote mappedJobs.
            for (GridJobResultImpl rslt : jobResultList) {
                evtListener.onJobSend(this, rslt.getSibling());

                try {
                    sendRequest(rslt);
                }
                finally {
                    // Open job for processing results.
                    synchronized (mux) {
                        rslt.setOccupied(false);
                    }
                }
            }

            processDelayedResponces();
        }
        catch (GridException e) {
            log.error("Failed to map task jobs to nodes: " + ses, e);

            finishTask(null, e);
        }
        // Catch throwable to protect against bad user code.
        catch (Throwable e) {
            String errMsg = "Failed to map task jobs to nodes due to undeclared user exception: " + ses;

            log.error(errMsg, e);

            GridUserUndeclaredException tmp = (GridUserUndeclaredException)new GridUserUndeclaredException(errMsg, e).setData(424, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

            finishTask(null, tmp);
        }
    }

    /**
     *
     */
    private void processDelayedResponces() {
        GridJobExecuteResponse res = null;

        while ((res = delayedResponses.poll()) != null) {
            onResponse(res);
        }
    }

    /**
     *
     * @param res FIXDOC
     */
    @SuppressWarnings({"CatchGenericClass", "unchecked"})
    void onResponse(GridJobExecuteResponse res) {
        assert res != null : "ASSERTION [line=447, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

        GridJobResultImpl jobRes = null;

        List<GridJobResult> results = null;

        GridJobResultPolicy policy = null;

        // Flag indicating whether occupied flag for
        // job response was changed in this method call.
        boolean selfOccupied = false;

        try {
            // Get nodes topology for failover.
            Collection<GridNode> top = mgrReg.getTopologyManager().getTopology(ses,
                mgrReg.getDiscoveryManager().getAllNodes());

            synchronized (mux) {
                // If task is not waiting for responses,
                // then there is no point to proceed.
                if (state != State.WAITING) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Ignoring response since task is already reducing or finishing [res=" + res +
                            ", job=" + ses + ", state=" + state + ']');
                    }

                    return;
                }

                jobRes = jobResults.get(res.getJobId());

                if (jobRes == null) {
                    if (log.isDebugEnabled() == true) {
                        log.warning("Received response for unknown child job (was job presumed failed?): " + res);
                    }

                    return;
                }

                // Only process 1st response and ignore following ones. This scenario
                // is possible if node has left topology and and fake failure response
                // was created from discovery listener and when sending request failed.
                if (jobRes.hasResponse() == true) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received redundant response for a job (will ignore): " + res);
                    }

                    return;
                }

                if (jobRes.getNode().getId().equals(res.getNodeId()) == false) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Ignoring stale response as job was already resent to other node [res=" + res +
                            ", jobRes=" + jobRes + ']');
                    }

                    return;
                }

                if (jobRes.isOccupied() == true) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Adding response to delayed queue (job is either being sent or processing " +
                            "another response): " + res);
                    }

                    delayedResponses.offer(res);

                    return;
                }

                selfOccupied = true;

                // Prevent processing 2 responses for the same job simultaneously.
                jobRes.setOccupied(true);

                if (res.getFakeException() != null) {
                    jobRes.onResponse(null, res.getFakeException(), null, false);
                }
                else {
                    ClassLoader clsLdr = dep.getClassLoader();

                    try {
                        jobRes.onResponse(
                            GridMarshalHelper.unmarshal(marshaller, res.getJobResult(), clsLdr),
                            (GridException)GridMarshalHelper.unmarshal(marshaller, res.getException(), clsLdr),
                            (Map<Serializable, Serializable>)
                            GridMarshalHelper.unmarshal(marshaller, res.getJobAttributes(), clsLdr),
                            res.isCancelled()
                        );
                    }
                    catch (GridException e) {
                        log.error("Error deserializing job response: " + res, e);

                        finishTask(null, e);
                    }
                }

                results = getRemoteResults();

                policy = result(jobRes, results);

                // If instructed not to cache results, then set the result to null.
                if (dep.getAnnotation(GridTaskNoResultCache.class) != null) {
                    jobRes.clearData();
                }

                if (policy != null && policy == GridJobResultPolicy.FAILOVER) {
                    // Make sure that fail-over SPI provided a new node.
                    if (failover(res, jobRes, top) == false) {
                        policy = null;
                    }
                }
            }

            // Outside of synchronization.
            if (policy != null) {
                // Handle failover.
                if (policy == GridJobResultPolicy.FAILOVER) {
                    sendFailoverRequest(jobRes);
                }
                else {
                    evtListener.onJobFinished(this, jobRes.getSibling());

                    if (policy == GridJobResultPolicy.REDUCE) {
                        reduce(results);
                    }
                }
            }
        }
        // Catch Throwable to protect against bad user code.
        catch (GridException e) {
            log.error("Failed to obtain topology [ses=" + ses + ", error=" + e + ']', e);

            finishTask(null, e);
        }
        finally {
            // Open up job for processing responses.
            // Only unset occupied flag, if it was
            // set in this method.
            if (selfOccupied == true) {
                assert jobRes != null : "ASSERTION [line=587, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

                synchronized (mux) {
                    jobRes.setOccupied(false);
                }

                processDelayedResponces();
            }
        }
    }

    /**
     *
     * @param jobRes Job result.
     * @param results Existing job results.
     * @return Job result policy.
     */
    @SuppressWarnings({"CatchGenericClass"})
    private GridJobResultPolicy result(GridJobResultImpl jobRes, List<GridJobResult> results) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=606, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

        Thread curThread = Thread.currentThread();

        ClassLoader ctxLoader = curThread.getContextClassLoader();

        try {
            // Obtain job result policy.
            GridJobResultPolicy policy = null;

            try {
                policy = getTask().result(jobRes, results);
            }
            finally {
                recordEvent(GridEventType.JOB_RESULTED, jobRes.getJobId(), jobRes.getNode().getId(), policy);
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Obtained job result policy [policy=" + policy + ", ses=" + ses + ']');
            }

            switch (policy) {
                // Start reducing all results received so far.
                case REDUCE: {
                    state = State.REDUCING;

                    break;
                }

                // Keep waiting if there are more responses to come,
                // otherwise, reduce.
                case WAIT: {
                    assert results.size() <= jobResults.size() : "ASSERTION [line=638, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

                    // If there are more results to wait for.
                    if (results.size() == jobResults.size()) {
                        policy = GridJobResultPolicy.REDUCE;

                        // All results are received, proceed to reduce method.
                        state = State.REDUCING;
                    }

                    break;
                }

                // Can't handle failover while holding the lock,
                // so no-op for now.
                case FAILOVER: { break; }

                default: { assert false : "Unknown policy: " + policy; }
            }

            return policy;
        }
        catch (GridException e) {
            log.error("Failed to obtain remote job result policy for result from GridTask.result(..) method " +
                "(will fail the whole task): " + jobRes, e);

            finishTask(null, e);

            return null;
        }
        catch (Throwable e) {
            String errMsg = "Failed to obtain remote job result policy for result from GridTask.result(..) " +
                "method due to undeclared user exception (will fail the whole task): " + jobRes;

            log.error(errMsg, e);

            GridUserUndeclaredException tmp = (GridUserUndeclaredException)new GridUserUndeclaredException(errMsg, e).setData(674, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

            // Failed to successfully obtain result policy and
            // hence forced to fail the whole deployed task.
            finishTask(null, tmp);

            return null;
        }
        // Set original class loader back.
        finally {
            curThread.setContextClassLoader(ctxLoader);
        }
    }

    /**
     *
     * @param results Job results.
     */
    @SuppressWarnings({"CatchGenericClass"})
    private void reduce(List<GridJobResult> results) {
        try {
            R reduceRes;

            Thread curThread = Thread.currentThread();

            ClassLoader ctxLoader = curThread.getContextClassLoader();

            try {
                curThread.setContextClassLoader(task.getClass().getClassLoader());

                // Reduce results.
                reduceRes = getTask().reduce(results);
            }
            finally {
                // Set original class loader back.
                curThread.setContextClassLoader(ctxLoader);

                synchronized (mux) {
                    assert state == State.REDUCING : "ASSERTION [line=712, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]. " + "Invalid task state: " + state;

                    state = State.REDUCED;
                }
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Reduced job responses [reduceRes=" + reduceRes + ", ses=" + ses + ']');
            }

            recordEvent(GridEventType.TASK_REDUCED, null);

            finishTask(reduceRes, null);

            // Try to cancel child jobs out of curtesy.
            cancelChildren();
        }
        catch (GridException e) {
            log.error("Failed to reduce job results for task: " + getTask(), e);

            finishTask(null, e);
        }
        // Catch Throwable to protect against bad user code.
        catch (Throwable e) {
            String errMsg = "Failed to reduce job results due to undeclared user exception [task=" +
                getTask() + ", error=" + e + ']';

            log.error(errMsg, e);

            GridUserUndeclaredException tmp = (GridUserUndeclaredException)new GridUserUndeclaredException(errMsg ,e).setData(741, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

            finishTask(null, tmp);
        }
     }

    /**
     *
     * @param res Execution response.
     * @param jobRes Job result.
     * @param top Topology.
     * @return <tt>True</tt> if fail-over SPI returned a new node.
     */
    @SuppressWarnings({"CatchGenericClass"})
    private boolean failover(GridJobExecuteResponse res, GridJobResultImpl jobRes, Collection<GridNode> top) {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=756, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

        try {
            List<GridNode> nodes = new ArrayList<GridNode>(top);

            // Shuffle nodes prior to giving them to user.
            Collections.shuffle(nodes);

            // Map to a new node.
            GridNode node = mgrReg.getFailoverManager().failover(ses, jobRes, nodes);

            if (node == null) {
                String msg = "Failed to failover a job to another node (failover SPI returned null) [job=" +
                    jobRes.getJob() + ", node=" + jobRes.getNode() + ']';

                if (log.isDebugEnabled() == true) {
                    log.debug(msg);
                }

                GridException e = (GridTopologyException)new GridTopologyException(msg).setData(775, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

                finishTask(null, e);

                return false;
            }

            if (log.isDebugEnabled() == true) {
                log.debug("Resolved job failover [newNode=" + node + ", oldNode=" + jobRes.getNode() +
                    ", job=" + jobRes.getJob() + ", resMsg=" + res + ']');
            }

            jobRes.setNode(node);
            jobRes.resetResponse();

            return true;
        }
        // Catch Throwable to protect against bad user code.
        catch (Throwable e) {
            String errMsg = "Failed to failover job due to undeclared user exception [job=" +
                jobRes.getJob() + ", error=" + e + ']';

            log.error(errMsg, e);

            GridUserUndeclaredException tmp = (GridUserUndeclaredException)new GridUserUndeclaredException(errMsg, e).setData(799, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

            finishTask(null, tmp);

            return false;
        }
    }

    /**
     *
     * @param jobRes Job result.
     */
    private void sendFailoverRequest(GridJobResultImpl jobRes) {
        // Internal failover notification.
        evtListener.onJobFailover(this, jobRes.getSibling(), jobRes.getNode().getId());

        long timeout = ses.getEndTime() - System.currentTimeMillis();

        if (timeout > 0) {
            recordEvent(GridEventType.JOB_FAILED_OVER, jobRes.getJobId());

            // Send new reference to remote nodes for execution.
            sendRequest(jobRes);
        }
        else {
            // Don't call 'finishTask(..)' here as it will
            // be called from 'onTimeout(..)' callback.
            log.warning("Failed to fail-over job due to task timeout: " + jobRes);
        }
    }

    /**
     * Interrupts child jobs on remote nodes.
     */
    private void cancelChildren() {
        List<GridJobResultImpl> doomed = new LinkedList<GridJobResultImpl>();

        synchronized (mux) {
            // Only interrupt unfinished jobs.
            for (GridJobResultImpl rslt : jobResults.values()) {
                if (rslt.hasResponse() == false) {
                    doomed.add(rslt);
                }
            }
        }

        // Send cancellation request to all unfinished children.
        for (GridJobResultImpl res : doomed) {
            try {
                GridNode node = mgrReg.getDiscoveryManager().getNode(res.getNode().getId());

                if (node != null) {
                    mgrReg.getCommunicationManager().sendMessage(node,
                        GridTopic.CANCEL.topic(),
                        new GridJobCancelRequest(ses.getId(), res.getJobId(), /*curtesy*/true),
                        POOLED_THREAD);
                }
            }
            catch (GridException e) {
                if (isDeadNode(res.getNode().getId()) == false) {
                    log.error("Failed to send cancel request to node (will ignore) [nodeId=" +
                        res.getNode().getId() + ", taskName=" + ses.getTaskName() +
                        ", taskSesId=" + ses.getId() + ", jobSesId=" + res.getJobId() + ']', e);
                }
            }
        }
    }

    /**
     *
     * @param res Job result.
     */
    private void sendRequest(GridJobResultImpl res) {
        assert res != null : "ASSERTION [line=872, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

        GridJobExecuteRequest req = null;

        GridNode node = res.getNode();

        try {
            GridNode curNode = mgrReg.getDiscoveryManager().getNode(node.getId());

            // Check if node exists prior to sending to avoid cases when a discovery
            // listener notified about node leaving after topology resolution. Note
            // that we make this check because we cannot count on exception being
            // thrown in case of send failure.
            if (curNode == null) {
                log.warning("Failed to send job request because remote node left grid (will attempt fail-over to " +
                    "another node) [node=" + node + ", taskName=" + ses.getTaskName() + ", taskSesId=" +
                    ses.getId() + ", jobSesId=" + res.getJobId() + ']');

                GridJobExecuteResponse fakeRes = new GridJobExecuteResponse(node.getId(), ses.getId(),
                    res.getJobId(), null, null, null, false);

                fakeRes.setFakeException(new GridTopologyException("Failed to send job due to node failure: " + node));

                onResponse(fakeRes);
            }
            else {
                long timeout = ses.getEndTime() - System.currentTimeMillis();

                if (timeout > 0) {
                    req = new GridJobExecuteRequest(
                        ses.getId(),
                        res.getJobId(),
                        ses.getTaskName(),
                        ses.getUserVersion(),
                        ses.getSequenceNumber(),
                        ses.getTaskClassName(),
                        GridMarshalHelper.marshal(marshaller, res.getJob()),
                        timeout,
                        cfg.getNodeId(),
                        ses.getJobSiblings(),
                        GridMarshalHelper.marshal(marshaller, ses.getAttributes()),
                        GridMarshalHelper.marshal(marshaller, res.getJobContext().getAttributes()),
                        ses.getCheckpointSpi(),
                        dep.getClassLoaderId(),
                        dep.getDeploymentMode());

                    if (log.isDebugEnabled() == true) {
                        log.debug("Sending grid job request [req=" + req + ", node=" + node + ']');
                    }

                    // Send job execution request.
                    mgrReg.getCommunicationManager().sendMessage(node, JOB.topic(), req, POOLED_THREAD);
                }
                else {
                    log.warning("Job timed out prior to sending job execution request: " + res.getJob());
                }
            }
        }
        catch (GridException e) {
            // Avoid stack trace if node has left grid.
            if (isDeadNode(res.getNode().getId()) == true) {
                log.warning("Failed to send job request because remote node left grid (will attempt fail-over to " +
                    "another node) [node=" + node + ", taskName=" + ses.getTaskName() +
                    ", taskSesId=" + ses.getId() + ", jobSesId=" + res.getJobId() + ']');
            }
            else {
                log.error("Failed to send job request: " + req, e);
            }

            GridJobExecuteResponse fakeRes = new GridJobExecuteResponse(node.getId(), ses.getId(),
                res.getJobId(), null, null, null, false);

            fakeRes.setFakeException(new GridTopologyException("Failed to send job due to node failure: " + node, e));

            onResponse(fakeRes);
        }
    }

    /**
     *
     * @param node FIXDOC
     */
    void onNodeLeft(GridNode node) {
        List<GridJobExecuteResponse> resList = new ArrayList<GridJobExecuteResponse>();

        synchronized (mux) {
            // First check if job cares about future responses.
            if (state != State.WAITING) {
                return;
            }

            if (jobResults != null) {
                for (GridJobResultImpl jobRes : jobResults.values()) {
                    if (jobRes.hasResponse() == false &&
                        jobRes.getNode().getId().equals(node.getId()) == true) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Creating fake response because node left grid [job=" + jobRes.getJob() +
                                ", node=" + node + ']');
                        }

                        GridTopologyException e = (GridTopologyException)new GridTopologyException("Node has left grid: " + node).setData(972, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

                        GridJobExecuteResponse fakeRes = new GridJobExecuteResponse(node.getId(), ses.getId(),
                            jobRes.getJobId(), null, null, null, false);

                        fakeRes.setFakeException(e);

                        // Artificial response in case if a job is waiting for a response from
                        // non-existent node.
                        resList.add(fakeRes);
                    }
                }
            }
        }

        // Simulate responses without holding synchronization.
        for (GridJobExecuteResponse res : resList) {
            if (log.isDebugEnabled() == true) {
                log.debug("Simulating fake response from left node [res=" + res + ", node=" + node + ']');
            }

            onResponse(res);
        }
    }

    /**
     *
     * @param nodes FIXDOC
     */
    void synchornizeNodes(Collection<GridNode> nodes) {
        List<GridJobExecuteResponse> ress = new ArrayList<GridJobExecuteResponse>();

        synchronized (mux) {
            // First check if job cares about future responses.
            if (state != State.WAITING) {
                return;
            }

            if (jobResults != null) {
                for (GridJobResultImpl jobResult : jobResults.values()) {
                    if (jobResult.hasResponse() == false) {
                        boolean found = false;

                        for (GridNode node : nodes) {
                            if (jobResult.getNode().getId().equals(node.getId()) == true) {
                                found = true;

                                break;
                            }
                        }

                        // If node does not exist.
                        if (found == false) {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Creating fake response when synchronizing nodes for jobRslt: " + jobResult);
                            }

                            GridTopologyException e = (GridTopologyException)new GridTopologyException("Node has left grid: " +
                                jobResult.getNode()).setData(1029, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java");

                            GridJobExecuteResponse fakeRes = new GridJobExecuteResponse(jobResult.getNode().getId(),
                                ses.getId(), jobResult.getJobId(), null, null, null, false);

                            fakeRes.setFakeException(e);

                            // Artificial response in case if a jobRslt is waiting for a response from
                            // non-existent node.
                            ress.add(fakeRes);
                        }
                    }
                }
            }
        }

        // Simulate responses without holding synchronization.
        for (GridJobExecuteResponse res : ress) {
            onResponse(res);
        }
    }

    /**
     *
     * @param evtType Event type.
     * @param jobId Job ID.
     */
    private void recordEvent(GridEventType evtType, UUID jobId) {
        mgrReg.getEventStorageManager().record(evtType, ses.getTaskName(), ses.getUserVersion(), ses.getId(), jobId,
            ses.getTaskNodeId(), null);
    }

    /**
     *
     * @param evtType Event type.
     * @param jobId Job ID.
     * @param evtNodeId Event node ID.
     * @param jobResPolicy Grid Job Result
     */
    private void recordEvent(GridEventType evtType, UUID jobId, UUID evtNodeId, GridJobResultPolicy jobResPolicy) {
        mgrReg.getEventStorageManager().record(evtType, ses.getTaskName(), ses.getUserVersion(), ses.getId(), jobId,
            evtNodeId, null, jobResPolicy, null);
    }

    /**
     *
     * @return FIXDOC
     */
    private List<GridJobResult> getRemoteResults() {
        assert Thread.holdsLock(mux) == true : "ASSERTION [line=1079, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskWorker.java]";

        List<GridJobResult> results = new ArrayList<GridJobResult>(jobResults.size());

        for (GridJobResultImpl jobResult : jobResults.values()) {
            if (jobResult.hasResponse() == true) {
                results.add(jobResult);
            }
        }

        return results;
    }

    /**
     *
     * @param res FIXDOC
     * @param e FIXDOC
     */
    void finishTask(R res, GridException e) {
        // Avoid finishing a job more than once from
        // different threads.
        synchronized (mux) {
            if (state == State.REDUCING || state == State.FINISHING) {
                return;
            }

            state = State.FINISHING;
        }

        recordEvent(e == null ? GridEventType.TASK_FINISHED : GridEventType.TASK_FAILED, null);

        // Clean resources prior to finishing future.
        evtListener.onTaskFinished(this);

        // Locally executing jobs that timed out will still exit normally.
        // In that case will record events, but do not notify listener,
        // since it was already notified at timeout time.
        if (e != null) {
            future.setException(e);

            if (taskListener != null) {
                taskListener.onFinished(future);
            }
        }
        else {
            future.setData(res);

            // If task was invoked asynchronously.
            if (taskListener != null) {
                taskListener.onFinished(future);
            }
        }
    }

    /**
     * Checks whether node is alive or dead.
     *
     * @param uid UID of node to check.
     * @return <tt>true</tt> if node is dead, <tt>false</tt> is node is alive.
     */
    private boolean isDeadNode(UUID uid) {
        return mgrReg.getDiscoveryManager().getNode(uid) == null || mgrReg.getDiscoveryManager().pingNode(uid) == false;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        return ses.getId().equals(((GridTaskWorker<T, R>)obj).ses.getId()) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return ses.getId().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridTaskWorker.class, this);
    }
}
