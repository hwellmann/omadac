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
import org.gridgain.grid.util.tostring.*;

/**
 * An array based queue. This queue orders elements FIFO (first-in-first-out).
 * The head of the queue is the element that has been on the queue
 * the longest time. The tail of the queue is the element that has been on
 * the queue the shortest time. New elements are inserted at the tail of
 * the queue, and the queue retrieval operations obtain elements at
 * the head of the queue.
 * Queue supports maximum capacity. When exceeded newest elements will
 * delete oldest ones.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <T> FIXDOC
 */
@GridToStringExclude
public class GridFifoQueue<T> implements Iterable<T> {
    /** */
    private static final int DFLT_SIZE = 16;

    /** Current size of the {@link #data} array. */
    private int capacity;

    /** Maximum size of the {@link #data} array. */
    private int maxCapacity;

    /** Last index in the {@link #data} array. */
    private int lastIndex;

    /** Head index. */
    private int head;

    /** Tail index. */
    private int tail;

    /** Number of objects in the queue. */
    private int count;

    /** Growth factor used when buffer space refreshed. */
    private float growthFactor = 2.0f;

    /** Data container. */
    private T[] data;

    /**
     *
     * @param capacity Initial size for the queue.
     * @param maxCapacity Maximum queue size.
     */
    @SuppressWarnings({"unchecked"})
    public GridFifoQueue(int capacity, int maxCapacity) {
        assert capacity > 0 : "ASSERTION [line=77, file=src/java/org/gridgain/grid/util/GridFifoQueue.java]"; 

        this.capacity = capacity;
        this.maxCapacity = maxCapacity;

        lastIndex = capacity - 1;

        head = 0;
        tail = 0;

        count = 0;

        data = (T[])new Object[capacity];
    }

    /**
     *
     * @param capacity Initial size for the queue.
     */
    @SuppressWarnings({"unchecked"})
    public GridFifoQueue(int capacity) {
        this(capacity, Integer.MAX_VALUE);
    }

    /**
     * Copy given queue into this queue.
     *
     * @param q The queue to copy.
     */
    @SuppressWarnings({"unchecked"})
    public final void copyFrom(GridFifoQueue<T> q) {
        if (capacity < q.capacity) {
            head = q.head;
            tail = q.tail;
            count = q.count;
            capacity = q.capacity;
            maxCapacity = q.maxCapacity;

            data = (T[])new Object[capacity];

            lastIndex = capacity - 1;

            System.arraycopy(q.data, 0, data, 0, capacity);
        }
        else if (capacity == q.capacity) {
            head = q.head;
            tail = q.tail;
            count = q.count;

            System.arraycopy(q.data, 0, data, 0, capacity);
        }
        // size > q.size
        else {
            // size and lastIndex remain the same.
            tail = q.tail;
            count = q.count;

            if (q.head <= q.tail) {
                head = q.head;

                System.arraycopy(q.data, 0, data, 0, q.capacity);
            }
            else {
                System.arraycopy(q.data, 0, data, 0, tail);

                int d = q.capacity - q.head;

                head = capacity - d;

                System.arraycopy(q.data, q.head, data, head, d);
            }
        }
    }

    /**
     * Constructs queue with default size.
     */
    public GridFifoQueue() {
        this(DFLT_SIZE);
    }

    /**
     * Sets growth factor used when buffer space refreshed.
     *
     * @param growthFactor Growth parameter used when buffer space refreshed.
     *      Must be greater that 1.
     */
    public void setGrowthFactory(float growthFactor) {
        assert growthFactor > 1.0 : "ASSERTION [line=165, file=src/java/org/gridgain/grid/util/GridFifoQueue.java]";

        this.growthFactor = growthFactor;
    }

    /**
     * Gets growth factor used when buffer space refreshed.
     *
     * @return The growth factor.
     */
    public float getGrowthFactor() {
        return growthFactor;
    }

    /**
     * Refresh internal buffer for queue with defined <tt>newCapacity</tt>.
     *
     * @param newCapacity New initial capacity.
     */
    @SuppressWarnings({"unchecked"})
    public void ensureCapacity(int newCapacity) {
        if (newCapacity > maxCapacity) {
            newCapacity = maxCapacity;
        }

        if (newCapacity > capacity) {
            T[] newData = (T[])new Object[newCapacity];

            int len = capacity - head;

            System.arraycopy(data, head, newData, 0, len);
            System.arraycopy(data, 0, newData, len, tail);

            head = 0;
            tail = capacity;
            capacity = newCapacity;
            data = newData;

            lastIndex = capacity - 1;
        }
    }

    /**
     *
     * @param object Object to add to the end of the queue.
     */
    public void add(T object) {
        if (count == maxCapacity) {
            // We exceeded maximum size. Remove oldest element.
            get();
        }
        else {
            if (isFilled() == true) {
                ensureCapacity((int)(capacity * growthFactor));
            }
        }

        data[tail] = object;

        tail = tail == lastIndex ? 0 : tail + 1;

        count++;
    }

