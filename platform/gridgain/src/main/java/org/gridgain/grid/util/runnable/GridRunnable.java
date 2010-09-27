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

package org.gridgain.grid.util.runnable;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Extension to standard {@link Runnable} interface. Adds proper details to be used
 * with {@link Executor} implementations. Only for internal use.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public abstract class GridRunnable implements Runnable {
    /** Grid logger. */
    private static final AtomicReference<GridLogger> clsLog = new AtomicReference<GridLogger>(null);
    
    /** Thread name. */
    private final String name;

    /** */
    private final String gridName;

    /** */
    private final GridRunnableListener listener;

    /** Deferred result. */
    private volatile FutureTask<Void> future = null;

    /** Whether or not this runnable is cancelled. */
    private volatile boolean isCancelled = false;

    /** Actual thread runner. */
    private volatile Thread runner = null;

    /**
     * Creates new grid runnable with given parameters.
     *
     * @param gridName Name of grid this runnable is used in.
     * @param name Runnable thread name.
     * @param log Grid logger to be used.
     * @param listener Listener for life-cycle events.
     */
    protected GridRunnable(String gridName, String name, GridLogger log, GridRunnableListener listener) {
        assert name != null : "ASSERTION [line=68, file=src/java/org/gridgain/grid/util/runnable/GridRunnable.java]";
        assert log != null : "ASSERTION [line=69, file=src/java/org/gridgain/grid/util/runnable/GridRunnable.java]";

        this.gridName = gridName;
        this.name = name;
        this.listener = listener;
        
        if (clsLog.get() == null) {
            clsLog.compareAndSet(null, log.getLogger(GridRunnable.class));
        }

        reset();
    }

    /**
     * Creates new grid runnable with given parameters.
     *
     * @param gridName Name of grid this runnable is used in.
     * @param name Runnable thread name.
     * @param log Grid logger to be used.
     */
    protected GridRunnable(String gridName, String name, GridLogger log) {
        this(gridName, name, log, null);
    }

    /**
     * Resets this instance in case if it needs to be executed more than once.
     */
    public final void reset() {
        isCancelled = false;

        future = new FutureTask<Void>(new Runnable() {
            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("CatchGenericClass")
            public final void run() {
                GridRunnableGroup.getInstance(gridName).onStart(GridRunnable.this);

                runner = Thread.currentThread();

                String threadName = null;

                GridLogger log = clsLog.get();
                
                // Reset thread name only if debug is enabled, as it is very expensive
                // operation. Otherwise, then name of the pooled thread will remain.
                if (log.isDebugEnabled() == true) {
                    threadName = runner.getName();
                    
                    // Reset thread name.
                    StringBuilder builder = GridStringBuilderFactory.acquire();
                    
                    try {
                        runner.setName(builder.append(threadName).append(':').append(name).toString());
                    }
                    finally {
                        GridStringBuilderFactory.release(builder);
                    }

                    log.debug("Grid runnable started: " + name);
                }
                
                try {
                    // Special case, when task gets cancelled before it got scheduled.
                    if (isCancelled == true) {
                        runner.interrupt();
                    }

                    // Listener callback.
                    if (listener != null) {
                        listener.onStarted(GridRunnable.this);
                    }

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

                    if (listener != null) {
                        listener.onStopped(GridRunnable.this);
                    }

                    if (log.isDebugEnabled() == true) {
                        if (isCancelled == true) {
                            log.debug("Grid runnable finished due to cancellation: " + name);
                        }
                        else if (runner.isInterrupted() == true) {
                            log.debug("Grid runnable finished due to interruption without cancellation: " + name);
                        }
                        else {
                            log.debug("Grid runnable finished normally: " + name);
                        }
                    }

                    GridRunnableGroup.getInstance(gridName).onFinish(GridRunnable.this);

                    if (threadName != null) {
                        // Reset thread name.
                        runner.setName(threadName);
                    }
                    
                    // Need to set runner to null, to make sure that
                    // further operations on this runnable won't
                    // affect the thread which could have been recycled
                    // by thread pool.
                    runner = null;
                }
            }
        }, null);
    }

    /**
     * {@inheritDoc}
     */
    public final void run() {
        future.run();
    }

    /**
     * The implementation should provide the execution body for this runnable.
     *
     * @throws InterruptedException Thrown in case of interruption.
     */
    protected abstract void body() throws InterruptedException;

    /**
     * Optional method that will be called after runnable is finished. Default
     * implementation is no-op.
     */
    protected void cleanup() {
        // No-op.
    }

    /**
     * Gets name of the grid this runnable belongs to.
     *
     * @return Name of the grid this runnable belongs to.
     */
    public String getGridName() {
        return gridName;
    }

    /**
     * Cancels this runnable interrupting actual runner.
     */
    public void cancel() {
        if (clsLog.get().isDebugEnabled() == true) {
            clsLog.get().debug("Cancelling grid runnable: " + this);
        }

        isCancelled = true;

        Thread runner = this.runner;

        // Cannot call Future.cancel() because if we do, then Future.get() would always
        // throw CancellationException and we would not be able to wait for task completion.
        if (runner != null) {
            runner.interrupt();
        }
    }

    /**
     * Joins this runnable.
     *
     * @throws InterruptedException Thrown in case of interruption.
     */
    public void join() throws InterruptedException {
        GridLogger log = clsLog.get();
        
        if (log.isDebugEnabled() == true) {
            log.debug("Joining grid runnable: " + this);
        }

        try {
            future.get();
        }
        catch (ExecutionException e) {
            log.error("Grid runnable execution threw an error: " + this, e);

            // Should never happen as no exception should penetrate
            // through the 'run' method.
            assert false : "ASSERTION [line=260, file=src/java/org/gridgain/grid/util/runnable/GridRunnable.java]. " + "Grid runnable execution should never throw errors: " + this;
        }
        catch (CancellationException e) {
            String err = "Grid runnable got cancelled via Future.cancel() which should never be called: " + this;

            log.error(err, e);

            assert false : "ASSERTION [line=267, file=src/java/org/gridgain/grid/util/runnable/GridRunnable.java]. " + err;
        }
    }

    /**
     * Tests whether or not this runnable is cancelled.
     *
     * @return <tt>true</tt> if this runnable is cancelled - <tt>false</tt> otherwise.
     * @see Future#isCancelled()
     */
    public boolean isCancelled() {
        return isCancelled == true;
    }

    /**
     * Tests whether or not this runnable is finished.
     *
     * @return <tt>true</tt> if this runnable is finished - <tt>false</tt> otherwise.
     * @see Future#isDone()
     */
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridRunnable.class, this);
    }
}
