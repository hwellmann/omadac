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

package org.gridgain.grid.spi.loadbalancing.roundrobin;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;

/**
 * Load balancer that works in global (not-per-task) mode.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridRoundRobinGlobalLoadBalancer {
    /** */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private final LinkedList<GridNode> nodeQueue = new LinkedList<GridNode>();

    /** */
    private GridSpiContext ctx = null;

    /** */
    private GridDiscoveryListener listener = null;

    /** */
    private final GridLogger log;

    /** */
    private final Object mux = new Object();

    /**
     *
     * @param log Grid logger.
     */
    GridRoundRobinGlobalLoadBalancer(GridLogger log) {
        assert log != null : "ASSERTION [line=58, file=src/java/org/gridgain/grid/spi/loadbalancing/roundrobin/GridRoundRobinGlobalLoadBalancer.java]";

        this.log = log;
    }

    /**
     *
     * @param ctx Load balancing context.
     */
    void onContextInitialized(GridSpiContext ctx) {
        synchronized (mux) {
            this.ctx = ctx;
        }

        ctx.addDiscoveryListener(listener = new GridDiscoveryListener() {
            /**
             * {@inheritDoc}
             */
            public void onDiscovery(GridDiscoveryEventType type, GridNode node) {
                switch (type) {
                    case JOINED: {
                        synchronized (mux) {
                            nodeQueue.addFirst(node);
                        }

                        break;
                    }

                    case LEFT:
                    case FAILED: {
                        synchronized (mux) {
                            nodeQueue.remove(node);
                        }

                        break;
                    }

                    case METRICS_UPDATED: { break; } // No-op.

                    default: { assert false; }
                }
            }
        });

        synchronized (mux) {
            for (GridNode node : ctx.getAllNodes()) {
                if (nodeQueue.contains(node) == false) {
                    nodeQueue.add(node);
                }
            }
        }
    }

    /**
     *
     */
    void onContextDestroyed() {
        ctx.removeDiscoveryListener(listener);
    }

    /**
     * THIS METHOD IS USED ONLY FOR TESTING.
     *
     * @return Internal list of nodes.
     */
    List<GridNode> getNodes() {
        synchronized (mux) {
            return Collections.unmodifiableList(new ArrayList<GridNode>(nodeQueue));
        }
    }

    /**
     * Gets balanced node for given topology.
     *
     * @param top Topology to pick from.
     * @return Best balanced node.
     * @throws GridException Thrown in case of any error.
     */
    GridNode getBalancedNode(List<GridNode> top) throws GridException {
        assert top != null : "ASSERTION [line=137, file=src/java/org/gridgain/grid/spi/loadbalancing/roundrobin/GridRoundRobinGlobalLoadBalancer.java]";
        assert top.isEmpty() == false : "ASSERTION [line=138, file=src/java/org/gridgain/grid/spi/loadbalancing/roundrobin/GridRoundRobinGlobalLoadBalancer.java]";

        Set<GridNode> topSet = new HashSet<GridNode>(top);

        synchronized (mux) {
            GridNode found = null;

            for (Iterator<GridNode> iter = nodeQueue.iterator(); iter.hasNext() == true;) {
                GridNode node = iter.next();

                if (topSet.contains(node) == true) {
                    found = node;

                    iter.remove();

                    break;
                }
            }

            if (found != null) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Found round-robin node: " + found);
                }

                nodeQueue.addLast(found);

                return found;
            }
        }

        throw (GridException)new GridException("Task topology does not have alive nodes: " + top).setData(168, "src/java/org/gridgain/grid/spi/loadbalancing/roundrobin/GridRoundRobinGlobalLoadBalancer.java");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridRoundRobinGlobalLoadBalancer.class, this);
    }
}
