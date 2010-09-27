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

package org.gridgain.grid.spi.collision.priorityqueue;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides implementation for Collision SPI based on priority queue. Jobs are first ordered
 * by their priority, if one is specified, and only first {@link #getParallelJobsNumber()} jobs
 * is allowed to execute in parallel. Other jobs will be queued up.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 * <li>
 *      Number of jobs that can be executed in parallel (see {@link #setParallelJobsNumber(int)}).
 *      This number should usually be set to no greater than number of threads in the execution thread pool.
 * </li>
 * <li>
 *      Priority attribute session key (see {@link #getPriorityAttributeKey()}). Prior to
 *      returning from {@link GridTask#map(List, Object)} method, task implementation should
 *      set a value into the task session keyed by this attribute key. See {@link GridTaskSession}
 *      for more information about task session.
 * </li>
 * <li>Default priority value (see {@link #getDefaultPriority()}). It is used when no priority is set.</li>
 * </ul>
 * Below is a Java example of configuration for priority collision SPI:
 * <pre name="code" class="java">
 * GridPriorityQueueCollisionSpi colSpi = new GridPriorityQueueCollisionSpi();
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
 * Here is Spring XML configuration example:
 * <pre name="code" class="xml">
 * &lt;property name="collisionSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.collision.priorityqueue.GridPriorityQueueCollisionSpi"&gt;
 *         &lt;property name="priorityAttributeKey" value="myPriorityAttributeKey"/&gt;
 *         &lt;property name="parallelJobsNumber" value="10"/&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * <h1 class="header">Coding Example</h1>
 * Here is an example of a grid tasks that uses priority collision SPI configured in example above.
 * Note that priority collision resolution is absolutely transparent to the user and is simply a matter of proper
 * grid configuration. Also, priority may be defined only for task (it can be defined within the task,
 * not at a job level). All split jobs will be started with priority declared in their owner task.
 * <p>
 * This example demonstrates how urgent task may be declared with a higher priority value.
 * Priority SPI guarantees (see its configuration in example above, where number of parallel
 * jobs is set to <tt>1</tt>) that all jobs from <tt>MyGridUrgentTask</tt> will most likely
 * be activated first (one by one) and jobs from <tt>MyGridUsualTask</tt> with lowest priority
 * will wait. Once higher priority jobs complete, lower priority jobs will be scheduled.
 * <pre name="code" class="java">
 * public class MyGridUsualTask extends GridTaskSplitAdapter&lt;Object, Object&gt; {
 *    public static final int SPLIT_COUNT = 20;
 *
 *    &#64;GridTaskSessionResource
 *    private GridTaskSession taskSes = null;
 *
 *    &#64;Override
 *    protected Collection&lt;? extends GridJob&gt; split(int gridSize, Object arg) throws GridException {
 *        ...
 *        // Set low task priority (note that attribute name is used by the SPI
 *        // and should not be changed).
 *        taskSes.setAttribute("grid.task.priority", 5);
 *
 *        Collection&lt;GridJob&gt; jobs = new ArrayList&lt;GridJob&gt;(SPLIT_COUNT);
 *
 *        for (int i = 1; i &lt;= SPLIT_COUNT; i++) {
 *            jobs.add(new GridJobAdapter&lt;Integer&gt;(i) {
 *                ...
 *            });
 *        }
 *        ...
 *    }
 * }
 * </pre>
 * and
 * <pre name="code" class="java">
 * public class MyGridUrgentTask extends GridTaskSplitAdapter&lt;Object, Object&gt; {
 *    public static final int SPLIT_COUNT = 5;
 *
 *    &#64;GridTaskSessionResource
 *    private GridTaskSession taskSes = null;
 *
 *    &#64;Override
 *    protected Collection&lt;? extends GridJob&gt; split(int gridSize, Object arg) throws GridException {
 *        ...
 *        // Set high task priority (note that attribute name is used by the SPI
 *        // and should not be changed).
 *        taskSes.setAttribute("grid.task.priority", 10);
 *
 *        Collection&lt;GridJob&gt; jobs = new ArrayList&lt;GridJob&gt;(SPLIT_COUNT);
 *
 *        for (int i = 1; i &lt;= SPLIT_COUNT; i++) {
 *            jobs.add(new GridJobAdapter&lt;Integer&gt;(i) {
 *                ...
 *            });
 *        }
 *        ...
 *    }
 * }
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
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
public class GridPriorityQueueCollisionSpi extends GridSpiAdapter implements GridCollisionSpi,
    GridPriorityQueueCollisionSpiMBean {
    /**
     * Default number of parallel jobs allowed (value is <tt>95</tt> which is
     * slightly less same as default value of threads in the execution thread pool
     * to allow some extra threads for system processing).
     */
    public static final int DFLT_PARALLEL_JOBS_NUM = 95;

    /** Default priority attribute key (value is <tt>grid.task.priority</tt>). */
    public static final String DFLT_PRIORITY_ATTRIBUTE_KEY = "grid.task.priority";

    /**
     * Default priority that will be assigned if job does not have a
     * priority attribute set (value is <tt>0</tt>).
     */
    public static final int DFLT_PRIORITY = 0;

    /** Priority attribute key should be the same on all nodes. */
    private static final String PRIORITY_ATTRIBUTE_KEY = "gridgain:collision:priority";

    /** Number of jobs that can be executed in parallel. */
    private int parallelJobsNum = DFLT_PARALLEL_JOBS_NUM;

    /** Number of jobs that were active last time. */
    private final AtomicInteger curActiveJobsNum = new AtomicInteger(0);

    /** Number of jobs that were waiting for execution last time. */
    private final AtomicInteger curWaitJobsNum = new AtomicInteger(0);

    /** */
    private String attrKey = DFLT_PRIORITY_ATTRIBUTE_KEY;

    /** */
    private int dfltPriority = DFLT_PRIORITY;

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /**
     * Sets number of jobs that are allowed to be executed in parallel on
     * this node.
     * <p>
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
     * Sets task priority attribute key. This key will be used to look up task
     * priorities from task context (see {@link GridTaskSession#getAttribute(Serializable)}).
     * <p>
     * If not provided, default value is <tt>{@link #DFLT_PRIORITY_ATTRIBUTE_KEY}</tt>.
     *
     * @param attrKey Priority session attribute key.
     */
    @GridSpiConfiguration(optional = true)
    public void setPriorityAttributeKey(String attrKey) {
        this.attrKey = attrKey;
    }

    /**
     * {@inheritDoc}
     */
    public String getPriorityAttributeKey() {
        return attrKey;
    }

    /**
     * {@inheritDoc}
     */
    public int getDefaultPriority() {
        return dfltPriority;
    }

    /**
     * Sets default job priority. If job has no set priority this value
     * will be used to compare with another job.
     * <p>
     * If not provided, default value is <tt>{@link #DFLT_PRIORITY}</tt>.
     *
     * @param dfltPriority Default job priority.
     */
    @GridSpiConfiguration(optional = true)
    public void setDefaultPriority(int dfltPriority) {
        this.dfltPriority = dfltPriority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(PRIORITY_ATTRIBUTE_KEY), getPriorityAttributeKey());
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        assertParameter(parallelJobsNum > 0, "parallelJobsNum > 0");
        assertParameter(attrKey != null, "attrKey != null");

        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("parallelJobsNum", parallelJobsNum));
            log.info(configInfo("attrKey", attrKey));
            log.info(configInfo("dfltPriority", dfltPriority));
        }

        registerMBean(gridName, this, GridPriorityQueueCollisionSpiMBean.class);

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
        assert waitJobs != null : "ASSERTION [line=330, file=src/java/org/gridgain/grid/spi/collision/priorityqueue/GridPriorityQueueCollisionSpi.java]";
        assert activeJobs != null : "ASSERTION [line=331, file=src/java/org/gridgain/grid/spi/collision/priorityqueue/GridPriorityQueueCollisionSpi.java]";

        curActiveJobsNum.set(activeJobs.size());
        curWaitJobsNum.set(waitJobs.size());

        if (activeJobs.size() < parallelJobsNum && waitJobs.isEmpty() == false) {
            int jobsToActivate = parallelJobsNum - activeJobs.size();

            if (jobsToActivate > 0) {
                if (waitJobs.size() <= jobsToActivate) {
                    for (GridCollisionJobContext waitJob : waitJobs) {
                        waitJob.activate();
                    }
                }
                else {
                    List<GridCollisionJobContext> passiveList = new ArrayList<GridCollisionJobContext>(waitJobs);

                    Collections.sort(passiveList, new Comparator<GridCollisionJobContext>() {
                        /**
                         * {@inheritDoc}
                         */
                        public int compare(GridCollisionJobContext o1, GridCollisionJobContext o2) {
                            int p1 = getTaskPriority(o1.getTaskSession());
                            int p2 = getTaskPriority(o2.getTaskSession());

                            return p1 < p2 ? 1 : p1 == p2 ? 0 : -1;
                        }
                    });

                    for (int i = 0; i < jobsToActivate; i++) {
                        passiveList.get(i).activate();
                    }
                }
            }
        }
    }

    /**
     * Gets task priority from task context. If task has no priority default
     * one will be used.
     *
     * @param ses Task session.
     * @return Task priority.
     */
    private int getTaskPriority(GridTaskSession ses) {
        assert ses != null : "ASSERTION [line=376, file=src/java/org/gridgain/grid/spi/collision/priorityqueue/GridPriorityQueueCollisionSpi.java]";

        Integer p = null;

        try {
            p = (Integer)ses.getAttribute(attrKey);
        }
        catch (ClassCastException e) {
            log.error("Type of task session priority attribute '" + attrKey +
                "' is not java.lang.Integer (will use default priority) [type=" +
                ses.getAttribute(attrKey).getClass() + ", dfltPriority=" + dfltPriority + ']', e);

            p = dfltPriority;
        }

        if (p == null) {
            if (log.isDebugEnabled() == true) {
                log.debug("Failed get priority from task session attribute '" + attrKey +
                    "' (will use default priority): " + dfltPriority);
            }

            p = dfltPriority;
        }

        assert p != null : "ASSERTION [line=400, file=src/java/org/gridgain/grid/spi/collision/priorityqueue/GridPriorityQueueCollisionSpi.java]";

        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        return Collections.singletonList(createSpiAttributeName(PRIORITY_ATTRIBUTE_KEY));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridPriorityQueueCollisionSpi.class, this);
    }
}
