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

/**
 * This class defines output stream backed by byte array.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridByteArrayOutputStream extends ByteArrayOutputStream {
    /**
     *
     */
    public GridByteArrayOutputStream() {
        // No-op.
    }

    /**
     *
     * @param size Byte array size.
     */
    public GridByteArrayOutputStream(int size) {
        super(size);
    }

    /**
     *
     * @param buf Byte buffer.
     * @param count Byte buffer size.
     */
    public GridByteArrayOutputStream(byte[] buf, int count) {
        this.buf = buf;
        this.count = count;
    }

    /**
     *
     * @param bytes Byte list.
     */
    public GridByteArrayOutputStream(GridByteArrayList bytes) {
        buf = bytes.getInternalArray();
        count = bytes.getSize();
    }

    /**
     * Gets {@link GridByteArrayList} wrapper around the internal array.
     *
     * @return Wrapper around the internal array.
     */
    public GridByteArrayList toByteArrayList() {
        return new GridByteArrayList(buf, count);
    }
}
