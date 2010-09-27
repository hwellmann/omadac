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

package org.gridgain.grid.spi.discovery;

import java.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.spi.metrics.*;

/**
 * Adapter for {@link GridLocalMetrics} interface.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridDiscoveryMetricsAdapter implements GridNodeMetrics, Externalizable {
    /** */
    private long lastUpdateTime = -1;

    /** */
    private int maxActiveJobs = -1;

    /** */
    private int curActiveJobs = -1;

    /** */
    private float avgActiveJobs = -1;

    /** */
    private int maxWaitingJobs = -1;

    /** */
    private int curWaitingJobs = -1;

    /** */
    private float avgWaitingJobs = -1;

    /** */
    private int maxRejectedJobs = -1;

    /** */
    private int curRejectedJobs = -1;

    /** */
    private float avgRejectedJobs = -1;

    /** */
    private int maxCancelledJobs = -1;

    /** */
    private int curCancelledJobs = -1;

    /** */
    private float avgCancelledJobs = -1;

    /** */
    private int totalRejectedJobs = -1;

    /** */
    private int totalCancelledJobs = -1;

    /** */
    private int totalExecutedJobs = -1;

    /** */
    private long maxJobWaitTime = -1;

    /** */
    private long curJobWaitTime = -1;

    /** */
    private double avgJobWaitTime = -1;

    /** */
    private long maxJobExecTime = -1;

    /** */
    private long curJobExecTime = -1;

    /** */
    private double avgJobExecTime = -1;

    /** */
    private long totalIdleTime = -1;

    /** */
    private long curIdleTime = -1;

    /** */
    private int availProcs = -1;

    /** */
    private double load = -1;

    /** */
    private double avgLoad = -1;

    /** */
    private long heapInit = -1;

    /** */
    private long heapUsed = -1;

    /** */
    private long heapCommitted = -1;

    /** */
    private long heapMax = -1;

    /** */
    private long nonHeapInit = -1;

    /** */
    private long nonHeapUsed = -1;

    /** */
    private long nonHeapCommitted = -1;

    /** */
    private long nonHeapMax = -1;

    /** */
    private long upTime = -1;

    /** */
    private long startTime = -1;

    /** */
    private int threadCnt = -1;

    /** */
    private int peakThreadCnt = -1;

    /** */
    private long startedThreadCnt = -1;

    /** */
    private int daemonThreadCnt = -1;

    /**
     * {@inheritDoc}
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * Sets last update time.
     *
     * @param lastUpdateTime Last update time.
     */
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumActiveJobs() {
        return maxActiveJobs;
    }

    /**
     * Sets max active jobs.
     *
     * @param maxActiveJobs Max active jobs.
     */
    public void setMaximumActiveJobs(int maxActiveJobs) {
        this.maxActiveJobs = maxActiveJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentActiveJobs() {
        return curActiveJobs;
    }

    /**
     * Sets current active jobs.
     *
     * @param curActiveJobs Current active jobs.
     */
    public void setCurrentActiveJobs(int curActiveJobs) {
        this.curActiveJobs = curActiveJobs;
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageActiveJobs() {
        return avgActiveJobs;
    }

    /**
     * Sets average active jobs.
     *
     * @param avgActiveJobs Average active jobs.
     */
    public void setAverageActiveJobs(float avgActiveJobs) {
        this.avgActiveJobs = avgActiveJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumWaitingJobs() {
        return maxWaitingJobs;
    }

    /**
     * Sets maximum waiting jobs.
     *
     * @param maxWaitingJobs Maximum waiting jobs.
     */
    public void setMaximumWaitingJobs(int maxWaitingJobs) {
        this.maxWaitingJobs = maxWaitingJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentWaitingJobs() {
        return curWaitingJobs;
    }

    /**
     * Sets current waiting jobs.
     *
     * @param curWaitingJobs Current waiting jobs.
     */
    public void setCurrentWaitingJobs(int curWaitingJobs) {
        this.curWaitingJobs = curWaitingJobs;
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageWaitingJobs() {
        return avgWaitingJobs;
    }

    /**
     * Sets average waiting jobs.
     *
     * @param avgWaitingJobs Average waiting jobs.
     */
    public void setAverageWaitingJobs(float avgWaitingJobs) {
        this.avgWaitingJobs = avgWaitingJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumRejectedJobs() {
        return maxRejectedJobs;
    }

    /**
     *
     * @param maxRejectedJobs Maximum number of jobs rejected during a single collision resolution event.
     */
    public void setMaximumRejectedJobs(int maxRejectedJobs) {
        this.maxRejectedJobs = maxRejectedJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentRejectedJobs() {
        return curRejectedJobs;
    }

    /**
     *
     * @param curRejectedJobs Number of jobs rejected during most recent collision resolution.
     */
    public void setCurrentRejectedJobs(int curRejectedJobs) {
        this.curRejectedJobs = curRejectedJobs;
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageRejectedJobs() {
        return avgRejectedJobs;
    }

    /**
     *
     * @param avgRejectedJobs Average number of jobs this node rejects.
     */
    public void setAverageRejectedJobs(float avgRejectedJobs) {
        this.avgRejectedJobs = avgRejectedJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalRejectedJobs() {
        return totalRejectedJobs;
    }

    /**
     *
     * @param totalRejectedJobs Total number of jobs this node ever rejected.
     */
    public void setTotalRejectedJobs(int totalRejectedJobs) {
        this.totalRejectedJobs = totalRejectedJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumCancelledJobs() {
        return maxCancelledJobs;
    }

    /**
     * Sets maximum cancelled jobs.
     *
     * @param maxCancelledJobs Maximum cancelled jobs.
     */
    public void setMaximumCancelledJobs(int maxCancelledJobs) {
        this.maxCancelledJobs = maxCancelledJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentCancelledJobs() {
        return curCancelledJobs;
    }

    /**
     * Sets current cancelled jobs.
     *
     * @param curCancelledJobs Current cancelled jobs.
     */
    public void setCurrentCancelledJobs(int curCancelledJobs) {
        this.curCancelledJobs = curCancelledJobs;
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageCancelledJobs() {
        return avgCancelledJobs;
    }

    /**
     * Sets average cancelled jobs.
     *
     * @param avgCancelledJobs Average cancelled jobs.
     */
    public void setAverageCancelledJobs(float avgCancelledJobs) {
        this.avgCancelledJobs = avgCancelledJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalExecutedJobs() {
        return totalExecutedJobs;
    }

    /**
     * Sets total active jobs.
     *
     * @param totalExecutedJobs Total active jobs.
     */
    public void setTotalExecutedJobs(int totalExecutedJobs) {
        this.totalExecutedJobs = totalExecutedJobs;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalCancelledJobs() {
        return totalCancelledJobs;
    }

    /**
     * Sets total cancelled jobs.
     *
     * @param totalCancelledJobs Total cancelled jobs.
     */
    public void setTotalCancelledJobs(int totalCancelledJobs) {
        this.totalCancelledJobs = totalCancelledJobs;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaximumJobWaitTime() {
        return maxJobWaitTime;
    }

    /**
     * Sets max job wait time.
     *
     * @param maxJobWaitTime Max job wait time.
     */
    public void setMaximumJobWaitTime(long maxJobWaitTime) {
        this.maxJobWaitTime = maxJobWaitTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentJobWaitTime() {
        return curJobWaitTime;
    }

    /**
     * Sets current job wait time.
     *
     * @param curJobWaitTime Current job wait time.
     */
    public void setCurrentJobWaitTime(long curJobWaitTime) {
        this.curJobWaitTime = curJobWaitTime;
    }

    /**
     * {@inheritDoc}
     */
    public double getAverageJobWaitTime() {
        return avgJobWaitTime;
    }

    /**
     * Sets average job wait time.
     *
     * @param avgJobWaitTime Average job wait time.
     */
    public void setAverageJobWaitTime(double avgJobWaitTime) {
        this.avgJobWaitTime = avgJobWaitTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaximumJobExecuteTime() {
        return maxJobExecTime;
    }

    /**
     * Sets maximum job execution time.
     *
     * @param maxJobExecTime Maximum job execution time.
     */
    public void setMaximumJobExecuteTime(long maxJobExecTime) {
        this.maxJobExecTime = maxJobExecTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentJobExecuteTime() {
        return curJobExecTime;
    }

    /**
     * Sets current job execute tiem.
     *
     * @param curJobExecTime Current job execute time.
     */
    public void setCurrentJobExecuteTime(long curJobExecTime) {
        this.curJobExecTime = curJobExecTime;
    }

    /**
     * {@inheritDoc}
     */
    public double getAverageJobExecuteTime() {
        return avgJobExecTime;
    }

    /**
     * Sets average job execution time.
     *
     * @param avgJobExecTime Average job execution time.
     */
    public void setAverageJobExecuteTime(double avgJobExecTime) {
        this.avgJobExecTime = avgJobExecTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getTotalBusyTime() {
        return getUpTime() - getTotalIdleTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getTotalIdleTime() {
        return totalIdleTime;
    }

    /**
     * Set total node idle time.
     *
     * @param totalIdleTime Total node idle time.
     */
    public void setTotalIdleTime(long totalIdleTime) {
        this.totalIdleTime = totalIdleTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentIdleTime() {
        return curIdleTime;
    }

    /**
     * Sets time elapsed since execution of last job.
     *
     * @param curIdleTime Time elapsed since execution of last job.
     */
    public void setCurrentIdleTime(long curIdleTime) {
        this.curIdleTime = curIdleTime;
    }

    /**
     * {@inheritDoc}
     */
    public float getBusyTimePercentage() {
        return 1 - getIdleTimePercentage();
    }

    /**
     * {@inheritDoc}
     */
    public float getIdleTimePercentage() {
        return getTotalIdleTime() / (float)getUpTime();
    }

    /**
     * {@inheritDoc}
     */
    public int getAvailableProcessors() {
        return availProcs;
    }

    /**
     * {@inheritDoc}
     */
    public double getCurrentCpuLoad() {
        return load;
    }

    /**
     * {@inheritDoc}
     */
    public double getAverageCpuLoad() {
        return avgLoad;
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryInitialized() {
        return heapInit;
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryUsed() {
        return heapUsed;
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryCommitted() {
        return heapCommitted;
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryMaximum() {
        return heapMax;
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryInitialized() {
        return nonHeapInit;
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryUsed() {
        return nonHeapUsed;
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryCommitted() {
        return nonHeapCommitted;
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryMaximum() {
        return nonHeapMax;
    }

    /**
     * {@inheritDoc}
     */
    public long getUpTime() {
        return upTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentThreadCount() {
        return threadCnt;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumThreadCount() {
        return peakThreadCnt;
    }

    /**
     * {@inheritDoc}
     */
    public long getTotalStartedThreadCount() {
        return startedThreadCnt;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentDaemonThreadCount() {
        return daemonThreadCnt;
    }

    /**
     * Sets available processors.
     *
     * @param availProcs Available processors.
     */
    public void setAvailableProcessors(int availProcs) {
        this.availProcs = availProcs;
    }

    /**
     * Sets current CPU load.
     *
     * @param load Current CPU load.
     */
    public void setCurrentCpuLoad(double load) {
        this.load = load;
    }

    /**
     * Sets CPU load average over the metrics history.
     *
     * @param avgLoad CPU load average.
     */
    public void setAverageCpuLoad(double avgLoad) {
        this.avgLoad = avgLoad;
    }

    /**
     * Sets heap initial memory.
     *
     * @param heapInit Heap initial memory.
     */
    public void setHeapMemoryInitialized(long heapInit) {
        this.heapInit = heapInit;
    }

    /**
     * Sets used heap memory.
     *
     * @param heapUsed Used heap memory.
     */
    public void setHeapMemoryUsed(long heapUsed) {
        this.heapUsed = heapUsed;
    }

    /**
     * Sets committed heap memory.
     *
     * @param heapCommitted Committed heap memory.
     */
    public void setHeapMemoryCommitted(long heapCommitted) {
        this.heapCommitted = heapCommitted;
    }

    /**
     * Sets maximum possible heap memory.
     *
     * @param heapMax Maximum possible heap memory.
     */
    public void setHeapMemoryMaximum(long heapMax) {
        this.heapMax = heapMax;
    }

    /**
     * Sets initial non-heap memory.
     *
     * @param nonHeapInit Initial non-heap memory.
     */
    public void setNonHeapMemoryInitialized(long nonHeapInit) {
        this.nonHeapInit = nonHeapInit;
    }

    /**
     * Sets used non-heap memory.
     *
     * @param nonHeapUsed Used non-heap memory.
     */
    public void setNonHeapMemoryUsed(long nonHeapUsed) {
        this.nonHeapUsed = nonHeapUsed;
    }

    /**
     * Sets committed non-heap memory.
     *
     * @param nonHeapCommitted Committed non-heap memory.
     */
    public void setNonHeapMemoryCommitted(long nonHeapCommitted) {
        this.nonHeapCommitted = nonHeapCommitted;
    }

    /**
     * Sets maximum possible non-heap memory.
     *
     * @param nonHeapMax Maximum possible non-heap memory.
     */
    public void setNonHeapMemoryMaximum(long nonHeapMax) {
        this.nonHeapMax = nonHeapMax;
    }

    /**
     * Sets VM up time.
     *
     * @param upTime VN up time.
     */
    public void setUpTime(long upTime) {
        this.upTime = upTime;
    }

    /**
     * Sets VM start time.
     *
     * @param startTime VM start time.
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets thread count.
     *
     * @param threadCnt Thread count.
     */
    public void setCurrentThreadCount(int threadCnt) {
        this.threadCnt = threadCnt;
    }

    /**
     * Sets peak thread count.
     *
     * @param peakThreadCnt Peak thread count.
     */
    public void setMaximumThreadCount(int peakThreadCnt) {
        this.peakThreadCnt = peakThreadCnt;
    }

    /**
     * Sets started thread count.
     *
     * @param startedThreadCnt Started thread count.
     */
    public void setTotalStartedThreadCount(long startedThreadCnt) {
        this.startedThreadCnt = startedThreadCnt;
    }

    /**
     * Sets daemon thread count.
     *
     * @param daemonThreadCnt Daemon thread count.
     */
    public void setCurrentDaemonThreadCount(int daemonThreadCnt) {
        this.daemonThreadCnt = daemonThreadCnt;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(lastUpdateTime);
        out.writeInt(maxActiveJobs);
        out.writeInt(curActiveJobs);
        out.writeFloat(avgActiveJobs);
        out.writeInt(maxWaitingJobs);
        out.writeInt(curWaitingJobs);
        out.writeFloat(avgWaitingJobs);
        out.writeInt(maxCancelledJobs);
        out.writeInt(curCancelledJobs);
        out.writeFloat(avgCancelledJobs);
        out.writeInt(totalExecutedJobs);
        out.writeInt(totalCancelledJobs);
        out.writeLong(maxJobWaitTime);
        out.writeLong(curJobWaitTime);
        out.writeDouble(avgJobWaitTime);
        out.writeLong(maxJobExecTime);
        out.writeLong(curJobExecTime);
        out.writeDouble(avgJobExecTime);
        out.writeLong(curIdleTime);
        out.writeLong(totalIdleTime);
        out.writeInt(availProcs);
        out.writeDouble(load);
        out.writeDouble(avgLoad);
        out.writeLong(heapInit);
        out.writeLong(heapUsed);
        out.writeLong(heapCommitted);
        out.writeLong(heapMax);
        out.writeLong(nonHeapInit);
        out.writeLong(nonHeapUsed);
        out.writeLong(nonHeapCommitted);
        out.writeLong(nonHeapMax);
        out.writeLong(upTime);
        out.writeLong(startTime);
        out.writeInt(threadCnt);
        out.writeInt(peakThreadCnt);
        out.writeLong(startedThreadCnt);
        out.writeInt(daemonThreadCnt);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException {
        lastUpdateTime = in.readLong();
        maxActiveJobs = in.readInt();
        curActiveJobs = in.readInt();
        avgActiveJobs = in.readFloat();
        maxWaitingJobs = in.readInt();
        curWaitingJobs = in.readInt();
        avgWaitingJobs = in.readFloat();
        maxCancelledJobs = in.readInt();
        curCancelledJobs = in.readInt();
        avgCancelledJobs = in.readFloat();
        totalExecutedJobs = in.readInt();
        totalCancelledJobs = in.readInt();
        maxJobWaitTime = in.readLong();
        curJobWaitTime = in.readLong();
        avgJobWaitTime = in.readDouble();
        maxJobExecTime = in.readLong();
        curJobExecTime = in.readLong();
        avgJobExecTime = in.readDouble();
        curIdleTime = in.readLong();
        totalIdleTime = in.readLong();
        availProcs = in.readInt();
        load = in.readDouble();
        avgLoad = in.readDouble();
        heapInit = in.readLong();
        heapUsed = in.readLong();
        heapCommitted = in.readLong();
        heapMax = in.readLong();
        nonHeapInit = in.readLong();
        nonHeapUsed = in.readLong();
        nonHeapCommitted = in.readLong();
        nonHeapMax = in.readLong();
        upTime = in.readLong();
        startTime = in.readLong();
        threadCnt = in.readInt();
        peakThreadCnt = in.readInt();
        startedThreadCnt = in.readLong();
        daemonThreadCnt = in.readInt();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        GridDiscoveryMetricsAdapter other = (GridDiscoveryMetricsAdapter)obj;

        if (availProcs != other.availProcs) {
            return false;
        }

        if (avgActiveJobs != other.avgActiveJobs) {
            return false;
        }

        if (avgCancelledJobs != other.avgCancelledJobs) {
            return false;
        }

        if (avgJobExecTime != other.avgJobExecTime) {
            return false;
        }

        if (avgJobWaitTime != other.avgJobWaitTime) {
            return false;
        }

        if (avgRejectedJobs != other.avgRejectedJobs) {
            return false;
        }

        if (avgWaitingJobs != other.avgWaitingJobs) {
            return false;
        }

        if (curActiveJobs != other.curActiveJobs) {
            return false;
        }

        if (curCancelledJobs != other.curCancelledJobs) {
            return false;
        }

        if (curIdleTime != other.curIdleTime) {
            return false;
        }

        if (curJobExecTime != other.curJobExecTime) {
            return false;
        }

        if (curJobWaitTime != other.curJobWaitTime) {
            return false;
        }

        if (curRejectedJobs != other.curRejectedJobs) {
            return false;
        }

        if (curWaitingJobs != other.curWaitingJobs) {
            return false;
        }

        if (daemonThreadCnt != other.daemonThreadCnt) {
            return false;
        }

        if (heapCommitted != other.heapCommitted) {
            return false;
        }

        if (heapInit != other.heapInit) {
            return false;
        }

        if (heapMax != other.heapMax) {
            return false;
        }

        if (heapUsed != other.heapUsed) {
            return false;
        }

        if (Double.compare(other.load, load) != 0) {
            return false;
        }

        if (maxActiveJobs != other.maxActiveJobs) {
            return false;
        }

        if (maxCancelledJobs != other.maxCancelledJobs) {
            return false;
        }

        if (maxJobExecTime != other.maxJobExecTime) {
            return false;
        }

        if (maxJobWaitTime != other.maxJobWaitTime) {
            return false;
        }

        if (maxRejectedJobs != other.maxRejectedJobs) {
            return false;
        }

        if (maxWaitingJobs != other.maxWaitingJobs) {
            return false;
        }

        if (nonHeapCommitted != other.nonHeapCommitted) {
            return false;
        }

        if (nonHeapInit != other.nonHeapInit) {
            return false;
        }

        if (nonHeapMax != other.nonHeapMax) {
            return false;
        }

        if (nonHeapUsed != other.nonHeapUsed) {
            return false;
        }

        if (peakThreadCnt != other.peakThreadCnt) {
            return false;
        }

        if (startTime != other.startTime) {
            return false;
        }

        if (startedThreadCnt != other.startedThreadCnt) {
            return false;
        }

        if (threadCnt != other.threadCnt) {
            return false;
        }

        if (totalCancelledJobs != other.totalCancelledJobs) {
            return false;
        }

        if (totalExecutedJobs != other.totalExecutedJobs) {
            return false;
        }

        if (totalIdleTime != other.totalIdleTime) {
            return false;
        }

        if (totalRejectedJobs != other.totalRejectedJobs) {
            return false;
        }

        //noinspection RedundantIfStatement
        if (upTime != other.upTime) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDiscoveryMetricsAdapter.class, this);
    }
}
