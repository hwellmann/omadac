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

package org.gridgain.grid.spi.discovery.mail;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of discovery message. This wrapper keeps message type, source node Id, destination address
 * and another fields inside.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMailDiscoveryMessage implements Serializable {
    /** Source node Id. */
    private final UUID srcNodeId;

    /** Flag indicates whether it's left or not.*/
    private final boolean leave;

    /** Time when message was sent. */
    private final long sendTime;

    /** Time when SPI was started. */
    private final long nodeStartTime;

    /** Node Id's requested to reply for ping. */
    private final Collection<UUID> pingedNodes;

    /** Node Id's requested to reply with attributes. */
    private final Collection<UUID> attrNodes;

    /** List of source node attributes. */
    private final Map<String, Serializable> nodeAttrs;

    /** Email message 'From' attribute. */
    private final String fromAddr;

    /** Grid node metrics. */
    @GridToStringExclude
    private final GridNodeMetrics metrics;

    /**
     * Creates message.
     *
     * @param srcNodeId Source node Id.
     * @param leave Node left or not.
     * @param pingedNodes Node Id's requested to reply for ping.
     * @param attrNodes Node Id's requested to reply with attributes.
     * @param nodeAttrs Node attributes.
     * @param fromAddr Email address 'From' field.
     * @param sendTime Time when message was sent.
     * @param nodeStartTime Time when SPI was started.
     * @param metrics Grid node metrics.
     */
    GridMailDiscoveryMessage(UUID srcNodeId, boolean leave, Collection<UUID> pingedNodes,
        Collection<UUID> attrNodes, Map<String, Serializable> nodeAttrs, String fromAddr,
        long sendTime, long nodeStartTime, GridNodeMetrics metrics) {
        assert srcNodeId != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryMessage.java]";
        assert leave == false || attrNodes == null && pingedNodes == null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryMessage.java]";
        assert sendTime > 0 : "ASSERTION [line=83, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryMessage.java]";
        assert nodeStartTime > 0 : "ASSERTION [line=84, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryMessage.java]";
        assert metrics != null : "ASSERTION [line=85, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryMessage.java]";

        this.srcNodeId = srcNodeId;
        this.leave = leave;
        this.pingedNodes = pingedNodes;
        this.attrNodes = attrNodes;
        this.nodeAttrs = nodeAttrs;
        this.fromAddr = fromAddr;
        this.sendTime = sendTime;
        this.nodeStartTime = nodeStartTime;
        this.metrics = metrics;
    }

    /**
     * Gets node state.
     *
     * @return <tt>true</tt> if node is leaving grid and <tt>false</tt> otherwise.
     */
    boolean isLeave() {
        return leave;
    }

    /**
     * Email address 'From' field.
     *
     * @return Email address 'From' field.
     */
    String getFromAddress() {
        return fromAddr;
    }

    /**
     * Gets node identifier.
     *
     * @return Node identifier.
     */
    UUID getSourceNodeId() {
        return srcNodeId;
    }

    /**
     * Gets collection of node Id's requested to reply for ping.
     *
     * @return Collection of node Id's.
     */
    Collection<UUID> getPingedNodes() {
        return pingedNodes;
    }

    /**
     * Gets collection of node Id's requested to reply with attributes.
     *
     * @return Collection of node Id's.
     */
    Collection<UUID> getAttributeNodes() {
        return attrNodes;
    }

    /**
     * Gets node attributes.
     *
     * @return Node attributes.
     */
    Map<String, Serializable> getAttributes() {
        return nodeAttrs;
    }

    /**
     * Gets time when message was sent.
     *
     * @return Time in milliseconds.
     */
    long getSendTime() {
        return sendTime;
    }

    /**
     * Gets time when SPI was started.
     *
     * @return Time in milliseconds.
     */
    long getNodeStartTime() {
        return nodeStartTime;
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
        return GridToStringBuilder.toString(GridMailDiscoveryMessage.class, this);
    }
}
