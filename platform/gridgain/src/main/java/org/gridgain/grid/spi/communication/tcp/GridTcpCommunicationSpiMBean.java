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

package org.gridgain.grid.spi.communication.tcp;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * MBean provide access to TCP-based communication SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean provide access to TCP-based communication SPI.")
public interface GridTcpCommunicationSpiMBean extends GridSpiManagementMBean {
    /**
     * Returns the approximate number of threads that are actively processing
     * NIO tasks.
     *
     * @return Approximate number of threads that are actively processing
     *      NIO tasks.
     */
    @GridMBeanDescription("Approximate number of threads that are actively processing NIO tasks.")
    public int getNioActiveThreadCount();

    /**
     * Returns the approximate total number of NIO tasks that have completed
     * execution. Because the states of tasks and threads may change dynamically
     * during computation, the returned value is only an approximation, but one
     * that does not ever decrease across successive calls.
     *
     * @return Approximate total number of NIO tasks that have completed execution.
     */
    @GridMBeanDescription("Approximate total number of NIO tasks that have completed execution.")
    public long getNioTotalCompletedTaskCount();

    /**
     * Gets current size of the NIO queue size. NIO queue buffers NIO tasks when
     * there are not threads available for processing in the pool.
     *
     * @return Current size of the NIO queue size.
     */
    @GridMBeanDescription("Current size of the NIO queue size.")
    public int getNioTaskQueueSize();

    /**
     * Returns the core number of NIO threads.
     *
     * @return Core number of NIO threads.
     */
    @GridMBeanDescription("Core number of NIO threads.")
    public int getNioCorePoolSize();

    /**
     * Returns the largest number of NIO threads that have ever simultaneously
     * been in the pool.
     *
     * @return Largest number of NIO threads that have ever simultaneously
     *      been in the pool.
     */
    @GridMBeanDescription("Largest number of NIO threads that have ever simultaneously been in the pool.")
    public int getNioLargestPoolSize();

    /**
     * Returns the maximum allowed number of NIO threads.
     *
     * @return Maximum allowed number of NIO threads.
     */
    @GridMBeanDescription("Maximum allowed number of NIO threads.")
    public int getNioMaximumPoolSize();

    /**
     * Returns the current number of NIO threads in the pool.
     *
     * @return Current number of NIO threads in the pool.
     */
    @GridMBeanDescription("Current number of NIO threads in the pool.")
    public int getNioPoolSize();

    /**
     * Returns the approximate total number of NIO tasks that have been scheduled
     * for execution. Because the states of tasks and threads may change dynamically
     * during computation, the returned value is only an approximation, but one that
     * does not ever decrease across successive calls.
     *
     * @return Approximate total number of NIO tasks that have been scheduled for execution.
     */
    @GridMBeanDescription("Approximate total number of NIO tasks that have been scheduled for execution.")
    public long getNioTotalScheduledTaskCount();

    /**
     * Gets local host address for socket binding.
     * Beside loopback address physical node could have
     * several other ones, but only one is assigned to grid node.
     *
     * @return Grid node IP address.
     */
    @GridMBeanDescription("Grid node IP address.")
    public String getLocalAddress();

    /**
     * Gets local port for socket binding.
     *
     * @return Port number.
     */
    @GridMBeanDescription("Port number.")
    public int getLocalPort();

    /**
     * Gets maximum number of local ports tried if all previously
     * tried ports are occupied.
     *
     * @return Local port range.
     */
    @GridMBeanDescription("Local port range.")
    public int getLocalPortRange();

    /**
     * Gets maximum idle connection time upon which idle connections
     * will be closed.
     *
     * @return Maximum idle connection time.
     */
    @GridMBeanDescription("Maximum idle connection time.")
    public long getIdleConnectionTimeout();

    /**
     * Gets flag that indicates whether direct or heap allocated buffer is used.
     *
     * @return Flag that indicates whether direct or heap allocated buffer is used.
     */
    @GridMBeanDescription("Flag that indicates whether direct or heap allocated buffer is used.")
    public boolean isDirectBuffer();

    /**
     * Gets number of threads used for handling NIO messages.
     *
     * @return Number of threads used for handling NIO messages.
     */
    @GridMBeanDescription("Number of threads used for handling NIO messages.")
    public int getMessageThreads();
}
