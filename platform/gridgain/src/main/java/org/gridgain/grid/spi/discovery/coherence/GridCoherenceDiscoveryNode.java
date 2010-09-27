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

package org.gridgain.grid.spi.discovery.coherence;

import java.io.*;
import java.net.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Represents local or remote node and its attributes. Discovery SPI use this
 * description to check node status (alive/failed), keep local and remote node
 * attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridCoherenceDiscoveryNode implements GridNode {
    /** Node IP address. */
    private final InetAddress addr;

    /** Cluster member identifier. */
    private final byte[] mbrUid;

    /** Node attributes. */
    @GridToStringExclude
    private Map<String, Serializable> attrs = new HashMap<String, Serializable>();

    /** Node unique Id. */
    private UUID id = null;

    /** Hash value. */
    private int hash = 0;

    /** */
    @GridToStringExclude
    private GridNodeMetrics metrics = null;

    /** */
    @GridToStringExclude
    private final transient GridDiscoveryMetricsProvider metricsProvider;

    /** */
    private boolean leaving = false;

    /** */
    private final transient Object mux = new Object();

    /**
     * Create instance of node description with given IP address and
     * cluster member Uid.
     *
     * @param addr Node IP address.
     * @param mbrUid Cluster member Uid.
     * @param metricsProvider Local node metrics provider.
     */
    GridCoherenceDiscoveryNode(InetAddress addr, byte[] mbrUid, GridDiscoveryMetricsProvider metricsProvider) {
        assert addr != null : "ASSERTION [line=80, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";
        assert mbrUid != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";

        this.metricsProvider = metricsProvider;
        this.addr = addr;
        this.mbrUid = mbrUid;

        hash = 17;

        for (byte b : mbrUid) {
            hash = 37 * hash + b;
        }
    }

    /**
     * Create instance of node description with given node data.
     *
     * @param data Node data.
     */
    GridCoherenceDiscoveryNode(GridCoherenceDiscoveryNodeData data) {
        this(data.getAddress(), data.getMemberUid(), null);

        onDataReceived(data);
    }

    /**
     * Update node with received data.
     *
     * @param data Node data.
     */
    void onDataReceived(GridCoherenceDiscoveryNodeData data) {
        assert data != null : "ASSERTION [line=111, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";
        assert Arrays.equals(data.getMemberUid(), mbrUid) == true : "ASSERTION [line=112, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";
        assert attrs.isEmpty() == true : "ASSERTION [line=113, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";
        assert data.getId() != null : "ASSERTION [line=114, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";

        synchronized (mux) {
            metrics = data.getMetrics();

            Map<String, Serializable> dataAttrs = data.getAllAttributes();

            if (dataAttrs != null) {
                attrs.putAll(dataAttrs);
            }

            id = data.getId();
        }
    }

    /**
     * Sets node leaving status.
     */
    void onLeaving() {
        leaving = true;
    }

    /**
     * Gets node leaving status.
     *
     * @return Node leaving status.
     */
    public boolean isLeaving() {
        return leaving;
    }

    /**
     * Gets cluster member Uid.
     *
     * @return Cluster member Uid.
     */
    byte[] getMemberUid() {
        return mbrUid;
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
        assert addr != null : "ASSERTION [line=165, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";

        return addr.getHostAddress();
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
     * {@inheritDoc}
     */
    public GridNodeMetrics getMetrics() {
        if (metricsProvider == null) {
            synchronized (mux) {
                assert metrics != null : "ASSERTION [line=191, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNode.java]";

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
        return this == obj || obj instanceof GridCoherenceDiscoveryNode == true &&
            Arrays.equals(mbrUid, ((GridCoherenceDiscoveryNode)obj).mbrUid) == true;

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
        return GridToStringBuilder.toString(GridCoherenceDiscoveryNode.class, this,
            "mbrUid", GridUtils.byteArray2HexString(mbrUid));
    }
}
