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

import java.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Ordered communication message set.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridCommunicationMessageSet {
    /** */
    private final UUID nodeId;

    /** */
    private final long endTime;

    /** */
    private final UUID timeoutId;

    /** */
    private final String topic;

    /** */
    private final List<GridCommunicationMessage> msgs = new ArrayList<GridCommunicationMessage>();

    /** */
    private long nextMsgId = 1;

    /** */
    private boolean reserved = false;

    /**
     *
     * @param topic Communication topic.
     * @param nodeId Node ID.
     * @param endTime endTime.
     */
    public GridCommunicationMessageSet(String topic, UUID nodeId, long endTime) {
        assert nodeId != null : "ASSERTION [line=62, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]";
        assert topic != null : "ASSERTION [line=63, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]";

        this.nodeId = nodeId;
        this.topic = topic;
        this.endTime = endTime;

        timeoutId = UUID.randomUUID();
    }

    /**
     *
     * @return ID of node that sent the messages in the set.
     */
    UUID getNodeId() {
        return nodeId;
    }

    /**
     *
     * @return Timeout for messages in the set.
     */
    long getEndTime() {
        return endTime;
    }

    /**
     *
     * @return Message topic.
     */
    String getTopic() {
        return topic;
    }

    /**
     *
     * @return Timeout ID for registration with endTime processor.
     */
    UUID getTimeoutId() {
        return timeoutId;
    }

    /**
     *
     * @return <tt>True</tt> if sucessful.
     */
    boolean reserve() {
        assert Thread.holdsLock(this) == true : "ASSERTION [line=109, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]";

        if (reserved == true) {
            return false;
        }

        reserved = true;

        return true;
    }

    /**
     * Releases reservation.
     */
    void unreserve() {
        assert Thread.holdsLock(this) == true : "ASSERTION [line=124, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]";

        assert reserved == true : "ASSERTION [line=126, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]. " + "Message set was never reserved: " + this;

        reserved = false;
    }

    /**
     *
     * @return Session request.
     */
    Collection<GridCommunicationMessage> unwind() {
        assert Thread.holdsLock(this) == true : "ASSERTION [line=136, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]";

        assert reserved == true : "ASSERTION [line=138, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]";

        if (msgs.isEmpty() == true) {
            return Collections.emptyList();
        }

        List<GridCommunicationMessage> orderedMsgs = new LinkedList<GridCommunicationMessage>();

        for (Iterator<GridCommunicationMessage> iter = msgs.iterator(); iter.hasNext() == true;) {
            GridCommunicationMessage msg = iter.next();

            if (msg.getMessageId() == nextMsgId) {
                orderedMsgs.add(msg);

                nextMsgId++;

                iter.remove();
            }
            else {
                break;
            }
        }

        return orderedMsgs;
    }

    /**
     *
     * @param msg Message to add.
     */
    void add(GridCommunicationMessage msg) {
        assert Thread.holdsLock(this) == true : "ASSERTION [line=169, file=src/java/org/gridgain/grid/kernal/managers/communication/GridCommunicationMessageSet.java]";

        msgs.add(msg);

        Collections.sort(msgs, new Comparator<GridCommunicationMessage>() {
            /*
             *
             */
            public int compare(GridCommunicationMessage o1, GridCommunicationMessage o2) {
                return o1.getMessageId() < o2.getMessageId() ? -1 : o1.getMessageId() == o2.getMessageId() ? 0 : 1;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCommunicationMessageSet.class, this);
    }
}
