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
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.*;

/**
 * Pool of runnable workers. This class automatically takes care of
 * error handling that has to do with executing a runnable task and
 * ensures that all tasks are finished when stop occurs.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridRunnablePool {
    /** Dummy object for concurrent map. */
    private static final Object DUMMY = new Object();

    /** */
    private final Executor exec;

    /** */
    private final GridLogger log;

    /** */
    private final ConcurrentMap<GridRunnable, Object> workers = new ConcurrentHashMap<GridRunnable, Object>();

    /**
     *
     * @param exec Executor service.
     * @param log Logger to use.
     */
    public GridRunnablePool(Executor exec, GridLogger log) {
        assert exec != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/util/runnable/GridRunnablePool.java]";
        assert log != null : "ASSERTION [line=57, file=src/java/org/gridgain/grid/util/runnable/GridRunnablePool.java]";

        this.exec = exec;
        this.log = log;
    }

    /**
     * Schedules runnable task for execution.
     *
     * @param r Runnable task.
     * @throws GridException Thrown if any exception occurred.
     */
    @SuppressWarnings({"CatchGenericClass", "ProhibitedExceptionThrown"})
    public void execute(final GridRunnable r) throws GridException {
        workers.put(r, DUMMY);

        try {
            exec.execute(new Runnable() {
                /**
                 * {@inheritDoc}
                 */
                public void run() {
                    try {
                        r.run();
                    }
                    finally {
                        workers.remove(r);
                    }
                }
            });
        }
        catch (RuntimeException e) {
            workers.remove(r);

            throw (GridException)new GridException("Failed to execute work due to runtime exception (possible execution rejection", e).setData(91, "src/java/org/gridgain/grid/util/runnable/GridRunnablePool.java");
        }
        catch (Error e) {
            workers.remove(r);

            throw e;
        }
    }

    /**
     * Waits for all workers to finish.
     *
     * @param cancel Flag to indicate whether workers should be cancelled
     *      before waiting for them to finish.
     */
    public void join(boolean cancel) {
        if (cancel == true) {
            GridUtils.cancel(workers.keySet());
        }

        // Record current interrupted status of calling thread.
        boolean interrupted = Thread.interrupted();

        GridUtils.join(workers.keySet(), log);

        // Reset interrupted flag on calling thread.
        if (interrupted == true) {
            Thread.currentThread().interrupt();
        }
    }
}
