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

package org.gridgain.grid.spi.discovery.mule;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMuleDiscoveryMessage implements Serializable {
    /** */
    private final UUID fromId;

    /** */
    private final UUID toId;

    /** */
    private final GridMuleDiscoveryMessageType type;

    /** Map of node attributes. */
    private final Map<String, Serializable> attrs;

    /** */
    private final long startTime;

    /** */
    private final String handshakeUri;

    /** */
    @GridToStringExclude
    private final GridNodeMetrics metrics;

    /**
     *
     * @param fromId Source ID.
     * @param toId Destination ID.
     * @param type Message type.
     * @param attrs Node attributes.
     * @param startTime Start time.
     * @param handshakeUri Handshake URI.
     * @param metrics Node metrics.
     */
    GridMuleDiscoveryMessage(UUID fromId, UUID toId, GridMuleDiscoveryMessageType type, Map<String,
        Serializable> attrs, long startTime, String handshakeUri, GridNodeMetrics metrics) {
        assert fromId != null : "ASSERTION [line=70, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryMessage.java]";
        assert toId != null : "ASSERTION [line=71, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryMessage.java]";
        assert type != null : "ASSERTION [line=72, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryMessage.java]";
        assert startTime > 0 : "ASSERTION [line=73, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryMessage.java]";
        assert handshakeUri != null : "ASSERTION [line=74, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryMessage.java]";

        this.fromId = fromId;
        this.toId = toId;
        this.type = type;
        this.attrs = attrs;
        this.startTime = startTime;
        this.handshakeUri = handshakeUri;
        this.metrics = metrics;
    }

    /**
     *
     * @return FIXDOC
     */
    UUID getFromId() {
        return fromId;
    }

    /**
     *
     * @return FIXDOC
     */
    UUID getToId() {
        return toId;
    }

    /**
     *
     * @return FIXDOC
     */
    GridMuleDiscoveryMessageType getType() {
        return type;
    }

    /**
     *
     * @return FIXDOC
     */
    Map<String, Serializable> getAttributes() {
        return attrs;
    }

    /**
     *
     * @return FIXDOC
     */
    long getStartTime() {
        return startTime;
    }

    /**
     *
     * @return FIXDOC
     */
    String getHandshakeUri() {
        return handshakeUri;
    }

    /**
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
        return GridToStringBuilder.toString(GridMuleDiscoveryMessage.class, this);
    }
}
