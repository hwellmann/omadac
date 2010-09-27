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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;

/**
 * Processes job metrics.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobMetricsProcessor extends GridProcessorAdapter {
    /** Time to live. */
    private long expireTime = 0;

    /** Maximum size. */
    private int histSize = 0;

    /** */
    private SortedSet<GridJobMetricsSnapshot> activeJobMaxSet = new TreeSet<GridJobMetricsSnapshot>(
        new Comparator<GridJobMetricsSnapshot>() {
            /**
             * {@inheritDoc}
             */
            public int compare(GridJobMetricsSnapshot o1, GridJobMetricsSnapshot o2) {
                return o1.getActiveJobs() > o2.getActiveJobs() ? -1 :
                    o1.getActiveJobs() == o2.getActiveJobs() ? 0 : 1;
            }
    });

    /** */
    private SortedSet<GridJobMetricsSnapshot> waitingJobMaxSet = new TreeSet<GridJobMetricsSnapshot>(
        new Comparator<GridJobMetricsSnapshot>() {
            /**
             * {@inheritDoc}
             */
            public int compare(GridJobMetricsSnapshot o1, GridJobMetricsSnapshot o2) {
                return o1.getPassiveJobs() > o2.getPassiveJobs() ? -1 :
                    o1.getPassiveJobs() == o2.getPassiveJobs() ? 0 : 1;
            }
    });

    /** */
    private SortedSet<GridJobMetricsSnapshot> cancelledJobMaxSet = new TreeSet<GridJobMetricsSnapshot>(
        new Comparator<GridJobMetricsSnapshot>() {
            /**
             * {@inheritDoc}
             */
            public int compare(GridJobMetricsSnapshot o1, GridJobMetricsSnapshot o2) {
                return o1.getCancelJobs() > o2.getCancelJobs() ? -1 :
                    o1.getCancelJobs() == o2.getCancelJobs() ? 0 : 1;
            }
    });

    /** */
    private SortedSet<GridJobMetricsSnapshot> rejectedJobMaxSet = new TreeSet<GridJobMetricsSnapshot>(
        new Comparator<GridJobMetricsSnapshot>() {
            /**
             * {@inheritDoc}
             */
            public int compare(GridJobMetricsSnapshot o1, GridJobMetricsSnapshot o2) {
                return o1.getRejectJobs() > o2.getRejectJobs() ? -1 :
                    o1.getRejectJobs() == o2.getRejectJobs() ? 0 : 1;
            }
    });

    /** */
    private SortedSet<GridJobMetricsSnapshot> execTimeMaxSet = new TreeSet<GridJobMetricsSnapshot>(
        new Comparator<GridJobMetricsSnapshot>() {
            /**
             * {@inheritDoc}
             */
            public int compare(GridJobMetricsSnapshot o1, GridJobMetricsSnapshot o2) {
                return o1.getMaximumExecutionTime() > o2.getMaximumExecutionTime() ? -1 :
                    o1.getMaximumExecutionTime() == o2.getMaximumExecutionTime() ? 0 : 1;
            }
    });

    /** */
    private SortedSet<GridJobMetricsSnapshot> waitTimeMaxSet = new TreeSet<GridJobMetricsSnapshot>(
        new Comparator<GridJobMetricsSnapshot>() {
            /**
             * {@inheritDoc}
             */
            public int compare(GridJobMetricsSnapshot o1, GridJobMetricsSnapshot o2) {
                return o1.getMaximumWaitTime() > o2.getMaximumWaitTime() ? -1 :
                    o1.getMaximumWaitTime() == o2.getMaximumWaitTime() ? 0 : 1;
            }
    });

    /** */
    private Queue<GridJobMetricsSnapshot> queue = new ConcurrentLinkedQueue<GridJobMetricsSnapshot>();

    /** */
    private MetricCounters cntrs = new MetricCounters();

    /**
     *
     * @param mgrReg Manager registry.
     * @param procReg Processor registry.
     * @param cfg Grid configuration.
     */
    public GridJobMetricsProcessor(GridManagerRegistry mgrReg, GridProcessorRegistry procReg, GridConfiguration cfg) {
        super(mgrReg, procReg, cfg);

        expireTime = cfg.getMetricsExpireTime();
        histSize = cfg.getMetricsHistorySize();
    }

    /**
     * Gets metrics history size.
     *
     * @return Maximum metrics queue size.
     */
    int getHistorySize() {
        return histSize;
    }

    /**
     * Gets snapshot queue.
     *
     * @return Metrics snapshot queue.
     */
    Queue<GridJobMetricsSnapshot> getQueue() {
        return queue;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        if (histSize == 0) {
            histSize = GridConfiguration.DFLT_METRICS_HISTORY_SIZE;
        }

        if (expireTime == 0) {
            expireTime = GridConfiguration.DFLT_METRICS_EXPIRE_TIME;
        }

        assertParameter(histSize > 0, "metricsHistorySize > 0");
        assertParameter(expireTime > 0, "metricsExpireTime > 0");

        if (log.isDebugEnabled() == true) {
            log.debug("Job metrics processor started [histSize=" + histSize + ", expireTime=" + expireTime + ']');
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(boolean cancel) throws GridException {
        if (log.isDebugEnabled() == true) {
            log.debug("Job metrics processor stopped.");
        }
    }

    /**
     * Gets latest metrics.
     *
     * @return Latest metrics.
     */
    public GridJobMetrics getJobMetrics() {
        return cntrs.getJobMetrics();
    }

    /**
     *
     * @param metrics New metrics.
     */
    public void addSnapshot(GridJobMetricsSnapshot metrics) {
        long now = System.currentTimeMillis();

        queue.add(metrics);

        // Update counters and obtain number of snapshots in the history
        // (i.e. current queue size). Note, that we cannot do 'queue.size()'
        // here as it has complexity of O(N) for ConcurrentLinkedQueue.
        int curHistorySize = cntrs.onAdd(metrics, now);

        for (Iterator<GridJobMetricsSnapshot> iter = queue.iterator(); iter.hasNext() == true;) {
            GridJobMetricsSnapshot m = iter.next();

            if (curHistorySize > histSize || now - m.getTimestamp() >= expireTime) {
                if (m.delete() == true) {
                    // Update counters.
                    curHistorySize = cntrs.onRemove(m);

                    iter.remove();
                }
            }
            else {
                break;
            }
        }
    }

    /**
     *
     * @param set Set to add to.
     * @param metrics Metrics to add.
     * @return First metric in the set.
     */
    private GridJobMetricsSnapshot addToSet(SortedSet<GridJobMetricsSnapshot> set, GridJobMetricsSnapshot metrics) {
        synchronized (set) {
            set.add(metrics);

            return set.first();
        }
    }

    /**
     *
     * @param set Set.
     * @param metrics Metrics to remove.
     * @return First metric in the set.
     */
    private GridJobMetricsSnapshot removeFromSet(SortedSet<GridJobMetricsSnapshot> set, GridJobMetricsSnapshot metrics) {
        synchronized (set) {
            set.remove(metrics);

            return set.isEmpty() == true ? null : set.first();
        }
    }

    /**
     * All metrics counters.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class MetricCounters {
        /** */
        private int totalActiveJobs = 0;

        /** */
        private int totalWaitingJobs = 0;

        /** */
        private int totalStartedJobs = 0;

        /** */
        private int totalCancelledJobs = 0;

        /** */
        private int totalRejectedJobs = 0;

        /** */
        private int totalFinishedJobs = 0;

        /** */
        private long totalExecTime = 0;

        /** */
        private long totalWaitTime = 0;

        /** */
        private double totalCpuLoad = 0;

        /** */
        private long totalIdleTime = 0;

        /** */
        private long curIdleTime = 0;

        /** */
        private boolean isIdle = true;

        /** */
        private int curHistSize = 0;

        /** */
        private AtomicReference<GridJobMetricsSnapshot> activeMax = new AtomicReference<GridJobMetricsSnapshot>(null);

        /** */
        private AtomicReference<GridJobMetricsSnapshot> waitingMax = new AtomicReference<GridJobMetricsSnapshot>(null);

        /** */
        private AtomicReference<GridJobMetricsSnapshot> cancelledMax = new AtomicReference<GridJobMetricsSnapshot>(null);

        /** */
        private AtomicReference<GridJobMetricsSnapshot> rejectedMax = new AtomicReference<GridJobMetricsSnapshot>(null);

        /** */
        private AtomicReference<GridJobMetricsSnapshot> waitTimeMax = new AtomicReference<GridJobMetricsSnapshot>(null);

        /** */
        private AtomicReference<GridJobMetricsSnapshot> execTimeMax = new AtomicReference<GridJobMetricsSnapshot>(null);

        /** */
        private long idleTimer = System.currentTimeMillis();

        /** */
        private GridJobMetricsSnapshot lastSnapshot = null;

        /** Mutex. */
        private final Object mux = new Object();

        /**
         *
         * @return Latest job metrics.
         */
        GridJobMetrics getJobMetrics() {
            GridJobMetrics metrics = new GridJobMetrics();

            GridJobMetricsSnapshot activeMax = this.activeMax.get();
            GridJobMetricsSnapshot waitingMax = this.waitingMax.get();
            GridJobMetricsSnapshot cancelledMax = this.cancelledMax.get();
            GridJobMetricsSnapshot rejectedMax = this.rejectedMax.get();
            GridJobMetricsSnapshot waitTimeMax = this.waitTimeMax.get();
            GridJobMetricsSnapshot execTimeMax = this.execTimeMax.get();

            // Maximums.
            metrics.setMaximumActiveJobs(activeMax == null ? 0 : activeMax.getActiveJobs());
            metrics.setMaximumWaitingJobs(waitingMax == null ? 0 : waitingMax.getPassiveJobs());
            metrics.setMaximumCancelledJobs(cancelledMax == null ? 0 : cancelledMax.getCancelJobs());
            metrics.setMaximumRejectedJobs(rejectedMax == null ? 0 : rejectedMax.getRejectJobs());
            metrics.setMaximumJobWaitTime(waitTimeMax == null ? 0 : waitTimeMax.getMaximumWaitTime());
            metrics.setMaxJobExecutionTime(execTimeMax == null ? 0 : execTimeMax.getMaximumExecutionTime());

            synchronized (mux) {
                // Current metrics.
                metrics.setCurrentIdleTime(curIdleTime);

                if (lastSnapshot != null) {
                    metrics.setCurrentActiveJobs(lastSnapshot.getActiveJobs());
                    metrics.setCurrentWaitingJobs(lastSnapshot.getPassiveJobs());
                    metrics.setCurrentCancelledJobs(lastSnapshot.getCancelJobs());
                    metrics.setCurrentRejectedJobs(lastSnapshot.getRejectJobs());
                    metrics.setCurrentJobExecutionTime(lastSnapshot.getMaximumExecutionTime());
                    metrics.setCurrentJobWaitTime(lastSnapshot.getMaximumWaitTime());
                }

                // Averages.
                if (curHistSize > 0) {
                    metrics.setAverageActiveJobs((float)totalActiveJobs / curHistSize);
                    metrics.setAverageWaitingJobs((float)totalWaitingJobs / curHistSize);
                    metrics.setAverageCancelledJobs((float)totalCancelledJobs / curHistSize);
                    metrics.setAverageRejectedJobs((float)totalRejectedJobs / curHistSize);
                    metrics.setAverageCpuLoad(totalCpuLoad / curHistSize);
                }

                metrics.setAverageJobExecutionTime(totalFinishedJobs > 0 ? (double)totalExecTime / totalFinishedJobs : 0);
                metrics.setAverageJobWaitTime(totalStartedJobs > 0 ? (double)totalWaitTime / totalStartedJobs : 0);

                // Totals.
                metrics.setTotalExecutedJobs(totalFinishedJobs);
                metrics.setTotalCancelledJobs(totalCancelledJobs);
                metrics.setTotalRejectedJobs(totalRejectedJobs);

                metrics.setTotalIdleTime(totalIdleTime);
            }

            return metrics;
        }

        /**
         *
         * @param metrics New metrics.
         * @param now Current timestamp.
         * @return Current history size (the size of queue).
         */
        int onAdd(GridJobMetricsSnapshot metrics, long now) {
            // Maximums.
            activeMax.set(addToSet(activeJobMaxSet, metrics));
            waitingMax.set(addToSet(waitingJobMaxSet, metrics));
            cancelledMax.set(addToSet(cancelledJobMaxSet, metrics));
            rejectedMax.set(addToSet(rejectedJobMaxSet, metrics));
            waitTimeMax.set(addToSet(waitTimeMaxSet, metrics));
            execTimeMax.set(addToSet(execTimeMaxSet, metrics));

            synchronized (mux) {
                assert curHistSize >= 0 : "ASSERTION [line=397, file=src/java/org/gridgain/grid/kernal/processors/jobmetrics/GridJobMetricsProcessor.java]";

                curHistSize++;

                // Totals.
                totalActiveJobs += metrics.getActiveJobs();
                totalCancelledJobs += metrics.getCancelJobs();
                totalWaitingJobs += metrics.getPassiveJobs();
                totalRejectedJobs += metrics.getRejectJobs();
                totalWaitTime += metrics.getWaitTime();
                totalExecTime += metrics.getExecutionTime();
                totalStartedJobs += metrics.getStartedJobs();
                totalFinishedJobs += metrics.getFinishedJobs();
                totalCpuLoad += metrics.getCpuLoad();

                // Handle current and total idle times.
                if (metrics.getActiveJobs() > 0) {
                    if (isIdle == true) {
                        totalIdleTime += now - idleTimer;

                        curIdleTime = 0;

                        isIdle = false;
                    }
                }
                else {
                    if (isIdle == false) {
                        isIdle = true;
                    }
                    else {
                        curIdleTime += now - idleTimer;

                        totalIdleTime += now - idleTimer;
                    }

                    // Reset timer.
                    idleTimer = now;
                }

                lastSnapshot = metrics;

                return curHistSize;
            }
        }

        /**
         *
         * @param metrics Expired metrics.
         * @return Current history size (the size of queue).
         */
        int onRemove(GridJobMetricsSnapshot metrics) {
            // Maximums.
            activeMax.set(removeFromSet(activeJobMaxSet, metrics));
            waitingMax.set(removeFromSet(waitingJobMaxSet, metrics));
            cancelledMax.set(removeFromSet(cancelledJobMaxSet, metrics));
            rejectedMax.set(removeFromSet(rejectedJobMaxSet, metrics));
            waitTimeMax.set(removeFromSet(waitTimeMaxSet, metrics));
            execTimeMax.set(removeFromSet(execTimeMaxSet, metrics));

            synchronized (mux) {
                assert curHistSize > 0 : "ASSERTION [line=457, file=src/java/org/gridgain/grid/kernal/processors/jobmetrics/GridJobMetricsProcessor.java]";

                curHistSize--;

                // Totals.
                totalActiveJobs -= metrics.getActiveJobs();
                totalCancelledJobs -= metrics.getCancelJobs();
                totalWaitingJobs -= metrics.getPassiveJobs();
                totalRejectedJobs -= metrics.getRejectJobs();
                totalWaitTime -= metrics.getWaitTime();
                totalExecTime -= metrics.getExecutionTime();
                totalStartedJobs -= metrics.getStartedJobs();
                totalFinishedJobs -= metrics.getFinishedJobs();
                totalCpuLoad -= metrics.getCpuLoad();

                if (curHistSize == 0) {
                    lastSnapshot = null;
                }

                return curHistSize;
            }
        }
    }
}
