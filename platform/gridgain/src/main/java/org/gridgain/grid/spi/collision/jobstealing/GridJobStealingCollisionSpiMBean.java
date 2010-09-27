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

package org.gridgain.grid.spi.collision.jobstealing;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management MBean for job stealing based collision SPI.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean for job stealing based collision SPI.")
public interface GridJobStealingCollisionSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets current number of jobs to be stolen. This is outstanding 
     * requests number.
     *
     * @return Number of jobs to be stolen.
     */
    @GridMBeanDescription("Number of jobs to be stolen.")
    public int getCurrentJobsToStealCount();

    /**
     * Gets current number of jobs that wait for the execution.
     *
     * @return Number of jobs that wait for execution.
     */
    @GridMBeanDescription("Number of jobs that wait for execution.")
    public int getCurrentWaitJobsCount();

    /**
     * Gets current number of jobs that are being executed.
     *
     * @return Number of active jobs.
     */
    @GridMBeanDescription("Number of active jobs.")
    public int getCurrentActiveJobsCount();

    /**
     * Gets total number of stolen jobs.
     *
     * @return Number of stolen jobs.
     */
    @GridMBeanDescription("Number of stolen jobs.")
    public int getTotalStolenJobsCount();

    /**
     * Gets number of jobs that can be executed in parallel.
     *
     * @return Number of jobs that can be executed in parallel.
     */
    @GridMBeanDescription("Number of jobs that can be executed in parallel.")
    public int getActiveJobsThreshold();

    /**
     * Gets job count threshold at which this node will
     * start stealing jobs from other nodes.
     *
     * @return Job count threshold.
     */
    @GridMBeanDescription("Job count threshold.")
    public int getWaitJobsThreshold();

    /**
     * Message expire time configuration parameter. If no response is received
     * from a busy node to a job stealing message, then implementation will
     * assume that message never got there, or that remote node does not have
     * this node included into topology of any of the jobs it has.
     *
     * @return Message expire time.
     */
    @GridMBeanDescription("Message expire time.")
    public long getMessageExpireTime();
    
    /**
     * Gets flag indicating whether this node should attempt to steal jobs
     * from other nodes. If <tt>false</tt>, then this node will steal allow
     * jobs to be stolen from it, but won't attempt to steal any jobs from
     * other nodes.
     * <p>
     * Default value is <tt>true</tt>.
     * 
     * @return Flag indicating whether this node should attempt to steal jobs
     *      from other nodes.
     */
    @GridMBeanDescription("Flag indicating whether this node should attempt to steal jobs from other nodes.")
    public boolean isStealingEnabled();
    
    /**
     * Gets maximum number of attempts to steal job by another node.
     * If not specified, {@link GridJobStealingCollisionSpi#DFLT_MAX_STEALING_ATTEMPTS} 
     * value will be used.
     *
     * @return Maximum number of attempts to steal job by another node.
     */
    @GridMBeanDescription("Maximum number of attempts to steal job by another node.")
    public int getMaximumStealingAttempts();
    
    /**
     * Configuration parameter to enable stealing to/from only nodes that
     * have these attributes set (see {@link GridNode#getAttribute(String)} and
     * {@link GridConfiguration#getUserAttributes()} methods).
     *  
     * @return Node attributes to enable job stealing for.
     */
    @GridMBeanDescription("Node attributes to enable job stealing for.")
    public Map<String, ? extends Serializable> getStealingAttributes();
}
