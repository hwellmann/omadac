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
 * Types of grid events. Note that these events are stored locally when generated by
 * actions on the local node and can be queried using:
 * <ul>
 *      <li>
 *          {@link Grid#queryEvents(GridEventFilter, Collection, long)} - querying
 *          events occurred on the nodes specified, including remote nodes (note
 *          that {@link GridEventFilter} must implement {@link Serializable}.
 *      </li>
 *      <li>
 *          {@link Grid#queryLocalEvents(GridEventFilter)} - querying only local
 *          events stored on this local node.
 *      </li>
 *      <li>
 *          {@link Grid#addLocalEventListener(GridLocalEventListener)} - listening
 *          to local grid events (events from remote nodes not included).
 *      </li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public enum GridEventType {
    /**
     * Node joined topology.
     */
    NODE_JOINED,

    /**
     * Node left topology.
     */
    NODE_LEFT,

    /**
     * Node failed and left topology unexpectedly.
     */
    NODE_FAILED,

    /**
     * Checkpoint was saved.
     */
    CHECKPOINT_SAVED,

    /**
     * Checkpoint was loaded.
     */
    CHECKPOINT_LOADED,

    /**
     * Checkpoint was removed (either timeout expired, or manually removed,
     * or automatically removed by task session).
     */
    CHECKPOINT_REMOVED,

    /**
     * Task got started.
     */
    TASK_STARTED,

    /**
     * Task got finished. This event is called every time
     * a task finished without exception.
     *
     * @see #TASK_FAILED
     */
    TASK_FINISHED,

    /**
     * Task failed. This event is called every time a task finished with an exception.
     * Note that prior to this event, there could be other events recorded specific
     * to the failure.
     *
     * @see #TASK_FINISHED
     */
    TASK_FAILED,

    /**
     * Non-task class deployed event.
     */
    CLASS_DEPLOYED,

    /**
     * Non-task class undeployed event.
     */
    CLASS_UNDEPLOYED,

    /**
     * Non-task class deployment failed.
     */
    CLASS_DEPLOYMENT_FAILED,

    /**
     * Task deployed event.
     */
    TASK_DEPLOYED,

    /**
     * Task deployment failed.
     */
    TASK_DEPLOYMENT_FAILED,

    /**
     * Task undeployed event.
     */
    TASK_UNDEPLOYED,

    /**
     * Grid job was mapped in {@link GridTask#map(List, Object)} method.
     */
    JOB_MAPPED,

    /**
     * Grid job result was received by {@link GridTask#result(GridJobResult, List)} method.
     */
    JOB_RESULTED,

    /**
     * All split jobs' results were reduced for the task in {@link GridTask#reduce(List)} method.
     */
    TASK_REDUCED,

    /**
     * Job got failed over.
     */
    JOB_FAILED_OVER,

    /**
     * Job got started.
     */
    JOB_STARTED,

    /**
     * Job has successfully completed and produced a result which from the user perspective
     * can still be either negative or positive.
     */
    JOB_FINISHED,

    /**
     * Job timed out.
     */
    JOB_TIMED_OUT,

    /**
     * Job has been rejected.
     */
    JOB_REJECTED,

    /**
     * Task timed out.
     */
    TASK_TIMED_OUT,

    /**
     * Job has failed. This means that there was some error event during job execution
     * and job did not produce a result.
     */
    JOB_FAILED,

    /**
     * Job arrived for execution and has been queued (added to passive queue during
     * collision resolution).
     */
    JOB_QUEUED,

    /**
     * Job got cancelled.
     */
    JOB_CANCELLED,

    /**
     * Session attribute(s) got set.
     */
    SESSION_ATTR_SET
}
