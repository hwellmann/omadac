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
import org.gridgain.apache.*;

/**
 * Result of remote job which gets passed into {@link GridTask#result(GridJobResult, List)}
 * method.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridJobResult extends Serializable {
    /**
     * Gets ID of the job that produced this result. This method has been deprecated
     * in favor of {@link #getJobContext()} method which contains all job related
     * information in addition to job ID. See {@link GridJobContext} for more information.
     *
     * @return ID of the job that produced this result.
     */
    @Deprecated
    public UUID getJobId();

    /**
     * Gets job context. Use job context to access job unique ID or to get/set
     * jobs attributes. Context is attached to a job and travels with it wherever
     * it goes. For example, if a job gets failed-over from one node to another,
     * then its context will be failed over with it and all attributes that
     * were set on the job on the first node will be available on the new node.
     *
     * @return Job context.
     */
    public GridJobContext getJobContext();

    /**
     * Gets data returned by remote job if it didn't fail. This data is the
     * object returned from {@link GridJob#execute()} method.
     * <p>
     * Note that if task is annotated with {@link GridTaskNoResultCache} annotation,
     * then job results will not be cached and will be available only in
     * {@link GridTask#result(GridJobResult, List)} method for every individual job,
     * but not in {@link GridTask#reduce(List)} method. This feature was added to
     * avoid excessive storing of overly large results.
     * 
     * @param <T> Type of the return value returning from {@link GridJob#execute()} method.
     * @return Data returned by remote job's {@link GridJob#execute()} method if it didn't fail.
     */
    public <T> T getData();

    /**
     * Gets exception produced by execution of remote job, or <tt>null</tt> if
     * remote execution finished normally and did not produce any exceptions.
     *
     * @return {@link GridException} produced by execution of remote job or <tt>null</tt> if
     *      no exception was produced.
     *      <p>
     *      Note that if remote job resulted in {@link RuntimeException}
     *      or {@link Error} then they will be wrapped into {@link GridUserUndeclaredException}
     *      returned by this method.
     *      <p>
     *      If job on remote node was rejected (cancelled while it was on waiting queue), then
     *      {@link GridExecutionRejectedException} will be returned.
     *      <p>
     *      If node on which job was computing failed, then {@link GridTopologyException} is
     *      returned.
     */
    public GridException getException();

    /**
     * Gets local instance of remote job returned by {@link GridTask#map(List, Object)} method.
     * 
     * @param <T> Type of {@link GridJob} that was sent to remote node.
     * @return Local instance of remote job returned by {@link GridTask#map(List, Object)} method.
     */
    public <T extends GridJob> T getJob();

    /**
     * Gets node this job executed on.
     *
     * @return Node this job executed on.
     */
    public GridNode getNode();
    
    /**
     * Gets job cancellation status. Returns <tt>true</tt> if job received cancellation 
     * request on remote node. Note that job, after receiving cancellation request, will still
     * need to finish and return, hence {@link #getData()} method may contain 
     * execution result even if the job was canceled.
     * <p>
     * Job can receive cancellation request if the task was explicitly cancelled
     * from future (see {@link GridTaskFuture#cancel()}) or if task completed prior
     * to getting results from all remote jobs.
     * 
     * @return <tt>true</tt> if job received cancellation request and <tt>false</tt> otherwise.
     */
    public boolean isCancelled();
}
