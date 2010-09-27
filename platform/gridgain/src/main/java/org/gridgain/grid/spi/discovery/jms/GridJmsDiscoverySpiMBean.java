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

import java.util.*;
import javax.jms.*;
import javax.naming.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridJmsDiscoverySpi}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to JMS-based discovery SPI configuration.")
public interface GridJmsDiscoverySpiMBean extends GridSpiManagementMBean {
    /**
     * Returns set of discovered remote nodes IDs.
     *
     * @return Set of remote nodes IDs.
     */
    @GridMBeanDescription("Set of remote nodes IDs.")
    public Collection<UUID> getRemoteNodeIds();

    /**
     * Returns number of remote nodes.
     *
     * @return Number of remote nodes.
     */
    @GridMBeanDescription("Number of remote nodes.")
    public int getRemoteNodeCount();

    /**
     * Returns username to connect to JNDI.
     *
     * @return Name of the user. If not set, <tt>null</tt> is returned.
     */
    @GridMBeanDescription("Username to connect to JNDI.")
    public String getUser();

    /**
     * Returns interval for heartbeat messages.
     *
     * @return Time in milliseconds.
     */
    @GridMBeanDescription("Interval in milliseconds for heartbeat messages.")
    public long getHeartbeatFrequency();

    /**
     * Returns numbers of heartbeat messages that could be missed before node
     * is considered to be failed.
     *
     * @return Number of heartbeat messages.
     */
    @GridMBeanDescription("Numbers of heartbeat messages that could be missed before node is considered to be failed.")
    public long getMaximumMissedHeartbeats();

    /**
     * Returns JMS time to live value for messages.
     *
     * @return Time-to-live value in milliseconds.
     */
    @GridMBeanDescription("JMS time to live value for messages in milliseconds.")
    public long getTimeToLive();

    /**
     * Returns timeout value for ping request.
     *
     * @return Time in milliseconds.
     */
    @GridMBeanDescription("Timeout value for ping request in milliseconds.")
    public long getPingWaitTime();

    /**
     * Returns timeout value for attributes handshake.
     *
     * @return Time in milliseconds.
     */
    @GridMBeanDescription("Timeout value for attributes handshake in milliseconds.")
    public long getHandshakeWaitTime();
    
    /**
     * Returns maximum number of handshake threads. This means maximum 
     * number of handshakes that can be executed in parallel.
     * <p>
     * Note that if you expect a lot of nodes discovered each other in parallel
     * you should better set higher value. After discovery number of unused 
     * threads will be shrank to 1. Typically two nodes that discover each other
     * require one thread.
     * 
     * @return Maximum number of handshake threads.
     */
    @GridMBeanDescription("Maximum number of handshake threads.")
    public int getMaximumHandshakeThreads();

    /**
     * Returns the approximate number of threads that are actively processing
     * handshake tasks.
     *
     * @return Approximate number of threads that are actively processing
     *      handshake tasks.
     */
    @GridMBeanDescription("Approximate number of threads that are actively processing handshake tasks.")
    public int getHandshakeActiveThreadCount();

    /**
     * Returns the approximate total number of handshakes that have completed
     * execution.
     *
     * @return Approximate total number of handshakes that have completed execution.
     */
    @GridMBeanDescription("Approximate total number of handshakes that have completed execution.")
    public long getHandshakeTotalCompletedCount();

    /**
     * Gets current size of the handshake queue size. Handshake queue keeps 
     * handshake tasks when there are not threads available for processing in the pool.
     *
     * @return Current size of the handshake queue size.
     */
    @GridMBeanDescription("Current size of the handshake queue size.")
    public int getHandshakeQueueSize();

    /**
     * Returns the core number of handshake threads.
     *
     * @return Core number of handshake threads.
     */
    @GridMBeanDescription("Core number of handshake threads.")
    public int getHandshakeCorePoolSize();

    /**
     * Returns the largest number of handshake threads that have ever simultaneously
     * been in the pool.
     *
     * @return Largest number of handshake threads that have ever simultaneously
     *      been in the pool.
     */
    @GridMBeanDescription("Largest number of handshake threads that have ever simultaneously been in the pool.")
    public int getHandshakeLargestPoolSize();

    /**
     * Returns the maximum allowed number of handshake threads.
     *
     * @return Maximum allowed number of handshake threads.
     */
    @GridMBeanDescription("Maximum allowed number of handshake threads.")
    public int getHandshakeMaximumPoolSize();

    /**
     * Returns the current number of handshake threads in the pool.
     *
     * @return Current number of handshake threads in the pool.
     */
    @GridMBeanDescription("Current number of handshake threads in the pool.")
    public int getHandshakePoolSize();

    /**
     * Returns the approximate total number of handshake tasks that have been scheduled
     * for execution.
     *
     * @return Approximate total number of handshake tasks that have been scheduled for execution.
     */
    @GridMBeanDescription("Approximate total number of handshake tasks that have been scheduled for execution.")
    public long getHandshakeTotalScheduledCount();

    /**
     * Returns naming context variables which are used by node to establish
     * JNDI tree connection.
     *
     * @return Map of JNDI environment variables.
     * @see Context
     */
    @GridMBeanDescription("Map of JNDI environment variables.")
    public Map<Object, Object> getJndiEnvironment();

    /**
     * Returns name of the JMS connection factory in JNDI tree that is used
     * for establishing connections by discovery SPI.
     *
     * @return Connection factory name. If not set, <tt>null</tt> is returned.
     */
    @GridMBeanDescription("Name of the JMS connection factory in JNDI tree.")
    public String getConnectionFactoryName();

    /**
     * Returns JMS connection factory.
     *
     * @return Connection factory.
     */
    @GridMBeanDescription("JMS connection factory.")
    public ConnectionFactory getConnectionFactory();    
    
    /**
     * Returns JMS topic name for broadcasting messages to all discovered nodes.
     *
     * @return Name of the topic in JNDI tree.
     */
    @GridMBeanDescription("Name of the topic in JNDI tree.")
    public String getTopicName();
    
    /**
     * Returns JMS topic for broadcasting messages to all discovered nodes.
     *
     * @return JMS topic.
     */
    @GridMBeanDescription("JMS topic for broadcasting messages to all discovered nodes.")
    public Topic getTopic();    
}
