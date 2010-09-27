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

import java.util.concurrent.*;

import org.gridgain.apache.*;

/**
 * This class defines a handler for asynchronous task execution. It's similar in design
 * to standard JDK {@link Future} interface. Instance of this interface is returned from
 * the following methods:
 * <ul>
 *      <li>{@link Grid#execute(String, Object)}</li>
 *      <li>{@link Grid#execute(String, Object, long)}</li>
 *      <li>{@link Grid#execute(String, Object, long, GridTaskListener)}</li>
 *      <li>{@link Grid#execute(String, Object, GridTaskListener)}</li>
 * </ul>
 * <p>
 * <img src="{@docRoot}/img/gg_20.png" style="padding: 0px 5px 0px 0px" align="left"><h1 class="header">Migrating to GridGain 2.0</h1>
 * In GridGain 2.0 this interface API has been updated for better static type checking. Although the change is
 * trivial and provides much better type safety during development - it introduced
 * incompatibility with prior versions of GridGain. <a href="http://www.gridgainsystems.com:8080/wiki/display/GG15UG/Migration+To+GridGain+2.0+From+Previous+Version">Follow this link</a>
 * for easy source code migration instructions.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <R> Type of the task result returning from {@link GridTask#reduce(java.util.List)} method.
 */
@Apache20LicenseCompatible
public interface GridTaskFuture<R> {
    /**
     * Gets task session of execution grid task.
     *
     * @return Task session.
     */
    public GridTaskSession getTaskSession();

    /**
     * Synchronously waits for task completion and returns task computation result.
     * If task computation caused an exception, then it gets thrown out of this method.

     * @return Task computation result.
     * @throws GridTaskTimeoutException If task execution has timed out. Note that physically
     *      task may still be executing, as there is no practical way to stop it (however,
     *      every job within task will receive interrupt call).
     * @throws GridException If task execution resulted in exception.
     */
    public R get() throws GridTaskTimeoutException, GridException;

    /**
     * Synchronously waits if necessary for at most the given time for task completion
     * and returns task computation result. If task computation caused an exception,
     * then it gets thrown out of this method.
     *
     * @param timeout The maximum timeout in milliseconds.
     * @return Task computation result.
     * @throws GridTaskTimeoutException If task execution has timed out. Note that physically
     *      task may still be executing, as there is no practical way to stop it (however,
     *      every job within task will receive interrupt call).
     * @throws GridException If task execution resulted in exception.
     */
    public R get(long timeout) throws GridTaskTimeoutException, GridException;

    /**
     * Cancels this task.
     * <p>
     * Note, that there is no guarantee that a task will be cancelled after invoking this method.
     * Implementation will attempt to cancel all jobs spawned by this task by calling {@link GridJob#cancel()},
     * but it is up to the actual {@link GridJob} implementation to react on it and cancel
     * the execution. There is also no guarantee that every job spawned by the task will get the
     * cancellation request, especially if a job was traveling on the network at the time the cancel
     * request was issued.
     *
     * @throws GridException If task cancellation request was not successfully sent to all
     *      participating nodes.
     */
    public void cancel() throws GridException;

    /**
     * Checks if task is finished.
     *
     * @return <tt>true</tt> if task is finished, <tt>false</tt> otherwise.
     */
    public boolean isDone();

    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed normally.
     *
     * @return <tt>true</tt> if this task was cancelled before it completed normally.
     */
    public boolean isCancelled();
}
