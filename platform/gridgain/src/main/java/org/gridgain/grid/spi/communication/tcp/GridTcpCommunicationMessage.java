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

package org.gridgain.grid.spi.communication.tcp;

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of TCP message. A kind of container for all communication messages
 * over TCP/IP protocol. Beside message itself it provides sender node id.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridTcpCommunicationMessage implements Externalizable {
    /** Communication message. */
    private Serializable msg = null;

    /** Sender node unique identifier. */
    private UUID nodeId = null;

    /**
     * No-op constructor to support {@link Externalizable} interface.
     * This constructor is not meant to be used for other purposes.
     */
    public GridTcpCommunicationMessage() {
        // No-op.
    }

    /**
     * Creates new instance of wrapper.
     *
     * @param nodeId    Sender node UID.
     * @param msg       Message is being sent.
     */
    GridTcpCommunicationMessage(UUID nodeId, Serializable msg) {
        assert nodeId != null : "ASSERTION [line=58, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationMessage.java]";
        assert msg != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/spi/communication/tcp/GridTcpCommunicationMessage.java]";

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
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(msg);

        GridUtils.writeUUID(out, nodeId);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        msg = (Serializable)in.readObject();

        nodeId = GridUtils.readUUID(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridTcpCommunicationMessage.class, this);
    }
}
