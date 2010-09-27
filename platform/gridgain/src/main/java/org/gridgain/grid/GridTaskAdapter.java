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

import java.util.*;

/**
 * Convenience adapter for {@link GridTask} interface. Here is an example of
 * how <tt>GridTaskAdapter</tt> can be used:
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridTaskAdapter&lt;String, String&gt; {
 *     // Inject load balancer.
 *     &#64;GridLoadBalancerResource
 *     GridLoadBalancer balancer;
 *
 *     // Map jobs to grid nodes.
 *     public Map&lt;? extends GridJob, GridNode&gt; map(List&lt;GridNode&gt; subgrid, String arg) throws GridException {
 *         Map&lt;MyFooBarJob, GridNode&gt; jobs = new HashMap&lt;MyFooBarJob, GridNode&gt;(subgrid.size());
 *
 *         // In more complex cases, you can actually do
 *         // more complicated assignments of jobs to nodes.
 *         for (int i = 0; i &lt; subgrid.size(); i++) {
 *             // Pick the next best balanced node for the job.
 *             jobs.put(new MyFooBarJob(arg), balancer.getBalancedNode())
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
 * For more information refer to {@link GridTask} documentation.
 * <p>
 * <img src="{@docRoot}/img/gg_20.png" style="padding: 0px 5px 0px 0px" align="left"><h1 class="header">Migrating to GridGain 2.0</h1>
 * In GridGain 2.0 this interface API has been updated for better static type checking. Although the change is
 * trivial and provides much better type safety during development - it introduced
 * incompatibility with prior versions of GridGain. Follow this <a href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Migration+To+GridGain+2.0+From+Previous+Version">link</a>
 * for easy source code migration instructions.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <T> Type of the task argument.
 * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
 */
public abstract class GridTaskAdapter<T, R> implements GridTask<T, R> {
    /**
     * Default implementation which will wait for all jobs to complete before
     * calling {@link #reduce(List)} method.
     * <p>
     * If remote job resulted in exception ({@link GridJobResult#getException()} is not <tt>null</tt>),
     * then {@link GridJobResultPolicy#FAILOVER} policy will be returned if the exception is instance
     * of {@link GridTopologyException} or {@link GridExecutionRejectedException}, which means that
     * remote node either failed or job execution was rejected before it got a chance to start. In all
     * other cases the exception will be rethrown which will ultimately cause task to fail.
     *
     * @param result Received remote grid executable result.
     * @param received All previously received results.
     * @return Result policy that dictates how to process further upcoming
     *       job results.
     * @throws GridException If handling a job result caused an error effectively rejecting
     *      a failover. This exception will be thrown out of {@link GridTaskFuture#get()} method.
     * @see GridTask#result(GridJobResult, List)
     */
    public GridJobResultPolicy result(GridJobResult result, List<GridJobResult> received) throws GridException {
        GridException e = result.getException();

        // Try to failover if result is failed.
        if (e != null) {
            // Don't failover user's code errors.
            if (e instanceof GridExecutionRejectedException == true ||
                e instanceof GridTopologyException == true) {
                return GridJobResultPolicy.FAILOVER;
            }

            throw (GridException)new GridException("Remote job threw user exception (override or implement GridTask.result(..) " +
                "method if you would like to have automatic failover for this exception).", e).setData(109, "src/java/org/gridgain/grid/GridTaskAdapter.java");
        }

        // Wait for all job responses.
        return GridJobResultPolicy.WAIT;
    }
}
