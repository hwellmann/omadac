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

package org.gridgain.grid;

import java.io.*;
import java.util.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.collision.*;
import org.gridgain.grid.spi.failover.*;
import org.gridgain.jsr305.*;
import org.gridgain.apache.*;

/** 
 * Context attached to every job executed on the grid. Note that unlike 
 * {@link GridTaskSession}, which distributes all attributes to all jobs 
 * in the task including the task itself, job context attributes belong 
 * to a job and do not get sent over network unless a job moves from one 
 * node to another.
 * <p>
 * In most cases a job, once assigned to a node, will never move to another
 * node. However, it is possible that collision SPI rejects a job before
 * it ever got a chance to execute (job rejection) which will cause fail-over
 * to another node. Or user is not satisfied with the outcome of a job and 
 * fails it over to another node by returning {@link GridJobResultPolicy#FAILOVER}
 * policy from {@link GridTask#result(GridJobResult, List)} method. In this case
 * all context attributes set on one node will be available on any other node
 * this job travels to.
 * <p>
 * You can also use <tt>GridJobContext</tt> to communicate between SPI's and jobs.
 * For example, if you need to cancel an actively running job from {@link GridCollisionSpi}
 * you may choose to set some context attribute on the job to mark the fact
 * that a job was cancelled by grid and not by a user. Context attributes can 
 * also be assigned in {@link GridFailoverSpi} prior to failing over a job.
 * <p>
 * From within {@link GridTask#result(GridJobResult, List)} or {@link GridTask#reduce(List)} methods, 
 * job context is available via {@link GridJobResult#getJobContext()} method which gives user the 
 * ability to check context attributes from within grid task implementation for every job 
 * returned from remote nodes.
 * <p>
 * Job context can be injected into {@link GridJob} via {@link GridJobContextResource @GridJobContextResource}
 * annotation. Refer to the {@link GridJobContextResource @GridJobContextResource} 
 * documentation for coding examples on how to inject job context.
 * <p>
 * Attribute names that start with <tt>"gridgain:"</tt> are reserved for internal system use.
 * 
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridJobContext {
    /**
     * Gets ID of the job this context belongs to.
     * 
     * @return ID of the job this context belongs to.
     */
    public UUID getJobId();

    /**
     * Sets an attribute into this job context. 
     *
     * @param key Attribute key.
     * @param val Attribute value.
     */
    public void setAttribute(Serializable key, @Nullable Serializable val);

    /**
     * Sets map of attributes into this job context.
     * 
     * @param attrs Local attributes.
     */
    public void setAttributes(Map<? extends Serializable, ? extends Serializable> attrs);

    /**
     * Gets attribute from this job context.
     * 
     * @param key Attribute key.
     * @return Attribute value (possibly <tt>null</tt>).
     */
    public Serializable getAttribute(Serializable key);

    /**
     * Gets all attributes present in this job context.
     * 
     * @return All attributes.
     */
    public Map<? extends Serializable, ? extends Serializable> getAttributes();
}
