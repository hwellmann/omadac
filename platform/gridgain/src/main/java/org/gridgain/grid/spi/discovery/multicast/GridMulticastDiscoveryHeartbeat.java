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

import java.net.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Helper class that simplifies heartbeat request data manipulation. When node wakes up
 * it immediately begins to start sending heartbeats. Sending heartbeats is the only
 * way to notify another node that this one is still alive.
 * <p>
 * Heartbeat request data consist of
 * <ul>
 * <li>Node state (see {@link GridMulticastDiscoveryNodeState})</li>
 * <li>Node unique identifier (see {@link UUID})</li>
 * <li>Local node port</li>
 * <li>Node start time.</li>
 * <li>Node local IP address</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMulticastDiscoveryHeartbeat {
    /** Length of data static part. */
    static final int STATIC_DATA_LENGTH =
        1/*leaving flag*/ +
        16/*UUID*/ +
        4/*port*/ *
        8/*start time*/ +
        GridDiscoveryMetricsHelper.METRICS_SIZE;

    /** Heartbeat data. */
    private final byte[] data;

    /** Node unique identifier. */
    private final UUID nodeId;

    /** Node local port number. */
    private final int tcpPort;

    /** Node local IP address. */
    private final InetAddress addr;

    /** Time when node woke up. */
    private final long startTime;

    /** Grid node metrics. */
    private GridNodeMetrics metrics = null;

    /** */
    private int off = 0;

    /**
     * Creates new instance of helper based on node characteristics.
     *
     * @param nodeId Local node identifier.
     * @param addr Node local IP address.
     * @param tcpPort Node local port number.
     * @param isLeaving Indicates whether node is leaving grid or not. <tt>true</tt> if yes
     *      and <tt>false</tt> if no.
     * @param startTime Node wake-up time.
     */
    GridMulticastDiscoveryHeartbeat(UUID nodeId, InetAddress addr, int tcpPort, boolean isLeaving, long startTime) {
        assert nodeId != null : "ASSERTION [line=89, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryHeartbeat.java]";
        assert addr != null : "ASSERTION [line=90, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryHeartbeat.java]";
        assert tcpPort > 0 : "ASSERTION [line=91, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryHeartbeat.java]";
        assert tcpPort < 0xffff : "ASSERTION [line=92, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryHeartbeat.java]";
        assert startTime > 0 : "ASSERTION [line=93, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryHeartbeat.java]";

        this.nodeId = nodeId;
        this.tcpPort = tcpPort;
        this.addr = addr;
        this.startTime = startTime;

        // Even though metrics are serialized as object, we assume that size does not change.
        data = new byte[STATIC_DATA_LENGTH + addr.getAddress().length];

        data[0] = (byte)(isLeaving == true ? 1 : 0);

        off++;

        off = GridUtils.longToBytes(nodeId.getLeastSignificantBits(), data, off);
        off = GridUtils.longToBytes(nodeId.getMostSignificantBits(), data, off);
        off = GridUtils.intToBytes(tcpPort, data, off);
        off = GridUtils.longToBytes(startTime, data, off);

        byte[] addrBytes = addr.getAddress();

        System.arraycopy(addrBytes, 0, data, off, addrBytes.length);

        off += addrBytes.length;
    }

    /**
     * Creates new instance of helper based on row bytes array. It also
     * tries to resolve given IP address.
     *
     * @param data Heartbeat request data.
     * @throws UnknownHostException if given IP address could not be resolved.
     */
    GridMulticastDiscoveryHeartbeat(byte[] data) throws UnknownHostException {
        assert data != null : "ASSERTION [line=127, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryHeartbeat.java]";
        assert data.length > STATIC_DATA_LENGTH : "ASSERTION [line=128, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoveryHeartbeat.java]";

        this.data = data;

        // Leave 1st byte for 'isLeaving' flag.
        off = 1;

        long idLeastSignificantBits = GridUtils.bytesToLong(data, off);

        off += 8;

        long idMostSignificantBits = GridUtils.bytesToLong(data, off);

        off += 8;

        nodeId = new UUID(idMostSignificantBits, idLeastSignificantBits);

        tcpPort = GridUtils.bytesToInt(data, off);

        off += 4;

        startTime = GridUtils.bytesToLong(data, off);

        off += 8;

        // Initialize address.
        byte[] addrBytes = new byte[data.length - STATIC_DATA_LENGTH];

        System.arraycopy(data, off, addrBytes, 0, addrBytes.length);

        addr = InetAddress.getByAddress(addrBytes);

        off += addrBytes.length;

        metrics = GridDiscoveryMetricsHelper.deserialize(data, off);
    }

    /**
     * Gets heartbeat data as byte array.
     *
     * @return Heartbeat data.
     */
    byte[] getData() {
        return data;
    }

    /**
     * Gets node identifier.
     *
     * @return Node identifier.
     */
    UUID getNodeId() {
        return nodeId;
    }

    /**
     * Gets port number.
     *
     * @return Port number.
     */
    int getTcpPort() {
        return tcpPort;
    }

    /**
     * Gets node wake-up time.
     *
     * @return Wake-up time.
     */
    long getStartTime() {
        return startTime;
    }

    /**
     * Gets IP address.
     *
     * @return IP address.
     */
    InetAddress getInetAddress() {
        return addr;
    }

    /**
     * Gets node state.
     *
     * @return <tt>true</tt> if node is leaving grid and <tt>false</tt> otherwise.
     */
    boolean isLeaving() {
        return data[0] == (byte)1;
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
     * Sets node metrics.
     *
     * @param metrics Node metrics.
     */
    void setMetrics(GridNodeMetrics metrics) {
        this.metrics = metrics;

        GridDiscoveryMetricsHelper.serialize(data, off, metrics);
    }

    /**
     * Sets node state in heartbeat data to <tt>LEFT</tt>.
     *
     * @param isLeaving <tt>true</tt> if node is leaving grid and <tt>false</tt> otherwise.
     */
    void setLeaving(boolean isLeaving) {
        data[0] = (byte)(isLeaving == true ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMulticastDiscoveryHeartbeat.class, this, 
            "isLeaving", data[0] == (byte)1);
    }
}
