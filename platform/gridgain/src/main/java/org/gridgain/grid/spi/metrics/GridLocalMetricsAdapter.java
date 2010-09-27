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

package org.gridgain.grid.spi.metrics;

import java.io.*;

import org.gridgain.apache.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Adapter for {@link GridLocalMetrics} interface.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public class GridLocalMetricsAdapter implements GridLocalMetrics, Externalizable {
    /** */
    private int availProcs = -1;

    /** */
    private double load = -1;

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
     * Empty constructor.
     */
    public GridLocalMetricsAdapter() {
        // No-op.
    }

    /**
     * Constructor to initialize all possible metrics.
     *
     * @param availProcs Number of available processors.
     * @param load Average system load for the last minute.
     * @param heapInit Heap initial memory.
     * @param heapUsed Heap used memory.
     * @param heapCommitted Heap committed memory.
     * @param heapMax Heap maximum memory.
     * @param nonHeapInit Non-heap initial memory.
     * @param nonHeapUsed Non-heap used memory.
     * @param nonHeapCommitted Non-heap committed memory.
     * @param nonHeapMax Non-heap maximum memory.
     * @param upTime VM uptime.
     * @param startTime VM start time.
     * @param threadCnt Current active thread count.
     * @param peakThreadCnt Peak thread count.
     * @param startedThreadCnt Started thread count.
     * @param daemonThreadCnt Daemon thread count.
     */
    public GridLocalMetricsAdapter(
        int availProcs,
        double load,
        long heapInit,
        long heapUsed,
        long heapCommitted,
        long heapMax,
        long nonHeapInit,
        long nonHeapUsed,
        long nonHeapCommitted,
        long nonHeapMax,
        long upTime,
        long startTime,
        int threadCnt,
        int peakThreadCnt,
        long startedThreadCnt,
        int daemonThreadCnt) {
        this.availProcs = availProcs;
        this.load = load;
        this.heapInit = heapInit;
        this.heapUsed = heapUsed;
        this.heapCommitted = heapCommitted;
        this.heapMax = heapMax;
        this.nonHeapInit = nonHeapInit;
        this.nonHeapUsed = nonHeapUsed;
        this.nonHeapCommitted = nonHeapCommitted;
        this.nonHeapMax = nonHeapMax;
        this.upTime = upTime;
        this.startTime = startTime;
        this.threadCnt = threadCnt;
        this.peakThreadCnt = peakThreadCnt;
        this.startedThreadCnt = startedThreadCnt;
        this.daemonThreadCnt = daemonThreadCnt;
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
    public long getUptime() {
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
    public int getThreadCount() {
        return threadCnt;
    }

    /**
     * {@inheritDoc}
     */
    public int getPeakThreadCount() {
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
    public int getDaemonThreadCount() {
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
     * Sets CPU load average over last minute.
     *
     * @param load CPU load average over last minute.
     */
    public void setCurrentCpuLoad(double load) {
        this.load = load;
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
    public void setThreadCount(int threadCnt) {
        this.threadCnt = threadCnt;
    }

    /**
     * Sets peak thread count.
     *
     * @param peakThreadCnt Peak thread count.
     */
    public void setPeakThreadCount(int peakThreadCnt) {
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
    public void setDaemonThreadCount(int daemonThreadCnt) {
        this.daemonThreadCnt = daemonThreadCnt;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(availProcs);
        out.writeDouble(load);
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
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        availProcs = in.readInt();
        load = in.readDouble();
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridLocalMetricsAdapter.class, this);
    }
}
