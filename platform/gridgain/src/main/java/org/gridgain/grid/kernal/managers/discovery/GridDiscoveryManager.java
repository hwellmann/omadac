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

package org.gridgain.grid.kernal.managers.discovery;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.gridgain.grid.*;
import static org.gridgain.grid.kernal.GridNodeAttributes.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.jobmetrics.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.metrics.*;
import org.gridgain.grid.thread.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.runnable.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridDiscoveryManager extends GridManagerAdapter<GridDiscoverySpi> {
    /** */
    private static final int MAX_DISCO_THREADS = 1;

    /** */
    private Set<GridDiscoveryListener> listeners = null;

    /**
     * Discovery executor. Note that since we use {@link LinkedBlockingQueue}, number of
     * maximum threads has no effect.
     */
    private ThreadPoolExecutor discoExecutor = null;

    /** */
    private GridRunnablePool discoPool = null;

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param cfg Grid configuration.
     * @param mgrReg Managers registry.
     * @param procReg Processors registry.
     */
    public GridDiscoveryManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridDiscoverySpi.class, cfg, procReg, mgrReg, cfg.getDiscoverySpi());
    }

    /**
     * Sets local node attributes into discovery SPI.
     *
     * @param attrs Attributes to set.
     */
    public void setNodeAttributes(Map<String, Serializable> attrs) {
        if (log.isDebugEnabled() == true) {
            log.debug("Setting local node attributes into discovery SPI.");
        }

        getSpi().setNodeAttributes(attrs);
    }

    /**
     *
     * @param rmtNode Remove node to verify configuration for.
     */
    private void verifyConfiguration(GridNode rmtNode) {
        assert rmtNode != null : "ASSERTION [line=92, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        StringBuilder buf = new StringBuilder();

        GridNode locNode = getSpi().getLocalNode();

        Serializable locVal = locNode.getAttribute(ATTR_BUILD_VER);
        Serializable rmtVal = rmtNode.getAttribute(ATTR_BUILD_VER);

        assert locVal != null : "ASSERTION [line=101, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";
        assert rmtVal != null : "ASSERTION [line=102, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        if (locVal.equals(rmtVal) == false) {
            buf.append(getMessageText("build version", locVal, rmtVal));
        }

        locVal = locNode.getAttribute(ATTR_GRID_NAME);
        rmtVal = rmtNode.getAttribute(ATTR_GRID_NAME);

        if (GridUtils.equalsWithNulls(locVal, rmtVal) == false) {
            buf.append(getMessageText("grid name", locVal, rmtVal));
        }

        if (buf.length() > 0) {
            log.info(NL + NL +
                ">>> -----------------------------------------------------------------" + NL +
                ">>> Courtesy notice that joining node has inconsistent configuration." + NL +
                ">>> Ignore this message if you are sure that this is done on purpose." + NL +
                ">>> -----------------------------------------------------------------" + NL +
                ">>> Remote Node ID: " + rmtNode.getId().toString().toUpperCase() + NL +
                buf.toString());
        }
    }

    /**
     *
     * @param msg Error message.
     * @param locVal Local node value.
     * @param rmtVal Remote node value.
     * @return Error text.
     */
    private String getMessageText(String msg, Serializable locVal, Serializable rmtVal) {
        return ">>> Remote node has different: " + msg + NL +
            ">>>     Local node:  " + locVal + NL +
            ">>>     Remote node: " + rmtVal + NL;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        listeners = new HashSet<GridDiscoveryListener>();

        getSpi().setMetricsProvider(new GridDiscoveryMetricsProvider() {
            /**
             * {@inheritDoc}
             */
            public GridNodeMetrics getMetrics() {
                GridLocalMetrics metrics =  mgrReg.getRuntimeMetricsManager().getMetrics();

                GridJobMetrics jobMetrics = procReg.getMetricsProcessor().getJobMetrics();

                GridDiscoveryMetricsAdapter nodeMetrics = new GridDiscoveryMetricsAdapter();

                nodeMetrics.setLastUpdateTime(System.currentTimeMillis());

                // Job metrics.
                nodeMetrics.setMaximumActiveJobs(jobMetrics.getMaximumActiveJobs());
                nodeMetrics.setCurrentActiveJobs(jobMetrics.getCurrentActiveJobs());
                nodeMetrics.setAverageActiveJobs(jobMetrics.getAverageActiveJobs());
                nodeMetrics.setMaximumWaitingJobs(jobMetrics.getMaximumWaitingJobs());
                nodeMetrics.setCurrentWaitingJobs(jobMetrics.getCurrentWaitingJobs());
                nodeMetrics.setAverageWaitingJobs(jobMetrics.getAverageWaitingJobs());
                nodeMetrics.setMaximumRejectedJobs(jobMetrics.getMaximumRejectedJobs());
                nodeMetrics.setCurrentRejectedJobs(jobMetrics.getCurrentRejectedJobs());
                nodeMetrics.setAverageRejectedJobs(jobMetrics.getAverageRejectedJobs());
                nodeMetrics.setMaximumCancelledJobs(jobMetrics.getMaximumCancelledJobs());
                nodeMetrics.setCurrentCancelledJobs(jobMetrics.getCurrentCancelledJobs());
                nodeMetrics.setAverageCancelledJobs(jobMetrics.getAverageCancelledJobs());
                nodeMetrics.setTotalRejectedJobs(jobMetrics.getTotalRejectedJobs());
                nodeMetrics.setTotalCancelledJobs(jobMetrics.getTotalCancelledJobs());
                nodeMetrics.setTotalExecutedJobs(jobMetrics.getTotalExecutedJobs());
                nodeMetrics.setMaximumJobWaitTime(jobMetrics.getMaximumJobWaitTime());
                nodeMetrics.setCurrentJobWaitTime(jobMetrics.getCurrentJobWaitTime());
                nodeMetrics.setAverageJobWaitTime(jobMetrics.getAverageJobWaitTime());
                nodeMetrics.setMaximumJobExecuteTime(jobMetrics.getMaximumJobExecuteTime());
                nodeMetrics.setCurrentJobExecuteTime(jobMetrics.getCurrentJobExecuteTime());
                nodeMetrics.setAverageJobExecuteTime(jobMetrics.getAverageJobExecuteTime());
                nodeMetrics.setCurrentIdleTime(jobMetrics.getCurrentIdleTime());
                nodeMetrics.setTotalIdleTime(jobMetrics.getTotalIdleTime());
                nodeMetrics.setAverageCpuLoad(jobMetrics.getAverageCpuLoad());

                // VM metrics.
                nodeMetrics.setAvailableProcessors(metrics.getAvailableProcessors());
                nodeMetrics.setCurrentCpuLoad(metrics.getCurrentCpuLoad());
                nodeMetrics.setHeapMemoryInitialized(metrics.getHeapMemoryInitialized());
                nodeMetrics.setHeapMemoryUsed(metrics.getHeapMemoryUsed());
                nodeMetrics.setHeapMemoryCommitted(metrics.getHeapMemoryCommitted());
                nodeMetrics.setHeapMemoryMaximum(metrics.getHeapMemoryMaximum());
                nodeMetrics.setNonHeapMemoryInitialized(metrics.getNonHeapMemoryInitialized());
                nodeMetrics.setNonHeapMemoryUsed(metrics.getNonHeapMemoryUsed());
                nodeMetrics.setNonHeapMemoryCommitted(metrics.getNonHeapMemoryCommitted());
                nodeMetrics.setNonHeapMemoryMaximum(metrics.getNonHeapMemoryMaximum());
                nodeMetrics.setUpTime(metrics.getUptime());
                nodeMetrics.setStartTime(metrics.getStartTime());
                nodeMetrics.setCurrentThreadCount(metrics.getThreadCount());
                nodeMetrics.setMaximumThreadCount(metrics.getPeakThreadCount());
                nodeMetrics.setTotalStartedThreadCount(metrics.getTotalStartedThreadCount());
                nodeMetrics.setCurrentDaemonThreadCount(metrics.getDaemonThreadCount());

                return nodeMetrics;
            }
        });

        discoExecutor = new GridThreadPoolExecutorService(cfg.getGridName(), MAX_DISCO_THREADS, MAX_DISCO_THREADS, 0,
            new LinkedBlockingQueue<Runnable>());

        discoExecutor.prestartAllCoreThreads();

        discoPool = new GridRunnablePool(discoExecutor, log);

        startSpi();

        getSpi().setListener(new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            @SuppressWarnings({"CatchGenericClass"})
            public void onDiscovery(final GridDiscoveryEventType type, final GridNode node) {
                try {
                    discoPool.execute(new GridRunnable(cfg.getGridName(), "disco-mgr-worker", log) {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        protected void body() throws InterruptedException {
                            switch (type) {
                                case JOINED: {
                                    if (log.isInfoEnabled() == true) {
                                        log.info("Added new node to topology: " + node);
                                    }

                                    // Verify configuration consistency.
                                    verifyConfiguration(node);

                                    break;
                                }

                                case LEFT: {
                                    if (log.isInfoEnabled() == true) {
                                        log.info("Grid node left topology: " + node);
                                    }

                                    break;
                                }

                                case FAILED: {
                                    log.warning("Removed failed node from topology: " + node);

                                    break;
                                }

                                case METRICS_UPDATED: {
                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Nodes metrics updated: " + node);
                                    }

                                    break;
                                }

                                default: { assert false : "Invalid discovery event: " + type; }
                            }

                            if (type != GridDiscoveryEventType.METRICS_UPDATED) {
                                logGridTopology();

                                mgrReg.getEventStorageManager().record(type, node);
                            }

                            Set<GridDiscoveryListener> tmp = null;

                            synchronized (mux) {
                                tmp = listeners;
                            }

                            if (tmp != null) {
                                // Note, that since listeners are stored in unmodifiable collection, we
                                // don't have to hold synchronization lock during event notifications.
                                for (final GridDiscoveryListener listener : tmp) {
                                    listener.onDiscovery(type, node);
                                }
                            }
                        }
                    });
                }
                // This should never happen, as the queue is unbounded here.
                catch (GridException e) {
                    log.error("Failed to execute discovery notification (node should be rebooted).", e);
                }
            }
        });

        logGridTopology();

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     * Logs grid size for license compliance.
     */
    private void logGridTopology() {
        /*
         * For compliance with support contract this log information
         * should be always printed (regardless of the log level).
         * If you change this code - make sure to have other means
         * to log the current size of the grid.
         *
         * NOTE:
         * GridGain Systems reserves the right to review the modified
         * code in order to confirm the compliance with support contract.
         */
        Collection<GridNode> rmtNodes = getSpi().getRemoteNodes();

        GridNode locNode = getSpi().getLocalNode();

        Collection<GridNode> allNodes = new ArrayList<GridNode>(rmtNodes.size() + 1);

        // Create total topology.
        allNodes.addAll(rmtNodes);
        allNodes.add(locNode);

        long topVer = getTopologyHash(allNodes);

        StringBuilder buf = new StringBuilder();

        buf.append(NL).append(NL);
        buf.append(">>> -------------------").append(NL);
        buf.append(">>> Discovery Snapshot.").append(NL);
        buf.append(">>> -------------------").append(NL);
        buf.append(">>> Number of nodes: ").append(rmtNodes.size() + 1).append(NL);
        buf.append(">>> Topology hash: 0x").append(Long.toHexString(topVer).toUpperCase()).append(NL);

        String locAddr = locNode.getPhysicalAddress();

        buf.append(">>> Local: ").
            append(locNode.getId().toString().toUpperCase()).
            append(", ").
            append(locNode.getPhysicalAddress()).
            append(", ").
            append(locNode.getAttribute("os.name")).
            append(' ').
            append(locNode.getAttribute("os.arch")).
            append(' ').
            append(locNode.getAttribute("os.version")).
            append(", ").
            append(System.getProperty("user.name")).
            append(", ").
            append(locNode.getAttribute("java.runtime.name")).
            append(' ').
            append(locNode.getAttribute("java.runtime.version")).append(NL);

        int totalCpus = locNode.getMetrics().getAvailableProcessors();

        Set<String> rmtAddrs = new HashSet<String>(rmtNodes.size());

        for (GridNode node : rmtNodes) {
            buf.append(">>> Remote: ").
                append(node.getId().toString().toUpperCase()).
                append(", ").
                append(node.getPhysicalAddress()).
                append(", ").
                append(node.getAttribute("os.name")).
                append(' ').
                append(node.getAttribute("os.arch")).
                append(' ').
                append(node.getAttribute("os.version")).
                append(", ").
                append(node.getAttribute(ATTR_USER_NAME)).
                append(", ").
                append(node.getAttribute("java.runtime.name")).
                append(' ').
                append(node.getAttribute("java.runtime.version")).append(NL);

            String nodeAddr = node.getPhysicalAddress();

            int cpus = node.getMetrics().getAvailableProcessors();

            // Don't count CPUs for nodes on the same host.
            if (locAddr.equals(nodeAddr) == false && rmtAddrs.contains(nodeAddr) == false) {
                totalCpus += cpus;

                if (nodeAddr != null) {
                    rmtAddrs.add(nodeAddr);
                }
            }
        }

        buf.append(">>> Total number of CPUs: ").append(totalCpus).append(NL);

        if (log.isInfoEnabled() == true) {
            log.info(buf.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws GridException {
        // Stop receiving notifications.
        getSpi().setListener(null);

        // Stop/wait for all running workers (listeners) that could call SPI.
        if (discoPool != null) {
            discoPool.join(true);
        }

        // Shutdown executor to be sure that no outstanding workers work with SPI
        GridUtils.shutdownNow(getClass(), discoExecutor, log);

        listeners = null;

        // Stop SPI itself.
        stopSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     *
     * @param listener Listener to add.
     */
    public void addDiscoveryListener(GridDiscoveryListener listener) {
        assert listener != null : "ASSERTION [line=428, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        synchronized (mux) {
            Set<GridDiscoveryListener> tmp = new HashSet<GridDiscoveryListener>(listeners.size() + 1);

            tmp.addAll(listeners);

            tmp.add(listener);

            // Seal it.
            listeners = Collections.unmodifiableSet(tmp);
        }
    }

    /**
     *
     * @param listener Listener to remove.
     * @return FIXDOC
     */
    public boolean removeDiscoveryListener(GridDiscoveryListener listener) {
        assert listener != null : "ASSERTION [line=448, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        synchronized (mux) {
            if (listeners != null && listeners.contains(listener) == true) {
                Set<GridDiscoveryListener> tmp = new HashSet<GridDiscoveryListener>(listeners);

                tmp.remove(listener);

                // Seal it.
                listeners = Collections.unmodifiableSet(tmp);

                return true;
            }

            return false;
        }
    }

    /**
     *
     * @param nodeId ID of the node.
     * @return FIXDOC
     */
    public boolean pingNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=472, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        return getSpi().pingNode(nodeId);
    }

    /**
     *
     * @param nodeId ID of the node.
     * @return FIXDOC
     */
    public GridNode getNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=483, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        return getSpi().getNode(nodeId);
    }

    /**
     *
     * @param nodes Subset of grid nodes for hashing.
     * @return Hash for given topology.
     * @see Grid#getTopologyHash(Collection)
     */
    public long getTopologyHash(Collection<GridNode> nodes) {
        assert nodes != null : "ASSERTION [line=495, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        if (nodes.isEmpty() == true) {
            return 0; // Special case.
        }

        List<String> uuids = new ArrayList<String>(nodes.size());

        for (GridNode node : nodes) {
            uuids.add(node.getId().toString());
        }

        Collections.sort(uuids);

        CRC32 hash = new CRC32();

        for (String uuid : uuids) {
            hash.update(uuid.getBytes());
        }

        return hash.getValue();
    }

    /**
     * @return FIXDOC
     * @see Grid#getRemoteNodes()
     */
    public Collection<GridNode> getRemoteNodes() {
        return getSpi().getRemoteNodes();
    }

    /**
     *
     * @param filter Filter for nodes.
     * @return Nodes accepted by the filter.
     */
    public Collection<GridNode> getNodes(GridNodeFilter filter) {
        assert filter != null : "ASSERTION [line=532, file=src/java/org/gridgain/grid/kernal/managers/discovery/GridDiscoveryManager.java]";

        Collection<GridNode> rmtNodes = getSpi().getRemoteNodes();

        GridNode locNode = getSpi().getLocalNode();

        Collection<GridNode> nodes = new HashSet<GridNode>(rmtNodes.size() + 1);

        if (filter.accept(locNode) == true) {
            nodes.add(locNode);
        }

        for (GridNode node : rmtNodes) {
            if (filter.accept(node) == true) {
                nodes.add(node);
            }
        }

        return nodes;
    }

    /**
     *
     * @return FIXDOC
     */
    public Collection<GridNode> getAllNodes() {
        Collection<GridNode> rmtNodes = getSpi().getRemoteNodes();

        Set<GridNode> allNodes = new HashSet<GridNode>(rmtNodes.size() + 1);

        allNodes.addAll(rmtNodes);

        allNodes.add(getSpi().getLocalNode());

        return allNodes;
    }

    /**
     *
     * @return FIXDOC
     */
    public GridNode getLocalNode() {
        return getSpi().getLocalNode();
    }
}
