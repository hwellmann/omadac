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

package org.gridgain.grid.spi.collision;

import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

/**
 * Collision SPI allows to regulate how grid jobs get executed when they arrive on a
 * destination node for execution. In general a grid node will have multiple jobs arriving
 * to it for execution and potentially multiple jobs that are already executing or waiting
 * for execution on it. There are multiple possible strategies dealing with this situation:
 * all jobs can proceed in parallel, or jobs can be serialized i.e., only one job can execute
 * in any given point of time, or only certain number or types of grid jobs can proceed in
 * parallel, etc.
 * <p>
 * Collision SPI provides developer with ability to use the custom logic in determining how
 * grid jobs should be executed on a destination grid node. GridGain comes with the following
 * ready implementations for collision resolution that cover most popular strategies:
 * <ul>
 *      <li>{@link org.gridgain.grid.spi.collision.fifoqueue.GridFifoQueueCollisionSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.collision.priorityqueue.GridPriorityQueueCollisionSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.collision.jobstealing.GridJobStealingCollisionSpi}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridCollisionSpi extends GridSpi {
    /**
     * This is a callback called when either new grid job arrived or executing job finished its
     * execution. When new job arrives it is added to the end of the wait list and this
     * method is called. When job finished its execution, it is removed from the active list and
     * this method is called (i.e., when grid job is finished it will not appear in any list
     * in collision resolution).
     * <p>
     * Implementation of this method should act on two lists, each of which contains collision
     * job contexts that define a set of operations available during collision resolution. Refer to
     * {@link GridCollisionJobContext} documentation for more information.
     *
     * @param waitJobs Ordered collection of collision contexts for jobs that are currently waiting
     *      for execution. It can be empty but never <tt>null</tt>. Note that a new newly
     *      arrived job, if any, will always be represented by the last item in this list.
     * @param activeJobs Ordered collection of collision contexts for jobs that are currently executing.
     *      It can be empty but never <tt>null</tt>.
     */
    public void onCollision(Collection<GridCollisionJobContext> waitJobs,
        Collection<GridCollisionJobContext> activeJobs);

    /**
     * Listener to be set for notification of external collision events (e.g. job stealing).
     * Once grid receives such notification, it will immediately invoke collision SPI.
     * <p>
     * GridGain uses this listener to enable job stealing from overloaded to underloaded nodes.
     * However, you can also utilize it, for instance, to provide time based collision 
     * resolution. To achieve this, you most likely would mark some job by setting a certain 
     * attribute in job context (see {@link GridJobContext}) for a job that requires 
     * time-based scheduling and set some timer in your SPI implementation that would wake up 
     * after a certain period of time. Once this period is reached, you would notify this 
     * listener that a collision resolution should take place. Then inside of your collision 
     * resolution logic, you would find the marked waiting job and activate it.
     * <p>
     * Note that most collision SPI's may not have external collisions. In that case,
     * they should simply ignore this method and do nothing when listener is set.
     *  
     * @param listener Listener for external collision events.
     */
    public void setExternalCollisionListener(GridCollisionExternalListener listener);
}
