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
import org.gridgain.grid.test.junit3.*;

/**
 * Optional router used to route individual tests to remote nodes for
 * execution. Distributed JUnits can be instructed to use a specific
 * implementation of this router in one of the following ways:
 * <ul>
 * <li>By specifying router class name as {@link GridTestVmParameters#GRID_TEST_ROUTER} VM parameter</li>
 * <li>By specifying router class from {@link GridifyTest#routerClass()} annotation method.</li>
 * <li>By setting it on test suite explicitely by calling {@link GridJunit3TestSuite#setRouterClass(Class)} method.</li>
 * </ul>
 * If not provided, then by default {@link GridTestRouterAdapter} is used which routes tests to
 * grid nodes in round-robin fashion. Refer to {@link GridTestRouterAdapter} documentation for
 * more information.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public interface GridTestRouter {
    /**
     * Routes a test to a specific node. This method is provided so subclasses
     * could specify their own logic for routing tests. It should be useful
     * when some test can only be run on a specific node or on a specific
     * set of nodes.
     * <p>
     * By default tests are routed to grid nodes in round-robin fashion.
     * Set {@link GridTestVmParameters#GRID_ROUTER_PREFER_REMOTE} VM parameter
     * to <tt>true</tt> to always route tests to remote nodes, assuming there are any.
     * If there are no remote nodes, then tests will still execute locally even
     * if this VM parameter is set.
     *
     * @param test Test class.
     * @param name Test name.
     * @param subgrid List of available grid nodes.
     * @param locNodeId Local node ID.
     * @return Node this test should execute on.
     */
    public GridNode route(Class<?> test, String name, List<GridNode> subgrid, UUID locNodeId);
}
