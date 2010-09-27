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
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class defines externalizable job cancellation request.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobCancelRequest implements Externalizable {
    /** */
    private UUID sesId = null;

    /** */
    private UUID jobId = null;

    /** */
    private boolean system = false;

    /**
     * No-op constructor to support {@link Externalizable} interface.
     * This constructor is not meant to be used for other purposes.
     */
    public GridJobCancelRequest() {
        // No-op.
    }

    /**
     * @param sesId Task session ID.
     */
    public GridJobCancelRequest(UUID sesId) {
        assert sesId != null : "ASSERTION [line=57, file=src/java/org/gridgain/grid/kernal/GridJobCancelRequest.java]";

        this.sesId = sesId;

        jobId = null;
    }

    /**
     * @param sesId Task session ID.
     * @param jobId Job ID.
     */
    public GridJobCancelRequest(UUID sesId, UUID jobId) {
        assert sesId != null : "ASSERTION [line=69, file=src/java/org/gridgain/grid/kernal/GridJobCancelRequest.java]";

        this.sesId = sesId;
        this.jobId = jobId;
    }

    /**
     *
     * @param sesId Session ID.
     * @param jobId Job ID.
     * @param system System flag.
     */
    public GridJobCancelRequest(UUID sesId, UUID jobId, boolean system) {
        assert sesId != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/kernal/GridJobCancelRequest.java]";

        this.sesId = sesId;
        this.jobId = jobId;
        this.system = system;
    }

    /**
     * Gets execution ID of task to be cancelled.
     *
     * @return Execution ID of task to be cancelled.
     */
    public UUID getSessionId() {
        return sesId;
    }

    /**
     * Gets session ID of job to be cancelled. If <tt>null</tt>, then
     * all jobs for the specified task execution ID will be cancelled.
     *
     * @return Execution ID of job to be cancelled.
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     *
     * @return <tt>True</tt> if request to cancel is sent out of system when task
     *       has already reduced and further results are no longer interesting.
     */
    public boolean isSystem() {
        return system;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        GridUtils.writeUUID(out, sesId);
        GridUtils.writeUUID(out, jobId);

        out.writeBoolean(system);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sesId = GridUtils.readUUID(in);
        jobId = GridUtils.readUUID(in);

        system = in.readBoolean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobCancelRequest.class, this);
    }
}
