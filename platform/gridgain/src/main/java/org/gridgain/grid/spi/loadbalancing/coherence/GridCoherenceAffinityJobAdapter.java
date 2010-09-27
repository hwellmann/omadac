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

import java.io.*;
import org.gridgain.grid.*;

/**
 * Convenience adapter for {@link GridCoherenceAffinityJob} interface that provides
 * default implementation of {@link GridJob#cancel()} method and allows
 * to add arguments.
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
 * @param <G> Job argument type.
 */
public abstract class GridCoherenceAffinityJobAdapter <A extends Serializable, G extends Serializable> 
    extends GridJobAdapter<G>  implements GridCoherenceAffinityJob<A> {
    /** Coherence cache name. */
    private String cacheName = null;
    
    /** Affinity key. */
    private A affKey = null;
    
    /**
     * No-arg constructor. Note that all fields will be <tt>null</tt>.
     * <p>
     * Please use {@link #setArgument(Serializable)} to set job argument(s),
     * {@link #setAffinityKey(Serializable)} to set affinity key, and
     * {@link #setCacheName(String)} to set cache name.
     */
    protected GridCoherenceAffinityJobAdapter() {
        // No-op.
    }
    
    /**
     * Initializes coherence affinity job with cache name. Note that
     * all other fields will be <tt>null</tt>.
     * <p>
     * Please use {@link #setArgument(Serializable)} to set job argument(s), and
     * {@link #setAffinityKey(Serializable)} to set affinity key.
     * 
     * @param cacheName Coherence cache name.
     */
    protected GridCoherenceAffinityJobAdapter(String cacheName) {
        this.cacheName = cacheName;
    }
    
    /**
     * Initializes coherence affinity job with cache name. Note that
     * all arguments will be <tt>null</tt>.
     * <p>
     * Please use {@link #setArgument(Serializable)} to set job argument(s).
     * 
     * @param cacheName Coherence cache name.
     * @param affKey Coherence affinity key.
     */
    protected GridCoherenceAffinityJobAdapter(String cacheName, A affKey) {
        this.cacheName = cacheName;
        this.affKey = affKey;
    }

    /**
     * Creates fully initialized job with specified cache name, affinity key,
     * and job argument(s).
     *
     * @param cacheName Coherence cache name.
     * @param affKey Coherence affinity key.
     * @param args Job argument(s).
     */
    protected GridCoherenceAffinityJobAdapter(String cacheName, A affKey, G... args) {
        super(args);
        
        this.cacheName = cacheName;
        this.affKey = affKey;
    }

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * @return The affKey.
     */
    public A getAffinityKey() {
        return affKey;
    }

    /**
     * Sets Coherence cache name.
     * 
     * @param cacheName Coherence cache name.
     */
    void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }


    /**
     * Sets affinity key.
     * 
     * @param affKey Affinity key.
     */
    void setAffinityKey(A affKey) {
        this.affKey = affKey;
    }
}

