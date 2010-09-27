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

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridDeploymentRequest implements Externalizable {
    /** Response topic name. Response should be sent back to this topic. */
    private String resTopic = null;

    /** Requested class name. */
    private String rsrcName = null;

    /** Class loader ID. */
    private UUID ldrId = null;

    /** Undeploy flag. */
    private boolean isUndeploy = false;

    /**
     * No-op constructor to support {@link Externalizable} interface.
     * This constructor is not meant to be used for other purposes.
     */
    public GridDeploymentRequest() {
        // No-op.
    }

    /**
     * Creates new request.
     *
     * @param ldrId Class loader ID.
     * @param rsrcName Resource name that should be found and sent back.
     * @param isUndeploy Undeploy property.
     */
    GridDeploymentRequest(UUID ldrId, String rsrcName, boolean isUndeploy) {
        assert isUndeploy == true || ldrId != null : "ASSERTION [line=64, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentRequest.java]";
        assert rsrcName != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/kernal/managers/deployment/GridDeploymentRequest.java]";

        this.ldrId = ldrId;
        this.rsrcName = rsrcName;
        this.isUndeploy = isUndeploy;
    }

    /**
     * Sets response topic.
     *
     * @param resTopic New response topic.
     */
    void setResponseTopic(String resTopic) {
        this.resTopic = resTopic;
    }

    /**
     * Get topic response should be sent to.
     *
     * @return Response topic name.
     */
    String getResponseTopic() {
        return resTopic;
    }

    /**
     * Class name/resource name that is being requested.
     *
     * @return Resource or class name.
     */
    String getResourceName() {
        return rsrcName;
    }

    /**
     * Gets property ldrId.
     *
     * @return Property ldrId.
     */
    UUID getClassLoaderId() {
        return ldrId;
    }

    /**
     * Gets property undeploy.
     *
     * @return Property undeploy.
     */
    boolean isUndeploy() {
        return isUndeploy;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(isUndeploy);
        GridUtils.writeString(out, resTopic);
        GridUtils.writeString(out, rsrcName);
        GridUtils.writeUUID(out, ldrId);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        isUndeploy = in.readBoolean();
        resTopic = GridUtils.readString(in);
        rsrcName = GridUtils.readString(in);
        ldrId = GridUtils.readUUID(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridDeploymentRequest.class, this);
    }
}
