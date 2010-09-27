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

package org.gridgain.grid.kernal.executor;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.logger.*;

/**
 * An {@link ExecutorService} that executes each submitted task in grid
 * through {@link Grid} instance, normally configured using
 * {@link Grid#newGridExecutorService()} method.
 * <tt>GridExecutorService</tt> delegates commands execution to already
 * started {@link Grid} instance. Every submitted task will be serialized and
 * transfered to any node in grid.
 * <p>
 * All submitted tasks must implement {@link Serializable} interface.
 * <p>
 * Note, that GridExecutorService implements ExecutorService from JDK 1.5.
 * If you have problems with compilation for JDK 1.6 and above you need to apply
 * some changes (see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6267833">http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6267833</a>)
 * <p>
 * Change signature for methods {@link GridExecutorService#invokeAll(Collection)},
 * {@link GridExecutorService#invokeAll(Collection, long, TimeUnit)},
 * {@link GridExecutorService#invokeAny(Collection)},
 * {@link GridExecutorService#invokeAny(Collection, long, TimeUnit)} to
 * <pre name="code" class="java">
 * public class GridExecutorService implements ExecutorService {
 * ...
 *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException {
 *         ...
 *     }
 *
 *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
 *         throws InterruptedException {
 *         ...
 *     }
 *
 *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException, ExecutionException {
 *         ...
 *     }
 *
 *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
 *         throws InterruptedException, ExecutionException, TimeoutException {
 *     }
 *     ...
 * }
 * </pre>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridExecutorService implements ExecutorService {
    /** Grid instance. */
    private final Grid grid;

    /** Logger. */
    private final GridLogger log;

    /** Whether service is being stopped or not. */
    private boolean isBeingShutdown = false;

    /** List of executing or scheduled for execution tasks. */
    private final List<GridTaskFuture<?>> futures = new ArrayList<GridTaskFuture<?>>();

    /** Rejected or completed tasks listener. */
    private final TaskTerminateListener termListener = new TaskTerminateListener();

    /** */
    private final Object mux = new Object();

    /**
     * Creates executor service.
     *
     * @param grid Grid instance.
     * @param log Grid logger.
     */
    public GridExecutorService(Grid grid, GridLogger log) {
        assert grid != null : "ASSERTION [line=101, file=src/java/org/gridgain/grid/kernal/executor/GridExecutorService.java]";
        assert log != null : "ASSERTION [line=102, file=src/java/org/gridgain/grid/kernal/executor/GridExecutorService.java]";

        this.grid = grid;
        this.log = log.getLogger(GridExecutorService.class);
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
    }

    /**
     * {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<GridTaskFuture<?>> copyFutures = null;

        // Cancel all tasks.
        synchronized (mux) {
            copyFutures = new ArrayList<GridTaskFuture<?>>(futures);

            isBeingShutdown = true;
        }

        for (GridTaskFuture<?> task : copyFutures) {
            try {
                task.cancel();
            }
            catch (GridException e) {
                log.error("Failed to cancel task: " + task, e);
            }
        }

        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isShutdown() {
        synchronized(mux) {
            return isBeingShutdown == true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerminated() {
        synchronized(mux) {
            return isBeingShutdown == true && futures.isEmpty() == true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long now = System.currentTimeMillis();

        timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);

        long end = timeout == 0 ? Long.MAX_VALUE : timeout + now;

        // Prevent overflow.
        if (end < 0) {
            end = Long.MAX_VALUE;
        }

        List<GridTaskFuture<?>> localTasks = null;

        // Cancel all tasks.
        synchronized (mux) {
            localTasks = new ArrayList<GridTaskFuture<?>>(futures);
        }

        Iterator<GridTaskFuture<?>> iter = localTasks.iterator();

        while(iter.hasNext() == true && now < end) {
            GridTaskFuture<?> future = iter.next();

            try {
                future.get(end - now);
            }
            catch (GridTaskTimeoutException e) {
                log.error("Failed to get task result: " + future, e);

                return false;
            }
            catch (GridException e) {
                log.error("Failed to get task result: " + future, e);

                if (e.getCause() instanceof InterruptedException) {
                    //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
                    throw new InterruptedException("Got interrupted while waiting for task completion.");
                }
            }

            now = System.currentTimeMillis();
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public <T> Future<T> submit(Callable<T> task) {
        GridArgumentCheck.checkNull(task, "task != null");

        checkSerializable(task);

        checkShutdown();

        deployTask(GridExecutorCallableTask.class, task);

        return addFuture(grid.execute(new GridExecutorCallableTask<T>(), task, termListener));
    }

    /**
     * {@inheritDoc}
     */
    public <T> Future<T> submit(Runnable task, T result) {
        GridArgumentCheck.checkNull(task, "task != null");

        checkSerializable(task);

        checkShutdown();

        deployTask(GridExecutorCallableTask.class, task);

        return addFuture(grid.execute(new GridExecutorCallableTask<T>(),
            new GridExecutorRunnableAdapter<T>(task, result), termListener));
    }

    /**
     * {@inheritDoc}
     */
    public Future<?> submit(Runnable task) {
        GridArgumentCheck.checkNull(task, "task != null");

        checkSerializable(task);

        checkShutdown();

        deployTask(GridExecutorRunnableTask.class, task);

        return addFuture(grid.execute(GridExecutorRunnableTask.class, task, termListener));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the <tt>&lt;? extends T&gt;</tt> clause).
     * <pre name="code" class="java">
     *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException {
     *         ...
     *     }
     * </pre>
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return invokeAll(tasks, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the <tt>&lt;? extends T&gt;</tt> clause).
     * <pre name="code" class="java">
     *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
     *         throws InterruptedException {
     *         ...
     *     }
     * </pre>
     */
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
        GridArgumentCheck.checkNull(tasks, "tasks != null");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");
        GridArgumentCheck.checkNull(unit, "unit != null");

        long now = System.currentTimeMillis();

        timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);

        long end = timeout == 0 ? Long.MAX_VALUE : timeout + now;

        // Prevent overflow.
        if (end < 0) {
            end = Long.MAX_VALUE;
        }

        // Check that all commands serializable.
        checkSerializable(tasks);

        checkShutdown();

        List<GridTaskFuture<T>> taskFutures = new ArrayList<GridTaskFuture<T>>();

        for (Callable<T> task : tasks) {
            deployTask(GridExecutorCallableTask.class, task);

            // Execute task without predefined timeout.
            // GridFuture.cancel() will be called if timeout elapsed.
            GridTaskFuture<T> future = grid.execute(new GridExecutorCallableTask<T>(), task);

            taskFutures.add(future);

            now = System.currentTimeMillis();
        }

        boolean isInterrupted = false;

        for (GridTaskFuture<T> future : taskFutures) {
            boolean cancel = false;

            if (isInterrupted == false && now < end) {
                //noinspection UnusedCatchParameter
                try {
                    future.get(end - now);
                }
                catch (GridTaskTimeoutException e) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Timeout occured during getting task result: " + future);
                    }

                    cancel = true;
                }
                catch (GridException e) {
                    // This invokeAll() method was interrupted (therefore, need to cancel all tasks).
                    // Note: that execution may be interrupted on remote node. Possible bug.
                    if (e.getCause() instanceof InterruptedException) {
                        isInterrupted = true;
                    }
                }
            }

            // Cancel active task if any task interrupted or timeout elapsed.
            if ((isInterrupted == true || cancel == true) && future.isDone() == false) {
                try {
                    future.cancel();
                }
                catch (GridException e) {
                    log.error("Failed to cancel task: " + future, e);
                }
            }

            now = System.currentTimeMillis();
        }

        // Throw exception if any task wait was interrupted.
        if (isInterrupted == true) {
            throw new InterruptedException("Got interrupted while waiting for tasks invocation.");
        }

        List<Future<T>> futures = new ArrayList<Future<T>>(taskFutures.size());

        // Convert futures.
        for (GridTaskFuture<T> future : taskFutures) {
            futures.add(new TaskFutureWrapper<T>(future));
        }

        return futures;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the <tt>&lt;? extends T&gt;</tt> clause).
     * <pre name="code" class="java">
     *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException, ExecutionException {
     *         ...
     *     }
     * </pre>
     */
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        try {
            return invokeAny(tasks, 0, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            throw new ExecutionException("Timeout occured during commands execution.", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the <tt>&lt;? extends T&gt;</tt> clause).
     * <pre name="code" class="java">
     *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
     *         throws InterruptedException, ExecutionException, TimeoutException {
     *     }
     * </pre>
     */
    @SuppressWarnings({"MethodWithTooExceptionsDeclared"})
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        GridArgumentCheck.checkNull(tasks, "tasks != null");
        GridArgumentCheck.checkRange(tasks.size() > 0, "tasks.size() > 0");
        GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");
        GridArgumentCheck.checkNull(unit, "unit != null");

        long now = System.currentTimeMillis();

        timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);

        long end = timeout == 0 ? Long.MAX_VALUE : timeout + now;

        // Prevent overflow.
        if (end < 0) {
            end = Long.MAX_VALUE;
        }

        // Check that all commands serializable.
        checkSerializable(tasks);

        checkShutdown();

        List<GridTaskFuture<T>> taskFutures = new ArrayList<GridTaskFuture<T>>();

        for (Callable<T> cmd : tasks) {
            // Execute task with predefined timeout.
            GridTaskFuture<T> future = grid.execute(new GridExecutorCallableTask<T>(), cmd);

            taskFutures.add(future);

            now = System.currentTimeMillis();
        }

        T result = null;

        boolean isTimeout = false;
        boolean isInterrupted = false;
        boolean isResultReceived = false;

        for (GridTaskFuture<T> future : taskFutures) {
            boolean cancel = false;

            if (isInterrupted == false && isResultReceived == false && now < end) {
                //noinspection UnusedCatchParameter
                try {
                    result = future.get(end - now);

                    isResultReceived = true;

                    now = System.currentTimeMillis();

                    // Cancel next tasks (avoid current task cancellation below in loop).
                    continue;
                }
                catch (GridTaskTimeoutException e) {
                    if (log.isDebugEnabled() == true) {
                        log.debug("Timeout occured during getting task result: " + future);
                    }

                    isTimeout = true;
                    cancel = true;
                }
                catch (GridException e) {
                    // This invokeAll() method was interrupted (therefore, need to cancel all tasks).
                    // Note: that execution may be interrupted on remote node. Possible bug.
                    if (e.getCause() instanceof InterruptedException) {
                        isInterrupted = true;
                    }
                }
            }

            // Cancel active task if any task interrupted, timeout elapsed or received task result before.
            if ((isInterrupted == true || isResultReceived == true || cancel == true) && future.isDone() == false) {
                try {
                    future.cancel();
                }
                catch (GridException e) {
                    log.error("Failed to cancel task: " + future, e);
                }
            }

            now = System.currentTimeMillis();
        }

        // No result received from task but timeout elapsed.
        if (isTimeout == true && isResultReceived == false) {
            throw new TimeoutException("Timeout occurred during tasks invocation.");
        }

        // Throw exception if any task wait was interrupted.
        if (isInterrupted == true) {
            throw new InterruptedException("Got interrupted while waiting for tasks invocation.");
        }

        // No result received.
        if (isResultReceived == false) {
            throw new ExecutionException("Failed to get any task completion.", null);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Runnable command) {
        GridArgumentCheck.checkNull(command, "command != null");

        checkSerializable(command);

        checkShutdown();

        deployTask(GridExecutorRunnableTask.class, command);

        addFuture(grid.execute(GridExecutorRunnableTask.class, command, termListener));
    }

    /**
     * Checks if service is being shutdown.
     */
    private void checkShutdown() {
        synchronized(mux) {
            if (isBeingShutdown == true) {
                throw new RejectedExecutionException("Failed to execute command during executor shutdown.");
            }
        }
    }

    /**
     *
     * @param <T> Type of command result.
     * @param future Future to add.
     * @return Future for command.
     */
    private <T> Future<T> addFuture(GridTaskFuture<T> future) {
        synchronized (mux) {
            if (future.isDone() == false) {
                futures.add(future);
            }

            return new TaskFutureWrapper<T>(future);
        }
    }

    /**
     *
     * @param taskCls Task class.
     * @param cmd Command to deploy.
     */
    @SuppressWarnings("unchecked")
    private void deployTask(Class<? extends GridTask> taskCls, Object cmd) {
        try {
            grid.deployTask(taskCls, cmd.getClass().getClassLoader());
        }
        catch (GridException e) {
            throw new RejectedExecutionException("Failed to deploy command: " + cmd, e);
        }
    }

    /**
     * Check that every task implements {@link Serializable} interface.
     *
     * @param cmds The collection of tasks.
     * @throws RejectedExecutionException FIXDOC
     */
    private void checkSerializable(Collection<?> cmds) throws RejectedExecutionException {
        assert cmds != null : "ASSERTION [line=576, file=src/java/org/gridgain/grid/kernal/executor/GridExecutorService.java]";

        for (Object cmd : cmds) {
            checkSerializable(cmd);
        }
    }

    /**
     * Check that defined task implements {@link Serializable} interface.
     *
     * @param cmd The runnable task.
     * @throws RejectedExecutionException FIXDOC
     */
    private void checkSerializable(Object cmd) throws RejectedExecutionException {
        if (cmd instanceof Serializable == false) {
            throw new RejectedExecutionException("Failed to execute command (instance must be serializable): " + cmd);
        }
    }

    /**
     * Listener to track tasks.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     */
    private class TaskTerminateListener implements GridTaskListener {
        /**
         * {@inheritDoc}
         */
        public void onFinished(GridTaskFuture<?> taskFuture) {
            synchronized(mux) {
                futures.remove(taskFuture);
            }
        }
    }

    /**
     * Wrapper for {@link GridTaskFuture}.
     * Used for compatibility {@link Future} interface.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @param <T> The result type of the {@link Future} argument.
     */
    private class TaskFutureWrapper<T> implements Future<T> {
        /** */
        private final GridTaskFuture<T> future;

        /**
         * Creates wrapper.
         *
         * @param future Grid future.
         */
        TaskFutureWrapper(GridTaskFuture<T> future) {
            assert future != null : "ASSERTION [line=628, file=src/java/org/gridgain/grid/kernal/executor/GridExecutorService.java]";

            this.future = future;
        }

        /**
         * {@inheritDoc}
         */
        public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                future.cancel();
            }
            catch (GridException e) {
                log.error("Failed to cancel task: " + future, e);
            }

            return true;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCancelled() {
            return future.isCancelled();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isDone() {
            return future.isDone();
        }

        /**
         * {@inheritDoc}
         */
        public T get() throws ExecutionException {
            try {
                T res = future.get();

                if (future.isCancelled() == true) {
                    throw new CancellationException("Task was cancelled: " + future);
                }

                return res;
            }
            catch (GridException e) {
                // Task cancellation may cause throwing exception.
                if (future.isCancelled() == true) {
                    CancellationException ex = new CancellationException("Task was cancelled: " + future);

                    ex.initCause(e);

                    throw ex;
                }

                throw new ExecutionException("Failed to get task result: " + future, e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"MethodWithTooExceptionsDeclared"})
        public T get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
            GridArgumentCheck.checkRange(timeout >= 0, "timeout >= 0");
            GridArgumentCheck.checkNull(unit, "unit != null");

            try {
                T res = future.get(unit.toMillis(timeout));

                if (future.isCancelled() == true) {
                    throw new CancellationException("Task was cancelled: " + future);
                }

                return res;
            }
            catch (GridTaskTimeoutException e) {
                TimeoutException ex = new TimeoutException("Timeout occured during getting task result: " + future);

                ex.initCause(e);

                throw ex;
            }
            catch (GridException e) {
                // Task cancellation may cause throwing exeption.
                if (future.isCancelled() == true) {
                    CancellationException ex = new CancellationException("Task was cancelled: " + future);

                    ex.initCause(e);

                    throw ex;
                }

                throw new ExecutionException("Failed to get task result.", e);
            }
        }
    }
}
