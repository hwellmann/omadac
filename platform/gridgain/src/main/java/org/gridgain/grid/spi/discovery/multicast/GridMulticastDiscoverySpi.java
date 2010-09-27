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

package org.gridgain.grid.spi.discovery.multicast;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.marshaller.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.tostring.*;
import static org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoveryMessageType.*;
import static org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoveryNodeState.*;

/**
 * Discovery SPI implementation that uses IP-multicast for node discovery. At startup
 * SPI starts sending IP/Multicast heartbeat messages. Once other nodes
 * receive these messages, they use TCP/IP to exchange node attributes and then
 * add the new node to their topology. When a node shuts down, it sends <tt>LEAVE</tt>
 * heartbeat to other nodes, so every node in the grid can gracefully remove
 * this node from topology.
 * <p>
 * Note that since IP/Multicast is not a reliable protocol, there is no guarantee that
 * a node will be discovered by other grid members. However, IP/Multicast works
 * very reliably within LANs and in most cases this SPI provides a very light weight
 * and easy to use grid node discovery.
 * <p>
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * The following configuration parameters are optional:
 * <ul>
 * <li>Heartbeat frequency (see {@link #setHeartbeatFrequency(long)})</li>
 * <li>Number of retries to send leaving notification(see {@link #setLeaveAttempts(int)})</li>
 * <li>Local IP address (see {@link #setLocalAddress(String)})</li>
 * <li>Port number (see {@link #setTcpPort(int)})</li>
 * <li>Messages TTL(see {@link #setTimeToLive(int)})</li>
 * <li>Number of heartbeats that could be missed (see {@link #setMaxMissedHeartbeats(int)})</li>
 * <li>Multicast IP address (see {@link #setMulticastGroup(String)})</li>
 * <li>Multicast port number (see {@link #setMulticastPort(int)})</li>
 * <li>Local port range (see {@link #setLocalPortRange(int)}</li>
 * <li>Check whether multiast is enabled (see {@link #setCheckMulticastEnabled(boolean)}</li>
 * </ul>
 * <h2 class="header">Java Example</h2>
 * GridMulticastDiscoverySpi is used by default and should be explicitely configured
 * only if some SPI configuration parameters need to be overridden. Examples below
 * insert own multicast group value that differs from default 228.1.2.4.
 * <pre name="code" class="java">
 * GridMulticastDiscoverySpi spi = new GridMulticastDiscoverySpi();
 *
 * // Put another multicast group.
 * spi.setMulticastGroup("228.10.10.157");
 *
 * GridConfigurationAdapter cfg = new GridConfigurationAdapter();
 *
 * // Override default discovery SPI.
 * cfg.setDiscoverySpi(spi);
 *
 * // Start grid.
 * GridFactory.start(cfg);
 * </pre>
 * <h2 class="header">Spring Example</h2>
 * GridMulticastDiscoverySpi can be configured from Spring XML configuration file:
 * <pre name="code" class="xml">
 * &lt;bean id="grid.custom.cfg" class="org.gridgain.grid.GridConfigurationAdapter" singleton="true"&gt;
 *         ...
 *         &lt;property name="discoverySpi"&gt;
 *             &lt;bean class="org.gridgain.grid.spi.discovery.multicast.GridMulticastDiscoverySpi"&gt;
 *                 &lt;property name="multicastGroup" value="228.10.10.157"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *         ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * <img src="http://www.gridgain.com/images/spring-small.png">
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 *
 * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
 * @version 2.1.1
 * @see GridDiscoverySpi
 */
@GridSpiInfo(
    author = "GridGain Project",
    url = "www.gridgain.org",
    email = "support@gridgain.com",
    version = "2.1.1")
