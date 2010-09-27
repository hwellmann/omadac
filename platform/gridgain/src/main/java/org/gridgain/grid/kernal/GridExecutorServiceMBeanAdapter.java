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

import java.util.concurrent.*;
import org.gridgain.grid.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridExecutorServiceMBeanAdapter implements GridExecutorServiceMBean {
    /** */
    private final ExecutorService exec;

    /**
     * Creates adapter.
     *
     * @param exec Executor service
     */
    public GridExecutorServiceMBeanAdapter(ExecutorService exec) {
        assert exec != null : "ASSERTION [line=43, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        this.exec = exec;
    }

    /**
     * {@inheritDoc}
     */
    public int getActiveCount() {
        assert exec != null : "ASSERTION [line=52, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getActiveCount() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getCompletedTaskCount() {
        assert exec != null : "ASSERTION [line=61, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getCompletedTaskCount() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public int getCorePoolSize() {
        assert exec != null : "ASSERTION [line=70, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getCorePoolSize() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public int getLargestPoolSize() {
        assert exec != null : "ASSERTION [line=79, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getLargestPoolSize() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumPoolSize() {
        assert exec != null : "ASSERTION [line=88, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getMaximumPoolSize() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public int getPoolSize() {
        assert exec != null : "ASSERTION [line=97, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getPoolSize() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getTaskCount() {
        assert exec != null : "ASSERTION [line=106, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getTaskCount() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public int getQueueSize() {
        assert exec != null : "ASSERTION [line=115, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).getQueue().size() : -1;
    }

    /**
     * {@inheritDoc}
     */
    public long getKeepAliveTime() {
        assert exec != null : "ASSERTION [line=124, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ?
            ((ThreadPoolExecutor)exec).getKeepAliveTime(TimeUnit.MILLISECONDS) : -1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isShutdown() {
        assert exec != null : "ASSERTION [line=134, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec.isShutdown();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerminated() {
        return exec.isTerminated();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerminating() {
        assert exec != null : "ASSERTION [line=150, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        return exec instanceof ThreadPoolExecutor ? ((ThreadPoolExecutor)exec).isTerminating() : false;
    }

    /**
     * {@inheritDoc}
     */
    public String getRejectedExecutionHandlerClass() {
        assert exec != null : "ASSERTION [line=159, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        if (exec instanceof ThreadPoolExecutor == false) {
            return "";
        }

        RejectedExecutionHandler handler = ((ThreadPoolExecutor)exec).getRejectedExecutionHandler();

        return handler == null ? "" : handler.getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getThreadFactoryClass() {
        assert exec != null : "ASSERTION [line=174, file=src/java/org/gridgain/grid/kernal/GridExecutorServiceMBeanAdapter.java]";

        if (exec instanceof ThreadPoolExecutor == false) {
            return "";
        }

        ThreadFactory factory = ((ThreadPoolExecutor)exec).getThreadFactory();

        return factory == null ? "" : factory.getClass().getName();
    }
}
