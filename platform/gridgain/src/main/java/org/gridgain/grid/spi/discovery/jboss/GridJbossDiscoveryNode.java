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

package org.gridgain.grid.spi.discovery.jboss;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.jgroups.stack.*;

/**
 * Represents local or remote node and its attributes. Discovery SPI use this
 * description to check node status (alive/failed), keep local and remote node
 * attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJbossDiscoveryNode implements GridNode {
    /** Jboss identifier. */
    private byte[] jbossId = null;

    /** Node status. */
    private GridJbossDiscoveryNodeState status = null;

    /** */
    private final transient Object mux = new Object();

    /** Map of node attributes. */
    @GridToStringExclude
    private Map<String, Serializable> attrs = new HashMap<String, Serializable>();

    /** */
    private int hash = 0;

    /** Node IP address. */
    private InetAddress addr = null;

    /** Node unique Id. */
    private UUID id = null;

    /** Port number. */
    private int port = -1;

    /** */
    @GridToStringExclude
    private GridNodeMetrics metrics = null;

    /** */
    @GridToStringExclude
    private final transient GridDiscoveryMetricsProvider metricsProvider;

    /**
     * Create instance of node description with given IP address and metrics provider.
     *
     * @param addr Node IP address.
     * @param metricsProvider Local node metrics provider.
     */
    GridJbossDiscoveryNode(IpAddress addr, GridDiscoveryMetricsProvider metricsProvider) {
        assert addr != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoveryNode.java]";

        this.addr = addr.getIpAddress();
        this.metricsProvider = metricsProvider;

        port = addr.getPort();

        byte[] addrBytes = this.addr.getAddress();

        jbossId = new byte[addrBytes.length + 4/* port */];

        int off = 0;

        System.arraycopy(addrBytes, 0, jbossId, off, addrBytes.length);

        off += addrBytes.length;

        // Convert port to bytes.
        jbossId[off++] = (byte)(0xff & port >>> 24);
        jbossId[off++] = (byte)(0xff & port >>> 16);
        jbossId[off++] = (byte)(0xff & port >>> 8);
        jbossId[off] = (byte)(0xff & port);

        status = GridJbossDiscoveryNodeState.NEW_NO_DATA;

        // Assign random UUID first, it will be replaced then by real.
        id = UUID.randomUUID();
    }

    /**
     * Create instance of node description with given IP address.
     *
     * @param addr Node IP address.
     */
    GridJbossDiscoveryNode(IpAddress addr) {
        this(addr, null);
    }

    /**
     * Create instance of node description with given node data.
     *
     * @param data Node data.
     * @throws UnknownHostException Thrown in case the host cannot be determined from the
     *      address in discovered node data.
     */
    GridJbossDiscoveryNode(GridJbossDiscoveryNodeData data) throws UnknownHostException {
        metricsProvider = null;

        metrics = data.getMetrics();

        jbossId = data.getJBossId();

        int off = 0;

        // Initialize address.
        byte[] addrBytes = new byte[jbossId.length - 4];

        System.arraycopy(jbossId, 0, addrBytes, 0, addrBytes.length);

        off += addrBytes.length;

        addr = InetAddress.getByAddress(addrBytes);

        // Initialize port.
        port = jbossId[off++] << 24 | jbossId[off++] << 16 | jbossId[off++] << 8 | jbossId[off];

        Map<String, Serializable> rmtAttrs = data.getAttributes();

        if (rmtAttrs != null) {
            attrs.putAll(rmtAttrs);
        }

        status = GridJbossDiscoveryNodeState.NEW_HAS_DATA;

        id = data.getId();
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
     * Gets JBoss Id.
     *
     * @return JBoss Id.
     */
    public byte[] getJBossId() {
        return jbossId;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getAttribute(String name) {
        return (T)attrs.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Serializable> getAttributes() {
        return new HashMap<String, Serializable>(attrs);
    }

    /**
     * Gets node state whether it's ready or not.
     *
     * @return Node state.
     */
    boolean isReady() {
        synchronized (mux) {
            return status == GridJbossDiscoveryNodeState.READY;
        }
    }

    /**
     * Gets node state.
     *
     * @return Node state.
     */
    public GridJbossDiscoveryNodeState getStatus() {
        return status;
    }

    /**
     * Update node with received data.
     *
     * @param data Node data.
     */
    void onDataReceived(GridJbossDiscoveryNodeData data) {
        assert Arrays.equals(data.getJBossId(), jbossId) == true : "ASSERTION [line=223, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoveryNode.java]";

        synchronized (mux) {
            // If there is no provider set metrics.
            if (metricsProvider == null) {
                metrics = data.getMetrics();
            }

            id = data.getId();

            Map<String, Serializable> rmtAttrs = data.getAttributes();

            if (rmtAttrs != null) {
                attrs.putAll(rmtAttrs);
            }

            status = GridJbossDiscoveryNodeState.READY;
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNodeMetrics getMetrics() {
        if (metricsProvider == null) {
            synchronized (mux) {
                assert metrics != null : "ASSERTION [line=249, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoveryNode.java]";

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
    public boolean equals(Object obj) {
        return this == obj || obj instanceof GridJbossDiscoveryNode == true && Arrays.equals(jbossId,
            ((GridJbossDiscoveryNode)obj).jbossId) == true;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJbossDiscoveryNode.class, this,
            "jbossId", GridUtils.byteArray2HexString(jbossId));
    }
}
