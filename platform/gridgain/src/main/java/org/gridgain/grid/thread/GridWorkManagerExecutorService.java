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

import commonj.work.*;
import java.util.*;
import java.util.concurrent.*;
import javax.naming.*;
import org.gridgain.grid.*;

/**
 * CommonJ-based wrapper for {@link ExecutorService}.
 * Implementation delegates all execution request to the work manager. Note that CommonJ
 * work manager is used and/or supported by wide verity of application servers and frameworks
 * such as Coherence, Weblogic, Websphere, Spring, Globus, apache projects, etc.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridWorkManagerExecutorService extends AbstractExecutorService {
    /** Work manager with all tasks are delegated to. */
    private final WorkManager workMgr;

    /** Whether service is being stopped or not. */
    private boolean isBeingShutdown = false;

    /** List of executing or scheduled for execution works. */
    private final List<WorkItem> works = new ArrayList<WorkItem>();

    /** Rejected or completed tasks listener. */
    private final WorkTerminateListener termListener = new WorkTerminateListener();

    /** */
    private final Object mux = new Object();

    /**
     * Creates new instance of execution service based on CommonJ implementation.
     *
     * @param jndiName Work manager JNDI name.
     * @throws GridException If work manager is unreachable.
     */
    @SuppressWarnings({"JNDIResourceOpenedButNotSafelyClosed"})
    public GridWorkManagerExecutorService(String jndiName) throws GridException {
        InitialContext ctx = null;

        try {
            // Create initial context and obtain work manager.
            ctx = new InitialContext();

            workMgr = (WorkManager)ctx.lookup(jndiName);
        }
        catch(NamingException e) {
            throw (GridException)new GridException("Failed to obtain initial context or lookup given JNDI name: " + jndiName, e).setData(72, "src/java/org/gridgain/grid/thread/GridWorkManagerExecutorService.java");
        }
        finally {
            closeCtx(ctx);
        }
    }

    /**
     * @param ctx JNDI context to close. 
     * @throws GridException Thrown in case of failure closing initial context.
     */
    private void closeCtx(InitialContext ctx) throws GridException {
        if (ctx != null) {
            try {
                ctx.close();
            }
            catch (NamingException e) {
                throw (GridException)new GridException("Failed to close initial context.", e).setData(89, "src/java/org/gridgain/grid/thread/GridWorkManagerExecutorService.java");
            }
        }
    }

    /**
     * Creates new instance of execution service based on CommonJ implementation.
     *
     * @param mngr Work manager.
     */
    public GridWorkManagerExecutorService(WorkManager mngr) {
        workMgr = mngr;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Runnable command) {
        RunnableWorkAdapter work = new RunnableWorkAdapter(command);

        try {
            synchronized(mux) {
                // If service is being shut down reject all requests.
                if (isBeingShutdown == false) {
                    // We put it here to control shutdown process.
                    works.add(workMgr.schedule(work, termListener));
                }
                else {
                    throw new RejectedExecutionException("Failed to execute command (service is being shut down).");
                }
            }
        }
        catch (WorkException e) {
            // Unfortunately RejectedExecutionException is the closest thing we have
            // as proper RuntimeException.
            throw new RejectedExecutionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        synchronized(mux) {
            if (isBeingShutdown == true) {
                return;
            }

            isBeingShutdown = true;
        }

        // Wait for all tasks to be finished or rejected.
        synchronized(works) {
            if (works.size() != 0) {
                try {
                    if (workMgr.waitForAll(works, WorkManager.INDEFINITE) == false) {
                        throw new IllegalStateException("Failed to shutdown service properly " +
                            "(tasks execution is timed out).");
                    }
                }
                catch (InterruptedException e) {
                    throw new IllegalStateException("Failed to shutdown service properly " +
                        "(waiting was interrupted).", e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        // Since we do not control execution we have to
        // wait until all tasks are executed. It's a conventional
        // implementation.
        shutdown();

        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isShutdown() {
        synchronized(mux) {
            synchronized(works) {
                return isBeingShutdown == true && works.isEmpty() == true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerminated() {
        return isShutdown();
    }

    /**
     * {@inheritDoc}
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // Wait for all tasks to be finished or rejected.
        synchronized(works) {
            if (works.size() != 0) {
                if (workMgr.waitForAll(works, unit.toMillis(timeout)) == false) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Listener to track works.
     */
    private class WorkTerminateListener implements WorkListener {
        /**
         * {@inheritDoc}
         */
        public void workAccepted(WorkEvent workEvent) {
            // No-op.
        }

        /**
         * {@inheritDoc}
         */
        public void workRejected(WorkEvent workEvent) {
            synchronized (works) {
                works.remove(workEvent.getWorkItem());
            }
        }

        /**
         * {@inheritDoc}
         */
        public void workStarted(WorkEvent workEvent) {
            // No-op.
        }

        /**
         * {@inheritDoc}
         */
        public void workCompleted(WorkEvent workEvent) {
            synchronized (works) {
                works.remove(workEvent.getWorkItem());
            }
        }
    }

    /**
     * Compatibility {@link Work} adapter.
     */
    private static final class RunnableWorkAdapter implements Work {
        /** Command which is wrapped by adapter. */
        private final Runnable cmd;

        /**
         * Creates new adapter for the given command.
         *
         * @param cmd Command to execute.
         */
        private RunnableWorkAdapter(Runnable cmd) {
            this.cmd = cmd;
        }

        /**
         * {@inheritDoc}
         */
        public void release() {
            // No-op.
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDaemon() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            cmd.run();
        }
    }
}
