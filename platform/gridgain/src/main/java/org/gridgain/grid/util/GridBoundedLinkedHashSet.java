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

import java.io.*;
import java.util.*;
import org.gridgain.apache.*;

/**
 * Hash table and linked list implementation of the <tt>Set</tt> interface,
 * with predictable iteration order.  This implementation differs from
 * <tt>HashSet</tt> in that it maintains a doubly-linked list running through
 * all of its entries.  This linked list defines the iteration ordering,
 * which is the order in which elements were inserted into the set
 * (<i>insertion-order</i>).  Note that insertion order is <i>not</i> affected
 * if an element is <i>re-inserted</i> into the set.  (An element <tt>e</tt>
 * is reinserted into a set <tt>s</tt> if <tt>s.add(e)</tt> is invoked when
 * <tt>s.contains(e)</tt> would return <tt>true</tt> immediately prior to
 * the invocation.)
 * <p>HashSet is also has maximum capacity. When it is reached the
 * newest elements supersede eldest ones.
 *
 * @param <E> Set element.
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public class GridBoundedLinkedHashSet<E> extends AbstractSet<E> implements Cloneable, Serializable {
    /** */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private transient HashMap<E, Object> map;

    /** */
    private static final Object FAKE = new Object();

    /**
     * Constructs a new, empty set; the backing <tt>LinkedHashMap</tt>
     * instance has default initial capacity (16) and load factor (0.75).
     *
     * @param maxCapacity Maximum set capacity.
     */
    public GridBoundedLinkedHashSet(int maxCapacity) {
        assert maxCapacity > 0 : "ASSERTION [line=62, file=src/java/org/gridgain/grid/util/GridBoundedLinkedHashSet.java]";

        map = new GridBoundedLinkedHashMap<E, Object>(maxCapacity);
    }

    /**
     * Constructs a new set containing the elements in the specified collection.
     * The <tt>LinkedHashMap</tt> is created with default load factor (0.75)
     * and an initial capacity sufficient to contain the elements in the specified
     * collection.
     *
     * @param c The collection whose elements are to be placed into this set.
     * @param maxCapacity Maximum set capacity.
     */
    public GridBoundedLinkedHashSet(Collection<? extends E> c, int maxCapacity) {
        assert maxCapacity > 0 : "ASSERTION [line=77, file=src/java/org/gridgain/grid/util/GridBoundedLinkedHashSet.java]";

        map = new GridBoundedLinkedHashMap<E, Object>(Math.max((int) (c.size() / 0.75f) + 1, 16), maxCapacity);

        addAll(c);
    }

    /**
     * Constructs a new, empty set; the backing <tt>LinkedHashMap</tt>
     * instance has the specified initial capacity and the specified load factor.
     *
     * @param initialCapacity The initial capacity of the hash map.
     * @param maxCapacity Maximum set capacity.
     * @param loadFactor the Load factor of the hash map.
     */
    public GridBoundedLinkedHashSet(int initialCapacity, int maxCapacity, float loadFactor) {
        assert maxCapacity > 0 : "ASSERTION [line=93, file=src/java/org/gridgain/grid/util/GridBoundedLinkedHashSet.java]";

        map = new GridBoundedLinkedHashMap<E, Object>(initialCapacity, maxCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty set; the backing <tt>LinkedHashHashMap</tt>
     * instance has the specified initial capacity and default load factor, which
     * is <tt>0.75</tt>.
     *
     * @param initialCapacity The initial capacity of the hash table.
     * @param maxCapacity Maximum capacity.
     */
    public GridBoundedLinkedHashSet(int initialCapacity, int maxCapacity) {
        assert maxCapacity > 0 : "ASSERTION [line=107, file=src/java/org/gridgain/grid/util/GridBoundedLinkedHashSet.java]";

        map = new GridBoundedLinkedHashMap<E, Object>(initialCapacity, maxCapacity);
    }

    /**
     * Returns an iterator over the elements in this set. The elements are
     * returned in no particular order.
     *
     * @return An iterator over the elements in this set.
     * @see ConcurrentModificationException
     */
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * @return The number of elements in this set (its cardinality).
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>True</tt> if this set contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     *
     * @param o Element whose presence in this set is to be tested.
     * @return <tt>true</tt> if this set contains the specified element.
     */
    @SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Adds the specified element to this set if it is not already present.
     *
     * @param o Element to be added to this set.
     * @return <tt>True</tt> if the set did not already contain the specified
     *      element.
     */
    @Override
    public boolean add(E o) {
        return map.put(o, FAKE) == null;
    }

    /**
     * Removes the specified element from this set if it is present.
     *
     * @param o Object to be removed from this set, if present.
     * @return <tt>True</tt> if the set contained the specified element.
     */
    @SuppressWarnings({"ObjectEquality"})
    @Override
    public boolean remove(Object o) {
        return map.remove(o) == FAKE;
    }

    /**
     * Removes all of the elements from this set.
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * Returns a shallow copy of this <tt>GridBoundedLinkedHashSet</tt>
     * instance: the elements themselves are not cloned.
     *
     * @return a shallow copy of this set.
     * @throws CloneNotSupportedException Thrown if cloning is not supported.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException {
        GridBoundedLinkedHashSet<E> newSet = (GridBoundedLinkedHashSet<E>) super.clone();

        newSet.map = (HashMap<E, Object>)map.clone();

        return newSet;
    }
}
