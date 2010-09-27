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

package org.gridgain.grid;

import org.gridgain.apache.*;
import org.gridgain.grid.spi.discovery.*;

/**
 * Event types for grid node discovery events. See {@link GridDiscoverySpi} for
 * information about how grid discovers nodes. Note that discovery events are
 * different from general {@link GridEventType} events and are used by
 * {@link GridDiscoveryListener}.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridDiscoveryListener
 */
@Apache20LicenseCompatible
public enum GridDiscoveryEventType {
    /**
     * New node has been discovered and joined grid topology.
     * Note that even though a node has been discovered there could be
     * a number of warnings in the log. In certain situations GridGain
     * doesn't prevent a node from joining but prints warning messages into the log.
     */
    JOINED,

    /**
     * Node has normally left the grid.
     */
    LEFT,

    /**
     * GridGain detected that node has presumably crashed and is considered failed.
     */
    FAILED,

    /**
     * Callback for when node's metrics are updated. In most cases this callback
     * is invoked with every heartbeat received from a node (including local node).
     */
    METRICS_UPDATED
}
