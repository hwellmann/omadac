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

package org.gridgain.grid.spi.discovery.mule2;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;
import java.util.*;

/**
 * Management bean for {@link GridMuleDiscoverySpi}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to Mule 2-based discovery SPI configuration.")
public interface GridMuleDiscoverySpiMBean extends GridSpiManagementMBean {
    /**
     * Gets collection of remote nodes' IDs.
     *
     * @return Set of remote nodes IDs.
     */
    @GridMBeanDescription("Set of remote nodes IDs.")
    public Collection<UUID> getRemoteNodeIds();

    /**
     * Gets the number of remote nodes.
     *
     * @return Number of remote nodes.
     */
    @GridMBeanDescription("Number of remote nodes.")
    public int getRemoteNodeCount();

    /**
     * Gets either absolute or relative to GridGain installation home folder path to Mule XML
     * configuration file.
     *
     * @return Path to Mule configuration file.
     */
    @GridMBeanDescription("Absolute or relative to GridGain installation home folder path to Mule XML configuration file.")
    public String getConfigurationFile();

    /**
     * Gets name of the component registered in Mule.
     *
     * @return Component name.
     */
    @GridMBeanDescription("Name of the component registered in Mule.")
    public String getComponentName();

    /**
     * Gets component inbound endpoint URI for heartbeats.
     *
     * @return Inbound endpoint URI.
     */
    @GridMBeanDescription("Component inbound endpoint URI for heartbeats.")
    public String getHeartbeatEndpointUri();

    /**
     * Gets component inbound endpoint URI for handshake.
     *
     * @return Inbound endpoint URI.
     */
    @GridMBeanDescription("Component inbound endpoint URI for handshake.")
    public String getHandshakeEndpointUri();

    /**
     * Gets delay between heartbeat requests. SPI sends broadcast messages in
     * configurable time interval to another nodes to notify them about node state.
     *
     * @return Time period in milliseconds.
     */
    @GridMBeanDescription("Delay between heartbeat requests in milliseconds.")
    public long getHeartbeatFrequency();

    /**
     * Gets number of heartbeat requests that could be missed before remote
     * node is considered to be failed.
     *
     * @return Number of requests.
     */
    @GridMBeanDescription("Number of heartbeat requests that could be missed before remote node is considered to be failed.")
    public int getMaximumMissedHeartbeats();

    /**
     * Gets number of attempts to notify another nodes that this one is leaving
     * grid. Multiple leave requests are sent to increase the chance of successful
     * delivery to every node.
     *
     * @return Number of attempts.
     */
    @GridMBeanDescription("Number of attempts to notify another nodes that this one is leaving grid.")
    public int getLeaveAttempts();
}
