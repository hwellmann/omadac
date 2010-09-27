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

package org.gridgain.grid.spi.collision.fifoqueue;

import java.util.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides implementation for Collision SPI based on FIFO queue. Jobs are ordered
 * as they arrived and only {@link #getParallelJobsNumber()} number of jobs is allowed to
 * execute in parallel. Other jobs will be buffered in the passive queue.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 * <li>
 *      Number of jobs that can execute in parallel (see {@link #setParallelJobsNumber(int)}).
 *      This number should usually be set to the number of threads in the execution thread pool.
 * </li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * <tt>GridFifoQueueCollisionSpi</tt> can be configured as follows:
 * <pre name="code" class="java">
 * GridFifoQueueCollisionSpi colSpi = new GridFifoQueueCollisionSpi();
 *
 * // Execute all jobs sequentially by setting parallel job number to 1.
 * colSpi.setParallelJobsNumber(1);
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default collision SPI.
 * cfg.setCollisionSpi(colSpi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * <tt>GridFifoQueueCollisionSpi</tt> can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *       ...
 *       &lt;property name="collisionSpi"&gt;
 *           &lt;bean class="org.gridgain.grid.spi.collision.fifoqueue.GridFifoQueueCollisionSpi"&gt;
 *               &lt;property name="parallelJobsNumber" value="1"/&gt;
 *           &lt;/bean&gt;
 *       &lt;/property&gt;
 *       ...
 * &lt;/bean&gt;
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridFifoQueueCollisionSpi extends GridSpiAdapter implements GridCollisionSpi, GridFifoQueueCollisionSpiMBean {
    /**
     * Default number of parallel jobs allowed (value is <tt>95</tt> which is
     * slightly less same as default value of threads in the execution thread pool
     * to allow some extra threads for system processing).
     */
    public static final int DFLT_PARALLEL_JOBS_NUM = 95;

    /** Number of jobs that can be executed in parallel. */
    private int parallelJobsNum = DFLT_PARALLEL_JOBS_NUM;

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** Number of jobs that were active last time. */
    private final AtomicInteger curActiveJobsNum = new AtomicInteger(0);

    /** Number of jobs that were waiting for execution last time. */
    private final AtomicInteger curWaitJobsNum = new AtomicInteger(0);

    /**
     * Sets number of jobs that are allowed to be executed in parallel on this node.
     * If not provided, default value is <tt>{@link #DFLT_PARALLEL_JOBS_NUM}</tt>.
     *
     * @param parallelJobsNum Maximum number of jobs to be executed in parallel.
     */
    @GridSpiConfiguration(optional = true)
    public void setParallelJobsNumber(int parallelJobsNum) {
        this.parallelJobsNum = parallelJobsNum;
    }

    /**
     * {@inheritDoc}
     */
    public int getParallelJobsNumber() {
        return parallelJobsNum;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentWaitJobsNumber() {
        return curWaitJobsNum.get();

    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentActiveJobsNumber() {
        return curActiveJobsNum.get();
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        assertParameter(parallelJobsNum > 0, "parallelJobsNum > 0");

        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("parallelJobsNum", parallelJobsNum));
        }

        registerMBean(gridName, this, GridFifoQueueCollisionSpiMBean.class);

        // Ack start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        // Ack stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setExternalCollisionListener(GridCollisionExternalListener listener) {
        // No-op.
    }

    /**
     * {@inheritDoc}
     */
    public void onCollision(Collection<GridCollisionJobContext> waitJobs,
        Collection<GridCollisionJobContext> activeJobs) {
        assert waitJobs != null : "ASSERTION [line=186, file=src/java/org/gridgain/grid/spi/collision/fifoqueue/GridFifoQueueCollisionSpi.java]";
        assert activeJobs != null : "ASSERTION [line=187, file=src/java/org/gridgain/grid/spi/collision/fifoqueue/GridFifoQueueCollisionSpi.java]";

        curActiveJobsNum.set(activeJobs.size());
        curWaitJobsNum.set(waitJobs.size());

        int activateCnt = parallelJobsNum - activeJobs.size();

        if (activateCnt > 0) {
            int cnt = 0;

            for (GridCollisionJobContext waitCtx : waitJobs) {
                if (cnt++ == activateCnt) {
                    break;
                }

                waitCtx.activate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridFifoQueueCollisionSpi.class, this);
    }
}
