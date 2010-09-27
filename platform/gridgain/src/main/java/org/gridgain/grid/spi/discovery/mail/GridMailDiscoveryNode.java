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
import org.gridgain.grid.spi.discovery.*;

/**
 * Represents local or remote node and its attributes. Discovery SPI use this
 * description to check node status (alive/failed), keep local and remote node
 * attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMailDiscoveryNode implements GridNode {
    /** */
    private final transient Object mux = new Object();

    /** Node Id. */
    private final UUID id;

    /** Email 'From' address. */
    private final String fromAddr;

    /** Node start time. */
    private final long startTime;

    /** Last heartbeat time. */
    private long lastHeartbeat = System.currentTimeMillis();

    /** Node attributes. */
    @GridToStringExclude
    private Map<String, Serializable> attrs = null;

    /** Node state. */
    private GridMailDiscoveryNodeState state = null;

    /** Node metrics. */
    @GridToStringExclude
    private GridNodeMetrics metrics = null;

    /** Metrics provider. */
    @GridToStringExclude
    private final transient GridDiscoveryMetricsProvider metricsProvider;

    /**
     * Creates new node instance.
     *
     * @param id Node Id.
     * @param fromAddr Email 'From' address.
     * @param startTime Node start time.
     * @param state Node state.
     * @param metrics Node metrics.
     * @param metricsProvider Metrics provider.
     */
    GridMailDiscoveryNode(UUID id, String fromAddr, long startTime, GridMailDiscoveryNodeState state,
        GridNodeMetrics metrics, GridDiscoveryMetricsProvider metricsProvider) {
        assert id != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryNode.java]";
        assert fromAddr != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryNode.java]";
        assert startTime > 0 : "ASSERTION [line=83, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryNode.java]";
        assert state != null : "ASSERTION [line=84, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryNode.java]";
        assert metrics != null || metricsProvider != null : "ASSERTION [line=85, file=src/java/org/gridgain/grid/spi/discovery/mail/GridMailDiscoveryNode.java]";

        this.id = id;
        this.fromAddr = fromAddr;
        this.startTime = startTime;
        this.state = state;
        this.metrics = metrics;
        this.metricsProvider = metricsProvider;
    }

    /**
     * Gets time when SPI was started.
     *
     * @return Time in milliseconds.
     */
    long getStartTime() {
        return startTime;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public String getPhysicalAddress() {
        return fromAddr;
    }

    /**
     * Gets node state.
     *
     * @return Node state.
     * @see GridMailDiscoveryNodeState
     */
    GridMailDiscoveryNodeState getState() {
        synchronized (mux) {
            return state;
        }
    }

    /**
     * Sets node state.
     *
     * @param state Node state.
     */
    void setState(GridMailDiscoveryNodeState state) {
        synchronized (mux) {
            this.state = state;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getAttribute(String name) {
        synchronized (mux) {
            return (T) attrs.get(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Serializable> getAttributes() {
        synchronized (mux) {
            return new HashMap<String, Serializable>(attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNodeMetrics getMetrics() {
        if (metricsProvider != null) {
            return metricsProvider.getMetrics();
        }

        synchronized (mux) {
            return metrics;
        }
    }

    /**
     * Gets last heartbeat time.
     *
     * @return Time in milliseconds.
     */
    long getLastHeartbeat() {
        synchronized (mux) {
            return lastHeartbeat;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof GridMailDiscoveryNode == true &&
            id.equals(((GridMailDiscoveryNode)obj).id) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Update last heartbeat time.
     *
     * @param metrics Node metrics.
     */
    void onHeartbeat(GridNodeMetrics metrics) {
        synchronized (mux) {
            lastHeartbeat = System.currentTimeMillis();

            this.metrics = metrics;
        }
    }

    /**
     * This method is called to initialize local node.
     *
     * @param attrs Local node attributes.
     */
    void setAttributes(Map<String, Serializable> attrs) {
        synchronized (mux) {
            this.attrs = new HashMap<String, Serializable>(attrs);

            state = GridMailDiscoveryNodeState.READY;
        }
    }

    /**
     * Update node with received node attributes.
     *
     * @param attrs Node attributes.
     */
    void onDataReceived(Map<String, Serializable> attrs) {
        synchronized (mux) {
            this.attrs = new HashMap<String, Serializable>(attrs);

            state = GridMailDiscoveryNodeState.READY;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMailDiscoveryNode.class, this);
    }
}
