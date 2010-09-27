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

import java.io.*;
import java.util.*;
import org.gridgain.apache.*;

/**
 * Filter for querying grid events. See {@link Grid#queryEvents(GridEventFilter, Collection, long)}
 * and {@link Grid#queryLocalEvents(GridEventFilter)} for information on querying grid events.
 * <p>
 * Starting with version <tt>2.1</tt> peer-class-loading is enabled for event filters.
 * You can simply call {@link Grid#queryEvents(GridEventFilter, Collection, long)} and
 * your event filter will be automatically loaded and executed on all remote nodes.
 * You no longer need to manually add it to the class path on all remote nodes.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridEventFilter extends Serializable {
    /**
     * Filter for querying grid events. Determines whether or not to accept the
     * given event into final set of returned events.
     *
     * @param evt Grid event to filter.
     * @return <tt>true</tt> if event is accepted by filter, <tt>false</tt> otherwise.
     * @see Grid#queryEvents(GridEventFilter, Collection, long)
     * @see Grid#queryLocalEvents(GridEventFilter)
     */
    public boolean accept(GridEvent evt);
}
