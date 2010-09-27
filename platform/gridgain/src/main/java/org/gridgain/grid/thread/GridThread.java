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

import java.util.concurrent.atomic.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class adds some necessary plumbing on top of the {@link Thread} class.
 * Specifically, it adds:
 * <ul>
 *      <li>Consistent naming of threads</li>
 *      <li>Dedicated parent thread group</li>
 *      <li>Backing interrupted flag</li>
 * </ul>
 * <b>Note</b>: this class is intended for internal use only.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridThread extends Thread {
    /** Default thread's group. */
    public static final ThreadGroup DFLT_GRP = new ThreadGroup("gridgain");

    /** Number of all grid threads in the system. */
    private static final AtomicLong threadCntr = new AtomicLong(0);

    /**
     * Internally maintained thread interrupted flag to avoid any bug issues with {@link Thread}
     * native implementation. May have been fixed in JDK 5.0.
     */
    private final AtomicBoolean interrupted = new AtomicBoolean(false);

    /**
     * Creates thread with given <tt>name</tt>.
     *
     * @param poolName Thread's pool name.
     * @param r Runnable to run.
     */
    public GridThread(String poolName, Runnable r) {
        this(DFLT_GRP, null, poolName, r);
    }

    /**
     * Creates grid thread with given name for a given grid.
     *
     * @param gridName Name of grid this thread is created for.
     * @param threadName Name of thread.
     * @param r Runnable to execute.
     */
    public GridThread(String gridName, String threadName, Runnable r) {
        this(DFLT_GRP, gridName, threadName, r);
    }

    /**
     * Creates grid thread with given name for a given grid with specified
     * thread group.
     *
     * @param parent Parent thread group.
     * @param gridName Name of grid this thread is created for.
     * @param threadName Name of thread.
     * @param r Runnable to execute.
     */
    public GridThread(ThreadGroup parent, String gridName, String threadName, Runnable r) {
        super(parent, r, createName(threadCntr.incrementAndGet(), threadName, gridName));
    }

    /**
     * Creates new thread name.
     *
     * @param num Thread number.
     * @param threadName Thread name.
     * @param gridName Grid name.
     * @return New thread name.
     */
    private static String createName(long num, String threadName, String gridName) {
        return threadName + "-#" + num + '%' + gridName + '%';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInterrupted() {
        return super.isInterrupted() == true || interrupted.get() == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interrupt() {
        interrupted.set(true);

        super.interrupt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridThread.class, this, "name", getName());
    }
}
