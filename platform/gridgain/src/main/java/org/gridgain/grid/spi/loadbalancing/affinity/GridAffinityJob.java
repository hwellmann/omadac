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

import org.gridgain.grid.*;

/**
 * Affinity job which extends {@link GridJob} and adds
 * {@link #getAffinityKey()} method. Basically the job should know the 
 * data it will access and provide the key for it to GridGain which will
 * route it to the grid node on which this data is cached.
 * <p>
 * Note that if {@link GridAffinityLoadBalancingSpi} will receive regular
 * {@link GridJob} instead of <tt>GridAffinityJob</tt>, then a random 
 * available node will be picked.
 * <p>
 * Here is an example of how affinity job is implemented:
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
 * Here is another example on how it can be used from task <tt>map</tt> (or <tt>split</tt>) method.
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
 * For complete documentation on how affinity jobs are created and used, refer to {@link GridAffinityLoadBalancingSpi}
 * documentation.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <A> Affinity key type.
 */
public interface GridAffinityJob<A> extends GridJob {
    /**
     * Gets affinity key for this job.
     *
     * @return Affinity key for this job.
     */
    public A getAffinityKey();
}
