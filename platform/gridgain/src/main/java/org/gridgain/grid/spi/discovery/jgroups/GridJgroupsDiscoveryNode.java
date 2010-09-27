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
import java.net.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Represents local or remote node and its attributes. Discovery SPI use this
 * description to check node status (alive/failed), keep local and remote node
 * attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJgroupsDiscoveryNode implements GridNode {
    /** Node Id. */
    private UUID id = null;

    /** JGroups channel local IP address. */
    private InetAddress addr = null;

    /** JGroups channel local port number. */
    private int port = -1;

    /** Flag for suspected nodes. */
    private boolean suspect = false;

    /** Internal node state. */
    private GridJgroupsDiscoveryNodeState status = null;

    /** Node attributes. */
    @GridToStringExclude
    private Map<String, Serializable> attrs = null;

    /** */
    @GridToStringExclude
    private GridNodeMetrics metrics = null;

    /** */
    @GridToStringExclude
    private final transient GridDiscoveryMetricsProvider metricsProvider;

    /** */
    private final transient Object mux = new Object();

    /**
     * Creates new node instance with data from message.
     *
     * @param msg Message being received.
     * @param metrics Node metrics.
     * @see GridJgroupsDiscoveryMessage
     */
    GridJgroupsDiscoveryNode(GridJgroupsDiscoveryMessage msg, GridNodeMetrics metrics) {
        id = msg.getId();
        addr = msg.getIpAddress().getIpAddress();
        port = msg.getIpAddress().getPort();

        this.metrics = metrics;

        createAttributes(msg.getAttributes());

        status = GridJgroupsDiscoveryNodeState.NEW_HAS_DATA;

        metricsProvider = null;
    }

    /**
     * Create instance of node description with given IP address and port number.
     *
     * @param addr IP address.
     * @param port Port number.
     */
    GridJgroupsDiscoveryNode(InetAddress addr, int port) {
        assert addr != null : "ASSERTION [line=98, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";
        assert port > 0 : "ASSERTION [line=99, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

        this.addr = addr;
        this.port = port;

        status = GridJgroupsDiscoveryNodeState.NEW_NO_DATA;

        metricsProvider = null;
    }

    /**
     * Creates new node instance with data from message.
     *
     * @param msg Message being received.
     * @param metricsProvider Node metrics provider.
     * @see GridJgroupsDiscoveryMessage
     */
    GridJgroupsDiscoveryNode(GridJgroupsDiscoveryMessage msg, GridDiscoveryMetricsProvider metricsProvider) {
        id = msg.getId();
        addr = msg.getIpAddress().getIpAddress();
        port = msg.getIpAddress().getPort();

        this.metricsProvider = metricsProvider;

        createAttributes(msg.getAttributes());

        status = GridJgroupsDiscoveryNodeState.NEW_HAS_DATA;
    }

    /**
     * Create instance of node description with given IP address and port number.
     *
     * @param addr IP address.
     * @param port Port number.
     * @param metricsProvider Node metrics provider.
     */
    GridJgroupsDiscoveryNode(InetAddress addr, int port, GridDiscoveryMetricsProvider metricsProvider) {
        assert addr != null : "ASSERTION [line=136, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";
        assert port > 0 : "ASSERTION [line=137, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

        this.addr = addr;
        this.port = port;
        this.metricsProvider = metricsProvider;

        status = GridJgroupsDiscoveryNodeState.NEW_NO_DATA;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getAttribute(String name) {
        assert isReady() == true : "ASSERTION [line=151, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

        return (T)attrs.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Serializable> getAttributes() {
        assert isReady() == true : "ASSERTION [line=160, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    public String getPhysicalAddress() {
        return addr.getHostAddress();
    }

    /**
     * {@inheritDoc}
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets node IP address.
     *
     * @return IP address.
     */
    InetAddress getAddress() {
        return addr;
    }

    /**
     * Gets node port number.
     *
     * @return Port number.
     */
    int getPort() {
        return port;
    }

    /**
     * Update node with received data.
     *
     * @param id Node Id.
     * @param rmtAttrs Node attributes.
     */
    void onDataReceived(UUID id, Map<String, Serializable> rmtAttrs) {
        assert attrs == null : "ASSERTION [line=204, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

        createAttributes(rmtAttrs);

        synchronized (mux) {
            assert status == GridJgroupsDiscoveryNodeState.NEW_NO_DATA : "ASSERTION [line=209, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

            this.id = id;

            status = GridJgroupsDiscoveryNodeState.READY;
        }
    }

    /**
     * Creates node attributes object based on data from <tt>attrs</tt>.
     *
     * @param attrs Node attributes.
     */
    private void createAttributes(Map<String, Serializable> attrs) {
        assert this.attrs == null : "ASSERTION [line=223, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

        this.attrs = new HashMap<String, Serializable>();

        if (attrs != null) {
            this.attrs = new HashMap<String, Serializable>(attrs);
        }

        // Seal it.
        this.attrs = Collections.unmodifiableMap(this.attrs);
    }

    /**
     * Change node status to ready.
     *
     */
    void onViewReceived() {
        synchronized (mux) {
            assert status == GridJgroupsDiscoveryNodeState.NEW_HAS_DATA : "ASSERTION [line=241, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

            status = GridJgroupsDiscoveryNodeState.READY;
        }
    }

    /**
     * Gets node status.
     *
     * @return Node status.
     */
    GridJgroupsDiscoveryNodeState getStatus() {
        synchronized (mux) {
            return status;
        }
    }

    /**
     * Gets node 'suspect' flag.
     *
     * @return Node flag.
     */
    boolean isSuspect() {
        synchronized (mux) {
            return suspect;
        }
    }

    /**
     * Set's node 'suspect' flag.
     *
     * @param suspect This node's suspect flag.
     */
    void setSuspect(boolean suspect) {
        synchronized (mux) {
            this.suspect = suspect;
        }
    }

    /**
     * Gets node state whether it's ready or not.
     *
     * @return Node state.
     */
    boolean isReady() {
        synchronized (mux) {
            return status == GridJgroupsDiscoveryNodeState.READY;
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNodeMetrics getMetrics() {
        if (metricsProvider == null) {
            synchronized (mux) {
                assert metrics != null : "ASSERTION [line=297, file=src/java/org/gridgain/grid/spi/discovery/jgroups/GridJgroupsDiscoveryNode.java]";

                return metrics;
            }
        }

        return metricsProvider.getMetrics();
    }

    /**
     * Update node metrics.
     *
     * @param metrics Up-to-date node metrics.
     */
    void onMetricsReceived(GridNodeMetrics metrics) {
        synchronized (mux) {
            this.metrics = metrics;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return addr.hashCode() ^ port;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof GridJgroupsDiscoveryNode == true) {
            GridJgroupsDiscoveryNode node = (GridJgroupsDiscoveryNode) obj;

            return addr.equals(node.getAddress()) && port == node.getPort();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJgroupsDiscoveryNode.class, this);
    }
}
