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

import java.util.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridMulticastDiscoverySpi}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to IP-multicast based discovery SPI configuration.")
public interface GridMulticastDiscoverySpiMBean extends GridSpiManagementMBean {
    /**
     * Gets IP address of multicast group.
     *
     * @return Multicast IP address.
     */
    @GridMBeanDescription("IP address of multicast group.")
    public String getMulticastGroup();

    /**
     * Gets port number which multicast messages are sent to.
     *
     * @return Port number.
     */
    @GridMBeanDescription("Port number which multicast messages are sent to.")
    public int getMulticastPort();

    /**
     * Gets local port number that is used by discovery SPI.
     *
     * @return Port number.
     */
    @GridMBeanDescription("Local port number that is used by discovery SPI.")
    public int getTcpPort();

    /**
     * Gets local port range for either TCP or multicast ports. See
     * {@link GridMulticastDiscoverySpi#setLocalPortRange(int)} for details.
     *
     * @return Local port range
     */
    @GridMBeanDescription("Local port range for either TCP or multicast ports.")
    public int getLocalPortRange();

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
     * Gets number of attempts to notify another nodes that this one is leaving grid.
     * It might be impossible to send leaving request and node will try to do
     * it several times.
     *
     * @return Number of retries.
     */
    @GridMBeanDescription("Number of attempts to notify another nodes that this one is leaving grid.")
    public int getLeaveAttempts();

    /**
     * Gets local host IP address that discovery SPI uses.
     *
     * @return IP address.
     */
    @GridMBeanDescription("Local host IP address that discovery SPI uses.")
    public String getLocalHost();

    /**
     * Gets set of remote nodes IDs that have <tt>READY</tt> state.
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
     * Gets TCP messages time-to-live.
     *
     * @return TCP messages time-to-live.
     */
    @GridMBeanDescription("TCP messages time-to-live.")
    public int getTimeToLive();

    /**
     * By default this value is <tt>true</tt>. On startup GridGain will check
     * if local node can receive multicast packets, and if not, will not allow
     * the node to startup.
     *
     * @return checkMulticastEnabled <tt>True</tt> if multicast check is enabled,
     *      <tt>false</tt> otherwise.
     */
    public boolean isCheckMulticastEnabled();
}
