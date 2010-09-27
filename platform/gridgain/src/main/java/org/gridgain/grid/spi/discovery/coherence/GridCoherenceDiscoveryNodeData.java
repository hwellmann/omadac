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
import java.util.*;
import java.net.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.*;

/**
 * Data that are sent by discovery SPI. They include node unique identifier
* and node attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridCoherenceDiscoveryNodeData implements Serializable {
    /** Node IP address. */
    private final InetAddress addr;

    /** Cluster member identifier. */
    private final byte[] mbrUid;

    /** Node attributes. */
    private Map<String, Serializable> attrs = null;

    /** Node unique Id. */
    private final UUID id;

    /** Node metrics. */
    private final GridNodeMetrics metrics;

    /** Flag indicates whether it's left or not.*/
    private boolean leave = false;

    /**
     * Creates new instance of Coherence node data.
     *
     * @param id Node identifier.
     * @param mbrUid Cluster member identifier.
     * @param addr Node IP address.
     * @param attrs Node attributes.
     * @param metrics Node metrics.
     */
    GridCoherenceDiscoveryNodeData(UUID id, byte[] mbrUid, InetAddress addr, Map<String, Serializable> attrs,
        GridNodeMetrics metrics) {
        assert id != null : "ASSERTION [line=68, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNodeData.java]";
        assert mbrUid != null : "ASSERTION [line=69, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNodeData.java]";
        assert addr != null : "ASSERTION [line=70, file=src/java/org/gridgain/grid/spi/discovery/coherence/GridCoherenceDiscoveryNodeData.java]";

        this.id = id;
        this.addr = addr;
        this.mbrUid = mbrUid;
        this.attrs = attrs;
        this.metrics = metrics;
    }

    /**
     * Creates new instance of Coherence node data.
     *
     * @param id Node identifier.
     * @param mbrUid Cluster member identifier.
     * @param addr Node IP address.
     * @param attrs Node attributes.
     * @param leave Flag indicates whether node left or not.
     * @param metrics Node metrics.
     */
    GridCoherenceDiscoveryNodeData(UUID id, byte[] mbrUid, InetAddress addr, Map<String, Serializable> attrs,
        boolean leave, GridNodeMetrics metrics) {
        this(id, mbrUid, addr, attrs, metrics);

        this.leave = leave;
    }

    /**
     * Gets node IP address.
     *
     * @return Node IP address.
     */
    public InetAddress getAddress() {
        return addr;
    }

    /**
     * Gets node Id.
     *
     * @return Node Id.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets cluster member Id.
     *
     * @return Cluster member Id.
     */
    byte[] getMemberUid() {
        return mbrUid;
    }

    /**
     * Gets node attibutes.
     *
     * @return Node attibutes.
     */
    Map<String, Serializable> getAllAttributes() {
        return attrs;
    }

    /**
     * Gets flag whether node left or not.
     *
     * @return FIXDOC.
     */
    public boolean isLeave() {
        return leave;
    }

    /**
     * Sets flag whether node left or not.
     *
     * @param leave Node leaving status.
     */
    public void setLeave(boolean leave) {
        this.leave = leave;
    }

    /**
     * Gets node metrics.
     *
     * @return FIXDOC.
     */
    public GridNodeMetrics getMetrics() {
        return metrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCoherenceDiscoveryNodeData.class, this,
            "mbrUid", GridUtils.byteArray2HexString(mbrUid));
    }
}
