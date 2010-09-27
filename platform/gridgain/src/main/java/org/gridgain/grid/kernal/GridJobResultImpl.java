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

package org.gridgain.grid.kernal;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Class provides implementation for job result.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobResultImpl implements GridJobResult {
    /** */
    private final GridJob job;

    /** */
    private final UUID jobId;

    /** */
    private final GridJobSiblingImpl sibling;

    /** */
    private final GridJobContextImpl jobCtx;

    /** */
    private GridNode node = null;

    /** */
    private Object data = null;

    /** */
    private GridException ex = null;

    /** */
    private boolean hasResponse = false;

    /** */
    private boolean isCancelled = false;
    
    /** */
    private boolean isOccupied = false;

    /**
     *
     * @param job Job instance.
     * @param jobId ID of the job.
     * @param node Node from where this result was received.
     * @param sibling Sibling associated with this result.
     */
    public GridJobResultImpl(GridJob job, UUID jobId, GridNode node, GridJobSiblingImpl sibling) {
        assert job != null : "ASSERTION [line=74, file=src/java/org/gridgain/grid/kernal/GridJobResultImpl.java]";
        assert jobId != null : "ASSERTION [line=75, file=src/java/org/gridgain/grid/kernal/GridJobResultImpl.java]";
        assert node != null : "ASSERTION [line=76, file=src/java/org/gridgain/grid/kernal/GridJobResultImpl.java]";
        assert sibling != null : "ASSERTION [line=77, file=src/java/org/gridgain/grid/kernal/GridJobResultImpl.java]";

        this.job = job;
        this.jobId = jobId;
        this.node = node;
        this.sibling = sibling;

        jobCtx = new GridJobContextImpl(jobId);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public GridJob getJob() {
        return job;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * {@inheritDoc}
     */
    public GridJobContext getJobContext() {
        return jobCtx;
    }

    /**
     * @return Sibling associated with this result.
     */
    public GridJobSiblingImpl getSibling() {
        return sibling;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized GridNode getNode() {
        return node;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public synchronized Object getData() {
        return data;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized GridException getException() {
        return ex;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isCancelled() {
        return isCancelled;
    }

    /**
     *
     * @param node Node from where this result was received.
     */
    public synchronized void setNode(GridNode node) {
        this.node = node;
    }
    
    /**
     *
     * @param data Job data.
     * @param ex Job exception.
     * @param jobAttrs Job attributes.
     * @param isCancelled Whether job was cancelled or not.
     */
    public synchronized void onResponse(Object data, GridException ex, Map<Serializable, Serializable> jobAttrs,
        boolean isCancelled) {
        this.data = data;
        this.ex = ex;
        this.isCancelled = isCancelled;

        if (jobAttrs != null) {
            jobCtx.setAttributes(jobAttrs);
        }

        hasResponse = true;
    }

    /**
     * 
     * @param isOccupied <tt>True</tt> if job for this response is being sent.
     */
    public synchronized void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
    }
    
    /**
     * 
     * @return <tt>True</tt> if job for this response is being sent.
     */
    public synchronized boolean isOccupied() {
        return isOccupied;
    }

    /**
     * Clears stored job data.
     */
    public synchronized void clearData() {
        data = null;
    }

    /**
     *
     */
    public synchronized void resetResponse() {
        data = null;
        ex = null;

        hasResponse = false;
    }

    /**
     *
     * @return <tt>true</tt> if remote job responded.
     */
    public synchronized boolean hasResponse() {
        return hasResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobResultImpl.class, this);
    }
}
