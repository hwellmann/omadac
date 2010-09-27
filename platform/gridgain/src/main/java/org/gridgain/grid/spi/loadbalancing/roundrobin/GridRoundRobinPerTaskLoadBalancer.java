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
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;

/**
 * Load balancer for per-task configuration.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridRoundRobinPerTaskLoadBalancer {
    /** */
    private Queue<GridNode> nodeQueue = null;
    
    /** Jobs mapped flag. */
    private AtomicBoolean isMapped = new AtomicBoolean(false);
    
    /** Mutex. */
    private final Object mux = new Object();
    
    /**
     * Call back for job mapped event.
     */
    void onMapped() {
        isMapped.set(true);
    }

    /**
     * Gets balanced node for given topology. This implementation
     * is to be used only from {@link GridTask#map(List, Object)} method
     * and, therefore, does not need to be thread-safe.
     *
     * @param top Topology to pick from.
     * @return Best balanced node.
     */
    GridNode getBalancedNode(List<GridNode> top) {
        assert top != null : "ASSERTION [line=60, file=src/java/org/gridgain/grid/spi/loadbalancing/roundrobin/GridRoundRobinPerTaskLoadBalancer.java]";
        assert top.isEmpty() == false : "ASSERTION [line=61, file=src/java/org/gridgain/grid/spi/loadbalancing/roundrobin/GridRoundRobinPerTaskLoadBalancer.java]";

        boolean readjust = isMapped.get();
        
        synchronized (mux) {
            // Populate first time.
            if (nodeQueue == null) {
                nodeQueue = new LinkedList<GridNode>(top);
            }
            
            // If job has been mapped, then it means 
            // that it is most likely being failed over.
            // In this case topology might have changed
            // and we need to readjust with every call.
            if (readjust == true) {
                // Add missing nodes.
                for (GridNode node : top) {
                    if (nodeQueue.contains(node) == false) {
                        nodeQueue.offer(node);
                    }
                }
            }
            
            GridNode next = nodeQueue.poll();

            // In case if jobs have been mapped, we need to
            // make sure that queued node is still in topology.
            if (readjust == true && next != null) {
                while (top.contains(next) == false && nodeQueue.isEmpty() == false) {
                    next = nodeQueue.poll();
                }
                
                // No nodes found and queue is empty. 
                if (next != null && top.contains(next) == false) {
                    return null;
                }
            }

            if (next != null) {
                // Add to the end.
                nodeQueue.offer(next);
            }
            
            return next;
        }
    }

    /**
     * THIS METHOD IS USED ONLY FOR TESTING.
     *
     * @return Internal list of nodes.
     */
    List<GridNode> getNodes() {
        return Collections.unmodifiableList(new ArrayList<GridNode>(nodeQueue));
    }
}
