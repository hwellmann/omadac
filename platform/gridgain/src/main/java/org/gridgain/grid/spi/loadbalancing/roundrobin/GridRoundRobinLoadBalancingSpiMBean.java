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

package org.gridgain.grid.spi.loadbalancing.roundrobin;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridRoundRobinLoadBalancingSpi} SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to round robin load balancing SPI configuration.")
public interface GridRoundRobinLoadBalancingSpiMBean extends GridSpiManagementMBean {
    /**
     * Configuration parameter indicating whether a new round robin order should be
     * created for every task. If <tt>true</tt> then load balancer is guaranteed
     * to iterate through nodes sequentially for every task - so as long as number
     * of jobs is less than or equal to the number of nodes, jobs are guaranteed to
     * be assigned to unique nodes. If <tt>false</tt> then one round-robin order
     * will be maintained for all tasks, so when tasks execute concurrently, it
     * is possible for more than one job within task to be assigned to the same
     * node.
     * <p>
     * Default is <tt>true</tt>.
     *
     * @return Configuration parameter indicating whether a new round robin order should
     *      be created for every task. Default is <tt>true</tt>.
     */
    @GridMBeanDescription("Configuration parameter indicating whether a new round robin order should be created for every task.")
    public boolean isPerTask();
}
