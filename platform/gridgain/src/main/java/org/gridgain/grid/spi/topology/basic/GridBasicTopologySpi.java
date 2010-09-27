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

package org.gridgain.grid.spi.topology.basic;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides basic implementation for topology SPI. This implementation
 * always returns either all available remote grid nodes, remote and local nodes, or only
 * a local node.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 *      <li>{@link #setLocalNode(boolean)} - whether or not to return local node (default is <tt>true</tt>).</li>
 *      <li>{@link #setRemoteNodes(boolean)} - whether or not to return remote nodes (default is <tt>true</tt>).</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridBasicTopologySpi extends GridSpiAdapter implements GridTopologySpi, GridBasicTopologySpiMBean {
    /** */
    private boolean isLocalNode = true;

    /** */
    private boolean isRmtNodes = true;

    /** */
    @GridLocalNodeIdResource
    private UUID localNodeId = null;

    /** Injected grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /**
     * {@inheritDoc}
     */
    public boolean isLocalNode() {
        return isLocalNode;
    }

    /**
     * Sets the flag on whether or not return local node.
     *
     * @param isLocalNode <tt>true</tt> to return local node, <tt>false</tt> otherwise.
     */
    @GridSpiConfiguration(optional = true)
    public void setLocalNode(boolean isLocalNode) {
        this.isLocalNode = isLocalNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemoteNodes() {
        return isRmtNodes;
    }

    /**
     * Sets the flag on whether or not return available remote nodes.
     *
     * @param isRmtNodes <tt>true</tt> to return remote nodes, <tt>false</tt> otherwise.
     */
    @GridSpiConfiguration(optional = true)
    public void setRemoteNodes(boolean isRmtNodes) {
        this.isRmtNodes = isRmtNodes;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getTopology(GridTaskSession ses, Collection<GridNode> grid) throws GridSpiException {
        List<GridNode> top = new ArrayList<GridNode>(grid.size());

        for (GridNode node : grid) {
            if (isLocalNode == true && node.getId().equals(localNodeId) == true) {
                top.add(node);
            }

            if (isRemoteNodes() == true && node.getId().equals(localNodeId) == false) {
                top.add(node);
            }
        }

        return top;
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        // Check parameters.
        assertParameter(isLocalNode == true || isRmtNodes == true, "isLocalNode == true || isRmtNodes == true");

        registerMBean(gridName, this, GridBasicTopologySpiMBean.class);

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("isLocalNode", isLocalNode));
            log.info(configInfo("isRmtNodes", isRmtNodes));
        }

        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        unregisterMBean();

        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridBasicTopologySpi.class, this);
    }  
}
