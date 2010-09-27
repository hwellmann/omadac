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

package org.gridgain.grid.spi.discovery.jgroups;

import java.io.*;
import java.util.*;
import org.jgroups.stack.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of discovery message. This wrapper keeps message type, source node Id
 * and destination address inside.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJgroupsDiscoveryMessage implements Serializable {
    /** Source node Id. */
    private final UUID id;

    /** List of source node attributes. */
    private final Map<String, Serializable> attrs;

    /** Discovery message type. */
    private final GridJgroupsDiscoveryMessageType type;

    /** Remote (destination) node address. */
    private final IpAddress addr;

    /** Node metrics. */
    private final GridNodeMetrics metrics;

    /**
     * Creates instance of message wrapper. This message is for the certain node.
     *
     * @param type Message type.
     * @param id Source node UID this message is sent from.
     * @param addr Destination node address.
     * @param attrs Source node attributes.
     * @param metrics Source node metrics.
     */
    GridJgroupsDiscoveryMessage(GridJgroupsDiscoveryMessageType type, UUID id, IpAddress addr,
        Map<String, Serializable> attrs, GridNodeMetrics metrics) {
        assert id != null : "ASSERTION [line=64, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryMessage.java]";
        assert type != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryMessage.java]";
        assert addr != null : "ASSERTION [line=66, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryMessage.java]";

        this.type = type;
        this.id = id;
        this.attrs = attrs;
        this.addr = addr;
        this.metrics = metrics;
    }

    /**
     * Gets local node UID this message is sent from.
     *
     * @return Source node UID.
     */
    UUID getId() {
        return id;
    }

    /**
     * Gets message type.
     *
     * @return Message type.
     * @see GridJgroupsDiscoveryMessageType
     */
    GridJgroupsDiscoveryMessageType getType() {
        return type;
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
    IpAddress getIpAddress() {
        return addr;
    }

    /**
     * Gets node metrics.
     *
     * @return the metrics
     */
    GridNodeMetrics getMetrics() {
        return metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJgroupsDiscoveryMessage.class, this);
    }
}
