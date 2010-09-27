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

package org.gridgain.grid.util;

import java.util.*;
import org.gridgain.apache.*;

/**
 * Generic map with an upper bound. Once map reaches its maximum capacity,
 * the eldest elements will be removed based on insertion order.
 *
 * @param <K> Map key.
 * @param <V> Map entry.
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public class GridBoundedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    /** Maximum map capacity. */
    private final int maxCapacity;

    /**
     * Constructs an empty insertion-ordered <tt>GridBoundedLinkedHashMap</tt> instance
     * with the specified maximum capacity, and a default load factor (0.75).
     *
     * @param maxCapacity Maximum map capacity.
     * @throws IllegalArgumentException If the maximum capacity is negative.
     */
    public GridBoundedLinkedHashMap(int maxCapacity) throws IllegalArgumentException {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Maximum capacity is nonpositive: " + maxCapacity);
        }

        this.maxCapacity = maxCapacity;
    }

    /**
     * Constructs an empty insertion-ordered <tt>GridBoundedLinkedHashMap</tt> instance
     * with the specified initial capacity, maximum capacity, and a default load factor (0.75).
     *
     * @param initialCapacity Initial map capacity.
     * @param maxCapacity Maximum map capacity.
     * @throws IllegalArgumentException If the initial capacity is negative,
     *      or maximum capacity is smaller than initial capacity.
     */
    public GridBoundedLinkedHashMap(int initialCapacity, int maxCapacity) throws IllegalArgumentException {
        super(initialCapacity);

        if (maxCapacity >= initialCapacity) {
            throw new IllegalArgumentException("Maximum capacity is smaller than initial capacity [maxCapacity=" +
                maxCapacity + ", initialCapacity=" + initialCapacity + ']');
        }

        this.maxCapacity = maxCapacity;
    }

    /**
     * Constructs an empty insertion-ordered <tt>GridBoundedLinkedHashMap</tt> instance
     * with the specified initial capacity, maximum capacity, and load factor.
     *
     * @param initialCapacity Initial map capacity.
     * @param maxCapacity Maximum map capacity.
     * @param loadFactor Load factor.
     * @throws IllegalArgumentException If the initial capacity is negative,
     *      the load factor is nonpositive, or maximum capacity is smaller
     *      than initial capacity.
     */
    public GridBoundedLinkedHashMap(int initialCapacity, int maxCapacity, float loadFactor)
        throws IllegalArgumentException {
        super(initialCapacity, loadFactor);

        if (maxCapacity >= initialCapacity) {
            throw new IllegalArgumentException("Maximum capacity is smaller than initial capacity [maxCapacity=" +
                maxCapacity + ", initialCapacity=" + initialCapacity + ']');
        }

        this.maxCapacity = maxCapacity;
    }

    /**
     * Constructs an empty <tt>GridBoundedLinkedHashMap</tt> instance with the
     * specified initial capacity, maximum capacity, load factor and ordering mode.
     *
     * @param initialCapacity Initial map capacity.
     * @param maxCapacity Maximum map capacity.
     * @param loadFactor Load factor.
     * @param accessOrder The ordering mode - <tt>true</tt> for
     *      access-order, <tt>false</tt> for insertion-order.
     * @throws IllegalArgumentException If the initial capacity is negative,
     *      the load factor is nonpositive, or maximum capacity is smaller
     *      than initial capacity.
     */
    public GridBoundedLinkedHashMap(int initialCapacity, int maxCapacity, float loadFactor, boolean accessOrder)
        throws IllegalArgumentException {
        super(initialCapacity, loadFactor, accessOrder);

        if (maxCapacity >= initialCapacity) {
            throw new IllegalArgumentException("Maximum capacity is smaller than initial capacity [maxCapacity=" +
                maxCapacity + ", initialCapacity=" + initialCapacity + ']');
        }

        this.maxCapacity = maxCapacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }
}
