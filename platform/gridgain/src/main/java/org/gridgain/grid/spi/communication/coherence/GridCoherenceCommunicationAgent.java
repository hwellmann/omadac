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

package org.gridgain.grid.spi.communication.coherence;

import com.tangosol.net.*;
import java.io.*;
import java.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * Contains task which should be running on destination Coherence member node.
 * SPI will send that objects only to members with started invocation service
 * {@link InvocationService} with name
 * {@link GridCoherenceCommunicationSpi#setServiceName(String)}. The agents used as
 * transport to notify remote communication SPI's.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
class GridCoherenceCommunicationAgent extends AbstractInvocable {
    /** Communication message. */
    private Serializable msg = null;

    /** Sender UID. */
    private UUID srcNodeId = null;

    /**
     * Creates an agent.
     *
     * @param srcNodeId Sender UID.
     * @param msg Message being sent.
     */
    GridCoherenceCommunicationAgent(UUID srcNodeId, Serializable msg) {
        assert srcNodeId != null : "ASSERTION [line=53, file=src/java/org/gridgain/grid/spi/communication/coherence/GridCoherenceCommunicationAgent.java]";

        this.srcNodeId = srcNodeId;
        this.msg = msg;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
       GridCoherenceCommunicationSpi spi = (GridCoherenceCommunicationSpi)getService().getUserContext();

        if (spi != null) {
            spi.onMessage(srcNodeId, msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridCoherenceCommunicationAgent.class, this);
    }
}
