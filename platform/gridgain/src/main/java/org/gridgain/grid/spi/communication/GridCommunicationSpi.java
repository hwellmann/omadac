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

package org.gridgain.grid.spi.communication;

import org.gridgain.apache.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;

import java.io.*;
import java.util.*;

/**
 * Communication SPI is responsible for data exchange between nodes.
 * <p>
 * Communication SPI is one of the most important SPI in GridGain. It is used
 * heavily throughout the system and provides means for all data exchanges 
 * between nodes, such as internal implementation details and user driven
 * messages.
 * <p>
 * Functionality to this SPI is exposed directly in {@link Grid} interface:
 * <ul>
 *      <li>{@link Grid#sendMessage(Collection, Serializable)}</li>
 *      <li>{@link Grid#sendMessage(GridNode, Serializable)}</li>
 *      <li>{@link Grid#addMessageListener(GridMessageListener)}</li>
 * </ul>
 * <p>
 * GridGain comes with large set of built-in communication SPI implementations:
 * <ul>
 *      <li>{@link org.gridgain.grid.spi.communication.coherence.GridCoherenceCommunicationSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.communication.jgroups.GridJgroupsCommunicationSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.communication.jms.GridJmsCommunicationSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.communication.mail.GridMailCommunicationSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.communication.mule.GridMuleCommunicationSpi}</li>
 *      <li>{@link org.gridgain.grid.spi.communication.tcp.GridTcpCommunicationSpi}</li>
 * </ul>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridCommunicationSpi extends GridSpi {
    /**
     * Sends given message to destination node. Note that characteristics of the 
     * exchange such as durability, guaranteed delivery or error notification is
     * dependant on SPI implementation.
     * 
     * @param destNode Destination node.
     * @param msg Message to send.
     * @throws GridSpiException Thrown in case of any error during sending the message. 
     *      Note that this is not guaranteed that failed communication will result
     *      in thrown exception as this is dependant on SPI implementation.
     */
    public void sendMessage(GridNode destNode, Serializable msg) throws GridSpiException;

    /**
     * Sends given message to destination nodes. Note that characteristics of the 
     * exchange such as durability, guaranteed delivery or error notification is
     * dependant on SPI implementation.
     * 
     * @param destNodes Destination nodes.
     * @param msg Message to send.
     * @throws GridSpiException Thrown in case of any error during sending the message. 
     *      Note that this is not guaranteed that failed communication will result
     *      in thrown exception as this is dependant on SPI implementation.
     */
    public void sendMessage(Collection<GridNode> destNodes, Serializable msg) throws GridSpiException;

    /**
     * Set communication listener. 
     * 
     * @param listener Listener to set or <tt>null</tt> to unset the listener.
     */
    public void setListener(GridMessageListener listener);
}
