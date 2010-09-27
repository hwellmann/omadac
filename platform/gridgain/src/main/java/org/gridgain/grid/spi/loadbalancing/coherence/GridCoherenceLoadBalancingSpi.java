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

package org.gridgain.grid.spi.loadbalancing.coherence;

import com.tangosol.net.*;
import java.util.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.coherence.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.jsr305.*;

/**
 * Load balancing SPI which uses data affinity for routing jobs to remote nodes. It provides
 * ability to <b>collocate computations with data</b>. Coherence Cache provides partitioned
 * cache feature which allows you to segment your cached data across cluster. This SPI
 * delegates to Coherence Cache to find out which node is responsible for caching data
 * and routes a job to it.
 * <p>
 * Note, that instead of regular {@link GridJob}, this SPI expects {@link GridCoherenceAffinityJob}
 * which allows user to specify affinity key and cache name.
 * <p>
 * <h1 class="header">Coding Example</h1>
 * To use load balancers for your job routing, in your {@link GridTask#map(List, Object)}
 * implementation use load balancer to find out the node this job should be routed to
 * (see {@link GridLoadBalancerResource} documentation for information on how a load balancer
 * can be injected into your task). However, the preferred way here is to use
 * {@link GridTaskSplitAdapter}, as it will handle affinity assignment of jobs to nodes
 * automatically. Node that when working with affinity load balancing, your task's
 * <tt>map(..)</tt> or <tt>split(..)</tt> methods should return {@link GridCoherenceAffinityJob}
 * instances instead of {@link GridJob} ones. {@link GridCoherenceAffinityJob} adds two additional
 * methods to grid job: {@link GridCoherenceAffinityJob#getAffinityKey()} and
 * {@link GridCoherenceAffinityJob#getCacheName()} which will allow GridGain to
 * delegate routing to Coherence Cache, so jobs for the same cache with the same key will
 * be always routed to the same node. In case if regular
 * {@link GridJob} is returned, not the {@link GridCoherenceAffinityJob}, it will be routed
 * to a randomly picked node.
 * <p>
 * Here is an example of a grid task that uses affinity load balancing. Note how load balancing
 * jobs is absolutely transparent to the user and is simply a matter of proper grid configuration.
 * <pre name="code" class="java">
 * public class MyFooBarCoherenceAffinityTask extends GridTaskSplitAdapter&lt;List&lt;Integer&gt;,Object&gt; {
 *     // For this example we receive a list of cache keys and for every key
 *     // create a job that accesses it.
 *     &#64;Override
 *     protected Collection&lt;? extends GridJob&gt; split(int gridSize, List&lt;Integer&gt; cacheKeys) throws GridException {
 *         List&lt;MyGridAffinityJob&gt; jobs = new ArrayList&lt;MyGridAffinityJob&gt;(gridSize);
 *
 *         for (Integer cacheKey : cacheKeys) {
 *             jobs.add(new MyFooBarCoherenceAffinityJob(cacheKey));
 *         }
 *
 *         // Node assignment via load balancer
 *         // happens automatically.
 *         return jobs;
 *     }
 *     ...
 * }
 * </pre>
 * Here is the example of grid jobs created by the task above:
 * <pre name="code" class="java">
 * public class MyFooBarCoherenceAffinityJob extends GridCoherenceAffinityJobAdapter&lt;Integer, Serializable&gt; {
 *    ...
 *    private static final String CACHE_NAME = "myDistributedCache";
 *
 *    public MyFooBarCoherenceAffinityJob(Integer cacheKey) {
 *        super(CACHE_NAME, cacheKey);
 *    }
 *
 *    public Serializable execute() throws GridException {
 *        ...
 *        // Access data by the same key returned in 'getAffinityKey()' method
 *        // and for cache with name returned in 'getCacheName()'.
 *        NamedCache mycache = CacheFactory.getCache(getCacheName);
 *
 *        mycache.get(getAffinityKey());
 *        ...
 *    }
 * }
 * </pre>
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has no optional configuration parameters.
 * <p>
 * Below is a Java example of configuration for Coherence affinity load balancing SPI:
 * <pre name="code" class="java">
 * GridCoherenceLoadBalancingSpi spi = new GridCoherenceLoadBalancingSpi();
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
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.coherence.GridCoherenceLoadBalancingSpi"/&gt;
 * &lt;/property&gt;
 * </pre>
 * <p>
 * For more information, how to create and use Coherence distributed cache see
 * <a target=_blank href="http://wiki.tangosol.com/display/COH33UG/Partitioned+Cache+Service">Partitioned Cache Service</a>
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
public class GridCoherenceLoadBalancingSpi extends GridSpiAdapter implements GridLoadBalancingSpi,
    GridCoherenceLoadBalancingSpiMBean {
    /** Random number generator. */
    private Random rand = new Random();

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    private GridDiscoveryListener listener = null;

    /** Maps coherence members to grid nodes. */
    private Map<Integer, GridNode> gridNodes = new HashMap<Integer, GridNode>();

    /** */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * {@inheritDoc}
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException {
        startStopwatch();

        registerMBean(gridName, this, GridCoherenceLoadBalancingSpiMBean.class);

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        gridNodes = null;

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

        getSpiContext().addDiscoveryListener(listener = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                rwLock.writeLock().lock();

                try {
                    GridCoherenceMember gridMbr = (GridCoherenceMember)node.getAttribute(
                        GridCoherenceDiscoverySpi.ATTR_COHERENCE_MBR);

                    if (gridMbr == null) {
                        log.error("GridCoherenceLoadBalancingSpi can only be used with GridCoherenceDiscoverySpi.");

                        return;
                    }

                    switch (type) {
                        case JOINED: {
                            gridNodes.put(gridMbr.getId(), node);

                            if (log.isDebugEnabled() == true) {
                                log.debug("Added node: " + node);
                            }

                            break;
                        }

                        case FAILED:
                        case LEFT: {
                            gridNodes.remove(gridMbr.getId());

                            break;
                        }

                        case METRICS_UPDATED: {
                            break;
                        }

                        default: { assert false; }
                    }
                }
                finally {
                    rwLock.writeLock().unlock();
                }
            }
        });

        rwLock.writeLock().lock();

        try {
            for (GridNode node : getSpiContext().getRemoteNodes()) {
                GridCoherenceMember gridMbr = (GridCoherenceMember)node.getAttribute(
                    GridCoherenceDiscoverySpi.ATTR_COHERENCE_MBR);

                if (gridMbr == null) {
                    log.error("GridCoherenceLoadBalancingSpi can only be used with GridCoherenceDiscoverySpi.");

                    continue;
                }

                if (gridNodes.containsKey(gridMbr.getId()) == false) {
                    gridNodes.put(gridMbr.getId(), node);

                    if (log.isDebugEnabled() == true) {
                        log.debug("Added node: " + node);
                    }
                }
            }
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onContextDestroyed() {
        if (listener != null) {
            GridSpiContext ctx = getSpiContext();

            if (ctx != null) {
                ctx.removeDiscoveryListener(listener);
            }
        }

        super.onContextDestroyed();
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getBalancedNode(GridTaskSession ses, List<GridNode> top, GridJob job) throws GridException {
        GridArgumentCheck.checkNull(ses, "ses");
        GridArgumentCheck.checkNull(top, "top");
        GridArgumentCheck.checkNull(job, "job");

        if (job instanceof GridCoherenceAffinityJob) {
            GridCoherenceAffinityJob<?> affJob = (GridCoherenceAffinityJob<?>)job;

            Object key = affJob.getAffinityKey();

            if (key != null ) {
                // Look for node through of Coherence cache key.
                NamedCache cache = CacheFactory.getCache(affJob.getCacheName());

                if (cache == null) {
                    throw (GridException)new GridException("Failed to find cache for name: " + affJob.getCacheName()).setData(307, "src/java/org/gridgain/grid/spi/loadbalancing/coherence/GridCoherenceLoadBalancingSpi.java");
                }

                CacheService svc = cache.getCacheService();

                if (svc instanceof PartitionedService == false) {
                    throw (GridException)new GridException("Cache is not coherence 'partitioned' cache: " + affJob.getCacheName()).setData(313, "src/java/org/gridgain/grid/spi/loadbalancing/coherence/GridCoherenceLoadBalancingSpi.java");
                }

                Member mbr = ((PartitionedService)svc).getKeyOwner(key);

                if (mbr != null) {
                    GridNode resNode = null;

                    rwLock.readLock().lock();

                    try {
                        resNode = gridNodes.get(mbr.getId());

                        if (log.isDebugEnabled() == true) {
                            log.debug("Getting balanced node for data [key=" + key + ", gridNodesSize=" +
                                gridNodes.size() + ", mbr=" + new GridCoherenceMember(mbr) + ", node=" + resNode + ']');
                        }
                    }
                    finally {
                        rwLock.readLock().unlock();
                    }

                    if (resNode != null) {
                        if (log.isDebugEnabled() == true) {
                            log.debug("Picked coherence affinity node for specified cache key [cacheKey=" + key +
                                ", node=" + resNode + ']');
                        }

                        return resNode;
                    }
                    else if (log.isDebugEnabled() == true) {
                        log.debug("Coherence affinity key owner is not a member of task topology [coherenceMbr=" + mbr +
                            ", taskTopology=" + top + ']');
                    }
                }
            }
            else if (log.isDebugEnabled() == true) {
                log.debug("No cache key was passed to coherence load balancer (random node will be picked).");
            }
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Coherence affinity node could not be picked (random node will be used): " + job);
        }

        return top.get(rand.nextInt(top.size()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCoherenceLoadBalancingSpi.class, this);
    }
}
