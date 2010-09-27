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

package org.gridgain.grid.kernal.managers.loadbalancing;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.loadbalancing.*;
import org.gridgain.grid.util.*;

/**
 * Load balancing manager.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridLoadBalancingManager extends GridManagerAdapter<GridLoadBalancingSpi> {
    /**
     * @param cfg Grid configuration.
     * @param mgrReg Manager registry.
     * @param procReg Processor registry.
     */
    public GridLoadBalancingManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridLoadBalancingSpi.class, cfg, procReg, mgrReg, cfg.getLoadBalancingSpi());
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws GridException {
        startSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws GridException {
        stopSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     * @param ses Task session.
     * @param top Task topology.
     * @param job Job to balance.
     * @return Next balanced node.
     * @throws GridException If anything failed.
     */
    public GridNode getBalancedNode(GridTaskSessionImpl ses, List<GridNode> top, GridJob job)
        throws GridException {
        assert ses != null : "ASSERTION [line=79, file=src/java/org/gridgain/grid/kernal/managers/loadbalancing/GridLoadBalancingManager.java]";
        assert top != null : "ASSERTION [line=80, file=src/java/org/gridgain/grid/kernal/managers/loadbalancing/GridLoadBalancingManager.java]";
        assert job != null : "ASSERTION [line=81, file=src/java/org/gridgain/grid/kernal/managers/loadbalancing/GridLoadBalancingManager.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Getting balanced node [job=" + job + ", ses=" + ses + ", top=" + top + ']');
        }

        return getSpi(ses.getLoadBalancingSpi()).getBalancedNode(ses, top, job);
    }

    /**
     * @param ses Grid task session.
     * @param top Task topology.
     * @return Load balancer.
     */
    public GridLoadBalancer getLoadBalancer(final GridTaskSessionImpl ses, final List<GridNode> top) {
        assert ses != null : "ASSERTION [line=96, file=src/java/org/gridgain/grid/kernal/managers/loadbalancing/GridLoadBalancingManager.java]";

        if (log.isDebugEnabled() == true) {
            log.debug("Getting load balancer for task session: " + ses);
        }

        return new GridLoadBalancer() {
            /**
             * {@inheritDoc}
             */
            public GridNode getBalancedNode(GridJob job, GridNode... excludeNodes) throws GridException {
                GridArgumentCheck.checkNull(job, "job");
                GridArgumentCheck.checkNull(excludeNodes, "excludeNodes");

                if (excludeNodes.length == 0) {
                    return getSpi(ses.getLoadBalancingSpi()).getBalancedNode(ses, top, job);
                }

                List<GridNode> nodes = GridUtils.copy(top, excludeNodes);

                if (nodes.isEmpty() == true) {
                    return null;
                }

                // Exclude list of nodes from topology.
                return getSpi(ses.getLoadBalancingSpi()).getBalancedNode(ses, nodes, job);
            }
        };
    }
}
