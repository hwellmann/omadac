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

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridBasicTopologySpi}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to basic topology SPI configuration.")
public interface GridBasicTopologySpiMBean extends GridSpiManagementMBean {
    /**
     * Indicates whether or not to return remote nodes.
     *  
     * @return Whether or not to return remote nodes.
     */
    @GridMBeanDescription("Indicates whether or not to return remote nodes.")
    public boolean isRemoteNodes();
    
    /**
     * Indicates whether or not to return local node.
     *  
     * @return Whether or not to return local node.
     */
    @GridMBeanDescription("Indicates whether or not to return local node.")
    public boolean isLocalNode();
}
