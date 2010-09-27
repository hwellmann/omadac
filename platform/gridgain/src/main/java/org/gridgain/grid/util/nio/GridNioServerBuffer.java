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

package org.gridgain.grid.util.nio;

import java.nio.*;
import org.gridgain.grid.util.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridNioServerBuffer {
    /** Preallocate 8K. */
    private GridByteArrayList msgBytes = new GridByteArrayList(1024 << 3);

    /** */
    private int msgSize = -1;

    /**
     *
     */
    void reset() {
        msgBytes.reset();

        msgSize = -1;
    }

    /**
     * Gets message size.
     *
     * @return Message size.
     */
    int getMessageSize() {
        return msgSize;
    }

    /**
     * Gets message bytes read so far.
     *
     * @return Message bytes read so far.
     */
    GridByteArrayList getMessageBytes() {
        return msgSize < 0 ? null : msgBytes;
    }

    /**
     * Checks whether the byte array is filled.
     *
     * @return Flag indicating whether byte array is filled or not.
     */
    boolean isFilled() {
        return msgSize > 0 && msgBytes.getSize() == msgSize;
    }

    /**
     *
     * @param buf FIXDOC
     */
    void read(ByteBuffer buf) {
        if (msgSize < 0) {
            int remaining = buf.remaining();

            if (remaining > 0) {
                int missing = 4 - msgBytes.getSize();

                msgBytes.add(buf, missing < remaining ? missing : remaining);

                assert msgBytes.getSize() <= 4 : "ASSERTION [line=89, file=src/java/org/gridgain/grid/util/nio/GridNioServerBuffer.java]";

                if (msgBytes.getSize() == 4) {
                    msgSize = msgBytes.getInt(0);

                    assert msgSize > 0 : "ASSERTION [line=94, file=src/java/org/gridgain/grid/util/nio/GridNioServerBuffer.java]";

                    msgBytes.reset();

                    // Allocate required size.
                    msgBytes.allocate(msgSize);
                }
            }
        }

        int remaining = buf.remaining();

        // If there are more bytes in buffer.
        if (remaining > 0) {
            int missing = msgSize - msgBytes.getSize();

            // Read only up to message size.
            if (missing > 0) {
                msgBytes.add(buf, missing < remaining ? missing : remaining);
            }
        }
    }
}
