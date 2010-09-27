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

package org.gridgain.grid.test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.thread.*;

/**
 * Special {@link ExecutorService} executor service to be used for JUnit execution.
 * This executor service is specified by default in <tt>$GRIDGAIN_HOME/config/junit/junit-spring.xml</tt>.
 * Every thread created and used within this executor service will belong to its own
 * {@link ThreadGroup} instance that is different from all other threads. This way all threads
 * spawned by a single unit test will belong to the same thread group. This feature is
 * used for logging to be able to group log statements from different threads for individual
 * tests.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridTestExecutorService extends ThreadPoolExecutor {
    /** Default core pool size (value is <tt>100</tt>). */
    public static final int DFLT_CORE_POOL_SIZE = 50;

    /** Default maximum pool size (value is <tt>100</tt>). */
    public static final int DFLT_MAX_POOL_SIZE = 50;

    /**
     * Creates a new service with default initial parameters. Each created thread will
     * belong to its own group.
     * <p>
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
    public GridTestExecutorService(String gridName) {
        super(DFLT_CORE_POOL_SIZE, DFLT_CORE_POOL_SIZE, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
            new LinkedBlockingQueue<Runnable>(), new GridJunitThreadFactory(gridName));
    }

    /**
     * Creates a new service with the given initial parameters. Each created thread
     * will belong to its own group.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle.
     * @param maxPoolSize The maximum number of threads to allow in the pool.
     * @param keepAliveTime When the number of threads is greater than the core, this is the maximum time
     *      that excess idle threads will wait for new tasks before terminating.
     * @param workQueue The queue to use for holding tasks before they are executed. This queue will hold only
     *      runnable tasks submitted by the {@link #execute(Runnable)} method.
     * @param gridName Name of the grid.
     */
    public GridTestExecutorService(String gridName, int corePoolSize, int maxPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue,
            new GridJunitThreadFactory(gridName));
    }

    /**
     * Creates a new service with the given initial parameters. Each created thread will
     * belong to its own group.
     *
     * @param corePoolSize The number of threads to keep in the pool, even if they are idle.
     * @param maxPoolSize The maximum number of threads to allow in the pool.
     * @param keepAliveTime When the number of threads is greater than the core, this is the maximum time
     *      that excess idle threads will wait for new tasks before terminating.
     * @param workQueue The queue to use for holding tasks before they are executed. This queue will hold only the
     *      runnable tasks submitted by the {@link #execute(Runnable)} method.
     * @param handler The handler to use when execution is blocked because the thread bounds and queue
     *      capacities are reached.
     * @param gridName Name of the grid.
     */
    public GridTestExecutorService(String gridName, int corePoolSize, int maxPoolSize, long keepAliveTime,
        BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue,
            new GridJunitThreadFactory(gridName), handler);
    }

    /**
     * This class provides implementation of {@link ThreadFactory}  factory
     * for creating JUnit grid threads. Note that in order to properly
     * sort out output from every thread, we create a new thread group for
     * every thread.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private static class GridJunitThreadFactory implements ThreadFactory {
        /** Grid name. */
        private final String gridName;

        /** Number of all system threads in the system. */
        private static final AtomicLong grpCntr = new AtomicLong(0);

        /**
         * Constructs new thread factory for given grid. All threads will belong
         * to the same default thread group.
         *
         * @param gridName Grid name.
         */
        GridJunitThreadFactory(String gridName) {
            this.gridName = gridName;
        }

        /**
         * {@inheritDoc}
         */
        public Thread newThread(Runnable r) {
            ThreadGroup parent = Thread.currentThread().getThreadGroup();

            while (parent instanceof GridJunitThreadGroup == true) {
                parent = parent.getParent();
            }

            return new GridThread(new GridJunitThreadGroup(parent, "gridgain-#" + grpCntr.incrementAndGet()),
                gridName, "gridgain", r);
        }

        /**
         *
         */
        private class GridJunitThreadGroup extends ThreadGroup {
            /**
             *
             * @param parent Group parent.
             * @param name Group name.
             */
            GridJunitThreadGroup(ThreadGroup parent, String name) {
                super(parent, name);

                assert parent instanceof GridJunitThreadGroup == false : "ASSERTION [line=172, file=src/java/org/gridgain/grid/test/GridTestExecutorService.java]";
            }
        }
    }
}
