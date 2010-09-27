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

package org.gridgain.grid.spi.failover.jobstealing;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.collision.jobstealing.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.grid.spi.failover.always.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Job stealing failover SPI needs to always be used in conjunction with
 * {@link GridJobStealingCollisionSpi} SPI. When {@link GridJobStealingCollisionSpi}
 * receives a <b>steal</b> request and rejects jobs so they can be routed to the
 * appropriate node, it is the responsibility of this <tt>GridJobStealingFailoverSpi</tt>
 * SPI to make sure that the job is indeed re-routed to the node that has sent the initial
 * request to <b>steal</b> it.
 * <p>
 * <tt>GridJobStealingFailoverSpi</tt> knows where to route a job based on the
 * {@link GridJobStealingCollisionSpi#THIEF_NODE_ATTR} job context attribute (see {@link GridJobContext}).
 * Prior to rejecting a job,  {@link GridJobStealingCollisionSpi} will populate this
 * attribute with the ID of the node that wants to <b>steal</b> this job.
 * Then <tt>GridJobStealingFailoverSpi</tt> will read the value of this attribute and
 * route the job to the node specified.
 * <p>
 * If failure is caused by a node crash, and not by <b>steal</b> request, then this
 * SPI behaves identically to {@link GridAlwaysFailoverSpi}, and tries to find the
 * next balanced node to fail-over a job to.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 * <li>Maximum failover attempts for a single job (see {@link #setMaximumFailoverAttempts(int)}).</li>
 * </ul>
 * Here is a Java example on how to configure grid with <tt>GridJobStealingFailoverSpi</tt>.
 * <pre name="code" class="java">
 * GridJobStealingFailoverSpi spi = new GridJobStealingFailoverSpi();
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
 </pre>
 * Here is an example of how to configure <tt>GridJobStealingFailoverSpi</tt> from Spring XML configuration file.
 * <pre name="code" class="xml">
 * &lt;property name="failoverSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.failover.jobstealing.GridJobStealingFailoverSpi"&gt;
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
public class GridJobStealingFailoverSpi extends GridSpiAdapter implements GridFailoverSpi,
    GridJobStealingFailoverSpiMBean {
    /** Maximum number of attempts to execute a failed job on another node (default is <tt>5</tt>). */
    public static final int DFLT_MAX_FAILOVER_ATTEMPTS = 5;

    /**
     * Name of job context attribute containing all nodes a job failed on. Note
     * that this list does not include nodes that a job was stolen from.
     *
     * @see GridJobContext
     */
    static final String FAILED_NODE_LIST_ATTR = "gridgain:failover:failednodelist";

    /**
     * Name of job context attribute containing current failover attempt count.
     * This count is incremented every time the same job gets failed over to
     * another node for execution if it was not successfully stolen.
     *
     * @see GridJobContext
     */
    static final String FAILOVER_ATTEMPT_COUNT_ATTR = "gridgain:failover:attemptcount";

    /** Maximum failover attempts job context attribute name. */
    private static final String MAX_FAILOVER_ATTEMPT_ATTR = "gridgain:failover:maxattempts";

    /** Injected grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** Maximum number of attempts to execute a failed job on another node. */
    private int maxFailoverAttempts = DFLT_MAX_FAILOVER_ATTEMPTS;

    /** Number of jobs that were failed over. */
    private int totalFailoveredJobs = 0;

    /**
     * {@inheritDoc}
     */
    public int getMaximumFailoverAttempts() {
        return maxFailoverAttempts;
    }

    /**
     * Sets maximum number of attempts to execute a failed job on another node.
     * If job gets stolen and thief node exists then it is not considered as
     * failed job.
     * If not specified, {@link #DFLT_MAX_FAILOVER_ATTEMPTS} value will be used.
     * <p>
     * Note this value must be identical for all grid nodes in the grid.
     *
     * @param maxFailoverAttempts Maximum number of attempts to execute a failed
     *      job on another node.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaximumFailoverAttempts(int maxFailoverAttempts) {
        this.maxFailoverAttempts = maxFailoverAttempts;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalFailoveredJobsCount() {
        return totalFailoveredJobs;
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

        assertParameter(maxFailoverAttempts >= 0, "maximumFailoverAttempts >= 0");

        if (log.isInfoEnabled()) {
            log.info(configInfo("maxFailoverAttempts", maxFailoverAttempts));
        }

        registerMBean(gridName, this, GridJobStealingFailoverSpiMBean.class);

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
        assert ctx != null : "ASSERTION [line=210, file=src/java/org/gridgain/grid/spi/failover/jobstealing/GridJobStealingFailoverSpi.java]";
        assert top != null : "ASSERTION [line=211, file=src/java/org/gridgain/grid/spi/failover/jobstealing/GridJobStealingFailoverSpi.java]";

        if (top.size() == 0) {
            log.warning("Received empty subgrid and is forced to fail (check topology SPI?)");

            // Nowhere to failover to.
            return null;
        }

        Integer failoverCnt = (Integer)ctx.getJobResult().getJobContext().getAttribute(FAILOVER_ATTEMPT_COUNT_ATTR);

        if (failoverCnt == null) {
            failoverCnt = 0;
        }

        if (failoverCnt > maxFailoverAttempts) {
            log.error("Failover count exceeded maximum failover attempts parameter [failedJob=" +
                ctx.getJobResult().getJob() + ", maxFailoverAttempts=" + maxFailoverAttempts + ']');

            return null;
        }

        if (failoverCnt == maxFailoverAttempts) {
            log.warning("Job failover failed because number of maximum failover attempts is exceeded [failedJob=" +
                ctx.getJobResult().getJob() + ", maxFailoverAttempts=" + maxFailoverAttempts + ']');

            return null;
        }

        GridNode thief = null;

        boolean isNodeFailed = false;

        try {
            UUID thiefId = (UUID)ctx.getJobResult().getJobContext().getAttribute(
                GridJobStealingCollisionSpi.THIEF_NODE_ATTR);

            if (thiefId != null) {
                thief = getSpiContext().getNode(thiefId);

                if (thief != null) {
                    // If sender != receiver.
                    if (thief.equals(ctx.getJobResult().getNode()) == true) {
                        log.error("Job stealer node is equal to job stealee node (will fail-over using load-balancing): "
                            + thief.getId());

                        isNodeFailed = true;

                        thief = null;
                    }
                    else if (top.contains(thief) == false) {
                        log.warning("Thief node is not part of task topology  (will fail-over using load-balancing) " +
                            "[thief=" + thiefId + ", topSize=" + top.size() + ']');

                        thief = null;
                    }

                    if (log.isDebugEnabled() == true) {
                        log.debug("Failing-over stolen job [from=" + ctx.getJobResult().getNode() + ", to=" +
                            thief + ']');
                    }
                }
                else {
                    isNodeFailed = true;

                    log.warning("Thief node left grid (will fail-over using load balancing): " + thiefId);
                }
            }
            else {
                isNodeFailed = true;
            }

            // If job was not stolen or stolen node is not part of topology,
            // then failover the regular way.
            if (thief == null) {
                Set<UUID> failedNodes = (Set<UUID>)ctx.getJobResult().getJobContext().getAttribute(
                    FAILED_NODE_LIST_ATTR);

                if (failedNodes == null) {
                    failedNodes = new HashSet<UUID>(1);
                }

                if (isNodeFailed == true) {
                    failedNodes.add(ctx.getJobResult().getNode().getId());
                }

                // Set updated failed node set into job context.
                ctx.getJobResult().getJobContext().setAttribute(FAILED_NODE_LIST_ATTR, (Serializable)failedNodes);

                // Copy.
                List<GridNode> newTop = new ArrayList<GridNode>(top.size());

                for (GridNode n : top) {
                    // Add non-failed nodes to topology.
                    if (failedNodes.contains(n.getId()) == false) {
                        newTop.add(n);
                    }
                }

                if (newTop.isEmpty() == true) {
                    log.warning("Received topology with only nodes that job had failed on (forced to fail) " +
                        "[failedNodes=" + failedNodes + ']');

                    // Nowhere to failover to.
                    return null;
                }

                thief = ctx.getBalancedNode(newTop);

                if (thief == null) {
                    log.warning("Load balancer returned null node for topology: " + newTop);
                }
            }

            if (isNodeFailed == true) {
                // This is a failover, not stealing.
                failoverCnt++;
            }

            // Even if it was stealing and thief node left grid we assume
            // that it is failover because of the fail.
            ctx.getJobResult().getJobContext().setAttribute(FAILOVER_ATTEMPT_COUNT_ATTR, failoverCnt);

            totalFailoveredJobs++;

            if (thief != null) {
                if (log.isInfoEnabled() == true) {
                    log.info("Stealing job to a new node [newNode=" + thief.getId() +
                        ", oldNode=" + ctx.getJobResult().getNode().getId() +
                        ", job=" + ctx.getJobResult().getJob() +
                        ", task=" + ctx.getTaskSession().getTaskName() +
                        ", sessionId=" + ctx.getTaskSession().getId() +']');
                }
            }

            return thief;
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
        return GridToStringBuilder.toString(GridJobStealingFailoverSpi.class, this);
    }
}
