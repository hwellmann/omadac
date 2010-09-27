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

package org.gridgain.grid.spi.topology.attributes;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.topology.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides attribute based implementation for topology SPI.
 * This implementation always returns all nodes (local and remote) that
 * have attributes provided in configuration with given values. If no
 * attributes were provided, all nodes, local and remote, will be included
 * into topology.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has following optional configuration parameters:
 * <ul>
 *      <li>{@link #setAttributes(Map)} - Map of attributes and their values that nodes should have.</li>
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
public class GridAttributesTopologySpi extends GridSpiAdapter implements GridTopologySpi,
    GridAttributesTopologySpiMBean {
    /** Injected grid logger. */
    @GridLoggerResource
    private GridLogger log = null;

    /** Named attributes. */
    private Map<String, Serializable> attrs = null;

    /**
     * {@inheritDoc}
     */
    public Map<String, Serializable> getAttributes() {
        return attrs;
    }

    /**
     * Sets attributes that all nodes should have, to be in topology.
     *
     * @param attrs Map of node attributes.
     */
    @GridSpiConfiguration(optional = true)
    public void setAttributes(Map<String, Serializable> attrs) {
        this.attrs = attrs;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<GridNode> getTopology(GridTaskSession ses, Collection<GridNode> grid) throws GridSpiException {
        List<GridNode> top = new ArrayList<GridNode>(grid.size());

        for (GridNode node : grid) {
            Map<String, Serializable> nodeAttrs = node.getAttributes();

            if (attrs != null && nodeAttrs != null) {
                if (GridUtils.containsAll(nodeAttrs, attrs) == false) {
                    continue;
                }
            }
            else if (nodeAttrs == null && attrs != null) {
                continue;
            }

            top.add(node);

            if (log.isDebugEnabled() == true) {
                log.debug("Included node into topology: " + node);
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

        registerMBean(gridName, this, GridAttributesTopologySpiMBean.class);

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("attrs", attrs));
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
        return GridToStringBuilder.toString(GridAttributesTopologySpi.class, this);
    }  
}
