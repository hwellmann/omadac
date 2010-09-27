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

package org.gridgain.grid.spi.communication.mule2;

import java.util.*;
import org.gridgain.grid.util.tostring.*;

/**
 * This component should be registered in Mule configuration.
 * It is used by SPI for getting notifications from Mule.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
public class GridMuleCommunicationComponent {
    /** Listener to notify SPI about new messages. */
    private GridMuleCommunicationComponentListener listener = null;

    /** Component properties. */
    private Properties props = null;

    /**
     * Sets SPI listener.
     *
     * @param listener SPI listener.
     */
    public void setListener(GridMuleCommunicationComponentListener listener) {
        this.listener = listener;
    }

    /**
     * Called by Mule when new message received.
     *
     * @param msg Message being received.
     */
    public void onMessage(byte[] msg) {
        GridMuleCommunicationComponentListener listener = this.listener;

        if (listener != null) {
            listener.onMessage(msg);
        }
    }

    /**
     * Gets component properties.
     *
     * @return Component properties.
     */
    public Properties getProperties() {
        return props;
    }

    /**
     * Sets component properties.
     *
     * @param props Component properties.
     */
    public void setProperties(Properties props) {
        this.props = props;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMuleCommunicationComponent.class, this);
    }
}
