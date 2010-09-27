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

package org.gridgain.grid.spi.loadbalancing.adaptive;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.jsr305.*;

/**
 * Load balancing SPI that adapts to overall node performance. It
 * proportionally distributes more jobs to more performant nodes based
 * on a pluggable and dynamic node load probing.
 * <p>
 * <h1 class="header">Adaptive Node Probe</h1>
 * This SPI comes with pluggable algorithm to calculate a node load
 * at any given point of time. The algorithm is defined by
 * {@link GridAdaptiveLoadProbe} interface and user is
 * free to provide custom implementations. By default
 * {@link GridAdaptiveCpuLoadProbe} implementation is used
 * which distributes jobs to nodes based on average CPU load
 * on every node.
 * <p>
 * The following load probes are available with the product:
 * <ul>
 * <li>{@link GridAdaptiveCpuLoadProbe} - default</li>
 * <li>{@link GridAdaptiveBenchmarkLoadProbe}</li>
 * <li>{@link GridAdaptiveProcessingTimeLoadProbe}</li>
 * <li>{@link GridAdaptiveJobCountLoadProbe}</li>
 * </ul>
 * Note that if {@link GridAdaptiveLoadProbe#getLoad(GridNode, int)} returns a value of <tt>0</tt>,
 * then implementation will assume that load value is simply not available and
 * will try to calculate an average of load values for other nodes. If such
 * average cannot be obtained (all node load values are <tt>0</tt>), then a value
 * of <tt>1</tt> will be used.
 * <p>
 * When working with node metrics, take into account that all averages are
 * calculated over metrics history size defined by {@link GridConfiguration#getMetricsExpireTime()}
 * and {@link GridConfiguration#getMetricsHistorySize()} grid configuration parameters.
 * Generally the larger these configuration parameter values are, the more precise the metrics are.
 * You should tune these values based on the level of accuracy needed vs. the additional memory
 * that would be required for storing metrics.
 * <p>
 * You should also keep in mind that metrics for remote nodes are delayed (usually by the
 * heartbeat frequency). So if it is acceptable in your environment, set the heartbeat frequency
 * to be more inline with job execution time. Generally, the more often heartbeats between nodes
 * are exchanged, the more precise the metrics are. However, you should keep in mind that if
 * heartbeats are exchanged too often then it may create unnecessary traffic in the network.
 * Heartbeats (or metrics update frequency) can be configured via underlying
 * {@link GridDiscoverySpi} used in your grid.
 * <p>
 * Here is an example of how probing can be implemented to use
 * number of active and waiting jobs as probing mechanism:
 * <pre name="code" class="java">
 * public class FooBarLoadProbe implements GridAdaptiveLoadProbe {
 *     // Flag indicating whether to use average value or current.
 *     private int useAvg = true;
 *
 *     public FooBarLoadProbe(boolean useAvg) {
 *         this.useAvg = useAvg;
 *     }
 *
 *     // Calculate load based on number of active and waiting jobs.
 *     public double getLoad(GridNode node, int jobsSentSinceLastUpdate) {
 *         GridNodeMetrics metrics = node.getMetrics();
 *
 *         if (useAvg == true) {
 *             double load = metrics.getAverageActiveJobs() + metrics.getAverageWaitingJobs();
 *
 *             if (load > 0) {
 *                 return load;
 *             }
 *         }
 *
 *         return metrics.getCurrentActiveJobs() + metrics.getCurrentWaitingJobs();
 *     }
 * }
 * </pre>
 * <h1 class="header">Which Node Probe To Use</h1>
 * There is no correct answer here. Every single node probe will work better or worse in
 * different environments. CPU load probe (default option) is the safest approach to start
 * with as it simply attempts to utilize every CPU on the grid to the maximum. However, you should
 * experiment with other probes by executing load tests in your environment and observing
 * which probe gives you best performance and load balancing.
 * <p>
 * <h1 class="header">Task Coding Example</h1>
 * If you are using {@link GridTaskSplitAdapter} then load balancing logic
 * is transparent to your code and is handled automatically by the adapter.
 * Here is an example of how your task will look:
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridTaskSplitAdapter&lt;Object, Object&gt; {
 *    &#64;Override
 *    protected Collection&lt;? extends GridJob&gt; split(int gridSize, Object arg) throws GridException {
 *        List&lt;MyFooBarJob&gt; jobs = new ArrayList&lt;MyFooBarJob&gt;(gridSize);
 *
 *        for (int i = 0; i &lt; gridSize; i++) {
 *            jobs.add(new MyFooBarJob(arg));
 *        }
 *
 *        // Node assignment via load balancer
 *        // happens automatically.
 *        return jobs;
 *    }
 *    ...
 * }
 * </pre>
 * If you need more fine-grained control over how some jobs within task get mapped to a node
 * and use affinity load balancing for some other jobs within task, then you should use
 * {@link GridTaskAdapter}. Here is an example of how your task will look. Note that in this
 * case we manually inject load balancer and use it to pick the best node. Doing it in
 * such way would allow user to map some jobs manually and for others use load balancer.
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridTaskAdapter&lt;String, String&gt; {
 *    // Inject load balancer.
 *    &#64;GridLoadBalancerResource
 *    GridLoadBalancer balancer;
 *
 *    // Map jobs to grid nodes.
 *    public Map&lt;? extends GridJob, GridNode&gt; map(List&lt;GridNode&gt; subgrid, String arg) throws GridException {
 *        Map&lt;MyFooBarJob, GridNode&gt; jobs = new HashMap&lt;MyFooBarJob, GridNode&gt;(subgrid.size());
 *
 *        // In more complex cases, you can actually do
 *        // more complicated assignments of jobs to nodes.
 *        for (int i = 0; i &lt; subgrid.size(); i++) {
 *            // Pick the next best balanced node for the job.
 *            jobs.put(new MyFooBarJob(arg), balancer.getBalancedNode())
 *        }
 *
 *        return jobs;
 *    }
 *
 *    // Aggregate results into one compound result.
 *    public String reduce(List&lt;GridJobResult&gt; results) throws GridException {
 *        // For the purpose of this example we simply
 *        // concatenate string representation of every
 *        // job result
 *        StringBuilder buf = new StringBuilder();
 *
 *        for (GridJobResult res : results) {
 *            // Append string representation of result
 *            // returned by every job.
 *            buf.append(res.getData().toString());
 *        }
 *
 *        return buf.toString();
 *    }
 * }
 * </pre>
 * <p>
 * <h1 class="header">Configuration</h1>
 * In order to use this load balancer, you should configure your grid instance
 * to use <tt>GridJobsLoadBalancingSpi</tt> either from Spring XML file or
 * directly. The following configuration parameters are supported:
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has the following optional configuration parameters:
 * <ul>
 * <li>
 *      Adaptive node load probing implementation (see {@link #setLoadProbe(GridAdaptiveLoadProbe)}).
 *      This configuration parameter supplies a custom algorithm for probing a node's load.
 *      By default, {@link GridAdaptiveCpuLoadProbe} implementation is used which
 *      takes every node's CPU load and tries to send proportionally more jobs to less loaded nodes.
 * </li>
 * </ul>
 * <p>
 * Below is Java configuration example:
 * <pre name="code" class="java">
 * GridAdaptiveLoadBalancingSpi spi = new GridAdaptiveLoadBalancingSpi();
 *
 * // Configure probe to use latest job execution time vs. average.
 * GridAdaptiveProcessingTimeLoadProbe probe = new GridAdaptiveProcessingTimeLoadProbe(false);
 *
 * spi.setLoadProbe(probe);
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default load balancing SPI.
 * cfg.setLoadBalancingSpi(spi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * Here is how you can configure <tt>GridJobsLoadBalancingSpi</tt> using Spring XML configuration:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveLoadBalancingSpi"&gt;
 *         &lt;property name="loadProbe"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.loadbalancing.adaptive.GridAdaptiveProcessingTimeLoadProbe"&gt;
 *                 &lt;constructor-arg value="false"/&gt;
 *             &lt;/bean&gt;
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
public class GridAdaptiveLoadBalancingSpi extends GridSpiAdapter implements GridLoadBalancingSpi,
    GridAdaptiveLoadBalancingSpiMBean {
    /** Random number generator. */
    private static final Random RAND = new Random();

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    private GridAdaptiveLoadProbe probe = new GridAdaptiveCpuLoadProbe();

    /** Local event listener to listen to task completion events. */
    private GridLocalEventListener evtListener = null;

    /** */
    private GridDiscoveryListener discoListener = null;

    /** Task topologies. First pair value indicates whether or not jobs have been mapped. */
    private ConcurrentMap<UUID, GridPair<Boolean, WeightedTopology>> taskTops =
        new ConcurrentHashMap<UUID, GridPair<Boolean, WeightedTopology>>();

    /** */
    private final Map<GridNode, AtomicInteger> nodeJobs = new HashMap<GridNode, AtomicInteger>();

    /** */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * {@inheritDoc}
     */
    public String getLoadProbeFormatted() {
        return probe.toString();
    }

    /**
     * Sets implementation of node load probe. By default {@link GridAdaptiveProcessingTimeLoadProbe}
     * is used which proportionally distributes load based on the average job execution
     * time on every node.
     *
     * @param probe Implementation of node load probe
     */
    @GridSpiConfiguration(optional = true)
    public void setLoadProbe(GridAdaptiveLoadProbe probe) {
        GridArgumentCheck.checkNull(probe, "probe");

        this.probe = probe;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException {
        startStopwatch();

        assertParameter(probe != null, "loadProbe != null");

        if (log.isInfoEnabled()) {
            log.info(configInfo("loadProbe", probe));
        }

        registerMBean(gridName, this, GridAdaptiveLoadBalancingSpiMBean.class);

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        rwLock.writeLock().lock();

        try {
            nodeJobs.clear();
        }
        finally {
            rwLock.writeLock().unlock();
        }

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
                if (evt.getType() == GridEventType.JOB_MAPPED) {
                    GridPair<Boolean, WeightedTopology> weightedTop = taskTops.get(evt.getTaskSessionId());

                    if (weightedTop != null) {
                        weightedTop.setValue1(true);
                    }

                    if (log.isDebugEnabled() == true) {
                        log.debug("Job has been mapped. Ignore cache for session: " + evt.getTaskSessionId());
                    }
                }
            }
        });

        getSpiContext().addDiscoveryListener(discoListener = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                rwLock.writeLock().lock();

                try {
                    switch (type) {
                        case JOINED: {
                            nodeJobs.put(node, new AtomicInteger(0));

                            break;
                        }

                        case FAILED:
                        case LEFT: {
                            nodeJobs.remove(node);

                            break;
                        }

                        case METRICS_UPDATED: {
                            // Reset counter.
                            nodeJobs.put(node, new AtomicInteger(0));

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

        // Put all known nodes.
        rwLock.writeLock().lock();

        try {
            for (GridNode node : getSpiContext().getAllNodes()) {
                nodeJobs.put(node, new AtomicInteger(0));
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

        GridPair<Boolean, WeightedTopology> weightedTop = taskTops.get(ses.getId());

        // Create new cached topology if there is no one. Do not
        // use cached topology after task has been mapped.
        if (weightedTop == null) {
            // Called from GridTask#map(). Put new topology and false as not mapped yet.
            taskTops.put(ses.getId(), weightedTop = new GridPair<Boolean, WeightedTopology>(false,
                new WeightedTopology(top)));
        }
        // We have topology - check if task has been mapped.
        else if (weightedTop.getValue1() == true) {
            // Do not use cache after GridTask#map().
            return new WeightedTopology(top).pickWeightedNode();
        }

        return weightedTop.getValue2().pickWeightedNode();
    }

    /**
     * Calculates node load based on set probe.
     *
     * @param top List of all nodes.
     * @param node Node to get load for.
     * @return Node load.
     * @throws GridException If returned load is negative.
     */
    private double getLoad(List<GridNode> top, GridNode node) throws GridException {
        assert top.size() != 0 : "ASSERTION [line=467, file=src/java/org/gridgain/grid/spi/loadbalancing/adaptive/GridAdaptiveLoadBalancingSpi.java]";

        int jobsSentSinceLastUpdate = 0;

        rwLock.readLock().lock();

        try {
            AtomicInteger cnt = nodeJobs.get(node);

            jobsSentSinceLastUpdate = cnt == null ? 0 : cnt.get();
        }
        finally {
            rwLock.readLock().unlock();
        }

        double load = probe.getLoad(node, jobsSentSinceLastUpdate);

        if (load < 0) {
            throw (GridException)new GridException("Failed to obtain non-negative load from adaptive load probe: " + load).setData(485, "src/java/org/gridgain/grid/spi/loadbalancing/adaptive/GridAdaptiveLoadBalancingSpi.java");
        }

        return load;
    }

    /**
     * Holder for weighted topology.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class WeightedTopology {
        /** Topology sorted by weight. */
        private final SortedMap<Double, GridNode> circle = new TreeMap<Double, GridNode>();

        /**
         *
         * @param top Task topology.
         * @throws GridException If any load was negative.
         */
        WeightedTopology(List<GridNode> top) throws GridException {
            assert top != null : "ASSERTION [line=507, file=src/java/org/gridgain/grid/spi/loadbalancing/adaptive/GridAdaptiveLoadBalancingSpi.java]";
            assert top.isEmpty() == false : "ASSERTION [line=508, file=src/java/org/gridgain/grid/spi/loadbalancing/adaptive/GridAdaptiveLoadBalancingSpi.java]";

            double totalLoad = 0;

            // We need to cache loads here to avoid calls later as load might be
            // changed between the calls.
            double[] nums = new double[top.size()];

            int zeroCnt = 0;

            // Compute loads.
            for (int i = 0; i < top.size(); i++) {
                double load = getLoad(top, top.get(i));

                nums[i] = load;

                if (load == 0) {
                    zeroCnt++;
                }

                totalLoad += load;
            }

            // Take care of zero loads.
            if (zeroCnt > 0) {
                double newTotal = totalLoad;

                int nonZeroCnt = top.size() - zeroCnt;

                for (int i = 0; i < nums.length; i++) {
                    double load = nums[i];

                    if (load == 0) {
                        if (nonZeroCnt > 0) {
                            load = totalLoad / nonZeroCnt;
                        }

                        if (load == 0) {
                            load = 1;
                        }

                        nums[i] = load;

                        newTotal += load;
                    }
                }

                totalLoad = newTotal;
            }

            double totalWeight = 0;

            // Calculate weights and total weight.
            for (int i = 0; i < nums.length; i++) {
                assert nums[i] > 0 : "ASSERTION [line=562, file=src/java/org/gridgain/grid/spi/loadbalancing/adaptive/GridAdaptiveLoadBalancingSpi.java]. " + "Invalid load: " + nums[i];

                double weight = totalLoad / nums[i];

                // Convert to weight.
                nums[i] = weight;

                totalWeight += weight;
            }

            double weight = 0;

            // Enforce range from 0 to 1.
            for (int i = 0; i < nums.length; i++) {
                weight = i == nums.length - 1 ? 1.0d : weight + nums[i] / totalWeight;

                assert weight < 2 : "ASSERTION [line=578, file=src/java/org/gridgain/grid/spi/loadbalancing/adaptive/GridAdaptiveLoadBalancingSpi.java]. " + "Invalid weight: " + weight;

                // Complexity of this put is O(logN).
                circle.put(weight, top.get(i));
            }
        }

        /**
         * Gets weighted node in random fashion.
         *
         * @return Weighted node.
         */
        GridNode pickWeightedNode() {
            double weight = RAND.nextDouble();

            SortedMap<Double, GridNode> pick = circle.tailMap(weight);

            GridNode node = pick.get(pick.firstKey());

            rwLock.readLock().lock();

            try {
                AtomicInteger cnt = nodeJobs.get(node);

                if (cnt != null) {
                    cnt.incrementAndGet();
                }
            }
            finally {
                rwLock.readLock().unlock();
            }

            return node;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridAdaptiveLoadBalancingSpi.class, this);
    }
}
