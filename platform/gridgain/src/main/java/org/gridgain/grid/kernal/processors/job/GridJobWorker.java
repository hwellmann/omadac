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
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.timeout.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.runnable.*;
import org.gridgain.grid.util.*;
import static org.gridgain.grid.kernal.GridTopic.*;
import static org.gridgain.grid.kernal.managers.communication.GridCommunicationThreadPolicy.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobWorker extends GridRunnable implements GridTimeoutObject {
    /** */
    private final long createTime;

    /** */
    private final AtomicLong startTime = new AtomicLong(0);

    /** */
    private final AtomicLong finishTime = new AtomicLong(0);

    /** */
    private final GridManagerRegistry mgrReg;

    /** */
    private final GridProcessorRegistry procReg;

    /** Grid configuration. */
    private final GridConfiguration cfg;

    /** */
    private final String jobTopic;

    /** */
    private final String taskTopic;

    /** */
    private GridByteArrayList jobBytes;

    /** Task originating node ID. */
    private final UUID taskNodeId;

    /** */
    private final UUID locNodeId;

    /** */
    private final GridLogger log;

    /** */
    private final GridMarshaller marshaller;

    /** */
    private final GridTaskSessionImpl ses;

    /** */
    private final GridJobContextImpl jobCtx;

    /** */
    private final GridJobEventListener evtListener;

    /** */
    private boolean isFinishing = false;

    /** */
    private boolean isTimedOut = false;

    /** */
    private final AtomicBoolean isSysCancelled = new AtomicBoolean(false);

    /** */
    private boolean isStarted = false;

    /** Deployed job. */
    private final AtomicReference<GridJob> job = new AtomicReference<GridJob>(null);

    /** Deployed task. */
    private final AtomicReference<GridDeploymentClass> deployedTask =
        new AtomicReference<GridDeploymentClass>(null);

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param cfg Grid configuration.
     * @param mgrReg Managers registry.
     * @param procReg Processors registry.
     * @param createTime Create time.
     * @param ses Grid task session.
     * @param jobCtx FIXDOC
     * @param jobBytes Grid job bytes.
     * @param taskNodeId Grid task node ID.
     * @param evtListener Job event listener.
     */
    GridJobWorker(
        GridConfiguration cfg,
        GridManagerRegistry mgrReg,
        GridProcessorRegistry procReg,
        long createTime,
        GridTaskSessionImpl ses,
        GridJobContextImpl jobCtx,
        GridByteArrayList jobBytes,
        UUID taskNodeId,
        GridJobEventListener evtListener) {
        super(cfg.getGridName(), "grid-job-worker", cfg.getGridLogger());

        assert mgrReg != null : "ASSERTION [line=142, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";
        assert ses != null : "ASSERTION [line=143, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";
        assert jobCtx != null : "ASSERTION [line=144, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";
        assert taskNodeId != null : "ASSERTION [line=145, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";
        assert evtListener != null : "ASSERTION [line=146, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";

        assert ses.getJobId() != null : "ASSERTION [line=148, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";

        this.createTime = createTime;
        this.evtListener = evtListener;
        this.cfg = cfg;
        this.mgrReg = mgrReg;
        this.procReg = procReg;
        this.ses = ses;
        this.jobCtx = jobCtx;
        this.jobBytes = jobBytes;
        this.taskNodeId = taskNodeId;

        log = cfg.getGridLogger().getLogger(getClass());
        marshaller = cfg.getMarshaller();

        locNodeId = mgrReg.getDiscoveryManager().getLocalNode().getId();

        jobTopic = JOB.topic(ses.getJobId(), locNodeId);
        taskTopic = TASK.topic(ses.getJobId(), locNodeId);
    }

    /**
     * Gets deployed job or <tt>null</tt> of job could not be deployed.
     *
     * @return Deployed job.
     */
    public GridJob getJob() {
        return job.get();
    }

    /**
     * @return Deployed task.
     */
    GridDeploymentClass getDeployedTask() {
        return deployedTask.get();
    }

    /**
     * Returns <tt>True</tt> if job was cancelled by the system.
     *
     * @return <tt>True</tt> if job was cancelled by the system.
     */
    boolean isSystemCanceled() {
        return isSysCancelled.get();
    }

    /**
     *
     * @return Create time.
     */
    long getCreateTime() {
        return createTime;
    }

    /**
     *
     * @return Unique job ID.
     */
    public UUID getJobId() {
        return ses.getJobId();
    }

    /**
     *
     * @return Job context.
     */
    public GridJobContext getJobContext() {
        return jobCtx;
    }

    /**
     *
     * @return Job communication topic.
     */
    String getJobTopic() {
        return jobTopic;
    }

    /**
     *
     * @return Task communication topic.
     */
    String getTaskTopic() {
        return taskTopic;
    }

    /**
     * @return FIXDOC
     */
    public GridTaskSessionImpl getSession() {
        return ses;
    }

    /**
     * Gets job finishing state.
     *
     * @return <tt>true</tt> if job is being finished after execution
     *      and <tt>false</tt> otherwise.
     */
    boolean isFinishing() {
        return isFinishing;
    }

    /**
     *
     * @return Parent task node ID.
     */
    UUID getTaskNodeId() {
        return taskNodeId;
    }

    /**
     *
     * @return FIXDOC
     */
    long getExecuteTime() {
        long startTime = this.startTime.get();
        long finishTime = this.finishTime.get();

        return startTime == 0 ? 0 : finishTime == 0 ?
            System.currentTimeMillis() - startTime : finishTime - startTime;
    }

    /**
     *
     * @return Time job spent on waiting queue.
     */
    long getQueuedTime() {
        long startTime = this.startTime.get();

        return startTime == 0 ? System.currentTimeMillis() - createTime : startTime - createTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getEndTime() {
        return ses.getEndTime();
    }

    /**
     * {@inheritDoc}
     */
    public UUID getTimeoutId() {
        return ses.getJobId();
    }

    /**
     * @return FIXDOC
     */
    boolean isTimedOut() {
        synchronized (mux) {
            return isTimedOut;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onTimeout() {
        synchronized (mux) {
            if (isFinishing == true || isTimedOut == true) {
                return;
            }

            isTimedOut = true;
        }

        log.warning("Job has timed out: " + ses);

        cancel(true);

        recordEvent(GridEventType.JOB_TIMED_OUT, "Job has timed out: " + job.get());
    }

    /**
     * Initializes job. Handles deployments and event recording.
     *
     * @param dep Job deployed task.
     * @return <tt>True</tt> if job was successfully initialized.
     */
    boolean initialize(GridDeploymentClass dep) {
        assert dep != null : "ASSERTION [line=330, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";

        GridException ex = null;

        //noinspection CatchGenericClass
        try {
            // Task deployment.
            deployedTask.set(dep);

            GridJob execJob = GridMarshalHelper.unmarshal(marshaller, jobBytes, dep.getClassLoader());

            // Inject resources.
            procReg.getResourceProcessor().inject(dep, execJob, ses, jobCtx);

            job.set(execJob);

            recordEvent(GridEventType.JOB_QUEUED, "Job got queued for computation.");
        }
        catch (GridException e) {
            log.error("Failed to initilize job [jobId=" + ses.getJobId() + ", ses=" + ses + ']', e);

            ex = e;
        }
        catch (Throwable e) {
            ex = handleThrowable(e);

            assert ex != null : "ASSERTION [line=356, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";
        }
        finally {
            if (ex != null) {
                finishJob(null, ex, true);
            }
        }

        return ex == null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"CatchGenericClass"})
    @Override
    protected void body() {
        assert job.get() != null : "ASSERTION [line=373, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";

        startTime.set(System.currentTimeMillis());

        boolean sendRes = true;

        isStarted = true;

        GridException ex = null;

        Serializable res = null;

        try {
            // Event notification.
            evtListener.onJobStarted(this);

            recordEvent(GridEventType.JOB_STARTED, /*no message for success. */null);

            // If job has timed out, then
            // avoid computation altogether.
            if (isTimedOut() == true) {
                sendRes = false;
            }
            else {
                Thread curThread = Thread.currentThread();

                // Save initial thread context class loader.
                ClassLoader ctxLoader = curThread.getContextClassLoader();

                try {
                    // Deployed task's class loader used as thread context class loader.
                    curThread.setContextClassLoader(deployedTask.get().getClassLoader());

                    res = job.get().execute();

                    if (log.isDebugEnabled() == true) {
                        log.debug("Job execution has successfully finished [job=" + job.get() + ", res=" + res + ']');
                    }
                }
                finally {
                    // Reset context loader to initial value.
                    curThread.setContextClassLoader(ctxLoader);
                }
            }
        }
        catch (GridException e) {
            log.error("Failed to execute job [jobId=" + ses.getJobId() + ", ses=" + ses + ']', e);

            ex = e;
        }
        // Catch Throwable to protect against bad user code except
        // InterruptedException if job is being cancelled.
        catch (Throwable e) {
            ex = handleThrowable(e);

            assert ex != null : "ASSERTION [line=428, file=src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java]";
        }
        finally {
            finishJob(res, ex, sendRes);
        }
    }

    /**
     * Handles {@link Throwable} generic exception for task
     * deployment and execution.
     *
     * @param e Exception.
     * @return Wrapped exception.
     */
    private GridException handleThrowable(Throwable e) {
        String msg;

        GridException ex;

        // Special handling for weird interrupted exception which
        // happens due to JDk 1.5 bug.
        if (e instanceof InterruptedException == true) {
            msg = "Failed to execute job due to interrupted exception.";

            // Turn interrupted exception into checked exception.
            ex = (GridException)new GridException(msg, e).setData(453, "src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java");
        }
        // Special NCDFE handling if P2P is on. We had pretty much questions
        // about this exception and decided to change error message.
        else if ((e instanceof NoClassDefFoundError == true || e instanceof ClassNotFoundException == true)
            && cfg.isPeerClassLoadingEnabled() == true) {
            msg = "Failed to execute job due to class or resource loading exception (make sure that task " +
                "originating node is still in grid and requested class is in the task class path) [jobId=" +
                ses.getJobId() + ", ses=" + ses + ']';

            ex = (GridUserUndeclaredException)new GridUserUndeclaredException(msg, e).setData(463, "src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java");
        }
        else {
            msg = "Failed to execute job due to unexpected runtime exception [jobId=" + ses.getJobId() +
                ", ses=" + ses + ']';

            ex = (GridUserUndeclaredException)new GridUserUndeclaredException(msg, e).setData(469, "src/java/org/gridgain/grid/kernal/processors/job/GridJobWorker.java");
        }

        log.error(msg, e);

        return ex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel() {
        cancel(false);
    }

    /**
     * @param system System flag.
     */
    public void cancel(boolean system) {
        //noinspection CatchGenericClass
        try {
            super.cancel();

            GridJob job = this.job.get();

            isSysCancelled.set(system);

            if (job != null) {
                Thread curThread = Thread.currentThread();

                ClassLoader ctxLdr = curThread.getContextClassLoader();

                try {
                    curThread.setContextClassLoader(ses.getClassLoader());

                    if (log.isInfoEnabled() == true) {
                        log.info("Cancelling job: " + ses);
                    }

                    job.cancel();
                }
                finally {
                    // Set context class loader back to original value.
                    curThread.setContextClassLoader(ctxLdr);
                }
            }

            recordEvent(GridEventType.JOB_CANCELLED, "Job was cancelled: " + job);
        }
        // Catch throwable to protect against bad user code.
        catch (Throwable e) {
            log.error("Failed to cancel job due to undeclared user exception [jobId=" + ses.getJobId() +
                ", ses=" + ses + ']', e);
        }
    }

    /**
     *
     * @param evtType FIXDOC
     * @param msg FIXDOC
     */
    private void recordEvent(GridEventType evtType, String msg) {
        mgrReg.getEventStorageManager().record(evtType, ses.getTaskName(), ses.getUserVersion(), ses.getId(),
            ses.getJobId(), ses.getTaskNodeId(), msg);
    }

    /**
     *
     * @param res FIXDOC
     * @param ex FIXDOC
     * @param sendReply FIXDOC
     */
    @SuppressWarnings({"CatchGenericClass"})
    void finishJob(Serializable res, GridException ex, boolean sendReply) {
        // Avoid finishing a job more than once from
        // different threads.
        synchronized (mux) {
            if (isFinishing == true) {
                return;
            }

            isFinishing = true;
        }

        try {
            // Send response back only if job has not timed out.
            if (isTimedOut() == false) {
                if (sendReply == true) {
                    GridNode senderNode = mgrReg.getDiscoveryManager().getNode(taskNodeId);

                    if (senderNode == null) {
                        log.error("Failed to reply to sender node because it left grid [nodeId=" + taskNodeId +
                            ", ses=" + ses + ", jobId=" + ses.getJobId() + ", job=" + job + ']');

                        // Record job reply failure.
                        recordEvent(GridEventType.JOB_FAILED, "Job reply failed (original task node left grid): " +
                            job.get());
                    }
                    else {
                        try {
                            if (ex != null) {
                                if (isStarted == true) {
                                    // Job failed.
                                    recordEvent(GridEventType.JOB_FAILED, "Job failed due to exception [ex=" +
                                        ex + ", job=" + job.get() + ']');
                                }
                                else {
                                    // Job has been rejected.
                                    recordEvent(GridEventType.JOB_REJECTED, "Job has been rejected before exception " +
                                        "[ex=" + ex + ", job=" + job.get() + ']');
                                }
                            }
                            else {
                                recordEvent(GridEventType.JOB_FINISHED, /*no message for success. */null);
                            }

                            GridJobExecuteResponse jobRes = new GridJobExecuteResponse(
                                cfg.getNodeId(),
                                ses.getId(),
                                ses.getJobId(),
                                GridMarshalHelper.marshal(marshaller, ex),
                                GridMarshalHelper.marshal(marshaller,res),
                                GridMarshalHelper.marshal(marshaller, jobCtx.getAttributes()),
                                isCancelled());

                            // Job response topic.
                            String topic = TASK.topic(ses.getJobId(), locNodeId);

                            long timeout = ses.getEndTime() - System.currentTimeMillis();

                            if (timeout <= 0) {
                                // Ignore the actual timeout and send response anyway.
                                timeout = 1;
                            }

                            // Send response to designated job topic.
                            mgrReg.getCommunicationManager().sendOrderedMessage(
                                senderNode,
                                topic,
                                mgrReg.getCommunicationManager().getNextMessageId(topic),
                                jobRes,
                                POOLED_THREAD,
                                timeout);
                        }
                        catch (GridException e) {
                            // The only option here is to log, as we must assume that resending will fail too.
                            if (isDeadNode(taskNodeId) == true) {
                                // Avoid stack trace for left nodes.
                                log.error("Failed to reply to sender node because it left grid [nodeId=" + taskNodeId +
                                    ", jobId=" + ses.getJobId() + ", ses=" + ses + ", job=" + job.get() + ']');
                            }
                            else {
                                log.error("Error sending reply for job [nodeId=" + senderNode.getId() + ", jobId=" +
                                    ses.getJobId() + ", ses=" + ses + ", job=" + job.get() + ']', e);
                            }

                            // Record job reply failure.
                            recordEvent(GridEventType.JOB_FAILED, "Failed to send reply for job [nodeId=" +
                                taskNodeId + ", job=" + job + ']');
                        }
                        // Catching interrupted exception because
                        // it gets thrown for some reason.
                        catch (Exception e) {
                            String msg = "Failed to send reply for job due to interrupted " +
                                "exception [nodeId=" + taskNodeId + ", job=" + job + ']';

                            log.error(msg, e);

                            // Record job reply failure.
                            recordEvent(GridEventType.JOB_FAILED, msg);
                        }
                    }
                }
                else {
                    if (ex != null) {
                        if (isStarted == true) {
                            // Job failed.
                            recordEvent(GridEventType.JOB_FAILED, "Job failed due to exception [ex=" +
                                ex + ", job=" + job.get() + ']');
                        }
                        else {
                            // Job has been rejected.
                            recordEvent(GridEventType.JOB_REJECTED, "Job has been rejected before exception [ex=" +
                                ex + ", job=" + job.get() + ']');
                        }
                    }
                    else {
                        recordEvent(GridEventType.JOB_FINISHED, /*no message for success. */null);
                    }
                }
            }
            // Job timed out.
            else {
                // Record failure for timed out job.
                recordEvent(GridEventType.JOB_FAILED, "Job failed due to timeout: " + job.get());
            }
        }
        finally {
            // Listener callback
            evtListener.onJobFinished(this);

            finishTime.set(System.currentTimeMillis());
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
    @Override
    public boolean equals(Object obj) {
        return ses.getJobId().equals(((GridJobWorker)obj).ses.getJobId()) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return ses.getJobId().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobWorker.class, this);
    }
}
