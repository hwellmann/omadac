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
import org.gridgain.grid.spi.discovery.*;

/**
 * Represents local or remote node and its attributes. Discovery SPI use this
 * description to check node status (alive/failed), keep local and remote node
 * attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMuleDiscoveryNode implements GridNode {
    /** Node unique identifier. */
    private final UUID id;

    /** Node state. */
    private GridMuleDiscoveryNodeState state = GridMuleDiscoveryNodeState.NEW;

    /** Node attributes. */
    @GridToStringExclude
    private Map<String, Serializable> attrs = null;

    /** Node wake-up time. */
    private final long startTime;

    /** Inbound endpoint uri for handshake. */
    private final String handshakeUri;

    /** Time when node sent heartbeat request last. */
    private volatile long lastHeartbeat = System.currentTimeMillis();

    /** */
    @GridToStringExclude
    private volatile GridNodeMetrics metrics = null;

    /** */
    @GridToStringExclude
    private final transient GridDiscoveryMetricsProvider metricsProvider;

    /** */
    private final transient Object mux = new Object();

    /**
     * Creates new node instance.
     *
     * @param id Node id.
     * @param startTime Node wake-up time.
     * @param state Node status.
     * @param handshakeUri Node inbound endpoint uri for handshake.
     * @param metrics Grid node metrics.
     */
    GridMuleDiscoveryNode(UUID id, long startTime, GridMuleDiscoveryNodeState state, String handshakeUri,
        GridNodeMetrics metrics) {
        assert id != null : "ASSERTION [line=80, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert startTime > 0 : "ASSERTION [line=81, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert state != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert handshakeUri != null : "ASSERTION [line=83, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert metrics != null : "ASSERTION [line=84, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";

        this.id = id;
        this.startTime = startTime;
        this.state = state;
        this.handshakeUri = handshakeUri;
        this.metrics = metrics;

        metricsProvider = null;
    }

    /**
     * Creates new node instance.
     *
     * @param id Node id.
     * @param startTime Node wake-up time.
     * @param state Node status.
     * @param handshakeUri Node inbound endpoint uri for handshake.
     * @param metricsProvider Grid node metrics provider.
     */
    GridMuleDiscoveryNode(UUID id, long startTime, GridMuleDiscoveryNodeState state, String handshakeUri,
        GridDiscoveryMetricsProvider metricsProvider) {
        assert id != null : "ASSERTION [line=106, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert startTime > 0 : "ASSERTION [line=107, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert state != null : "ASSERTION [line=108, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert handshakeUri != null : "ASSERTION [line=109, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert metricsProvider != null : "ASSERTION [line=110, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";

        this.id = id;
        this.startTime = startTime;
        this.state = state;
        this.handshakeUri = handshakeUri;
        this.metricsProvider = metricsProvider;
    }

    /**
     * Creates new node instance.
     *
     * @param id Node id.
     * @param startTime Node wake-up time.
     * @param state Node status.
     * @param attrs Node attributes.
     * @param handshakeUri Node inbound endpoint uri for handshake.
     * @param metrics Grid node metrics.
     */
    GridMuleDiscoveryNode(UUID id, long startTime, GridMuleDiscoveryNodeState state, Map<String,
        Serializable> attrs, String handshakeUri, GridNodeMetrics metrics) {
        assert id != null : "ASSERTION [line=131, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert startTime > 0 : "ASSERTION [line=132, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert state != null : "ASSERTION [line=133, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert attrs != null : "ASSERTION [line=134, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert handshakeUri != null : "ASSERTION [line=135, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";
        assert metrics != null : "ASSERTION [line=136, file=src/java/org/gridgain/grid/spi/discovery/mule/GridMuleDiscoveryNode.java]";

        this.id = id;
        this.state = state;
        this.attrs = attrs;
        this.startTime = startTime;
        this.handshakeUri = handshakeUri;
        this.metrics = metrics;

        metricsProvider = null;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets node state.
     *
     * @param state Node state.
     */
    void setState(GridMuleDiscoveryNodeState state) {
        this.state = state;
    }

    /**
     * Gets node state.
     *
     * @return Node state.
     */
    GridMuleDiscoveryNodeState getState() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public String getPhysicalAddress() {
        return handshakeUri;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getAttribute(String name) {
        synchronized (mux) {
            return (T) (attrs != null ? attrs.get(name) : null);
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
        if (metricsProvider == null) {
            return metrics;
        }

        return metricsProvider.getMetrics();
    }

    /**
     * Sets node attributes.
     *
     * @param attrs Node attributes.
     */
    void setAttributes(Map<String, Serializable> attrs) {
        synchronized (mux) {
            this.attrs = new HashMap<String, Serializable>();

            if (attrs != null) {
                this.attrs = new HashMap<String, Serializable>(attrs);
            }

            // Seal it.
            this.attrs = Collections.unmodifiableMap(this.attrs);
        }
    }

    /**
     * Gets time when heartbeat request from this node came last.
     *
     * @return Time in milliseconds.
     */
    long getLastHeartbeat() {
        return lastHeartbeat;
    }

    /**
     * This method is called when heartbeat request from remote node represented
     * by this object comes.
     *
     * @param metrics Node metrics.
     */
    void onHeartbeat(GridNodeMetrics metrics) {
        lastHeartbeat = System.currentTimeMillis();

        this.metrics = metrics;
    }

    /**
     * Gets node wake-up time.
     *
     * @return Time in milliseconds.
     */
    long getStartTime() {
        return startTime;
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof GridMuleDiscoveryNode == true &&
            id.equals(((GridMuleDiscoveryNode)obj).id) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMuleDiscoveryNode.class, this);
    }
}
