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

package org.gridgain.grid.spi.communication.mule;

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of communication message. It keeps message and sender node ID inside.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridMuleCommunicationMessage implements Serializable {
    /** Communication message. */
    private final Serializable msg;

    /** Sender node unique identifier. */
    private final UUID nodeId;

    /**
     * Creates new instance of wrapper.
     *
     * @param nodeId Sender node UID.
     * @param msg Message is being sent.
     */
    GridMuleCommunicationMessage(UUID nodeId, Serializable msg) {
        assert nodeId != null : "ASSERTION [line=48, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationMessage.java]";
        assert msg != null : "ASSERTION [line=49, file=src/java/org/gridgain/grid/spi/communication/mule/GridMuleCommunicationMessage.java]";

        this.nodeId = nodeId;
        this.msg = msg;
    }

    /**
     * Gets unwrapped message.
     *
     * @return Message that was sent.
     */
    Serializable getMessage() {
        return msg;
    }

    /**
     * Gets sender node UID.
     *
     * @return Node unique identifier in grid.
     */
    UUID getNodeId() {
        return nodeId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMuleCommunicationMessage.class, this);
    }
}
