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

package org.gridgain.grid.spi.failover.always;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Failover SPI that always reroutes a failed job to another node.
 * Note, that at first an attempt will be made to reroute the failed job
 * to a node that was not part of initial split for a better chance of
 * success. If no such nodes are available, then an attempt will be made to
 * reroute the failed job to the nodes in the initial split minus the node
 * the job is failed on. If none of the above attempts succeeded, then the
 * job will not be failed over and <tt>null</tt> will be returned.
 * <p>
 * <h1 class="header">Configuration</h1>
 * This SPI is default failover SPI and does not have to be explicitly
 * configured unless configuration parameters need to be changed.
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 *      <li>
 *          Maximum failover attempts for a single job (see {@link #setMaximumFailoverAttempts(int)}).
 *          If maximum failover attempts is reached, then job will not be failed-over and,
 *          hence, will fail.
 *      </li>
 * </ul>
 * Here is a Java example how to configure grid with <tt>GridAlwaysFailoverSpi</tt> failover SPI.
 * <pre name="code" class="java">
 * GridAlwaysFailoverSpi spi = new GridAlwaysFailoverSpi();
 *
 * // Override maximum failover attempts.
 * spi.setMaximumFailoverAttempts(5);
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default failover SPI.
 * cfg.setFailoverSpiSpi(spi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * Here is an example of how to configure <tt>GridAlwaysFailoverSpi</tt> from Spring XML configuration file.
 * <pre name="code" class="xml">
 * &lt;property name="failoverSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.failover.always.GridAlwaysFailoverSpi"&gt;
 *         &lt;property name="maximumFailoverAttempts" value="5"/&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridFailoverSpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridAlwaysFailoverSpi extends GridSpiAdapter implements GridFailoverSpi, GridAlwaysFailoverSpiMBean {
    /** Maximum number of attempts to execute a failed job on another node (default is <tt>5</tt>). */
    public static final int DFLT_MAX_FAILOVER_ATTEMPTS = 5;

    /**
     * Name of job context attribute containing all nodes a job failed on.
     *
     * @see GridJobContext
     */
    public static final String FAILED_NODE_LIST_ATTR = "gridgain:failover:failednodelist";

    /** Maximum attempts attribute key should be the same on all nodes. */
    public static final String MAX_FAILOVER_ATTEMPT_ATTR = "gridgain:failover:maxattempts";

    /** Injected grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** Maximum number of attempts to execute a failed job on another node. */
    private int maxFailoverAttempts = DFLT_MAX_FAILOVER_ATTEMPTS;

    /** Number of jobs that were failed over. */
    private int totalFailoverJobs = 0;

    /**
     * {@inheritDoc}
     */
    public int getMaximumFailoverAttempts() {
        return maxFailoverAttempts;
    }

    /**
     * Sets maximum number of attempts to execute a failed job on another node.
     * If not specified, {@link #DFLT_MAX_FAILOVER_ATTEMPTS} value will be used.
     *
     * @param maxFailoverAttempts Maximum number of attempts to execute a failed job on another node.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaximumFailoverAttempts(int maxFailoverAttempts) {
        this.maxFailoverAttempts = maxFailoverAttempts;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalFailoverJobsCount() {
        return totalFailoverJobs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(MAX_FAILOVER_ATTEMPT_ATTR), maxFailoverAttempts);
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(maxFailoverAttempts >= 0, "maxFailoverAttempts >= 0");

        if (log.isInfoEnabled()) {
            log.info(configInfo("maximumFailoverAttempts", maxFailoverAttempts));
        }

        registerMBean(gridName, this, GridAlwaysFailoverSpiMBean.class);

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public GridNode failover(GridFailoverContext ctx, List<GridNode> top) {
        assert ctx != null : "ASSERTION [line=188, file=src/java/org/gridgain/grid/spi/failover/always/GridAlwaysFailoverSpi.java]";
        assert top != null : "ASSERTION [line=189, file=src/java/org/gridgain/grid/spi/failover/always/GridAlwaysFailoverSpi.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Received failed job result: " + ctx.getJobResult());
        }

        if (top.isEmpty() == true) {
            log.warning("Received empty topology for failover and is forced to fail (check topology SPI?)");

            // Nowhere to failover to.
            return null;
        }

        Set<UUID> failedNodes = (Set<UUID>)ctx.getJobResult().getJobContext().getAttribute(FAILED_NODE_LIST_ATTR);

        if (failedNodes == null) {
            failedNodes = new HashSet<UUID>(1);
        }

        Integer failoverCnt = failedNodes.size();

        if (failoverCnt >= maxFailoverAttempts) {
            log.warning("Job failover failed because number of maximum failover attempts is exceeded [failedJob=" +
                ctx.getJobResult().getJob() + ", maxFailoverAttempts=" + maxFailoverAttempts + ']');

            return null;
        }

        failedNodes.add(ctx.getJobResult().getNode().getId());

        // Copy.
        List<GridNode> newTop = new ArrayList<GridNode>(top.size());

        for (GridNode node : top) {
            if (failedNodes.contains(node.getId()) == false) {
                newTop.add(node);
            }
        }

        if (newTop.isEmpty() == true) {
            log.warning("Received topology with only nodes that job had failed on (forced to fail) [failedNodes=" +
                failedNodes + ']');

            // Nowhere to failover to.
            return null;
        }

        try {
            GridNode node = ctx.getBalancedNode(newTop);

            if (node == null) {
                log.warning("Load balancer returned null node for topology: " + newTop);
            }
            else {
                // Increment failover count.
                ctx.getJobResult().getJobContext().setAttribute(FAILED_NODE_LIST_ATTR, (Serializable)failedNodes);

                totalFailoverJobs++;
            }

            if (node != null) {
                log.warning("Failed over job to a new node [newNode=" + node.getId() +
                    ", oldNode=" + ctx.getJobResult().getNode().getId() +
                    ", sesId=" + ctx.getTaskSession().getId() +
                    ", job=" + ctx.getJobResult().getJob() +
                    ", jobCtx=" + ctx.getJobResult().getJobContext() +
                    ", task=" + ctx.getTaskSession().getTaskName() + ']');
            }

            return node;
        }
        catch (GridException e) {
            log.error("Failed to get next balanced node for failover: " + ctx, e);

            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        return Collections.singletonList(createSpiAttributeName(MAX_FAILOVER_ATTEMPT_ATTR));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridAlwaysFailoverSpi.class, this);
    }
}
