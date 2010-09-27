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

package org.gridgain.grid;

import java.io.*;
import java.util.*;

import org.gridgain.apache.*;

/**
 * Grid events are used for notification about what happens within grid. Note that by
 * design GridGain keeps all events generated on the local node locally and it provides
 * APIs for performing a distributed queries across multiple nodes.
 * See {@link Grid#queryEvents(GridEventFilter, Collection, long)} and
 * {@link Grid#queryLocalEvents(GridEventFilter)} for information on how to query events
 * within grid.
 * <p>
 * You can also subscribe for local node event notifications through
 * {@link Grid#addLocalEventListener(GridLocalEventListener)} method.
 * <p>
 * Note that most of the properties of the event are optional. Only the following
 * properties are always set and are not <tt>null</tt>:
 * <ul>
 *      <li>{@link #getLocalNodeId()}</li>
 *      <li>{@link #getTimestamp()}</li>
 *      <li>{@link #getType()}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridEvent extends Serializable {
    /**
     * User code version of the node on which event occured.
     *
     * @return User code version.
     */
    public String getUserVersion();

    /**
     * Gets grid event type.
     *
     * @return Grid event type.
     */
    public GridEventType getType();

    /**
     * Gets event local node ID, i.e. the ID of the node where event was recorded.
     * Grid event always happens on some node so node is always present. Note that
     * this node ID is generally different from the one available via
     * {@link #getEventNodeId()} method.
     *
     * @return ID of the node where event was recorded.
     * @see #getEventNodeId()
     */
    public UUID getLocalNodeId();

    /**
     * Gets ID of the node related to this event, e.g. if node joined or left topology
     * this method will return ID of that node. Possibly <tt>null</tt> if
     * event is not node related. Note that this node ID is generally different
     * from the one available via {@link #getLocalNodeId()} method.
     *
     * @return ID of the node related to this event. Possibly <tt>null</tt> if
     *      event is not node related.
     * @see #getLocalNodeId()
     */
    public UUID getEventNodeId();

    /**
     * Gets checkpoint key associated with this event. Possibly <tt>null</tt> if
     * event is not checkpoint related.
     *
     * @return Checkpoint key associated with this event. Possibly <tt>null</tt> if
     *      event is not checkpoint related.
     */
    public String getCheckpointKey();

    /**
     * Gets name of the task that triggered the event. Possibly <tt>null</tt> if
     * event is not task related.
     *
     * @return Name of the task that triggered the event.
     */
    public String getTaskName();

    /**
     * Gets session ID of the task that triggered the event. Possibly <tt>null</tt> if
     * event is not task related.
     *
     * @return Session ID of the task that triggered the event.
     */
    public UUID getTaskSessionId();

    /**
     * Gets job ID, possibly <tt>null</tt> if event is not job related.
     *
     * @return Job ID.
     */
    public UUID getJobId();

    /**
     * Gets message associated with this event. Possibly <tt>null</tt> if event
     * provided no message.
     *
     * @return Message associated with this event.
     */
    public String getMessage();

    /**
     * Gets event timestamp. Timestamp is local to node on which this
     * event was produced. This event property is always set.
     *
     * @return Event timestamp.
     */
    public long getTimestamp();

    /**
     * Returns job result policy if event type is {@link GridEventType#JOB_RESULTED}.
     *
     * @return Result policy.
     */
    public GridJobResultPolicy getJobResultPolicy();
}
