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

package org.gridgain.grid.kernal.managers.deployment;

import java.util.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.*;

/**
 * Deployment metadata.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridDeploymentMetadata {
    /** Deployment mode. */
    private GridDeploymentMode depMode = null;

    /** */
    private String alias = null;

    /** */
    private String clsName = null;

    /** */
    private long seqNum = 0;

    /** */
    private String userVer = null;

    /** */
    private UUID senderNodeId = null;

    /** */
    private UUID clsLdrId = null;

    /** */
    private ClassLoader parentLdr = null;

    /** */
    private boolean record = false;

    /**
     * Gets property depMode.
     *
     * @return Property depMode.
     */
    GridDeploymentMode getDeploymentMode() {
        return depMode;
    }

    /**
     * Sets property depMode.
     *
     * @param depMode Property depMode.
     */
    void setDeploymentMode(GridDeploymentMode depMode) {
        this.depMode = depMode;
    }

    /**
     * Gets property alias.
     *
     * @return Property alias.
     */
    String getAlias() {
        return alias;
    }

    /**
     * Sets property alias.
     *
     * @param alias Property alias.
     */
    void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Gets property clsName.
     *
     * @return Property clsName.
     */
    String getClassName() {
        return clsName;
    }

    /**
     * Sets property clsName.
     *
     * @param clsName Property clsName.
     */
    void setClassName(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Gets property seqNum.
     *
     * @return Property seqNum.
     */
    long getSequenceNumber() {
        return seqNum;
    }

    /**
     * Sets property seqNum.
     *
     * @param seqNum Property seqNum.
     */
    void setSequenceNumber(long seqNum) {
        this.seqNum = seqNum;
    }

    /**
     * Gets property userVer.
     *
     * @return Property userVer.
     */
    String getUserVersion() {
        return userVer;
    }

    /**
     * Sets property userVer.
     *
     * @param userVer Property userVer.
     */
    void setUserVersion(String userVer) {
        this.userVer = userVer;
    }

    /**
     * Gets property senderNodeId.
     *
     * @return Property senderNodeId.
     */
    UUID getSenderNodeId() {
        return senderNodeId;
    }

    /**
     * Sets property senderNodeId.
     *
     * @param senderNodeId Property senderNodeId.
     */
    void setSenderNodeId(UUID senderNodeId) {
        this.senderNodeId = senderNodeId;
    }

    /**
     * Gets property clsLdrId.
     *
     * @return Property clsLdrId.
     */
    UUID getClassLoaderId() {
        return clsLdrId;
    }

    /**
     * Sets property clsLdrId.
     *
     * @param clsLdrId Property clsLdrId.
     */
    void setClassLoaderId(UUID clsLdrId) {
        this.clsLdrId = clsLdrId;
    }

    /**
     * Gets parent loader.
     *
     * @return Parent loader.
     */
    public ClassLoader getParentLoader() {
        return parentLdr;
    }

    /**
     * Sets parent loader.
     *
     * @param parentLdr Parent loader.
     */
    public void setParentLoader(ClassLoader parentLdr) {
        this.parentLdr = parentLdr;
    }

    /**
     * Gets property record.
     *
     * @return Property record.
     */
    boolean isRecord() {
        return record;
    }

    /**
     * Sets property record.
     *
     * @param record Property record.
     */
    void setRecord(boolean record) {
        this.record = record;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentMetadata.class, this);
    }
}
