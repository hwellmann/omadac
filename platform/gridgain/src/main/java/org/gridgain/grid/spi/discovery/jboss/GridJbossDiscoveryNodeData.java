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
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
  * Data that are sent by discovery SPI. They include node unique identifier
 * and node attributes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJbossDiscoveryNodeData implements Serializable {
    /** Node identifier lower bits. */
    private long idLeastSignificantBits = 0;

    /** Node identifier higher bits. */
    private long idMostSignificantBits = 0;

    /** Jboss identifier. */
    private final byte[] jbossId;

    /** Map of node attributes. */
    private final Map<String, Serializable> attrs;
    
    /** Node metrics. */
    private final GridNodeMetrics metrics;

    /** Flag indicates whether it's left or not.*/
    private boolean leave = false;

    /**
     * Creates new instance of JBoss node data.
     *
     * @param jbossId JBoss identifier.
     * @param id Node unique identifier.
     * @param attrs Node attributes.
     * @param metrics Node metrics.
     */
    GridJbossDiscoveryNodeData(byte[] jbossId, UUID id, Map<String, Serializable> attrs, GridNodeMetrics metrics) {
        assert jbossId != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/spi/discovery/jboss/GridJbossDiscoveryNodeData.java]";

        this.jbossId = jbossId;
        this.attrs = attrs;
        this.metrics = metrics;

        if (id != null) {
            idLeastSignificantBits = id.getLeastSignificantBits();
            idMostSignificantBits = id.getMostSignificantBits();
        }
    }

    /**
     * Creates new instance of JBoss node data.
     *
     * @param jbossId JBoss identifier.
     * @param id Node unique identifier.
     * @param attrs Node attributes.
     * @param leave Flag indicates whether node left or not.
     * @param metrics Node metrics.
     */
    GridJbossDiscoveryNodeData(byte[] jbossId, UUID id, Map<String, Serializable> attrs, boolean leave, 
        GridNodeMetrics metrics) {
        this(jbossId, id, attrs, metrics);

        this.leave = leave;
    }
    
    /**
     * Gets sender node unique identifier. This identifier is based on hi and low bits.
     *
     * @return Node unique id..
     */
    public UUID getId() {
        if (idLeastSignificantBits != 0 && idMostSignificantBits != 0) {
            return new UUID(idMostSignificantBits, idLeastSignificantBits);
        }

        return null;
    }

    /**
     * Gets sender node JBoss identifier.
     *
     * @return JBoss unique id..
     */
    public byte[] getJBossId() {
        return jbossId;
    }

    /**
     * Gets map of sender node attributes.
     *
     * @return Map of attributes.
     */
    public Map<String, Serializable> getAttributes() {
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
        return GridToStringBuilder.toString(GridJbossDiscoveryNodeData.class, this, 
            "jbossId", GridUtils.byteArray2HexString(jbossId));
    }
}