@GridSpiMultipleInstancesSupport(true)
public class GridMulticastDiscoverySpi extends GridSpiAdapter implements GridDiscoverySpi,
    GridMulticastDiscoverySpiMBean {
    /** Default heartbeat delay (value is <tt>3000</tt>). */
    public static final long DFLT_HEARTBEAT_FREQ = 3000;

    /** Default number of heartbeat messages that could be missed (value is <tt>3</tt>). */
    public static final int DFLT_MAX_MISSED_HEARTBEATS = 3;

    /** Default multicast IP address (value is <tt>228.1.2.4</tt>). */
    public static final String DFLT_MCAST_GROUP = "228.1.2.4";

    /** Default multicast port number (value is <tt>47200</tt>). */
    public static final int DFLT_MCAST_PORT = 47200;

    /** Default local port number for SPI (value is <tt>47300</tt>). */
    public static final int DFLT_TCP_PORT = 47300;

    /**
     * Default local port range (value is <tt>10</tt>).
     * See {@link #setLocalPortRange(int)} for details.
     */
    public static final int DFLT_PORT_RANGE = 10;

    /** Default number of attempts to send leaving notification (value is <tt>3</tt>). */
    public static final int DFLT_LEAVE_ATTEMPTS = 3;

    /**  Default multicast messages time-to-live value (value is <tt>8</tt>). */
    public static final int DFLT_TTL = 8;

    /** Heartbeat attribute key should be the same on all nodes. */
    private static final String HEARTBEAT_ATTRIBUTE_KEY = "gridgain:discovery:heartbeat";

    /** */
    @GridLoggerResource
    private GridLogger log = null;

    /** */
    @GridMarshallerResource
    private GridMarshaller marshaller = null;

    /** */
    @GridLocalNodeIdResource
    private UUID nodeId = null;

    /** */
    private String gridName = null;

    /** Map of all nodes in grid. */
    private Map<UUID, GridMulticastDiscoveryNode> allNodes = null;

    /** Set of remote nodes that have state <tt>READY</tt>. */
    private List<GridNode> rmtNodes = null;

    /** Local node representation. */
    private GridMulticastDiscoveryNode locNode = null;

    /** Local node attributes. */
    private Map<String, Serializable> nodeAttrs = null;

    /** Delay between heartbeat requests. */
    private long beatFreq = DFLT_HEARTBEAT_FREQ;

    /** Number of heartbeat messages that could be missed before remote node is considered as failed one. */
    private int maxMissedBeats = DFLT_MAX_MISSED_HEARTBEATS;

    /** Local IP address. */
    private InetAddress localHost = null;

    /** Multicast IP address as string. */
    private String mcastGroup = DFLT_MCAST_GROUP;

    /** Multicast IP address. */
    private InetAddress mcastAddr = null;

    /** Multicast port number. */
    private int mcastPort = DFLT_MCAST_PORT;

    /** Local port number. */
    private int tcpPort = DFLT_TCP_PORT;

    /** Number of attempts to send leaving notification. */
    private int leaveAttempts = DFLT_LEAVE_ATTEMPTS;

    /** Local IP address as string. */
    private String localAddr = null;

    /** */
    private int localPortRange = DFLT_PORT_RANGE;

    /** */
    private volatile GridDiscoveryListener listener = null;

    /** */
    private GridDiscoveryMetricsProvider metricsProvider = null;

    /** */
    private MulticastHeartbeatSender mcastSender = null;

    /** */
    private MulticastHeartbeatReceiver mcastRcvr = null;

    /** */
    private TcpHandshakeListener tcpListener = null;

    /** */
    private NodeSweeper nodeSweeper = null;

    /** Set of threads that requests attributes from remote nodes. */
    private Set<GridSpiThread> workers = null;

    /** */
    private long startTime = -1;

    /** Messages TTL value. */
    private int ttl = DFLT_TTL;

    /** */
    private int boundTcpPort = -1;

    /**
     * Flag to check whether multicast is enabled on local node or not. This
     * check is performed by sending an multicast message to itself.
     */
    private boolean isMcastEnabled = false;

    /** Flag indicating whether need to check for multicast enabled or not. */
    private boolean isCheckMulticastEnabled = true;

    /** Set to <tt>true</tt> when {@link #spiStop()} is called. */
    private final AtomicBoolean isStopping = new AtomicBoolean(false);

    /** */
    private final Object mux = new Object();

    /**
     * Sets IP address of multicast group.
     * <p>
     * If not provided, default value is {@link #DFLT_MCAST_GROUP}.
     *
     * @param mcastGroup Multicast IP address.
     */
    @GridSpiConfiguration(optional = true)
    public void setMulticastGroup(String mcastGroup) {
        this.mcastGroup = mcastGroup;
    }

    /**
     * {@inheritDoc}
     */
    public String getMulticastGroup() {
        return mcastGroup;
    }

    /**
     * Sets port number which multicast messages are sent to.
     * <p>
     * If not provided, default value is {@link #DFLT_MCAST_PORT}.
     *
     * @param mcastPort Multicast port number.
     */
    @GridSpiConfiguration(optional = true)
    public void setMulticastPort(int mcastPort) {
        this.mcastPort = mcastPort;
    }

    /**
     * {@inheritDoc}
     */
    public int getMulticastPort() {
        return mcastPort;
    }

    /**
     * Sets local TCP port number to be used for node attribute
     * exchange upon discovery.
     * <p>
     * If not provided, default value is {@link #DFLT_TCP_PORT}.
     *
     * @param tcpPort Port number.
     */
    @GridSpiConfiguration(optional = true)
    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    /**
     * {@inheritDoc}
     */
    public int getTcpPort() {
        return tcpPort;
    }

    /**
     * Sets delay between heartbeat requests. SPI sends broadcast messages in
     * configurable time interval to other nodes to notify them about its state.
     * <p>
     * If not provided, default value is {@link #DFLT_HEARTBEAT_FREQ}.
     *
     * @param beatFreq Time in milliseconds.
     */
    @GridSpiConfiguration(optional = true)
    public void setHeartbeatFrequency(long beatFreq) {
        this.beatFreq = beatFreq;
    }

    /**
     * {@inheritDoc}
     */
    public long getHeartbeatFrequency() {
        return beatFreq;
    }

    /**
     * Sets number of heartbeat requests that could be missed before remote
     * node is considered to be failed.
     * <p>
     * If not provided, default value is {@link #DFLT_MAX_MISSED_HEARTBEATS}.
     *
     * @param maxMissedBeats Number of missed requests.
     */
    @GridSpiConfiguration(optional = true)
    public void setMaxMissedHeartbeats(int maxMissedBeats) {
        this.maxMissedBeats = maxMissedBeats;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumMissedHeartbeats() {
        return maxMissedBeats;
    }

    /**
     * Sets number of attempts to notify another nodes that this one is leaving
     * grid. Multiple leave requests are sent to increase the chance of successful
     * delivery to every node, since IP Multicast protocol is unreliable.
     * Note that on most networks loss of IP Multicast packets is generally
     * negligible.
     * <p>
     * If not provided, default value is {@link #DFLT_LEAVE_ATTEMPTS}.
     *
     * @param leaveAttempts Number of attempts.
     */
    @GridSpiConfiguration(optional = true)
    public void setLeaveAttempts(int leaveAttempts) {
        this.leaveAttempts = leaveAttempts;
    }

    /**
     * {@inheritDoc}
     */
    public int getLeaveAttempts() {
        return leaveAttempts;
    }

    /**
     * Sets local host IP address that discovery SPI uses.
     * <p>
     * If not provided, by default a first found non-loopback address
     * will be used. If there is no non-loopback address available,
     * then {@link InetAddress#getLocalHost()} will be used.
     *
     * @param localAddr IP address.
     */
    @GridSpiConfiguration(optional = true)
    public void setLocalAddress(String localAddr) {
        this.localAddr = localAddr;
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalHost() {
        return locNode.getPhysicalAddress();
    }

    /**
     * Gets TCP messages time-to-live.
     *
     * @return TTL.
     */
    public int getTimeToLive() {
        return ttl;
    }

    /**
     * Sets Multicast messages time-to-live in router hops.
     * <p>
     * If not provided, default value is {@link #DFLT_TTL}.
     *
     * @param ttl Messages TTL.
     */
    @GridSpiConfiguration(optional = true)
    public void setTimeToLive(int ttl) {
        this.ttl = ttl;
    }

    /**
     * Sets local port range for TCP and Multicast ports (value must greater than or equal to <tt>0</tt>).
     * If provided local port (see {@link #setMulticastPort(int)} or {@link #setTcpPort(int)} is occupied,
     * implementation will try to increment the port number for as long as it is less than
     * initial value plus this range.
     * <p>
     * If port range value is <tt>0</tt>, then implementation will try bind only to the port provided by
     * {@link #setMulticastPort(int)} or {@link #setTcpPort(int)} methods and fail if binding to these
     * ports did not succeed.
     * <p>
     * Local port range is very useful during development when more than one grid nodes need to run
     * on the same physical machine.
     *
     * @param localPortRange New local port range.
     * @see #DFLT_PORT_RANGE
     */
    @GridSpiConfiguration(optional = true)
    public void setLocalPortRange(int localPortRange) {
        this.localPortRange = localPortRange;
    }

    /**
     * {@inheritDoc}
     */
    public int getLocalPortRange() {
        return localPortRange;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCheckMulticastEnabled() {
        return isCheckMulticastEnabled;
    }

    /**
     * Enables or disabled check whether multicast is enabled on local node.
     * By default this value is <tt>true</tt>. On startup GridGain will check
     * if local node can receive multicast packets, and if not, will not allow
     * the node to startup.
     * <p>
     * This property should be disabled in rare cases when loopback multicast
     * is disabled, but multicast to other remote boxes is enabled.
     *
     * @param isCheckMulticastEnabled <tt>True</tt> for enabling multicast check,
     *      <tt>false</tt> for disabling it.
     */
    public void setCheckMulticastEnabled(boolean isCheckMulticastEnabled) {
        this.isCheckMulticastEnabled = isCheckMulticastEnabled;
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeAttributes(Map<String, Serializable> attrs) {
        // Seal it.
        nodeAttrs = Collections.unmodifiableMap(attrs);
    }

    /**
     * {@inheritDoc}
     */
    public List<GridNode> getRemoteNodes() {
        synchronized (mux) {
            if (rmtNodes == null) {
                rmtNodes = new ArrayList<GridNode>(allNodes.size());

                for (GridMulticastDiscoveryNode node : allNodes.values()) {
                    assert node.equals(locNode) == false : "ASSERTION [line=481, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

                    if (node.getState() == READY) {
                        rmtNodes.add(node);
                    }
                }

                // Seal it.
                rmtNodes = Collections.unmodifiableList(rmtNodes);
            }

            return rmtNodes;
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=500, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

        if (locNode.getId().equals(nodeId) == true) {
            return locNode;
        }

        synchronized (mux) {
            GridMulticastDiscoveryNode node = allNodes.get(nodeId);

            return node != null && node.getState() == READY ? node : null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<UUID> getRemoteNodeIds() {
        Set<UUID> ids = new HashSet<UUID>();

        for (GridNode node : getRemoteNodes()) {
            ids.add(node.getId());
        }

        return ids;
    }

    /**
     * {@inheritDoc}
     */
    public int getRemoteNodeCount() {
        Collection<GridNode> tmp = getRemoteNodes();

        return tmp == null ? 0 : tmp.size();
    }

    /**
     * {@inheritDoc}
     */
    public GridNode getLocalNode() {
        return locNode;
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(GridDiscoveryListener listener) {
        this.listener = listener;
    }

    /**
     * {@inheritDoc}
     */
    public void setMetricsProvider(GridDiscoveryMetricsProvider metricsProvider) {
        this.metricsProvider = metricsProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Serializable> getNodeAttributes() throws GridSpiException {
        return GridUtils.makeMap(createSpiAttributeName(HEARTBEAT_ATTRIBUTE_KEY), getHeartbeatFrequency());
    }

    /**
     * {@inheritDoc}
     */
    public void spiStart(String gridName) throws GridSpiException {
        // Start SPI start stopwatch.
        startStopwatch();

        assertParameter(mcastGroup != null, "mcastGroup != null");
        assertParameter(mcastPort >= 0, "mcastPort >= 0");
        assertParameter(mcastPort <= 65535, "mcastPort <= 65535");
        assertParameter(tcpPort >= 0, "tcpPort >= 0");
        assertParameter(tcpPort <= 65535, "tcpPort <= 65535");
        assertParameter(beatFreq > 0, "beatFreq > 0");
        assertParameter(maxMissedBeats > 0, "maxMissedBeats > 0");
        assertParameter(leaveAttempts > 0, "leaveAttempts > 0");
        assertParameter(ttl > 0, "ttl > 0");
        assertParameter(localPortRange >= 0, "localPortRange >= 0");

        startTime = System.currentTimeMillis();

        allNodes = new HashMap<UUID, GridMulticastDiscoveryNode>();

        workers = new HashSet<GridSpiThread>();

        // Verify valid addresses.
        try {
            localHost = localAddr == null ? GridNetworkHelper.getLocalHost() : InetAddress.getByName(localAddr);
        }
        catch (IOException e) {
            throw (GridSpiException)new GridSpiException("Unknown local address: " + localAddr, e).setData(593, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
        }

        try {
            mcastAddr = InetAddress.getByName(mcastGroup);
        }
        catch (UnknownHostException e) {
            throw (GridSpiException)new GridSpiException("Unknown multicast group: " + mcastGroup, e).setData(600, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
        }

        if (mcastAddr.isMulticastAddress() == false) {
            throw (GridSpiException)new GridSpiException("Invalid multicast group address : " + mcastAddr).setData(604, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
        }

        // Ack parameters.
        if (log.isInfoEnabled() == true) {
            log.info(configInfo("mcastGroup", mcastGroup));
            log.info(configInfo("mcastPort", mcastPort));
            log.info(configInfo("tcpPort", tcpPort));
            log.info(configInfo("localPortRange", localPortRange));
            log.info(configInfo("beatFreq", beatFreq));
            log.info(configInfo("maxMissedBeats", maxMissedBeats));
            log.info(configInfo("leaveAttempts", leaveAttempts));
            log.info(configInfo("localHost", localHost.getHostAddress()));
            log.info(configInfo("ttl", ttl));
        }

        // Warn on odd beat frequency.
        if (beatFreq < 2000) {
            log.warning("Heartbeat frequency is too low (at least 2000ms): " + beatFreq);
        }

        // Warn on odd maximum missed heartbeats.
        if (maxMissedBeats < 3) {
            log.warning("Maximum missed heartbeats value is too low (at least 3): " + maxMissedBeats);
        }

        this.gridName = gridName;

        // Create TCP listener first, as it initializes boundTcpPort used
        // by MulticastHeartbeatSender to send heartbeats.
        tcpListener = new TcpHandshakeListener();
        mcastRcvr = new MulticastHeartbeatReceiver();
        mcastSender = new MulticastHeartbeatSender();
        nodeSweeper = new NodeSweeper();

        // Initialize local node prior to starting threads, as they
        // are using data from local node.
        locNode = new GridMulticastDiscoveryNode(nodeId, localHost, boundTcpPort, startTime, metricsProvider);

        locNode.setAttributes(nodeAttrs);

        registerMBean(gridName, this, GridMulticastDiscoverySpiMBean.class);

        tcpListener.start();
        mcastRcvr.start();
        mcastSender.start();
        nodeSweeper.start();

        // Ack local node.
        if (log.isInfoEnabled() == true) {
            log.info("Local node: " + locNode);
        }

        try {
            // Wait to discover other nodes.
            if (log.isInfoEnabled() == true) {
                log.info("Waiting for initial heartbeat timeout (" + beatFreq + " milliseconds)");
            }

            // Wait for others to add this node to topology.
            Thread.sleep(beatFreq);

            if (isCheckMulticastEnabled == true) {
                long delta = beatFreq * maxMissedBeats;
                long end = System.currentTimeMillis() + delta;

                synchronized (mux) {
                    while (isMcastEnabled == false && delta > 0) {
                        mux.wait(delta);

                        delta = end - System.currentTimeMillis();
                    }

                    if (isMcastEnabled == false) {
                        throw (GridSpiException)new GridSpiException("Multicast is not enabled on this node. Check you firewall settings " +
                            "or contact network administrator if Windows group policy is used.").setData(678, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
                    }
                }
            }
        }
        catch (InterruptedException e) {
            throw (GridSpiException)new GridSpiException("Got interrupted while starting multicast discovery.", e).setData(685, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
        }

        // Ack ok start.
        if (log.isInfoEnabled() == true) {
            log.info(startInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void spiStop() throws GridSpiException {
        isStopping.set(true);

        GridUtils.interrupt(mcastSender);
        GridUtils.interrupt(nodeSweeper);
        GridUtils.interrupt(mcastRcvr);
        GridUtils.interrupt(tcpListener);

        GridUtils.join(mcastSender, log);
        GridUtils.join(nodeSweeper, log);
        GridUtils.join(mcastRcvr, log);
        GridUtils.join(tcpListener, log);

        Set<GridSpiThread> rcvrs = null;

        synchronized (mux) {
            // Copy to local set to avoid deadlock.
            if (workers != null) {
                rcvrs = new HashSet<GridSpiThread>(workers);
            }
        }

        if (rcvrs != null) {
            GridUtils.interrupt(rcvrs);
            GridUtils.joinThreads(rcvrs, log);
        }

        startTime = -1;

        //Clear inner collections.
        synchronized(mux) {
            rmtNodes = null;
            allNodes = null;
        }

        mcastSender = null;
        mcastRcvr = null;
        tcpListener = null;
        nodeSweeper = null;
        workers = null;

        unregisterMBean();

        // Ack ok stop.
        if (log.isInfoEnabled() == true) {
            log.info(stopInfo());
        }
    }

    /**
     * Method is called when any discovery event occurs. It calls external listener.
     *
     * @param evt Discovery event type.
     * @param node Remote node this event is connected with.
     */
    private void notifyDiscovery(GridDiscoveryEventType evt, GridMulticastDiscoveryNode node) {
        assert evt != null : "ASSERTION [line=753, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";
        assert node != null : "ASSERTION [line=754, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

        if (node.getState().equals(NEW) == false) {
            GridDiscoveryListener listener = this.listener;

            if (listener != null) {
                listener.onDiscovery(evt, node);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean pingNode(UUID nodeId) {
        assert nodeId != null : "ASSERTION [line=769, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

        GridMulticastDiscoveryNode node = null;

        if (locNode.getId().equals(nodeId) == true) {
            node = locNode;
        }
        else {
            synchronized (mux) {
                node = allNodes.get(nodeId);
            }
        }

        if (node == null || node.getInetAddress() == null || node.getTcpPort() <= 0) {
            if (log.isDebugEnabled() == true) {
                log.debug("Ping failed (invalid node): " + nodeId);
            }

            return false;
        }

        Socket sock = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            sock = new Socket(node.getInetAddress(), node.getTcpPort());

            in = sock.getInputStream();
            out = sock.getOutputStream();

            GridMarshalHelper.marshal(marshaller, new GridMulticastDiscoveryMessage(PING_REQUEST), out);

            GridMulticastDiscoveryMessage res = GridMarshalHelper.unmarshal(marshaller, in, getClass().getClassLoader());

            if (res == null) {
                if (log.isDebugEnabled() == true) {
                    log.debug("Ping failed (invalid response): " + nodeId);
                }

                return false;
            }

            assert res.getType() == PING_RESPONSE : "ASSERTION [line=812, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";
        }
        catch (IOException e) {
            if (log.isDebugEnabled() == true) {
                log.debug("Ping failed (" + e.getMessage() + "): " + nodeId);
            }

            return false;
        }
        catch (GridException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                log.error("Ping failed (Invalid class for ping response).", e);
            }
            else if (log.isDebugEnabled() == true) {
                log.debug("Ping failed ("
                    + (e.getCause() instanceof IOException ? e.getCause().getMessage() : e.getMessage()) + "): "
                    + nodeId);
            }

            return false;
        }
        finally {
            GridUtils.close(out, log);
            GridUtils.close(in, log);
            GridUtils.close(sock, log);
        }

        if (log.isDebugEnabled() == true) {
            log.debug("Ping ok: " + nodeId);
        }

        return true;
    }

    /**
     * @param e IO error.
     */
    private void handleNetworkChecks(IOException e) {
        //noinspection InstanceofCatchParameter
        if (e instanceof SocketException == true && GridOs.isWindowsVista() == true) {
            log.warning("Note that Windows Vista has a known problem with recovering network " +
                "connectivity after waking up from deep sleep or hibernate mode. " +
                "Due to this error GridGain cannot recover from this error " +
                "automatically and you will need to restart this grid node manually.");
        }

        //noinspection UnusedCatchParameter
        try {
            if (GridNetworkHelper.isLocalHostChanged() == true) {
                log.warning("It appears that you are running on DHCP and " +
                    "local host has been changed. GridGain cannot recover from this error " +
                    "automatically and you will need to manually restart this grid node. For this " +
                    "reason we do not recommend running grid node on DHCP (at least not in a " +
                    "production environment).");
            }
        }
        catch (IOException e1) {
            // Ignore this error as we probably experiencing the same
            // network problem.
        }
    }

    /**
     * Heartbeat sending thread. It sends heartbeat messages every
     * {@link GridMulticastDiscoverySpi#beatFreq} milliseconds. If node is going
     * to leave grid it sends corresponded message with leaving state.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class MulticastHeartbeatSender extends GridSpiThread {
        /** Heartbeat message helper. */
        private final GridMulticastDiscoveryHeartbeat beat = new GridMulticastDiscoveryHeartbeat(
            nodeId, localHost, boundTcpPort, false, startTime);

        /** Multicast socket to send broadcast messages. */
        private volatile MulticastSocket sock = null;

        /** */
        private final Object beatMux = new Object();

        /** */
        private boolean errMsgThrottle = false;

        /**
         * Creates new instance of sender.
         *
         * @throws GridSpiException Thrown if SPI is unable to create multicast socket.
         */
        MulticastHeartbeatSender() throws GridSpiException {
            super(gridName, "grid-mcast-disco-beat-sender", log);

            try {
                createSocket();
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("Failed to create multicast sender socket.", e).setData(908, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void cleanup() {
            // Potentially double closing, better safe than sorry.
            GridUtils.close(sock);
        }

        /**
         * Creates new multicast socket and disables loopback mode.
         *
         * @throws IOException Thrown if unable to create socket.
         */
        private void createSocket() throws IOException {
            sock = new MulticastSocket(new InetSocketAddress(localHost, 0));

            // Number of router hops (0 for loopback).
            sock.setTimeToLive(ttl);
        }

        /**
         *
         */
        @SuppressWarnings({"NakedNotify"})
        void wakeUp() {
            // Wake up waiting sender, so the heartbeat
            // can be sent right away.
            synchronized (beatMux) {
                beatMux.notifyAll();
            }
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"UnconditionalWait"})
        @Override
        public void body() throws InterruptedException {
            int n = leaveAttempts;

            // Remote nodes will be able to extract node address, port,
            // and alive status from the beat.
            DatagramPacket packet = new DatagramPacket(beat.getData(), beat.getData().length, mcastAddr,
                mcastPort);

            while (n > 0) {
                beat.setLeaving(isInterrupted());

                try {
                    if (sock == null) {
                        createSocket();
                    }

                    // Update node metrics.
                    beat.setMetrics(metricsProvider.getMetrics());

                    // Reset buffer before every send.
                    packet.setData(beat.getData());

                    sock.send(packet);

                    // Reset error message throttle flag.
                    errMsgThrottle = false;
                }
                catch (IOException e) {
                    if (errMsgThrottle == false) {
                        handleNetworkChecks(e);

                        log.error("Failed to send heart beat (will try again in " + beatFreq + "ms). Note that " +
                            "this error message will appear only once for this network problem to avoid log flooding " +
                            "but attempts to reconnect will continue.", e);

                        errMsgThrottle = true;
                    }

                    GridUtils.close(sock);

                    sock = null;
                }

                if (isInterrupted() == true) {
                    n--;
                }
                else {
                    //noinspection CaughtExceptionImmediatelyRethrown
                    try {
                        synchronized (beatMux) {
                            beatMux.wait(beatFreq);
                        }
                    }
                    catch (InterruptedException e) {
                        if (isInterrupted() == false) {
                            throw e;
                        }
                    }
                }
            }
        }
    }

    /**
     * Multicast messages receiving thread. This class process all heartbeat messages
     * that comes from the others.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class MulticastHeartbeatReceiver extends GridSpiThread {
        /** Multicast socket message is read from. */
        private MulticastSocket sock = null;

        /**
         * Creates new instance of receiver and joins multicast group.
         *
         * @throws GridSpiException Thrown if SPI is unable to create socket or join group.
         */
        MulticastHeartbeatReceiver() throws GridSpiException {
            super(gridName, "grid-mcast-disco-beat-rcvr", log);

            try {
                createSocket();
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("Failed to create multicast socket.", e).setData(1036, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
            }
        }

        /**
         * Creates new multicast socket and joins to multicast group.
         *
         * @return Created socket.
         * @throws IOException Thrown if it's impossible to create socket or join multicast group.
         */
        private MulticastSocket createSocket() throws IOException {
            synchronized (mux) {
                // Note, that we purposely don't specify local host binding,
                // as it does not work on some OS (including Fedora).
                sock = new MulticastSocket(mcastPort);

                // Enable support for more than one node on the same machine.
                sock.setLoopbackMode(false);

                // If loopback mode did not get enabled.
                if (sock.getLoopbackMode() == true) {
                    log.warning("Loopback mode is disabled which prevents nodes on the same machine from discovering " +
                        "each other.");

                    if (isMcastEnabled == false) {
                        // Since there is no way to check if multicast is enabled,
                        // we assume that it is enabled.
                        isMcastEnabled = true;

                        mux.notifyAll();
                    }
                }

                // Set to local bind interface.
                sock.setInterface(localHost);

                // Join multicast group.
                sock.joinGroup(mcastAddr);

                if (log.isInfoEnabled() == true) {
                    log.info("Successfully bound to Multicast port: " + mcastPort);
                }

                return sock;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void interrupt() {
            super.interrupt();

            synchronized (mux) {
                GridUtils.close(sock);

                sock = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void cleanup() {
            synchronized (mux) {
                // Potentially double closing, better safe than sorry.
                GridUtils.close(sock);

                sock = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({"NonStaticInitializer"})
        @Override
        public void body() throws InterruptedException {
            // Note that local host can be IPv4 or IPv6.
            byte[] data = new byte[GridMulticastDiscoveryHeartbeat.STATIC_DATA_LENGTH + localHost.getAddress().length];

            final DatagramPacket pckt = new DatagramPacket(data, data.length);

            while (isInterrupted() == false) {
                try {
                    MulticastSocket sock;

                    synchronized (mux) {
                        // Avoid recreating socket after cancel.
                        if (isInterrupted() == true) {
                            return;
                        }

                        sock = this.sock;

                        if (sock == null) {
                            sock = createSocket();
                        }
                    }

                    // Wait for node attributes.
                    sock.receive(pckt);

                    final GridMulticastDiscoveryHeartbeat beat = new GridMulticastDiscoveryHeartbeat(pckt.getData());

                    final UUID beatNodeId = beat.getNodeId();

                    GridMulticastDiscoveryNode node = null;

                    GridMulticastDiscoveryNode beatNode = null;

                    synchronized (mux) {
                        if (beatNodeId.equals(nodeId) == true) {
                            // If received a heartbeat from itself, then multicast
                            // is enabled on local node.
                            if (isMcastEnabled == false) {
                                isMcastEnabled = true;

                                mux.notifyAll();
                            }

                            beatNode = locNode;

                            node = locNode;
                        }
                        else {
                            if (log.isDebugEnabled() == true) {
                                log.debug("Received beat: " + beat);
                            }

                            if (beat.isLeaving() == true) {
                                node = allNodes.get(beatNodeId);

                                if (node != null && node.getState() == READY) {
                                    node.onLeft();

                                    assert node.getState() == LEFT : "ASSERTION [line=1174, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]. " + "Invalid node state: " + node.getState();

                                    rmtNodes = null;

                                    // Listener notification.
                                    notifyDiscovery(GridDiscoveryEventType.LEFT, node);

                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Node left grid: " + node);
                                    }

                                    // Notify threads waiting for node to change state.
                                    mux.notifyAll();
                                }
                                else {
                                    // Either receiving heartbeats after node was removed
                                    // by topology cleaner or node left before joined.
                                    continue;
                                }
                            }
                            else {
                                node = allNodes.get(beatNodeId);

                                // If found new node.
                                if (node == null) {
                                    // Local node cannot communicate with itself.
                                    assert nodeId.equals(beatNodeId) == false : "ASSERTION [line=1200, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

                                    allNodes.put(
                                        beatNodeId,
                                        node = new GridMulticastDiscoveryNode(
                                            beatNodeId,
                                            beat.getInetAddress(),
                                            beat.getTcpPort(),
                                            beat.getStartTime(),
                                            beat.getMetrics()));

                                    rmtNodes = null;

                                    // No listener notification since node is not READY yet.
                                    assert node.getState() == NEW : "ASSERTION [line=1214, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]. " + "Invalid node state: " + node.getState();

                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Added NEW node to grid: " + node);
                                    }
                                }
                                else if (node.getState() == NEW) {
                                    node.onHeartbeat(beat.getMetrics());

                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Received heartbeat for new node (will ignore): " + node);
                                    }

                                    continue;
                                }
                                // If zombie.
                                else if (node.getState() == LEFT) {
                                    if (log.isDebugEnabled() == true) {
                                        log.debug("Received zombie heartbeat for left node (will ignore): " + node);
                                    }

                                    continue;
                                }
                                // If duplicate node ID.
                                else if (node.getStartTime() < beat.getStartTime()) {
                                    log.warning("Node with duplicate node ID is trying to join (will ignore): " +
                                        node.getId());

                                    continue;
                                }
                                else {
                                    // New heartbeat callback.
                                    node.onHeartbeat(beat.getMetrics());

                                    beatNode = node;
                                }
                            }
                        }
                    }

                    // Metrics update callback.
                    if (beatNode != null) {
                        notifyDiscovery(GridDiscoveryEventType.METRICS_UPDATED, beatNode);
                    }

                    assert node != null : "ASSERTION [line=1259, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

                    // If node is new, initiate TCP handshake.
                    if (node.getState() == NEW) {
                        assert nodeId.equals(node.getId()) == false : "ASSERTION [line=1263, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

                        // Wake up heartbeat sender, so heartbeat will be
                        // sent immediately.
                        mcastSender.wakeUp();

                        synchronized (mux) {
                            // If stopping process started, no point
                            // to initiate handshakes.
                            if (isStopping.get() == false) {
                                // Node with larger UUID gets to initiate handshake.
                                boolean activeHandshakeSender = node.getId().compareTo(locNode.getId()) > 0;

                                TcpHandshakeSender sender = new TcpHandshakeSender(beat, node, activeHandshakeSender);

                                // Register sender.
                                workers.add(sender);

                                sender.start();
                            }
                        }
                    }
                }
                catch (IOException e) {
                    if (isInterrupted() == false) {
                        log.error("Failed to listen to heartbeats (will wait for " + beatFreq + "ms and try again)", e);

                        synchronized (mux) {
                            GridUtils.close(sock);

                            sock = null;
                        }

                        Thread.sleep(beatFreq);
                    }
                }
            }
        }
    }

    /**
     * Tcp handshake sender.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class TcpHandshakeSender extends GridSpiThread {
        /** Heartbeat. */
        private final GridMulticastDiscoveryHeartbeat beat;

        /** New node. */
        private final GridMulticastDiscoveryNode newNode;

        /** */
        private volatile Socket attrSock = null;

        /** True if this handshake sender should send handshake immediatly. */
        private boolean activeSender;

        /**
         * @param beat Heartbeat received.
         * @param newNode Joining node.
         * @param activeSender whether send handshake immediatly.
         */
        TcpHandshakeSender(GridMulticastDiscoveryHeartbeat beat, GridMulticastDiscoveryNode newNode, boolean activeSender) {
            super(gridName, "grid-mcast-disco-tcp-handshake-sender", log);

            assert beat != null : "ASSERTION [line=1330, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";
            assert newNode != null : "ASSERTION [line=1331, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

            this.beat = beat;
            this.newNode = newNode;
            this.activeSender = activeSender;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void body() {
            try {
                if (activeSender == false) {
                    //noinspection UnusedCatchParameter
                    try {
                        Thread.sleep(beatFreq * maxMissedBeats);
                    }
                    catch (InterruptedException e) {
                        return;
                    }
                    synchronized (mux) {
                        if (newNode.getState() != NEW || isStopping.get() == true) {
                            return; // Handshake already come.
                        }
                    }
                }

                if (log.isDebugEnabled() == true) {
                    log.debug("Request attributes from node [addr=" + beat.getInetAddress() +
                        ", port=" + beat.getTcpPort() + ']');
                }

                attrSock = new Socket(beat.getInetAddress(), beat.getTcpPort(), localHost, 0);

                InputStream in = null;
                OutputStream out = null;

                try {
                    out = attrSock.getOutputStream();
                    in = attrSock.getInputStream();

                    GridMarshalHelper.marshal(marshaller, new GridMulticastDiscoveryMessage(ATTRS_REQUEST, nodeId,
                        localHost, boundTcpPort, nodeAttrs, startTime, metricsProvider.getMetrics()), out);

                    GridMulticastDiscoveryMessage msg =
                        GridMarshalHelper.unmarshal(marshaller, in, getClass().getClassLoader());

                    // Safety check.
                    if (msg.getType() != ATTRS_RESPONSE) {
                        log.warning("Received message of wrong type [expected=ATTRS_RESPONSE"
                            + ", actual=" + msg.getType() + ']');

                        return;
                    }

                    // Safety check.
                    if (msg.getNodeId().equals(beat.getNodeId()) == false) {
                        log.warning("Received attributes from unexpected node [expected=" +
                            beat.getNodeId() + ", actual=" + msg.getNodeId() + ']');

                        return;
                    }

                    synchronized (mux) {
                        if (newNode.getState() == NEW) {
                            assert msg.getNodeId().equals(newNode.getId()) : "ASSERTION [line=1397, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

                            newNode.setAttributes(msg.getAttributes());

                            rmtNodes = null;

                            if (log.isDebugEnabled() == true) {
                                log.debug("Node moved from NEW to READY: " + newNode);
                            }

                            // Listener notification.
                            notifyDiscovery(GridDiscoveryEventType.JOINED, newNode);
                        }
                        else {
                            // Node might be in state LEFT if it was swept.
                            if (log.isDebugEnabled() == true) {
                                log.debug("Node is not in NEW state: " + newNode);
                            }

                            return;
                        }

                        // Notify threads waiting for node to change state.
                        mux.notifyAll();
                    }

                    // Confirm received attributes.
                    GridMarshalHelper.marshal(marshaller, new GridMulticastDiscoveryMessage(ATTRS_CONFIRMED), out);
                }
                finally {
                    GridUtils.close(out, log);
                    GridUtils.close(in, log);
                }
            }
            catch (ConnectException e) {
                if (isStopping.get() == false && isInterrupted() == false) {
                    log.warning("Failed to connect to node (did the node stop?) [addr" + beat.getInetAddress() +
                        ", port=" + beat.getTcpPort() + ", error=" + e.getMessage() + "]. " +
                        "Make sure that destination node is alive and has properly " +
                        "configured firewall that allows GridGain incoming traffic " +
                        "(especially on Windows Vista).", e);
                }
            }
            catch (IOException e) {
                if (isStopping.get() == false && isInterrupted() == false) {
                    log.error("Error requesting node attributes from node (did the node stop?) [addr=" +
                        beat.getInetAddress() + ", port=" + beat.getTcpPort() + ']', e);

                    handleNetworkChecks(e);
                }
            }
            catch (GridException e) {
                log.error("Error requesting node attributes from node.", e);
            }
            finally {
                GridUtils.close(attrSock, log);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void cleanup() {
            synchronized (mux) {
                workers.remove(this);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void interrupt() {
            super.interrupt();

            GridUtils.close(attrSock, log);
        }
    }

    /**
     * Listener that processes TCP messages.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class TcpHandshakeListener extends GridSpiThread {
        /** Socket TCP listener is set to. */
        private ServerSocket tcpSock = null;

        /**
         * Creates new instance of listener.
         *
         * @throws GridSpiException Thrown if SPI is unable to create socket.
         */
        TcpHandshakeListener() throws GridSpiException {
            super(gridName, "grid-mcast-disco-tcp-handshake-listener", log);

            try {
                createTcpSocket();
            }
            catch (IOException e) {
                throw (GridSpiException)new GridSpiException("Failed to create TCP server for receiving node attributes.", e).setData(1499, "src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java");
            }
        }

        /**
         * Creates new socket.
         *
         * @return Created socket.
         * @throws IOException Thrown if socket could not be open.
         */
        private ServerSocket createTcpSocket() throws IOException {
            int maxPort = tcpPort + localPortRange;

            synchronized (mux) {
                for (int port = tcpPort; port < maxPort; port++) {
                    //noinspection CaughtExceptionImmediatelyRethrown
                    try {
                        tcpSock = new ServerSocket(port, 0, localHost);

                        boundTcpPort = port;

                        if (log.isInfoEnabled() == true) {
                            log.info("Successfully bound to TCP port: " + boundTcpPort);
                        }

                        break;
                    }
                    catch (BindException e) {
                        if (port + 1 < maxPort) {
                            if (log.isInfoEnabled() == true) {
                                log.info("Failed to bind to local TCP port (will try next port within range): " +
                                    port);
                            }
                        }
                        else {
                            throw e;
                        }
                    }
                }

                return tcpSock;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void interrupt() {
            super.interrupt();

            synchronized (mux) {
                GridUtils.close(tcpSock, log);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void cleanup() {
            synchronized (mux) {
                // Potentially double closing, better safe than sorry.
                GridUtils.close(tcpSock, log);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void body() throws InterruptedException {
            while (isInterrupted() == false) {
                try {
                    ServerSocket server;

                    synchronized (mux) {
                        server = tcpSock;

                        if (server == null) {
                            server = createTcpSocket();
                        }
                    }

                    Socket sock = server.accept();

                    try {
                        InputStream in = sock.getInputStream();
                        OutputStream out = sock.getOutputStream();

                        try {
                            GridMulticastDiscoveryMessage msg =
                                GridMarshalHelper.unmarshal(marshaller, in, getClass().getClassLoader());

                            if (msg.getType() == PING_REQUEST) {
                                GridMarshalHelper.marshal(marshaller, new GridMulticastDiscoveryMessage(PING_RESPONSE),
                                    out);
                            }
                            else if (msg.getType() == ATTRS_REQUEST) {
                                GridMulticastDiscoveryNode node;

                                UUID id = msg.getNodeId();

                                // Local node cannot communicate with itself.
                                assert nodeId.equals(id) == false : "ASSERTION [line=1603, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

                                // Get metrics outside the synchronization to avoid
                                // possible deadlocks.
                                GridNodeMetrics locMetrics = metricsProvider.getMetrics();

                                synchronized (mux) {
                                    node = allNodes.get(id);

                                    if (node == null) {
                                        node = new GridMulticastDiscoveryNode(id, msg.getAddress(),
                                            msg.getPort(), msg.getStartTime(), msg.getMetrics());
                                    }

                                    assert id.equals(node.getId()) == true : "ASSERTION [line=1617, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]";

                                    Map<String, Serializable> attrs = msg.getAttributes();

                                    // Send back own attributes. This requires confirmation (ATTRS_CONFIRMED).
                                    GridMulticastDiscoveryMessage confirmMsg =
                                        new GridMulticastDiscoveryMessage(ATTRS_RESPONSE, nodeId, localHost,
                                            boundTcpPort, nodeAttrs, startTime, locMetrics);

                                    GridMarshalHelper.marshal(marshaller, confirmMsg, out);

                                    msg = GridMarshalHelper.unmarshal(marshaller, in, getClass().getClassLoader());

                                    if (msg.getType() == ATTRS_CONFIRMED) {
                                        if (node.getState() != NEW) {
                                            if (log.isDebugEnabled() == true) {
                                                log.debug("Received handshake request from stopping node (will ignore): " +
                                                    node);
                                            }
                                        }
                                        else {
                                            node.setAttributes(attrs);

                                            allNodes.put(id, node);

                                            if (log.isDebugEnabled() == true) {
                                                log.debug("Node moved to READY: " + node);
                                            }

                                            rmtNodes = null;

                                            // Listener notification.
                                            notifyDiscovery(GridDiscoveryEventType.JOINED, node);
                                        }
                                    }
                                    else {
                                        log.warning("Received message of wrong type [expected=ATTRS_CONFIRMED"
                                            + ", actual=" + msg.getType() + ']');

                                        continue;
                                    }

                                    // Notify threads waiting for node to change state.
                                    mux.notifyAll();
                                }

                                if (log.isDebugEnabled() == true) {
                                    log.debug("Added new node to the topology: " + node);
                                }
                            }
                            else {
                                log.error("Received unknown message: " + msg);
                            }
                        }
                        finally {
                            GridUtils.close(out, log);
                            GridUtils.close(in, log);
                        }
                    }
                    catch (IOException e) {
                        if (isStopping.get() == false && isInterrupted() == false) {
                            log.warning("Failed to send local node attributes to remote node (did the node stop?): " +
                                sock, e);
                        }
                    }
                    catch (GridException e) {
                        log.error("Failed to send local node attributes to remote node.", e);
                    }
                    finally {
                        GridUtils.close(sock, log);
                    }
                }
                catch (IOException e) {
                    if (isStopping.get() == false && isInterrupted() == false) {
                        log.error("Failed to accept remote TCP connections, will wait for " + beatFreq +
                            "ms and try again.", e);

                        synchronized (mux) {
                            GridUtils.close(tcpSock, log);

                            tcpSock = null;
                        }

                        Thread.sleep(beatFreq);
                    }
                }
            }
        }
    }

    /**
     * Node sweeper implementation that cleans up dead nodes. This thread looks after
     * available nodes list and removes those ones that did not send heartbeat message
     * last {@link GridMulticastDiscoverySpi#maxMissedBeats} * {@link GridMulticastDiscoverySpi#beatFreq}
     * milliseconds.
     *
     * @author 2005-2009 Copyright (C) GridGain Systems. All Rights Reserved.
     * @version 2.1.1
     */
    private class NodeSweeper extends GridSpiThread {
        /**
         * Creates new instance of node sweeper.
         */
        NodeSweeper() {
            super(gridName, "grid-mcast-disco-node-sweeper", log);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void body() throws InterruptedException {
            final long maxSilenceTime = beatFreq * maxMissedBeats;

            while (isInterrupted() == false) {
                synchronized (mux) {
                    for (Iterator<GridMulticastDiscoveryNode> iter = allNodes.values().iterator();
                        iter.hasNext() == true;) {
                        GridMulticastDiscoveryNode node = iter.next();

                        // Check if node needs to be removed from topology.
                        if (System.currentTimeMillis() - node.getLastHeartbeat() > maxSilenceTime) {
                            if (node.getState() != LEFT) {
                                if (log.isDebugEnabled() == true) {
                                    log.debug("Removed failed node from topology: " + node);
                                }

                                boolean notify = node.getState() == READY;

                                node.onFailed();

                                assert node.getState() == LEFT : "ASSERTION [line=1748, file=src/java/org/gridgain/grid/spi/discovery/multicast/GridMulticastDiscoverySpi.java]. " + "Invalid node state: " + node.getState();

                                rmtNodes = null;

                                if (notify == true) {
                                    // Notify listener of failure only for ready nodes.
                                    notifyDiscovery(GridDiscoveryEventType.FAILED, node);
                                }

                                mux.notifyAll();
                            }

                            iter.remove();
                        }
                    }
                }

                Thread.sleep(beatFreq);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getConsistentAttributeNames() {
        List<String> attrs = new ArrayList<String>(3);

        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_CLASS));
        attrs.add(createSpiAttributeName(GridNodeAttributes.ATTR_SPI_VER));
        attrs.add(createSpiAttributeName(HEARTBEAT_ATTRIBUTE_KEY));

        return attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return GridToStringBuilder.toString(GridMulticastDiscoverySpi.class, this);
    }
}
