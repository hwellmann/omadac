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
import org.gridgain.grid.kernal.managers.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.failover.*;

/**
 * FIXDOC: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridFailoverManager extends GridManagerAdapter<GridFailoverSpi> {
    /**
     * @param mgrReg Manager registry.
     * @param procReg Processor registry.
     * @param cfg Grid configuration.
     */
    public GridFailoverManager(GridConfiguration cfg, GridManagerRegistry mgrReg, GridProcessorRegistry procReg) {
        super(GridFailoverSpi.class, cfg, procReg, mgrReg, cfg.getFailoverSpi());
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
     * @param taskSes FIXDOC
     * @param jobRes FIXDOC
     * @param top Collection of all top nodes that does not include the failed node.
     * @return New node to route this job to.
     */
    public GridNode failover(GridTaskSessionImpl taskSes, GridJobResult jobRes, List<GridNode> top) {
        return getSpi(taskSes.getFailoverSpi()).failover(new GridFailoverContextImpl(taskSes, jobRes,
            mgrReg.getLoadBalancingManager()), top);
    }
}
