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

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridEventStorageMessage implements Serializable {
    /** */
    private final String resTopic;

    /** */
    private final GridByteArrayList filter;

    /** */
    private final Collection<GridEvent> evts;

    /** */
    private final Throwable ex;

    /** */
    private final UUID clsLdrId;

    /** */
    private final GridDeploymentMode depMode;

    /** */
    private final String filterClsName;

    /** */
    private final long seqNum;

    /** */
    private final String userVer;

    /**
     * @param resTopic Response topic,
     * @param filter Query filter.
     * @param filterClsName Filter class name.
     * @param clsLdrId Class loader ID.
     * @param depMode Deployment mode.
     * @param seqNum Sequence number.
     * @param userVer User version.
     */
    GridEventStorageMessage(String resTopic, GridByteArrayList filter, String filterClsName, UUID clsLdrId,
        GridDeploymentMode depMode, long seqNum, String userVer) {
        this.resTopic = resTopic;
        this.filter = filter;
        this.filterClsName = filterClsName;
        this.depMode = depMode;
        this.seqNum = seqNum;
        this.clsLdrId = clsLdrId;
        this.userVer = userVer;

        evts = null;
        ex = null;
    }

    /**
     * @param evts Grid events.
     * @param ex Exception occurred during processing.
     */
    GridEventStorageMessage(Collection<GridEvent> evts, Throwable ex) {
        this.evts = evts;
        this.ex = ex;

        resTopic = null;
        filter = null;
        filterClsName = null;
        depMode = null;
        seqNum = 0;
        clsLdrId = null;
        userVer = null;
    }

    /**
     *
     * @return FIXDOC
     */
    String getResponseTopic() {
        return resTopic;
    }

    /**
     *
     * @return FIXDOC
     */
    GridByteArrayList getFilter() {
        return filter;
    }

    /**
     *
     * @return FIXDOC
     */
    Collection<GridEvent> getEvents() {
        return evts;
    }

    /**
     *
     * @return the FIXDOC
     */
    public UUID getClassLoaderId() {
        return clsLdrId;
    }

    /**
     * @return Deployment mode.
     */
    public GridDeploymentMode getDeploymentMode() {
        return depMode;
    }

    /**
     * @return Filter class name.
     */
    public String getFilterClassName() {
        return filterClsName;
    }

    /**
     * @return Sequence number.
     */
    public long getSequenceNumber() {
        return seqNum;
    }

    /**
     * @return User version.
     */
    public String getUserVersion() {
        return userVer;
    }

    /**
     * Gets property ex.
     *
     * @return Property ex.
     */
    public Throwable getException() {
        return ex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridEventStorageMessage.class, this);
    }
}
