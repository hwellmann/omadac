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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;

/**
 * TODO: Add class comment.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridLocalNodeMetrics implements GridNodeMetricsMBean {
    /** Grid node. */
    private final GridNode node;
    
    /**
     * @param node Node to manage.
     */
    public GridLocalNodeMetrics(GridNode node) {
        assert node != null : "ASSERTION [line=40, file=src/java/org/gridgain/grid/kernal/GridLocalNodeMetrics.java]";
        
        this.node = node;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getAvailableProcessors() {
        return node.getMetrics().getAvailableProcessors();
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageActiveJobs() {
        return node.getMetrics().getAverageActiveJobs();
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageCancelledJobs() {
        return node.getMetrics().getAverageCancelledJobs();
    }

    /**
     * {@inheritDoc}
     */
    public double getAverageJobExecuteTime() {
        return node.getMetrics().getAverageJobExecuteTime();
    }

    /**
     * {@inheritDoc}
     */
    public double getAverageJobWaitTime() {
        return node.getMetrics().getAverageJobWaitTime();
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageRejectedJobs() {
        return node.getMetrics().getAverageRejectedJobs();
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageWaitingJobs() {
        return node.getMetrics().getAverageWaitingJobs();
    }

    /**
     * {@inheritDoc}
     */
    public float getBusyTimePercentage() {
        return node.getMetrics().getBusyTimePercentage() * 100;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentActiveJobs() {
        return node.getMetrics().getCurrentActiveJobs();
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentCancelledJobs() {
        return node.getMetrics().getCurrentCancelledJobs();
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentIdleTime() {
        return node.getMetrics().getCurrentIdleTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentJobExecuteTime() {
        return node.getMetrics().getCurrentJobExecuteTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getCurrentJobWaitTime() {
        return node.getMetrics().getCurrentJobWaitTime();
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentRejectedJobs() {
        return node.getMetrics().getCurrentRejectedJobs();
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentWaitingJobs() {
        return node.getMetrics().getCurrentWaitingJobs();
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentDaemonThreadCount() {
        return node.getMetrics().getCurrentDaemonThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryCommitted() {
        return node.getMetrics().getHeapMemoryCommitted();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryInitialized() {
        return node.getMetrics().getHeapMemoryInitialized();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryMaximum() {
        return node.getMetrics().getHeapMemoryMaximum();
    }

    /**
     * {@inheritDoc}
     */
    public long getHeapMemoryUsed() {
        return node.getMetrics().getHeapMemoryUsed();
    }

    /**
     * {@inheritDoc}
     */
    public float getIdleTimePercentage() {
        return node.getMetrics().getIdleTimePercentage() * 100;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastUpdateTime() {
        return node.getMetrics().getLastUpdateTime();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumActiveJobs() {
        return node.getMetrics().getMaximumActiveJobs();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumCancelledJobs() {
        return node.getMetrics().getMaximumCancelledJobs();
    }

    /**
     * {@inheritDoc}
     */
    public long getMaximumJobExecuteTime() {
        return node.getMetrics().getMaximumJobExecuteTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getMaximumJobWaitTime() {
        return node.getMetrics().getMaximumJobWaitTime();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumRejectedJobs() {
        return node.getMetrics().getMaximumRejectedJobs();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumWaitingJobs() {
        return node.getMetrics().getMaximumWaitingJobs();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryCommitted() {
        return node.getMetrics().getNonHeapMemoryCommitted();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryInitialized() {
        return node.getMetrics().getNonHeapMemoryInitialized();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryMaximum() {
        return node.getMetrics().getNonHeapMemoryMaximum();
    }

    /**
     * {@inheritDoc}
     */
    public long getNonHeapMemoryUsed() {
        return node.getMetrics().getNonHeapMemoryUsed();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumThreadCount() {
        return node.getMetrics().getMaximumThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getStartTime() {
        return node.getMetrics().getStartTime();
    }

    /**
     * {@inheritDoc}
     */
    public double getCurrentCpuLoad() {
        return node.getMetrics().getCurrentCpuLoad() * 100;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getAverageCpuLoad() {
        return node.getMetrics().getAverageCpuLoad() * 100;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentThreadCount() {
        return node.getMetrics().getCurrentThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTotalBusyTime() {
        return node.getMetrics().getTotalBusyTime();
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalCancelledJobs() {
        return node.getMetrics().getTotalCancelledJobs();
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalExecutedJobs() {
        return node.getMetrics().getTotalExecutedJobs();
    }

    /**
     * {@inheritDoc}
     */
    public long getTotalIdleTime() {
        return node.getMetrics().getTotalIdleTime();
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalRejectedJobs() {
        return node.getMetrics().getTotalRejectedJobs();
    }

    /**
     * {@inheritDoc}
     */
    public long getTotalStartedThreadCount() {
        return node.getMetrics().getTotalStartedThreadCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getUpTime() {
        return node.getMetrics().getUpTime();
    }
}
