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

package org.gridgain.grid.spi.loadbalancing.weightedrandom;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management MBean for {@link GridWeightedRandomLoadBalancingSpi} SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to weighted random load balancing SPI configuration.")
public interface GridWeightedRandomLoadBalancingSpiMBean extends GridSpiManagementMBean {
    /**
     * Checks whether node weights are considered when doing
     * random load balancing.
     *
     * @return If <tt>true</tt> then random load is distributed according
     *      to node weights.
     */
    @GridMBeanDescription("Whether node weights are considered when doing random load balancing.")
    public boolean isUseWeights();
    
    /** 
     * Gets weight of this node.
     * 
     * @return Weight of this node.
     */
    @GridMBeanDescription("Weight of this node.")
    public int getNodeWeight();
}
