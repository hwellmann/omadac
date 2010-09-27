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

package org.gridgain.grid.spi.loadbalancing.affinity;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.jsr305.*;

/**
 * Load balancing SPI which uses data affinity for routing jobs to remote nodes. It provides
 * ability to <b>collocate computations with data</b>. This SPI
 * is best used with distributed caches for which it is really important that computation
 * is routed exactly to the node on which data is cached. Many data cache schemes
 * can take advantage of this SPI, distributed, or invalidation based. The real value is
 * that you now can partition your database between data servers and hence load
 * the whole database into memory for faster access.
 * <p>
 * <h1 class="header">Architecture and Deployment</h1>
 * The diagram below illustrates the difference between using data grids without
 * and with GridGain. The left side shows execution flow without GridGain, in which
 * a remote data server is queried for data, the data is then delivered to caller (master)
 * node, which is faster than DB access, but results into unnecessary network traffic.
 * <p>
 * On the right hand side, you can see the value that GridGain brings to the picture. The
 * whole computation logic together with data access logic is brought to data server
 * for local execution. Assuming that serialization of computation logic is much lighter
 * than serializing data, the network traffic in this case is minimal. Also, your computation
 * may access data from both, Node 2 and Node 3. In this case, GridGain will split your
 * computation into logical jobs and route appropriate logical jobs to the corresponding
 * data servers to ensure that all computations still remain local. Now, if one of the
 * data server nodes crashes, your jobs will be automatically failed-over to other nodes,
 * which allows you to fail-over logic together with data (not just data fail-over provided
 * by data grids or distributed caches).
 * <p>
 * <center>
 * <img src="http://www.gridgain.com/images/affinity_white.png" alt="Data Partitioning and Affinity Map/Reduce"/>
 * </center>
 * <p>
 * <h1 class="header">Coding Example</h1>
 * To use load balancers for your job routing, in your {@link GridTask#map(List, Object)}
 * implementation use load balancer to find out the node this job should be routed to
 * (see {@link GridLoadBalancerResource} documentation for information on how a load balancer
 * can be injected into your task. However, the preferred way here is to use
 * {@link GridTaskSplitAdapter}, as it will handle affinity assignment of jobs to nodes
 * automatically. Node that when working with affinity load balancing, your task's
 * <tt>map(..)</tt> or <tt>split(..)</tt> methods should return {@link GridAffinityJob} instances
 * instead of {@link GridJob} ones. {@link GridAffinityJob} adds one additional
 * method to grid job: {@link GridAffinityJob#getAffinityKey()} which will allow GridGain to
 * properly route the job with the same key to the same grid node every time. In case if regular
 * {@link GridJob} is returned, not the {@link GridAffinityJob}, it will be routed to a randomly
 * picked node.
 * Here is an example of a grid task that uses affinity load balancing. Note how load balancing
 * jobs is absolutely transparent to the user and is simply a matter of proper grid configuration.
 * <pre name="code" class="java">
 * public class MyFooBarAffinityTask extends GridTaskSplitAdapter&lt;List&lt;Integer&gt;,Object&gt; {
 *    // For this example we receive a list of cache keys and for every key
 *    // create a job that accesses it.
 *    &#64;Override
 *    protected Collection&lt;? extends GridJob&gt; split(int gridSize, List&lt;Integer&gt; cacheKeys) throws GridException {
 *        List&lt;MyGridAffinityJob&gt; jobs = new ArrayList&lt;MyGridAffinityJob&gt;(gridSize);
 *
 *        for (Integer cacheKey : cacheKeys) {
 *            jobs.add(new MyGridAffinityJob(cacheKey));
 *        }
 *
 *        // Node assignment via load balancer
 *        // happens automatically.
 *        return jobs;
 *    }
 *    ...
 * }
 * </pre>
 * Here is the example of grid jobs created by the task above:
 * <pre name="code" class="java">
 * public class MyGridAffinityJob extends GridAffinityJobAdapter&lt;Integer, Serializable&gt; {
 *    public MyGridAffinityJob(Integer cacheKey) {
 *        // Pass cache key as a job argument.
 *        super(cacheKey);
 *    }
 *
 *    public Serializable execute() throws GridException {
 *        ...
 *        // Access data by the same key returned
 *        // in 'getAffinityKey()' method.
 *        mycache.get(getAffinityKey());
 *        ...
 *    }
 * }
 * </pre>
 * <p>
 * Also note that there may be cases where your underlying cache product supports multiple
 * caches and you need to cache data with identical keys on those caches. Although, it still
 * may be OK to return the same key from {@link GridAffinityJob#getAffinityKey()} method
 * for either cache, you may wish to change your affinity key method as follows to make
 * sure that affinity load balancing for one cache is independent from another:
 * <pre name="code" class="java">
 * public class MyFooBarAffintyJob extends GridAffinityJobAdapter&lt;String, Integer&gt; {
 *    public MyFooBarAffinity(String cacheName, Integer cacheKey) {
 *        // Construct affinity key by concatenating cache name
 *        // and affinity key. Note that we also pass cacheKey as
 *        // argument to access from execute method.
 *        super(cacheName + '.' + cacheKey, cacheKey);
 *    }
 *
 *    &#64;Override
 *    pubic Serializable execute() {
 *       ...
 *       // Access data from your cache by the cache key.
 *       // The main point to note here is that the same
 *       // affinity key always corresponds to the same
 *       // cache key.
 *       Integer cacheKey = getArgument();
 *
 *       Object data = someCache.get(cacheKey);
 *       ...
 *       // Do computations.
 *    }
 * }
 * </pre>
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>Virtual node count (see {@link #setVirtualNodeCount(int)}).</li>
 * <li>Affinity node attributes (see {@link #setAffinityNodeAttributes(Map)}).</li>
 * <li>Affinity seed (see {@link #setAffinitySeed(String)}).</li>
 * </ul>
 * Please pay specific attention to the number of virtual nodes for Consistent Hashing algorithm
 * (see {@link #setVirtualNodeCount(int)}). The larger the virtual node count, the more
 * even the data distribution is across nodes. For best affinity distribution the value
 * should usually be larger than <tt>500</tt>. The default value of <tt>1000</tt>
 * is good enough for most grid deployments. If you set the value
 * too large (larger than several thousands), it may cause performance degradation.
 * Consistent Hashing algorithm generally yields between
 * 2% and 4% standard deviation for equal data affinity distribution.
 * <p>
 * You can use virtual node count to distribute load in uneven grid. Since the larger the
 * virtual node count is, the more data will be stored on that node (which leads to more jobs
 * sent to that node), <i>nodes that have higher Memory or CPU capacity should have larger
 * virtual node count value</i>.
 * <p>
 * When configuring virtual node count, it is common to assign a certain number of virtual
 * nodes to a single unit of capacity characteristic. For example, if you have 3 nodes in
 * the grid: N1, N2, and N3, and nodes N1 and N2 have 2GB of memory and node N3 has 3GB of memory,
 * then, to ease up calculations, for every 1GB of memory on a node you could assign 500 virtual nodes.
 * As a result, nodes N1 and N2 should be assigned <tt>1000</tt> virtual nodes and node N3 should
 * be assigned <tt>1500</tt> virtual nodes.
 * <p>
 * Below is a Java example of configuration for Affinity load balancing SPI:
 * GridAffinityLoadBalancingSpi spi = new GridAffnityLoadBalancingSpi();
 * <pre name="code" class="java">
 * GridAffinityLoadBalancingSpi spi = new GridAffnityLoadBalancingSpi();
 *
 * // Change number of virtual nodes.
 * spi.setVirtualNodeCount(1500);
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default load balancing SPI.
 * cfg.setLoadBalancingSpi(spi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * Here is Spring XML configuration example:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi">
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.affinity.GridAffinityLoadBalancingSpi"&gt;
 *         &lt;property name="virtualNodeCount" value="1500"/&gt;
 *
 *         &lt;!--
 *             If your grid is segmented via node attributes,
 *             then provide all attributes a node should have
 *             in order to be considered by affinity load balancer.
 *         --&gt;
 *         &lt;property name="affinityNodeAttributes"&gt;
 *             &lt;map&gt;
 *                 &lt;entry key="node.segment" value="foobar"/&gt;
 *             &lt;/map&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * The implementation utilizes Consistent Hashing algorithm that is best documented in
 * <a href="http://weblogs.java.net/blog/tomwhite/archive/2007/11/consistent_hash.html">Tom White's Blog</a>
 * (we modified the algorithm to fit better into GridGain).
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
public class GridAffinityLoadBalancingSpi extends GridSpiAdapter implements GridLoadBalancingSpi,
    GridAffinityLoadBalancingSpiMBean {
    /** Default virtual node count for Consistent Hashing algorithm (value is <tt>1000</tt>). */
    public static final int DFLT_VIRTUAL_NODE_COUNT = 1000;

    /** Name of node attribute to specify number of replicas for a node. */
    public static final String VIRTUAL_NODE_COUNT_ATTR_NAME = "gridgain:affinity:node:replicas";

    /** Recommended maximum node weight. */
    public static final int RECOMMENDED_MAX_NODE_WEIGHT = 10;

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** Node replicas configuration parameter. */
    private int virtualNodeCnt = DFLT_VIRTUAL_NODE_COUNT;

    /** Affinity seed. */
    private String affSeed = "";

    /** Named attributes. */
    private Map<String, ? extends Serializable> affAttrs = null;

    /** Affinity. */
    private GridAffinity aff = null;

    /** Task topologies. First pair value indicates whether or not jobs have been mapped. */
    private ConcurrentMap<UUID, GridPair<Boolean, Set<GridNode>>> taskTops =
        new ConcurrentHashMap<UUID, GridPair<Boolean, Set<GridNode>>>();

    /** Node discovery listener. */
    private GridDiscoveryListener discoListener = null;

    /** Local event listener to listen to task completion events. */
    private GridLocalEventListener evtListener = null;

    /** Random number generator. */
    private Random rand = new Random();

    /** Read-write lock. */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * {@inheritDoc}
     */
    public int getVirtualNodeCount() {
        return virtualNodeCnt;
    }

    /**
     * Sets number of virtual nodes for Consistent Hashing algorithm. Generally, the larger
     * the virtual node count, the more even the data affinity distribution is across nodes.
     * The value should generally be larger than <tt>500</tt>. The default value of <tt>1000</tt>
     * is good enough for most grid deployments. Consistent Hashing generally yields between
     * 2% and 4% standard deviation for equal data affinity distribution. If you set the value
     * too large (larger than several thousands), it may cause performance degradation
     * <p>
     * You can use virtual node count to distribute load in uneven grid. The larger the
     * virtual node count is, the more data will be stored on that node which leads to more jobs
     * sent to that node. Hence, nodes that have higher Memory or CPU capacity should have larger
     * virtual node count value.
     * <p>
     * For example, if you have 3 nodes in the grid: N1, N2, and N3, and nodes N1 and N2 have 2GB of
     * memory and node N3 has 3GB of memory, then, to ease up calculations, for every 1GB of memory
     * on a node you could assign 500 virtual nodes. As a result, nodes N1 and N2 should be assigned
     * <tt>1000</tt> virtual nodes and node N3 should be assigned <tt>1500</tt> virtual nodes.
     *
     * @param virtualNodeCnt Weight of the node.
     */
    @GridSpiConfiguration(optional = true)
    public void setVirtualNodeCount(int virtualNodeCnt) {
        this.virtualNodeCnt = virtualNodeCnt;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, ? extends Serializable> getAffinityNodeAttributes() {
        return affAttrs;
    }

    /**
     * Sets node attributes for data affinity grid segment. All nodes that want to participate
     * in affinity load balancing should have these attributes. This is useful when grid is
     * segmented, for example, into clients and servers, and client nodes will never cache any data.
     * <p>
     * Default value is <tt>null</tt>, which means that all nodes will
     * be included.
     *
     * @param affAttrs Map of node attributes for affinity load balancing.
     */
    @GridSpiConfiguration(optional = true)
    public void setAffinityNodeAttributes(Map<String, ? extends Serializable> affAttrs) {
        this.affAttrs = affAttrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(VIRTUAL_NODE_COUNT_ATTR_NAME), virtualNodeCnt);
    }

    /**
     * {@inheritDoc}
     */
    public String getAffinitySeed() {
        return affSeed;
    }

    /**
     * Sets affinity seed used by Conistent Hashing algorithm.
     * By default this seed is empty string.
     * <p>
     * Whenever starting multiple instances of this SPI, you should make
     * sure that every instance has a different seed to achieve different
     * affinity assignment. Otherwise, affinity assignment for different
     * instances of this SPI will be identical, which defeats the purpose
     * of starting multiple affinity load balancing SPI's altogether.
     * <p>
     * <b>Note that affinity seed must be identical for corresponding
     * instances of this SPI on all nodes.</b> If this is not the case,
     * then different nodes will calculate affinity differently which may
     * result in multiple nodes responsible for the same affinity key.
     *
     * @param affSeed Non-null value for affinity seed.
     */
    public void setAffinitySeed(String affSeed) {
        this.affSeed = affSeed;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException {
        startStopwatch();

        assertParameter(affSeed != null, "affinitySeed != null");
        assertParameter(virtualNodeCnt > 0, "virtualNodeCount > 0");

        if (log.isInfoEnabled() == true) {
            log.info(configInfo("virtualNodeCount", virtualNodeCnt));
            log.info(configInfo("affinityNodeAttributes", affAttrs));
            log.info(configInfo("affinitySeed", affSeed));
        }

        if (virtualNodeCnt < 500) {
            log.warning("'virtualNodeCount' is less than recommended minimum value of '500' " +
                "(this may result in less than ideal affinity distribution: " + virtualNodeCnt);
        }

        aff = new GridAffinity(affSeed, log);

        registerMBean(gridName, this, GridAffinityLoadBalancingSpiMBean.class);

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        aff = null;

        taskTops.clear();

        unregisterMBean();

        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onContextInitialized(GridSpiContext spiCtx) throws GridSpiException {
        super.onContextInitialized(spiCtx);

        getSpiContext().addDiscoveryListener(discoListener = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                rwLock.writeLock().lock();

                try {
                    if (isNodeIncluded(node) == true) {
                        Integer replicas = (Integer)node.getAttribute(createSpiAttributeName(
                            VIRTUAL_NODE_COUNT_ATTR_NAME));

                        boolean hasAttr = replicas != null;

                        if (replicas == null) {
                            replicas = DFLT_VIRTUAL_NODE_COUNT;
                        }

                        switch (type) {
                            case JOINED: {
                                aff.add(node, replicas);

                                if (hasAttr == false) {
                                    log.info("Remote node does not have \"gridgain:affinity:node:replicas\" configured."
                                        + "Will use default value " + DFLT_VIRTUAL_NODE_COUNT + " for node="
                                        + node + ']');
                                }

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Added new node to affinity: " + node);
                                }

                                break;
                            }

                            case LEFT:
                            case FAILED: {
                                aff.remove(node, replicas);

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Removed node from affinity: " + node);
                                }

                                break;
                            }

                            case METRICS_UPDATED: {
                                if (log.isDebugEnabled() == true) {
                                    log.debug("Received metrics update event for node: " + node);
                                }

                                break;
                            }

                            default: { assert false; } // Should never reach here.
                        }
                    }
                }
                finally {
                    rwLock.writeLock().unlock();
                }
            }
        });

        rwLock.writeLock().lock();

        try {
            for (GridNode node : getSpiContext().getAllNodes()) {
                if (isNodeIncluded(node) == true) {
                    Integer replicas = (Integer)node.getAttribute(createSpiAttributeName(VIRTUAL_NODE_COUNT_ATTR_NAME));

                    if (replicas != null) {
                        aff.add(node, replicas);
                    }
                    else {
                        aff.add(node, DFLT_VIRTUAL_NODE_COUNT);
                    }

                    if (log.isDebugEnabled() == true) {
                        log.debug("Added new node to affinity: " + node);
                    }
                }
            }
        }
        finally {
            rwLock.writeLock().unlock();
        }

        getSpiContext().addLocalEventListener(evtListener = new GridLocalEventListener() {
            /**
             * {@inheritDoc}
             */
            public void onEvent(GridEvent evt) {
                if (evt.getType() == GridEventType.TASK_FINISHED || evt.getType() == GridEventType.TASK_FAILED) {
                    taskTops.remove(evt.getTaskSessionId());

                    if (log.isDebugEnabled() == true) {
                        log.debug("Removed task topology from topology cache for session: " + evt.getTaskSessionId());
                    }
                }
                // We should keep topology and use cache in GridTask#map() method to
                // avoid O(n*n/2) complexity, after that we can drop caches.
                // Here we set mapped property and later cache will be ignored
                else if (evt.getType() == GridEventType.JOB_MAPPED) {
                    GridPair<Boolean, Set<GridNode>> top = taskTops.get(evt.getTaskSessionId());

                    if (top != null) {
                        top.setValue1(true);

                        // Remove the cached topology.
                        top.setValue2(null);
                    }

                    if (log.isDebugEnabled() == true) {
                        log.debug("Job has been mapped. Ignore cache for session: " + evt.getTaskSessionId());
                    }
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onContextDestroyed() {
        if (discoListener != null) {
            getSpiContext().removeDiscoveryListener(discoListener);
        }

        if (evtListener != null) {
            GridSpiContext ctx = getSpiContext();

            if (ctx != null) {
                ctx.removeLocalEventListener(evtListener);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getBalancedNode(GridTaskSession ses, List<GridNode> top, GridJob job) throws GridException {
        GridArgumentCheck.checkNull(ses, "ses");
        GridArgumentCheck.checkNull(top, "top");
        GridArgumentCheck.checkNull(job, "job");

        GridArgumentCheck.checkRange(top.isEmpty() == false, "top.isEmpty() == false");

        if (job instanceof GridAffinityJob) {
            GridPair<Boolean, Set<GridNode>> topSet = taskTops.get(ses.getId());

            Set<GridNode> nodes = null;

            // Create new cached topology if there is no one. Do not
            // use cached topology after task has been mapped.
            if (topSet == null) {
                nodes = new HashSet<GridNode>(top);

                // There is no topology - assume that task has not been mapped.
                taskTops.put(ses.getId(), new GridPair<Boolean, Set<GridNode>>(false, nodes));
            }
            // If during map operation, use cached value,
            // as topology could not have possibly changed.
            else if (topSet.getValue1() == false) {
                nodes = topSet.getValue2();
            }
            // Map operation has already been done,
            // so this is most likely fail-over.
            else {
                nodes = new HashSet<GridNode>(top);
            }

            Object key = ((GridAffinityJob<?>)job).getAffinityKey();

            if (key == null) {
                throw (GridException)new GridException("Affinity job returned null key: " + job).setData(594, "src/java/org/gridgain/grid/spi/loadbalancing/affinity/GridAffinityLoadBalancingSpi.java");
            }

            GridNode node = aff.get(key, nodes);

            if (node != null) {
                return node;
            }

            log.warning("Failed to find affinity node for affinity job most likely due to misconfiguration of " +
                "'affinityNodeAttributes' configuration parameter (will pick random node instead) [job=" + job +
                "top=" + top + ']');
        }
        else if (log.isDebugEnabled() == true) {
            log.debug("Affinity load balancer cannot balance non-affinity job (will pick random node instead): " + job);
        }

        // For non-affinity jobs, pick random node.
        return top.get(rand.nextInt(top.size()));
    }

    /**
     * Checks if node is eligible for affinity load balancing.
     *
     * @param node Node to check.
     * @return <tt>True</tt> if node should be load-balanced via affinity.
     */
    private boolean isNodeIncluded(GridNode node) {
        Map<String, Serializable> nodeAttrs = node.getAttributes();

        if (nodeAttrs != null && affAttrs != null) {
            if (GridUtils.containsAll(nodeAttrs, affAttrs) == false) {
                return false;
            }
        }
        else if (nodeAttrs == null && affAttrs != null) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(2);

        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));
        attrs.add(createSpiAttributeName(VIRTUAL_NODE_COUNT_ATTR_NAME));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridAffinityLoadBalancingSpi.class, this);
    }
}
