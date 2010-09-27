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

package org.gridgain.grid.kernal.managers.eventstorage;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridEventImpl implements GridEvent {
    /** */
    private GridEventType type = null;

    /** */
    private UUID locNodeId = null;

    /** */
    private UUID evtNodeId = null;

    /** */
    private String taskName = null;

    /** */
    private String userVer = null;

    /** */
    private String cpKey = null;

    /** */
    private UUID taskSesId = null;

    /** */
    private UUID jobId;

    /** */
    private long tstamp = -1;

    /** */
    private GridJobResultPolicy jobResultPolicy = null;

    /** */
    private String msg = null;

    /**
     * {@inheritDoc}
     */
    public String getCheckpointKey() {
        return cpKey;
    }

    /**
     * @param cpKey  FIXDOC
     */
    public void setCheckpointKey(String cpKey) {
        this.cpKey = cpKey;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getEventNodeId() {
        return evtNodeId;
    }

    /**
     * @param evtNodeId  FIXDOC
     */
    public void setEventNodeId(UUID evtNodeId) {
        this.evtNodeId = evtNodeId;
    }

    /**
     * {@inheritDoc}
     */
    public GridEventType getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getLocalNodeId() {
        return locNodeId;
    }

    /**
     * {@inheritDoc}
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getTaskSessionId() {
        return taskSesId;
    }

    /**
     * {@inheritDoc}
     */
    public long getTimestamp() {
        return tstamp;
    }

    /**
     * {@inheritDoc}
     */
    public String getMessage() {
        return msg;
    }

    /**
     * {@inheritDoc}
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     *
     * @param jobId FIXDOC
     */
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    /**
     * @param locNodeId FIXDOC
     */
    void setLocalNodeId(UUID locNodeId) {
        this.locNodeId = locNodeId;
    }

    /**
     * @param taskSesId FIXDOC
     */
    void setSessionId(UUID taskSesId) {
        this.taskSesId = taskSesId;
    }

    /**
     * @param taskName FIXDOC
     */
    void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * @param tstamp FIXDOC
     */
    void setTimestamp(long tstamp) {
        this.tstamp = tstamp;
    }

    /**
     * @param type FIXDOC
     */
    void setType(GridEventType type) {
        this.type = type;
    }

    /**
     *
     * @param msg FIXDOC
     */
    public void setMessage(String msg) {
        this.msg = msg;
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
     * @param userVer Task version.
     */
    public void setUserVersion(String userVer) {
        this.userVer = userVer;
    }

    /**
     *
     * @return the jobResultPolicy
     */
    public GridJobResultPolicy getJobResultPolicy() {
        return jobResultPolicy;
    }

    /**
     *
     * @param jobResultPolicy Job Result Policy
     */
    public void setJobResultPolicy(GridJobResultPolicy jobResultPolicy) {
        this.jobResultPolicy = jobResultPolicy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridEventImpl.class, this);
    }
}
