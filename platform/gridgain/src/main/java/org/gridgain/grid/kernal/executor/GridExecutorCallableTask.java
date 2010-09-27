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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.gridgain.grid.*;

/**
 * This class defines own implementation for {@link GridTask}. This class used by
 * {@link GridExecutorService} when commands submitted and can be
 * randomly assigned to available grid nodes. This grid task creates only one
 * {@link GridJob} and transfer it to any available node. See {@link GridTaskSplitAdapter}
 * for more details.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @param <T> Return type of {@link Callable} t.
 */
public class GridExecutorCallableTask<T> extends GridTaskSplitAdapter<Callable<T>, T> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<? extends GridJob> split(int gridSize, Callable<T> arg) throws GridException {
        return Collections.singletonList(new GridJobAdapter<Serializable>((Serializable)arg) {
            /*
            * Simply execute command passed into the job and
            * returns result.
            */
            @SuppressWarnings("unchecked")
            public Serializable execute() throws GridException {
                final Callable<T> call = (Callable<T>)getArgument();

                // Execute command.
                try {
                    T res = call.call();

                    if (res != null && res instanceof Serializable == false) {
                        throw (GridException)new GridException("Result of callable must be Serializable: " + res).setData(60, "src/java/org/gridgain/grid/kernal/executor/GridExecutorCallableTask.java");
                    }

                    return (Serializable)res;
                }
                catch (RuntimeException e) {
                    throw e;
                }
                catch (Error e) {
                    throw e;
                }
                catch (Exception e) {
                    throw (GridException)new GridException("Failed to execute command.", e).setData(72, "src/java/org/gridgain/grid/kernal/executor/GridExecutorCallableTask.java");
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    public T reduce(List<GridJobResult> results) throws GridException {
        assert results != null : "ASSERTION [line=83, file=src/java/org/gridgain/grid/kernal/executor/GridExecutorCallableTask.java]";
        assert results.size() == 1 : "ASSERTION [line=84, file=src/java/org/gridgain/grid/kernal/executor/GridExecutorCallableTask.java]";

        return (T)results.get(0).getData();
    }
}
