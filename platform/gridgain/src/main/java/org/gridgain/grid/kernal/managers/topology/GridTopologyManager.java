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
package org.gridgain.grid.kernal.managers.topology;

import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.topology.*;

/**
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 *
 */
public class GridTopologyManager extends GridManagerAdapter<GridTopologySpi> {
    /**
     * @param cfg Grid configuration.
     * @param mgrRec Manager registry.
     * @param procReg Processor registry.
     */
    public GridTopologyManager(GridConfiguration cfg, GridManagerRegistry mgrRec, GridProcessorRegistry procReg) {
        super(GridTopologySpi.class, cfg, procReg, mgrRec, cfg.getTopologySpi());
    }

    /**
     *
     * @throws GridException FIXDOC
     */
    public void start() throws GridException {
        startSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(startInfo());
        }
    }

    /**
     *
     * @throws GridException FIXDOC
     */
    public void stop() throws GridException {
        stopSpi();

        if (log.isDebugEnabled() == true) {
            log.debug(stopInfo());
        }
    }

    /**
     * @param taskSes FIXDOC
     * @param grid FIXDOC
     * @return Task topology.
     * @throws GridException FIXDOC
     */
    public Collection<GridNode> getTopology(GridTaskSessionImpl taskSes, Collection<GridNode> grid)
        throws GridException {
        try {
            return getSpi(taskSes.getTopologySpi()).getTopology(taskSes, grid);
        }
        catch (GridSpiException e) {
            throw (GridException)new GridException("Failed to get topology for task [taskSes=" + taskSes + ", grid=" + grid + ']', e).setData(82, "src/java/org/gridgain/grid/kernal/managers/topology/GridTopologyManager.java");
        }
    }
}
