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
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.util.*;

/**
 * This class defines task processor.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridTaskProcessor extends GridProcessorAdapter {
    /** Wait for 5 seconds to allow discovery to take effect (best effort). */
    private static final long DISCO_TIMEOUT = 5000;

    /** */
    private final GridMarshaller marshaller;

    /** */
    private final Map<UUID, GridTaskWorker<?, ?>> tasks = new HashMap<UUID, GridTaskWorker<?, ?>>();

    /** */
    private boolean stopping = false;

    /** */
    private int callCnt = 0;

    /** */
    private final GridDiscoveryListener discoListener;

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param mgrReg Managers registry.
     * @param procReg Processor registry.
     * @param cfg Grid configuration.
     */
    public GridTaskProcessor(GridManagerRegistry mgrReg, GridProcessorRegistry procReg, GridConfiguration cfg) {
        super(mgrReg, procReg, cfg);

        marshaller = cfg.getMarshaller();

        discoListener = new TaskDiscoveryListener();
    }

    /**
     * {@inheritDoc}
     */
    public void start() {
        if (log.isDebugEnabled() == true) {
            log.debug("Task processor started.");
        }
    }

    /**
     * Registers listener with discovery SPI. Note that discovery listener
     * registration cannot be done during start because task executor
     * starts before discovery manager.
     */
    @Override
    public void onKernalStart() {
        mgrReg.getDiscoveryManager().addDiscoveryListener(discoListener);

        Collection<GridNode> allNodes = mgrReg.getDiscoveryManager().getAllNodes();

        List<GridTaskWorker<?, ?>> tasks;

        synchronized (mux) {
            tasks = new ArrayList<GridTaskWorker<?, ?>>(this.tasks.values());
        }

        // Outside of synchronization.
        for (GridTaskWorker<?, ?> task : tasks) {
            // Synchronize nodes with discovery SPI just in case if
            // some node left before listener was registered.
            task.synchornizeNodes(allNodes);
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Added discovery listener and synchronized nodes.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(boolean cancel) {
        if (log.isDebugEnabled() == true) {
            log.debug("Task processor stopped.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStop(boolean cancel) {
        final List<GridTaskWorker<?, ?>> execTasks;

        synchronized (mux) {
            // Set stopping flag.
            stopping = true;

            // Wait for all method calls to complete. Note that we can only
            // do it after interrupting all tasks.
            while (true) {
                assert callCnt >= 0 : "ASSERTION [line=139, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

                // This condition is taken out of the loop to avoid
                // potentially wrong optimization by the compiler of
                // moving field access out of the loop causing this loop
                // to never exit.
                if (callCnt == 0) {
                    break;
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Waiting for job calls to finish: " + callCnt);
                }

                try {
                    // Release mux.
                    mux.wait();
                }
                catch (InterruptedException e) {
                    log.error("Got interrupted while stopping (shutdown is incomplete)", e);
                }
            }

            // Save executing tasks so we can wait for their completion
            // outside of synchronization.
            execTasks = new ArrayList<GridTaskWorker<?, ?>>(tasks.values());
        }

        // Interrupt jobs outside of synchronization.
        for (GridTaskWorker<?, ?> task : execTasks) {
            if (cancel == true) {
                task.cancel();
            }

            GridException ex = (GridException)new GridException("Task failed due to stopping of the grid: " + task).setData(173, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");

            task.finishTask(null, ex);
        }

        GridUtils.join(execTasks, log);

        // Finish job will remove all tasks.
        assert tasks.size() == 0 : "ASSERTION [line=181, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

        // Unsubscribe discovery last to avoid waiting for results from
        // non-existing jobs during stopping.
        mgrReg.getDiscoveryManager().removeDiscoveryListener(discoListener);

        if (log.isDebugEnabled() == true) {
            log.debug("All tasks have been cancelled and no more tasks will execute due to kernal stop.");
        }
    }

    /**
     * Waits for all tasks to be finished.
     *
     * @throws InterruptedException if waiting was interrupted.
     */
    public void waitForTasksFinishing() throws InterruptedException {
        synchronized(mux) {
            while (tasks.size() > 0) {
                mux.wait();
            }
        }
    }

    /**
     *
     * @param taskCls Task class.
     * @param arg Execution argument.
     * @param timeout Execution timeout.
     * @param listener Task execution listener.
     * @return Task future.
     * @param <T> Task argument type.
     * @param <R> Task return value type.
     */
    public <T, R> GridTaskFuture<R> execute(Class<? extends GridTask<T, R>> taskCls, T arg, long timeout,
        GridTaskListener listener) {

        synchronized (mux) {
            // Prohibit execution after stop has been called.
            if (stopping == true) {
                throw new IllegalStateException("Failed to execute task due to grid shutdown: " + taskCls);
            }

            callCnt++;
        }

        try {
            return startTask(null, taskCls, null, UUID.randomUUID(), timeout, listener, arg);
        }
        finally {
            synchronized (mux) {
                callCnt--;

                if (callCnt == 0) {
                    mux.notifyAll();
                }
            }
        }
    }

    /**
     *
     * @param task Actual task.
     * @param arg Task argument.
     * @param timeout Task timeout.
     * @param listener task listener.
     * @return Task future.
     * @param <T> Task argument type.
     * @param <R> Task return value type.
     */
    @SuppressWarnings("unchecked")
    public <T, R> GridTaskFuture<R> execute(GridTask<T, R> task, T arg, long timeout, GridTaskListener listener) {
        synchronized (mux) {
            // Prohibit execution after stop has been called.
            if (stopping == true) {
                throw new IllegalStateException("Failed to execute task due to grid shutdown: " + task);
            }

            callCnt++;
        }

        try {
            return startTask(null, null, task, UUID.randomUUID(), timeout, listener, arg);
        }
        finally {
            synchronized (mux) {
                callCnt--;

                if (callCnt == 0) {
                    mux.notifyAll();
                }
            }
        }
    }

    /**
     *
     * @param taskName Task name.
     * @param arg Execution argument.
     * @param timeout Execution timeout.
     * @param listener Task execution listener.
     * @return Task future.
     * @param <T> Task argument type.
     * @param <R> Task return value type.
     */
    public <T, R> GridTaskFuture<R> execute(String taskName, T arg, long timeout, GridTaskListener listener) {
        assert taskName != null : "ASSERTION [line=287, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
        assert timeout >= 0 : "ASSERTION [line=288, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

        synchronized (mux) {
            // Prohibit execution after stop has been called.
            if (stopping == true) {
                throw new IllegalStateException("Failed to execute task due to grid shutdown: " + taskName);
            }

            callCnt++;
        }

        try {
            return startTask(taskName, null, null, UUID.randomUUID(), timeout, listener, arg);
        }
        finally {
            synchronized (mux) {
                callCnt--;

                if (callCnt == 0) {
                    mux.notifyAll();
                }
            }
        }
    }

    /**
     *
     * @param taskName FIXDOC
     * @param taskCls Task class.
     * @param task FIXDOC
     * @param sesId FIXDOC
     * @param timeout FIXDOC
     * @param listener FIXDOC
     * @param arg FIXDOC
     * @return FIXDOC
     * @param <T> Task argument type.
     * @param <R> Task return value type.
     */
    @SuppressWarnings("unchecked")
    private <T, R> GridTaskFuture<R> startTask(String taskName, Class<? extends GridTask<T, R>> taskCls,
        GridTask<T, R> task, UUID sesId, long timeout, GridTaskListener listener, T arg) {
        long endTime = timeout == 0 ? Long.MAX_VALUE : timeout + System.currentTimeMillis();

        // Account for overflow.
        if (endTime < 0) {
            endTime = Long.MAX_VALUE;
        }

        GridException deployEx = null;
        GridDeploymentClass dep = null;

        // User provided task name.
        if (taskName != null) {
            assert taskCls == null : "ASSERTION [line=341, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
            assert task == null : "ASSERTION [line=342, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

            try {
                dep = mgrReg.getDeploymentManager().acquireLocalClass(taskName);

                if (dep == null) {
                    throw (GridException)new GridException("Failed to deploy task (was task (re|un)deployed?): " + taskName).setData(348, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");
                }

                if (GridTask.class.isAssignableFrom(dep.getDeployedClass()) == false) {
                    // Don't forget to release deployed class.
                    mgrReg.getDeploymentManager().releaseClass(dep);

                    throw (GridException)new GridException("Failed to deploy task (deployed class is not a task) [taskName=" +
                        taskName + ", depCls=" + dep.getDeployedClass() + ']').setData(355, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");
                }

                taskCls = (Class<? extends GridTask<T, R>>)dep.getDeployedClass();
            }
            catch (GridException e) {
                deployEx = e;
            }
        }
        // Deploy user task class.
        else if (taskCls != null) {
            assert task == null : "ASSERTION [line=367, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

            try {
                // Implicit deploy.
                dep = mgrReg.getDeploymentManager().deployAndAcquire(taskCls, GridUtils.detectClassLoader(taskCls));

                if (dep == null) {
                    throw (GridException)new GridException("Failed to deploy task (was task (re|un)deployed?): " + taskCls).setData(374, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");
                }

                taskName = dep.getAlias();
            }
            catch (GridException e) {
                taskName = taskCls.getName();

                deployEx = e;
            }
        }
        // Deploy user task.
        else if (task != null) {
            Class<?> cls;

            ClassLoader ldr;

            if (task instanceof GridSystemTask == true) {
                GridSystemTask sysTask = (GridSystemTask)task;

                cls = sysTask.getExecutionClass();
                ldr = sysTask.getExecutionClassLoader();
            }
            else {
                cls = task.getClass();
                ldr = GridUtils.detectClassLoader(cls);
            }

            try {
                // Implicit deploy.
                dep = mgrReg.getDeploymentManager().deployAndAcquire(cls, ldr);

                if (dep == null) {
                    throw (GridException)new GridException("Failed to deploy task (was task (re|un)deployed?): " + taskCls).setData(407, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");
                }

                taskName = dep.getAlias();
                taskCls = (Class<? extends GridTask<T, R>>)task.getClass();
            }
            catch (GridException e) {
                taskName = task.getClass().getName();

                deployEx = e;
            }
        }

        assert taskName != null : "ASSERTION [line=420, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

        Collection<GridJobSibling> siblings = Collections.emptyList();

        Map<String, Serializable> attrs = Collections.emptyMap();

        // Create task session with task name and task version.
        GridTaskSessionImpl ses = new GridTaskSessionImpl(
            cfg.getNodeId(),
            taskName,
            dep == null ? "" : dep.getUserVersion(),
            dep == null ? 0 : dep.getSequenceNumber(),
            taskCls == null ? null : taskCls.getName(),
            sesId,
            null,
            endTime,
            siblings,
            attrs,
            procReg,
            mgrReg);

        GridTaskFutureImpl<R> future = new GridTaskFutureImpl<R>(ses, mgrReg);

        if (deployEx == null) {
            if (dep == null) {
                GridException e = (GridException)new GridException("Task not deployed: " + ses.getTaskName()).setData(445, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");

                handleException(listener, e, future);
            }
            else {
                GridTaskWorker<?, ?> taskWorker = new GridTaskWorker<T, R>(cfg, arg, ses, mgrReg,
                    procReg, future, taskCls, task, dep, listener, new TaskEventListener());

                boolean release = false;

                synchronized (mux) {
                    if (task != null) {
                        // Check if someone reuse the same task instance by walking
                        // through the "tasks" map
                        for (GridTaskWorker worker: tasks.values()) {
                            GridTask workerTask = worker.getTask();

                            if (workerTask != null) {
                                // Check that the same instance of task is being used by comparing references.
                                //noinspection ObjectEquality
                                if (task == workerTask) {
                                    log.warning("Most likely the same task instance is being executed. " +
                                        "Please avoid executing the same task instances in parallel because " +
                                        "they may have concurrent resources access and conflict each other: " + task);
                                }
                            }
                        }
                    }

                    if (tasks.containsKey(sesId) == true) {
                        GridException e = (GridException)new GridException("Failed to create unique session ID (is node ID unique?): "
                            + sesId).setData(475, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");

                        handleException(listener, e, future);

                        release = true;
                    }
                    else {
                        tasks.put(sesId, taskWorker);
                    }
                }

                if (release == true) {
                    // Don't forget to release deployed class.
                    mgrReg.getDeploymentManager().releaseClass(dep);

                    return future;
                }

                try {
                    // Start task execution in another thread.
                    cfg.getExecutorService().execute(taskWorker);
                }
                catch (RejectedExecutionException e) {
                    synchronized (mux) {
                        tasks.remove(sesId);
                    }

                    GridExecutionRejectedException e2 = (GridExecutionRejectedException)new GridExecutionRejectedException("Failed to execute task " +
                        "due to thread pool execution rejection: " + taskName, e).setData(503, "src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java");

                    handleException(listener, e2, future);

                    // Don't forget to release deployed class.
                    mgrReg.getDeploymentManager().releaseClass(dep);
                }
            }
        }
        else {
            handleException(listener, deployEx, future);

            if (dep != null) {
                // Don't forget to release deployed class.
                mgrReg.getDeploymentManager().releaseClass(dep);
            }
        }

        return future;
    }

    /**
     *
     * @param listener Task listener.
     * @param ex Exception.
     * @param future Task future.
     * @param <R> Result type.
     */
    private <R> void handleException(GridTaskListener listener, GridException ex, GridTaskFutureImpl<R> future) {
        future.setException(ex);

        if (listener != null) {
            listener.onFinished(future);
        }
    }

    /**
     *
     * @param ses FIXDOC
     * @param attrs FIXDOC
     * @throws GridException FIXDOC
     */
    public void setAttributes(GridTaskSessionImpl ses, Map<? extends Serializable, ? extends Serializable> attrs)
        throws GridException {
        long timeout = ses.getEndTime() - System.currentTimeMillis();

        if (timeout <= 0) {
            log.warning("Task execution timed out (remote session attributes won't be set): " + ses);

            return;
        }

        // If setting from task or future.
        if (log.isDebugEnabled() == true) {
            log.debug("Setting session attribute(s) from task or future: " + ses);
        }

        Collection<GridJobSibling> siblings = ses.getJobSiblings();

        // Siblings should never be empty. However, future
        // may set attributes prior to map method being called.
        if (siblings.isEmpty() == false) {
            GridByteArrayList serAttrs = GridMarshalHelper.marshal(marshaller, attrs);

            GridException ex = sendSessionAttributes(serAttrs, attrs, ses, null, null);

            if (ex != null) {
                throw ex;
            }
        }
    }

    /**
     *
     * @param serAttrs Serialized session attributes.
     * @param attrs Deserialized session attributes.
     * @param ses Task session.
     * @param senderSibling Sender sibling (<tt>null</tt> if attributes are set from task node).
     * @param senderNodeId Sender node ID (<tt>null</tt> if attributes are set from task node).
     * @return Exception if sending attributes failed.
     */
    private GridException sendSessionAttributes(
        GridByteArrayList serAttrs,
        Map<? extends Serializable, ? extends Serializable> attrs,
        GridTaskSessionImpl ses,
        GridJobSiblingImpl senderSibling,
        UUID senderNodeId) {
        assert serAttrs != null : "ASSERTION [line=591, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
        assert attrs != null : "ASSERTION [line=592, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
        assert ses != null : "ASSERTION [line=593, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

        Collection<GridJobSibling> siblings = ses.getJobSiblings();

        GridCommunicationManager commMgr = mgrReg.getCommunicationManager();

        long timeout = ses.getEndTime() - System.currentTimeMillis();

        if (timeout <= 0) {
            log.warning("Session attributes won't be set due to task timeout: " + attrs);

            return null;
        }

        Map<UUID, GridPair<UUID, Long>> pairs = new HashMap<UUID, GridPair<UUID, Long>>(siblings.size(), 1.0f);

        synchronized (ses) {
            if (ses.isClosed() == true) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Setting session attributes on closed session (will ignore): " + ses);
                }

                return null;
            }

            if (senderSibling != null) {
                assert senderNodeId != null : "ASSERTION [line=619, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

                // Don't process session requests from failed jobs.
                if (senderSibling.getNodeId().equals(senderNodeId) == false) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received session request from failed job (will ignore) [, previousNodeId=" +
                            senderNodeId + ", newNodeId=" + senderSibling.getNodeId() + ']');
                    }

                    return null;
                }
            }

            ses.setInternal(attrs);

            // Do this inside of synchronization block, so every message
            // ID will be associated with a certain session state.
            for (GridJobSibling s : siblings) {
                GridJobSiblingImpl sibling = (GridJobSiblingImpl)s;

                if (sibling.isJobDone() == false) {
                    pairs.put(sibling.getJobId(), new GridPair<UUID, Long>(sibling.getNodeId(),
                        commMgr.getNextMessageId(sibling.getJobTopic())));
                }
            }
        }

        mgrReg.getEventStorageManager().record(GridEventType.SESSION_ATTR_SET, ses, null);

        GridException ex = null;

        // Every job gets an individual message to keep track of ghost requests.
        for (GridJobSibling s : ses.getJobSiblings()) {
            GridJobSiblingImpl sibling = (GridJobSiblingImpl)s;

            GridPair<UUID, Long> pair = pairs.get(sibling.getJobId());

            // Pair can be null if job is finished.
            if (pair != null) {
                GridNode node = mgrReg.getDiscoveryManager().getNode(pair.getValue1());

                // Check that node didn't change (it could happen in case of failover).
                if (node != null && node.getId().equals(sibling.getNodeId()) == true) {
                    Long nextMsgId = pair.getValue2();

                    assert nextMsgId > 0 : "ASSERTION [line=664, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

                    GridTaskSessionRequest req = new GridTaskSessionRequest(ses.getId(), sibling.getJobId(),
                        serAttrs);

                    try {
                        commMgr.sendOrderedMessage(
                            node,
                            sibling.getJobTopic(),
                            nextMsgId,
                            req,
                            GridCommunicationThreadPolicy.POOLED_THREAD,
                            timeout);
                    }
                    catch (GridException e) {
                        node = mgrReg.getDiscoveryManager().getNode(pair.getValue1());

                        if (node != null) {
                            try {
                                // Since communication on remote node may stop before
                                // we get discovery notification, we give ourselves the
                                // best effort to detect it.
                                Thread.sleep(DISCO_TIMEOUT);
                            }
                            catch (InterruptedException e1) {
                                log.warning("Got interrupted while sending session attributes.", e1);
                            }

                            node = mgrReg.getDiscoveryManager().getNode(pair.getValue1());
                        }

                        String err = "Failed to send session attribute request message to node " +
                            "(normal case if node left grid) [node=" + node + ", req=" + req + ']';

                        if (node != null) {
                            log.warning(err, e);
                        }
                        else if (log.isDebugEnabled() == true) {
                            log.debug(err);
                        }

                        if (ex == null) {
                            ex = e;
                        }
                    }
                }
            }
        }

        return ex;
    }

    /**
     * Listener for individual task events.
     */
    private class TaskEventListener implements GridTaskEventListener {
        /** */
        private final JobMessageListener msgListener = new JobMessageListener();

        /**
         * {@inheritDoc}
         */
        public void onTaskStarted(GridTaskWorker<?, ?> worker) {
            // Register for timeout notifications.
            procReg.getTimeoutProcessor().addTimeoutObject(worker);

            mgrReg.getCheckpointManager().onSessionStart(worker.getSession());
        }

        /**
         * {@inheritDoc}
         */
        public void onJobSend(GridTaskWorker<?, ?> worker, GridJobSiblingImpl sibling) {
            // Listener is stateless, so same listener can be reused for all jobs.
            mgrReg.getCommunicationManager().addMessageListener(sibling.getTaskTopic(), msgListener);
        }

        /**
         * {@inheritDoc}
         */
        public void onJobFailover(GridTaskWorker<?, ?> worker, GridJobSiblingImpl sibling, UUID nodeId) {
            GridCommunicationManager commMgr = mgrReg.getCommunicationManager();

            // Remove message ID registration.
            commMgr.removeMessageId(sibling.getJobTopic());

            // Remove old listener.
            commMgr.removeMessageListener(sibling.getTaskTopic(), msgListener);

            synchronized (worker.getSession()) {
                // Reset ID on sibling prior to sending request.
                sibling.setNodeId(nodeId);
            }

            // Register new listener on new topic.
            commMgr.addMessageListener(sibling.getTaskTopic(), msgListener);
        }

        /**
         * {@inheritDoc}
         */
        public void onJobFinished(GridTaskWorker<?, ?> worker, GridJobSiblingImpl sibling) {
            // Mark sibling finished for the purpose of
            // setting session attributes.
            synchronized (worker.getSession()) {
                sibling.onJobDone();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void onTaskFinished(GridTaskWorker<?, ?> worker) {
            synchronized (worker.getSession()) {
                worker.getSession().onClosed();
            }

            synchronized (mux) {
                tasks.remove(worker.getTaskSessionId());

                mux.notifyAll();
            }

            // Release deployment usage.
            if (worker.getDeployedClass() != null) {
                mgrReg.getDeploymentManager().releaseClass(worker.getDeployedClass());
            }

            GridTaskSessionImpl ses = worker.getSession();

            mgrReg.getCheckpointManager().onSessionEnd(ses, false);

            // Unregister from timeout notifications.
            procReg.getTimeoutProcessor().removeTimeoutObject(worker);

            GridCommunicationManager commMgr = mgrReg.getCommunicationManager();

            // Unregister job message listener from all job topics.
            for (GridJobSibling sibling : worker.getSession().getJobSiblings()) {
                GridJobSiblingImpl s = (GridJobSiblingImpl)sibling;

                commMgr.removeMessageId(s.getJobTopic());
                commMgr.removeMessageListener(s.getTaskTopic(), msgListener);
            }
        }
    }

    /**
     * Handles job execution responses.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class JobMessageListener implements GridMessageListener {
        /**
         * {@inheritDoc}
         */
        public void onMessage(UUID nodeId, Serializable msg) {
            if (msg instanceof GridTaskMessage == false) {
                log.warning("Received message of unknown type: " + msg);

                return;
            }

            final GridTaskWorker<?, ?> task;

            synchronized (mux) {
                if (stopping == true) {
                    log.warning("Received job execution response while stopping grid (will ignore): " + msg);

                    return;
                }

                task = tasks.get(((GridTaskMessage)msg).getSessionId());

                if (task == null) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received task message for unknown task (was task already reduced?): " + msg);
                    }

                    return;
                }

                callCnt++;
            }

            try {
                if (msg instanceof GridJobExecuteResponse == true) {
                    processJobExecuteResponse(nodeId, (GridJobExecuteResponse)msg, task);
                }
                else if (msg instanceof GridTaskSessionRequest == true) {
                    processTaskSessonRequest(nodeId, (GridTaskSessionRequest)msg, task);
                }
                else {
                    log.warning("Received message of unknown type: " + msg);
                }
            }
            finally {
                synchronized (mux) {
                    callCnt--;

                    if (callCnt == 0) {
                        mux.notifyAll();
                    }
                }
            }
        }

        /**
         *
         * @param nodeId Node ID.
         * @param msg Execute response message.
         * @param task Grid task worker.
         */
        private void processJobExecuteResponse(UUID nodeId, GridJobExecuteResponse msg, GridTaskWorker<?, ?> task) {
            assert nodeId != null : "ASSERTION [line=878, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
            assert msg != null : "ASSERTION [line=879, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
            assert task != null : "ASSERTION [line=880, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

            if (log.isDebugEnabled() == true) {
                log.debug("Received grid job response message [msg=" + msg + ", nodeId=" + nodeId + ']');
            }

            task.onResponse(msg);
        }

        /**
         *
         * @param nodeId Node ID.
         * @param msg Execute response message.
         * @param task Grid task worker.
         */
        @SuppressWarnings({"unchecked"})
        private void processTaskSessonRequest(UUID nodeId, GridTaskSessionRequest msg, GridTaskWorker task) {
            assert nodeId != null : "ASSERTION [line=897, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
            assert msg != null : "ASSERTION [line=898, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
            assert task != null : "ASSERTION [line=899, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

            try {
                Map<Serializable, Serializable> attrs = GridMarshalHelper.unmarshal(marshaller, msg.getAttributes(),
                    task.getTask().getClass().getClassLoader());

                final GridTaskSessionImpl ses = task.getSession();

                GridJobSiblingImpl sender = (GridJobSiblingImpl)ses.getJobSibling(msg.getJobId());

                if (sender == null) {
                    log.error("Received session request from unknown sibling: " + sender);

                    return;
                }

                sendSessionAttributes(msg.getAttributes(), attrs, ses, sender, nodeId);
            }
            catch (GridException e) {
                log.error("Failed to deserialize session request: " + msg, e);
            }
        }
    }

    /**
     * Listener to node discovery events.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class TaskDiscoveryListener implements GridDiscoveryListener {
        /**
         * {@inheritDoc}
         */
        public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
            assert type != null : "ASSERTION [line=934, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";
            assert node != null : "ASSERTION [line=935, file=src/java/org/gridgain/grid/kernal/processors/task/GridTaskProcessor.java]";

            switch (type) {
                case LEFT:
                case FAILED: {
                    final List<GridTaskWorker<?, ?>> taskList;

                    synchronized (mux) {
                        if (stopping == true) {
                            log.warning("Task executor received discovery event while stopping (will ignore) [evt=" +
                                type + ", node=" + node + ']');

                            return;
                        }

                        taskList = new ArrayList<GridTaskWorker<?, ?>>(tasks.values());

                        callCnt++;
                    }

                    // Outside of synchronization.
                    try {
                        for (GridTaskWorker<?, ?> task : taskList) {
                            task.onNodeLeft(node);
                        }
                    }
                    finally {
                        synchronized (mux) {
                            callCnt--;

                            if (callCnt == 0) {
                                mux.notifyAll();
                            }
                        }
                    }

                    break;
                }

                case JOINED:
                case METRICS_UPDATED: { break; } // No-op.

                default: { assert false : "Unknown discovery event: " + type; }
            }
        }
    }
}
