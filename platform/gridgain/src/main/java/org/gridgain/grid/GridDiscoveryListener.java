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

import java.util.*;
import org.gridgain.apache.*;
import org.gridgain.grid.spi.discovery.*;

/**
 * Listener for grid node discovery events. See
 * {@link GridDiscoverySpi} for information on how grid nodes get discovered.
 * <p>
 * Use {@link Grid#addDiscoveryListener(GridDiscoveryListener)} to register
 * this listener with grid.
 * <p>
 * Note, that user should keep logic in discovery listener very short because
 * discovery notifications happen from one thread and therefore listener logic
 * must complete fast. Holding discovery listener for a long time will result
 * in delayed discovery notifications. The reason it is implemented this way is
 * to guarantee order - {@link GridDiscoveryEventType#JOINED} event
 * will always come before {@link GridDiscoveryEventType#LEFT} event.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridDiscoveryListener extends EventListener {
    /**
     * Notification for grid node discovery events.
     *
     * @param type Node discovery event type.
     * @param node Node affected. Either newly joined node, left node or failed node.
     */
    public void onDiscovery(GridDiscoveryEventType type, GridNode node);
}
