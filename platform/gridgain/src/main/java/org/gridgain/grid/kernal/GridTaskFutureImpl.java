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

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import static org.gridgain.grid.kernal.GridTopic.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.managers.communication.*;

/**
 * This class provide implementation for task future.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <R> Type of the task result returning from {@link GridTask#reduce(java.util.List)} method.
 */
public class GridTaskFutureImpl<R> implements GridTaskFuture<R> {
    /** */
    private final GridTaskSession taskSes;

    /** */
    private R data = null;

    /** */
    private GridException ex = null;

    /** */
    private boolean isFinished = false;

    /** */
    private boolean isCancelled = false;

    /** */
    private final GridManagerRegistry reg;

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param taskSes Task session instance.
     * @param reg Managers registry.
     */
    public GridTaskFutureImpl(GridTaskSession taskSes, GridManagerRegistry reg) {
        assert taskSes != null : "ASSERTION [line=65, file=src/java/org/gridgain/grid/kernal/GridTaskFutureImpl.java]";
        assert reg != null : "ASSERTION [line=66, file=src/java/org/gridgain/grid/kernal/GridTaskFutureImpl.java]";

        this.taskSes = taskSes;
        this.reg = reg;
    }

    /**
     * Gets task timeout.
     *
     * @return Task timeout.
     */
    public GridTaskSession getTaskSession() {
        return taskSes;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public R get() throws GridException {
        return get(0);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public R get(long timeout) throws GridException {
        long now = System.currentTimeMillis();

        long end = timeout == 0 ? Long.MAX_VALUE : timeout + now;

        // Prevent overflow.
        if (end < 0) {
            end = Long.MAX_VALUE;
        }

        boolean isTimeout = true;

        synchronized (mux) {
            while (now < end) {
                if (isFinished == true) {
                    isTimeout = false;

                    break;
                }

                try {
                    mux.wait(end - now);
                }
                catch (InterruptedException e) {
                    throw (GridException)new GridException("Got interrupted while waiting for task completion.", e).setData(117, "src/java/org/gridgain/grid/kernal/GridTaskFutureImpl.java");
                }

                now = System.currentTimeMillis();
            }

            if (isTimeout == true) {
                throw (GridTaskTimeoutException)new GridTaskTimeoutException("Timeout occurred while waiting for task completion.").setData(124, "src/java/org/gridgain/grid/kernal/GridTaskFutureImpl.java");
            }

            if (ex != null) {
                 throw ex;
            }

            return data;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() throws GridException {
        if (isDone() == false) {
            reg.getCommunicationManager().sendMessage(reg.getDiscoveryManager().getAllNodes(),
                CANCEL.topic(), new GridJobCancelRequest(taskSes.getId()),
                GridCommunicationThreadPolicy.POOLED_THREAD);

            synchronized (mux) {
                isCancelled = true;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDone() {
        synchronized (mux) {
            return isFinished;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCancelled() {
        synchronized (mux) {
            return isCancelled;
        }
    }

    /**
     *
     * @param data Future data.
     */
    public void setData(R data) {
        synchronized (mux) {
            this.data = data;

            isFinished = true;

            mux.notifyAll();
        }
    }

    /**
     *
     * @param ex Future exception.
     */
    public void setException(GridException ex) {
        synchronized (mux) {
            this.ex = ex;

            isFinished = true;

            mux.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridTaskFutureImpl.class, this);
    }
}
