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

package org.gridgain.grid.spi.failover;

import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.loadbalancing.*;

/**
 * This interface defines a set of operations available to failover SPI
 * one a given failed job.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridFailoverContext {
    /**
     * Gets current task session.
     *
     * @return Grid task session.
     */
    public GridTaskSession getTaskSession();

    /**
     * Gets failed result of job execution.
     *
     * @return Result of a failed job.
     */
    public GridJobResult getJobResult();

    /**
     * Gets the next balanced node for failed job. Internally this method will
     * delegate to load balancing SPI (see {@link GridLoadBalancingSpi} to
     * determine the optimal node for execution.
     *
     * @param top Topology to pick balanced node from.
     * @return The next balanced node.
     * @throws GridException If anything failed.
     */
    public GridNode getBalancedNode(List<GridNode> top) throws GridException;
}
