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

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.gridify.*;
import org.gridgain.grid.resources.*;

/**
 * Default gridify task which simply executes a method on remote node.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * <tt>gridified</tt> methods.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see Gridify
 */
public class GridifyDefaultTask extends GridSystemTask<GridifyArgument, Object> {
    /** Grid instance. */
    @GridInstanceResource
    private Grid grid = null;

    /** Load balancer. */
    @GridLoadBalancerResource
    private GridLoadBalancer balancer = null;

    /**
     * @param execCls Execution class.
     */
    public GridifyDefaultTask(Class<?> execCls) {
        super(execCls);
    }

    /**
     * Default <tt>map</tt> implementation for <tt>Gridify</tt> job that simply
     * creates one job for execution.
     *
     * @param subgrid {@inheritDoc}
     * @param arg {@inheritDoc}
     * @return {@inheritDoc}
     * @throws GridException {@inheritDoc}
     */
    public Map<? extends GridJob, GridNode> map(List<GridNode> subgrid, GridifyArgument arg) throws GridException {
        assert subgrid.isEmpty() == false : "ASSERTION [line=65, file=src/java/org/gridgain/grid/kernal/GridifyDefaultTask.java]. " + "Subgrid should not be empty: " + subgrid;

        assert grid != null : "ASSERTION [line=67, file=src/java/org/gridgain/grid/kernal/GridifyDefaultTask.java]. " + "Grid instance could not be injected";
        assert balancer != null : "ASSERTION [line=68, file=src/java/org/gridgain/grid/kernal/GridifyDefaultTask.java]. " + "Load balancer could not be injected";

        GridJob job = new GridifyJobAdapter(arg);

        GridNode node = balancer.getBalancedNode(job, grid.getLocalNode());

        if (node != null) {
            // Give preference to remote nodes.
            return Collections.singletonMap(job, node);
        }

        return Collections.singletonMap(job, balancer.getBalancedNode(job));
    }

    /**
     * {@inheritDoc}
     */
    public final Object reduce(List<GridJobResult> results) throws GridException {
        assert results.size() == 1 : "ASSERTION [line=86, file=src/java/org/gridgain/grid/kernal/GridifyDefaultTask.java]";

        GridJobResult res = results.get(0);

        if (res.getException() != null) {
            throw res.getException();
        }

        return res.getData();
    }
}
