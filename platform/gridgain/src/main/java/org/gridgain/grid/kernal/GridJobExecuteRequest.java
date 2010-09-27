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
 * This class defines externalizable job execution request.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridJobExecuteRequest implements GridTaskMessage, Externalizable {
    /** */
    private UUID sesId = null;

    /** */
    private UUID jobId = null;

    /** */
    @GridToStringExclude
    private GridByteArrayList jobBytes = null;

    /** */
    private long timeout = -1;

    /** */
    private String taskName = null;

    /** */
    private String userVer = null;

    /** */
    private long seqNum = 0;

    /** */
    private String taskClsName = null;

    /** ID of the node that initiated the task. */
    private UUID taskNodeId = null;

    /** */
    @GridToStringExclude
    private GridByteArrayList sesAttrs = null;

    /** */
    @GridToStringExclude
    private GridByteArrayList jobAttrs = null;

    /** Checkpoint SPI name. */
    private String cpSpi = null;

    /** */
    private Collection<GridJobSibling> siblings = null;

    /** */
    private final transient long createTime = System.currentTimeMillis();

    /** */
    private UUID clsLdrId = null;

    /** */
    private GridDeploymentMode depMode = null;

    /**
     * No-op constructor to support {@link Externalizable} interface.
     */
    public GridJobExecuteRequest() {
        // No-op.
    }

    /**
     *
     * @param sesId Task session ID.
     * @param jobId Job ID.
     * @param taskName Task name.
     * @param userVer Code version.
     * @param seqNum Internal task version for the task originating node.
     * @param taskClsName Fully qualified task name.
     * @param jobBytes Job serialized body.
     * @param timeout Task execution timeout.
     * @param taskNodeId Original task execution node ID.
     * @param siblings Collection of split siblings.
     * @param sesAttrs Map of session attributes.
     * @param jobAttrs FIXDOC.
     * @param cpSpi FIXDOC.
     * @param clsLdrId Task local class loader id.
     * @param depMode Task deployment mode.
     */
    public GridJobExecuteRequest(UUID sesId, UUID jobId, String taskName, String userVer, long seqNum,
        String taskClsName, GridByteArrayList jobBytes, long timeout, UUID taskNodeId,
        Collection<GridJobSibling> siblings, GridByteArrayList sesAttrs, GridByteArrayList jobAttrs, String cpSpi,
        UUID clsLdrId, GridDeploymentMode depMode) {
        assert sesId != null : "ASSERTION [line=117, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert jobId != null : "ASSERTION [line=118, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert taskName != null : "ASSERTION [line=119, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert taskClsName != null : "ASSERTION [line=120, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert jobBytes != null : "ASSERTION [line=121, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert taskNodeId != null : "ASSERTION [line=122, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert siblings != null : "ASSERTION [line=123, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert sesAttrs != null : "ASSERTION [line=124, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert jobAttrs != null : "ASSERTION [line=125, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert clsLdrId != null : "ASSERTION [line=126, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert userVer != null : "ASSERTION [line=127, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert seqNum > 0 : "ASSERTION [line=128, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";
        assert depMode != null : "ASSERTION [line=129, file=src/java/org/gridgain/grid/kernal/GridJobExecuteRequest.java]";

        this.sesId = sesId;
        this.jobId = jobId;
        this.taskName = taskName;
        this.userVer = userVer;
        this.taskClsName = taskClsName;
        this.jobBytes = jobBytes;
        this.timeout = timeout;
        this.taskNodeId = taskNodeId;
        this.siblings = siblings;
        this.sesAttrs = sesAttrs;
        this.jobAttrs = jobAttrs;
        this.clsLdrId = clsLdrId;
        this.depMode = depMode;
        this.seqNum = seqNum;

        this.cpSpi = cpSpi == null || cpSpi.length() == 0 ? null : cpSpi;
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
     * @return Job session ID.
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     *
     * @return Task version.
     */
    public String getTaskClassName() {
        return taskClsName;
    }

    /**
     *
     * @return Task name.
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     *
     * @return Task version.
     */
    public String getUserVersion() {
        return userVer;
    }

    /**
     *
     * @return Serialized job bytes.
     */
    public GridByteArrayList getJobBytes() {
        return jobBytes;
    }

    /**
     *
     * @return Timeout.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @return Task node ID.
     */
    public UUID getTaskNodeId() {
        return taskNodeId;
    }

    /**
     * Gets this instance creation time.
     *
     * @return This instance creation time.
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     *
     * @return Job siblings.
     */
    public Collection<GridJobSibling> getSiblings() {
        return siblings;
    }

    /**
     *
     * @return Session attributes.
     */
    public GridByteArrayList getSessionAttributes() {
        return sesAttrs;
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
     * @return Checkpoint SPI name.
     */
    public String getCheckpointSpi() {
        return cpSpi;
    }

    /**
     *
     * @return Task local class loader id.
     */
    public UUID getClassLoaderId() {
        return clsLdrId;
    }

    /**
     *
     * @return the taskIntVer
     */
    public Long getSequenceNumber() {
        return seqNum;
    }

    /**
     *
     * @return the deplMode
     */
    public GridDeploymentMode getDeploymentMode() {
        return depMode;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(depMode.ordinal());

        out.writeLong(timeout);
        out.writeLong(seqNum);

        out.writeObject(siblings);
        out.writeObject(jobBytes);
        out.writeObject(sesAttrs);
        out.writeObject(jobAttrs);

        GridUtils.writeString(out, userVer);
        GridUtils.writeString(out, cpSpi);
        GridUtils.writeString(out, taskName);
        GridUtils.writeString(out, taskClsName);

        GridUtils.writeUUID(out, sesId);
        GridUtils.writeUUID(out, jobId);
        GridUtils.writeUUID(out, taskNodeId);
        GridUtils.writeUUID(out, clsLdrId);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        depMode = GridDeploymentMode.values()[in.readInt()];

        timeout = in.readLong();
        seqNum = in.readLong();

        siblings = (Collection<GridJobSibling>)in.readObject();
        jobBytes = (GridByteArrayList)in.readObject();
        sesAttrs = (GridByteArrayList) in.readObject();
        jobAttrs = (GridByteArrayList)in.readObject();

        userVer = GridUtils.readString(in);
        cpSpi = GridUtils.readString(in);
        taskName = GridUtils.readString(in);
        taskClsName = GridUtils.readString(in);

        sesId = GridUtils.readUUID(in);
        jobId = GridUtils.readUUID(in);
        taskNodeId = GridUtils.readUUID(in);
        clsLdrId = GridUtils.readUUID(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJobExecuteRequest.class, this);
    }
}
