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

package org.gridgain.grid.kernal.processors.jobmetrics;

import java.util.concurrent.atomic.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Job metrics snapshot.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobMetricsSnapshot {
    /** */
    private final long timestamp = System.currentTimeMillis();

    /** */
    private int started = 0;

    /** */
    private int activeJobs = 0;

    /** */
    private int passiveJobs = 0;

    /** */
    private int cancelJobs = 0;

    /** */
    private int rejectJobs = 0;

    /** */
    private long execTime = 0;

    /** */
    private long waitTime = 0;

    /** */
    private long maxExecTime = 0;

    /** */
    private long maxWaitTime = 0;

    /** */
    private int finished = 0;

    /** */
    private double cpuLoad = 0;

    /** */
    private AtomicBoolean deleted = new AtomicBoolean(false);

    /**
     * @return The activeJobs.
     */
    public int getActiveJobs() {
        return activeJobs;
    }

    /**
     * @param activeJobs The activeJobs to set.
     */
    public void setActiveJobs(int activeJobs) {
        this.activeJobs = activeJobs;
    }

    /**
     * @return The passiveJobs.
     */
    public int getPassiveJobs() {
        return passiveJobs;
    }

    /**
     * @param passiveJobs The passiveJobs to set.
     */
    public void setPassiveJobs(int passiveJobs) {
        this.passiveJobs = passiveJobs;
    }

    /**
     * @return The cancelJobs.
     */
    public int getCancelJobs() {
        return cancelJobs;
    }

    /**
     * @param cancelJobs The cancelJobs to set.
     */
    public void setCancelJobs(int cancelJobs) {
        this.cancelJobs = cancelJobs;
    }

    /**
     * @return The rejectJobs.
     */
    public int getRejectJobs() {
        return rejectJobs;
    }

    /**
     * @param rejectJobs The rejectJobs to set.
     */
    public void setRejectJobs(int rejectJobs) {
        this.rejectJobs = rejectJobs;
    }

    /**
     * @return The execTime.
     */
    public long getExecutionTime() {
        return execTime;
    }

    /**
     * @param execTime The execTime to set.
     */
    public void setExecutionTime(long execTime) {
        this.execTime = execTime;
    }

    /**
     * @return The waitTime.
     */
    public long getWaitTime() {
        return waitTime;
    }

    /**
     * @param waitTime The waitTime to set.
     */
    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }

    /**
     * @return The maxExecTime.
     */
    public long getMaximumExecutionTime() {
        return maxExecTime;
    }

    /**
     * @param maxExecTime The maxExecTime to set.
     */
    public void setMaximumExecutionTime(long maxExecTime) {
        this.maxExecTime = maxExecTime;
    }

    /**
     * @return The maxWaitTime.
     */
    public long getMaximumWaitTime() {
        return maxWaitTime;
    }

    /**
     * @param maxWaitTime The maxWaitTime to set.
     */
    public void setMaximumWaitTime(long maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * @return The timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return Number of finished jobs for this snapshot.
     */
    public int getFinishedJobs() {
        return finished;
    }

    /**
     * @param finished Number of finished jobs for this snapshot.
     */
    public void setFinishedJobs(int finished) {
        this.finished = finished;
    }

    /**
     *
     * @return Started jobs.
     */
    public int getStartedJobs() {
        return started;
    }

    /**
     *
     * @param startedJobs Started jobs.
     */
    public void setStartedJobs(int startedJobs) {
        this.started = startedJobs;
    }

    /**
     *
     * @return Current CPU load.
     */
    public double getCpuLoad() {
        return cpuLoad;
    }

    /**
     *
     * @param cpuLoad Current CPU load.
     */
    public void setCpuLoad(double cpuLoad) {
        this.cpuLoad = cpuLoad;
    }

    /**
     * Marks this snapshot as deleted if it has not been deleted yet.
     *
     * @return <tt>True</tt> if snapshot has not been deleted yet.
     */
    boolean delete() {
        return deleted.compareAndSet(false, true);
    }

    /**
     * Checks if snapshot has been deleted.
     *
     * @return <tt>True</tt> if snapshot has been deleted.
     */
    boolean isDeleted() {
        return deleted.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobMetricsSnapshot.class, this);
    }
}
