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

package org.gridgain.grid.spi.topology;

import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

/**
 * Topology SPI allows developer to have a custom logic deciding what specific set of
 * grid nodes (topology) is available to GridGain in any given point of time. This SPI is
 * called every time before grid task gets mapped ({@link GridTask#map(List, Object)}).
 * <p>
 * Implementations can employ various strategies, e.g., some may be time based when certain nodes
 * are available only at certain time or dates, or topology can be based on average load of
 * the nodes, or it can be based on specifics of the task obtained from the task session
 * and ability to match them to grid nodes.
 * <p>
 * Note that in simple environments the topology is often the same as entire grid (sometimes
 * minus the local node). More complex topology management is required only when available
 * topology changes per task or per some other condition.
 * <p>
 * GridGain comes with following implementations:
 * <ul>
 *      <li>
 *          {@link  org.gridgain.grid.spi.topology.basic.GridBasicTopologySpi} - 
 *          based on configuration returns either all,
 *          only local, or only remote nodes. This one is a default implementation.
 *      </li>
 *      <li>
 *          {@link org.gridgain.grid.spi.topology.attributes.GridAttributesTopologySpi} - 
 *          based on attributes set.
 *          Those nodes that have attributes with the same values will be included.
 *      </li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridTopologySpi extends GridSpi {
    /**
     * This method is called by GridGain right before calling {@link GridTask#map(List, Object)}
     * to obtain a topology for the task's split.
     *
     * @param ses Current task's session. If implementation does not depend on task's
     *      information it may ignore it.
     * @param grid Full set of all grid nodes.
     * @return Topology to use for execution of the task represented by the
     *      session passed in.
     * @throws GridSpiException Thrown in case if topology cannot be obtained.
     */
    public Collection<GridNode> getTopology(GridTaskSession ses, Collection<GridNode> grid) throws GridSpiException;
}
