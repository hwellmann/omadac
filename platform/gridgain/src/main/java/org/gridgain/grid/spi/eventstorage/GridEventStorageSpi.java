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

package org.gridgain.grid.spi.eventstorage;

import java.util.*;

import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;
import org.gridgain.jsr305.*;

/**
 * This SPI provides local node events storage. SPI allows for recording local
 * node events and querying recorded local events. Every node during its life-cycle
 * goes through a serious of events such as task deployment, task execution, job
 * execution, etc. (see {@link GridEventType} for full list of event types). For
 * performance reasons GridGain is designed to store all locally produced events
 * locally. These events can be later retrieved using either distributed query:
 * <ul>
 *      <li>{@link Grid#queryEvents(GridEventFilter, Collection, long)}</li>
 * </ul>
 * or local only query:
 * <ul>
 *      <li>{@link Grid#queryLocalEvents(GridEventFilter)}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 *
 * @see GridEventType
 */
@Apache20LicenseCompatible
public interface GridEventStorageSpi extends GridSpi {
    /**
     * Queries locally-stored events only. Events could be filtered out
     * by given <tt>filter</tt>.
     *
     * @param filter Event filter or <tt>null</tt> to use no filter
     *      and return all events.
     * @return Collection of events.
     */
    public List<GridEvent> queryLocalEvents(@Nullable GridEventFilter filter);

    /**
     * Records single event.
     *
     * @param evt Event that should be recorded.
     * @throws GridSpiException If event recording failed for any reason.
     */
    public void record(GridEvent evt) throws GridSpiException;
}
