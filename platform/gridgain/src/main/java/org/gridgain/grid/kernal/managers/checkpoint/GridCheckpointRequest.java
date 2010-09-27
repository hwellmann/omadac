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

package org.gridgain.grid.kernal.managers.checkpoint;

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class defines checkpoint request.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridCheckpointRequest implements Externalizable {
    /** */
    private UUID sesId = null;

    /** */
    private String key = null;

    /** */
    private String cpSpi = null;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridCheckpointRequest() {
        // No-op.
    }

    /**
     *
     * @param sesId Task session ID.
     * @param key Checkpoint key.
     * @param cpSpi Checkpoing SPI.
     */
    public GridCheckpointRequest(UUID sesId, String key, String cpSpi) {
        assert sesId != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointRequest.java]";
        assert key != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/kernal/managers/checkpoint/GridCheckpointRequest.java]";

        this.sesId = sesId;
        this.key = key;

        this.cpSpi = cpSpi == null || cpSpi.length() == 0 ? null : cpSpi;
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
     * @return Checkpoint key.
     */
    public String getKey() {
        return key;
    }

    /**
     *
     * @return Checkpoint SPI.
     */
    public String getCheckpointSpi() {
        return cpSpi;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        GridUtils.writeUUID(out, sesId);
        GridUtils.writeString(out, key);
        GridUtils.writeString(out, cpSpi);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sesId = GridUtils.readUUID(in);
        key = GridUtils.readString(in);
        cpSpi = GridUtils.readString(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCheckpointRequest.class, this);
    }
}
