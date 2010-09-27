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

package org.gridgain.grid.spi.discovery.jgroups;

import java.util.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridJgroupsDiscoverySpi}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to JGroups-based discovery SPI configuration.")
public interface GridJgroupsDiscoverySpiMBean extends GridSpiManagementMBean {
    /**
     * Gets set of remote nodes' IDs.
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
     * Gets either absolute or relative to GridGain installation home folder path to JGroups XML
     * configuration file.
     *
     * @return Path to JGroups configuration file.
     */
    @GridMBeanDescription("Path to JGroups configuration file.")
    public String getConfigurationFile();

    /**
     * Gets JGroups channel local IP address.
     *
     * @return Channel address or <tt>null</tt> if channel is closed or
     *      unconnected.
     */
    @GridMBeanDescription("JGroups channel local IP address.")
    public String getLocalHost();

    /**
     * Gets JGroups channel local port number.
     *
     * @return Channel port or <tt>-1</tt> if channel is closed or
     *      unconnected.
     */
    @GridMBeanDescription("JGroups channel local port number.")
    public int getLocalPort();

    /**
     * Gets time limit in milliseconds to wait for message responses
     * from remote nodes.
     *
     * @return Timeout to wait for responses.
     */
    @GridMBeanDescription("Timeout to wait for responses.")
    public long getJoinTimeout();

    /**
     * Gets JGroups group name. In order to communicate with
     * each other nodes must have the same group name.
     *
     * @return JGroups group name.
     */
    @GridMBeanDescription("JGroups group name.")
    public String getGroupName();
    
    /**
     * Gets JGroups stack name. In order to use multiplexor 
     * over the same channel SPIs must have the same stack name.
     * Stack name is a name of configuration in the configuration file.
     *
     * @return JGroups stack name.
     */
    @GridMBeanDescription("JGroups stack name.")
    public String getStackName();

    /**
     * Gets delay between metrics requests. SPI sends broadcast messages in
     * configurable time interval to another nodes to notify them about node metrics.
     *
     * @return Time period in milliseconds.
     */
    @GridMBeanDescription("Delay in milliseconds between metrics requests.")
    public long getMetricsFrequency();
}
