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

package org.gridgain.grid.spi.discovery.jms;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of discovery message. This wrapper keeps message type, source node Id
 * and destination address inside.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJmsDiscoveryMessage implements Serializable {
    /** Discovery message type. */
    private GridJmsDiscoveryMessageType msgType;

    /** Source node Id. */
    private final UUID nodeId;

    /** List of source node attributes. */
    private final Map<String, Serializable> attrs;

    /** Remote (destination) node address. */
    private final String addr;

    /** */
    @GridToStringExclude
    private GridNodeMetrics metrics = null;

    /**
     * Creates instance of message wrapper. This message is for the certain node.
     *
     * @param msgType Message type.
     * @param nodeId Source node UID this message is sent from.
     * @param attrs Source node attributes.
     * @param addr Destination node address.
     * @param metrics FIXDOC
     */
    GridJmsDiscoveryMessage(GridJmsDiscoveryMessageType msgType, UUID nodeId, Map<String, Serializable> attrs,
        String addr, GridNodeMetrics metrics) {
        assert msgType != null : "ASSERTION [line=64, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryMessage.java]";
        assert nodeId != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryMessage.java]";

        this.msgType = msgType;
        this.nodeId = nodeId;
        this.attrs = attrs;
        this.addr = addr;
        this.metrics = metrics;
    }

    /**
     * Creates broadcast message.
     *
     * @param msgType Message type.
     * @param nodeId Source UID this message is sent from.
     */
    GridJmsDiscoveryMessage(GridJmsDiscoveryMessageType msgType, UUID nodeId) {
        assert msgType != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryMessage.java]";
        assert nodeId != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryMessage.java]";

        this.msgType = msgType;
        this.nodeId = nodeId;

        attrs = null;
        addr = null;
        metrics = null;
    }

    /**
     * Gets message type.
     *
     * @return Message type.
     */
    GridJmsDiscoveryMessageType getMessageType() {
        return msgType;
    }

    /**
     * Sets message type.
     *
     * @param msgType Message type.
     */
    void setMessageType(GridJmsDiscoveryMessageType msgType) {
        this.msgType = msgType;
    }

    /**
     * Gets local node UID this message is sent from.
     *
     * @return Source node UID.
     */
    UUID getNodeId() {
        return nodeId;
    }

    /**
     * Gets names source node attributes.
     *
     * @return Source node attributes.
     */
    Map<String, Serializable> getAttributes() {
        return attrs;
    }

    /**
     * Gets destination node address.
     *
     * @return Destination node address.
     */
    String getAddress() {
        return addr;
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
     * Sets node metrics.
     *
     * @param metrics Node metrics.
     */
    void setMetrics(GridNodeMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJmsDiscoveryMessage.class, this);
    }
}
