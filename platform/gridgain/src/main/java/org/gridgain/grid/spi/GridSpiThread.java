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

package org.gridgain.grid.spi;

import java.util.concurrent.atomic.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides convenient adapter for threads used by SPIs.
 * This class adds necessary plumbing on top of the {@link Thread} class:
 * <ul>
 * <li>Consistent naming of threads</li>
 * <li>Dedicated parent thread group</li>
 * <li>Backing interrupted flag</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public abstract class GridSpiThread extends Thread {
    /** Default thread's group. */
    public static final ThreadGroup DFLT_GRP = new ThreadGroup("gridgain-spi");

    /** Number of all system threads in the system. */
    private static final AtomicLong cntr = new AtomicLong(0);

    /** Grid logger. */
    private final GridLogger log;

    /**
     * Internally maintained thread interrupted flag to avoid any bug issues with {@link Thread}
     * native implementation. May have been fixed in JDK 5.0.
     */
    private volatile boolean interrupted = false;

    /**
     * Creates thread with given <tt>name</tt>.
     *
     * @param gridName Name of grid this thread is created in.
     * @param name Thread's name.
     * @param log Grid logger to use.
     */
    protected GridSpiThread(String gridName, String name, GridLogger log) {
        super(DFLT_GRP, name + "-#" + cntr.incrementAndGet() + '%' + gridName);

        assert log != null : "ASSERTION [line=66, file=src/java/org/gridgain/grid/spi/GridSpiThread.java]";

        this.log = log;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInterrupted() {
        return super.isInterrupted() == true || interrupted == true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void interrupt() {
        interrupted = true;

        super.interrupt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"CatchGenericClass"})
    public final void run() {
        try {
            body();
        }
        catch (InterruptedException e) {
            if (log.isDebugEnabled() == true) {
                log.debug("Caught interrupted exception: " + e);
            }
        }
        // Catch everything to make sure that it gets logged properly and
        // not to kill any threads from the underlying thread pool.
        catch (Throwable e) {
            log.error("Runtime error caught during grid runnable execution: " + this, e);
        }
        finally {
            cleanup();

            if (log.isDebugEnabled() == true) {
                if (isInterrupted() == true) {
                    log.debug("Grid runnable finished due to interruption without cancellation: " + getName());
                }
                else {
                    log.debug("Grid runnable finished normally: " + getName());
                }
            }
        }
    }

    /**
     * Should be overridden by child classes if cleanup logic is required.
     */
    protected void cleanup() {
        // No-op.
    }

    /**
     * Body of SPI thread.
     *
     * @throws InterruptedException If thread got interrupted.
     */
    protected abstract void body() throws InterruptedException;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridSpiThread.class, this, "name", getName());
    }
}
