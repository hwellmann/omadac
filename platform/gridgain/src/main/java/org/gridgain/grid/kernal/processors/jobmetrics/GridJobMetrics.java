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

import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobMetrics {
    /** */
    private int maxActiveJobs = 0;

    /** */
    private int curActiveJobs = 0;

    /** */
    private float avgActiveJobs = 0;

    /** */
    private int maxWaitingJobs = 0;

    /** */
    private int curWaitingJobs = 0;

    /** */
    private float avgWaitingJobs = 0;

    /** */
    private int maxCancelledJobs = 0;

    /** */
    private int curCancelledJobs = 0;

    /** */
    private float avgCancelledJobs = 0;

    /** */
    private int maxRejectedJobs = 0;

    /** */
    private int curRejectedJobs = 0;

    /** */
    private float avgRejectedJobs = 0;

    /** */
    private int totalRejectedJobs = 0;

    /** */
    private int totalCancelledJobs = 0;

    /** */
    private int totalExecutedJobs = 0;

    /** */
    private long maxJobWaitTime = 0;

    /** */
    private long curJobWaitTime = 0;

    /** */
    private double avgJobWaitTime = 0;

    /** */
    private long maxJobExecTime = 0;

    /** */
    private long curJobExecTime = 0;

    /** */
    private double avgJobExecTime = 0;

    /** */
    private long totalIdleTime = 0;

    /** */
    private long curIdleTime = 0;

    /** */
    private double cpuLoadAvg = 0;

    /**
     *
     * @return Maximum active jobs.
     */
    public int getMaximumActiveJobs() {
        return maxActiveJobs;
    }

    /**
     *
     * @return Number of jobs currently executing.
     */
    public int getCurrentActiveJobs() {
        return curActiveJobs;
    }

    /**
     *
     * @return Average number of concurrently executing active jobs.
     */
    public float getAverageActiveJobs() {
        return avgActiveJobs;
    }

    /**
     *
     * @return Maximum number of jobs waiting concurrently in the queue.
     */
    public int getMaximumWaitingJobs() {
        return maxWaitingJobs;
    }

    /**
     *
     * @return Current waiting jobs.
     */
    public int getCurrentWaitingJobs() {
        return curWaitingJobs;
    }

    /**
     *
     * @return Average waiting jobs.
     */
    public float getAverageWaitingJobs() {
        return avgWaitingJobs;
    }

    /**
     *
     * @return Maximum number of rejected jobs during a single collision resolution.
     */
    public int getMaximumRejectedJobs() {
        return maxRejectedJobs;
    }

    /**
     *
     * @return Jobs rejected during last collision resolution on this node.
     */
    public int getCurrentRejectedJobs() {
        return curRejectedJobs;
    }

    /**
     *
     * @return Average number of jobs rejected with every collision resolution.
     */
    public float getAverageRejectedJobs() {
        return avgRejectedJobs;
    }

    /**
     *
     * @return Total number of jobs rejected on this node.
     */
    public int getTotalRejectedJobs() {
        return totalRejectedJobs;
    }

    /**
     *
     * @return Maximum canceled jobs.
     */
    public int getMaximumCancelledJobs() {
        return maxCancelledJobs;
    }

    /**
     *
     * @return Current canceled jobs.
     */
    public int getCurrentCancelledJobs() {
        return curCancelledJobs;
    }

    /**
     *
     * @return Average canceled jobs.
     */
    public float getAverageCancelledJobs() {
        return avgCancelledJobs;
    }

    /**
     *
     * @return Total executed jobs.
     */
    public int getTotalExecutedJobs() {
        return totalExecutedJobs;
    }

    /**
     *
     * @return Total canceled jobs.
     */
    public int getTotalCancelledJobs() {
        return totalCancelledJobs;
    }

    /**
     *
     * @return Maximum job wait time.
     */
    public long getMaximumJobWaitTime() {
        return maxJobWaitTime;
    }

    /**
     *
     * @return Current job wait time.
     */
    public long getCurrentJobWaitTime() {
        return curJobWaitTime;
    }

    /**
     *
     * @return Average job wait time.
     */
    public double getAverageJobWaitTime() {
        return avgJobWaitTime;
    }

    /**
     *
     * @return Maximum job execute time.
     */
    public long getMaximumJobExecuteTime() {
        return maxJobExecTime;
    }

    /**
     * Gets execution time of longest job currently running.
     *
     * @return Execution time of longest job currently running.
     */
    public long getCurrentJobExecuteTime() {
        return curJobExecTime;
    }

    /**
     * Gets average job execution time.
     *
     * @return Average job execution time.
     */
    public double getAverageJobExecuteTime() {
        return avgJobExecTime;
    }

    /**
     * Gets total idle time.
     *
     * @return Total idle time.
     */
    public long getTotalIdleTime() {
        return totalIdleTime;
    }

    /**
     * Gets current idle time.
     *
     * @return Current idle time.
     */
    public long getCurrentIdleTime() {
        return curIdleTime;
    }

    /**
     * Gets CPU load average.
     *
     * @return CPU load average.
     */
    public double getAverageCpuLoad() {
        return cpuLoadAvg;
    }

    /**
     * @param maxActiveJobs The maxActiveJobs to set.
     */
    void setMaximumActiveJobs(int maxActiveJobs) {
        this.maxActiveJobs = maxActiveJobs;
    }

    /**
     * @param curActiveJobs The curActiveJobs to set.
     */
    void setCurrentActiveJobs(int curActiveJobs) {
        this.curActiveJobs = curActiveJobs;
    }

    /**
     * @param avgActiveJobs The avgActiveJobs to set.
     */
    void setAverageActiveJobs(float avgActiveJobs) {
        this.avgActiveJobs = avgActiveJobs;
    }

    /**
     * @param maxWaitingJobs The maxWaitingJobs to set.
     */
    void setMaximumWaitingJobs(int maxWaitingJobs) {
        this.maxWaitingJobs = maxWaitingJobs;
    }

    /**
     * @param curWaitingJobs The curWaitingJobs to set.
     */
    void setCurrentWaitingJobs(int curWaitingJobs) {
        this.curWaitingJobs = curWaitingJobs;
    }

    /**
     * @param avgWaitingJobs The avgWaitingJobs to set.
     */
    void setAverageWaitingJobs(float avgWaitingJobs) {
        this.avgWaitingJobs = avgWaitingJobs;
    }

    /**
     * @param maxCancelledJobs The maxCancelledJobs to set.
     */
    void setMaximumCancelledJobs(int maxCancelledJobs) {
        this.maxCancelledJobs = maxCancelledJobs;
    }

    /**
     * @param curCancelledJobs The curCancelledJobs to set.
     */
    void setCurrentCancelledJobs(int curCancelledJobs) {
        this.curCancelledJobs = curCancelledJobs;
    }

    /**
     * @param avgCancelledJobs The avgCancelledJobs to set.
     */
    void setAverageCancelledJobs(float avgCancelledJobs) {
        this.avgCancelledJobs = avgCancelledJobs;
    }

    /**
     * @param maxRejectedJobs The maxRejectedJobs to set.
     */
    void setMaximumRejectedJobs(int maxRejectedJobs) {
        this.maxRejectedJobs = maxRejectedJobs;
    }

    /**
     * @param curRejectedJobs The curRejectedJobs to set.
     */
    void setCurrentRejectedJobs(int curRejectedJobs) {
        this.curRejectedJobs = curRejectedJobs;
    }

    /**
     * @param avgRejectedJobs The avgRejectedJobs to set.
     */
    void setAverageRejectedJobs(float avgRejectedJobs) {
        this.avgRejectedJobs = avgRejectedJobs;
    }

    /**
     * @param totalRejectedJobs The totalRejectedJobs to set.
     */
    void setTotalRejectedJobs(int totalRejectedJobs) {
        this.totalRejectedJobs = totalRejectedJobs;
    }

    /**
     * @param totalCancelledJobs The totalCancelledJobs to set.
     */
    void setTotalCancelledJobs(int totalCancelledJobs) {
        this.totalCancelledJobs = totalCancelledJobs;
    }

    /**
     * @param totalExecutedJobs The totalExecutedJobs to set.
     */
    void setTotalExecutedJobs(int totalExecutedJobs) {
        this.totalExecutedJobs = totalExecutedJobs;
    }

    /**
     * @param maxJobWaitTime The maxJobWaitTime to set.
     */
    void setMaximumJobWaitTime(long maxJobWaitTime) {
        this.maxJobWaitTime = maxJobWaitTime;
    }

    /**
     * @param curJobWaitTime The curJobWaitTime to set.
     */
    void setCurrentJobWaitTime(long curJobWaitTime) {
        this.curJobWaitTime = curJobWaitTime;
    }

    /**
     * @param avgJobWaitTime The avgJobWaitTime to set.
     */
    void setAverageJobWaitTime(double avgJobWaitTime) {
        this.avgJobWaitTime = avgJobWaitTime;
    }

    /**
     * @param maxJobExecTime The maxJobExecTime to set.
     */
    void setMaxJobExecutionTime(long maxJobExecTime) {
        this.maxJobExecTime = maxJobExecTime;
    }

    /**
     * @param curJobExecTime The curJobExecTime to set.
     */
    void setCurrentJobExecutionTime(long curJobExecTime) {
        this.curJobExecTime = curJobExecTime;
    }

    /**
     * @param avgJobExecTime The avgJobExecTime to set.
     */
    void setAverageJobExecutionTime(double avgJobExecTime) {
        this.avgJobExecTime = avgJobExecTime;
    }

    /**
     * @param totalIdleTime The totalIdleTime to set.
     */
    void setTotalIdleTime(long totalIdleTime) {
        this.totalIdleTime = totalIdleTime;
    }

    /**
     * @param curIdleTime The curIdleTime to set.
     */
    void setCurrentIdleTime(long curIdleTime) {
        this.curIdleTime = curIdleTime;
    }

    /**
     * @param cpuLoadAvg CPU load average.
     */
    void setAverageCpuLoad(double cpuLoadAvg) {
        this.cpuLoadAvg = cpuLoadAvg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobMetrics.class, this);
    }
}
