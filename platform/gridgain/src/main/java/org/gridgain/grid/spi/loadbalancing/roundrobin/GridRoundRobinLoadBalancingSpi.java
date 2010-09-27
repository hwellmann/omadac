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

package org.gridgain.grid.spi.loadbalancing.roundrobin;

import java.util.*;
import java.util.concurrent.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.jsr305.*;

/**
 * This SPI iterates through nodes in round-robin fashion and pick the next
 * sequential node. Two modes of operation are supported: per-task and global
 * (see {@link #setPerTask(boolean)} configuration).
 * <p>
 * When configured in per-task mode, implementation will pick a random starting
 * node at the beginning of every task execution and then sequentially iterate through all
 * nodes in topology starting from the picked node. This is the default configuration
 * and should fit most of the use cases as it provides a fairly well-distributed
 * split and also ensures that jobs within a single task are spread out across
 * nodes to the maximum. For cases when split size is equal to the number of nodes,
 * this mode guarantees that all nodes will participate in the split.
 * <p>
 * When configured in global mode, a single sequential queue of nodes is maintained for
 * all tasks and the next node in the queue is picked every time. In this mode (unlike in
 * <tt>per-task</tt> mode) it is possible that even if split size may be equal to the
 * number of nodes, some jobs within the same task will be assigned to the same node if
 * multiple tasks are executing concurrently.
 * <h1 class="header">Coding Example</h1>
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
 * to use <tt>GridRoundRobinLoadBalancingSpi</tt> either from Spring XML file or
 * directly. The following configuration parameters are supported:
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>
 *      Flag that indicates whether to use <tt>per-task</tt> or global
 *      round-robin modes described above (see {@link #setPerTask(boolean)}).
 * </li>
 * </ul>
 * Below is Java configuration example:
 * <pre name="code" class="java">
 * GridRandomLoadBalancingSpi = new GridRandomLoadBalancingSpi();
 *
 * // Configure SPI to use global round-robin mode.
 * spi.setPerTask(false);
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default load balancing SPI.
 * cfg.setLoadBalancingSpi(spi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * Here is how you can configure <tt>GridRandomLoadBalancingSpi</tt> using Spring XML configuration:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="org.gridgain.grid.spi.loadbalancing.roundrobin.GridRoundRobinLoadBalancingSpi"&gt;
 *         &lt;!-- Set to global round-robin mode. --&gt;
 *         &lt;property name="perTask" value="false"/&gt;
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
public class GridRoundRobinLoadBalancingSpi extends GridSpiAdapter implements GridLoadBalancingSpi,
    GridRoundRobinLoadBalancingSpiMBean {
    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    private GridRoundRobinGlobalLoadBalancer balancer = null;

    /** */
    private boolean isPerTask = true;

    /** */
    private final Map<UUID, GridRoundRobinPerTaskLoadBalancer> perTaskBalancers =
        new ConcurrentHashMap<UUID, GridRoundRobinPerTaskLoadBalancer>();

    /** Event listener. */
    private final GridLocalEventListener evtListener = new GridLocalEventListener() {
        /**
         * {@inheritDoc}
         */
        public void onEvent(GridEvent evt) {
            if (evt.getType() == GridEventType.TASK_FAILED ||
                evt.getType() == GridEventType.TASK_FINISHED) {
                perTaskBalancers.remove(evt.getTaskSessionId());
            }
            else if (evt.getType() == GridEventType.JOB_MAPPED){
                GridRoundRobinPerTaskLoadBalancer balancer = perTaskBalancers.get(evt.getTaskSessionId());
                
                if (balancer != null) {
                    balancer.onMapped();
                }
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    public boolean isPerTask() {
        return isPerTask;
    }

    /**
     * Configuration parameter indicating whether a new round robin order should be
     * created for every task. If <tt>true</tt> then load balancer is guaranteed
     * to iterate through nodes sequentially for every task - so as long as number
     * of jobs is less than or equal to the number of nodes, jobs are guaranteed to
     * be assigned to unique nodes. If <tt>false</tt> then one round-robin order
     * will be maintained for all tasks, so when tasks execute concurrently, it
     * is possible for more than one job within task to be assigned to the same
     * node.
     * <p>
     * Default is <tt>true</tt>.
     *
     * @param isPerTask Configuration parameter indicating whether a new round robin order should
     *      be created for every task. Default is <tt>true</tt>.
     */
    @GridSpiConfiguration(optional = true)
    public void setPerTask(boolean isPerTask) {
        this.isPerTask = isPerTask;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(@Nullable String gridName) throws GridSpiException {
        startStopwatch();

        if (log.isInfoEnabled() == true) {
            log.info(configInfo("isPerTask", isPerTask));
        }

        registerMBean(gridName, this, GridRoundRobinLoadBalancingSpiMBean.class);

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }

        balancer = new GridRoundRobinGlobalLoadBalancer(log);
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        balancer = null;

        perTaskBalancers.clear();

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

        if (isPerTask == false) {
            balancer.onContextInitialized(spiCtx);
        }
        else {
            getSpiContext().addLocalEventListener(evtListener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onContextDestroyed() {
        if (isPerTask == false) {
            if (balancer != null) {
                balancer.onContextDestroyed();
            }
        }
        else {
            GridSpiContext spiCtx = getSpiContext();

            if (spiCtx != null) {
                spiCtx.removeLocalEventListener(evtListener);
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

        if (isPerTask == true) {
            // Note that every session operates from single thread which
            // allows us to use concurrent map and avoid synchronization.
            GridRoundRobinPerTaskLoadBalancer balancer = perTaskBalancers.get(ses.getId());

            if (balancer == null) {
                perTaskBalancers.put(ses.getId(), balancer = new GridRoundRobinPerTaskLoadBalancer());
            }

            return balancer.getBalancedNode(top);
        }

        return balancer.getBalancedNode(top);
    }

    /**
     * THIS METHOD IS USED ONLY FOR TESTING.
     *
     * @param ses Task session.
     * @return Internal list of nodes.
     */
    List<GridNode> getNodes(GridTaskSession ses) {
        if (isPerTask == true) {
            GridRoundRobinPerTaskLoadBalancer balancer = perTaskBalancers.get(ses.getId());

            if (balancer == null) {
                return Collections.emptyList();
            }

            return balancer.getNodes();
        }

        return balancer.getNodes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridRoundRobinLoadBalancingSpi.class, this);
    }
}
