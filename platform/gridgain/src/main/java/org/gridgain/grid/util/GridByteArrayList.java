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
import java.nio.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Resizable array implementation of the byte list (eliminating auto-boxing of primitive byte type).
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridToStringExclude
public class GridByteArrayList implements Externalizable {
    /** List byte data. */
    @GridToStringExclude
    private byte[] data = null;

    /** List's size. */
    private int size = 0;

    /**
     * No-op constructor that creates unitialized list. This method is meant
     * to by used only by {@link Externalizable} interface.
     */
    public GridByteArrayList() {
        // No-op.
    }

    /**
     * Creates empty list with the specified initial capacity.
     *
     * @param capacity Initial capacity.
     */
    public GridByteArrayList(int capacity) {
        assert capacity > 0 : "ASSERTION [line=57, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        data = new byte[capacity];
    }

    /**
     * Wraps existing array into byte array list.
     *
     * @param data Array to wrap.
     * @param size Size of data inside of array.
     */
    public GridByteArrayList(byte[] data, int size) {
        assert data != null : "ASSERTION [line=69, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";
        assert size > 0 : "ASSERTION [line=70, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        this.data = data;
        this.size = size;
    }

    /**
     * Wraps existing array into byte array list.
     *
     * @param data Array to wrap.
     */
    public GridByteArrayList(byte[] data) {
        assert data != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        this.data = data;
        this.size = data.length;
    }

    /**
     * Resets byte array to empty. Note that this method simply resets the size
     * as there is no need to reset every byte in the array.
     */
    public void reset() {
        size = 0;
    }

    /**
     * Returns the underlying array. This method exists as performance
     * optimization to avoid extra copying of the arrays. Data inside
     * of this array should not be altered, only copied.
     *
     * @return Internal array.
     */
    public byte[] getInternalArray() {
        return data;
    }

    /**
     * Gets copy of internal array.
     *
     * @return Copy of internal array.
     */
    public byte[] getArray() {
        byte[] res = new byte[size];

        System.arraycopy(data, 0, res, 0, size);

        return res;
    }

    /**
     * Gets initial capacity of the list.
     *
     * @return Initial capacity.
     */
    public int getCapacity() {
        return data.length;
    }

    /**
     * Sets initial capacity of the list.
     *
     * @param capacity Initial capacity.
     */
    private void setCapacity(int capacity) {
        assert capacity > 0 : "ASSERTION [line=135, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        if (capacity != getCapacity()) {
            if (capacity < size) {
                size = capacity;
            }

            byte[] newData = new byte[capacity];

            System.arraycopy(data, 0, newData, 0, size);

            data = newData;
        }
    }

    /**
     * Gets number of bytes in the list.
     *
     * @return Number of bytes in the list.
     */
    public int getSize() {
        return size;
    }

    /**
     * Preallocates internal array for specified byte number only
     * if it currently is smaller than desired number.
     *
     * @param cnt Byte number to preallocate.
     */
    public void allocate(int cnt) {
        if (size + cnt > getCapacity()) {
            setCapacity(size + cnt);
        }
    }

    /**
     * Resizes internal byte array representation.
     *
     * @param cnt Number of bytes to request.
     */
    private void requestFreeSize(int cnt) {
        if (size + cnt > getCapacity()) {
            setCapacity((size + cnt) << 1);
        }
    }

    /**
     * Appends byte element to the list.
     *
     * @param b Byte value to append.
     */
    public void add(byte b) {
        requestFreeSize(1);

        data[size++] = b;
    }

    /**
     * Sets a byte at specified position.
     *
     * @param pos Specified position.
     * @param b Byte to set.
     */
    public void set(int pos, byte b) {
        assert pos >= 0 : "ASSERTION [line=200, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";
        assert pos < size : "ASSERTION [line=201, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        data[pos] = b;
    }

    /**
     * Appends integer to the next 4 bytes of list.
     *
     * @param i Integer to append.
     */
    public void add(int i) {
        requestFreeSize(4);

        GridUtils.intToBytes(i, data, size);

        size += 4;
    }

    /**
     * Sets integer at specified position.
     *
     * @param pos Specified position.
     * @param i Integer to set.
     */
    public void set(int pos, int i) {
        assert pos >= 0 : "ASSERTION [line=226, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";
        assert pos + 4 <= size : "ASSERTION [line=227, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        GridUtils.intToBytes(i, data, pos);
    }

    /**
     *
     * @param bytes Byte to add.
     * @param off Offest at which to add.
     * @param len Number of bytes to add.
     */
    public void add(byte[] bytes, int off, int len) {
        requestFreeSize(len);

        System.arraycopy(bytes, off, data, size, len);

        size += len;
    }

    /**
     * Adds data from byte buffer into array.
     *
     * @param buf Buffer to read bytes from.
     * @param len Number of bytes to add.
     */
    public void add(ByteBuffer buf, int len) {
        requestFreeSize(len);

        buf.get(data, size, len);

        size += len;
    }

    /**
     * Gets the element (byte) at the specified position in the list.
     *
     * @param i Index of element to return.
     * @return The element at the specified position in the list.
     */
    public byte get(int i) {
        assert i < size : "ASSERTION [line=267, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        return data[i];
    }

    /**
     * Gets 4 bytes from byte list as an integer.
     *
     * @param i Index into the byte list.
     * @return Integer starting at index location.
     */
    public int getInt(int i) {
        assert i + 4 <= size : "ASSERTION [line=279, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";

        return GridUtils.bytesToInt(data, i);
    }

    /**
     * Reads all data from input stream until the end into this byte list.
     *
     * @param in Input stream to read from.
     * @throws IOException Thrown if any I/O error occurred.
     */
    public void readAll(InputStream in) throws IOException {
        assert in != null : "ASSERTION [line=291, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";
        
        int read = 0;

        while (read >= 0) {
            int free = getCapacity() - size;

            if (free == 0) {
                requestFreeSize(1);

                free = getCapacity() - size;

                assert free > 0 : "ASSERTION [line=303, file=src/java/org/gridgain/grid/util/GridByteArrayList.java]";
            }

            read = in.read(data, size, free);

            if (read > 0) {
                size += read;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(size);

        out.write(data, 0, size);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        size = in.readInt();

        data = new byte[size];

        in.readFully(data, 0, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridByteArrayList.class, this);
    }
}
