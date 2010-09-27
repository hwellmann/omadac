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
import org.gridgain.grid.spi.discovery.*;
import static org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoveryNodeState.*;

/**
 * Class represents single node in the grid. Every node has unique identifier, state , attributes,
 * IP address and port.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMulticastDiscoveryNode implements GridNode {
    /** Node unique identifier. */
    private final UUID id;

    /** Node state. */
    private GridMulticastDiscoveryNodeState state = NEW;

    /** Node attributes. */
    @GridToStringExclude
    private Map<String, Serializable> attrs = null;

    /** Time when node sent heartbeat request last. */
    private long lastHeartbeat = System.currentTimeMillis();

    /** Node IP address. */
    private InetAddress addr = null;

    /** Node port number. */
    private int tcpPort = 0;

    /** Node wake-up time. */
    private long startTime = -1;

    /** */
    @GridToStringExclude
    private GridNodeMetrics metrics = null;

    /** */
    @GridToStringExclude
    private final transient GridDiscoveryMetricsProvider metricsProvider;

    /** */
    private final transient Object mux = new Object();

    /**
     * Creates new node instance.
     *
     * @param id Node unique identifier.
     * @param addr Node IP address.
     * @param tcpPort Node port number.
     * @param startTime Node wake-up time.
     * @param metrics FIXDOC
     */
    GridMulticastDiscoveryNode(UUID id, InetAddress addr, int tcpPort, long startTime, GridNodeMetrics metrics) {
        assert id != null : "ASSERTION [line=83, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";
        assert addr != null : "ASSERTION [line=84, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";
        assert tcpPort > 0 : "ASSERTION [line=85, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";
        assert tcpPort <= 0xffff : "ASSERTION [line=86, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";

        this.id = id;
        this.addr = addr;
        this.tcpPort = tcpPort;
        this.startTime = startTime;
        this.metrics = metrics;

        metricsProvider = null;

        state = NEW;
    }

    /**
     * Creates new node instance.
     *
     * @param id Node unique identifier.
     * @param addr Node IP address.
     * @param tcpPort Node port number.
     * @param startTime Node wake-up time.
     * @param metricsProvider FIXDOC
     */
    GridMulticastDiscoveryNode(UUID id, InetAddress addr, int tcpPort, long startTime,
        GridDiscoveryMetricsProvider metricsProvider) {
        assert id != null : "ASSERTION [line=110, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";
        assert addr != null : "ASSERTION [line=111, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";
        assert tcpPort > 0 : "ASSERTION [line=112, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";
        assert tcpPort <= 0xffff : "ASSERTION [line=113, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";
        assert metricsProvider != null : "ASSERTION [line=114, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";

        this.id = id;
        this.addr = addr;
        this.tcpPort = tcpPort;
        this.startTime = startTime;
        this.metricsProvider = metricsProvider;

        // State new because node does not have attributes yet.
        state = NEW;
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
        return addr.getHostAddress();
    }

    /**
     * Gets node local IP address.
     *
     * @return IP address.
     */
    InetAddress getInetAddress() {
        return addr;
    }

    /**
     * Gets node local port number.
     *
     * @return Port number.
     */
    int getTcpPort() {
        return tcpPort;
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
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getAttribute(String name) {
        assert getState() == READY || getState() == LEFT : "ASSERTION [line=172, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]. " + "Invalid state: " + getState();

        return (T)attrs.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public GridNodeMetrics getMetrics() {
        if (metricsProvider == null) {
            synchronized (mux) {
                assert metrics != null : "ASSERTION [line=183, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";

                return metrics;
            }
        }

        return metricsProvider.getMetrics();
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Serializable> getAttributes() {
        assert getState() == READY || getState() == LEFT : "ASSERTION [line=196, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]. " + "Invalid state: " + getState();

        return attrs;
    }

    /**
     * This method is called to initialize local node.
     *
     * @param attrs Local node attributes.
     */
    void setAttributes(Map<String, Serializable> attrs) {
        createAttributes(attrs);

        synchronized (mux) {
            state = READY;
        }
    }

    /**
     * Method creates internal map of attributes and fills it with given mapNode.
     *
     * @param attrs Initial attributes.
     */
    private void createAttributes(Map<String, Serializable> attrs) {
        assert this.attrs == null : "ASSERTION [line=220, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryNode.java]";

        this.attrs = new HashMap<String, Serializable>();

        if (attrs != null) {
            this.attrs = new HashMap<String, Serializable>(attrs);
        }

        // Seal it.
        this.attrs = Collections.unmodifiableMap(this.attrs);
    }

    /**
     * This method is called when heartbeat request from remote node represented
     * by this object comes.
     *
     * @param metrics FIXDOC
     */
    void onHeartbeat(GridNodeMetrics metrics) {
        synchronized (mux) {
            this.metrics = metrics;

            lastHeartbeat = System.currentTimeMillis();
        }
    }

    /**
     * Gets time when heartbeat request from this node came last.
     *
     * @return Time in milliseconds.
     */
    public long getLastHeartbeat() {
        synchronized (mux) {
            return lastHeartbeat;
        }
    }

    /**
     * Method is called when remote node represented by this object changes its state to <tt>LEFT</tt>.
     */
    void onLeft() {
        synchronized (mux) {
            state = LEFT;

            lastHeartbeat = System.currentTimeMillis();
        }
    }

    /**
     * Called for failed nodes.
     */
    void onFailed() {
        synchronized (mux) {
            state = LEFT;
        }
    }

    /**
     * Gets current node state.
     *
     * @return Node state.
     * @see GridMulticastDiscoveryNodeState
     */
    GridMulticastDiscoveryNodeState getState() {
        synchronized (mux) {
            return state;
        }
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof GridMulticastDiscoveryNode && id.equals(((GridMulticastDiscoveryNode)obj).id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMulticastDiscoveryNode.class, this);
    }
}
