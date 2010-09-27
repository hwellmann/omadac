package org.gridgain.grid.spi.discovery.jboss;

/**
 * Internal status of the node monitored by the system.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
enum GridJbossDiscoveryNodeState {
    /** Node created, but node data was not received yet. */
    NEW_NO_DATA,

    /** Data received before node joined discovery. */
    NEW_HAS_DATA,

    /** Node is fully initialized. */
    READY
}
