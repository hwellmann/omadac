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

import org.gridgain.grid.util.tostring.*;

/**
 * Helper class that keeps checkpoint expiration date and last file
 * access date inside. This class used by {@link GridSharedFsTimeoutTask}
 * to track and delete obsolete files.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridSharedFsTimeData {
    /** Checkpoint expiration date. */
    private long expireTime = 0;

    /** File last access date. */
    private long lastAccessTime = 0;

    /**
     * Creates new instance of checkpoint time information.
     *
     * @param expireTime Checkpoint expiration time.
     * @param lastAccessTime File last access time.
     */
    GridSharedFsTimeData(long expireTime, long lastAccessTime) {
        assert expireTime >= 0 : "ASSERTION [line=48, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeData.java]";
        assert lastAccessTime > 0 : "ASSERTION [line=49, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeData.java]";

        this.lastAccessTime = lastAccessTime;
        this.expireTime = expireTime;
    }

    /**
     * Gets checkpoint expiration time.
     *
     * @return Expire time.
     */
    long getExpireTime() {
        return expireTime;
    }

    /**
     * Sets checkpoint expiration time.
     *
     * @param expireTime Checkpoint time-to-live value.
     */
    void setExpireTime(long expireTime) {
        assert expireTime >= 0 : "ASSERTION [line=70, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeData.java]";

        this.expireTime = expireTime;
    }

    /**
     * Gets last file access time.
     *
     * @return Saved time.
     */
    long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * Sets file last access time. This time usually is the same as file last
     * modification date.
     *
     * @param lastAccessTime File access time in milliseconds.
     */
    void setLastAccessTime(long lastAccessTime) {
        assert lastAccessTime > 0 : "ASSERTION [line=91, file=src/java/org/gridgain/grid/spi/checkpoint/sharedfs/GridSharedFsTimeData.java]";

        this.lastAccessTime = lastAccessTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridSharedFsTimeData.class, this);
    }
}
