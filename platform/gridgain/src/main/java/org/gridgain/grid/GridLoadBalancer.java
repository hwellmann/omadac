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

package org.gridgain.grid;

import org.gridgain.apache.*;

/**
 * Load balancer is used for finding the best balanced node according
 * to load balancing policy. Internally load balancer will
 * query the {@link org.gridgain.grid.spi.loadbalancing.GridLoadBalancingSpi}
 * to get the balanced node.
 * <p>
 * Load balancer can be used <i>explicitly</i> from inside {@link GridTask#map(java.util.List, Object)}
 * method when you implement {@link GridTask} interface directly or use
 * {@link GridTaskAdapter}. If you use {@link GridTaskSplitAdapter} then 
 * load balancer is accessed <i>implicitly</i> by the adapter so you don't have
 * to use it directly in your logic.
 * <h1 class="header">Coding Examples</h1>
 * If you are using {@link GridTaskSplitAdapter} then load balancing logic
 * is transparent to your code and is handled automatically by the adapter.
 * Here is an example of how your task will look:
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridTaskSplitAdapter&lt;String> {
 *     &#64;Override
 *     protected Collection&lt;? extends GridJob> split(int gridSize, String arg) throws GridException {
 *         List&lt;MyFooBarJob> jobs = new ArrayList&lt;MyFooBarJob>(gridSize);
 * 
 *         for (int i = 0; i &lt; gridSize; i++) {
 *             jobs.add(new MyFooBarJob(arg));
 *         }
 *  
 *         // Node assignment via load balancer 
 *         // happens automatically.
 *         return jobs;
 *     }
 *     ...
 * } 
 * </pre>
 * If you need more fine-grained control over how some jobs within task get mapped to a node
 * and use affinity load balancing for some other jobs within task, then you should use
 * {@link GridTaskAdapter}. Here is an example of how your task will look. Note that in this
 * case we manually inject load balancer and use it to pick the best node. Doing it in 
 * such way would allow user to map some jobs manually and for others use load balancer.
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridTaskAdapter&lt;String, String> {
 *     // Inject load balancer.
 *     &#64;GridLoadBalancerResource
 *     GridLoadBalancer balancer;
 * 
 *     // Map jobs to grid nodes.
 *     public Map&lt;? extends GridJob, GridNode> map(List&lt;GridNode> subgrid, String arg) throws GridException {
 *         Map&lt;MyFooBarJob, GridNode> jobs = new HashMap&lt;MyFooBarJob, GridNode>(subgrid.size());
 * 
 *         // In more complex cases, you can actually do
 *         // more complicated assignments of jobs to nodes.
 *         for (int i = 0; i &lt; subgrid.size(); i++) {
 *             // Pick the next best balanced node for the job.
 *             GridJob myJob = new MyFooBarJob(arg);
 *             
 *             jobs.put(myJob, balancer.getBalancedNode(myJob));
 *         }
 * 
 *         return jobs;
 *     }
 * 
 *     // Aggregate results into one compound result.
 *     public String reduce(List&lt;GridJobResult&gt; results) throws GridException {
 *         // For the purpose of this example we simply
 *         // concatenate string representation of every 
 *         // job result
 *         StringBuilder buf = new StringBuilder();
 * 
 *         for (GridJobResult res : results) {
 *             // Append string representation of result
 *             // returned by every job.
 *             buf.append(res.getData().toString());
 *         }
 * 
 *         return buf.toString();
 *     }
 * } 
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridLoadBalancer {
    /**
     * Gets the next balanced node according to the underlying load
     * balancing policy.
     * 
     * @param job Job to get the balanced node for.
     * @param excludeNodes List of nodes that should be excluded from balanced nodes.
     * @return Next balanced node.
     * @throws GridException If any error occurred when finding next balanced node.
     */
    public GridNode getBalancedNode(GridJob job, GridNode... excludeNodes) throws GridException;
}
