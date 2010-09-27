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

package org.gridgain.grid.kernal.processors.job;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.collision.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.managers.discovery.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.jobmetrics.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import static org.gridgain.grid.kernal.GridTopic.*;
import static org.gridgain.grid.kernal.managers.communication.GridCommunicationThreadPolicy.*;

/**
 * Responsible for all grid job execution and communication.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 */
public class GridJobProcessor extends GridProcessorAdapter {
    /** */
    private static final int CANCEL_REQS_NUM = 1000;

    /** */
    private final GridMarshaller marshaller;

    /** */
    private final Map<UUID, GridJobWorker> activeJobs = new LinkedHashMap<UUID, GridJobWorker>();

    /** */
    private final Map<UUID, GridJobWorker> passiveJobs = new LinkedHashMap<UUID, GridJobWorker>();

    /** */
    private final Map<UUID, GridJobWorker> cancelledJobs = new LinkedHashMap<UUID, GridJobWorker>();

    /** */
    private final Set<UUID> cancelReqs = new GridBoundedLinkedHashSet<UUID>(CANCEL_REQS_NUM);

    /** */
    private final GridJobEventListener evtListener;

    /** */
    private final GridMessageListener cancelListener;

    /** */
    private final GridMessageListener reqListener;

    /** */
    private final GridDiscoveryListener discoListener;

    /** */
    private final GridCollisionExternalListener colListener;

    /** */
    private int callCnt = 0;

    /** Needed for statistics. */
    private final AtomicInteger finishedJobsCnt = new AtomicInteger(0);

    /** Total job execution time (unaccounted for in metrics). */
    private final AtomicLong finishedJobsTime = new AtomicLong(0);

    /** */
    private final AtomicReference<CollisionSnapshot> lastSnapshot = new AtomicReference<CollisionSnapshot>(null);

    /** */
    private final Object mux = new Object();

    /**
     * This flag is used a guard to prevent a new collision resolution when
     * there were no changes since last one.
     */
    private boolean collisionsHandled = false;

    /** */
    private boolean stopping = false;

    /**
     *
     * @param mgrReg Managers registry.
     * @param procReg Processor registry.
     * @param cfg Grid configuration.
     */
    public GridJobProcessor(GridManagerRegistry mgrReg, GridProcessorRegistry procReg, GridConfiguration cfg) {
        super(mgrReg, procReg, cfg);

        marshaller = cfg.getMarshaller();

        evtListener = new JobEventListener();
        cancelListener = new JobCancelListener();
        reqListener = new JobExecutionListener();
        discoListener = new JobDiscoveryListener();
        colListener = new CollisionExternalListener();
    }

    /**
     *
     */
    public void start() {
        mgrReg.getCollisionManager().setCollisionExternalListener(colListener);

        GridCommunicationManager commMgr = mgrReg.getCommunicationManager();

        commMgr.addMessageListener(CANCEL.topic(), cancelListener);
        commMgr.addMessageListener(JOB.topic(), reqListener);

        if (log.isDebugEnabled() == true) {
            log.debug("Job processor started.");
        }
    }

    /**
     * Registers listener with discovery SPI. Note that discovery listener
     * registration cannot be done during start because job processor
     * starts before discovery manager.
     */
    @Override
    public void onKernalStart() {
        super.onKernalStart();

        mgrReg.getDiscoveryManager().addDiscoveryListener(discoListener);

        GridDiscoveryManager discoMgr = mgrReg.getDiscoveryManager();

        List<GridJobWorker> jobsToCancel;
        List<GridJobWorker> jobsToReject;

        synchronized (mux) {
            jobsToReject = new ArrayList<GridJobWorker>();

            for (Iterator<GridJobWorker> iter = passiveJobs.values().iterator(); iter.hasNext() == true;) {
                GridJobWorker job = iter.next();

                if (discoMgr.getNode(job.getTaskNodeId()) == null) {
                    iter.remove();

                    jobsToReject.add(job);
                }
            }

            jobsToCancel = new ArrayList<GridJobWorker>();

            for (Iterator<GridJobWorker> iter = activeJobs.values().iterator(); iter.hasNext() == true;) {
                GridJobWorker job = iter.next();

                if (discoMgr.getNode(job.getTaskNodeId()) == null) {
                   iter.remove();

                    cancelledJobs.put(job.getJobId(), job);

                    jobsToCancel.add(job);
                }
            }

            collisionsHandled = false;
        }

        // Passive jobs.
        for (GridJobWorker job : jobsToReject) {
            GridException e = (GridTopologyException)new GridTopologyException("Originating task node left grid [nodeId=" +
                job.getTaskNodeId() + ", jobSes=" + job.getSession() + ", job=" + job + ']').setData(189, "src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java");

            log.error(e.getMessage(), e);

            finishJob(job, null, e, false);
        }

        // Cancelled jobs.
        for (GridJobWorker job : jobsToCancel) {
            cancelJob(job, false);
        }

        // Force collision handling on node startup.
        handleCollisions();
    }

