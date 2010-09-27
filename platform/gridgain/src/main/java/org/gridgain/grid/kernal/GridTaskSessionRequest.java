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
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridTaskSessionRequest implements GridTaskMessage, Externalizable {
    /** Changed attributes. */
    private GridByteArrayList attrs = null;

    /** Task session ID. */
    private UUID sesId = null;

    /** ID of job within a task. */
    private UUID jobId = null;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridTaskSessionRequest() {
        // No-op.
    }

    /**
     *
     * @param sesId Session ID.
     * @param jobId Job ID within the session.
     * @param attrs Changed attribute.
     */
    public GridTaskSessionRequest(UUID sesId, UUID jobId, GridByteArrayList attrs) {
        assert sesId != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/kernal/GridTaskSessionRequest.java]";
        assert jobId != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/kernal/GridTaskSessionRequest.java]";
        assert attrs != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/kernal/GridTaskSessionRequest.java]";

        this.sesId = sesId;
        this.attrs = attrs;
        this.jobId = jobId;
    }

    /**
     *
     * @return Changed attributes.
     */
    public GridByteArrayList getAttributes() {
        return attrs;
    }

    /**
     *
     * @return Session ID.
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
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(attrs);

        GridUtils.writeUUID(out, sesId);
        GridUtils.writeUUID(out, jobId);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        attrs = (GridByteArrayList)in.readObject();

        sesId = GridUtils.readUUID(in);
        jobId = GridUtils.readUUID(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridTaskSessionRequest.class, this);
    }
}
