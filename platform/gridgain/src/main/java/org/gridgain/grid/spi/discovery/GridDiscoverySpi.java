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

package org.gridgain.grid.spi.discovery;

import java.io.*;
import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

/**
 * Grid discovery SPI allows to discover remote nodes in grid.
 * <p>
 * The default discovery SPI is {@link org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoverySpi}
 * with default configuration which allows all nodes in local network
 * (with enabled multicast) to discover each other.
 * <p>
 * Gridgain provides the following <tt>GridDeploymentSpi</tt> implementations:
 * <ul>
 * <li>{@link org.gridgain.grid.spi.discovery.jboss.GridJbossDiscoverySpi}</li>
 * <li>{@link org.gridgain.grid.spi.discovery.jgroups.GridJgroupsDiscoverySpi}</li>
 * <li>{@link org.gridgain.grid.spi.discovery.jms.GridJmsDiscoverySpi}</li>
 * <li>{@link org.gridgain.grid.spi.discovery.mail.GridMailDiscoverySpi}</li>
 * <li>{@link org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoverySpi}</li>
 * <li>{@link org.gridgain.grid.spi.discovery.mule.GridMuleDiscoverySpi}</li>
 * <li>{@link org.gridgain.grid.spi.discovery.coherence.GridCoherenceDiscoverySpi}</li>
 *
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridDiscoverySpi extends GridSpi {
    /**
     * Gets collection of remote nodes in grid or empty collection if no remote nodes found.
     *
     * @return Collection of remote nodes.
     */
    public Collection<GridNode> getRemoteNodes();

    /**
     * Gets local node.
     *
     * @return Local node.
     */
    public GridNode getLocalNode();

    /**
     * Gets node by ID.
     *
     * @param nodeId Node ID.
     * @return Node with given ID or <tt>null</tt> if node is not found.
     */
    public GridNode getNode(UUID nodeId);

    /**
     * Pings the remote node to see if it's alive.
     *
     * @param nodeId Node Id.
     * @return <tt>true</tt> if node alive, <tt>false</tt> otherwise.
     */
    public boolean pingNode(UUID nodeId);

    /**
     * Sets node attributes which will be distributed in grid during join process.
     * Note that these attributes cannot be changed and set only once.
     *
     * @param attrs Map of node attributes.
     */
    public void setNodeAttributes(Map<String, Serializable> attrs);

    /**
     * Sets a listener for discovery events. Refer to {@link GridDiscoveryEventType} for a set of all possible
     * discovery events.
     *
     * @param listener Listener to discovery events.
     */
    public void setListener(GridDiscoveryListener listener);

    /**
     * Sets discovery metrics provider. Use metrics provided by
     * {@link GridDiscoveryMetricsProvider#getMetrics()} method to exchange
     * dynamic metrics between nodes.
     *
     * @param metricsProvider Provider of metrics data.
     */
    public void setMetricsProvider(GridDiscoveryMetricsProvider metricsProvider);
}
