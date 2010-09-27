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

package org.gridgain.grid.spi.eventstorage.memory;

import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.mbean.*;

/**
 * Management bean for {@link GridMemoryEventStorageSpi}.
 * Beside properties defined for every SPI bean this one gives access to:
 * <ul>
 * <li>Event expiration time (see {@link #getExpireAgeMs()})</li>
 * <li>Maximum queue size (see {@link #getExpireCount()})</li>
 * <li>Method that removes all items from queue (see {@link #clearAll()})</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@GridMBeanDescription("MBean that provides access to memory event storage SPI configuration.")
public interface GridMemoryEventStorageSpiMBean extends GridSpiManagementMBean {
    /**
     * Gets event time-to-live value. Implementation must guarantee
     * that event would not be accessible if its lifetime exceeds this value.
     *
     * @return Event time-to-live.
     */
    @GridMBeanDescription("Event time-to-live value.")
    public long getExpireAgeMs();

    /**
     * Gets maximum event queue size. New incoming events will oust
     * oldest ones if queue size exceeds this limit. 
     *
     * @return Maximum event queue size. 
     */
    @GridMBeanDescription("Maximum event queue size.")
    public long getExpireCount();

    /**
     * Removes all events from the event queue.
     */
    @GridMBeanDescription("Removes all events from the event queue.")
    public void clearAll();
}
