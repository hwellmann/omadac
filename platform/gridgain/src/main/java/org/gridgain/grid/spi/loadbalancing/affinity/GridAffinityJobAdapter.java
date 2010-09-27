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
import org.gridgain.grid.*;

/**
 * Convenience adapter for {@link GridAffinityJob} interface that provides
 * default implementation of {@link GridJob#cancel()} method and allows
 * to add arguments.
 * <p>
 * Here is example of how job affinity adapter could be used:
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
 * For more information and examples, see {@link GridAffinityLoadBalancingSpi}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <A> Affinity key type.
 * @param <G> Job argument type.
 */
public abstract class  GridAffinityJobAdapter<A extends Serializable, G extends Serializable> extends GridJobAdapter<G> 
    implements GridAffinityJob<A> {
    /** */
    private A affKey = null;
    
    /**
     * No-arg constructor. 
     * <p>
     * Note that job argument(s) and affinity key will be <tt>null</tt>. Please use 
     * {@link #setArgument(Serializable)} to set job argument(s) and
     * {@link #setAffinityKey(Serializable)} to set affinity key.
     */
    protected GridAffinityJobAdapter() {
        // No-op.
    }

    /**
     * Creates affinity job with a given key. 
     * <p>
     * Note that job argument(s) and affinity key will be <tt>null</tt>. Please use 
     * {@link #setArgument(Serializable)} to set job argument(s).
     * 
     * @param affKey Affinity key.
     */
    protected GridAffinityJobAdapter(A affKey) {
        this.affKey = affKey;    
    }
    
    /**
     * Creates a fully initialized affinity job with a given key and
     * specified job argument(s).  
     * 
     * @param affKey Affinity key.
     * @param args Job arguments.
     */
    protected GridAffinityJobAdapter(A affKey, G... args) {
        super(args);
        
        this.affKey = affKey;    
    }

    /**
     * {@inheritDoc}
     */
    public A getAffinityKey() {
        return affKey;
    }
    
    /**
     * Sets affinity key.
     * 
     * @param affKey Affinity key
     */
    public void setAffinityKey(A affKey) {
        this.affKey = affKey;
    }
}
