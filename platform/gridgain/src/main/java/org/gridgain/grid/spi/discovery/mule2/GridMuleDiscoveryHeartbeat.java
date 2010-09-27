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

package org.gridgain.grid.spi.discovery.mule2;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Helper class that simplifies heartbeat request data manipulation. When node wakes up
 * it immediately begins to start sending heartbeats. Sending heartbeats is the only
 * way to notify another node that this one is still alive.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMuleDiscoveryHeartbeat implements Serializable {
    /** Node unique identifier. */
    private final UUID nodeId;

    /** Flag indicates whether node leaving grid or not. */
    private boolean leaving = false;

    /** Node inbound endpoint uri. */
    private final String handshakeUri;

    /** Time when node woke up. */
    private final long startTime;

    /** */
    @GridToStringExclude
    private GridNodeMetrics metrics = null;

    /**
     * Creates new instance of helper based on node characteristics.
     *
     * @param nodeId    Local node identifier.
     * @param handshakeUri Inbound endpoint uri.
     * @param startTime Node wake-up time.
     */
    GridMuleDiscoveryHeartbeat(UUID nodeId, String handshakeUri, long startTime) {
        assert nodeId != null : "ASSERTION [line=62, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoveryHeartbeat.java]";
        assert handshakeUri != null : "ASSERTION [line=63, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoveryHeartbeat.java]";
        assert startTime > 0 : "ASSERTION [line=64, file=src/java/org/gridgain/grid/spi/discovery/mule2/GridMuleDiscoveryHeartbeat.java]";

        this.nodeId = nodeId;
        this.handshakeUri = handshakeUri;
        this.startTime = startTime;
    }

    /**
     * Gets node identifier.
     *
     * @return Node identifier.
     */
    UUID getNodeId() {
        return nodeId;
    }

    /**
     * Gets node state.
     *
     * @return <tt>true</tt> if node is leaving grid and <tt>false</tt> otherwise.
     */
    boolean isLeaving() {
        return leaving;
    }

    /**
     * Sets node state in heartbeat data to <tt>LEFT</tt>.
     *
     * @param leaving <tt>true</tt> if node is leaving grid and <tt>false</tt> otherwise.
     */
    void setLeaving(boolean leaving) {
        this.leaving = leaving;
    }

    /**
     * Gets inbound endpoint uri for handshake.
     *
     * @return Inbound endpoint uri.
     */
    String getHandshakeUri() {
        return handshakeUri;
    }

    /**
     * Gets node wake-up time.
     *
     * @return Wake-up time.
     */
    long getStartTime() {
        return startTime;
    }

    /**
     * Gets grid node metrics.
     *
     * @return Grid node metrics.
     */
    GridNodeMetrics getMetrics() {
        return metrics;
    }

    /**
     * Sets grid node metrics.
     *
     * @param metrics Grid node metrics.
     */
    void setMetrics(GridNodeMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMuleDiscoveryHeartbeat.class, this);
    }
}
