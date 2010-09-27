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

package org.gridgain.grid.spi.discovery.multicast;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Multicast discovery message.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMulticastDiscoveryMessage implements Serializable {
    /** */
    private final GridMulticastDiscoveryMessageType type;

    /** Sender node ID. */
    private final UUID nodeId;

    /** */
    private final InetAddress addr;

    /** */
    private final int port;

    /** Map of node attributes. */
    private final Map<String, Serializable> attrs;

    /** */
    private final long startTime;

    /** */
    @GridToStringExclude
    private final GridNodeMetrics metrics;

    /**
     *
     * @param type Message type.
     */
    GridMulticastDiscoveryMessage(GridMulticastDiscoveryMessageType type) {
        assert type != null : "ASSERTION [line=64, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";

        this.type = type;

        nodeId = null;
        attrs = null;
        addr = null;
        port = -1;
        startTime = -1;
        metrics = null;
    }

    /**
     * @param type Message type.
     * @param nodeId FIXDOC
     * @param addr FIXDOC
     * @param port FIXDOC
     * @param attrs Node attributes.
     * @param startTime Start time.
     * @param metrics FIXDOC
     */
    GridMulticastDiscoveryMessage(GridMulticastDiscoveryMessageType type, UUID nodeId, InetAddress addr,
        int port, Map<String, Serializable> attrs, long startTime, GridNodeMetrics metrics) {
        assert type != null : "ASSERTION [line=87, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";
        assert nodeId != null : "ASSERTION [line=88, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";
        assert addr != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";
        assert port > 0 : "ASSERTION [line=90, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";
        assert attrs != null : "ASSERTION [line=91, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";
        assert startTime > 0 : "ASSERTION [line=92, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";
        assert metrics != null : "ASSERTION [line=93, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryMessage.java]";

        this.type = type;
        this.nodeId = nodeId;
        this.attrs = attrs;
        this.addr = addr;
        this.port = port;
        this.startTime = startTime;
        this.metrics = metrics;
    }

    /**
     * Gets message data.
     *
     * @return Message data.
     */
    Map<String, Serializable> getAttributes() {
        return attrs;
    }

    /**
     * Gets message type.
     *
     * @return Message type.
     */
    GridMulticastDiscoveryMessageType getType() {
        return type;
    }

    /**
     * Gets sender node ID.
     *
     * @return Sender node ID.
     */
    UUID getNodeId() {
        return nodeId;
    }

    /**
     * Gets sender address.
     *
     * @return Sender address.
     */
    InetAddress getAddress() {
        return addr;
    }

    /**
     * Gets sender port.
     *
     * @return Sender port.
     */
    int getPort() {
        return port;
    }

    /**
     * Gets sender node start time.
     *
     * @return sender node start time.
     */
    long getStartTime() {
        return startTime;
    }

    /**
     * Gets node metrics.
     *
     * @return Node metrics.
     */
    GridNodeMetrics getMetrics() {
        return metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMulticastDiscoveryMessage.class, this);
    }
}
