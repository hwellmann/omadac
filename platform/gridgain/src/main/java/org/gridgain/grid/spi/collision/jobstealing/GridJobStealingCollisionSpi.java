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

package org.gridgain.grid.spi.collision.jobstealing;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Collision SPI that supports job stealing from over-utilized nodes to
 * under-utilized nodes. This SPI is especially useful if you have
 * some jobs within task complete fast, and others sitting in the waiting
 * queue on slower nodes. In such case, the waiting jobs will be <b>stolen</b>
 * from slower node and moved to the fast under-utilized node.
 * <p>
 * The design and ideas for this SPI are significantly influenced by
 * <a href="http://gee.cs.oswego.edu/dl/papers/fj.pdf">Java Fork/Join Framework</a>
 * authored by Doug Lea and planned for Java 7. <tt>GridJobStealingCollisionSpi</tt> took
 * similar concepts and applied them to the grid (as opposed to within VM support planned
 * in Java 7).
 * <p>
 * Quite often grids are deployed across many computers some of which will
 * always be more powerful than others. This SPI helps you avoid jobs being
 * stuck at a slower node, as they will be stolen by a faster node. In the following picture
 * when Node<sub>3</sub> becomes free, it steals Job<sub>13</sub> and Job<sub>23</sub>
 * from Node<sub>1</sub> and Node<sub>2</sub> respectively.
 * <p>
 * <center><img src="http://www.gridgain.com/images/job_stealing_white.png"></center>
 * <p>
 * <i>
 * Note that this SPI must always be used in conjunction with
 * {@link org.gridgain.grid.spi.failover.jobstealing.GridJobStealingFailoverSpi}.
 * The responsibility of Job Stealing Failover SPI is to properly route <b>stolen</b>
 * jobs to the nodes that initially requested (<b>stole</b>) these jobs. The
 * SPI maintains a counter of how many times a jobs was stolen and
 * hence traveled to another node. <tt>GridJobStealingCollisionSpi</tt>
 * checks this counter and will not allow a job to be stolen if this counter
 * exceeds a certain threshold {@link GridJobStealingCollisionSpi#setMaximumStealingAttempts(int)}.
 * </i>
 * <p>
 * <h1 class="header">Configuration</h1>
 * In order to use this SPI, you should configure your grid instance
 * to use <tt>GridJobStealingCollisionSpi</tt> either from Spring XML file or
 * directly. The following configuration parameters are supported:
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>
 *      Maximum number of active jobs that will be allowed by this SPI
 *      to execute concurrently (see {@link #setActiveJobsThreshold(int)}).
 * </li>
 * <li>
 *      Maximum number of waiting jobs. Once waiting queue size goes below
 *      this number, this SPI will attempt to steal jobs from over-utilized
 *      nodes by sending <b>"steal"</b> requests (see {@link #setWaitJobsThreshold(int)}).
 * </li>
 * <li>
 *      Steal message expire time. If no response was received from a node
 *      to which <b>steal</b> request was sent, then request will be considered
 *      lost and will be resent, potentially to another node (see {@link #setMessageExpireTime(long)}).
 * </li>
 * <li>
 *      Maximum number of stealing attempts for the job (see {@link #setMaximumStealingAttempts(int)}).
 * </li>
 * <li>
 *      Whether stealing enabled or not (see {@link #setStealingEnabled(boolean)}).
 * </li>
 * <li>
 *     Enables stealing to/from only nodes that have these attributes set
 *     (see {@link #setStealingAttributes(Map)}).
 * </li>
 * </ul>
 * Below is example of configuring this SPI from Java code:
 * <pre name="code" class="java">
 * GridJobStealingCollisionSpi spi = new GridJobStealingCollisionSpi();
 *
 * // Configure number of waiting jobs
 * // in the queue for job stealing.
 * spi.setWaitJobsThreshold(10);
 *
 * // Configure message expire time (in milliseconds).
 * spi.setMessageExpireTime(500);
 *
 * // Configure stealing attempts number.
 * spi.setMaximumStealingAttempts(10);
 *
 * // Configure number of active jobs that are allowed to execute
 * // in parallel. This number should usually be equal to the number
 * // of threads in the pool (default is 100).
 * spi.setActiveJobsThreshold(50);
 *
 * // Enable stealing.
 * spi.setStealingEnabled(true);
 *
 * // Set stealing attribute to steal from/to nodes that have it.
 * spi.setStealingAttributes(Collections.singletonMap("node.segment", "foobar"));
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default Collision SPI.
 * cfg.setCollisionSpi(spi);
 * </pre>
 * Here is an example of how this SPI can be configured from Spring XML configuration:
 * <pre name="code" class="xml">
 * &lt;property name="collisionSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.collision.jobstealing.GridJobStealingCollisionSpi"&gt;
 *         &lt;property name="activeJobsThreshold" value="100"/&gt;
 *         &lt;property name="waitJobsThreshold" value="0"/&gt;
 *         &lt;property name="messageExpireTime" value="1000"/&gt;
 *         &lt;property name="maximumStealingAttempts" value="10"/&gt;
 *         &lt;property name="stealingEnabled" value="true"/&gt;
 *         &lt;property name="stealingAttributes"&gt;
 *             &lt;map&gt;
 *                 &lt;entry key="node.segment" value="foobar"/&gt;
 *             &lt;/map&gt;
 *         &lt;/property&gt;
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
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridJobStealingCollisionSpi extends GridSpiAdapter implements GridCollisionSpi,
    GridJobStealingCollisionSpiMBean {
    /** Maximum number of attempts to steal job by another node (default is <tt>5</tt>). */
    public static final int DFLT_MAX_STEALING_ATTEMPTS = 5;

    /**
     * Default number of parallel jobs allowed (value is <tt>95</tt> which is
     * slightly less same as default value of threads in the execution thread pool
     * to allow some extra threads for system processing).
     */
    public static final int DFLT_ACTIVE_JOBS_THRESHOLD = 95;

    /**
     * Default steal message expire time in milliseconds (value is <tt>1000</tt>).
     * Once this time is elapsed and no response for steal message is received,
     * the message is considered lost and another steal message will be generated,
     * potentially to another node.
     */
    public static final long DFLT_MSG_EXPIRE_TIME = 1000;

    /**
     * Default threshold of waiting jobs. If number of waiting jobs exceeds this threshold,
     * then waiting jobs will become available to be stolen (value is <tt>0</tt>).
     */
    public static final int DFLT_WAIT_JOBS_THRESHOLD = 0;

    /** Communication topic. */
    private static final String JOB_STEALING_COMM_TOPIC = "gridgain:collision:jobstealingtopic";

    /** Job context attribute for storing thief node UUID (this attribute is used in job stealing failover SPI). */
    public static final String THIEF_NODE_ATTR = "gridgain:collision:theifnode";

    /** Threshold of maximum jobs on waiting queue. */
    static final String WAIT_JOBS_THRESHOLD_NODE_ATTR = "gridgain:collision:waitjobsthreshold";

    /** Threshold of maximum jobs executing concurrently. */
    private static final String ACTIVE_JOBS_THRESHOLD_NODE_ATTR = "gridgain:collision:activejobsthreshold";

    /**
     * Name of job context attribute containing current stealing attempt count.
     * This count is incremented every time the same job gets stolen for
     * execution.
     *
     * @see GridJobContext
     */
    static final String STEALING_ATTEMPT_COUNT_ATTR = "gridgain:stealing:attemptcount";

    /** Maximum stealing attempts attribute name. */
    private static final String MAX_STEALING_ATTEMPT_ATTR = "gridgain:stealing:maxattempts";

    /** Stealing request expiration time attribute name. */
    private static final String MSG_EXPIRE_TIME = "gridgain:stealing:msgexpiretime";

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** Number of jobs that can be executed in parallel. */
    private int activeJobsThreshold = DFLT_ACTIVE_JOBS_THRESHOLD;

    /** Configuration parameter defining waiting job count threshold for stealing to start. */
    private int waitJobsThreshold = DFLT_WAIT_JOBS_THRESHOLD;

    /** Message expire time configuration parameter. */
    private long msgExpireTime = DFLT_MSG_EXPIRE_TIME;

    /** Maximum number of attempts to steal job by another node. */
    private int maxStealingAttempts = DFLT_MAX_STEALING_ATTEMPTS;

    /** Flag indicating whether job stealing is enabled. */
    private boolean isStealingEnabled = true;

    /** */
    @GridToStringInclude
    private Map<String, ? extends Serializable> stealAttrs = null;

    /** Number of jobs that were active last time. */
    private final AtomicInteger curActiveJobsNum = new AtomicInteger(0);

    /** Number of jobs that were waiting for execution last time. */
    private final AtomicInteger curWaitJobsNum = new AtomicInteger(0);

    /** Total number of stolen jobs. */
    private final AtomicInteger totalStolenJobsNum = new AtomicInteger(0);

    /**
     * Map of sent messages. Note that we choose concurrency level of <tt>64</tt> as
     * there is no way to predict number of concurrent threads and this is the closest
     * power of 2 that makes sense.
     */
    private final ConcurrentMap<UUID, MessageInfo> sendMsgMap =
        new ConcurrentHashMap<UUID, MessageInfo>(16, 0.75f, 64);

    /**
     * Map of received messages. Note that we choose concurrency level of <tt>64</tt> as
     * there is no way to predict number of concurrent threads and this is the closest
     * power of 2 that makes sense.
     */
    private final ConcurrentMap<UUID, MessageInfo> rcvMsgMap =
        new ConcurrentHashMap<UUID, MessageInfo>(16, 0.75f, 64);

    /** */
    private final ConcurrentLinkedQueue<GridNode> nodeQueue = new ConcurrentLinkedQueue<GridNode>();

    /** */
    private GridCollisionExternalListener externalListener = null;

    /** Discovery listener. */
    private GridDiscoveryListener discoListener = null;

    /** Communication listener. */
    private GridMessageListener msgListener = null;

    /** Number of steal requests. */
    private AtomicInteger stealReqs = new AtomicInteger(0);

    /**
     * Sets number of jobs that are allowed to be executed in parallel on
     * this node. Node that this attribute may be different for different
     * grid nodes as stronger nodes may be able to execute more jobs in
     * parallel.
     * <p>
     * If not provided, default value is <tt>{@link #DFLT_ACTIVE_JOBS_THRESHOLD}</tt>.
     *
     * @param activeJobsThreshold Maximum number of jobs to be executed in parallel.
     */
    @GridSpiConfiguration(optional = true)
    public void setActiveJobsThreshold(int activeJobsThreshold) {
        this.activeJobsThreshold = activeJobsThreshold;
    }

    /**
     * {@inheritDoc}
     */
    public int getActiveJobsThreshold() {
        return activeJobsThreshold;
    }

    /**
     * Sets wait jobs threshold. If number of jobs in the waiting queue goes
     * below (or equal to) this threshold, then implementation will attempt to steal jobs
     * from other, more over-loaded nodes.
     * <p>
     * Note this value may be different (but does not have to be) for different
     * nodes in the grid. You may wish to give stronger nodes a smaller waiting
     * threshold so they can start stealing jobs from other nodes sooner.
     * <p>
     * If not provided, default value is <tt>{@link #DFLT_WAIT_JOBS_THRESHOLD}</tt>.
     *
     * @param waitJobsThreshold Default job priority.
     */
    @GridSpiConfiguration(optional = true)
    public void setWaitJobsThreshold(int waitJobsThreshold) {
        this.waitJobsThreshold = waitJobsThreshold;
    }

    /**
     * {@inheritDoc}
     */
    public int getWaitJobsThreshold() {
        return waitJobsThreshold;
    }

    /**
     * Message expire time configuration parameter. If no response is received
     * from a busy node to a job stealing request, then implementation will
     * assume that message never got there, or that remote node does not have
     * this node included into topology of any of the jobs it has. In any
     * case, job steal request will be resent (potentially to another node).
     * <p>
     * If not provided, default value is <tt>{@link #DFLT_MSG_EXPIRE_TIME}</tt>.
     *
     * @param msgExpireTime Message expire time.
     */
    @GridSpiConfiguration(optional = true)
    public void setMessageExpireTime(long msgExpireTime) {
        this.msgExpireTime = msgExpireTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getMessageExpireTime() {
        return msgExpireTime;
    }

    /**
     * Sets flag indicating whether this node should attempt to steal jobs
     * from other nodes. If <tt>false</tt>, then this node will steal allow
     * jobs to be stolen from it, but won't attempt to steal any jobs from
     * other nodes.
     * <p>
     * Default value is <tt>true</tt>.
     *
     * @param isStealingEnabled Flag indicating whether this node should attempt
     *      to steal jobs from other nodes
     */
    @GridSpiConfiguration(optional = true)
    public void setStealingEnabled(boolean isStealingEnabled) {
        this.isStealingEnabled = isStealingEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStealingEnabled() {
        return isStealingEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumStealingAttempts() {
        return maxStealingAttempts;
    }

    /**
     * Sets maximum number of attempts to steal job by another node.
     * If not specified, {@link #DFLT_MAX_STEALING_ATTEMPTS} value will be used.
     * <p>
     * Note this value must be identical for all grid nodes in the grid.
     *
     * @param maxStealingAttempts Maximum number of attempts to steal job by
     *      another node.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaximumStealingAttempts(int maxStealingAttempts) {
        this.maxStealingAttempts = maxStealingAttempts;
    }

    /**
     * Configuration parameter to enable stealing to/from only nodes that
     * have these attributes set (see {@link GridNode#getAttribute(String)} and
     * {@link GridConfiguration#getUserAttributes()} methods).
     *
     * @param stealAttrs Node attributes to enable job stealing for.
     */
    @GridSpiConfiguration(optional = true)
    public void setStealingAttributes(Map<String, ? extends Serializable> stealAttrs) {
        this.stealAttrs = stealAttrs;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, ? extends Serializable> getStealingAttributes() {
        return stealAttrs;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentWaitJobsCount() {
        return curWaitJobsNum.get();

    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentActiveJobsCount() {
        return curActiveJobsNum.get();
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalStolenJobsCount() {
        return totalStolenJobsNum.get();
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentJobsToStealCount() {
        return stealReqs.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(WAIT_JOBS_THRESHOLD_NODE_ATTR), waitJobsThreshold,
            createSpiAttributeName(ACTIVE_JOBS_THRESHOLD_NODE_ATTR), activeJobsThreshold,
            createSpiAttributeName(MAX_STEALING_ATTEMPT_ATTR), maxStealingAttempts,
            createSpiAttributeName(MSG_EXPIRE_TIME), msgExpireTime);
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        assertParameter(activeJobsThreshold >= 0, "activeJobsThreshold >= 0");
        assertParameter(waitJobsThreshold >= 0, "waitJobsThreshold >= 0");
        assertParameter(msgExpireTime > 0, "messageExpireTime > 0");
        assertParameter(maxStealingAttempts > 0, "maxStealingAttempts > 0");

        if (waitJobsThreshold < DFLT_WAIT_JOBS_THRESHOLD && activeJobsThreshold == DFLT_WAIT_JOBS_THRESHOLD) {
            throw (GridSpiException)new GridSpiException("'waitJobsThreshold' configuration parameter does not have effect " +
                "because 'parallelJobsNumber' is boundless by default. Make sure to limit 'parallelJobsNumber'" +
                "configuration parameter.").setData(463, "src/java/org/gridgain/grid/spi/collision/jobstealing/GridJobStealingCollisionSpi.java");
        }

        // Start SPI start stopwatch.
        startStopwatch();

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("activeJobsThreshold", activeJobsThreshold));
            log.info(configInfo("waitJobsThreshold", waitJobsThreshold));
            log.info(configInfo("messageExpireTime", msgExpireTime));
            log.info(configInfo("maxStealingAttempts", maxStealingAttempts));
        }

        registerMBean(gridName, this, GridJobStealingCollisionSpiMBean.class);

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
        externalListener = listener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onContextInitialized(GridSpiContext spiCtx) throws GridSpiException {
        super.onContextInitialized(spiCtx);

        Collection<GridNode> rmtNodes = getSpiContext().getRemoteNodes();

        for (GridNode node : rmtNodes) {
            sendMsgMap.put(node.getId(), new MessageInfo());
            rcvMsgMap.put(node.getId(), new MessageInfo());
        }

        nodeQueue.addAll(rmtNodes);

        getSpiContext().addDiscoveryListener(discoListener = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                switch (type) {
                    case FAILED:
                    case LEFT: {
                        nodeQueue.remove(node);

                        sendMsgMap.remove(node.getId());
                        rcvMsgMap.remove(node.getId());

                        break;
                    }

                    case JOINED: {
                        sendMsgMap.put(node.getId(), new MessageInfo());
                        rcvMsgMap.put(node.getId(), new MessageInfo());

                        nodeQueue.offer(node);

                        break;
                    }

                    case METRICS_UPDATED: { break; } // No-op.

                    default: { assert false; } // Should never be reached.
                }
            }
        });

        getSpiContext().addMessageListener(msgListener = new GridMessageListener() {
            /**
             * {@inheritDoc}
             */
            public void onMessage(UUID nodeId, Serializable msg) {
                MessageInfo info = rcvMsgMap.get(nodeId);

                if (info == null) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Ignoring message steal request as discovery event was not received yet for node: " +
                            nodeId);
                    }

                    return;
                }

                synchronized (info) {
                    // Increment total number of steal requests.
                    // Note that it is critical to increment total
                    // number of steal requests before resetting message info.
                    stealReqs.addAndGet((Integer)msg - info.getJobsToSteal());

                    info.reset((Integer)msg);
                }

                GridCollisionExternalListener listener = externalListener;

                // Let grid know that collisions should be resolved.
                if (listener != null) {
                    listener.onExternalCollision();
                }
            }
        }, JOB_STEALING_COMM_TOPIC);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onContextDestroyed() {
        if (discoListener != null) {
            getSpiContext().removeDiscoveryListener(discoListener);
        }

        if (msgListener != null) {
            getSpiContext().removeMessageListener(msgListener, JOB_STEALING_COMM_TOPIC);
        }

        super.onContextDestroyed();
    }

    /**
     * {@inheritDoc}
     */
    public void onCollision(Collection<GridCollisionJobContext> waitJobs,
        Collection<GridCollisionJobContext> activeJobs) {
        assert activeJobs.size() <= activeJobsThreshold : "Amount of active jobs exceeds threshold [activeJobs=" +
            activeJobs.size() + ", activeJobsTrheshold=" + activeJobsThreshold + ']';

        curActiveJobsNum.set(activeJobs.size());
        curWaitJobsNum.set(waitJobs.size());

        // Check if there are any jobs to activate or reject.
        int rejected = checkBusy(waitJobs, activeJobs);

        totalStolenJobsNum.addAndGet(rejected);

        // No point of stealing jobs if some jobs were rejected.
        if (rejected > 0) {
            if (log.isDebugEnabled() == true) {
                log.debug("Total count of rejected jobs: " + rejected);
            }

            return;
        }

        if (isStealingEnabled == true) {
            // Check if there are jobs to steal.
            checkIdle(waitJobs, activeJobs);
        }
    }

    /**
     * Check if node is busy and activate/reject proper number of jobs.
     *
     * @param waitJobs Waiting jobs.
     * @param activeJobs Active jobs.
     * @return Number of rejected jobs.
     */
    private int checkBusy(Collection<GridCollisionJobContext> waitJobs,
        Collection<GridCollisionJobContext> activeJobs) {
        GridSpiContext ctx = getSpiContext();

        int activateCnt = activeJobsThreshold - activeJobs.size();

        int cnt = 0;

        int rejected = 0;

        for (GridCollisionJobContext waitCtx : waitJobs) {
            if (activateCnt > 0 && cnt < activateCnt) {
                cnt++;

                // If job was activated/cancelled by another thread, then
                // this method is no-op.
                waitCtx.activate();
            }
            else if (waitCtx.getJob().getClass().isAnnotationPresent(GridJobStealingDisabled.class) == false
                && stealReqs.get() > 0) {
                // Collision count attribute.
                Integer stealingCnt = (Integer)waitCtx.getJobContext().getAttribute(STEALING_ATTEMPT_COUNT_ATTR);

                // Check that maximum stealing attempt threshold
                // has not been exceeded.
                if (stealingCnt != null) {
                    // If job exceeded failover threshold, skip it.
                    if (stealingCnt >= maxStealingAttempts) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Waiting job exceeded stealing attempts and won't be rejected " +
                                "(will try other jobs on waiting list): " + waitCtx);
                        }

                        continue;
                    }
                }
                else {
                    stealingCnt = 0;
                }

                // Check if allowed to reject job.
                int jobsToReject = waitJobs.size() - cnt - rejected - waitJobsThreshold;

                if (log.isDebugEnabled() == true) {
                    log.debug("Jobs to reject count [jobsToReject=" + jobsToReject + ", waitCtx=" + waitCtx + ']');
                }

                if (jobsToReject <= 0) {
                    break;
                }

                // If we have an excess of waiting jobs, reject as many as there are
                // requested to be stolen. Note, that we use lose total steal request
                // counter to prevent excessive iteration over nodes under load.
                for (Iterator<Entry<UUID, MessageInfo>> iter = rcvMsgMap.entrySet().iterator();
                    iter.hasNext() == true && stealReqs.get() > 0;) {
                    Entry<UUID, MessageInfo> entry = iter.next();

                    UUID nodeId = entry.getKey();

                    // Node has left topology.
                    if (ctx.getNode(nodeId) == null) {
                        iter.remove();

                        continue;
                    }

                    MessageInfo info = entry.getValue();

                    int jobsAsked;

                    synchronized (info) {
                        jobsAsked = info.getJobsToSteal();

                        assert jobsAsked >= 0 : "ASSERTION [line=717, file=src/java/org/gridgain/grid/spi/collision/jobstealing/GridJobStealingCollisionSpi.java]";

                        // Skip nodes that have not asked for jobs to steal.
                        if (jobsAsked == 0) {
                            // Move to next node.
                            continue;
                        }

                        // If message is expired, ignore it.
                        if (info.isExpired() == true) {
                            // Subtract expired messages.
                            stealReqs.addAndGet(-info.getJobsToSteal());

                            assert stealReqs.get() >= 0 : "ASSERTION [line=730, file=src/java/org/gridgain/grid/spi/collision/jobstealing/GridJobStealingCollisionSpi.java]";

                            info.reset(0);

                            continue;
                        }

                        // Check that waiting job has thief node in topology.
                        try {
                            Collection<GridNode> top = ctx.getTopology(waitCtx.getTaskSession(), ctx.getAllNodes());

                            boolean found = false;

                            for (GridNode node : top) {
                                if (node.getId().equals(nodeId) == true) {
                                    found = true;

                                    break;
                                }
                            }

                            if (found == false) {
                                if (log.isDebugEnabled() == true) {
                                    log.debug("Thief node does not belong to task topology [thief=" + nodeId +
                                        ", task=" + waitCtx.getTaskSession() + ']');
                                }

                                continue;
                            }
                        }
                        catch (GridSpiException e) {
                            log.error("Failed to check topology for job: " + waitCtx.getTaskSession(), e);

                            continue;
                        }

                        rejected++;

                        // If job was not cancelled already by another thread.
                        if (waitCtx.cancel() == true) {
                            stealReqs.decrementAndGet();

                            assert stealReqs.get() >= 0 : "ASSERTION [line=772, file=src/java/org/gridgain/grid/spi/collision/jobstealing/GridJobStealingCollisionSpi.java]";

                            // Mark job as stolen.
                            waitCtx.getJobContext().setAttribute(THIEF_NODE_ATTR, nodeId);
                            waitCtx.getJobContext().setAttribute(STEALING_ATTEMPT_COUNT_ATTR, stealingCnt + 1);

                            info.reset(jobsAsked - 1);

                            if (log.isDebugEnabled() == true) {
                                log.debug("Rejecting job due to steal request [ctx=" + waitCtx + ", nodeId=" +
                                    nodeId + ']');
                            }
                        }

                        // Move to next job.
                        break;
                    }
                }

                assert stealReqs.get() >= 0 : "ASSERTION [line=791, file=src/java/org/gridgain/grid/spi/collision/jobstealing/GridJobStealingCollisionSpi.java]";
            }
            // No more jobs to steal or activate.
            else {
                break;
            }
        }

        return rejected;
    }

    /**
     * Check if the node is idle and steal as many jobs from other nodes
     * as possible.
     *
     * @param waitJobs Waiting jobs.
     * @param activeJobs Active jobs.
     */
    private void checkIdle(Collection<GridCollisionJobContext> waitJobs,
        Collection<GridCollisionJobContext> activeJobs) {
        // Check for overflow.
        int max = waitJobsThreshold + activeJobsThreshold < 0 ?
            Integer.MAX_VALUE : waitJobsThreshold + activeJobsThreshold;

        int jobsToSteal = max - (waitJobs.size() + activeJobs.size());

        if (log.isDebugEnabled() == true) {
            log.debug("Total number of jobs to be stolen: " + jobsToSteal);
        }

        if (jobsToSteal > 0) {
            int jobsLeft = jobsToSteal;

            GridNode next;

            int nodeCnt = getSpiContext().getRemoteNodes().size();

            int idx = 0;

            while (jobsLeft > 0 && idx++ < nodeCnt && (next = nodeQueue.poll()) != null) {
                int delta = 0;

                try {
                    // Remote node does not have attributes - do not still from it.
                    if (stealAttrs != null && stealAttrs.size() > 0) {
                        if (next.getAttributes() == null || GridUtils.containsAll(next.getAttributes(), stealAttrs) == false) {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Skip node as it does not have all attributes: " + next.getId());
                            }

                            continue;
                        }
                    }

                    MessageInfo msgInfo = sendMsgMap.get(next.getId());

                    if (msgInfo == null) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Failed to find message info for node: " + next.getId());
                        }

                        // Node left topology.
                        continue;
                    }

                    Integer waitThreshold = (Integer)next.getAttribute(createSpiAttributeName(
                        WAIT_JOBS_THRESHOLD_NODE_ATTR));

                    if (waitThreshold == null) {
                        log.error("Remote node is not configured with GridJobStealingCollisionSpi and jobs will not " +
                            "be stolen from it (you must stop it and update its configuration to use " +
                            "GridJobStealingCollisionSpi): " + next);

                        continue;
                    }

                    delta = next.getMetrics().getCurrentWaitingJobs() - waitThreshold;

                    if (log.isDebugEnabled() == true) {
                        log.debug("Maximum number of jobs to steal from node [jobsToSteal=" + delta + ", node=" +
                            next.getId() + ']');
                    }

                    // Nothing to steal from this node.
                    if (delta <= 0) {
                        continue;
                    }

                    synchronized (msgInfo) {
                        if (msgInfo.isExpired() == false && msgInfo.getJobsToSteal() > 0) {
                            // Count messages being waited for as present.
                            jobsLeft -= msgInfo.getJobsToSteal();

                            continue;
                        }

                        if (jobsLeft < delta) {
                            delta = jobsLeft;
                        }

                        jobsLeft -= delta;

                        msgInfo.reset(delta);
                    }

                    // Send request to remote node to still jobs.
                    // Message is a plain integer represented by 'delta'.
                    getSpiContext().sendMessage(next, delta, JOB_STEALING_COMM_TOPIC);
                }
                catch (GridSpiException e) {
                    log.error("Failed to send job stealing message to node: " + next, e);

                    // Rollback.
                    jobsLeft += delta;
                }
                finally {
                    // If node is alive, add back to the end of the queue.
                    if (getSpiContext().getNode(next.getId()) != null) {
                        nodeQueue.offer(next);
                    }
                }
            }
        }
    }

    /**
     *
     */
    private class MessageInfo {
        /** */
        private int jobsToSteal = 0;

        /** */
        private long timestamp = System.currentTimeMillis();

        /**
         *
         * @return Job to steal.
         */
        int getJobsToSteal() {
            return jobsToSteal;
        }

        /**
         *
         * @return Message send time.
         */
        long getTimestamp() {
            return timestamp;
        }

        /**
         *
         * @return <tt>True</tt> if message is expired.
         */
        boolean isExpired() {
            return jobsToSteal > 0 && System.currentTimeMillis() - timestamp >= msgExpireTime;
        }

        /**
         *
         * @param jobsToSteal Jobs to steal.
         */
        void setJobsToSteal(int jobsToSteal) {
            this.jobsToSteal = jobsToSteal;
        }

        /**
         *
         * @param jobsToSteal Jobs to steal.
         */
        void reset(int jobsToSteal) {
            this.jobsToSteal = jobsToSteal;

            timestamp = System.currentTimeMillis();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return GridToStringBuilder.toString(MessageInfo.class, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(2);

        attrs.add(createSpiAttributeName(MAX_STEALING_ATTEMPT_ATTR));
        attrs.add(createSpiAttributeName(MSG_EXPIRE_TIME));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobStealingCollisionSpi.class, this);
    }
}
