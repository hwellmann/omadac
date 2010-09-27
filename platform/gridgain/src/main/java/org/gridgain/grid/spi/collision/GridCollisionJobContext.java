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

/**
 * This interface defines set of operations that collision SPI implementation can perform on
 * jobs that are either waiting or executing.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridCollisionJobContext {
    /**
     * Gets current task session associated with this job.
     *
     * @return Grid task session.
     */
    public GridTaskSession getTaskSession();

    /**
     * Gets job context. Use this context to set/get attributes that
     * should be visible only to this job and should not be distributed
     * to other jobs in the grid.
     * <p>
     * Job context travels with job whenever it gets failed-over to another
     * node, so attributes set on the context on one node will be visible
     * on other nodes this job may potentially end up on.
     *
     * @return Job context.
     */
    public GridJobContext getJobContext();

    /**
     * Job for this context.
     *
     * @return Job for this context.
     */
    public GridJob getJob();

    /**
     * Gets ID of this job. Deprecated in favor of {@link #getJobContext()}
     * which provides job attributes in addition to job ID.
     *
     * @return Job ID.
     */
    @Deprecated
    public UUID getJobId();

    /**
     * Activates the job. If job is already active this is no-op. Collision resolution
     * is handled concurrently, so it may be possible that other threads already activated
     * or cancelled/rejected this job. This method will return <tt>true</tt> if it was
     * able to activate the job, and <tt>false</tt> otherwise.
     *
     * @return <tt>True</tt> if it was possible to activate the job, and
     *      <tt>false</tt> otherwise.
     */
    public boolean activate();

    /**
     * Cancels the job. If job was active (executing) method {@link GridJob#cancel()} will
     * be called on the job. If job was in wait state, then it will be <tt>rejected</tt>
     * prior to execution and {@link GridJob#cancel()} will not be called.
     * <p>
     * Collision resolution is handled concurrently, so it may be possible that other threads
     * already activated or cancelled/rejected this job. This method will return <tt>true</tt>
     * if it was able to cancel/reject the job and <tt>false</tt> otherwise.
     *
     * @return <tt>True</tt> if it was possible to cancel/reject this job, <tt>false</tt>
     *      otherwise.
     */
    public boolean cancel();
}
