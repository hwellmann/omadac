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
import org.gridgain.grid.spi.discovery.*;

/**
 * Represents local or remote node and its attributes. Discovery SPI use this
 * description to check node status (alive/failed), keep local and remote node
 * attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJmsDiscoveryNode implements GridNode {
    /**
     * Node state. If node sends heartbeats and  its attributes
     * node state will be ready. Otherwise node state is not ready.
     */
    private volatile boolean ready = false;

    /** Last received heartbeat message time. */
    private volatile long lastHeartbeat = System.currentTimeMillis();

    /** Node attributes. */
    @GridToStringExclude
    private Map<String, Serializable> attrs = null;

    /** */
    private final UUID id;

    /** Node TCP/IP address. */
    private final String addr;

    /** */
    @GridToStringExclude
    private volatile GridNodeMetrics metrics = null;

    /** */
    @GridToStringExclude
    private final transient GridDiscoveryMetricsProvider metricsProvider;

    /**
     * Create instance of node description with given UID and TCP/IP address.
     *
     * @param id Node UID.
     * @param addr Node address.
     * @param metrics FIXDOC
     * @param metricsProvider FIXDOC
     */
    GridJmsDiscoveryNode(UUID id, String addr, GridNodeMetrics metrics, GridDiscoveryMetricsProvider metricsProvider) {
        assert id != null : "ASSERTION [line=75, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryNode.java]";
        assert addr != null : "ASSERTION [line=76, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryNode.java]";
        assert metrics != null || metricsProvider != null : "ASSERTION [line=77, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryNode.java]";

        this.id = id;
        this.addr = addr;
        this.metrics = metrics;
        this.metricsProvider = metricsProvider;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getId() {
        assert id != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryNode.java]";

        return id;
    }

    /**
     * Gets node TCP/IP address.
     *
     * @return Node address.
     */
    public String getPhysicalAddress() {
        assert addr != null : "ASSERTION [line=100, file=src/java/org/gridgain/grid/spi/discovery/jms/GridJmsDiscoveryNode.java]";

        return addr;
    }

    /**
     * Gets node state whether it's ready or not.
     *
     * @return Node state.
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getAttribute(String name) {
        // No need to synchronize here, as attributes never set and retrieved concurrently.
        // Attributes get set during start and get retrieved after start.
        return (T) ((T)attrs == null ? null : attrs.get(name));
    }

    /**
     * Gets last received heartbeat time.
     *
     * @return Time in milliseconds.
     */
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    /**
     * Method process heartbeat notifications. Every time when delivery SPI gets
     * a heartbeat message from remote node associated with this instance last is notified.
     *
     * @param metrics FIXDOC
     */
    void onHeartbeat(GridNodeMetrics metrics) {
        lastHeartbeat = System.currentTimeMillis();

        this.metrics = metrics;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Serializable> getAttributes() {
        return attrs;
    }

    /**
     * When discovery SPI receives attributes from remote node associated with this instance it calls this
     * method. Method changes node state to <tt>ready</tt>
     *
     * @param attrs Local node attributes.
     * @see #isReady()
     */
    void setAttributes(Map<String, Serializable> attrs) {
        // Seal it. No need to synchronize here, as attributes never set and retrieved concurrently.
        // Attributes get set during start and get retrieved after start.
        this.attrs = Collections.unmodifiableMap(new HashMap<String, Serializable>(attrs));

        ready = true;
    }

    /**
     * {@inheritDoc}
     */
    public GridNodeMetrics getMetrics() {
        if (metricsProvider != null) {
            return metricsProvider.getMetrics();
        }

        return metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof GridJmsDiscoveryNode == true &&
            id.equals(((GridJmsDiscoveryNode)obj).id) == true;
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
        return GridToStringBuilder.toString(GridJmsDiscoveryNode.class, this);
    }
}
