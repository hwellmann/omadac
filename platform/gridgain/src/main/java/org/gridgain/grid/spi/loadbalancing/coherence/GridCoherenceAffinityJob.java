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

import org.gridgain.grid.*;

/**
 * Affinity job which extends {@link GridJob} and asks user to implement
 * {@link #getAffinityKey()} and {@link #getCacheName()} methods. Basically
 * the job should know the data it will access and provide the key and
 * Coherence cache name for it to GridGain which will route it to the
 * grid node on which this data is cached.
 * <p>
 * Here is an example of how affinity job is implemented:
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
 * For complete documentation on how affinity jobs are created and used, refer to {@link GridCoherenceLoadBalancingSpi}
 * documentation.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <A> Affinity key type.
 */
public interface GridCoherenceAffinityJob<A> extends GridJob {
    /**
     * Gets affinity key for this job.
     *
     * @return Affinity key for this job.
     */
    public A getAffinityKey();

    /**
     * Gets cache name for this job.
     *
     * @return Cache name for this job.
     */
    public String getCacheName();
}