    /**
     * Peeks object from the beginning of the queue.
     * Note that the object remains in the queue.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    public T head() {
        assert count > 0 : "ASSERTION [line=236, file=src/java/org/gridgain/grid/util/GridFifoQueue.java]";

        return data[head];
    }

    /**
     * Peeks object from the end of the queue.
     * Note that the object remains in the queue.
     *
     * @return An object from the end of the queue.
     */
    public T tail() {
        assert count > 0 : "ASSERTION [line=248, file=src/java/org/gridgain/grid/util/GridFifoQueue.java]";

        return data[tail == 0 ? lastIndex : tail - 1];
    }

    /**
     * Removes and returns object from the beginning of the queue.
     *
     * @throws IndexOutOfBoundsException If queue is empty.
     * @return An object from the beginning of the queue.
     */
    public T get() throws IndexOutOfBoundsException {
        if (count == 0) {
            throw new IndexOutOfBoundsException("Queue is empty.");
        }

        assert count > 0 : "ASSERTION [line=264, file=src/java/org/gridgain/grid/util/GridFifoQueue.java]";

        T retval = data[head];

        data[head] = null;

        count--;

        head = head == lastIndex ? 0 : head + 1;

        return retval;
    }

    /**
     * Removes object if it found in queue.
     *
     * @param obj Object to delete from queue.
     * @return <tt>true</tt> if object found in queue.
     */
    public boolean remove(T obj) {
        int size = head + count;

        for (int i = head; i < size; i++) {
            if (data[i % data.length].equals(obj) == true) {  // (i % data.length) keeps index in the right range.
                if (head < tail) {
                    if (i < tail) {
                        System.arraycopy(data, i + 1, data, i, tail - i);
                    }

                    assert tail != 0 : "ASSERTION [line=293, file=src/java/org/gridgain/grid/util/GridFifoQueue.java]";

                    tail--;
                }
                else {
                    if (i <= tail) {
                        if (i < tail) {
                            System.arraycopy(data, i + 1, data, i, tail - i);
                        }

                        tail = tail == 0 ? lastIndex : tail - 1;
                    }
                    else {
                        if (i > head) {
                            System.arraycopy(data, head, data, head + 1, i - head);
                        }

                        head = head == lastIndex ? 0 : head + 1;
                    }
                }

                count--;

                return true;
            }
        }

        return false;
    }

    /**
     * Gets <tt>i'th</tt> element of the queue.
     * Note that index is independent of the internal implementation.
     *
     * @param i Index of the queued element.
     * @return Gets <tt>i'th</tt> element of the queue.
     */
    public T peek(int i) {
        if (i > data.length) {
            throw new IndexOutOfBoundsException("Invalid index: " + i);
        }

        return data[(head + i) % data.length];
    }

    /**
     * Checks if an object exists in the queue.
     * Objects must implement <tt>equals()</tt> correctly.
     *
     * @param obj Object to check for containment.
     * @return <tt>true</tt> if object is in the queue, <tt>false</tt>
     *      otherwise.
     */
    public boolean contains(T obj) {
        int size = head + count;

        for (int i = head; i < size; i++) {
            if (data[i % data.length].equals(obj) == true) {  // (i % data.length) keeps index in the right range.
                return true;
            }
        }

        return false;
    }

    /**
     * Gets an iterator over the elements in this queue in proper sequence.
     *
     * @return An iterator over the elements in this queue in proper sequence.
     */
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            /** */
            private int i = head;

            /** */
            private final int length = head + count;

            /** */
            private final int dataLength = data.length;

            /**
             * {@inheritDoc}
             */
            public boolean hasNext() {
                return i != length;
            }

            /**
             * {@inheritDoc}
             */
            public T next() {
                if (i >= length) {
                    throw new NoSuchElementException("Iterator reached the end.");
                }

                return data[i++ % dataLength];
            }

            /**
             * {@inheritDoc}
             */
            public void remove() {
                throw new UnsupportedOperationException("Operation is not support for this collection.");
            }
        };
    }

    /**
     * Gets <tt>true</tt> if this queue contains no elements.
     *
     * @return <tt>true</tt> if this queue contains no elements.
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Gets <tt>true</tt> if this queue contains no allocated free space
     * for new elements.
     *
     * @return <tt>true</tt> if this queue contains no allocated free space
     *      for new elements.
     */
    public boolean isFilled() {
        return count == capacity;
    }

    /**
     * Gets elements count in queue.
     *
     * @return Elements count in queue.
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets initial capacity of the queue.
     *
     * @return Initial capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Clears this queue. Note that queue doesn't down size.
     */
    public void clear() {
        Arrays.fill(data, null);

        head = 0;
        tail = 0;

        count = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridFifoQueue.class, this);
    }
}
