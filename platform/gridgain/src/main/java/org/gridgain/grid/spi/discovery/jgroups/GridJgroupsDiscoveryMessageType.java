package org.gridgain.grid.spi.discovery.jgroups;

/**
 * Enumeration of JGroups discovery message types.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
enum GridJgroupsDiscoveryMessageType {
    /** Request attributes. Node ID contains ID of the requested node. */
    GET_ATTRS,

    /** Response node's attributes. */
    SET_ATTRS,
    
    /** Response node's attributes and request for the sender attributes. */
    EXCHANGE_ATTRS,
    
    /** Metrics update from remote node. */
    METRICS_UPDATE
}
