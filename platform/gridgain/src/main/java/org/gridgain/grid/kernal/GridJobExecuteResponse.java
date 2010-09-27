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
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class defines externalizable job execution response.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobExecuteResponse implements GridTaskMessage, Externalizable {
    /** */
    private UUID sesId = null;

    /** */
    private UUID jobId = null;

    /** */
    private GridByteArrayList res = null;

    /** */
    private GridByteArrayList gridEx = null;

    /** */
    private GridByteArrayList jobAttrs = null;

    /** */
    @GridToStringExclude
    private transient GridException fakeEx = null;

    /** */
    private boolean isCancelled = false;

    /** */
    private UUID nodeId = null;

    /**
     * No-op constructor to support {@link Externalizable} interface. This
     * constructor is not meant to be used for other purposes.
     */
    public GridJobExecuteResponse() {
        // No-op.
    }

    /**
     * @param nodeId Sender node ID.
     * @param sesId Task session ID.
     * @param jobId Job ID.
     * @param gridEx Serialized grid exception.
     * @param res Serialized result.
     * @param jobAttrs FIXDOC
     * @param isCancelled Whether job was cancelled or not.
     */
    public GridJobExecuteResponse(UUID nodeId, UUID sesId, UUID jobId, GridByteArrayList gridEx, GridByteArrayList res,
        GridByteArrayList jobAttrs, boolean isCancelled) {
        assert nodeId != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/kernal/GridJobExecuteResponse.java]";
        assert sesId != null : "ASSERTION [line=82, file=src/java/org/gridgain/grid/kernal/GridJobExecuteResponse.java]";
        assert jobId != null : "ASSERTION [line=83, file=src/java/org/gridgain/grid/kernal/GridJobExecuteResponse.java]";

        this.nodeId = nodeId;
        this.sesId = sesId;
        this.jobId = jobId;
        this.gridEx = gridEx;
        this.res = res;
        this.jobAttrs = jobAttrs;
        this.isCancelled = isCancelled;
    }

    /**
     *
     * @return Task session ID.
     */
    public UUID getSessionId() {
        return sesId;
    }

    /**
     *
     * @return Job ID.
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     *
     * @return FIXDOC
     */
    public GridByteArrayList getJobResult() {
        return res;
    }

    /**
     *
     * @return FIXDOC
     */
    public GridByteArrayList getException() {
        return gridEx;
    }

    /**
     *
     * @return Job attributes.
     */
    public GridByteArrayList getJobAttributes() {
        return jobAttrs;
    }


    /**
     *
     * @return Job cancellation status.
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     *
     * @return Sender node ID.
     */
    public UUID getNodeId() {
        return nodeId;
    }

    /**
     *
     * @return Fake exception.
     */
    public GridException getFakeException() {
        return fakeEx;
    }

    /**
     *
     * @param fakeEx Fake exception.
     */
    public void setFakeException(GridException fakeEx) {
        this.fakeEx = fakeEx;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(isCancelled);

        out.writeObject(gridEx);
        out.writeObject(res);
        out.writeObject(jobAttrs);

        GridUtils.writeUUID(out, nodeId);
        GridUtils.writeUUID(out, sesId);
        GridUtils.writeUUID(out, jobId);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        isCancelled = in.readBoolean();

        gridEx = (GridByteArrayList)in.readObject();
        res = (GridByteArrayList)in.readObject();
        jobAttrs = (GridByteArrayList)in.readObject();

        nodeId = GridUtils.readUUID(in);
        sesId = GridUtils.readUUID(in);
        jobId = GridUtils.readUUID(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobExecuteResponse.class, this);
    }
}