    /**
     * {@inheritDoc}
     */
    public void stop(boolean cancel) {
        // Clear collections.
        synchronized (mux) {
            activeJobs.clear();
            cancelledJobs.clear();
            cancelReqs.clear();
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Job processor stopped.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onKernalStop(boolean cancel) {
        // Stop receiving new requests and sending responses.
        GridCommunicationManager commMgr = mgrReg.getCommunicationManager();

        commMgr.removeMessageListener(JOB.topic(), reqListener);
        commMgr.removeMessageListener(CANCEL.topic(), cancelListener);

        // Ignore external collision events.
        mgrReg.getCollisionManager().setCollisionExternalListener(null);

        List<GridJobWorker> jobsToReject = null;
        List<GridJobWorker> jobsToCancel = null;
        List<GridJobWorker> jobsToJoin = null;

        synchronized (mux) {
            // Set stopping flag first.
            stopping = true;

            // Wait for all listener callbacks to complete.
            while (true) {
                assert callCnt >= 0 : "ASSERTION [line=246, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";

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

            jobsToReject = new ArrayList<GridJobWorker>(passiveJobs.values());

            passiveJobs.clear();

            jobsToCancel = new ArrayList<GridJobWorker>(activeJobs.values());

            jobsToJoin = new ArrayList<GridJobWorker>(cancelledJobs.values());

            jobsToJoin.addAll(jobsToCancel);
        }

        // Rejected jobs.
        for (GridJobWorker job : jobsToReject) {
            rejectJob(job);
        }

        // Cancel only if we force grid to stop
        if (cancel == true) {
            for (GridJobWorker job : jobsToCancel) {
                cancelJob(job, false);
            }
        }

        GridUtils.join(jobsToJoin, log);

        // Ignore topology changes.
        mgrReg.getDiscoveryManager().removeDiscoveryListener(discoListener);

        if (log.isDebugEnabled() == true) {
            log.debug("Job processor will not process any more jobs due to kernal stopping.");
        }
    }

    /**
     *
     * @param job Rejected job.
     */
    private void rejectJob(GridJobWorker job) {
        GridException e = (GridExecutionRejectedException)new GridExecutionRejectedException("Job was cancelled before execution [taskSesId=" +
            job.getSession().getId() + ", jobId=" + job.getJobId() + ", job=" + job.getJob() + ']').setData(307, "src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java");

        job.finishJob(null, e, true);
    }

    /**
     *
     * @param job Canceled job.
     * @param system System flag.
     */
    private void cancelJob(GridJobWorker job, boolean system) {
        job.cancel(system);
    }

    /**
     *
     * @param job Finished job.
     * @param res Job's result.
     * @param ex Optional exception.
     * @param sendReply Send reply flag.
     */
    private void finishJob(GridJobWorker job, Serializable res, GridException ex, boolean sendReply) {
        job.finishJob(res, ex, sendReply);
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

        if (log.isDebugEnabled() == true) {
            log.debug("Setting session attribute(s) from job: " + ses);
        }

        GridNode taskNode = mgrReg.getDiscoveryManager().getNode(ses.getTaskNodeId());

        if (taskNode == null) {
            throw (GridException)new GridException("Node that originated task execution has left grid: " +
                ses.getTaskNodeId()).setData(356, "src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java");
        }

        GridByteArrayList serAttrs = GridMarshalHelper.marshal(marshaller, attrs);

        final String topic = TASK.topic(ses.getJobId(),
            mgrReg.getDiscoveryManager().getLocalNode().getId());

        mgrReg.getCommunicationManager().sendOrderedMessage(
            taskNode,
            topic, // Job topic.
            mgrReg.getCommunicationManager().getNextMessageId(topic),
            new GridTaskSessionRequest(ses.getId(), ses.getJobId(), serAttrs),
            POOLED_THREAD,
            timeout);
    }

    /**
     *
     */
    private void handleCollisions() {
        CollisionSnapshot snapshot = null;

        synchronized (mux) {
            // Don't do anything if collisions were handled by another thread.
            if (collisionsHandled == true) {
                return;
            }

            snapshot = new CollisionSnapshot();

            // Create collection of passive contexts.
            for (GridJobWorker job : passiveJobs.values()) {
                snapshot.addPassive(job);
            }

            // Create collection of active contexts. Note that we don't
            // care about cancelled jobs that are still running.
            for (GridJobWorker job : activeJobs.values()) {
                snapshot.addActive(job);
            }

            // Even though lastSnapshot is atomic, we still set
            // it inside of synchronized block because we want
            // to maintain proper order of snapshots.
            lastSnapshot.set(snapshot);

            // Mark collisions as handled to make sure that nothing will
            // happen if job topology did not change.
            collisionsHandled = true;
        }

        // Outside of synchronization handle whichever snapshot was added last
        if ((snapshot = lastSnapshot.getAndSet(null)) != null) {
            snapshot.onCollision();
        }
    }

    /**
     *
     */
    private class CollisionSnapshot {
        /** */
        private final Collection<GridCollisionJobContext> passiveCtxs = new LinkedList<GridCollisionJobContext>();

        /** */
        private final Collection<GridCollisionJobContext> activeCtxs = new LinkedList<GridCollisionJobContext>();

        /** */
        private int startedCtr = 0;

        /** */
        private int activeCtr = 0;

        /** */
        private int passiveCtr = 0;

        /** */
        private int cancelCtr = 0;

        /** */
        private int rejectCtr = 0;

        /** */
        private long totalWaitTime = 0;

        /** */
        private CollisionJobContext oldestPassive = null;

        /** */
        private CollisionJobContext oldestActive = null;

        /**
         *
         * @param job Passive job.
         */
        void addPassive(GridJobWorker job) {
            passiveCtxs.add(new CollisionJobContext(job, true));
        }

        /**
         *
         * @param job Active job.
         */
        void addActive(GridJobWorker job) {
            activeCtxs.add(new CollisionJobContext(job, false));
        }

        /**
         * Handles collisions.
         */
        void onCollision() {
            // Invoke collision SPI.
            mgrReg.getCollisionManager().onCollision(passiveCtxs, activeCtxs);

            // Process waiting list.
            for (GridCollisionJobContext c : passiveCtxs) {
                CollisionJobContext ctx = (CollisionJobContext)c;

                if (ctx.isCancelled() == true) {
                    rejectJob(ctx.getJobWorker());

                    rejectCtr++;
                }
                else {
                    if (ctx.isActivated() == true) {
                        totalWaitTime += ctx.getJobWorker().getQueuedTime();

                        try {
                            // Execute in a different thread.
                            cfg.getExecutorService().execute(ctx.getJobWorker());

                            startedCtr++;

                            activeCtr++;
                        }
                        catch (RejectedExecutionException e) {
                            synchronized (mux) {
                                activeJobs.remove(ctx.getJobWorker().getJobId());
                            }

                            GridExecutionRejectedException e2 = new GridExecutionRejectedException(
                                "Job was cancelled before execution [jobSes=" + ctx.getJobWorker().getSession() +
                                ", job=" + ctx.getJobWorker().getJob() + ']', e);

                            finishJob(ctx.getJobWorker(), null, e2, true);
                        }
                    }
                    // Job remains on passive list.
                    else {
                        passiveCtr++;
                    }

                    // Since jobs are ordered, first job is the oldest passive job.
                    if (oldestPassive == null) {
                        oldestPassive = ctx;
                    }
                }
            }

            // Process active list.
            for (GridCollisionJobContext c : activeCtxs) {
                CollisionJobContext ctx = (CollisionJobContext)c;

                if (ctx.isCancelled() == true) {
                    boolean isCancelled = ctx.getJobWorker().isCancelled();

                    // We do call cancel as many times as user cancel job.
                    cancelJob(ctx.getJobWorker(), false);

                    // But we don't increment number of cancelled jobs if it
                    // was already cancelled.
                    if (isCancelled == false) {
                        cancelCtr++;
                    }
                }
                // Job remains on active list.
                else {
                    if (oldestActive == null) {
                        oldestActive = ctx;
                    }

                    activeCtr++;
                }
            }

            updateCollisionMetrics();
        }

        /**
         *
         */
        private void updateCollisionMetrics() {
            int curCancelled = cancelCtr;

            GridJobMetricsSnapshot metrics = new GridJobMetricsSnapshot();

            metrics.setActiveJobs(activeCtr);
            metrics.setCancelJobs(curCancelled);
            metrics.setMaximumExecutionTime(oldestActive == null ? 0 : oldestActive.getJobWorker().getExecuteTime());
            metrics.setMaximumWaitTime(oldestPassive == null ? 0 : oldestPassive.getJobWorker().getQueuedTime());
            metrics.setPassiveJobs(passiveCtr);
            metrics.setRejectJobs(rejectCtr);
            metrics.setWaitTime(totalWaitTime);
            metrics.setStartedJobs(startedCtr);

            // Get and reset finished jobs metrics.
            metrics.setFinishedJobs(finishedJobsCnt.getAndSet(0));
            metrics.setExecutionTime(finishedJobsTime.getAndSet(0));

            // CPU load.
            metrics.setCpuLoad(mgrReg.getRuntimeMetricsManager().getMetrics().getCurrentCpuLoad());

            procReg.getMetricsProcessor().addSnapshot(metrics);
        }

        /**
         *
         */
        private class CollisionJobContext extends GridCollisionJobContextAdapter {
            /** */
            private final boolean isPassive;

            /** */
            private boolean isActivated = false;

            /** */
            private boolean isCancelled = false;

            /**
             *
             * @param jobWorker Job Worker.
             * @param isPassive <tt>True</tt> if job is active.
             */
            CollisionJobContext(GridJobWorker jobWorker, boolean isPassive) {
                super(jobWorker);

                this.isPassive = isPassive;
            }

            /**
             * {@inheritDoc}
             */
            public boolean activate() {
                synchronized (mux) {
                    if (passiveJobs.remove(getJobWorker().getJobId()) != null) {
                        activeJobs.put(getJobWorker().getJobId(), getJobWorker());

                        isActivated = true;
                    }
                }

                return isActivated;
            }

            /**
             * {@inheritDoc}
             */
            public boolean cancel() {
                synchronized (mux) {
                    // If waiting job being rejected.
                    if (isPassive == true) {
                        isCancelled = passiveJobs.remove(getJobWorker().getJobId()) != null;
                    }
                    // If active job being cancelled.
                    else if (activeJobs.remove(getJobWorker().getJobId()) != null) {
                        cancelledJobs.put(getJobWorker().getJobId(), getJobWorker());

                        isCancelled = true;
                    }
                }

                return isCancelled;
            }

            /**
             * <tt>True</tt> if context was activated.
             *
             * @return <tt>True</tt> if context was activated.
             */
            public boolean isActivated() {
                return isActivated;
            }

            /**
             * <tt>True</tt> if context was cancelled.
             *
             * @return <tt>True</tt> if context was cancelled.
             */
            public boolean isCancelled() {
                return isCancelled;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return GridToStringBuilder.toString(CollisionJobContext.class, this);
            }
        }
    }

    /**
     *
     */
    private class CollisionExternalListener implements GridCollisionExternalListener {
        /**
         * {@inheritDoc}
         */
        public void onExternalCollision() {
            if (log.isDebugEnabled() == true) {
                log.debug("Received external collision event.");
            }

            synchronized (mux) {
                if (stopping == true) {
                    if (log.isInfoEnabled() == true) {
                        log.info("Received external collision notification while stopping grid (will ignore).");
                    }

                    return;
                }

                collisionsHandled = false;

                callCnt++;
            }

            try {
                handleCollisions();
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
    }

    /**
     * Handles job state changes.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class JobEventListener implements GridJobEventListener {
        /**
         * {@inheritDoc}
         */
        public void onJobStarted(GridJobWorker worker) {
            // Register for timeout notifications.
            procReg.getTimeoutProcessor().addTimeoutObject(worker);

            // Register session request listener for this job.
            mgrReg.getCommunicationManager().addMessageListener(worker.getJobTopic(),
                new JobSessionListener());

            // Register checkpoints.
            mgrReg.getCheckpointManager().onSessionStart(worker.getSession());
        }

        /**
         * {@inheritDoc}
         */
        public void onJobFinished(GridJobWorker worker) {
            worker.getSession().onClosed();

            // Release deployment.
            if (worker.getDeployedTask() != null) {
                mgrReg.getDeploymentManager().releaseClass(worker.getDeployedTask());
            }

            // Unregister session request listener for this jobs.
            mgrReg.getCommunicationManager().removeMessageListener(worker.getJobTopic());

            // Deregister from timeout notifications.
            procReg.getTimeoutProcessor().removeTimeoutObject(worker);

            // Unregister message IDs used for sending.
            mgrReg.getCommunicationManager().removeMessageId(worker.getTaskTopic());

            // Unregister checkpoints.
            mgrReg.getCheckpointManager().onSessionEnd(worker.getSession(), worker.isSystemCanceled());

            synchronized (mux) {
                assert passiveJobs.containsKey(worker.getJobId()) == false : "ASSERTION [line=746, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";

                activeJobs.remove(worker.getJobId());
                cancelledJobs.remove(worker.getJobId());

                collisionsHandled = false;

                callCnt++;
            }

            try {
                // Increment job execution counter. This counter gets
                // reset once this job will be accounted for in metrics.
                finishedJobsCnt.incrementAndGet();

                // Increment job execution time. This counter gets
                // reset once this job will be accounted for in metrics.
                finishedJobsTime.addAndGet(worker.getExecuteTime());

                handleCollisions();
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
    }

    /**
     * Handles task and job cancellations.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class JobCancelListener implements GridMessageListener {
        /**
         * {@inheritDoc}
         */
        public void onMessage(UUID nodeId, Serializable msg) {
            GridJobCancelRequest cancelMsg = (GridJobCancelRequest)msg;

            assert nodeId != null : "ASSERTION [line=791, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";
            assert cancelMsg != null : "ASSERTION [line=792, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";

            if (log.isDebugEnabled() == true) {
                log.debug("Received cancellation request message [cancelMsg=" + cancelMsg + ", nodeId=" + nodeId + ']');
            }

            List<GridJobWorker> jobsToCancel = new ArrayList<GridJobWorker>();
            List<GridJobWorker> jobsToReject = new ArrayList<GridJobWorker>();

            synchronized (mux) {
                if (stopping == true) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Received task cancellation request while stopping grid (will ignore): " + cancelMsg);
                    }

                    return;
                }

                callCnt++;

                // Put either job id or session id (they are unique).
                if (cancelMsg.getJobId() != null) {
                    cancelReqs.add(cancelMsg.getJobId());
                }
                else {
                    cancelReqs.add(cancelMsg.getSessionId());
                }

                // Passive jobs.
                for (Iterator<GridJobWorker> iter = passiveJobs.values().iterator(); iter.hasNext() == true;) {
                    GridJobWorker job = iter.next();

                    if (job.getSession().getId().equals(cancelMsg.getSessionId()) == true) {
                        // If job session ID is provided, then match it too.
                        if (cancelMsg.getJobId() != null) {
                            if (job.getJobId().equals(cancelMsg.getJobId()) == true) {
                                iter.remove();

                                jobsToReject.add(job);

                                collisionsHandled = false;
                            }
                        }
                        else {
                            iter.remove();

                            jobsToReject.add(job);

                            collisionsHandled = false;
                        }
                    }
                }

                // Active jobs.
                for (Iterator<GridJobWorker> iter = activeJobs.values().iterator(); iter.hasNext() == true;) {
                    GridJobWorker job = iter.next();

                    if (job.getSession().getId().equals(cancelMsg.getSessionId()) == true) {
                        // If job session ID is provided, then match it too.
                        if (cancelMsg.getJobId() != null) {
                            if (job.getJobId().equals(cancelMsg.getJobId()) == true) {
                                iter.remove();

                                cancelledJobs.put(job.getJobId(), job);

                                jobsToCancel.add(job);

                                collisionsHandled = false;
                            }
                        }
                        else {
                            iter.remove();

                            cancelledJobs.put(job.getJobId(), job);

                            jobsToCancel.add(job);

                            collisionsHandled = false;
                        }
                    }
                }
            }

            try {
                for (GridJobWorker job : jobsToReject) {
                    rejectJob(job);
                }

                for (GridJobWorker job : jobsToCancel) {
                    cancelJob(job, cancelMsg.isSystem());
                }

                handleCollisions();
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
    }

    /**
     * Handles job execution requests.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class JobExecutionListener implements GridMessageListener {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        public void onMessage(UUID nodeId, Serializable msg) {
            assert nodeId != null : "ASSERTION [line=910, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";
            assert msg != null : "ASSERTION [line=911, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";

            if (log.isDebugEnabled() == true) {
                log.debug("Received job request message [msg=" + msg + ", nodeId=" + nodeId + ']');
            }

            synchronized (mux) {
                if (stopping == true) {
                    if (log.isInfoEnabled() == true) {
                        log.info("Received job execution request while stopping this node (will ignore): " + msg);
                    }

                    return;
                }

                callCnt++;
            }

            try {
                GridJobExecuteRequest req = (GridJobExecuteRequest)msg;

                long endTime = req.getCreateTime() + req.getTimeout();

                // Account for overflow.
                if (endTime < 0) {
                    endTime = Long.MAX_VALUE;
                }

                List<GridJobSibling> siblings = new ArrayList<GridJobSibling>(req.getSiblings().size());

                // Initialize manager registry for siblings.
                for (GridJobSibling sibling : req.getSiblings()) {
                    // Don't add yourself.
                    if (sibling.getJobId().equals(req.getJobId()) == false) {
                        ((GridJobSiblingImpl)sibling).setManagerRegistry(mgrReg);

                        siblings.add(sibling);
                    }
                }

                // Try to find out task.
                GridDeploymentClass dep = mgrReg.getDeploymentManager().acquireGlobalClass(
                    req.getDeploymentMode(),
                    req.getTaskName(),
                    req.getTaskClassName(),
                    req.getSequenceNumber(),
                    req.getUserVersion(),
                    req.getTaskNodeId(),
                    req.getClassLoaderId());

                if (dep != null) {
                    GridTaskSessionImpl taskSes;
                    GridJobContextImpl jobCtx;

                    try {
                        // Note that we unmarshall session/job attributes here with proper class loader.
                        taskSes = new GridTaskSessionImpl(
                            nodeId,
                            req.getTaskName(),
                            req.getUserVersion(),
                            req.getSequenceNumber(),
                            req.getTaskClassName(),
                            req.getSessionId(),
                            req.getJobId(),
                            endTime,
                            siblings,
                            (Map<? extends Serializable, ? extends Serializable>)GridMarshalHelper.
                                unmarshal(marshaller, req.getSessionAttributes(), dep.getClassLoader()),
                            procReg,
                            mgrReg);

                        taskSes.setCheckpointSpi(req.getCheckpointSpi());
                        taskSes.setClassLoader(dep.getClassLoader());

                        jobCtx = new GridJobContextImpl(req.getJobId(),
                            (Map<? extends Serializable, ? extends Serializable>)GridMarshalHelper.unmarshal(
                                marshaller, req.getJobAttributes(), dep.getClassLoader()));
                    }
                    catch (GridException e) {
                        GridException ex = (GridException)new GridException("Failed to deserialize task  attributes [taskName=" +
                            req.getTaskName() + ", taskClsName=" + req.getTaskClassName() + ", codeVer=" +
                            req.getUserVersion() + ", taskClsLdr=" + dep.getClassLoader() + ']').setData(990, "src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java");

                        log.error(ex.getMessage(), e);

                        handleException(req, ex, endTime);

                        // Don't forget to release acquired class.
                        mgrReg.getDeploymentManager().releaseClass(dep);

                        return;
                    }

                    GridJobWorker job = new GridJobWorker(
                        cfg,
                        mgrReg,
                        procReg,
                        req.getCreateTime(),
                        taskSes,
                        jobCtx,
                        req.getJobBytes(),
                        req.getTaskNodeId(),
                        evtListener);

                    if (job.initialize(dep) == true) {
                        boolean release = false;

                        synchronized (mux) {
                            // Check if job or task has already been canceled.
                            if (cancelReqs.contains(req.getJobId()) == true ||
                                cancelReqs.contains(req.getSessionId()) == true) {
                                log.warning("Received execution request for the cancelled job (will ignore) [srcNode=" +
                                    req.getTaskNodeId() + ", jobId=" + req.getJobId() + ", sesId=" + req.getSessionId() +
                                    ']');

                                release = true;
                            }
                            else if (passiveJobs.containsKey(job.getJobId()) == true ||
                                activeJobs.containsKey(job.getJobId()) == true ||
                                cancelledJobs.containsKey(job.getJobId()) == true) {
                                log.error("Received computation request with duplicate job ID " +
                                    "(could be network malfunction, source node may hang if task timeout was not set) " +
                                    "[srcNode=" + req.getTaskNodeId() +
                                    ", jobId=" + req.getJobId() +
                                    ", sesId=" + req.getSessionId() +
                                    ", locNodeId=" + cfg.getNodeId() +
                                    ", isActive=" + activeJobs.containsKey(job.getJobId()) +
                                    ", isPassive=" + passiveJobs.containsKey(job.getJobId()) +
                                    ", isCancelled=" + cancelledJobs.containsKey(job.getJobId()) +
                                    ']');

                                release = true;
                            }
                            else {
                                passiveJobs.put(job.getJobId(), job);

                                collisionsHandled = false;
                            }
                        }

                        if (release == true) {
                            // Don't forget to release acquired class.
                            mgrReg.getDeploymentManager().releaseClass(dep);

                            return;
                        }

                        handleCollisions();
                    }
                }
                // If deployment is null.
                else {
                    GridException ex = (GridException)new GridException("Task was not deployed or was redeployed since task " +
                        "execution (either received a stale message in which case you should increase " +
                        "GridConfiguration.getPeerClassLoadingTimeout() configuration parameter, or encountered " +
                        "some invalid condition, like internal or user code version mismatch) [taskName=" +
                        req.getTaskName() +  ", taskClsName=" + req.getTaskClassName() + ", codeVer=" +
                        req.getUserVersion() + ", clsLdrId=" + req.getClassLoaderId() + ", seqNum=" +
                        req.getSequenceNumber() + ']').setData(1063, "src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java");

                    log.error(ex.getMessage(), ex);

                    handleException(req, ex, endTime);
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
         * Handles errors that happened prior to job creation.
         *
         * @param req Job execution request.
         * @param ex Exception that happened.
         * @param endTime Job end time.
         */
        private void handleException(GridJobExecuteRequest req, GridException ex, long endTime) {
            UUID locNodeId = cfg.getNodeId();

            GridNode senderNode = mgrReg.getDiscoveryManager().getNode(req.getTaskNodeId());

            try {
                GridJobExecuteResponse jobRes = new GridJobExecuteResponse(
                    locNodeId,
                    req.getSessionId(),
                    req.getJobId(),
                    GridMarshalHelper.marshal(marshaller, ex),
                    GridMarshalHelper.marshal(marshaller, null),
                    GridMarshalHelper.marshal(marshaller, Collections.emptyMap()),
                    false);

                // Job response topic.
                String topic = TASK.topic(req.getJobId(), locNodeId);

                long timeout = endTime - System.currentTimeMillis();

                if (timeout <= 0) {
                    // Ignore the actual timeout and send response anyway.
                    timeout = 1;
                }

                if (senderNode == null) {
                    log.error("Failed to reply to sender node because it left grid [nodeId=" + req.getTaskNodeId() +
                        ", jobId=" + req.getJobId() + ']');

                    // Record job reply failure.
                    mgrReg.getEventStorageManager().record(GridEventType.JOB_FAILED, req.getTaskName(),
                        req.getUserVersion(), req.getSessionId(), req.getJobId(), req.getTaskNodeId(),
                        "Job reply failed (original task node left grid): " + req.getJobId());
                }
                else {
                    // Send response to designated job topic.
                    mgrReg.getCommunicationManager().sendOrderedMessage(
                        senderNode,
                        topic,
                        mgrReg.getCommunicationManager().getNextMessageId(topic),
                        jobRes,
                        POOLED_THREAD,
                        timeout);
                }
            }
            catch (GridException e) {
                // The only option here is to log, as we must assume that resending will fail too.
                if (isDeadNode(req.getTaskNodeId()) == true) {
                    // Avoid stack trace for left nodes.
                    log.error("Failed to reply to sender node because it left grid [nodeId=" + req.getTaskNodeId() +
                        ", jobId=" + req.getJobId() +  ']');
                }
                else {
                    log.error("Error sending reply for job [nodeId=" + senderNode.getId() + ", jobId=" +
                        req.getJobId() + ']', e);
                }

                // Record job reply failure.
                mgrReg.getEventStorageManager().record(GridEventType.JOB_FAILED, req.getTaskName(),
                    req.getUserVersion(), req.getSessionId(), req.getJobId(), req.getTaskNodeId(),
                    "Failed to send reply for job: " + req.getJobId());
            }
        }

        /**
         * Checks whether node is alive or dead.
         *
         * @param uid UID of node to check.
         * @return <tt>true</tt> if node is dead, <tt>false</tt> is node is alive.
         */
        private boolean isDeadNode(UUID uid) {
            return mgrReg.getDiscoveryManager().getNode(uid) == null ||
                mgrReg.getDiscoveryManager().pingNode(uid) == false;
        }
    }

    /**
     *
     */
    private class JobSessionListener implements GridMessageListener {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"unchecked"})
        public void onMessage(UUID nodeId, Serializable msg) {
            assert nodeId != null : "ASSERTION [line=1179, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";
            assert msg != null : "ASSERTION [line=1180, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";

            if (log.isDebugEnabled() == true) {
                log.debug("Received session attribute request message [msg=" + msg + ", nodeId=" + nodeId + ']');
            }

            GridTaskSessionRequest req = (GridTaskSessionRequest)msg;

            GridJobWorker job = null;

            synchronized (mux) {
                if (stopping == true) {
                    if (log.isInfoEnabled() == true) {
                        log.info("Received job session request while stopping grid (will ignore): " + req);
                    }

                    return;
                }

                callCnt++;

                job = activeJobs.get(req.getJobId());

                if (job == null) {
                    job = passiveJobs.get(req.getJobId());
                }

                if (job == null) {
                    job = cancelledJobs.get(req.getJobId());
                }
            }

            try {
                // This is normal case if job finished right before
                // getting session attribute request.
                if (job == null) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Failed to find existing job for session attribute request (will ignore): " + req);
                    }

                    return;
                }

                Map<? extends Serializable, ? extends Serializable> attrs =
                    GridMarshalHelper.unmarshal(marshaller, req.getAttributes(), job.getSession().getClassLoader());

                mgrReg.getEventStorageManager().record(GridEventType.SESSION_ATTR_SET, job.getSession(),
                    "Changed attributes: " + attrs);

                GridTaskSessionImpl ses = job.getSession();

                synchronized (ses) {
                    ses.setInternal(attrs);
                }
            }
            catch (GridException e) {
                log.error("Failed to deserialize session attributes.", e);
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
    }

    /**
     * Listener to node discovery events.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class JobDiscoveryListener implements GridDiscoveryListener {
        /**
         * Counter used to determine whether all nodes updated metrics or not.
         * This counter is reset every time collisions are handled.
         */
        private int metricsUpdateCntr = 0;

        /**
         * {@inheritDoc}
         */
        public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
            assert type != null : "ASSERTION [line=1267, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";
            assert node != null : "ASSERTION [line=1268, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java]";

            List<GridJobWorker> jobsToReject = new ArrayList<GridJobWorker>();
            List<GridJobWorker> jobsToCancel = new ArrayList<GridJobWorker>();

            synchronized (mux) {
                callCnt++;
            }

            try {
                switch (type) {
                    case LEFT:
                    case FAILED: {
                        synchronized (mux) {
                            if (stopping == true) {
                                if (log.isDebugEnabled() == true) {
                                    log.debug("Received discovery event while stopping (will ignore) [evt=" + type +
                                        ", node=" + node + ']');
                                }

                                return;
                            }

                            for (Iterator<GridJobWorker> iter = passiveJobs.values().iterator();
                                iter.hasNext() == true;) {
                                GridJobWorker job = iter.next();

                                if (job.getTaskNodeId().equals(node.getId()) == true) {
                                    // Remove from passive jobs.
                                    iter.remove();

                                    jobsToReject.add(job);

                                    collisionsHandled = false;
                                }
                            }

                            for (Iterator<GridJobWorker> iter = activeJobs.values().iterator();
                                iter.hasNext() == true;) {
                                GridJobWorker job = iter.next();

                                if (job.getTaskNodeId().equals(node.getId()) == true && job.isFinishing() == false) {
                                    // Remove from active jobs.
                                    iter.remove();

                                    // Add to cancelled jobs.
                                    cancelledJobs.put(job.getJobId(), job);

                                    jobsToCancel.add(job);

                                    collisionsHandled = false;
                                }
                            }
                        }

                        // Outside of synchronization.
                        for (GridJobWorker job : jobsToReject) {
                            GridException e = (GridTopologyException)new GridTopologyException("Task originating node left grid " +
                                "(job will fail): [node=" + node + ", jobSes=" + job.getSession() +
                                ", job=" + job + ']').setData(1325, "src/java/org/gridgain/grid/kernal/processors/job/GridJobProcessor.java");

                            log.error(e.getMessage(), e);

                            finishJob(job, null, e, false);
                        }

                        for (GridJobWorker job : jobsToCancel) {
                            log.warning("Job is being cancelled because master task node left grid (as there is " +
                                "no one waiting for results, job will not be failed over)");

                            cancelJob(job, true);
                        }

                        handleCollisions();

                        break;
                    }

                    case METRICS_UPDATED: {
                        // Update metrics for all nodes.
                        int gridSize = mgrReg.getDiscoveryManager().getAllNodes().size();

                        synchronized (mux) {
                            // Check for less-than-equal rather than just equal
                            // in guard against topology changes.
                            if (gridSize <= ++metricsUpdateCntr) {
                                collisionsHandled = false;

                                metricsUpdateCntr = 0;
                            }
                        }

                        handleCollisions();

                        break;
                    }

                    case JOINED: { break; } // No-op.

                    default: { assert false : "Unknown discovery event: " + type; }
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
    }
}
