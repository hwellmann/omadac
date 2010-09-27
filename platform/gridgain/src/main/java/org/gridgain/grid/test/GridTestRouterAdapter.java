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

package org.gridgain.grid.test;

import java.util.*;
import org.gridgain.grid.*;
import static org.gridgain.grid.test.GridTestVmParameters.*;

/**
 * Round-Robin implementation of {@link GridTestRouter} interface.
 * The implementation makes sure that nodes are assigned to tests
 * sequentially, one after another.
 * <p>
 * Note that if {@link GridTestVmParameters#GRID_ROUTER_PREFER_REMOTE} VM parameter
 * is set to <tt>true</tt>, then tests will be routed to remote nodes assuming there
 * are any. If there are no remote nodes, tests will be routed to local node.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridTestRouterAdapter implements GridTestRouter {
    /** */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    private LinkedList<GridNode> nodes = new LinkedList<GridNode>();

    /**
     * Check if local node should not execute distributed tests by checking
     * {@link GridTestVmParameters#GRID_ROUTER_PREFER_REMOTE} system
     * property.
     */
    private boolean isPreferRemote = Boolean.getBoolean(GRID_ROUTER_PREFER_REMOTE.name());

    /**
     * {@inheritDoc}
     */
    public synchronized GridNode route(Class<?> test, String name, List<GridNode> subgrid, UUID locNodeId) {
        assert subgrid != null : "ASSERTION [line=56, file=src/java/org/gridgain/grid/test/GridTestRouterAdapter.java]";
        assert subgrid.size() > 0 : "ASSERTION [line=57, file=src/java/org/gridgain/grid/test/GridTestRouterAdapter.java]";

        // Remove old nodes.
        nodes.retainAll(subgrid);

        // Add new nodes from subgrid at the begin of collection.
        // Process subgrid in reverse order. New nodes will be ordered like in subgrid.
        for (ListIterator<GridNode> iter = subgrid.listIterator(subgrid.size()); iter.hasPrevious() == true;) {
            GridNode node = iter.previous();

            if (nodes.contains(node) == false) {
                // Collection must be ordered. Add new nodes at the begin of the collection.
                nodes.addFirst(node);
            }
        }

        if (nodes.size() == 1) {
            return nodes.getFirst();
        }

        // Remove local node.
        if (isPreferRemote == true && nodes.getFirst().getId().equals(locNodeId) == true) {
            nodes.removeFirst();
        }

        // Rotate the first node to the end of the list.
        nodes.addLast(nodes.removeFirst());

        // Return the node that was just rotated.
        return nodes.getLast();
    }
}
