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
import org.gridgain.grid.resources.*;

/**
 * This class defines simplified adapter for {@link GridTask}. This adapter can be used
 * when jobs can be randomly assigned to available grid nodes. This adapter is sufficient
 * in most homogeneous environments where all nodes are equally suitable for executing grid
 * job. See {@link #split(int, Object)} method for more details.
 * <p>
 * Below is a coding example of how you would use <tt>GridTaskSplitAdapter</tt>:
 * <pre name="code" class="java">
 * public class MyFooBarTask extends GridTaskSplitAdapter&lt;Object, String&gt; {
 *     &#64;Override
 *     protected Collection&lt;? extends GridJob&gt; split(int gridSize, Object arg) throws GridException {
 *         List&lt;MyFooBarJob&gt; jobs = new ArrayList&lt;MyFooBarJob&gt;(gridSize);
 * 
 *         for (int i = 0; i &lt; gridSize; i++) {
 *             jobs.add(new MyFooBarJob(arg));
 *         }
 * 
 *         // Node assignment via load balancer 
 *         // happens automatically.
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
 * <p>
 * <img src="{@docRoot}/img/gg_20.png" style="padding: 0px 5px 0px 0px" align="left"><h1 class="header">Migrating to GridGain 2.0</h1>
 * In GridGain 2.0 this interface API has been updated for better static type checking. Although the change is
 * trivial and provides much better type safety during development - it introduced 
 * incompatibility with prior versions of GridGain. <a href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Migration+To+GridGain+2.0+From+Previous+Version">Follow this link</a> 
 * for easy source code migration instructions. 
 *       
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <T> Type of the task execution argument passed as a 2nd argument into 
 *      {@link Grid#execute(Class, Object)} method.
 * @param <R> Type of the task result returning from {@link GridTask#reduce(List)} method.
 */
public abstract class GridTaskSplitAdapter<T, R> extends GridTaskAdapter<T, R> {
    /** Load balancer. */
    @GridLoadBalancerResource
    private GridLoadBalancer balancer = null;

    /**
     * This is a simplified version of {@link GridTask#map(List, Object)} method.
     * <p>
     * This method basically takes given argument and splits it into a collection
     * of {@link GridJob} using provided grid size as indication of how many node are
     * available.
     *
     * @param gridSize Number of available grid nodes. Note that returned number of
     *      jobs can be less, equal or greater than this grid size.
     * @param arg Task execution argument. Can be <tt>null</tt>.
     * @return Collection of grid jobs. These jobs will be randomly mapped to
     *      available grid nodes. Note that if number of jobs is greater than number of
     *      grid nodes (i.e, grid size), the grid nodes will be reused and some jobs
     *      will end up on the same grid nodes.
     * @throws GridException Thrown in case of any errors.
     *
     * @see GridTask#map(List, Object)
     */
    protected abstract Collection<? extends GridJob> split(int gridSize, T arg) throws GridException;

    /**
     * {@inheritDoc}
     */
    public final Map<? extends GridJob, GridNode> map(List<GridNode> subgrid, T arg) throws GridException {
        assert subgrid != null : "ASSERTION [line=108, file=src/java/org/gridgain/grid/GridTaskSplitAdapter.java]";
        assert subgrid.isEmpty() == false : "ASSERTION [line=109, file=src/java/org/gridgain/grid/GridTaskSplitAdapter.java]";

        Collection<? extends GridJob> jobs = split(subgrid.size(), arg);

        if (jobs == null || jobs.isEmpty() == true) {
            throw (GridException)new GridException("Split returned no jobs.").setData(114, "src/java/org/gridgain/grid/GridTaskSplitAdapter.java");
        }

        Map<GridJob, GridNode> map = new HashMap<GridJob, GridNode>(jobs.size());

        for (GridJob job : jobs) {
            map.put(job, balancer.getBalancedNode(job));
        }

        return map;
    }
}
