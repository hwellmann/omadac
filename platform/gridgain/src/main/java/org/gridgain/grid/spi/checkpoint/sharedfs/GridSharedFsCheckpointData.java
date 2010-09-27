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

package org.gridgain.grid.spi.checkpoint.sharedfs;

import java.io.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of all checkpoint that are saved to the file system. It
 * extends every checkpoint with expiration time and host name
 * which created this checkpoint.
 * <p>
 * Host name is used by {@link GridSharedFsCheckpointSpi} SPI to give node
 * correct files if it is restarted.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridSharedFsCheckpointData implements Serializable {
    /** Checkpoint data. */
    private final byte[] state;

    /** Checkpoint expiration time. */
    private final long expireTime;

    /** Host name that created this checkpoint. */
    private final String host;

    /**
     * Creates new instance of checkpoint data wrapper.
     *
     * @param state      Checkpoint data.
     * @param expireTime Checkpoint expiration time in milliseconds.
     * @param host       name of host that created this checkpoint.
     */
    GridSharedFsCheckpointData(byte[] state, long expireTime, String host) {
        assert expireTime >= 0 : "ASSERTION [line=56, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointData.java]";
        assert host != null : "ASSERTION [line=57, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsCheckpointData.java]";

        this.state = state;
        this.expireTime = expireTime;
        this.host = host;
    }

    /**
     * Gets checkpoint data.
     *
     * @return Checkpoint data.
     */
    byte[] getState() {
        return state;
    }

    /**
     * Gets checkpoint expiration time.
     *
     * @return Expire time in milliseconds.
     */
    long getExpireTime() {
        return expireTime;
    }

    /**
     * Gets checkpoint host name.
     *
     * @return Host name.
     */
    String getHost() {
        return host;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridSharedFsCheckpointData.class, this);
    }
}
