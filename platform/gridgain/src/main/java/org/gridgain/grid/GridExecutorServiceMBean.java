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

import org.gridgain.apache.*;
import org.gridgain.grid.util.mbean.*;

/**
 * MBean that provides access to information about executor service. 
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
@GridMBeanDescription("MBean that provides access to information about executor service.")
public interface GridExecutorServiceMBean {
    /**
     * Returns the approximate number of threads that are actively executing tasks.
     *
     * @return The number of threads.
     */
    @GridMBeanDescription("Approximate number of threads that are actively executing tasks.")
    public int getActiveCount();

    /**
     * Returns the approximate total number of tasks that have completed execution.
     * Because the states of tasks and threads may change dynamically during
     * computation, the returned value is only an approximation, but one that
     * does not ever decrease across successive calls.
     *
     * @return The number of tasks.
     */
    @GridMBeanDescription("Approximate total number of tasks that have completed execution.")
    public long getCompletedTaskCount();

    /**
     * Returns the core number of threads.
     *
     * @return The core number of threads.
     */
    @GridMBeanDescription("The core number of threads.")
    public int getCorePoolSize();

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return The number of threads.
     */
    @GridMBeanDescription("Largest number of threads that have ever simultaneously been in the pool.")
    public int getLargestPoolSize();

    /**
     * Returns the maximum allowed number of threads.
     *
     * @return The maximum allowed number of threads.
     */
    @GridMBeanDescription("The maximum allowed number of threads.")
    public int getMaximumPoolSize();

    /**
     * Returns the current number of threads in the pool.
     *
     * @return The number of threads.
     */
    @GridMBeanDescription("Current number of threads in the pool.")
    public int getPoolSize();

    /**
     * Returns the approximate total number of tasks that have been scheduled
     * for execution. Because the states of tasks and threads may change dynamically
     * during computation, the returned value is only an approximation, but
     * one that does not ever decrease across successive calls.
     *
     * @return The number of tasks.
     */
    @GridMBeanDescription("Approximate total number of tasks that have been scheduled for execution.")
    public long getTaskCount();

    /**
     * Gets current size of the execution queue. This queue buffers local
     * executions when there are not threads available for processing in the pool.
     *
     * @return Current size of the execution queue.
     */
    @GridMBeanDescription("Current size of the execution queue.")
    public int getQueueSize();

    /**
     * Returns the thread keep-alive time, which is the amount of time which threads
     * in excess of the core pool size may remain idle before being terminated.
     *
     * @return Keep alive time.
     */
    @GridMBeanDescription("Thread keep-alive time, which is the amount of time which threads in excess of " +
        "the core pool size may remain idle before being terminated.")
    public long getKeepAliveTime();

    /**
     * Returns <tt>true</tt> if this executor has been shut down.
     *
     * @return <tt>True</tt> if this executor has been shut down.
     */
    @GridMBeanDescription("True if this executor has been shut down.")
    public boolean isShutdown();

    /**
     * Returns <tt>true</tt> if all tasks have completed following shut down. Note that
     * <tt>isTerminated()</tt> is never <tt>true</tt> unless either <tt>shutdown()</tt> or
     * <tt>shutdownNow()</tt> was called first.
     *
     * @return <tt>True</tt> if all tasks have completed following shut down.
     */
    @GridMBeanDescription("True if all tasks have completed following shut down.")
    public boolean isTerminated();

    /**
     * Returns <tt>true</tt> if this executor is in the process of terminating after
     * <tt>shutdown()</tt> or <tt>shutdownNow()</tt> but has not completely terminated.
     * This method may be useful for debugging. A return of <tt>true</tt> reported a
     * sufficient period after shutdown may indicate that submitted tasks have ignored
     * or suppressed interruption, causing this executor not to properly terminate.
     *
     * @return <tt>True</tt> if terminating but not yet terminated.
     */
    @GridMBeanDescription("True if terminating but not yet terminated.")
    public boolean isTerminating();

    /**
     * Returns the class name of current rejection handler.
     *
     * @return Class name of current rejection handler.
     */
    @GridMBeanDescription("Class name of current rejection handler.")
    public String getRejectedExecutionHandlerClass();

    /**
     * Returns the class name of thread factory used to create new threads.
     *
     * @return Class name of thread factory used to create new threads.
     */
    @GridMBeanDescription("Class name of thread factory used to create new threads.")
    public String getThreadFactoryClass();
}
