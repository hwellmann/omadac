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

package org.gridgain.grid.kernal.managers.communication;

import java.io.*;
import java.util.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridCommunicationMessage implements Externalizable {
    /** Sender ID. */
    private UUID senderId = null;
    
    /** */
    private List<UUID> destIds = null;

    /** Message topic. */
    private String topic = null;

    /** Message order. */
    private long msgId = -1;

    /** Message timeout. */
    private long timeout = 0;

    /** Message body. */
    private GridByteArrayList msg = null;

    /** Message processing policy. */
    private GridCommunicationThreadPolicy policy = null;

    /** Message receive time. */
    private final transient long rcvTime = System.currentTimeMillis();

    /**
     * No-op constructor to support {@link Externalizable} interface.
     * This constructor is not meant to be used for other purposes.
     */
    public GridCommunicationMessage() {
        // No-op.
    }

    /**
     *
     * @param senderId Node ID.
     * @param destId Destination ID.
     * @param topic Communication topic.
     * @param msg Communication message.
     * @param policy Thread policy.
     */
    public GridCommunicationMessage(UUID senderId, UUID destId, String topic, GridByteArrayList msg,
        GridCommunicationThreadPolicy policy) {
        this(senderId, Collections.singletonList(destId), topic, msg, policy);
    }

    /**
     *
     * @param senderId Node ID.
     * @param destIds Destination IDs.
     * @param topic Communication topic.
     * @param msg Communication message.
     * @param policy Thread policy.
     */
    public GridCommunicationMessage(UUID senderId, List<UUID> destIds, String topic, GridByteArrayList msg,
        GridCommunicationThreadPolicy policy) {
        assert senderId != null : "ASSERTION [line=91, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessage.java]";
        assert destIds != null : "ASSERTION [line=92, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessage.java]";
        assert topic != null : "ASSERTION [line=93, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessage.java]";
        assert policy != null : "ASSERTION [line=94, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessage.java]";
        assert msg != null : "ASSERTION [line=95, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessage.java]";

        this.senderId = senderId;
        this.destIds = destIds;
        this.msg = msg;
        this.topic = topic;
        this.policy = policy;
    }

    /**
     *
     * @param senderId Node ID.
     * @param destId Destination node ID.
     * @param topic Communication topic.
     * @param msg Communication message.
     * @param policy Thread policy.
     * @param msgId Message ID.
     * @param timeout Timeout.
     */
    public GridCommunicationMessage(UUID senderId, UUID destId, String topic, GridByteArrayList msg,
        GridCommunicationThreadPolicy policy, long msgId, long timeout) {
        this(senderId, Collections.singletonList(destId), topic, msg, policy);

        assert msgId > 0 : "ASSERTION [line=118, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessage.java]";
        assert timeout > 0 : "ASSERTION [line=119, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessage.java]";

        this.msgId = msgId;
        this.timeout = timeout;
    }

    /**
     *
     * @return FIXDOC
     */
    String getTopic() {
        return topic;
    }

    /**
     *
     * @return FIXDOC
     */
    GridByteArrayList getMessage() {
        return msg;
    }

    /**
     *
     * @return FIXDOC
     */
    GridCommunicationThreadPolicy getPolicy() {
        return policy;
    }

    /**
     *
     * @return Message ID.
     */
    long getMessageId() {
        return msgId;
    }

    /**
     *
     * @return Message timeout.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     *
     * @return <tt>True</tt> if message is ordered, <tt>false</tt> otherwise.
     */
    boolean isOrdered() {
        return msgId > 0;
    }

    /**
     *
     * @return Sender node ID.
     */
    UUID getSenderId() {
        return senderId;
    }

    /**
     *
     * @return Gets destination node IDs.
     */
    List<UUID> getDestinationIds() {
        return destIds;
    }

    /**
     *
     * @return Message receive time.
     */
    long getReceiveTime() {
        return rcvTime;
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(msg);
        out.writeObject(policy);
        out.writeLong(msgId);
        out.writeLong(timeout);

        GridUtils.writeUUID(out, senderId);
        GridUtils.writeString(out, topic);

        // Write destination IDs.
        out.writeInt(destIds.size());

        // Purposely don't use foreach loop for
        // better performance.
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < destIds.size(); i++) {
            GridUtils.writeUUID(out, destIds.get(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        msg = (GridByteArrayList)in.readObject();
        policy = (GridCommunicationThreadPolicy)in.readObject();
        msgId = in.readLong();
        timeout = in.readLong();

        senderId = GridUtils.readUUID(in);
        topic = GridUtils.readString(in);

        int size = in.readInt();

        if (size == 1) {
            destIds = Collections.singletonList(GridUtils.readUUID(in));
        }
        else {
            destIds = new ArrayList<UUID>(size);

            for (int i = 0; i < size; i++) {
                destIds.add(GridUtils.readUUID(in));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof GridCommunicationMessage == false) {
            return false;
        }

        GridCommunicationMessage other = (GridCommunicationMessage)obj;

        return
            policy == other.policy &&
            topic.equals(other.topic) == true &&
            msgId == other.msgId &&
            senderId.equals(other.senderId) == true &&
            destIds.equals(other.destIds) == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int res = topic.hashCode();

        res = 31 * res + (int)(msgId ^ (msgId >>> 32));
        res = 31 * res + msg.hashCode();
        res = 31 * res + policy.hashCode();
        res = 31 * res + senderId.hashCode();
        res = 31 * res + topic.hashCode();

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCommunicationMessage.class, this);
    }
}
