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

package org.gridgain.grid.kernal.managers;

import java.util.*;
import org.gridgain.grid.kernal.managers.checkpoint.*;
import org.gridgain.grid.kernal.managers.collision.*;
import org.gridgain.grid.kernal.managers.communication.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.managers.discovery.*;
import org.gridgain.grid.kernal.managers.eventstorage.*;
import org.gridgain.grid.kernal.managers.failover.*;
import org.gridgain.grid.kernal.managers.loadbalancing.*;
import org.gridgain.grid.kernal.managers.metrics.*;
import org.gridgain.grid.kernal.managers.topology.*;
import org.gridgain.grid.kernal.managers.tracing.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This class provides centralized registry for kernal managers.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridToStringExclude
public class GridManagerRegistry {
    /** */
    @GridToStringExclude
    private GridDeploymentManager depMgr = null;

    /** */
    @GridToStringExclude
    private GridCommunicationManager commMgr = null;

    /** */
    @GridToStringExclude
    private GridDiscoveryManager discoMgr = null;

    /** */
    @GridToStringExclude
    private GridCheckpointManager cpMgr = null;

    /** */
    @GridToStringExclude
    private GridEventStorageManager evtMgr = null;

    /** */
    @GridToStringExclude
    private GridFailoverManager failoverMgr = null;

    /** */
    @GridToStringExclude
    private GridTopologyManager topMgr = null;

    /** */
    @GridToStringExclude
    private GridCollisionManager colMgr = null;

    /** */
    @GridToStringExclude
    private GridLoadBalancingManager loadMgr = null;

    /** */
    @GridToStringExclude
    private GridLocalMetricsManager metricsMgr = null;

    /** */
    @GridToStringExclude
    private GridTracingManager traceMgr = null;

    /** */
    @GridToStringExclude
    private List<GridManager> mgrs = new ArrayList<GridManager>();

    /**
     *
     * @param mgr Manager to add.
     */
    public void add(GridManager mgr) {
        assert mgr != null : "ASSERTION [line=99, file=src/java/org/gridgain/grid/kernal/managers/GridManagerRegistry.java]";

        if (mgr instanceof GridDeploymentManager) {
            depMgr = (GridDeploymentManager)mgr;
        }
        else if (mgr instanceof GridCommunicationManager) {
            commMgr = (GridCommunicationManager)mgr;
        }
        else if (mgr instanceof GridDiscoveryManager) {
            discoMgr = (GridDiscoveryManager)mgr;
        }
        else if (mgr instanceof GridCheckpointManager) {
            cpMgr = (GridCheckpointManager)mgr;
        }
        else if (mgr instanceof GridEventStorageManager) {
            evtMgr = (GridEventStorageManager)mgr;
        }
        else if (mgr instanceof GridFailoverManager) {
            failoverMgr = (GridFailoverManager)mgr;
        }
        else if (mgr instanceof GridTopologyManager) {
            topMgr = (GridTopologyManager)mgr;
        }
        else if (mgr instanceof GridCollisionManager) {
            colMgr = (GridCollisionManager)mgr;
        }
        else if (mgr instanceof GridLocalMetricsManager) {
            metricsMgr = (GridLocalMetricsManager)mgr;
        }
        else if (mgr instanceof GridLoadBalancingManager) {
            loadMgr = (GridLoadBalancingManager)mgr;
        }
        else if (mgr instanceof GridTracingManager) {
            traceMgr = (GridTracingManager)mgr;
        }
        else {
            assert false : "ASSERTION [line=135, file=src/java/org/gridgain/grid/kernal/managers/GridManagerRegistry.java]. " + "Unknown manager class: " + mgr.getClass();
        }

        mgrs.add(mgr);
    }

    /**
     *
     * @return Deployment manager.
     */
    public GridDeploymentManager getDeploymentManager() {
        return depMgr;
    }

    /**
     *
     * @return Communication manager.
     */
    public GridCommunicationManager getCommunicationManager() {
        return commMgr;
    }

    /**
     *
     * @return Discovery manager.
     */
    public GridDiscoveryManager getDiscoveryManager() {
        return discoMgr;
    }

    /**
     *
     * @return Checkpoint manager.
     */
    public GridCheckpointManager getCheckpointManager() {
        return cpMgr;
    }

    /**
     *
     * @return Event storage manager.
     */
    public GridEventStorageManager getEventStorageManager() {
        return evtMgr;
    }

    /**
     *
     * @return Tracing manager, possibly <tt>null</tt> if tracing is disabled.
     */
    public GridTracingManager getTracingManager() {
        return traceMgr;
    }

    /**
     *
     * @return Failover manager.
     */
    public GridFailoverManager getFailoverManager() {
        return failoverMgr;
    }

    /**
     *
     * @return Topology manager.
     */
    public GridTopologyManager getTopologyManager() {
        return topMgr;
    }

    /**
     *
     * @return Collision manager.
     */
    public GridCollisionManager getCollisionManager() {
        return colMgr;
    }

    /**
     *
     * @return Metrics manager.
     */
    public GridLocalMetricsManager getRuntimeMetricsManager() {
        return metricsMgr;
    }

    /**
     *
     * @return Load balancing manager.
     */
    public GridLoadBalancingManager getLoadBalancingManager() {
        return loadMgr;
    }

    /**
     * @return List of started managers.
     */
    public List<GridManager> getManagers() {
        return mgrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridManagerRegistry.class, this);
    }
}
