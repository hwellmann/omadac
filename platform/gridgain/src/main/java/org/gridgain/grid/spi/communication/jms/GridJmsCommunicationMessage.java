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

package org.gridgain.grid.spi.communication.jms;

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Wrapper of real message. This implementation is a helper class that keeps
 * source node ID and destination node IDs beside the message itself.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridJmsCommunicationMessage implements Externalizable {
    /** Message that is being sent. */
    private Serializable msg;

    /** List of node UIDs message is being sent to. */
    private List<UUID> nodeIds;

    /** Source node UID which is sending the message. */
    @GridToStringExclude
    private UUID nodeId;

    /**
     * No-op constructor to support {@link Externalizable} interface.
     * This constructor is not meant to be used for other purposes.
     */
    public GridJmsCommunicationMessage() {
        // No-op.
    }

    /**
     * Creates instance of communication message. All parameters are mandatory.
     *
     * @param nodeId Source node UID.
     * @param msg Message that is being sent.
     * @param nodeIds Destination node UIDs
     */
    GridJmsCommunicationMessage(UUID nodeId, Serializable msg, List<UUID> nodeIds) {
        assert nodeId != null : "ASSERTION [line=63, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationMessage.java]";
        assert msg != null : "ASSERTION [line=64, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationMessage.java]";
        assert nodeIds != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/spi/communication/jms/GridJmsCommunicationMessage.java]";

        this.nodeId = nodeId;
        this.msg = msg;
        this.nodeIds = nodeIds;
    }

    /**
     * Gets UID of the node which sent this message.
     *
     * @return Source node UID.
     */
    UUID getNodeId() {
        return nodeId;
    }

    /**
     * Gets message that was sent.
     *
     * @return Sent message.
     */
    Serializable getMessage() {
        return msg;
    }

    /**
     * Gets UIDs of nodes this message was sent to.
     *
     * @return Destination node UIDs.
     */
    List<UUID> getNodesIds() {
        return nodeIds;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(msg);

        int destNodes = nodeIds.size();

        out.writeInt(destNodes);

        if (destNodes > 0) {
            for (UUID id : nodeIds) {
                GridUtils.writeUUID(out, id);
            }
        }

        GridUtils.writeUUID(out, nodeId);
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        msg = (Serializable)in.readObject();

        int destNodes = in.readInt();

        // Note, that all deserialized lists will be created as ArrayList.
        nodeIds = new ArrayList<UUID>(destNodes);

        if (destNodes > 0) {
            for (int i = 0; i < destNodes; i++) {
                nodeIds.add(GridUtils.readUUID(in));
            }
        }

        nodeId = GridUtils.readUUID(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridJmsCommunicationMessage.class, this);
    }
}
