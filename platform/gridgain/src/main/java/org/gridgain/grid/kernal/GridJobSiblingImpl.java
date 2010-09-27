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
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import static org.gridgain.grid.kernal.GridTopic.*;

/**
 * This class provides implementation for job sibling.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobSiblingImpl implements GridJobSibling, Externalizable {
    /** */
    private UUID sesId = null;

    /** */
    private UUID jobId = null;

    /** */
    private transient String taskTopic = null;

    /** */
    private transient String jobTopic = null;

    /** */
    private transient UUID nodeId = null;

    /** */
    private transient boolean isJobDone = false;

    /** */
    private transient GridManagerRegistry reg = null;

    /**
     *
     */
    public GridJobSiblingImpl() {
        // No-op.
    }

    /**
     *
     * @param sesId Task session ID.
     * @param jobId Job ID.
     * @param nodeId ID of the node where this sibling was sent for execution.
     * @param reg Managers registry.
     */
    public GridJobSiblingImpl(UUID sesId, UUID jobId, UUID nodeId, GridManagerRegistry reg) {
        assert sesId != null : "ASSERTION [line=76, file=src/java/org/gridgain/grid/kernal/GridJobSiblingImpl.java]";
        assert jobId != null : "ASSERTION [line=77, file=src/java/org/gridgain/grid/kernal/GridJobSiblingImpl.java]";
        assert nodeId != null : "ASSERTION [line=78, file=src/java/org/gridgain/grid/kernal/GridJobSiblingImpl.java]";
        assert reg != null : "ASSERTION [line=79, file=src/java/org/gridgain/grid/kernal/GridJobSiblingImpl.java]";

        this.sesId = sesId;
        this.jobId = jobId;
        this.nodeId = nodeId;
        this.reg = reg;

        taskTopic = GridTopic.TASK.topic(jobId, nodeId);
        jobTopic = GridTopic.JOB.topic(jobId, nodeId);
    }

    /**
     * {@inheritDoc}
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * @return Node ID.
     */
    public synchronized UUID getNodeId() {
        return nodeId;
    }

    /**
     *
     * @param nodeId Node where this sibling is executing.
     */
    public synchronized void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;

        taskTopic = GridTopic.TASK.topic(jobId, nodeId);
        jobTopic = GridTopic.JOB.topic(jobId, nodeId);
    }

    /**
     *
     * @return <tt>True</tt> if job has finished.
     */
    public synchronized boolean isJobDone() {
        return isJobDone;
    }

    /**
     *
     */
    public synchronized void onJobDone() {
        isJobDone = true;
    }

    /**
     *
     * @return Communication topic for receiving.
     */
    public synchronized String getTaskTopic() {
        return taskTopic;
    }

    /**
     *
     * @return Communication topic for sending.
     */
    public synchronized String getJobTopic() {
        return jobTopic;
    }

    /**
     *
     * @param reg Managers registry.
     */
    public void setManagerRegistry(GridManagerRegistry reg) {
        this.reg = reg;
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() throws GridException {
        reg.getCommunicationManager().sendMessage(reg.getDiscoveryManager().getAllNodes(),
            CANCEL.topic(), new GridJobCancelRequest(sesId, jobId),
            GridCommunicationThreadPolicy.POOLED_THREAD);
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        // Don't serialize node ID.
        GridUtils.writeUUID(out, sesId);
        GridUtils.writeUUID(out, jobId);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // Don't serialize node ID.
        sesId = GridUtils.readUUID(in);
        jobId = GridUtils.readUUID(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobSiblingImpl.class, this);
    }
}
