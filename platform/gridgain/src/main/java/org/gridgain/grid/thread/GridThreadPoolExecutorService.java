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

package org.gridgain.grid.thread;

import java.util.concurrent.*;

/**
 * An {@link ExecutorService} that executes submitted tasks using pooled grid threads.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@SuppressWarnings({"UnnecessarilyQualifiedStaticUsage"})
public class GridThreadPoolExecutorService extends ThreadPoolExecutor {
    /** Default core pool size (value is <tt>100</tt>). */
    public static final int DFLT_CORE_POOL_SIZE = 100;

    /** Default maximum pool size (value is <tt>100</tt>). */
    public static final int DFLT_MAX_POOL_SIZE = 100;

    /**
     * Creates a new service with default initial parameters.
     * Default values are:
     * <table class="doctable">
     * <tr>
     *      <th>Name</th>
     *      <th>Default Value</th>
     * </tr>
     * <tr>
     *      <td>Core Pool Size</td>
     *      <td><tt>100</tt> (see {@link #DFLT_CORE_POOL_SIZE}).</td>
     * </tr>
     * <tr>
     *      <td>Maximum Pool Size</td>
     *      <td>None, is it is not used for unbounded queues.</td>
     * </tr>
     * <tr>
     *      <td>Keep alive time</td>
     *      <td>No limit (see {@link Long#MAX_VALUE}).</td>
     * </tr>
     * <tr>
     *      <td>Blocking Queue (see {@link BlockingQueue}).</td>
     *      <td>Unbounded linked blocking queue (see {@link LinkedBlockingQueue}).</td>
     * </tr>
     * </table>
     */
    public GridThreadPoolExecutorService() {
        this(DFLT_CORE_POOL_SIZE, DFLT_CORE_POOL_SIZE, 0,
            new LinkedBlockingQueue<Runnable>(), new GridThreadFactory(null), null);
    }

    /**
     * Creates a new service with the given initial parameters.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle.
     * @param maxPoolSize The maximum number of threads to allow in the pool.
     * @param keepAliveTime When the number of threads is greater than the core, this is the maximum time
     *      that excess idle threads will wait for new tasks before terminating.
     * @param workQueue The queue to use for holding tasks before they are executed. This queue will hold only
     *      runnable tasks submitted by the {@link #execute(Runnable)} method.
     */
    public GridThreadPoolExecutorService(int corePoolSize, int maxPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maxPoolSize, keepAliveTime, workQueue, new GridThreadFactory(null), null);
    }

    /**
     * Creates a new service with the given initial parameters.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle.
     * @param maxPoolSize The maximum number of threads to allow in the pool.
     * @param keepAliveTime When the number of threads is greater than the core, this is the maximum time
     *      that excess idle threads will wait for new tasks before terminating.
     * @param workQueue The queue to use for holding tasks before they are executed. This queue will hold only the
     *      runnable tasks submitted by the {@link #execute(Runnable)} method.
     * @param handler Optional handler to use when execution is blocked because the thread bounds and queue
     *      capacities are reached. If <tt>null</tt> then {@link java.util.concurrent.ThreadPoolExecutor.AbortPolicy}
     *      handler is used by default.
     */
    public GridThreadPoolExecutorService(int corePoolSize, int maxPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        this(corePoolSize, maxPoolSize, keepAliveTime, workQueue, new GridThreadFactory(null), handler);
    }

    /**
     * Creates a new service with default initial parameters.
     * Default values are:
     * <table class="doctable">
     * <tr>
     *      <th>Name</th>
     *      <th>Default Value</th>
     * </tr>
     * <tr>
     *      <td>Core Pool Size</td>
     *      <td><tt>100</tt> (see {@link #DFLT_CORE_POOL_SIZE}).</td>
     * </tr>
     * <tr>
     *      <td>Maximum Pool Size</td>
     *      <td>None, is it is not used for unbounded queues.</td>
     * </tr>
     * <tr>
     *      <td>Keep alive time</td>
     *      <td>No limit (see {@link Long#MAX_VALUE}).</td>
     * </tr>
     * <tr>
     *      <td>Blocking Queue (see {@link BlockingQueue}).</td>
     *      <td>Unbounded linked blocking queue (see {@link LinkedBlockingQueue}).</td>
     * </tr>
     * </table>
     *
     * @param gridName Name of the grid.
     */
    public GridThreadPoolExecutorService(String gridName) {
        this(DFLT_CORE_POOL_SIZE, DFLT_CORE_POOL_SIZE, 0,
            new LinkedBlockingQueue<Runnable>(), new GridThreadFactory(gridName), null);
    }

    /**
     * Creates a new service with the given initial parameters.
     *
     * @param gridName Name of the grid
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle.
     * @param maxPoolSize The maximum number of threads to allow in the pool.
     * @param keepAliveTime When the number of threads is greater than the core, this is the maximum time
     *      that excess idle threads will wait for new tasks before terminating.
     * @param workQueue The queue to use for holding tasks before they are executed. This queue will hold only
     *      runnable tasks submitted by the {@link #execute(Runnable)} method.
     */
    public GridThreadPoolExecutorService(String gridName, int corePoolSize, int maxPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue,
            new GridThreadFactory(gridName));
    }

    /**
     * Creates a new service with the given initial parameters.
     *
     * @param gridName Name of the grid.
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle.
     * @param maxPoolSize The maximum number of threads to allow in the pool.
     * @param keepAliveTime When the number of threads is greater than the core, this is the maximum time
     *      that excess idle threads will wait for new tasks before terminating.
     * @param workQueue The queue to use for holding tasks before they are executed. This queue will hold only the
     *      runnable tasks submitted by the {@link #execute(Runnable)} method.
     * @param handler Optional handler to use when execution is blocked because the thread bounds and queue
     *      capacities are reached. If <tt>null</tt> then {@link java.util.concurrent.ThreadPoolExecutor.AbortPolicy}
     *      handler is used by default.
     */
    public GridThreadPoolExecutorService(String gridName, int corePoolSize, int maxPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        this(corePoolSize, maxPoolSize, keepAliveTime, workQueue, new GridThreadFactory(gridName), handler);
    }

    /**
     * Creates a new service with the given initial parameters.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle.
     * @param maxPoolSize The maximum number of threads to allow in the pool.
     * @param keepAliveTime When the number of threads is greater than the core, this is the maximum time
     *      that excess idle threads will wait for new tasks before terminating.
     * @param workQueue The queue to use for holding tasks before they are executed. This queue will hold only the
     *      runnable tasks submitted by the {@link #execute(Runnable)} method.
     * @param threadFactory Thread factory.
     * @param handler Optional handler to use when execution is blocked because the thread bounds and queue
     *      capacities are reached. If <tt>null</tt> then {@link java.util.concurrent.ThreadPoolExecutor.AbortPolicy}
     *      handler is used by default.
     */
    public GridThreadPoolExecutorService(int corePoolSize, int maxPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue,
            threadFactory, handler == null ? new AbortPolicy() : handler);
    }
}
