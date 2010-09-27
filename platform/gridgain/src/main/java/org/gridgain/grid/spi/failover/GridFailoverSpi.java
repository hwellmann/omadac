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
import org.gridgain.grid.spi.*;

/**
 * Failover SPI provides developer with ability to supply custom logic for handling
 * failed execution of a grid job. Job execution can fail for a number of reasons:
 * <ul>
 *      <li>Job execution threw an exception (runtime, assertion or error)</li>
 *      <li>Node on which job was execution left topology (crashed or stopped)</li>
 *      <li>Collision SPI on remote node cancelled a job before it got a chance to execute (job rejection).</li>
 * </ul>
 * In all cases failover SPI takes failed job (as failover context) and list of all
 * grid nodes and provides another node on which the job execution will be retried.
 * It is up to failover SPI to make sure that job is not mapped to the node it
 * failed on. The failed node can be retrieved from
 * {@link GridJobResult#getNode() GridFailoverContext.getJobResult().getNode()}
 * method.
 * <p>
 * GridGain comes with the following built-in failover SPI implementations:
 * <ul>
 *      <li>{@link org.gridgain.grid.spi.failover.never.GridNeverFailoverSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.failover.always.GridAlwaysFailoverSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.failover.jobstealing.GridJobStealingFailoverSpi}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridFailoverSpi extends GridSpi {
    /**
     * This method is called when method {@link GridTask#result(GridJobResult, List)} returns
     * value {@link GridJobResultPolicy#FAILOVER} policy indicating that the result of
     * job execution must be failed over. Implementation of this method should examine failover
     * context and choose one of the grid nodes from supplied <tt>topology</tt> to retry job execution
     * on it. For best performance it is advised that {@link GridFailoverContext#getBalancedNode(List)}
     * method is used to select node for execution of failed job.
     *
     * @param ctx Failover context.
     * @param top Collection of all grid nodes within task topology (may include failed node).
     * @return New node to route this job to or <tt>null</tt> if new node cannot be picked.
     *      If job failover fails (returns <tt>null</tt>) the whole task will be failed.
     */
    public GridNode failover(GridFailoverContext ctx, List<GridNode> top);
}
