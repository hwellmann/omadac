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

package org.gridgain.grid.kernal.managers.failover;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.kernal.managers.loadbalancing.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.failover.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridFailoverContextImpl implements GridFailoverContext {
    /** Grid task session. */
    private final GridTaskSessionImpl taskSes;

    /** Failed job result. */
    private final GridJobResult jobRes;

    /** Load balancing manager. */
    @GridToStringExclude
    private final GridLoadBalancingManager loadMgr;

    /**
     * Initializes failover context.
     *
     * @param taskSes Grid task session.
     * @param jobRes Failed job result.
     * @param loadMgr Load manager.
     */
    public GridFailoverContextImpl(GridTaskSessionImpl taskSes, GridJobResult jobRes,
        GridLoadBalancingManager loadMgr) {
        assert taskSes != null : "ASSERTION [line=57, file=src/java/org/gridgain/grid/kernal/managers/failover/GridFailoverContextImpl.java]";
        assert jobRes != null : "ASSERTION [line=58, file=src/java/org/gridgain/grid/kernal/managers/failover/GridFailoverContextImpl.java]";
        assert loadMgr != null : "ASSERTION [line=59, file=src/java/org/gridgain/grid/kernal/managers/failover/GridFailoverContextImpl.java]";

        this.taskSes = taskSes;
        this.jobRes = jobRes;
        this.loadMgr = loadMgr;
    }

    /**
     * {@inheritDoc}
     */
    public GridTaskSession getTaskSession() {
        return taskSes;
    }

    /**
     * {@inheritDoc}
     */
    public GridJobResult getJobResult() {
        return jobRes;
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getBalancedNode(List<GridNode> top) throws GridException {
        return loadMgr.getBalancedNode(taskSes, top, jobRes.getJob());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridFailoverContextImpl.class, this);
    }
}
