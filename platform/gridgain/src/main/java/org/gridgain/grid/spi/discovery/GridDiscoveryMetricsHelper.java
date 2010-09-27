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

import org.gridgain.grid.*;
import org.gridgain.grid.util.*;

/**
 * Helper class to serialize and deserialize node metrics..
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public final class GridDiscoveryMetricsHelper {
    /** Size of serialized node metrics. */
    public static final int METRICS_SIZE =
        4/*max active jobs*/ +
        4/*current active jobs*/ +
        4/*average active jobs*/ +
        4/*max waiting jobs*/ +
        4/*current waiting jobs*/ +
        4/*average waiting jobs*/ +
        4/*max cancelled jobs*/ +
        4/*current cancelled jobs*/ +
        4/*average cancelled jobs*/ +
        4/*max rejected jobs*/ +
        4/*current rejected jobs*/ +
        4/*average rejected jobs*/ +
        4/*total executed jobs*/ +
        4/*total rejected jobs*/ +
        4/*total cancelled jobs*/ +
        8/*max job wait time*/ +
        8/*current job wait time*/ +
        8/*average job wait time*/ +
        8/*max job execute time*/ +
        8/*current job execute time*/ +
        8/*average job execute time*/ +
        8/*current idle time*/ +
        8/*total idle time*/ +
        4/*available processors*/ +
        8/*current CPU load*/ +
        8/*average CPU load*/ +
        8/*heap memory init*/ +
        8/*heap memory used*/ +
        8/*heap memory committed*/ +
        8/*heap memory max*/ +
        8/*non-heap memory init*/ +
        8/*non-heap memory used*/ +
        8/*non-heap memory committed*/ +
        8/*non-heap memory max*/ +
        8/*uptime*/ +
        8/*start time*/ +
        4/*thread count*/ +
        4/*peak thread count*/ +
        8/*total started thread count*/ +
        4/*daemon thread count*/;

    /**
     * Enforces singleton.
     */
    private GridDiscoveryMetricsHelper() {
        // No-op.
    }

    /**
     * Serializes node metrics into byte array.
     *
     * @param data Byte array.
     * @param off Offset into byte array.
     * @param metrics Node metrics to serialize.
     * @return New offset.
     */
    public static int serialize(byte[] data, int off, GridNodeMetrics metrics) {
        int start = off;

        off = GridUtils.intToBytes(metrics.getMaximumActiveJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getCurrentActiveJobs(), data, off);
        off = GridUtils.floatToBytes(metrics.getAverageActiveJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getMaximumWaitingJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getCurrentWaitingJobs(), data, off);
        off = GridUtils.floatToBytes(metrics.getAverageWaitingJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getMaximumRejectedJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getCurrentRejectedJobs(), data, off);
        off = GridUtils.floatToBytes(metrics.getAverageRejectedJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getMaximumCancelledJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getCurrentCancelledJobs(), data, off);
        off = GridUtils.floatToBytes(metrics.getAverageCancelledJobs(), data, off);
        off = GridUtils.intToBytes(metrics.getTotalRejectedJobs(), data , off);
        off = GridUtils.intToBytes(metrics.getTotalCancelledJobs(), data , off);
        off = GridUtils.intToBytes(metrics.getTotalExecutedJobs(), data , off);
        off = GridUtils.longToBytes(metrics.getMaximumJobWaitTime(), data, off);
        off = GridUtils.longToBytes(metrics.getCurrentJobWaitTime(), data, off);
        off = GridUtils.doubleToBytes(metrics.getAverageJobWaitTime(), data, off);
        off = GridUtils.longToBytes(metrics.getMaximumJobExecuteTime(), data, off);
        off = GridUtils.longToBytes(metrics.getCurrentJobExecuteTime(), data, off);
        off = GridUtils.doubleToBytes(metrics.getAverageJobExecuteTime(), data, off);
        off = GridUtils.longToBytes(metrics.getCurrentIdleTime(), data, off);
        off = GridUtils.longToBytes(metrics.getTotalIdleTime(), data , off);
        off = GridUtils.intToBytes(metrics.getAvailableProcessors(), data, off);
        off = GridUtils.doubleToBytes(metrics.getCurrentCpuLoad(), data, off);
        off = GridUtils.doubleToBytes(metrics.getAverageCpuLoad(), data, off);
        off = GridUtils.longToBytes(metrics.getHeapMemoryInitialized(), data, off);
        off = GridUtils.longToBytes(metrics.getHeapMemoryUsed(), data, off);
        off = GridUtils.longToBytes(metrics.getHeapMemoryCommitted(), data, off);
        off = GridUtils.longToBytes(metrics.getHeapMemoryMaximum(), data, off);
        off = GridUtils.longToBytes(metrics.getNonHeapMemoryInitialized(), data, off);
        off = GridUtils.longToBytes(metrics.getNonHeapMemoryUsed(), data, off);
        off = GridUtils.longToBytes(metrics.getNonHeapMemoryCommitted(), data, off);
        off = GridUtils.longToBytes(metrics.getNonHeapMemoryMaximum(), data, off);
        off = GridUtils.longToBytes(metrics.getStartTime(), data, off);
        off = GridUtils.longToBytes(metrics.getUpTime(), data, off);
        off = GridUtils.intToBytes(metrics.getCurrentThreadCount(), data, off);
        off = GridUtils.intToBytes(metrics.getMaximumThreadCount(), data, off);
        off = GridUtils.longToBytes(metrics.getTotalStartedThreadCount(), data, off);
        off = GridUtils.intToBytes(metrics.getCurrentDaemonThreadCount(), data, off);

        assert off - start == METRICS_SIZE : "Invalid metrics size [expected=" + METRICS_SIZE + ", actual=" +
            (off - start) + ']';

        return off;
    }

    /**
     * De-serializes node metrics.
     *
     * @param data Byte array.
     * @param off Offset into byte array.
     * @return Deserialized node metrics.
     */
    public static GridNodeMetrics deserialize(byte[] data, int off) {
        int start = off;

        GridDiscoveryMetricsAdapter metrics = new GridDiscoveryMetricsAdapter();

        metrics.setLastUpdateTime(System.currentTimeMillis());

        metrics.setMaximumActiveJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setCurrentActiveJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setAverageActiveJobs(GridUtils.bytesToFloat(data, off));

        off += 4;

        metrics.setMaximumWaitingJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setCurrentWaitingJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setAverageWaitingJobs(GridUtils.bytesToFloat(data, off));

        off += 4;

        metrics.setMaximumRejectedJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setCurrentRejectedJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setAverageRejectedJobs(GridUtils.bytesToFloat(data, off));

        off += 4;

        metrics.setMaximumCancelledJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setCurrentCancelledJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setAverageCancelledJobs(GridUtils.bytesToFloat(data, off));

        off += 4;

        metrics.setTotalRejectedJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setTotalCancelledJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setTotalExecutedJobs(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setMaximumJobWaitTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setCurrentJobWaitTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setAverageJobWaitTime(GridUtils.bytesToDouble(data, off));

        off += 8;

        metrics.setMaximumJobExecuteTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setCurrentJobExecuteTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setAverageJobExecuteTime(GridUtils.bytesToDouble(data, off));

        off += 8;

        metrics.setCurrentIdleTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setTotalIdleTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setAvailableProcessors(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setCurrentCpuLoad(GridUtils.bytesToDouble(data, off));

        off += 8;
        
        metrics.setAverageCpuLoad(GridUtils.bytesToDouble(data, off));
        
        off += 8;

        metrics.setHeapMemoryInitialized(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setHeapMemoryUsed(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setHeapMemoryCommitted(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setHeapMemoryMaximum(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setNonHeapMemoryInitialized(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setNonHeapMemoryUsed(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setNonHeapMemoryCommitted(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setNonHeapMemoryMaximum(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setStartTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setUpTime(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setCurrentThreadCount(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setMaximumThreadCount(GridUtils.bytesToInt(data, off));

        off += 4;

        metrics.setTotalStartedThreadCount(GridUtils.bytesToLong(data, off));

        off += 8;

        metrics.setCurrentDaemonThreadCount(GridUtils.bytesToInt(data, off));

        off += 4;

        assert off - start == METRICS_SIZE : "Invalid metrics size [expected=" + METRICS_SIZE + ", actual=" +
            (off - start) + ']';

        return metrics;
    }
}
