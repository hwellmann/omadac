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

package org.gridgain.grid.spi;

import java.io.*;
import java.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.communication.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.apache.*;

/**
 * TODO: add file description.
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 */
@Apache20LicenseCompatible
public interface GridSpiContext {
    /**
     * Gets a collection of remote grid nodes. Remote nodes are discovered via underlying
     * {@link GridDiscoverySpi} implementation used. Unlike {@link #getAllNodes()},
     * this method does not include local grid node.
     *
     * @return Collection of remote grid nodes.
     * @see #getLocalNode()
     * @see #getAllNodes()
     * @see GridDiscoverySpi
     */
    public Collection<GridNode> getRemoteNodes();

    /**
     * Gets a collection of all grid nodes. Remote nodes are discovered via underlying
     * {@link GridDiscoverySpi} implementation used. Unlike {@link #getRemoteNodes()},
     * this method does include local grid node.
     *
     * @return Collection of remote grid nodes.
     * @see #getLocalNode()
     * @see #getRemoteNodes()
     * @see GridDiscoverySpi
     */
    public Collection<GridNode> getAllNodes();

    /**
     * Gets local grid node. Instance of local node is provided by underlying {@link GridDiscoverySpi}
     * implementation used.
     *
     * @return Local grid node.
     * @see GridDiscoverySpi
     */
    public GridNode getLocalNode();

    /**
     * Gets a node instance based on its ID.
     *
     * @param nodeId ID of a node to get.
     * @return Node for a given ID or <tt>null</tt> is such not has not been discovered.
     * @see GridDiscoverySpi
     */
    public GridNode getNode(UUID nodeId);

    /**
     * Pings a remote node. The underlying communication is provided via
     * {@link GridDiscoverySpi#pingNode(UUID)} implementation.
     * <p>
     * Discovery SPIs usually have some latency in discovering failed nodes. Hence,
     * communication to remote nodes may fail at times if an attempt was made to
     * establish communication with a failed node. This method can be used to check
     * if communication has failed due to node failure or due to some other reason.
     *
     * @param nodeId ID of a node to ping.
     * @return <tt>true</tt> if node for a given ID is alive, <tt>false</tt> otherwise.
     * @see GridDiscoverySpi
     */
    public boolean pingNode(UUID nodeId);

    /**
     * Adds a listener for discovery events. Refer to {@link GridDiscoveryEventType}
     * for a set of all possible discovery events.
     *
     * @param listener Listener to discovery events.
     */
    public void addDiscoveryListener(GridDiscoveryListener listener);

    /**
     * Removes discovery event listener.
     *
     * @param listener Discovery event listener to remove.
     * @return <tt>True</tt> if listener was removed, <tt>false</tt> otherwise.
     */
    public boolean removeDiscoveryListener(GridDiscoveryListener listener);

    /**
     * Sends a message to a remote node. The underlying communication mechanism is defined by
     * {@link GridCommunicationSpi} implementation used.
     * <p>
     * This method can be used by jobs to communicate with other nodes in the grid. Remote nodes
     * can receive messages by registering a listener through {@link #addMessageListener(GridMessageListener, String)}
     * method.
     *
     * @param node Node to send a message to.
     * @param msg Message to send.
     * @param topic Topic to send message to.
     * @throws GridSpiException If failed to send a message to remote node.
     * @see GridCommunicationSpi
     * @see #addMessageListener(GridMessageListener, String)
     * @see #removeMessageListener(GridMessageListener, String)
     */
    public void sendMessage(GridNode node, Serializable msg, String topic) throws GridSpiException;

    /**
     * Sends a message to a group of remote nodes. The underlying communication mechanism is defined by
     * {@link GridCommunicationSpi} implementation used.
     * <p>
     * This method can be used by jobs to communicate with other nodes in the grid. Remote nodes
     * can receive messages by registering a listener through {@link #addMessageListener(GridMessageListener, String)}
     * method.
     *
     * @param nodes Group of nodes to send a message to.
     * @param msg Message to send.
     * @param topic Topic to send message to.
     * @throws GridSpiException If failed to send a message to any of the remote nodes.
     * @see GridCommunicationSpi
     * @see #addMessageListener(GridMessageListener, String)
     * @see #removeMessageListener(GridMessageListener, String)
     */
    public void sendMessage(Collection<GridNode> nodes, Serializable msg, String topic) throws GridSpiException;

    /**
     * Register a message listener to receive messages sent by remote nodes. The underlying
     * communication mechanism is defined by {@link GridCommunicationSpi} implementation used.
     * <p>
     * This method can be used by jobs to communicate with other nodes in the grid. Remote nodes
     * can send messages by calling {@link #sendMessage(GridNode, Serializable, String)} or
     * {@link #sendMessage(Collection, Serializable, String)} methods.
     *
     * @param listener Message listener to register.
     * @param topic Topic to register listener for.
     * @see GridCommunicationSpi
     * @see #sendMessage(GridNode, Serializable, String)
     * @see #sendMessage(Collection, Serializable, String)
     * @see #removeMessageListener(GridMessageListener, String)
     */
    public void addMessageListener(GridMessageListener listener, String topic);

    /**
     * Removes a previously registered message listener.
     *
     * @param listener Message listener to remove.
     * @param topic Topic to unregister listener for.
     * @return <tt>true</tt> of message listener was removed, <tt>false</tt> if it was not
     *      previously registered.
     * @see #addMessageListener(GridMessageListener, String)
     */
    public boolean removeMessageListener(GridMessageListener listener, String topic);

    /**
     * Adds an event listener for local events. Refer to {@link GridEventType}
     * for a set of all possible events.
     *
     * @param listener Event listener for local events.
     * @see GridEventType
     */
    public void addLocalEventListener(GridLocalEventListener listener);

    /**
     * Removes local event listener.
     *
     * @param listener Local event listener to remove.
     * @return <tt>true</tt> if listener was removed, <tt>false</tt> otherwise.
     */
    public boolean removeLocalEventListener(GridLocalEventListener listener);

    /**
     * Obtain grid node topology for a given task.
     *
     * @param taskSes Task session.
     * @param grid Available grid nodes.
     * @return Topology for given task session.
     * @throws GridSpiException If failed to get topology.
     */
    public Collection<GridNode> getTopology(GridTaskSession taskSes, Collection<GridNode> grid) throws GridSpiException;
}
